package com.huang.backend.service;

import com.huang.backend.payload.request.LoginRequest;
import com.huang.backend.payload.response.JwtResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
    String refreshToken(HttpServletRequest request, HttpServletResponse response);
    List<String> getPermissionCodes(String username);
} 