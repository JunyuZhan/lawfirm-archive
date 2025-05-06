package com.lawfirm.archive.controller;

import com.lawfirm.archive.model.LoginRequest;
import com.lawfirm.archive.model.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 简单示例：仅做用户名密码验证，实际项目应增加加密和安全措施
        // 固定admin/admin登录凭证用于演示
        if ("admin".equals(request.getUsername()) && "admin".equals(request.getPassword())) {
            // 返回模拟的JWT令牌
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwibmFtZSI6ImFkbWluIiwiaWF0IjoxNTE2MjM5MDIyfQ";
            return ResponseEntity.ok(new LoginResponse(token));
        }
        
        // 用户名或密码错误
        return ResponseEntity.status(401).build();
    }
} 