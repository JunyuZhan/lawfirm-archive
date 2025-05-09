package com.lawfirm.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResponse {
    /**
     * 上传任务ID
     */
    private String taskId;
    
    /**
     * 当前分片序号
     */
    private int chunkNumber;
    
    /**
     * 已上传分片数
     */
    private int uploadedChunks;
    
    /**
     * 总分片数
     */
    private int totalChunks;
    
    /**
     * 是否已完成所有分片上传
     */
    private boolean completed;
}