#!/bin/bash

# 颜色设置
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}初始化无人机管理系统Git仓库...${NC}"

# 检查git是否安装
if ! command -v git &> /dev/null; then
    echo -e "${YELLOW}Git未安装。请先安装Git: https://git-scm.com/downloads${NC}"
    exit 1
fi

# 初始化git仓库
git init

# 添加文件到暂存区
git add .

# 初始提交
git commit -m "初始提交: 无人机管理系统开发环境配置"

# 请求GitHub仓库URL
echo -e "${YELLOW}请输入您的GitHub仓库URL (例如: https://github.com/username/drone-management.git):${NC}"
read github_url

# 添加远程仓库
git remote add origin $github_url

# 配置默认分支名为main
git branch -M main

# 推送到GitHub
echo -e "${GREEN}正在推送到GitHub...${NC}"
git push -u origin main

# 完成
echo -e "${GREEN}无人机管理系统已成功推送到GitHub!${NC}"
echo -e "${GREEN}仓库URL: ${YELLOW}$github_url${NC}"
echo ""
echo -e "${GREEN}Git使用提示:${NC}"
echo -e "1. 使用 ${YELLOW}git status${NC} 查看更改状态"
echo -e "2. 使用 ${YELLOW}git add .${NC} 添加更改到暂存区"
echo -e "3. 使用 ${YELLOW}git commit -m \"您的提交消息\"${NC} 提交更改"
echo -e "4. 使用 ${YELLOW}git push${NC} 推送更改到GitHub"
echo -e "5. 使用 ${YELLOW}git pull${NC} 从GitHub获取最新更改" 