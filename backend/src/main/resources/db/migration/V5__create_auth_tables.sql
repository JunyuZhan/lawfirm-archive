-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    avatar VARCHAR(255),
    position VARCHAR(50),
    department VARCHAR(50),
    status INTEGER DEFAULT 1,
    last_login_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    type VARCHAR(20) NOT NULL,
    resource VARCHAR(255)
);

-- 菜单表
CREATE TABLE IF NOT EXISTS menus (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    path VARCHAR(100),
    component VARCHAR(100),
    sort INTEGER DEFAULT 0,
    hidden BOOLEAN DEFAULT FALSE,
    parent_id BIGINT,
    permission VARCHAR(100)
);

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 初始化系统管理员角色
INSERT INTO roles (name, description) VALUES ('ADMIN', '系统管理员') ON CONFLICT DO NOTHING;
INSERT INTO roles (name, description) VALUES ('CASE_MANAGER', '案件主管') ON CONFLICT DO NOTHING;
INSERT INTO roles (name, description) VALUES ('LAWYER', '承办律师') ON CONFLICT DO NOTHING;
INSERT INTO roles (name, description) VALUES ('ARCHIVIST', '档案管理员') ON CONFLICT DO NOTHING;
INSERT INTO roles (name, description) VALUES ('GUEST', '访客') ON CONFLICT DO NOTHING;

-- 初始化系统管理员账号 (密码: admin123)
INSERT INTO users (username, password, email, real_name, status, created_at)
VALUES ('admin', '$2a$10$uMzJJsTgcqF.kCsGYPSAuez4ELNymM2InXw3A8VGjP8xvEVF4J4UW', 'admin@lawfirm.com', '系统管理员', 1, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- 关联系统管理员角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING; 