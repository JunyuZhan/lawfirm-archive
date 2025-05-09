package com.lawfirm.archive.controller;

import com.lawfirm.archive.dto.ChunkDTO;
import com.lawfirm.archive.dto.ChunkResponse;
import com.lawfirm.archive.dto.MergeChunksDTO;
import com.lawfirm.archive.exception.StorageException;
import com.lawfirm.archive.model.Document;
import com.lawfirm.archive.model.UploadTask;
import com.lawfirm.archive.service.ChunkedUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chunked-upload")
@RequiredArgsConstructor
@Slf4j
public class ChunkedUploadController {

    private final ChunkedUploadService chunkedUploadService;

    /**
     * 初始化分片上传
     */
    @PostMapping("/init")
    public ResponseEntity<UploadTask> initUpload(
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam String fileType,
            @RequestParam Long caseId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String remarks) {
        
        log.info("初始化分片上传: fileName={}, fileSize={}, fileType={}, caseId={}", 
                fileName, fileSize, fileType, caseId);
        
        UploadTask task = chunkedUploadService.initUpload(
                fileName, fileSize, fileType, caseId, category, remarks);
        
        return ResponseEntity.ok(task);
    }

    /**
     * 上传分片
     */
    @PostMapping("/chunk")
    public ResponseEntity<ChunkResponse> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskId") String taskId,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks) {
        
        log.info("上传分片: taskId={}, chunkNumber={}/{}", taskId, chunkNumber, totalChunks);
        
        try {
            ChunkDTO chunkDTO = new ChunkDTO();
            chunkDTO.setTaskId(taskId);
            chunkDTO.setChunkNumber(chunkNumber);
            chunkDTO.setTotalChunks(totalChunks);
            chunkDTO.setFile(file);
            
            ChunkResponse response = chunkedUploadService.processChunk(chunkDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("上传分片失败", e);
            throw new StorageException("上传分片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    public ResponseEntity<Document> mergeChunks(@RequestBody MergeChunksDTO mergeRequest) {
        log.info("合并分片: taskId={}", mergeRequest.getTaskId());
        
        Document document = chunkedUploadService.mergeChunks(mergeRequest.getTaskId());
        return ResponseEntity.ok(document);
    }

    /**
     * 检查上传状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<UploadTask> checkStatus(@PathVariable String taskId) {
        log.info("检查上传状态: taskId={}", taskId);
        
        UploadTask task = chunkedUploadService.getTaskStatus(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * 取消上传
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> cancelUpload(@PathVariable String taskId) {
        log.info("取消上传: taskId={}", taskId);
        
        chunkedUploadService.cancelUpload(taskId);
        return ResponseEntity.ok().build();
    }
} 