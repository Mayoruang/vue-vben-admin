package com.huang.backend.auth.controller;

import com.huang.backend.auth.dto.LoginRequest;
import com.huang.backend.auth.dto.LoginResponse;
import com.huang.backend.auth.dto.MenuDTO;
import com.huang.backend.auth.dto.UserInfo;
import com.huang.backend.auth.service.AuthService;
import com.huang.backend.auth.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    
    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("message", "登录成功");
            response.put("result", loginResponse);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "登录失败: " + e.getMessage());
            errorResponse.put("error", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(@RequestParam(required = false) String username) {
        if (username == null || username.isEmpty()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            username = authentication.getName();
        }
        
        UserInfo userInfo = authService.getUserInfo(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "获取用户信息成功");
        response.put("result", userInfo);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/getPermCode")
    public ResponseEntity<?> getPermCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        UserInfo userInfo = authService.getUserInfo(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "获取权限成功");
        response.put("result", userInfo.getPermissions());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/getMenuList")
    public ResponseEntity<?> getMenuList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        List<MenuDTO> menus = authService.getUserMenus(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "获取菜单成功");
        response.put("result", menus);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "登出成功");
        
        return ResponseEntity.ok(response);
    }
} 