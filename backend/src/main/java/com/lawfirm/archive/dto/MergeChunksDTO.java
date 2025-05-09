package com.lawfirm.archive.dto;

import lombok.Data;

/**
 * 合并分片请求
 */
@Data
public class MergeChunksDTO {
    /**
     * 上传任务ID
     */
    private String taskId;
} 