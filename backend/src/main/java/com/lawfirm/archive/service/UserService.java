package com.lawfirm.archive.service;

import com.lawfirm.archive.dto.BatchResultDto;
import com.lawfirm.archive.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface UserService {
    /**
     * 分页查询用户列表
     */
    Page<UserDto> findAll(String keyword, Integer status, Long roleId, Pageable pageable);
    
    /**
     * 根据ID查询用户
     */
    UserDto findById(Long id);
    
    /**
     * 创建新用户
     */
    UserDto create(UserDto userDto);
    
    /**
     * 更新用户信息
     */
    UserDto update(Long id, UserDto userDto);
    
    /**
     * 删除用户
     */
    void delete(Long id);
    
    /**
     * 更新用户状态（启用/禁用）
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 批量更新用户状态
     */
    BatchResultDto batchUpdateStatus(List<Long> userIds, Integer status);
    
    /**
     * 重置用户密码
     */
    void resetPassword(Long id);
    
    /**
     * 修改用户密码
     */
    void changePassword(Long id, String oldPassword, String newPassword);
    
    /**
     * 为用户分配角色
     */
    void assignRoles(Long userId, Set<Long> roleIds);
    
    /**
     * 获取当前登录用户信息
     */
    UserDto getCurrentUser();
    
    /**
     * 批量导入用户
     */
    BatchResultDto batchImport(List<UserDto> users);
    
    /**
     * 获取所有用户（用于导出）
     */
    List<UserDto> findAllForExport();
} 