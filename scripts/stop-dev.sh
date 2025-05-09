#!/bin/bash

# 获取脚本所在目录和项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "停止开发环境..."

# 停止后端进程
pkill -f "spring-boot:run" || true

# 停止前端进程
pkill -f "vue-vben-admin/node_modules/.bin/vite" || true

# 停止Docker容器
cd "$PROJECT_ROOT" && docker-compose down
 
echo "开发环境已停止" 