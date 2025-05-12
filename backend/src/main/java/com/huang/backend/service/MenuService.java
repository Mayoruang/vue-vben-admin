package com.huang.backend.service;

import com.huang.backend.model.Menu;

import java.util.List;

public interface MenuService {
    List<Menu> getAllMenusByUsername(String username);
} 