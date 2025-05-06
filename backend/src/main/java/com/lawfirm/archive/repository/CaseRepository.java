package com.lawfirm.archive.repository;

import com.lawfirm.archive.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    // 可根据需要添加自定义查询方法
} 