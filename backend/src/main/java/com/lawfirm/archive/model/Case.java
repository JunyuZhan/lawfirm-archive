package com.lawfirm.archive.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "cases")
public class Case {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 案号
    private String caseNumber;

    // 案由
    private String caseReason;

    // 当事人
    private String parties;

    // 代理人
    private String agents;

    // 立案日期
    private LocalDate filingDate;

    // 管辖法院
    private String court;

    // 审理程序
    private String procedure;

    // 案件状态
    private String status;

    // 案件标签
    @ManyToMany
    @JoinTable(
        name = "case_tags",
        joinColumns = @JoinColumn(name = "case_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private java.util.List<Tag> tags;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
} 