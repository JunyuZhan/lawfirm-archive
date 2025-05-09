package com.lawfirm.archive.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "menus")
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 菜单名称
    @Column(nullable = false)
    private String name;

    // 菜单图标
    private String icon;

    // 菜单路径
    private String path;

    // 菜单组件
    private String component;

    // 菜单排序
    private Integer sort = 0;

    // 是否隐藏
    private Boolean hidden = false;

    // 父菜单ID，顶级菜单为null
    private Long parentId;

    // 权限标识 - 对应Permission中的name
    private String permission;

    // 子菜单列表，不存入数据库
    @Transient
    private List<Menu> children = new ArrayList<>();
}