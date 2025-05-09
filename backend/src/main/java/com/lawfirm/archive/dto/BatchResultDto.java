package com.lawfirm.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量操作结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResultDto {
    /**
     * 成功数量
     */
    private int successCount;
    
    /**
     * 失败数量
     */
    private int failCount;
    
    /**
     * 成功的ID列表
     */
    private String successIds;
    
    /**
     * 失败的ID列表
     */
    private String failIds;
    
    /**
     * 详细消息
     */
    private String message;
} 