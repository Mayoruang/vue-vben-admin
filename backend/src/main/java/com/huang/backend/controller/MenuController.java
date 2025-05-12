package com.huang.backend.controller;

import com.huang.backend.model.Menu;
import com.huang.backend.payload.response.ApiResponse;
import com.huang.backend.security.service.UserDetailsImpl;
import com.huang.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/all")
    public ApiResponse<List<Menu>> getAllMenus(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResponse.success(menuService.getAllMenusByUsername(userDetails.getUsername()));
    }
} 