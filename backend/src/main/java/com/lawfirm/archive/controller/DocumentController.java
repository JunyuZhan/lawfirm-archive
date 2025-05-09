package com.lawfirm.archive.controller;

import com.lawfirm.archive.exception.ResourceNotFoundException;
import com.lawfirm.archive.model.Document;
import com.lawfirm.archive.repository.DocumentRepository;
import com.lawfirm.archive.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    @GetMapping
    public List<Document> getDocuments(@RequestParam Long caseId) {
        return documentService.getDocumentsByCase(caseId);
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("caseId") Long caseId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "remarks", required = false) String remarks) throws Exception {
        log.info("上传文件: {}, 案件ID: {}", file.getOriginalFilename(), caseId);
        Document doc = documentService.uploadDocument(file, caseId, category, remarks);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) throws Exception {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        byte[] content = documentService.downloadDocument(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", doc.getFileName());
        
        log.info("下载文件: {}, ID: {}", doc.getFileName(), id);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @PostMapping("/batch-download")
    public ResponseEntity<byte[]> batchDownloadDocuments(@RequestBody Map<String, List<Long>> requestBody) throws Exception {
        List<Long> ids = requestBody.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("批量下载文件, 文件数量: {}", ids.size());
        byte[] zipContent = documentService.batchDownloadDocuments(ids);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "documents.zip");
        return new ResponseEntity<>(zipContent, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) throws Exception {
        log.info("删除文件, ID: {}", id);
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/sort")
    public ResponseEntity<Document> updateSortOrder(
            @PathVariable Long id,
            @RequestParam int sortOrder) {
        log.info("更新文件排序, ID: {}, 排序: {}", id, sortOrder);
        Document doc = documentService.updateSortOrder(id, sortOrder);
        return ResponseEntity.ok(doc);
    }

    @PutMapping("/{id}/remarks")
    public ResponseEntity<Document> updateRemarks(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        log.info("更新文件备注, ID: {}", id);
        Document doc = documentService.updateRemarks(id, payload.get("remarks"));
        return ResponseEntity.ok(doc);
    }

    @PutMapping("/batch-remarks")
    public ResponseEntity<Void> batchUpdateRemarks(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) payload.get("ids");
        String remarks = (String) payload.get("remarks");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("批量更新文件备注, 文件数量: {}", ids.size());
        documentService.batchUpdateRemarks(ids, remarks);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/category")
    public ResponseEntity<Document> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        log.info("更新文件分类, ID: {}", id);
        Document doc = documentService.updateCategory(id, payload.get("category"));
        return ResponseEntity.ok(doc);
    }

    @PutMapping("/batch-category")
    public ResponseEntity<Void> batchUpdateCategory(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) payload.get("ids");
        String category = (String) payload.get("category");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("批量更新文件分类, 文件数量: {}", ids.size());
        documentService.batchUpdateCategory(ids, category);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<Void> batchDeleteDocuments(@RequestBody Map<String, List<Long>> requestBody) throws Exception {
        List<Long> ids = requestBody.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("批量删除文件, 文件数量: {}", ids.size());
        documentService.batchDelete(ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewDocument(@PathVariable Long id) throws Exception {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        byte[] content = documentService.previewDocument(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(doc.getFileType()));
        
        log.info("预览文件: {}, ID: {}", doc.getFileName(), id);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
} 