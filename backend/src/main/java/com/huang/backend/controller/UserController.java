package com.huang.backend.controller;

import com.huang.backend.payload.response.ApiResponse;
import com.huang.backend.payload.response.UserInfoResponse;
import com.huang.backend.security.service.UserDetailsImpl;
import com.huang.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResponse.success(userService.getUserInfo(userDetails.getUsername()));
    }
} 