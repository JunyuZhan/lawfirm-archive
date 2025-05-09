package com.lawfirm.archive.service.impl;

import com.lawfirm.archive.exception.ResourceNotFoundException;
import com.lawfirm.archive.exception.StorageException;
import com.lawfirm.archive.model.Document;
import com.lawfirm.archive.model.Case;
import com.lawfirm.archive.repository.DocumentRepository;
import com.lawfirm.archive.repository.CaseRepository;
import com.lawfirm.archive.service.DocumentService;
import com.lawfirm.archive.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final CaseRepository caseRepository;
    private final MinioService minioService;

    @Override
    public List<Document> getDocumentsByCase(Long caseId) {
        return documentRepository.findByCaseEntityIdOrderBySortOrderAsc(caseId);
    }

    @Override
    @Transactional
    public Document uploadDocument(MultipartFile file, Long caseId, String category, String remarks) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));

        try {
            String storageName = minioService.uploadFile(file);
            
            Document doc = new Document();
            doc.setCaseId(caseId);
            doc.setCaseEntity(caseEntity);
            doc.setFileName(file.getOriginalFilename());
            doc.setStorageName(storageName);
            doc.setFileType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setUploadTime(ZonedDateTime.now());
            doc.setCategory(category);
            doc.setRemarks(remarks);
            doc.setSortOrder(0);
            doc.setVersion(1);
            doc.setUpdatedAt(ZonedDateTime.now());
            
            return documentRepository.save(doc);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new StorageException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadDocument(Long id) throws Exception {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        
        try (InputStream is = minioService.downloadFile(doc.getStorageName())) {
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new StorageException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) throws Exception {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        
        try {
            minioService.deleteFile(doc.getStorageName());
            documentRepository.delete(doc);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new StorageException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] batchDownloadDocuments(List<Long> ids) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);
        
        try {
            for (Long id : ids) {
                Document doc = documentRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
                        
                try (InputStream is = minioService.downloadFile(doc.getStorageName())) {
                    ZipEntry zipEntry = new ZipEntry(doc.getFileName());
                    zipOut.putNextEntry(zipEntry);
                    IOUtils.copy(is, zipOut);
                    zipOut.closeEntry();
                }
            }
            
            zipOut.close();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("批量下载文件失败", e);
            throw new StorageException("批量下载文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void batchUpdateRemarks(List<Long> ids, String remarks) {
        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.size() != ids.size()) {
            throw new ResourceNotFoundException("部分文档不存在");
        }
        
        ZonedDateTime now = ZonedDateTime.now();
        documents.forEach(doc -> {
            doc.setRemarks(remarks);
            doc.setUpdatedAt(now);
        });
        
        documentRepository.saveAll(documents);
    }

    @Override
    @Transactional
    public void batchUpdateCategory(List<Long> ids, String category) {
        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.size() != ids.size()) {
            throw new ResourceNotFoundException("部分文档不存在");
        }
        
        ZonedDateTime now = ZonedDateTime.now();
        documents.forEach(doc -> {
            doc.setCategory(category);
            doc.setUpdatedAt(now);
        });
        
        documentRepository.saveAll(documents);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) throws Exception {
        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.size() != ids.size()) {
            throw new ResourceNotFoundException("部分文档不存在");
        }
        
        StringBuilder errors = new StringBuilder();
        List<Document> successfullyDeletedDocs = new ArrayList<>();
        
        // 第一阶段：尝试删除所有MinIO中的文件
        for (Document doc : documents) {
            try {
                minioService.deleteFile(doc.getStorageName());
                successfullyDeletedDocs.add(doc);
            } catch (Exception e) {
                log.error("删除文件失败: {}", doc.getFileName(), e);
                errors.append(doc.getFileName()).append(": ").append(e.getMessage()).append("\n");
            }
        }
        
        // 如果有任何文件删除失败，回滚事务
        if (errors.length() > 0) {
            throw new StorageException("部分文件删除失败:\n" + errors.toString());
        }
        
        // 所有文件删除成功，现在删除数据库记录
        documentRepository.deleteAll(documents);
    }

    @Override
    @Transactional
    public Document updateSortOrder(Long id, Integer sortOrder) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
                
        doc.setSortOrder(sortOrder);
        doc.setUpdatedAt(ZonedDateTime.now());
        return documentRepository.save(doc);
    }

    @Override
    @Transactional
    public Document updateRemarks(Long id, String remarks) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
                
        doc.setRemarks(remarks);
        doc.setUpdatedAt(ZonedDateTime.now());
        return documentRepository.save(doc);
    }

    @Override
    @Transactional
    public Document updateCategory(Long id, String category) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
                
        doc.setCategory(category);
        doc.setUpdatedAt(ZonedDateTime.now());
        return documentRepository.save(doc);
    }

    @Override
    public byte[] previewDocument(Long id) throws Exception {
        // 预览文档，可以进行格式转换或其他处理以适应预览需求
        // 目前简单实现与下载相同
        return downloadDocument(id);
    }
} 