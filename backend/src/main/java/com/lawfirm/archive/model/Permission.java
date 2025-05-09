package com.lawfirm.archive.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // 权限简要描述
    private String description;

    // 权限分类：MENU-菜单权限，OPERATION-操作权限，API-接口权限
    @Column(nullable = false)
    private String type;

    // 权限对应的资源（如菜单ID、API路径等）
    private String resource;

    // 权限与角色多对多关系(由Role方维护)
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
} 