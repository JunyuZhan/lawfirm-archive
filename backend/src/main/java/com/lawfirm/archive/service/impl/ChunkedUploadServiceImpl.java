package com.lawfirm.archive.service.impl;

import com.lawfirm.archive.dto.ChunkDTO;
import com.lawfirm.archive.dto.ChunkResponse;
import com.lawfirm.archive.exception.ResourceNotFoundException;
import com.lawfirm.archive.exception.StorageException;
import com.lawfirm.archive.model.Case;
import com.lawfirm.archive.model.Document;
import com.lawfirm.archive.model.UploadTask;
import com.lawfirm.archive.model.UploadTask.UploadTaskStatus;
import com.lawfirm.archive.repository.CaseRepository;
import com.lawfirm.archive.repository.DocumentRepository;
import com.lawfirm.archive.repository.UploadTaskRepository;
import com.lawfirm.archive.service.ChunkedUploadService;
import com.lawfirm.archive.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkedUploadServiceImpl implements ChunkedUploadService {

    private final UploadTaskRepository uploadTaskRepository;
    private final DocumentRepository documentRepository;
    private final CaseRepository caseRepository;
    private final MinioService minioService;

    @Value("${app.upload.chunk-size:5242880}") // 默认5MB
    private int defaultChunkSize;

    @Value("${app.upload.temp-dir:temp}")
    private String tempDir;

    @Value("${app.upload.expire-hours:24}")
    private int expireHours;

    @Override
    @Transactional
    public UploadTask initUpload(String fileName, Long fileSize, String fileType, Long caseId, 
                                String category, String remarks) {
        // 检查案件是否存在
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));

        // 计算分片数量
        int totalChunks = calculateTotalChunks(fileSize);
        String taskId = UUID.randomUUID().toString();

        // 创建上传临时目录
        createChunkDir(taskId);

        // 创建上传任务
        UploadTask task = UploadTask.builder()
                .id(taskId)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .chunkSize(defaultChunkSize)
                .totalChunks(totalChunks)
                .uploadedChunks(0)
                .caseId(caseId)
                .category(category)
                .remarks(remarks)
                .storageName(UUID.randomUUID().toString() + "_" + fileName)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .status(UploadTaskStatus.INITIALIZED)
                .build();

        return uploadTaskRepository.save(task);
    }

    @Override
    @Transactional
    public ChunkResponse processChunk(ChunkDTO chunkDTO) throws Exception {
        String taskId = chunkDTO.getTaskId();
        int chunkNumber = chunkDTO.getChunkNumber();

        // 获取上传任务
        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("上传任务不存在"));

        // 检查任务状态
        if (task.getStatus() != UploadTaskStatus.INITIALIZED && 
            task.getStatus() != UploadTaskStatus.IN_PROGRESS) {
            throw new StorageException("上传任务状态异常: " + task.getStatus());
        }

        // 保存分片到临时目录
        String chunkFileName = getChunkFileName(taskId, chunkNumber);
        File chunkFile = new File(chunkFileName);
        try {
            chunkDTO.getFile().transferTo(chunkFile);
        } catch (IOException e) {
            throw new StorageException("保存分片文件失败", e);
        }

        // 更新任务状态
        task.getUploadedChunkNumbers().add(chunkNumber);
        task.setUploadedChunks(task.getUploadedChunkNumbers().size());
        task.setStatus(UploadTaskStatus.IN_PROGRESS);
        task.setUpdatedAt(ZonedDateTime.now());

        // 检查是否所有分片都已上传
        boolean completed = task.isAllChunksUploaded();
        if (completed) {
            task.setStatus(UploadTaskStatus.COMPLETED);
        }

        uploadTaskRepository.save(task);

        // 构建响应
        return ChunkResponse.builder()
                .taskId(taskId)
                .chunkNumber(chunkNumber)
                .uploadedChunks(task.getUploadedChunks())
                .totalChunks(task.getTotalChunks())
                .completed(completed)
                .build();
    }

    @Override
    @Transactional
    public Document mergeChunks(String taskId) {
        // 获取上传任务
        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("上传任务不存在"));

        // 检查任务状态
        if (task.getStatus() != UploadTaskStatus.COMPLETED) {
            throw new StorageException("上传任务未完成，无法合并分片");
        }

        try {
            // 更新任务状态
            task.setStatus(UploadTaskStatus.MERGING);
            uploadTaskRepository.save(task);

            // 使用本地文件合并方式，然后上传到MinIO
            File mergedFile = mergeChunksToFile(task);
            
            // 上传合并后的文件到MinIO
            try (FileInputStream fis = new FileInputStream(mergedFile)) {
                minioService.uploadFile(fis, task.getStorageName(), mergedFile.length(), task.getFileType());
            }

            // 创建文档记录
            Case caseEntity = caseRepository.findById(task.getCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Case", "id", task.getCaseId()));

            Document document = new Document();
            document.setCaseId(task.getCaseId());
            document.setCaseEntity(caseEntity);
            document.setFileName(task.getFileName());
            document.setStorageName(task.getStorageName());
            document.setFileType(task.getFileType());
            document.setFileSize(task.getFileSize());
            document.setUploadTime(ZonedDateTime.now());
            document.setUpdatedAt(ZonedDateTime.now());
            document.setCategory(task.getCategory());
            document.setRemarks(task.getRemarks());
            document.setSortOrder(0);
            document.setVersion(1);

            Document savedDoc = documentRepository.save(document);

            // 更新任务状态
            task.setStatus(UploadTaskStatus.MERGED);
            uploadTaskRepository.save(task);

            // 清理临时文件
            cleanupChunks(taskId);
            mergedFile.delete();

            return savedDoc;
        } catch (Exception e) {
            // 更新任务状态为失败
            task.setStatus(UploadTaskStatus.FAILED);
            uploadTaskRepository.save(task);
            throw new StorageException("合并分片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public UploadTask getTaskStatus(String taskId) {
        return uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("上传任务不存在"));
    }

    @Override
    @Transactional
    public void cancelUpload(String taskId) {
        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("上传任务不存在"));

        task.setStatus(UploadTaskStatus.CANCELED);
        uploadTaskRepository.save(task);

        // 清理临时文件
        cleanupChunks(taskId);
    }

    @Override
    @Scheduled(fixedDelayString = "${app.upload.cleanup-interval:3600000}") // 默认每小时执行一次
    @Transactional
    public void cleanupExpiredTasks() {
        ZonedDateTime expiryTime = ZonedDateTime.now().minusHours(expireHours);
        
        List<UploadTaskStatus> statusesToClean = Arrays.asList(
                UploadTaskStatus.INITIALIZED,
                UploadTaskStatus.IN_PROGRESS,
                UploadTaskStatus.FAILED,
                UploadTaskStatus.CANCELED
        );
        
        List<UploadTask> expiredTasks = uploadTaskRepository.findByStatusInAndCreatedAtBefore(
                statusesToClean, expiryTime);
        
        for (UploadTask task : expiredTasks) {
            try {
                cleanupChunks(task.getId());
                log.info("清理过期上传任务: {}", task.getId());
            } catch (Exception e) {
                log.error("清理过期上传任务失败: {}", task.getId(), e);
            }
        }
        
        uploadTaskRepository.deleteAll(expiredTasks);
    }

    /**
     * 计算总分片数
     */
    private int calculateTotalChunks(Long fileSize) {
        return (int) Math.ceil((double) fileSize / defaultChunkSize);
    }

    /**
     * 创建分片目录
     */
    private void createChunkDir(String taskId) {
        File dir = new File(getChunkDir(taskId));
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 获取分片目录
     */
    private String getChunkDir(String taskId) {
        return tempDir + File.separator + taskId;
    }

    /**
     * 获取分片文件名
     */
    private String getChunkFileName(String taskId, int chunkNumber) {
        return getChunkDir(taskId) + File.separator + chunkNumber;
    }

    /**
     * 清理分片文件
     */
    private void cleanupChunks(String taskId) {
        try {
            Path chunkDirPath = Paths.get(getChunkDir(taskId));
            if (Files.exists(chunkDirPath)) {
                FileUtils.deleteDirectory(new File(getChunkDir(taskId)));
            }
        } catch (IOException e) {
            log.error("清理分片文件失败: {}", taskId, e);
        }
    }

    /**
     * 合并分片为文件
     */
    private File mergeChunksToFile(UploadTask task) throws IOException {
        String taskId = task.getId();
        int totalChunks = task.getTotalChunks();
        
        File mergedFile = new File(tempDir + File.separator + "merged_" + taskId);
        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            for (int i = 1; i <= totalChunks; i++) {
                File chunkFile = new File(getChunkFileName(taskId, i));
                Files.copy(chunkFile.toPath(), fos);
            }
        }
        
        return mergedFile;
    }
} 