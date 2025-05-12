#!/bin/bash
# 生成随机序列号和当前时间戳
SERIAL="DRONE-$(date +%s)"
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")
echo "===== 注册新无人机 - $TIMESTAMP ====="
echo "序列号: $SERIAL"
JSON_DATA="{\"serialNumber\":\"$SERIAL\",\"model\":\"Script-Generated-Model\",\"notes\":\"这是通过脚本在 $TIMESTAMP 创建的测试无人机\"}"
echo "发送请求..."
curl -X POST http://localhost:8080/api/v1/drones/register -H "Content-Type: application/json" -d "$JSON_DATA"
echo -e "
注册请求已发送！"
