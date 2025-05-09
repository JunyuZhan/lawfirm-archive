package com.lawfirm.archive.service;

import com.lawfirm.archive.dto.ChunkDTO;
import com.lawfirm.archive.dto.ChunkResponse;
import com.lawfirm.archive.model.Document;
import com.lawfirm.archive.model.UploadTask;

/**
 * 分片上传服务接口
 */
public interface ChunkedUploadService {
    /**
     * 初始化上传任务
     */
    UploadTask initUpload(String fileName, Long fileSize, String fileType, Long caseId, 
                          String category, String remarks);
    
    /**
     * 处理上传分片
     */
    ChunkResponse processChunk(ChunkDTO chunkDTO) throws Exception;
    
    /**
     * 合并分片
     */
    Document mergeChunks(String taskId);
    
    /**
     * 获取上传任务状态
     */
    UploadTask getTaskStatus(String taskId);
    
    /**
     * 取消上传任务
     */
    void cancelUpload(String taskId);
    
    /**
     * 清理过期的上传任务
     */
    void cleanupExpiredTasks();
} 