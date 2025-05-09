package com.lawfirm.archive.service;

import com.lawfirm.archive.config.MinioConfig;
import com.lawfirm.archive.exception.StorageException;
import com.lawfirm.archive.model.Document;
import com.lawfirm.archive.repository.DocumentRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class DataMigrationService {
    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private MinioService minioService;
    
    @Autowired
    private MinioClient minioClient;
    
    @Autowired
    private MinioConfig minioConfig;

    /**
     * 迁移文件到MinIO
     */
    @Transactional
    public void migrateFilesToMinio() throws Exception {
        List<Document> documents = documentRepository.findAll();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        
        if (!Files.exists(uploadPath)) {
            log.info("上传目录不存在,无需迁移");
            return;
        }

        for (Document doc : documents) {
            Path filePath = uploadPath.resolve(doc.getFileName());
            if (Files.exists(filePath)) {
                // 上传到MinIO
                File file = filePath.toFile();
                try (FileInputStream fis = new FileInputStream(file)) {
                    String storageName = doc.getStorageName();
                    if (storageName == null) {
                        storageName = doc.getFileName();
                        doc.setStorageName(storageName);
                    }
                    
                    minioService.uploadFile(fis, storageName, file.length(), doc.getFileType());
                    documentRepository.save(doc);
                    
                    // 删除本地文件
                    Files.delete(filePath);
                    log.info("已迁移文件: {}", doc.getFileName());
                }
            }
        }
        
        // 如果上传目录为空,删除目录
        if (Files.list(uploadPath).findFirst().isEmpty()) {
            Files.delete(uploadPath);
        }
        
        log.info("文件迁移完成");
    }
    
    /**
     * 检查MinIO连接状态并确保bucket存在
     */
    public void checkMinioConnection() throws Exception {
        try {
            String bucketName = minioConfig.getBucketName();
            
            // 检查存储桶是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
                    
            if (!found) {
                // 创建存储桶
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("创建MinIO存储桶: {}", bucketName);
            }
            
            log.info("MinIO连接正常");
        } catch (Exception e) {
            log.error("MinIO连接失败", e);
            throw new StorageException("MinIO连接失败: " + e.getMessage(), e);
        }
    }
} 