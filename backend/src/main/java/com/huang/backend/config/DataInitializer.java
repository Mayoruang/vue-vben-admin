package com.huang.backend.config;

import com.huang.backend.model.*;
import com.huang.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionCodeRepository permissionCodeRepository;
    private final MenuRepository menuRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("数据库已经初始化，跳过初始化步骤");
            return;
        }
        
        log.info("开始初始化数据...");
        
        // 初始化角色
        Role superRole = createRole("super", "超级管理员");
        Role adminRole = createRole("admin", "管理员");
        Role userRole = createRole("user", "普通用户");
        
        // 初始化权限码
        PermissionCode ac100100 = createPermissionCode("AC_100100", "Super权限码1");
        PermissionCode ac100110 = createPermissionCode("AC_100110", "Super权限码2");
        PermissionCode ac100120 = createPermissionCode("AC_100120", "Super权限码3");
        PermissionCode ac100010 = createPermissionCode("AC_100010", "管理员权限码1");
        PermissionCode ac100020 = createPermissionCode("AC_100020", "管理员权限码2");
        PermissionCode ac100030 = createPermissionCode("AC_100030", "管理员权限码3");
        PermissionCode ac1000001 = createPermissionCode("AC_1000001", "普通用户权限码1");
        PermissionCode ac1000002 = createPermissionCode("AC_1000002", "普通用户权限码2");
        
        // 给角色分配权限码
        superRole.getPermissionCodes().addAll(Set.of(ac100100, ac100110, ac100120, ac100010));
        adminRole.getPermissionCodes().addAll(Set.of(ac100010, ac100020, ac100030));
        userRole.getPermissionCodes().addAll(Set.of(ac1000001, ac1000002));
        
        roleRepository.saveAll(List.of(superRole, adminRole, userRole));
        
        // 初始化用户
        User vbenUser = createUser("vben", "123456", "Vben", null, superRole);
        User adminUser = createUser("admin", "123456", "Admin", "/workspace", adminRole);
        User jackUser = createUser("jack", "123456", "Jack", "/analytics", userRole);
        
        userRepository.saveAll(List.of(vbenUser, adminUser, jackUser));
        
        // 初始化菜单
        initializeMenus(vbenUser, adminUser, jackUser, superRole, adminRole, userRole);
        
        log.info("数据初始化完成");
    }
    
    private Role createRole(String code, String name) {
        return roleRepository.save(Role.builder()
                .roleCode(code)
                .roleName(name)
                .description(name + "角色")
                .status(1)
                .permissionCodes(new HashSet<>())
                .menus(new HashSet<>())
                .build());
    }
    
    private PermissionCode createPermissionCode(String code, String description) {
        return permissionCodeRepository.save(PermissionCode.builder()
                .code(code)
                .description(description)
                .build());
    }
    
    private User createUser(String username, String password, String realName, String homePath, Role role) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .realName(realName)
                .homePath(homePath)
                .status(1)
                .roles(new HashSet<>(Collections.singletonList(role)))
                .build();
        return userRepository.save(user);
    }
    
    private void initializeMenus(User vbenUser, User adminUser, User jackUser, 
                                 Role superRole, Role adminRole, Role userRole) {
        // Dashboard菜单
        Menu dashboardMenu = createMenu(null, "catalog", "Dashboard", "/dashboard", null, "/analytics", null, 1, -1);
        
        Menu analyticsMenu = createMenu(dashboardMenu.getId(), "menu", "Analytics", "/analytics", 
                "/dashboard/analytics/index", null, null, 1, 0);
        createMenuMeta(analyticsMenu, "page.dashboard.analytics", "carbon:analytics", true, 0);
        
        Menu workspaceMenu = createMenu(dashboardMenu.getId(), "menu", "Workspace", "/workspace", 
                "/dashboard/workspace/index", null, null, 1, 1);
        createMenuMeta(workspaceMenu, "page.dashboard.workspace", "carbon:workspace", false, 1);
        
        // Demos菜单
        Menu demosMenu = createMenu(null, "catalog", "Demos", "/demos", null, "/demos/access", null, 1, 1000);
        createMenuMeta(demosMenu, "demos.title", "ic:baseline-view-in-ar", true, 1000);
        
        Menu accessDemosMenu = createMenu(demosMenu.getId(), "catalog", "AccessDemos", "/demosaccess", null, 
                "/demos/access/page-control", null, 1, 0);
        createMenuMeta(accessDemosMenu, "demos.access.backendPermissions", "mdi:cloud-key-outline", false, 0);
        
        Menu pageControlMenu = createMenu(accessDemosMenu.getId(), "menu", "AccessPageControlDemo", 
                "/demos/access/page-control", "/demos/access/index", null, null, 1, 0);
        createMenuMeta(pageControlMenu, "demos.access.pageAccess", "mdi:page-previous-outline", false, 0);
        
        Menu buttonControlMenu = createMenu(accessDemosMenu.getId(), "menu", "AccessButtonControlDemo", 
                "/demos/access/button-control", "/demos/access/button-control", null, null, 1, 1);
        createMenuMeta(buttonControlMenu, "demos.access.buttonControl", "mdi:button-cursor", false, 1);
        
        Menu menuVisible403Menu = createMenu(accessDemosMenu.getId(), "menu", "AccessMenuVisible403Demo", 
                "/demos/access/menu-visible-403", "/demos/access/menu-visible-403", null, null, 1, 2);
        MenuMeta menuVisible403Meta = createMenuMeta(menuVisible403Menu, "demos.access.menuVisible403", "mdi:button-cursor", false, 2);
        menuVisible403Meta.setAuthority("[\"no-body\"]");
        menuVisible403Meta.setMenuVisibleWithForbidden(true);
        
        // 角色特定的菜单
        Menu superVisibleMenu = createMenu(accessDemosMenu.getId(), "menu", "AccessSuperVisibleDemo", 
                "/demos/access/super-visible", "/demos/access/super-visible", null, null, 1, 3);
        createMenuMeta(superVisibleMenu, "demos.access.superVisible", "mdi:button-cursor", false, 3);
        
        Menu adminVisibleMenu = createMenu(accessDemosMenu.getId(), "menu", "AccessAdminVisibleDemo", 
                "/demos/access/admin-visible", "/demos/access/admin-visible", null, null, 1, 3);
        createMenuMeta(adminVisibleMenu, "demos.access.adminVisible", "mdi:button-cursor", false, 3);
        
        Menu userVisibleMenu = createMenu(accessDemosMenu.getId(), "menu", "AccessUserVisibleDemo", 
                "/demos/access/user-visible", "/demos/access/user-visible", null, null, 1, 3);
        createMenuMeta(userVisibleMenu, "demos.access.userVisible", "mdi:button-cursor", false, 3);
        
        // 为角色分配菜单
        superRole.getMenus().addAll(Set.of(dashboardMenu, analyticsMenu, workspaceMenu, demosMenu, 
                accessDemosMenu, pageControlMenu, buttonControlMenu, menuVisible403Menu, superVisibleMenu));
        
        adminRole.getMenus().addAll(Set.of(dashboardMenu, analyticsMenu, workspaceMenu, demosMenu, 
                accessDemosMenu, pageControlMenu, buttonControlMenu, menuVisible403Menu, adminVisibleMenu));
        
        userRole.getMenus().addAll(Set.of(dashboardMenu, analyticsMenu, workspaceMenu, demosMenu, 
                accessDemosMenu, pageControlMenu, buttonControlMenu, menuVisible403Menu, userVisibleMenu));
        
        roleRepository.saveAll(List.of(superRole, adminRole, userRole));
    }
    
    private Menu createMenu(Long parentId, String type, String name, String path, 
                           String component, String redirect, String authCode, Integer status, Integer order) {
        Menu menu = Menu.builder()
                .parentId(parentId != null ? parentId : 0L)
                .type(type)
                .name(name)
                .path(path)
                .component(component)
                .redirect(redirect)
                .authCode(authCode)
                .status(status)
                .build();
        
        return menuRepository.save(menu);
    }
    
    private MenuMeta createMenuMeta(Menu menu, String title, String icon, Boolean affixTab, Integer orderNum) {
        MenuMeta meta = MenuMeta.builder()
                .menu(menu)
                .title(title)
                .icon(icon)
                .affixTab(affixTab)
                .keepAlive(true)
                .hidden(false)
                .orderNum(orderNum)
                .build();
        
        menu.setMeta(meta);
        menuRepository.save(menu);
        return meta;
    }
} 