#!/bin/bash

# 无人机管理系统启动器
# 此脚本仅作为主管理脚本的简单入口点

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 调用主管理脚本
exec "$SCRIPT_DIR/scripts/drone-manager.sh" "$@" 