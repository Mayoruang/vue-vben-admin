#!/bin/bash

# 获取脚本所在目录和项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 启动Docker容器
echo "启动基础Docker服务..."
cd "$PROJECT_ROOT" && docker-compose up -d postgres influxdb emqx

echo "等待服务启动..."
sleep 10

echo "基础服务已启动，请手动启动前端和后端服务"
echo ""
echo "后端启动命令："
echo "cd backend && ./mvnw spring-boot:run"
echo ""
echo "前端启动命令："
echo "cd vue-vben-admin && npm install && npm run dev"
echo ""
echo "服务访问信息:"
echo "- PostgreSQL: localhost:5432 (用户名:drone, 密码:dronepassword, 数据库:dronedb)"
echo "- InfluxDB: http://localhost:8086 (用户名:admin, 密码:influxdb123, 组织:drone_org, Token:my-super-secret-token)"
echo "- EMQX控制台: http://localhost:18083 (用户名:admin, 密码:public)"
echo "- 后端API: http://localhost:8080 (需手动启动)"
echo "- 前端开发服务器: http://localhost:3100 (需手动启动)"
echo ""
echo "可以使用以下命令快速查看服务状态:"
echo "./drone9.sh status" 