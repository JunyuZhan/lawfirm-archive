package com.lawfirm.archive.controller;

import com.lawfirm.archive.model.Case;
import com.lawfirm.archive.repository.CaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cases")
public class CaseController {
    @Autowired
    private CaseRepository caseRepository;

    // 获取所有案件
    @GetMapping
    public List<Case> getAllCases() {
        return caseRepository.findAll();
    }

    // 新增案件
    @PostMapping
    public Case createCase(@RequestBody Case newCase) {
        return caseRepository.save(newCase);
    }

    // 获取单个案件
    @GetMapping("/{id}")
    public Optional<Case> getCaseById(@PathVariable Long id) {
        return caseRepository.findById(id);
    }

    // 更新案件
    @PutMapping("/{id}")
    public Case updateCase(@PathVariable Long id, @RequestBody Case updatedCase) {
        updatedCase.setId(id);
        return caseRepository.save(updatedCase);
    }

    // 删除案件
    @DeleteMapping("/{id}")
    public void deleteCase(@PathVariable Long id) {
        caseRepository.deleteById(id);
    }
} 