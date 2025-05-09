# 无人机管理系统 - 开发环境

这是一套基于Web的无人机管理系统开发环境配置，用于在本地（Mac）环境一键部署与开发，面向毕业设计阶段的功能验证。

## 系统组件

- **前端**：基于Vue3/Vue-vben-admin的管理后台
- **后端**：Spring Boot提供REST和MQTT客户端服务
- **消息中间件**：EMQX Enterprise 5.8.6
- **数据库**：
  - PostgreSQL 16.0 (关系型数据库)
  - InfluxDB 2.7 (时序数据库)

## 开发环境设置

### 前提条件

- Docker & Docker Compose
- Java 17
- Node.js & npm
- pnpm
- Git

### 启动开发环境

1. 启动后台服务（PostgreSQL、InfluxDB、EMQX）：

```bash
./start-dev.sh
```

2. 开发后端（Spring Boot）：

```bash
cd backend
./mvnw spring-boot:run
```

3. 开发前端（Vue-vben-admin）：

```bash
cd vue-vben-admin
npm i -g corepack  # 如果未安装
pnpm install
pnpm dev:antd
```

### 停止开发环境

```bash
./stop-dev.sh
```

## 服务访问信息

- **PostgreSQL**: localhost:5432
  - 用户名: drone
  - 密码: dronepassword
  - 数据库: dronedb

- **InfluxDB**: http://localhost:8086
  - 用户名: admin
  - 密码: influxdb123
  - 组织: drone_org
  - Token: my-super-secret-token

- **EMQX控制台**: http://localhost:18083
  - 用户名: admin
  - 密码: public

- **后端API**: http://localhost:8080

- **前端开发服务器**: http://localhost:3100

## Git版本管理

### 初始设置

首次克隆后，使用以下命令初始化并推送到您的GitHub仓库：

```bash
./setup-git.sh
```

脚本会引导您完成仓库初始化和推送过程。

### 日常Git操作

1. 查看更改状态：
```bash
git status
```

2. 添加更改到暂存区：
```bash
git add .
```

3. 提交更改：
```bash
git commit -m "提交描述"
```

4. 推送到GitHub：
```bash
git push
```

5. 获取最新更改：
```bash
git pull
```

## 代码结构

```
drone9/
├── backend/               # Spring Boot后端服务
├── vue-vben-admin/        # Vue3前端项目
├── docker-compose.yml     # Docker服务配置
├── start-dev.sh           # 开发环境启动脚本
├── stop-dev.sh            # 开发环境停止脚本
└── setup-git.sh           # Git仓库初始化脚本
```

## 系统功能

- 无人机注册：通过MQTT+REST接口实现设备上线登记
- 实时遥测：无人机定时发布位置、速度、电量等数据
- 指令交互：平台向指定主题下发飞行或导航命令
- 地理展示：前端集成百度地图API，可视化展示无人机位置和轨迹
- 围栏管理：通过地理围栏限制飞行区域，触发预警或指令下达 