# 无人机管理系统

这是一个基于前后端分离架构的无人机管理系统，使用Docker Compose管理基础服务。

## 项目架构

### 前端
- 基于Vue的web-antd应用
- 位于`vue-vben-admin/apps/web-antd`
- 通过vite配置代理连接后端

### 后端
- Spring Boot应用
- 与PostgreSQL、InfluxDB和EMQX进行交互
- 启动时自动检查服务连接状态

### 基础服务（Docker容器）
- **PostgreSQL**: 存储关系型数据
- **InfluxDB**: 存储时序数据（如无人机传感器数据）
- **EMQX**: 消息队列（处理无人机实时通信）

## 开发环境设置

### 前提条件
- Docker 和 Docker Compose
- JDK 17
- Node.js 和 npm
- Maven

## 开发流程

### 1. 启动基础服务

```bash
docker-compose up -d postgres influxdb emqx
```

这将启动所有必要的基础服务，但不会启动后端应用容器。

### 2. 启动后端开发环境

后端可以通过两种方式启动：

**方式一：本地直接启动**
```bash
cd backend
./mvnw spring-boot:run
```

**方式二：使用Docker（开发模式）**
```bash
cd backend
docker build -f Dockerfile.dev -t drone-backend-dev .
docker run -p 8080:8080 --network="host" drone-backend-dev
```

后端启动后会自动检查与PostgreSQL、InfluxDB和EMQX的连接。

### 3. 启动前端开发环境

```bash
cd vue-vben-admin/apps/web-antd
npm install
npm run dev
```

前端配置了两个代理：
- `/api` - 连接到mock服务，处理登录等功能
- `/spring` - 连接到SpringBoot后端

## 开发工作流

### 前端开发
- 修改`vue-vben-admin/apps/web-antd/src`下的文件
- 对于需要后端的API，使用`/spring/`前缀
- 对于使用mock服务的功能，使用`/api/`前缀

### 后端开发
- 编写Controller、Service等业务逻辑
- 使用JPA访问PostgreSQL
- 使用InfluxDB客户端操作时序数据
- 使用MQTT客户端与EMQX交互

## 调试与监控

### 后端监控
- 健康检查：`http://localhost:8080/api/health`
- Spring Boot Actuator：`http://localhost:8080/actuator`

### 数据库访问
- PostgreSQL：`localhost:5432` (用户名:drone, 密码:dronepassword)
- InfluxDB管理界面：`http://localhost:8086` (用户名:admin, 密码:influxdb123)

### 消息队列
- EMQX控制面板：`http://localhost:18083` (用户名:admin, 密码:public)

## 项目结构

```
.
├── backend/                  # 后端Spring Boot应用
│   ├── src/                  # 源代码
│   ├── Dockerfile.dev        # 开发环境Dockerfile
│   └── pom.xml               # Maven配置
├── vue-vben-admin/           # 前端Vue应用
│   └── apps/
│       └── web-antd/         # 主前端应用
├── docker-compose.yml        # Docker Compose配置
└── README.md                 # 项目文档
```

## 技术栈

### 前端
- Vue.js
- Ant Design Vue
- Vite

### 后端
- Spring Boot 3.2.3
- Spring Data JPA
- PostgreSQL
- InfluxDB
- MQTT (EMQX)

### 基础设施
- Docker
- Docker Compose

## 健康检查

系统提供了健康检查功能，可以监控所有服务的连接状态：

```bash
curl http://localhost:8080/api/health
```

响应示例：
```json
{
  "status": "UP",
  "services": {
    "postgresql": {
      "service": "PostgreSQL",
      "status": "UP"
    },
    "influxdb": {
      "service": "InfluxDB",
      "status": "UP"
    },
    "emqx": {
      "service": "EMQX",
      "status": "UP"
    }
  }
}
```

## 故障排除

### 后端连接问题
- 检查Docker容器是否正常运行：`docker ps`
- 检查环境变量是否正确设置
- 查看后端日志：`docker logs drone-backend`

### 前端连接问题
- 检查vite.config.mts中的代理配置
- 确保后端服务正在运行
- 检查浏览器控制台是否有错误

## 贡献指南

1. Fork项目
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'Add some feature'`
4. 推送到分支：`git push origin feature/your-feature`
5. 提交Pull Request 