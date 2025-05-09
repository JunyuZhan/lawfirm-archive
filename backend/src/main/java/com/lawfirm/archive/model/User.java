package com.lawfirm.archive.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    // 用户真实姓名
    private String realName;

    // 手机号码
    private String phone;

    // 个人头像地址
    private String avatar;

    // 职位信息
    private String position;

    // 部门信息
    private String department;

    // 用户状态：0-禁用 1-启用
    private Integer status = 1;

    // 最近登录时间
    private OffsetDateTime lastLoginTime;

    // 创建时间
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    // 更新时间
    private OffsetDateTime updatedAt;

    // 用户与角色多对多关系
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // 判断用户是否有指定角色
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }
} 