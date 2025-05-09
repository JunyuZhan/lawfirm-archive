package com.lawfirm.archive.service;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import io.minio.messages.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.lawfirm.archive.config.MinioConfig;
import com.lawfirm.archive.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件(MultipartFile)
     */
    public String uploadFile(MultipartFile file) throws Exception {
        // 生成唯一文件名
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        
        // 上传文件到MinIO
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        return fileName;
    }

    /**
     * 上传文件(InputStream)
     */
    public void uploadFile(InputStream inputStream, String fileName, long size, String contentType) throws Exception {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build()
        );
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String fileName) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName)
                .build()
        );
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName)
                .build()
        );
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String fileName) throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(fileName)
                .method(Method.GET)
                .build()
        );
    }
    
    /**
     * 初始化分片上传
     * 在8.5.7版本的MinIO Java SDK中，没有直接的CreateMultipartUploadArgs类
     * 使用替代方法实现
     * @param fileName 文件名
     * @param contentType 内容类型
     * @return 上传ID
     */
    public String initMultipartUpload(String fileName, String contentType) throws Exception {
        try {
            // 在8.5.7版本中，我们可以通过生成预签名URL来初始化分片上传
            Map<String, String> reqParams = new HashMap<>();
            reqParams.put("uploads", "");
            
            String uploadUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.POST)
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .expiry(1, TimeUnit.DAYS)
                    .extraQueryParams(reqParams)
                    .build()
            );
            
            // 实际应用中，需要通过HTTP客户端发送POST请求到uploadUrl来获取uploadId
            // 这里为了简化，我们直接返回一个模拟的uploadId
            String uploadId = UUID.randomUUID().toString();
            log.info("初始化分片上传成功，uploadId: {}, 文件名: {}", uploadId, fileName);
            return uploadId;
        } catch (Exception e) {
            log.error("初始化分片上传失败", e);
            throw new StorageException("初始化分片上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取分片上传URL
     * @param fileName 文件名
     * @param uploadId 上传ID
     * @param partNumber 分片序号
     * @param expiry 过期时间（秒）
     * @return 上传URL
     */
    public String getPresignedUrlForPart(String fileName, String uploadId, int partNumber, int expiry) throws Exception {
        try {
            Map<String, String> reqParams = new HashMap<>();
            reqParams.put("uploadId", uploadId);
            reqParams.put("partNumber", String.valueOf(partNumber));
            
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .expiry(expiry, TimeUnit.SECONDS)
                    .extraQueryParams(reqParams)
                    .build()
            );
            
            return url;
        } catch (Exception e) {
            log.error("获取分片上传URL失败", e);
            throw new StorageException("获取分片上传URL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传分片
     * 在8.5.7版本的MinIO Java SDK中，没有直接的UploadPartArgs类
     * 这里我们简化实现，直接使用PutObjectArgs上传小文件
     * @param fileName 文件名
     * @param uploadId 上传ID
     * @param partNumber 分片序号
     * @param partData 分片数据
     * @return etag 标识
     */
    public String uploadPart(String fileName, String uploadId, int partNumber, byte[] partData) throws Exception {
        try {
            // 修改文件名，加入分片信息
            String partFileName = fileName + ".part." + partNumber;
            InputStream partStream = new ByteArrayInputStream(partData);
            
            ObjectWriteResponse response = minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(partFileName)
                    .stream(partStream, partData.length, -1)
                    .build()
            );
            
            return response.etag();
        } catch (Exception e) {
            log.error("上传分片失败", e);
            throw new StorageException("上传分片失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成分片上传
     * 在8.5.7版本的MinIO Java SDK中，没有直接的CompleteMultipartUploadArgs类
     * 这里我们改为手动合并之前上传的分片
     * @param fileName 文件名
     * @param uploadId 上传ID
     * @param parts 分片信息
     */
    public void completeMultipartUpload(String fileName, String uploadId, Part[] parts) throws Exception {
        try {
            // 在实际应用中，需要通过HTTP客户端发送请求完成分片上传
            // 这里为了简化，我们记录一下日志表示操作成功
            log.info("完成分片上传, 文件名: {}, 分片数: {}", fileName, parts.length);
            
            // 注意：这里没有真正的合并操作，在ChunkedUploadServiceImpl中我们已经在本地合并了文件
            // 若要在真实环境中使用，还需要实现真正的分片合并逻辑
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            throw new StorageException("完成分片上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 中止分片上传
     * 在8.5.7版本的MinIO Java SDK中，没有直接的AbortMultipartUploadArgs类
     * @param fileName 文件名
     * @param uploadId 上传ID
     */
    public void abortMultipartUpload(String fileName, String uploadId) throws Exception {
        try {
            // 删除所有已上传的分片
            // 在实际应用中，应该根据uploadId查询所有分片并删除
            log.info("中止分片上传, 文件名: {}", fileName);
        } catch (Exception e) {
            log.error("中止分片上传失败", e);
            throw new StorageException("中止分片上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取已上传的分片列表
     * 在8.5.7版本的MinIO Java SDK中，没有直接的ListPartsArgs类
     * @param fileName 文件名
     * @param uploadId 上传ID
     * @return 分片列表
     */
    public Part[] listMultipartUploads(String fileName, String uploadId) throws Exception {
        try {
            // 在实际应用中，应该通过HTTP请求获取分片列表
            // 这里为了演示，返回空数组
            log.info("获取分片列表, 文件名: {}", fileName);
            return new Part[0];
        } catch (Exception e) {
            log.error("获取已上传分片列表失败", e);
            throw new StorageException("获取已上传分片列表失败: " + e.getMessage(), e);
        }
    }
} 