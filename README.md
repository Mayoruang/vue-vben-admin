# 无人机管理系统 (Drone Management System)

一个功能全面的无人机管理系统，支持无人机监控、数据收集和分析。

## 系统架构

系统由以下组件构成：

1. **后端服务**：基于Spring Boot的REST API
2. **前端界面**：基于Vue-vben-admin的管理控制台
3. **数据存储**：
   - PostgreSQL：关系型数据（用户、权限、无人机信息等）
   - InfluxDB：时序数据（无人机遥测数据）
4. **消息通信**：
   - EMQX：MQTT消息代理，用于与无人机通信

## 快速开始

### 前置条件

- Docker 和 Docker Compose
- Java 17 或更高版本
- Node.js 16 或更高版本
- Maven 3.8 或更高版本

### 环境启动

方式一：使用管理脚本（推荐）

```bash
# 给脚本添加执行权限
chmod +x drone9.sh

# 启动所有服务
./drone9.sh start

# 查看服务状态
./drone9.sh status

# 停止所有服务
./drone9.sh stop

# 查看所有可用命令
./drone9.sh help
```

方式二：手动启动各组件

1. 启动所有基础服务：

```bash
docker-compose up -d
```

2. 启动后端服务：

```bash
cd backend
./mvnw spring-boot:run
```

3. 启动前端服务：

```bash
cd vue-vben-admin
npm install
npm run dev
```

### 默认账号

系统预设了以下测试账号：

| 用户名 | 密码 | 角色 |
|-------|------|------|
| admin | 123456 | 管理员 |
| operator | 123456 | 操作员 |
| guest | 123456 | 访客 |
| vben | 123456 | 超级管理员 |

## 主要功能

### 用户管理
- 用户认证与授权
- 基于角色的权限控制
- JWT令牌认证

### 无人机管理
- 无人机信息管理
- 实时监控无人机状态
- 无人机操作控制

### 数据分析
- 遥测数据收集
- 数据可视化
- 数据导出

## 项目结构

```
drone9/
├── backend/                # Spring Boot 后端
│   ├── src/                # 源代码
│   │   ├── main/java/com/huang/backend/
│   │   │   ├── auth/       # 认证模块
│   │   │   ├── config/     # 配置类
│   │   │   ├── debug/      # 调试工具
│   │   │   └── ...
│   │   └── resources/
│   │       ├── db/migration/ # Flyway 数据库迁移脚本
│   │       └── ...
│   └── ...
├── vue-vben-admin/        # Vue 前端
├── db/
│   └── scripts/           # 数据库脚本
├── docs/                  # 项目文档
├── scripts/               # 脚本文件
│   ├── drone-manager.sh   # 主管理脚本
│   ├── start-dev.sh       # 启动开发环境
│   └── ...
├── docker-compose.yml     # Docker 服务配置
└── drone9.sh              # 主入口脚本
```

## 开发指南

### 后端开发

后端基于Spring Boot框架开发，主要包含以下模块：

- `auth`: 认证与授权
- `drone`: 无人机管理
- `telemetry`: 遥测数据处理
- `analysis`: 数据分析

### 前端开发

前端基于Vue-vben-admin，主要包含以下模块：

- 认证管理
- 无人机管理界面
- 数据可视化界面
- 系统管理

## 系统架构图

```
+---------------+      +-------------+
|               |      |             |
|   Frontend    |<---->|   Backend   |
|  (Vue Admin)  |      | (Spring Boot)|
|               |      |             |
+---------------+      +------+------+
                              |
                              |
              +---------------+----------------+
              |               |                |
     +--------v-----+  +------v-------+  +-----v------+
     |              |  |              |  |            |
     |  PostgreSQL  |  |   InfluxDB   |  |    EMQX    |
     |  (Relational)|  | (Time-Series)|  |   (MQTT)   |
     |              |  |              |  |            |
     +--------------+  +--------------+  +------------+
```

## API文档

系统API文档可通过以下方式访问：

- Swagger UI: http://localhost:8080/swagger-ui.html

## 健康检查

系统集成了全面的健康检查机制，确保各组件正常工作：

- 后端健康检查: http://localhost:8080/management/health
- 数据库连接测试
- MQTT连接测试
- InfluxDB连接测试

## 故障排除

### 后端启动失败

1. 检查数据库连接是否正确
2. 检查端口是否被占用
3. 查看日志文件: `backend/logs/drone-management.log`
4. 使用 `./drone9.sh status` 检查服务状态

### 前端启动失败

1. 检查Node.js版本
2. 清除依赖并重新安装: `rm -rf node_modules && npm install`
3. 检查配置文件中的API路径

## 文档

更多文档请查看 `docs/` 目录：

- `auth-implementation-summary.md`: 认证系统实现总结
- `backend-fix-summary.md`: 后端修复总结
- `backend-startup-fix-summary.md`: 后端启动问题修复总结 