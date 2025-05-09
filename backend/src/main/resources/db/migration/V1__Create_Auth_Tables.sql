-- 用户表
CREATE TABLE t_user (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    home_path VARCHAR(50),
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE t_role (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE t_user_role (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- 权限码表
CREATE TABLE t_permission (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 角色权限关联表
CREATE TABLE t_role_permission (
    id SERIAL PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- 菜单表
CREATE TABLE t_menu (
    id SERIAL PRIMARY KEY,
    pid INT,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(100),
    component VARCHAR(100),
    redirect VARCHAR(100),
    auth_code VARCHAR(100),
    type VARCHAR(20) NOT NULL,
    icon VARCHAR(50),
    status INT NOT NULL DEFAULT 1,
    meta JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_menu_parent FOREIGN KEY (pid) REFERENCES t_menu(id) ON DELETE SET NULL
);

-- 角色菜单关联表
CREATE TABLE t_role_menu (
    id SERIAL PRIMARY KEY,
    role_id INT NOT NULL,
    menu_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES t_menu(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

-- 初始化超级管理员和基本角色
INSERT INTO t_role (name, code, description) VALUES 
('超级管理员', 'super', '系统超级管理员'),
('管理员', 'admin', '系统管理员'),
('普通用户', 'user', '普通用户');

-- 初始化基础用户(密码均为123456，实际存储为加密后的值)
INSERT INTO t_user (username, password, real_name, home_path) VALUES 
('vben', '$2a$10$ySG2lkvjFHY5O0./CPIE1OI8VJsuKYEzOYzqIa7AJR6sEgSzUFOAm', 'Vben', '/dashboard'),
('admin', '$2a$10$ySG2lkvjFHY5O0./CPIE1OI8VJsuKYEzOYzqIa7AJR6sEgSzUFOAm', 'Admin', '/workspace'),
('jack', '$2a$10$ySG2lkvjFHY5O0./CPIE1OI8VJsuKYEzOYzqIa7AJR6sEgSzUFOAm', 'Jack', '/analytics');

-- 用户角色关联
INSERT INTO t_user_role (user_id, role_id) VALUES 
(1, 1), -- vben -> super
(2, 2), -- admin -> admin
(3, 3); -- jack -> user

-- 初始化权限码
INSERT INTO t_permission (code, name, description) VALUES 
('AC_100100', '系统配置权限', '系统配置相关权限'),
('AC_100110', '用户管理权限', '用户管理相关权限'),
('AC_100120', '菜单管理权限', '菜单管理相关权限'),
('AC_100010', '仪表盘权限', '仪表盘相关权限'),
('AC_100020', '角色管理权限', '角色管理相关权限'),
('AC_100030', '权限配置权限', '权限配置相关权限'),
('AC_1000001', '普通功能权限1', '普通功能相关权限1'),
('AC_1000002', '普通功能权限2', '普通功能相关权限2');

-- 角色权限关联
INSERT INTO t_role_permission (role_id, permission_id) VALUES 
(1, 1), (1, 2), (1, 3), (1, 4), -- super拥有全部权限
(2, 4), (2, 5), (2, 6), -- admin拥有部分权限
(3, 7), (3, 8); -- user拥有部分权限 