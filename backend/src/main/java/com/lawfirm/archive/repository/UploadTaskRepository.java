package com.lawfirm.archive.repository;

import com.lawfirm.archive.model.UploadTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface UploadTaskRepository extends JpaRepository<UploadTask, String> {
    /**
     * 查询过期的上传任务
     */
    List<UploadTask> findByStatusInAndCreatedAtBefore(
            List<UploadTask.UploadTaskStatus> statuses, 
            ZonedDateTime createdAt);
} 