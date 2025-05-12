package com.huang.backend.service.impl;

import com.huang.backend.model.Menu;
import com.huang.backend.repository.MenuRepository;
import com.huang.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;

    @Override
    public List<Menu> getAllMenusByUsername(String username) {
        return menuRepository.findAllMenusByUsername(username);
    }
} 