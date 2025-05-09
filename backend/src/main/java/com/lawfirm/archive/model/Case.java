package com.lawfirm.archive.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "cases")
public class Case {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number")
    private String caseNumber;
    
    private String agents;
    
    @Column(name = "case_reason")
    private String caseReason;
    
    private String court;
    
    @Column(name = "filing_date")
    private LocalDate filingDate;
    
    private String parties;
    
    private String procedure;
    
    private String status;
    
    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @OneToMany(mappedBy = "caseEntity")
    private List<Document> documents = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "case_tags",
        joinColumns = @JoinColumn(name = "case_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}