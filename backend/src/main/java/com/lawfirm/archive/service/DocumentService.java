package com.lawfirm.archive.service;

import com.lawfirm.archive.model.Document;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface DocumentService {
    // 基本操作
    List<Document> getDocumentsByCase(Long caseId);
    Document uploadDocument(MultipartFile file, Long caseId, String category, String remarks) throws Exception;
    byte[] downloadDocument(Long id) throws Exception;
    void deleteDocument(Long id) throws Exception;
    
    // 批量操作
    byte[] batchDownloadDocuments(List<Long> ids) throws Exception;
    void batchUpdateRemarks(List<Long> ids, String remarks);
    void batchUpdateCategory(List<Long> ids, String category);
    void batchDelete(List<Long> ids) throws Exception;
    
    // 文档属性更新
    Document updateSortOrder(Long id, Integer sortOrder);
    Document updateRemarks(Long id, String remarks);
    Document updateCategory(Long id, String category);
    
    // 文档预览
    byte[] previewDocument(Long id) throws Exception;
} 