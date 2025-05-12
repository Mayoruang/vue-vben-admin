package com.huang.backend.service.impl;

import com.huang.backend.model.User;
import com.huang.backend.payload.request.LoginRequest;
import com.huang.backend.payload.response.JwtResponse;
import com.huang.backend.repository.PermissionCodeRepository;
import com.huang.backend.repository.UserRepository;
import com.huang.backend.security.jwt.JwtUtils;
import com.huang.backend.security.service.UserDetailsImpl;
import com.huang.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PermissionCodeRepository permissionCodeRepository;

    @Value("${application.security.jwt.refresh-token-cookie-name}")
    private String refreshTokenCookieName;

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${application.security.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    public JwtResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String accessToken = jwtUtils.generateAccessToken(User.builder().username(userDetails.getUsername()).build());
        String refreshToken = jwtUtils.generateRefreshToken(User.builder().username(userDetails.getUsername()).build());

        // 更新用户最近登录时间
        userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
        });

        // 设置 Refresh Token 到 Cookie
        setRefreshTokenCookie(response, refreshToken);

        return JwtResponse.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .realName(userDetails.getRealName())
                .homePath(userDetails.getHomePath())
                .accessToken(accessToken)
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        clearRefreshTokenCookie(response);
    }

    @Override
    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new RuntimeException("Refresh token is missing");
        }

        try {
            String username = jwtUtils.extractUsername(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = jwtUtils.generateAccessToken(user);
            String newRefreshToken = jwtUtils.generateRefreshToken(user);

            // 更新 Refresh Token Cookie
            setRefreshTokenCookie(response, newRefreshToken);

            return newAccessToken;
        } catch (Exception e) {
            clearRefreshTokenCookie(response);
            throw new RuntimeException("Invalid refresh token", e);
        }
    }

    @Override
    public List<String> getPermissionCodes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> roleCodes = permissionCodeRepository.findCodesByUser(user);
        List<String> directCodes = permissionCodeRepository.findDirectCodesByUser(user);

        List<String> allCodes = new ArrayList<>(roleCodes);
        allCodes.addAll(directCodes);

        return allCodes.stream().distinct().collect(Collectors.toList());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenCookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshTokenCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
} 