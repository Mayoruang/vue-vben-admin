# 无人机模拟工具

## 简介

`sim_drone.py` 是一个用于模拟无人机行为的Python脚本。它可以模拟无人机执行以下操作：

1. 向后端API注册自身
2. 轮询注册状态直到被批准
3. 使用获取的MQTT凭证连接到MQTT代理
4. 发送模拟的遥测数据
5. 接收并响应命令

## 依赖

脚本需要以下Python库：

```bash
pip install requests paho-mqtt
```

## 使用方法

### 基本用法

```bash
# 使用默认设置运行模拟无人机
./sim_drone.py

# 启用调试日志
./sim_drone.py --debug
```

### 自定义选项

```bash
# 指定无人机序列号和型号
./sim_drone.py --serial DRONE-001 --model "SuperDrone X5"

# 设置API URL
./sim_drone.py --api-url "http://localhost:8080/api/v1"

# 自定义初始位置
./sim_drone.py --lat 22.543099 --lon 114.057868 --alt 10.0

# 自定义轮询和遥测间隔
./sim_drone.py --poll-interval 3 --telemetry-interval 0.5
```

### 帮助信息

```bash
./sim_drone.py --help
```

## 参数说明

| 参数 | 描述 | 默认值 |
|-----|------|-------|
| `--serial` | 无人机序列号 | 随机生成的序列号 |
| `--model` | 无人机型号 | Simulator-X1 |
| `--api-url` | API基础URL | http://localhost:8080/api/v1 |
| `--poll-interval` | 注册状态轮询间隔（秒） | 5.0 |
| `--telemetry-interval` | 遥测数据发送间隔（秒） | 1.0 |
| `--lat` | 初始纬度 | 22.543099 |
| `--lon` | 初始经度 | 114.057868 |
| `--alt` | 初始高度（米） | 10.0 |
| `--debug` | 启用调试日志 | 关闭 |

## 工作流程

1. 脚本向后端API发送注册请求
2. 定期轮询获取注册状态
3. 当状态为"APPROVED"时，获取MQTT凭证
4. 使用凭证连接到MQTT代理
5. 开始周期性发送遥测数据
6. 监听命令主题并响应接收到的命令

## 命令处理

脚本可以处理以下命令类型：

- `TAKEOFF` - 模拟起飞到指定高度
- `LAND` - 模拟降落
- `RTL` - 模拟返航
- `GOTO` - 模拟飞行到指定坐标
- `ARM` - 模拟解锁电机
- `DISARM` - 模拟上锁电机

## 遥测数据

脚本发送的遥测数据包含以下字段：

- 无人机ID
- 时间戳
- 电池电量和电压
- 位置（经纬度、高度）
- 速度和航向
- 卫星数量
- 信号强度
- 飞行模式
- 温度 