#!/bin/bash

# 无人机管理系统主脚本
# 作者: Mayor Huang
# 创建日期: 2025-05-09

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 颜色设置
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印标题
print_header() {
    echo -e "${BLUE}=========================================${NC}"
    echo -e "${BLUE}       无人机管理系统 - Drone9${NC}"
    echo -e "${BLUE}=========================================${NC}"
    echo ""
}

# 显示帮助信息
show_help() {
    echo -e "用法: ${GREEN}./$(basename $0) [命令]${NC}"
    echo ""
    echo "可用命令:"
    echo -e "  ${GREEN}start${NC}      启动整个开发环境"
    echo -e "  ${GREEN}stop${NC}       停止所有服务"
    echo -e "  ${GREEN}status${NC}     查看服务状态"
    echo -e "  ${GREEN}backend${NC}    仅启动后端服务"
    echo -e "  ${GREEN}frontend${NC}   仅启动前端服务"
    echo -e "  ${GREEN}db${NC}         仅启动数据库服务"
    echo -e "  ${GREEN}clean${NC}      清理临时文件"
    echo -e "  ${GREEN}help${NC}       显示此帮助信息"
    echo ""
}

# 启动全部服务
start_all() {
    echo -e "${GREEN}启动所有服务...${NC}"
    "$SCRIPT_DIR/start-dev.sh"
    echo -e "${GREEN}所有服务已启动.${NC}"
}

# 停止所有服务
stop_all() {
    echo -e "${YELLOW}停止所有服务...${NC}"
    "$SCRIPT_DIR/stop-dev.sh"
    echo -e "${YELLOW}所有服务已停止.${NC}"
}

# 查看服务状态
check_status() {
    echo -e "${BLUE}检查服务状态...${NC}"
    
    echo -e "${BLUE}Docker 容器状态:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E 'postgres|influxdb|emqx'
    
    echo -e "\n${BLUE}后端服务状态:${NC}"
    if pgrep -f "spring-boot:run" > /dev/null; then
        echo -e "${GREEN}后端正在运行${NC}"
    else
        echo -e "${RED}后端未运行${NC}"
    fi
    
    echo -e "\n${BLUE}前端服务状态:${NC}"
    if pgrep -f "vue-vben-admin/node_modules/.bin/vite" > /dev/null; then
        echo -e "${GREEN}前端正在运行${NC}"
    else
        echo -e "${RED}前端未运行${NC}"
    fi
}

# 仅启动后端
start_backend() {
    echo -e "${GREEN}启动后端服务...${NC}"
    cd "$PROJECT_ROOT/backend" && ./mvnw spring-boot:run
}

# 仅启动前端
start_frontend() {
    echo -e "${GREEN}启动前端服务...${NC}"
    cd "$PROJECT_ROOT/vue-vben-admin" && npm run dev
}

# 仅启动数据库服务
start_db() {
    echo -e "${GREEN}启动数据库服务...${NC}"
    cd "$PROJECT_ROOT" && docker-compose up -d postgres influxdb emqx
    echo -e "${GREEN}数据库服务已启动.${NC}"
}

# 清理临时文件
clean_temp() {
    echo -e "${YELLOW}清理临时文件...${NC}"
    
    # 清理后端临时文件
    find "$PROJECT_ROOT/backend" -name "*.log" -type f -delete
    
    # 清理前端临时文件
    rm -rf "$PROJECT_ROOT/vue-vben-admin/node_modules/.vite"
    
    echo -e "${GREEN}临时文件已清理.${NC}"
}

# 主函数
main() {
    print_header
    
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi
    
    case "$1" in
        start)
            start_all
            ;;
        stop)
            stop_all
            ;;
        status)
            check_status
            ;;
        backend)
            start_backend
            ;;
        frontend)
            start_frontend
            ;;
        db)
            start_db
            ;;
        clean)
            clean_temp
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            echo -e "${RED}未知命令: $1${NC}"
            show_help
            exit 1
            ;;
    esac
}

main "$@" 