package com.lawfirm.archive.service.impl;

import com.lawfirm.archive.dto.BatchResultDto;
import com.lawfirm.archive.dto.PermissionDto;
import com.lawfirm.archive.dto.RoleDto;
import com.lawfirm.archive.dto.UserDto;
import com.lawfirm.archive.exception.ResourceNotFoundException;
import com.lawfirm.archive.model.Role;
import com.lawfirm.archive.model.User;
import com.lawfirm.archive.repository.RoleRepository;
import com.lawfirm.archive.repository.UserRepository;
import com.lawfirm.archive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserDto> findAll(String keyword, Integer status, Long roleId, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 关键词搜索
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.or(
                    cb.like(root.get("username"), "%" + keyword + "%"),
                    cb.like(root.get("realName"), "%" + keyword + "%"),
                    cb.like(root.get("email"), "%" + keyword + "%")
                ));
            }
            
            // 状态过滤
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            // 角色过滤
            if (roleId != null) {
                Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
                predicates.add(cb.equal(roleJoin.get("id"), roleId));
            }
            
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return userRepository.findAll(spec, pageable)
                .map(this::convertToDto);
    }

    @Override
    public UserDto findById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("用户未找到: " + id));
    }

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setEmail(userDto.getEmail());
        user.setRealName(userDto.getRealName());
        user.setPhone(userDto.getPhone());
        user.setAvatar(userDto.getAvatar());
        user.setPosition(userDto.getPosition());
        user.setDepartment(userDto.getDepartment());
        user.setStatus(userDto.getStatus() != null ? userDto.getStatus() : 1);
        user.setCreatedAt(OffsetDateTime.now());
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        // 分配角色
        if (userDto.getRoles() != null && !userDto.getRoles().isEmpty()) {
            Set<Long> roleIds = userDto.getRoles().stream()
                    .map(RoleDto::getId)
                    .collect(Collectors.toSet());
            assignRoles(savedUser.getId(), roleIds);
        }
        
        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户未找到: " + id));
        
        user.setEmail(userDto.getEmail());
        user.setRealName(userDto.getRealName());
        user.setPhone(userDto.getPhone());
        user.setAvatar(userDto.getAvatar());
        user.setPosition(userDto.getPosition());
        user.setDepartment(userDto.getDepartment());
        user.setStatus(userDto.getStatus());
        user.setUpdatedAt(OffsetDateTime.now());
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        // 分配角色
        if (userDto.getRoles() != null) {
            Set<Long> roleIds = userDto.getRoles().stream()
                    .map(RoleDto::getId)
                    .collect(Collectors.toSet());
            assignRoles(savedUser.getId(), roleIds);
        }
        
        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户未找到: " + id));
        
        user.setStatus(status);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public BatchResultDto batchUpdateStatus(List<Long> userIds, Integer status) {
        List<Long> successIds = new ArrayList<>();
        List<Long> failIds = new ArrayList<>();
        
        for (Long userId : userIds) {
            try {
                // 跳过admin用户
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && !"admin".equals(user.getUsername())) {
                    updateStatus(userId, status);
                    successIds.add(userId);
                } else {
                    failIds.add(userId);
                }
            } catch (Exception e) {
                failIds.add(userId);
            }
        }
        
        return BatchResultDto.builder()
                .successCount(successIds.size())
                .failCount(failIds.size())
                .successIds(successIds.toString())
                .failIds(failIds.toString())
                .message(String.format("成功更新%d个用户状态，失败%d个", successIds.size(), failIds.size()))
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户未找到: " + id));
        
        // 生成8位随机密码
        String password = generateRandomPassword(8);
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        
        // TODO: 发送邮件通知用户新密码
    }

    @Override
    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户未找到: " + id));
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, Set<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户未找到: " + userId));
        
        Set<Role> roles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            roles = roleRepository.findAllById(roleIds)
                    .stream()
                    .collect(Collectors.toSet());
        }
        
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(this::convertToDto)
                .orElse(null);
    }
    
    @Override
    @Transactional
    public BatchResultDto batchImport(List<UserDto> users) {
        List<Long> successIds = new ArrayList<>();
        List<Long> failIds = new ArrayList<>();
        
        for (UserDto userDto : users) {
            try {
                // 验证必要字段
                if (!StringUtils.hasText(userDto.getUsername()) || 
                    !StringUtils.hasText(userDto.getPassword()) ||
                    !StringUtils.hasText(userDto.getEmail()) ||
                    !StringUtils.hasText(userDto.getRealName())) {
                    failIds.add(-1L); // 使用-1表示导入失败但没有ID的情况
                    continue;
                }
                
                // 检查用户名是否已存在
                if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
                    failIds.add(-1L);
                    continue;
                }
                
                UserDto createdUser = create(userDto);
                successIds.add(createdUser.getId());
            } catch (Exception e) {
                failIds.add(-1L);
            }
        }
        
        return BatchResultDto.builder()
                .successCount(successIds.size())
                .failCount(failIds.size())
                .successIds(successIds.toString())
                .failIds(failIds.toString())
                .message(String.format("成功导入%d个用户，失败%d个", successIds.size(), failIds.size()))
                .build();
    }
    
    @Override
    public List<UserDto> findAllForExport() {
        // 查询所有用户（不分页）
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // 辅助方法：将实体转换为DTO
    private UserDto convertToDto(User user) {
        if (user == null) {
            return null;
        }
        
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        // 不传输密码
        dto.setPassword(null);
        dto.setEmail(user.getEmail());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setPosition(user.getPosition());
        dto.setDepartment(user.getDepartment());
        dto.setStatus(user.getStatus());
        dto.setLastLoginTime(user.getLastLoginTime());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        // 转换角色
        if (user.getRoles() != null) {
            Set<RoleDto> roleDtos = user.getRoles().stream()
                    .map(this::convertToRoleDto)
                    .collect(Collectors.toSet());
            dto.setRoles(roleDtos);
        }
        
        return dto;
    }
    
    private RoleDto convertToRoleDto(Role role) {
        if (role == null) {
            return null;
        }
        
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        
        // 转换权限
        if (role.getPermissions() != null) {
            Set<PermissionDto> permissionDtos = role.getPermissions().stream()
                    .map(permission -> {
                        PermissionDto permissionDto = new PermissionDto();
                        permissionDto.setId(permission.getId());
                        permissionDto.setName(permission.getName());
                        permissionDto.setDescription(permission.getDescription());
                        permissionDto.setType(permission.getType());
                        permissionDto.setResource(permission.getResource());
                        return permissionDto;
                    })
                    .collect(Collectors.toSet());
            dto.setPermissions(permissionDtos);
        }
        
        return dto;
    }
    
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }
} 