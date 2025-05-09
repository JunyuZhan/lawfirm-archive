package com.lawfirm.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String realName;
    private String phone;
    private String avatar;
    private String position;
    private String department;
    private Integer status;
    private OffsetDateTime lastLoginTime;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Set<RoleDto> roles;
} 