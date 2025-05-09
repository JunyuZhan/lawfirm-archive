package com.lawfirm.archive.controller;

import com.lawfirm.archive.service.DataMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final DataMigrationService dataMigrationService;

    /**
     * 将文件从本地存储迁移到MinIO
     */
    @PostMapping("/migrate-to-minio")
    public ResponseEntity<Map<String, Object>> migrateFilesToMinio() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("开始迁移文件到MinIO");
            dataMigrationService.migrateFilesToMinio();
            
            response.put("success", true);
            response.put("message", "文件迁移完成");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("文件迁移失败", e);
            
            response.put("success", false);
            response.put("message", "文件迁移失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 检查MinIO连接状态
     */
    @PostMapping("/check-minio")
    public ResponseEntity<Map<String, Object>> checkMinioConnection() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("检查MinIO连接");
            // 简单调用一下服务确保连接正常
            dataMigrationService.checkMinioConnection();
            
            response.put("success", true);
            response.put("message", "MinIO连接正常");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MinIO连接失败", e);
            
            response.put("success", false);
            response.put("message", "MinIO连接失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 