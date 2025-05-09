#!/bin/bash

# 启动Docker容器
echo "启动Docker服务..."
docker-compose up -d postgres influxdb emqx

echo "等待服务启动..."
sleep 10

echo "数据库服务已启动，可以在本地开发环境中运行后端和前端项目"
echo ""
echo "后端开发说明："
echo "1. 进入backend目录: cd backend"
echo "2. 运行Spring Boot: ./mvnw spring-boot:run"
echo ""
echo "前端开发说明："
echo "1. 进入vue-vben-admin目录: cd vue-vben-admin"
echo "2. 安装依赖: npm i -g corepack && pnpm install"
echo "3. 启动开发服务器: pnpm dev:antd"
echo ""
echo "服务访问信息:"
echo "- PostgreSQL: localhost:5432 (用户名:drone, 密码:dronepassword, 数据库:dronedb)"
echo "- InfluxDB: http://localhost:8086 (用户名:admin, 密码:influxdb123, 组织:drone_org, Token:my-super-secret-token)"
echo "- EMQX控制台: http://localhost:18083 (用户名:admin, 密码:public)"
echo "- 后端API: http://localhost:8080"
echo "- 前端开发服务器: http://localhost:3100" 