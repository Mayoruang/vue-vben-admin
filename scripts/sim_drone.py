#!/usr/bin/env python3
"""
Drone Simulator Script 

This script simulates a drone by:
1. Registering with the backend API
2. Polling for registration status until approved
3. Connecting to MQTT broker with obtained credentials
4. Publishing telemetry data periodically
"""

import argparse
import json
import logging
import random
import sys
import time
import uuid
from datetime import datetime
from typing import Dict, Optional, Any, Tuple

import paho.mqtt.client as mqtt
import requests

# 设置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("sim_drone")

# 默认配置
DEFAULT_API_BASE_URL = "http://localhost:8080/api/v1"
DEFAULT_POLL_INTERVAL_SEC = 5
DEFAULT_TELEMETRY_INTERVAL_SEC = 1.0
DEFAULT_INITIAL_LATITUDE = 22.543099
DEFAULT_INITIAL_LONGITUDE = 114.057868
DEFAULT_INITIAL_ALTITUDE = 10.0


class DroneSimulator:
    """模拟无人机类，处理API通信和MQTT消息发布"""

    def __init__(
            self,
            drone_serial: str,
            drone_model: str,
            api_base_url: str = DEFAULT_API_BASE_URL,
            poll_interval: float = DEFAULT_POLL_INTERVAL_SEC,
            telemetry_interval: float = DEFAULT_TELEMETRY_INTERVAL_SEC,
            initial_position: Tuple[float, float, float] = (
                DEFAULT_INITIAL_LATITUDE, DEFAULT_INITIAL_LONGITUDE, DEFAULT_INITIAL_ALTITUDE
            )
    ):
        """初始化模拟无人机
        
        Args:
            drone_serial: 无人机序列号
            drone_model: 无人机型号
            api_base_url: API基础URL
            poll_interval: 轮询注册状态的间隔（秒）
            telemetry_interval: 发送遥测数据的间隔（秒）
            initial_position: 初始位置(纬度, 经度, 高度)
        """
        self.drone_serial = drone_serial
        self.drone_model = drone_model
        self.api_base_url = api_base_url
        self.poll_interval = poll_interval
        self.telemetry_interval = telemetry_interval
        
        # 内部状态
        self.request_id = None
        self.status_url = None
        self.mqtt_credentials = None
        self.mqtt_client = None
        self.drone_id = None
        self.running = False
        
        # 模拟的位置和状态
        self.latitude, self.longitude, self.altitude = initial_position
        self.heading = random.uniform(0, 359)
        self.speed = 0.0
        self.battery_level = 100.0
        self.battery_voltage = 12.6
        self.satellites = random.randint(8, 16)
        self.signal_strength = random.uniform(80, 100)
        self.temperature = random.uniform(25, 30)
        self.flight_mode = "HOVER"

    def register(self) -> bool:
        """向后端API注册无人机
        
        Returns:
            bool: 注册是否成功
        """
        logger.info(f"开始注册无人机 {self.drone_serial} ({self.drone_model})")
        
        registration_url = f"{self.api_base_url}/drones/register"
        payload = {
            "serialNumber": self.drone_serial,
            "model": self.drone_model,
            "notes": f"模拟无人机，由 sim_drone.py 创建于 {datetime.now().isoformat()}"
        }
        
        try:
            response = requests.post(registration_url, json=payload)
            if response.status_code == 202:  # Accepted
                data = response.json()
                self.request_id = data.get("requestId")
                self.status_url = data.get("statusCheckUrl")
                if not self.status_url:
                    # 如果API没有返回完整URL，构造一个
                    self.status_url = f"{self.api_base_url}/drones/registration/{self.request_id}/status"
                
                logger.info(f"注册请求已提交，请求ID: {self.request_id}")
                logger.info(f"状态检查URL: {self.status_url}")
                return True
            else:
                logger.error(f"注册失败: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            logger.error(f"注册过程中发生错误: {str(e)}")
            return False
    
    def poll_registration_status(self) -> bool:
        """轮询注册状态直到获得批准或拒绝
        
        Returns:
            bool: 如果注册获得批准则返回True，否则返回False
        """
        if not self.status_url:
            logger.error("无法检查状态: 缺少状态URL")
            return False
        
        logger.info(f"开始轮询注册状态，间隔为 {self.poll_interval} 秒...")
        
        while True:
            try:
                response = requests.get(self.status_url)
                if response.status_code == 200:
                    data = response.json()
                    status = data.get("status")
                    message = data.get("message", "")
                    
                    logger.info(f"当前注册状态: {status} - {message}")
                    
                    if status == "APPROVED":
                        # 保存重要信息
                        self.drone_id = data.get("droneId")
                        self.mqtt_credentials = data.get("mqttCredentials")
                        logger.info(f"注册已批准！无人机ID: {self.drone_id}")
                        return True
                    
                    elif status == "REJECTED":
                        logger.error(f"注册被拒绝: {message}")
                        return False
                    
                    # 如果状态是PENDING，继续轮询
                    elif status == "PENDING":
                        logger.info("注册正在审核中，继续等待...")
                    
                    else:
                        logger.warning(f"收到未知状态: {status}")
                
                else:
                    logger.error(f"检查状态时出错: {response.status_code} - {response.text}")
            
            except Exception as e:
                logger.error(f"轮询状态时发生错误: {str(e)}")
            
            # 等待下一次轮询
            time.sleep(self.poll_interval)
    
    def on_connect(self, client, userdata, flags, rc):
        """MQTT连接回调函数"""
        if rc == 0:
            logger.info(f"成功连接到MQTT代理: {self.mqtt_credentials.get('mqttBrokerUrl')}")
            # 订阅命令主题
            commands_topic = self.mqtt_credentials.get("mqttTopicCommands")
            if commands_topic:
                client.subscribe(commands_topic)
                logger.info(f"已订阅命令主题: {commands_topic}")
        else:
            logger.error(f"MQTT连接失败，返回码: {rc}")
    
    def on_message(self, client, userdata, msg):
        """MQTT消息接收回调函数"""
        try:
            payload = msg.payload.decode("utf-8")
            logger.info(f"收到MQTT消息: 主题={msg.topic}, 内容={payload}")
            
            # 解析命令
            command = json.loads(payload)
            command_id = command.get("commandId")
            command_type = command.get("type")
            
            logger.info(f"处理命令: ID={command_id}, 类型={command_type}")
            
            # 模拟命令执行
            self.handle_command(command)
            
            # 发送命令响应
            self.send_command_response(command_id, command_type, "SUCCESS", "命令执行成功")
            
        except json.JSONDecodeError:
            logger.error(f"无法解析命令JSON: {payload}")
        except Exception as e:
            logger.error(f"处理命令时出错: {str(e)}")
    
    def handle_command(self, command: Dict[str, Any]):
        """处理接收到的命令
        
        Args:
            command: 命令数据字典
        """
        command_type = command.get("type")
        parameters = command.get("parameters", {})
        
        # 根据命令类型更新模拟状态
        if command_type == "TAKEOFF":
            altitude = parameters.get("altitude", 10.0)
            self.altitude = altitude
            self.flight_mode = "TAKEOFF"
            logger.info(f"模拟无人机起飞到高度: {altitude}米")
        
        elif command_type == "LAND":
            self.altitude = 0.0
            self.flight_mode = "LAND"
            logger.info("模拟无人机降落")
        
        elif command_type == "RTL":
            self.flight_mode = "RTL"
            logger.info("模拟无人机返航")
        
        elif command_type == "GOTO":
            self.latitude = parameters.get("latitude", self.latitude)
            self.longitude = parameters.get("longitude", self.longitude)
            self.altitude = parameters.get("altitude", self.altitude)
            self.flight_mode = "GOTO"
            logger.info(f"模拟无人机飞行到: 纬度={self.latitude}, 经度={self.longitude}, 高度={self.altitude}米")
        
        elif command_type == "ARM":
            logger.info("模拟无人机解锁")
        
        elif command_type == "DISARM":
            logger.info("模拟无人机上锁")
        
        else:
            logger.warning(f"未知命令类型: {command_type}")
    
    def send_command_response(self, command_id: str, command_type: str, status: str, message: str):
        """发送命令响应
        
        Args:
            command_id: 命令ID
            command_type: 命令类型
            status: 命令状态（RECEIVED, IN_PROGRESS, SUCCESS, FAILED, REJECTED, DEFERRED）
            message: 详细信息
        """
        if not self.mqtt_client or not self.mqtt_credentials:
            logger.error("无法发送命令响应: MQTT连接未建立")
            return
        
        response_topic = self.mqtt_credentials.get("mqttTopicTelemetry").replace("telemetry", "responses")
        
        response = {
            "commandId": command_id,
            "droneId": self.drone_serial,
            "timestamp": datetime.now().isoformat(),
            "status": status,
            "message": message
        }
        
        try:
            payload = json.dumps(response)
            self.mqtt_client.publish(response_topic, payload, qos=1)
            logger.info(f"已发送命令响应: 主题={response_topic}, 状态={status}")
        except Exception as e:
            logger.error(f"发送命令响应时出错: {str(e)}")
    
    def run_mqtt_client(self) -> bool:
        """使用从API获取的凭证连接MQTT代理并开始发布遥测数据
        
        Returns:
            bool: 如果成功则返回True，否则返回False
        """
        if not self.mqtt_credentials:
            logger.error("无法启动MQTT客户端: 缺少MQTT凭证")
            return False
        
        # 提取MQTT凭证
        broker_url = self.mqtt_credentials.get("mqttBrokerUrl")
        username = self.mqtt_credentials.get("mqttUsername")
        password = self.mqtt_credentials.get("mqttPassword")
        telemetry_topic = self.mqtt_credentials.get("mqttTopicTelemetry")
        
        if not all([broker_url, username, password, telemetry_topic]):
            logger.error("MQTT凭证不完整")
            return False
        
        # 解析代理URL
        broker_parts = broker_url.split(":")
        if len(broker_parts) < 2 or not broker_parts[0].startswith("tcp"):
            logger.error(f"无效的MQTT代理URL: {broker_url}")
            return False
        
        broker_host = broker_parts[1].replace("//", "")
        broker_port = int(broker_parts[2]) if len(broker_parts) > 2 else 1883
        
        logger.info(f"连接到MQTT代理: {broker_host}:{broker_port}")
        
        # 创建MQTT客户端
        client_id = f"sim-drone-{self.drone_serial}-{uuid.uuid4().hex[:8]}"
        self.mqtt_client = mqtt.Client(client_id=client_id)
        self.mqtt_client.username_pw_set(username, password)
        self.mqtt_client.on_connect = self.on_connect
        self.mqtt_client.on_message = self.on_message
        
        try:
            # 连接到代理
            self.mqtt_client.connect(broker_host, broker_port, 60)
            
            # 在后台线程启动网络循环
            self.mqtt_client.loop_start()
            
            # 开始发送遥测数据
            self.running = True
            self.publish_telemetry_loop(telemetry_topic)
            
            return True
        
        except Exception as e:
            logger.error(f"MQTT连接失败: {str(e)}")
            return False
    
    def update_simulated_state(self):
        """更新模拟的无人机状态，使数据更加自然"""
        # 电池电量缓慢下降
        self.battery_level = max(0.0, self.battery_level - random.uniform(0.001, 0.005))
        self.battery_voltage = 10.8 + (self.battery_level / 100) * 1.8  # 10.8V到12.6V
        
        # 随机微小的GPS漂移
        if self.flight_mode not in ["LAND", "RTL"]:
            drift = random.uniform(-0.00001, 0.00001)
            self.latitude += drift
            self.longitude += drift
        
        # 随机调整信号强度
        self.signal_strength = min(100, max(60, self.signal_strength + random.uniform(-1, 1)))
        
        # 随机调整卫星数量
        if random.random() < 0.05:  # 5%的概率变化卫星数量
            self.satellites = max(4, min(20, self.satellites + random.choice([-1, 0, 1])))
        
        # 随机调整温度
        self.temperature += random.uniform(-0.1, 0.1)
    
    def publish_telemetry_loop(self, telemetry_topic: str):
        """持续发布遥测数据到指定主题
        
        Args:
            telemetry_topic: 遥测数据发布主题
        """
        logger.info(f"开始发布遥测数据到主题: {telemetry_topic}")
        
        try:
            while self.running:
                # 更新模拟状态
                self.update_simulated_state()
                
                # 准备遥测数据
                telemetry_data = {
                    "droneId": self.drone_serial,
                    "timestamp": datetime.now().isoformat(),
                    "batteryLevel": round(self.battery_level, 2),
                    "batteryVoltage": round(self.battery_voltage, 2),
                    "latitude": self.latitude,
                    "longitude": self.longitude,
                    "altitude": self.altitude,
                    "speed": self.speed,
                    "heading": self.heading,
                    "satellites": self.satellites,
                    "signalStrength": round(self.signal_strength, 2),
                    "flightMode": self.flight_mode,
                    "temperature": round(self.temperature, 2)
                }
                
                # 发布遥测数据
                payload = json.dumps(telemetry_data)
                self.mqtt_client.publish(telemetry_topic, payload, qos=0)
                logger.debug(f"已发布遥测数据: {payload}")
                
                # 等待下一个遥测周期
                time.sleep(self.telemetry_interval)
        
        except KeyboardInterrupt:
            logger.info("收到中断信号，停止发布遥测数据")
        except Exception as e:
            logger.error(f"发布遥测数据时发生错误: {str(e)}")
        finally:
            if self.mqtt_client:
                self.mqtt_client.loop_stop()
                self.mqtt_client.disconnect()
                logger.info("已断开MQTT连接")
    
    def run(self):
        """运行无人机模拟器的完整流程"""
        try:
            # 步骤1: 注册无人机
            if not self.register():
                logger.error("无人机注册失败，退出")
                return False
            
            # 步骤2: 轮询注册状态直到批准或拒绝
            if not self.poll_registration_status():
                logger.error("无人机注册未获批准，退出")
                return False
            
            # 步骤3: 连接MQTT并发送遥测数据
            if not self.run_mqtt_client():
                logger.error("MQTT通信启动失败，退出")
                return False
            
            return True
        
        except KeyboardInterrupt:
            logger.info("收到用户中断，程序退出")
            self.running = False
            return False
        except Exception as e:
            logger.error(f"运行过程中发生未处理的异常: {str(e)}")
            self.running = False
            return False


def main():
    """主函数，处理命令行参数并启动模拟器"""
    parser = argparse.ArgumentParser(description="无人机模拟器")
    parser.add_argument("--serial", type=str, help="无人机序列号", default=f"SIM-DRONE-{uuid.uuid4().hex[:8].upper()}")
    parser.add_argument("--model", type=str, help="无人机型号", default="Simulator-X1")
    parser.add_argument("--api-url", type=str, help="API基础URL", default=DEFAULT_API_BASE_URL)
    parser.add_argument("--poll-interval", type=float, help="注册状态轮询间隔（秒）", default=DEFAULT_POLL_INTERVAL_SEC)
    parser.add_argument("--telemetry-interval", type=float, help="遥测数据发送间隔（秒）", default=DEFAULT_TELEMETRY_INTERVAL_SEC)
    parser.add_argument("--lat", type=float, help="初始纬度", default=DEFAULT_INITIAL_LATITUDE)
    parser.add_argument("--lon", type=float, help="初始经度", default=DEFAULT_INITIAL_LONGITUDE)
    parser.add_argument("--alt", type=float, help="初始高度（米）", default=DEFAULT_INITIAL_ALTITUDE)
    parser.add_argument("--debug", action="store_true", help="启用调试日志")
    
    args = parser.parse_args()
    
    # 如果启用调试模式，设置日志级别为DEBUG
    if args.debug:
        logger.setLevel(logging.DEBUG)
    
    # 记录配置信息
    logger.info("启动无人机模拟器")
    logger.info(f"序列号: {args.serial}")
    logger.info(f"型号: {args.model}")
    logger.info(f"API URL: {args.api_url}")
    logger.info(f"初始位置: 纬度={args.lat}, 经度={args.lon}, 高度={args.alt}米")
    
    # 创建并运行模拟器
    simulator = DroneSimulator(
        drone_serial=args.serial,
        drone_model=args.model,
        api_base_url=args.api_url,
        poll_interval=args.poll_interval,
        telemetry_interval=args.telemetry_interval,
        initial_position=(args.lat, args.lon, args.alt)
    )
    
    simulator.run()


if __name__ == "__main__":
    main() 