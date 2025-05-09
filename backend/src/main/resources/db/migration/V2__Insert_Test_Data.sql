-- Insert roles
INSERT INTO t_role (name, code, description, status, created_at, updated_at)
VALUES 
('管理员', 'admin', '系统管理员', 1, NOW(), NOW()),
('操作员', 'operator', '无人机操作员', 1, NOW(), NOW()),
('访客', 'guest', '只读权限', 1, NOW(), NOW()),
('超级管理员', 'super', '超级管理员', 1, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Insert permissions
INSERT INTO t_permission (code, name, description, status, created_at, updated_at)
VALUES 
('system:admin', '系统管理', '系统管理权限', 1, NOW(), NOW()),
('drone:view', '查看无人机', '查看无人机信息权限', 1, NOW(), NOW()),
('drone:control', '控制无人机', '控制无人机操作权限', 1, NOW(), NOW()),
('data:view', '查看数据', '查看数据权限', 1, NOW(), NOW()),
('data:export', '导出数据', '导出数据权限', 1, NOW(), NOW()),
('AC_100010', '用户管理', '用户管理权限', 1, NOW(), NOW()),
('AC_100020', '角色管理', '角色管理权限', 1, NOW(), NOW()),
('AC_100030', '权限管理', '权限管理权限', 1, NOW(), NOW()),
('AC_100100', '无人机管理', '无人机管理权限', 1, NOW(), NOW()),
('AC_100110', '无人机监控', '无人机监控权限', 1, NOW(), NOW()),
('AC_100120', '无人机任务', '无人机任务权限', 1, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Link roles with permissions
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM t_role_permission rp
        JOIN t_role r ON rp.role_id = r.id
        JOIN t_permission p ON rp.permission_id = p.id
        WHERE r.code = 'admin' AND p.code = 'system:admin'
    ) THEN
        INSERT INTO t_role_permission (role_id, permission_id)
        SELECT r.id, p.id FROM t_role r, t_permission p
        WHERE r.code = 'admin' AND p.code = 'system:admin';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_role_permission rp
        JOIN t_role r ON rp.role_id = r.id
        JOIN t_permission p ON rp.permission_id = p.id
        WHERE r.code = 'admin' AND p.code IN ('drone:view', 'drone:control', 'data:view', 'data:export')
    ) THEN
        INSERT INTO t_role_permission (role_id, permission_id)
        SELECT r.id, p.id FROM t_role r, t_permission p
        WHERE r.code = 'admin' AND p.code IN ('drone:view', 'drone:control', 'data:view', 'data:export');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_role_permission rp
        JOIN t_role r ON rp.role_id = r.id
        JOIN t_permission p ON rp.permission_id = p.id
        WHERE r.code = 'operator' AND p.code IN ('drone:view', 'drone:control', 'data:view')
    ) THEN
        INSERT INTO t_role_permission (role_id, permission_id)
        SELECT r.id, p.id FROM t_role r, t_permission p
        WHERE r.code = 'operator' AND p.code IN ('drone:view', 'drone:control', 'data:view');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_role_permission rp
        JOIN t_role r ON rp.role_id = r.id
        JOIN t_permission p ON rp.permission_id = p.id
        WHERE r.code = 'guest' AND p.code IN ('drone:view', 'data:view')
    ) THEN
        INSERT INTO t_role_permission (role_id, permission_id)
        SELECT r.id, p.id FROM t_role r, t_permission p
        WHERE r.code = 'guest' AND p.code IN ('drone:view', 'data:view');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_role_permission rp
        JOIN t_role r ON rp.role_id = r.id
        JOIN t_permission p ON rp.permission_id = p.id
        WHERE r.code = 'super'
    ) THEN
        INSERT INTO t_role_permission (role_id, permission_id)
        SELECT r.id, p.id FROM t_role r, t_permission p
        WHERE r.code = 'super';
    END IF;
END $$;

-- Insert test user with hashed password ('123456')
INSERT INTO t_user (username, password, real_name, home_path, status, created_at, updated_at)
VALUES 
('admin', '$2a$10$4FJ697z9CvmYn0hlxR9zeOT46Oyc0/32mrOutav09P9YjUHrABD0i', '管理员', '/dashboard', 1, NOW(), NOW()),
('operator', '$2a$10$4FJ697z9CvmYn0hlxR9zeOT46Oyc0/32mrOutav09P9YjUHrABD0i', '操作员', '/dashboard', 1, NOW(), NOW()),
('guest', '$2a$10$4FJ697z9CvmYn0hlxR9zeOT46Oyc0/32mrOutav09P9YjUHrABD0i', '访客', '/dashboard', 1, NOW(), NOW()),
('vben', '$2a$10$4FJ697z9CvmYn0hlxR9zeOT46Oyc0/32mrOutav09P9YjUHrABD0i', 'Vben', '/dashboard', 1, NOW(), NOW())
ON CONFLICT (username) DO UPDATE 
SET password = EXCLUDED.password;

-- Link users with roles
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM t_user_role ur
        JOIN t_user u ON ur.user_id = u.id
        JOIN t_role r ON ur.role_id = r.id
        WHERE u.username = 'admin' AND r.code = 'admin'
    ) THEN
        INSERT INTO t_user_role (user_id, role_id)
        SELECT u.id, r.id FROM t_user u, t_role r
        WHERE u.username = 'admin' AND r.code = 'admin';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_user_role ur
        JOIN t_user u ON ur.user_id = u.id
        JOIN t_role r ON ur.role_id = r.id
        WHERE u.username = 'operator' AND r.code = 'operator'
    ) THEN
        INSERT INTO t_user_role (user_id, role_id)
        SELECT u.id, r.id FROM t_user u, t_role r
        WHERE u.username = 'operator' AND r.code = 'operator';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_user_role ur
        JOIN t_user u ON ur.user_id = u.id
        JOIN t_role r ON ur.role_id = r.id
        WHERE u.username = 'guest' AND r.code = 'guest'
    ) THEN
        INSERT INTO t_user_role (user_id, role_id)
        SELECT u.id, r.id FROM t_user u, t_role r
        WHERE u.username = 'guest' AND r.code = 'guest';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_user_role ur
        JOIN t_user u ON ur.user_id = u.id
        JOIN t_role r ON ur.role_id = r.id
        WHERE u.username = 'vben' AND r.code = 'admin'
    ) THEN
        INSERT INTO t_user_role (user_id, role_id)
        SELECT u.id, r.id FROM t_user u, t_role r
        WHERE u.username = 'vben' AND r.code = 'admin';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM t_user_role ur
        JOIN t_user u ON ur.user_id = u.id
        JOIN t_role r ON ur.role_id = r.id
        WHERE u.username = 'vben' AND r.code = 'super'
    ) THEN
        INSERT INTO t_user_role (user_id, role_id)
        SELECT u.id, r.id FROM t_user u, t_role r
        WHERE u.username = 'vben' AND r.code = 'super';
    END IF;
END $$;

-- Only insert menu items if they don't exist
DO $$
DECLARE
    dashboard_id INT;
    drone_id INT;
    system_id INT;
BEGIN
    -- Insert top-level menu items if they don't exist
    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = '/dashboard' AND name = 'Dashboard') THEN
        INSERT INTO t_menu (name, path, component, redirect, auth_code, type, icon, status, meta, created_at, updated_at)
        VALUES ('Dashboard', '/dashboard', 'LAYOUT', '/dashboard/analysis', 'dashboard', 'menu', 'ion:grid-outline', 1, 
                '{"title": "Dashboard", "icon": "ion:grid-outline", "hideChildrenInMenu": false}', NOW(), NOW())
        RETURNING id INTO dashboard_id;
    ELSE
        SELECT id INTO dashboard_id FROM t_menu WHERE path = '/dashboard' AND name = 'Dashboard' LIMIT 1;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = '/drone' AND name = 'Drone Management') THEN
        INSERT INTO t_menu (name, path, component, redirect, auth_code, type, icon, status, meta, created_at, updated_at)
        VALUES ('Drone Management', '/drone', 'LAYOUT', '/drone/list', 'drone', 'menu', 'ant-design:cloud-server-outlined', 1, 
                '{"title": "Drone Management", "icon": "ant-design:cloud-server-outlined"}', NOW(), NOW())
        RETURNING id INTO drone_id;
    ELSE
        SELECT id INTO drone_id FROM t_menu WHERE path = '/drone' AND name = 'Drone Management' LIMIT 1;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = '/system' AND name = 'System') THEN
        INSERT INTO t_menu (name, path, component, redirect, auth_code, type, icon, status, meta, created_at, updated_at)
        VALUES ('System', '/system', 'LAYOUT', '/system/user', 'system', 'menu', 'ion:settings-outline', 1, 
                '{"title": "System", "icon": "ion:settings-outline"}', NOW(), NOW())
        RETURNING id INTO system_id;
    ELSE
        SELECT id INTO system_id FROM t_menu WHERE path = '/system' AND name = 'System' LIMIT 1;
    END IF;

    -- Insert child menu items if they don't exist
    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = 'analysis' AND name = 'Analysis') THEN
        INSERT INTO t_menu (pid, name, path, component, auth_code, type, status, meta, created_at, updated_at)
        VALUES (dashboard_id, 'Analysis', 'analysis', '/dashboard/analysis/index', 'dashboard:analysis', 'menu', 1, 
                '{"title": "Analysis"}', NOW(), NOW());
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = 'workbench' AND name = 'Workbench') THEN
        INSERT INTO t_menu (pid, name, path, component, auth_code, type, status, meta, created_at, updated_at)
        VALUES (dashboard_id, 'Workbench', 'workbench', '/dashboard/workbench/index', 'dashboard:workbench', 'menu', 1, 
                '{"title": "Workbench"}', NOW(), NOW());
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = 'list' AND name = 'Drone List') THEN
        INSERT INTO t_menu (pid, name, path, component, auth_code, type, status, meta, created_at, updated_at)
        VALUES (drone_id, 'Drone List', 'list', '/drone/list/index', 'drone:list', 'menu', 1, 
                '{"title": "Drone List"}', NOW(), NOW());
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = 'monitor' AND name = 'Drone Monitor') THEN
        INSERT INTO t_menu (pid, name, path, component, auth_code, type, status, meta, created_at, updated_at)
        VALUES (drone_id, 'Drone Monitor', 'monitor', '/drone/monitor/index', 'drone:monitor', 'menu', 1, 
                '{"title": "Drone Monitor"}', NOW(), NOW());
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = 'user' AND name = 'User Management') THEN
        INSERT INTO t_menu (pid, name, path, component, auth_code, type, status, meta, created_at, updated_at)
        VALUES (system_id, 'User Management', 'user', '/system/user/index', 'system:user', 'menu', 1, 
                '{"title": "User Management"}', NOW(), NOW());
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = 'role' AND name = 'Role Management') THEN
        INSERT INTO t_menu (pid, name, path, component, auth_code, type, status, meta, created_at, updated_at)
        VALUES (system_id, 'Role Management', 'role', '/system/role/index', 'system:role', 'menu', 1, 
                '{"title": "Role Management"}', NOW(), NOW());
    END IF;

    IF NOT EXISTS (SELECT 1 FROM t_menu WHERE path = 'permission' AND name = 'Permission Management') THEN
        INSERT INTO t_menu (pid, name, path, component, auth_code, type, status, meta, created_at, updated_at)
        VALUES (system_id, 'Permission Management', 'permission', '/system/permission/index', 'system:permission', 'menu', 1, 
                '{"title": "Permission Management"}', NOW(), NOW());
    END IF;

    -- Grant menu access to roles
    INSERT INTO t_role_menu (role_id, menu_id)
    SELECT r.id, m.id FROM t_role r, t_menu m
    WHERE r.code = 'admin'
    ON CONFLICT DO NOTHING;

    INSERT INTO t_role_menu (role_id, menu_id)
    SELECT r.id, m.id FROM t_role r, t_menu m
    WHERE r.code = 'operator' AND m.path NOT IN ('/system')
    AND m.path NOT IN ('user', 'role', 'permission')
    ON CONFLICT DO NOTHING;

    INSERT INTO t_role_menu (role_id, menu_id)
    SELECT r.id, m.id FROM t_role r, t_menu m
    WHERE r.code = 'guest' AND m.path IN ('/dashboard', 'analysis', 'workbench', '/drone', 'list')
    ON CONFLICT DO NOTHING;
    
    INSERT INTO t_role_menu (role_id, menu_id)
    SELECT r.id, m.id FROM t_role r, t_menu m
    WHERE r.code = 'super'
    ON CONFLICT DO NOTHING;
END
$$; 