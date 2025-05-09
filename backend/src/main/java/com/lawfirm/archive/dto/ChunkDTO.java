package com.lawfirm.archive.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片传输对象
 */
@Data
public class ChunkDTO {
    /**
     * 上传任务ID
     */
    private String taskId;
    
    /**
     * 当前分片序号
     */
    private int chunkNumber;
    
    /**
     * 总分片数
     */
    private int totalChunks;
    
    /**
     * 分片文件
     */
    private MultipartFile file;
} 