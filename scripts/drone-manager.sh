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
    echo -e "  ${GREEN}start${NC}      仅启动数据库等基础服务"
    echo -e "  ${GREEN}stop${NC}       停止所有服务"
    echo -e "  ${GREEN}status${NC}     查看服务状态"
    echo -e "  ${GREEN}help${NC}       显示此帮助信息"
    echo -e "  ${GREEN}clean${NC}      清理临时文件"
    echo ""
    echo "注意: 前端和后端需要手动启动:"
    echo -e "  后端: ${YELLOW}cd backend && ./mvnw spring-boot:run${NC}"
    echo -e "  前端: ${YELLOW}cd vue-vben-admin && npm run dev${NC}"
    echo ""
}

# 启动基础服务
start_services() {
    echo -e "${GREEN}启动基础服务（PostgreSQL, InfluxDB, EMQX）...${NC}"
    cd "$PROJECT_ROOT" && docker-compose up -d postgres influxdb emqx
    
    echo "等待服务启动..."
    sleep 5
    
    echo -e "${GREEN}基础服务已启动.${NC}"
    echo ""
    echo -e "现在您可以手动启动后端和前端服务:"
    echo -e "后端: ${YELLOW}cd backend && ./mvnw spring-boot:run${NC}"
    echo -e "前端: ${YELLOW}cd vue-vben-admin && npm run dev${NC}"
}

# 停止所有服务
stop_all() {
    echo -e "${YELLOW}停止所有服务...${NC}"
    
    # 不需要主动停止后端和前端，因为它们是手动启动的
    # 只停止Docker服务
    cd "$PROJECT_ROOT" && docker-compose down
    
    echo -e "${YELLOW}基础服务已停止.${NC}"
    echo -e "请确保手动关闭您的后端和前端终端窗口"
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
        echo -e "${RED}后端未运行${NC} - 需要手动启动 (cd backend && ./mvnw spring-boot:run)"
    fi
    
    echo -e "\n${BLUE}前端服务状态:${NC}"
    if pgrep -f "vue-vben-admin/node_modules/.bin/vite" > /dev/null; then
        echo -e "${GREEN}前端正在运行${NC}"
    else
        echo -e "${RED}前端未运行${NC} - 需要手动启动 (cd vue-vben-admin && npm run dev)"
    fi
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
            start_services
            ;;
        stop)
            stop_all
            ;;
        status)
            check_status
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