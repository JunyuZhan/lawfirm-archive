package com.lawfirm.archive.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id")
    private Long caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", insertable = false, updatable = false)
    private Case caseEntity;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_name", nullable = false)
    private String storageName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "upload_time")
    private ZonedDateTime uploadTime;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    private String category;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    private String remarks;

    private Integer version = 1;
} 