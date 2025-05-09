package com.lawfirm.archive.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 分片上传任务
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "upload_tasks")
public class UploadTask {
    /**
     * 上传任务ID
     */
    @Id
    private String id;
    
    /**
     * 文件名
     */
    @Column(nullable = false)
    private String fileName;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 分片大小
     */
    private Integer chunkSize;
    
    /**
     * 总分片数
     */
    private Integer totalChunks;
    
    /**
     * 已上传分片数
     */
    @Column(name = "uploaded_chunks")
    private Integer uploadedChunks;
    
    /**
     * 已上传分片列表
     */
    @ElementCollection
    @CollectionTable(name = "upload_task_chunks", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "chunk_number")
    @Builder.Default
    private Set<Integer> uploadedChunkNumbers = new HashSet<>();
    
    /**
     * 存储名称
     */
    private String storageName;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 案件ID
     */
    @Column(name = "case_id")
    private Long caseId;
    
    /**
     * 上传者ID
     */
    @Column(name = "uploader_id")
    private Long uploaderId;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private ZonedDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
    
    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    private UploadTaskStatus status;
    
    /**
     * 任务状态枚举
     */
    public enum UploadTaskStatus {
        INITIALIZED,  // 初始化
        IN_PROGRESS,  // 上传中
        COMPLETED,    // 完成
        MERGING,      // 合并中
        MERGED,       // 已合并
        FAILED,       // 失败
        CANCELED      // 取消
    }
    
    /**
     * 是否已完成所有分片上传
     */
    @Transient
    public boolean isAllChunksUploaded() {
        return uploadedChunks != null && totalChunks != null && uploadedChunks >= totalChunks;
    }
} 