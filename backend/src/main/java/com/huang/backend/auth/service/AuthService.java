package com.huang.backend.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.backend.auth.dto.LoginRequest;
import com.huang.backend.auth.dto.LoginResponse;
import com.huang.backend.auth.dto.MenuDTO;
import com.huang.backend.auth.dto.UserInfo;
import com.huang.backend.auth.entity.Menu;
import com.huang.backend.auth.entity.User;
import com.huang.backend.auth.repository.MenuRepository;
import com.huang.backend.auth.repository.UserRepository;
import com.huang.backend.auth.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    
    public AuthService(AuthenticationManager authenticationManager, 
                      UserRepository userRepository,
                      MenuRepository menuRepository,
                      JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.menuRepository = menuRepository;
        this.jwtUtil = jwtUtil;
        this.objectMapper = new ObjectMapper();
    }
    
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);
        
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .token(token)
                .homePath(user.getHomePath())
                .build();
    }
    
    public UserInfo getUserInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toList());
        
        List<String> permissions = new ArrayList<>();
        user.getAuthorities().forEach(authority -> {
            String auth = authority.getAuthority();
            if (!auth.startsWith("ROLE_")) {
                permissions.add(auth);
            }
        });
        
        return UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .homePath(user.getHomePath())
                .roles(roles)
                .permissions(permissions)
                .build();
    }
    
    public List<MenuDTO> getUserMenus(String username) {
        List<Menu> menus = menuRepository.findUserMenus(username);
        return convertToMenuTree(menus);
    }
    
    private List<MenuDTO> convertToMenuTree(List<Menu> menus) {
        Map<Integer, MenuDTO> menuMap = new HashMap<>();
        List<MenuDTO> rootMenus = new ArrayList<>();
        
        // 第一遍：创建所有节点
        for (Menu menu : menus) {
            Map<String, Object> metaMap = null;
            
            try {
                if (menu.getMeta() != null && !menu.getMeta().isEmpty()) {
                    metaMap = objectMapper.readValue(menu.getMeta(), Map.class);
                }
            } catch (JsonProcessingException e) {
                // 处理异常情况，可能是日志记录或者使用默认值
                metaMap = new HashMap<>();
            }
            
            MenuDTO menuDTO = MenuDTO.builder()
                    .id(menu.getId())
                    .name(menu.getName())
                    .path(menu.getPath())
                    .component(menu.getComponent())
                    .redirect(menu.getRedirect())
                    .meta(metaMap)
                    .children(new ArrayList<>())
                    .build();
            
            menuMap.put(menu.getId(), menuDTO);
        }
        
        // 第二遍：构建树
        for (Menu menu : menus) {
            MenuDTO menuDTO = menuMap.get(menu.getId());
            if (menu.getParent() != null && menuMap.containsKey(menu.getParent().getId())) {
                MenuDTO parentDTO = menuMap.get(menu.getParent().getId());
                parentDTO.getChildren().add(menuDTO);
            } else {
                rootMenus.add(menuDTO);
            }
        }
        
        return rootMenus;
    }
} 