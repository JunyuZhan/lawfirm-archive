package com.lawfirm.archive.repository;

import com.lawfirm.archive.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByCaseEntityIdOrderBySortOrderAsc(Long caseId);
} 