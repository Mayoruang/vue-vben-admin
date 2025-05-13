<script lang="ts" setup>
import { ref, onMounted, onUnmounted, computed, reactive } from 'vue';
import { Card, Drawer, Button, Tabs, Descriptions, Tag, Slider, Switch, Input, notification, Space, Modal, Form } from 'ant-design-vue';
import { EyeOutlined, SendOutlined, EnvironmentOutlined, BarsOutlined, WarningOutlined, ClockCircleOutlined, BorderOutlined, ToolOutlined } from '@ant-design/icons-vue';

// 无人机状态类型
type DroneStatus = 'FLYING' | 'IDLE' | 'LOW_BATTERY' | 'TRAJECTORY_ERROR' | 'OFFLINE';

// 无人机数据接口
interface DroneData {
  droneId: string;
  serialNumber: string;
  model: string;
  status: DroneStatus;
  batteryPercentage: number;
  position: {
    latitude: number;
    longitude: number;
    altitude: number;
  };
  speed: number;
  lastHeartbeat: string;
  mqtt: {
    username: string;
    topicTelemetry: string;
    topicCommands: string;
  };
}

// 初始化状态
const loading = ref(false);
const map = ref<any>(null);
const droneMarkers = ref<any[]>([]);
const drawerVisible = ref(false);
const selectedDrone = ref<DroneData | null>(null);
const activeTabKey = ref('1');
const commandMessage = ref('');
const geofenceActive = ref(false);
const geofenceRadius = ref(500); // 默认500米
const mockDrones = ref<DroneData[]>([]);

// MQTT消息模态框
const mqttModalVisible = ref(false);
const mqttForm = reactive({
  topic: '',
  message: '',
});

// 状态对应的颜色
const statusColors = {
  FLYING: '#1890ff', // 蓝色 - 正常执行任务
  IDLE: '#52c41a',   // 绿色 - 地面待命
  LOW_BATTERY: '#faad14', // 黄色 - 低电量警告
  TRAJECTORY_ERROR: '#ff4d4f', // 红色 - 轨迹异常警告
  OFFLINE: '#d9d9d9' // 灰色 - 离线
};

// 状态对应的中文描述
const statusText = {
  FLYING: '飞行中',
  IDLE: '地面待命',
  LOW_BATTERY: '低电量警告',
  TRAJECTORY_ERROR: '轨迹异常警告',
  OFFLINE: '离线'
};

// 获取状态标签样式
const getStatusTag = (status: DroneStatus) => {
  const color = statusColors[status];
  const text = statusText[status];
  return { color, text };
};

// 获取电池颜色
const getBatteryColor = (percentage: number) => {
  if (percentage <= 20) return '#ff4d4f';
  if (percentage <= 40) return '#faad14';
  return '#52c41a';
};

// 模拟初始无人机数据
const initMockDrones = () => {
  // 以百度地图默认中心为基准生成随机位置
  const baseLatitude = 39.915;
  const baseLongitude = 116.404;

  const statuses: DroneStatus[] = ['FLYING', 'IDLE', 'LOW_BATTERY', 'TRAJECTORY_ERROR', 'OFFLINE'];

  const drones: DroneData[] = [];

  for (let i = 1; i <= 10; i++) {
    // 生成随机位置，范围在中心点周围约10公里内
    const latOffset = (Math.random() - 0.5) * 0.1;
    const lngOffset = (Math.random() - 0.5) * 0.1;

    // 随机选择一个状态
    const randomStatus = statuses[Math.floor(Math.random() * statuses.length)] as DroneStatus;

    // 根据状态调整电池电量
    let batteryPercentage = Math.floor(Math.random() * 100);
    if (randomStatus === 'LOW_BATTERY') {
      batteryPercentage = Math.floor(Math.random() * 20) + 1; // 1-20%
    }

    drones.push({
      droneId: `drone-${i}`,
      serialNumber: `SN${100000 + i}`,
      model: `Model-${String.fromCharCode(65 + (i % 5))}`, // Model-A to Model-E
      status: randomStatus,
      batteryPercentage,
      position: {
        latitude: baseLatitude + latOffset,
        longitude: baseLongitude + lngOffset,
        altitude: Math.floor(Math.random() * 150) + 50, // 50-200米
      },
      speed: Math.floor(Math.random() * 15) + 5, // 5-20 m/s
      lastHeartbeat: new Date().toISOString(),
      mqtt: {
        username: `drone_${i}`,
        topicTelemetry: `drones/drone-${i}/telemetry`,
        topicCommands: `drones/drone-${i}/commands`,
      }
    });
  }

  mockDrones.value = drones;
};

// 初始化百度地图
const initBaiduMap = () => {
  // 动态加载百度地图API
  const script = document.createElement('script');
  script.src = `https://api.map.baidu.com/api?v=3.0&ak=PmtVSHO54O3gJgO3Z9J1VnYP07uHE3TE&callback=initMapInstance`;
  document.body.appendChild(script);

  // 全局回调函数
  window.initMapInstance = () => {
    // 创建地图实例
    const BMap = window.BMap;
    const bmap = new BMap.Map('baiduMap');

    // 创建点坐标（北京天安门）
    const point = new BMap.Point(116.404, 39.915);

    // 初始化地图，设置中心点和缩放级别
    bmap.centerAndZoom(point, 12);

    // 添加缩放和平移控件
    bmap.addControl(new BMap.NavigationControl());
    bmap.addControl(new BMap.ScaleControl());
    bmap.addControl(new BMap.OverviewMapControl());

    // 允许鼠标滚轮缩放
    bmap.enableScrollWheelZoom(true);

    // 保存地图实例
    map.value = bmap;

    // 渲染无人机标记
    renderDroneMarkers();
  };
};

// 显示无人机标记
const renderDroneMarkers = () => {
  if (!map.value) return;

  // 清除所有现有标记
  droneMarkers.value.forEach(marker => {
    map.value.removeOverlay(marker);
  });
  droneMarkers.value = [];

  // 为每个无人机创建标记
  mockDrones.value.forEach(drone => {
    const BMap = window.BMap;
    const point = new BMap.Point(drone.position.longitude, drone.position.latitude);

    // 创建自定义标记
    const customIcon = createDroneIcon(drone);
    const marker = new BMap.Marker(point, { icon: customIcon });

    // 添加点击事件
    marker.addEventListener('click', () => {
      selectedDrone.value = drone;
      drawerVisible.value = true;
    });

    // 添加悬停事件，显示信息窗口
    marker.addEventListener('mouseover', () => {
      const infoWindow = createInfoWindow(drone);
      marker.openInfoWindow(infoWindow);
    });

    // 添加到地图
    map.value.addOverlay(marker);

    // 保存标记引用，以便后续更新
    droneMarkers.value.push(marker);
  });
};

// 创建自定义无人机图标
const createDroneIcon = (drone: DroneData) => {
  const BMap = window.BMap;

  // 根据无人机状态设置颜色
  const color = statusColors[drone.status];

  // 自定义图标HTML内容（使用SVG）
  const content = `
    <div style="position: relative; width: 30px; height: 30px;">
      <!-- 无人机图标 -->
      <svg viewBox="0 0 24 24" width="30" height="30" fill="${color}">
        <path d="M12,0L7,5H9V8H5V6L0,11L5,16V14H9V17H7L12,22L17,17H15V14H19V16L24,11L19,6V8H15V5H17L12,0Z" />
      </svg>

      <!-- 电池指示器("血条") -->
      <div style="position: absolute; top: -8px; left: 50%; transform: translateX(-50%); width: 20px; height: 4px; background-color: #444; border-radius: 2px;">
        <div style="width: ${drone.batteryPercentage}%; height: 100%; background-color: ${getBatteryColor(drone.batteryPercentage)}; border-radius: 2px;"></div>
      </div>
    </div>
  `;

  // 创建自定义图标
  const size = new BMap.Size(30, 30);
  const imageSize = new BMap.Size(30, 30);
  const imageOffset = new BMap.Size(0, 0);
  const infoWindowAnchor = new BMap.Size(15, 0);

  return new BMap.Icon("data:image/svg+xml;charset=utf-8," + encodeURIComponent(content), size, {
    imageSize: imageSize,
    imageOffset: imageOffset,
    infoWindowAnchor: infoWindowAnchor,
    anchor: new BMap.Size(15, 15)
  });
};

// 创建信息窗口
const createInfoWindow = (drone: DroneData) => {
  const BMap = window.BMap;

  // 信息窗口内容
  const content = `
    <div style="min-width: 200px;">
      <h4 style="margin: 0 0 8px; color: #1890ff;">${drone.serialNumber}</h4>
      <p style="margin: 4px 0;">型号: ${drone.model}</p>
      <p style="margin: 4px 0;">状态: <span style="color: ${statusColors[drone.status]};">${statusText[drone.status]}</span></p>
      <p style="margin: 4px 0;">电量: ${drone.batteryPercentage}%</p>
      <p style="margin: 4px 0;">高度: ${drone.position.altitude}米</p>
      <p style="margin: 4px 0;">速度: ${drone.speed}m/s</p>
    </div>
  `;

  return new BMap.InfoWindow(content, {
    width: 220,
    height: 160,
    title: "无人机详情"
  });
};

// 发送指令到无人机
const sendCommand = () => {
  if (!selectedDrone.value || !commandMessage.value) {
    notification.warning({
      message: '发送失败',
      description: '请选择无人机并输入指令信息',
    });
    return;
  }

  notification.success({
    message: '指令已发送',
    description: `已向无人机 ${selectedDrone.value.serialNumber} 发送指令: ${commandMessage.value}`,
  });

  // 清空输入
  commandMessage.value = '';
};

// 绘制地理围栏
const toggleGeofence = () => {
  if (!map.value || !selectedDrone.value) return;

  // 切换围栏状态
  geofenceActive.value = !geofenceActive.value;

  // 清除现有围栏
  map.value.clearOverlays();

  // 重新添加无人机标记
  renderDroneMarkers();

  // 如果开启围栏，绘制围栏圆圈
  if (geofenceActive.value) {
    const BMap = window.BMap;
    const drone = selectedDrone.value;
    const point = new BMap.Point(drone.position.longitude, drone.position.latitude);

    // 创建地理围栏圆形
    const circle = new BMap.Circle(point, geofenceRadius.value, {
      strokeColor: "#1890ff",
      strokeWeight: 2,
      strokeOpacity: 0.8,
      fillColor: "#1890ff",
      fillOpacity: 0.1
    });

    // 添加到地图
    map.value.addOverlay(circle);

    notification.info({
      message: '地理围栏已启用',
      description: `已为无人机 ${drone.serialNumber} 设置${geofenceRadius.value}米半径的地理围栏`,
    });
  } else {
    notification.info({
      message: '地理围栏已禁用',
      description: `已为无人机 ${selectedDrone.value.serialNumber} 禁用地理围栏`,
    });
  }
};

// 打开MQTT消息对话框
const openMqttModal = () => {
  if (!selectedDrone.value) return;

  mqttForm.topic = selectedDrone.value.mqtt.topicCommands;
  mqttForm.message = '';
  mqttModalVisible.value = true;
};

// 发送MQTT消息
const sendMqttMessage = () => {
  if (!selectedDrone.value || !mqttForm.message) {
    notification.warning({
      message: '发送失败',
      description: '请输入消息内容',
    });
    return;
  }

  // 模拟MQTT消息发送
  notification.success({
    message: 'MQTT消息已发送',
    description: `主题: ${mqttForm.topic}, 消息: ${mqttForm.message}`,
  });

  // 关闭对话框
  mqttModalVisible.value = false;
};

// 更新无人机位置（模拟实时移动）
const startDroneMovement = () => {
  const moveDrones = () => {
    mockDrones.value = mockDrones.value.map(drone => {
      // 只移动飞行中的无人机
      if (drone.status === 'FLYING') {
        // 随机偏移
        const latOffset = (Math.random() - 0.5) * 0.002;
        const lngOffset = (Math.random() - 0.5) * 0.002;

        drone.position.latitude += latOffset;
        drone.position.longitude += lngOffset;

        // 随机调整高度
        drone.position.altitude += (Math.random() - 0.5) * 5;
        if (drone.position.altitude < 50) drone.position.altitude = 50;
        if (drone.position.altitude > 200) drone.position.altitude = 200;

        // 减少电量
        drone.batteryPercentage -= 0.1;
        if (drone.batteryPercentage < 0) drone.batteryPercentage = 0;

        // 低电量检测
        if (drone.batteryPercentage <= 20 && (drone.status as DroneStatus) !== 'LOW_BATTERY') {
          drone.status = 'LOW_BATTERY';
        }

        // 更新心跳时间
        drone.lastHeartbeat = new Date().toISOString();
      }

      return drone;
    });

    // 更新地图标记
    renderDroneMarkers();
  };

  // 每2秒更新一次
  const timer = setInterval(moveDrones, 2000);

  // 返回清理函数
  return () => clearInterval(timer);
};

// 生命周期钩子
onMounted(() => {
  initMockDrones();
  initBaiduMap();

  // 开始无人机移动模拟
  const cleanupMovement = startDroneMovement();

  // 组件卸载时清理
  onUnmounted(() => {
    cleanupMovement();
    // 移除地图API脚本
    const script = document.querySelector('script[src*="api.map.baidu.com"]');
    if (script) document.body.removeChild(script);
  });
});

// 向窗口对象添加BMap接口以避免TypeScript错误
declare global {
  interface Window {
    BMap: any;
    initMapInstance: () => void;
  }
}
</script>

<template>
  <div class="p-5">
    <Card title="无人机状态监控" :loading="loading" class="shadow-md" :bodyStyle="{ padding: 0 }">
      <!-- 地图容器 -->
      <div id="baiduMap" style="width: 100%; height: 700px;"></div>

      <!-- 状态图例 -->
      <div class="absolute top-16 right-8 bg-white p-3 shadow-md rounded-md z-10">
        <h4 class="text-base font-medium mb-2">无人机状态</h4>
        <div class="flex flex-col space-y-2">
          <div class="flex items-center">
            <div class="w-4 h-4 rounded-full mr-2" style="background-color: #1890ff;"></div>
            <span>飞行中</span>
          </div>
          <div class="flex items-center">
            <div class="w-4 h-4 rounded-full mr-2" style="background-color: #52c41a;"></div>
            <span>地面待命</span>
          </div>
          <div class="flex items-center">
            <div class="w-4 h-4 rounded-full mr-2" style="background-color: #faad14;"></div>
            <span>低电量警告</span>
          </div>
          <div class="flex items-center">
            <div class="w-4 h-4 rounded-full mr-2" style="background-color: #ff4d4f;"></div>
            <span>轨迹异常警告</span>
          </div>
          <div class="flex items-center">
            <div class="w-4 h-4 rounded-full mr-2" style="background-color: #d9d9d9;"></div>
            <span>离线</span>
          </div>
        </div>
      </div>
    </Card>

    <!-- 无人机详情抽屉 -->
    <Drawer
      :title="`无人机详情 - ${selectedDrone?.serialNumber || ''}`"
      placement="right"
      :width="500"
      :visible="drawerVisible"
      @close="drawerVisible = false"
    >
      <template v-if="selectedDrone">
        <!-- 标签页 -->
        <Tabs v-model:activeKey="activeTabKey">
          <!-- 基本信息标签 -->
          <Tabs.TabPane key="1" tab="基本信息">
            <Descriptions bordered :column="1">
              <Descriptions.Item label="无人机ID">{{ selectedDrone.droneId }}</Descriptions.Item>
              <Descriptions.Item label="序列号">{{ selectedDrone.serialNumber }}</Descriptions.Item>
              <Descriptions.Item label="型号">{{ selectedDrone.model }}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag :color="getStatusTag(selectedDrone.status).color">
                  {{ getStatusTag(selectedDrone.status).text }}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="电量">
                <div class="flex items-center">
                  <BarsOutlined />
                  <div class="ml-2 w-32 h-4 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      class="h-full rounded-full"
                      :style="{
                        width: `${selectedDrone.batteryPercentage}%`,
                        backgroundColor: getBatteryColor(selectedDrone.batteryPercentage)
                      }"
                    ></div>
                  </div>
                  <span class="ml-2">{{ selectedDrone.batteryPercentage }}%</span>
                </div>
              </Descriptions.Item>
              <Descriptions.Item label="位置">
                <div class="flex items-center">
                  <EnvironmentOutlined />
                  <span class="ml-2">
                    {{ selectedDrone.position.latitude.toFixed(6) }},
                    {{ selectedDrone.position.longitude.toFixed(6) }}
                  </span>
                </div>
              </Descriptions.Item>
              <Descriptions.Item label="高度">{{ selectedDrone.position.altitude }}米</Descriptions.Item>
              <Descriptions.Item label="速度">{{ selectedDrone.speed }}m/s</Descriptions.Item>
              <Descriptions.Item label="最后心跳">
                <div class="flex items-center">
                  <ClockCircleOutlined />
                  <span class="ml-2">{{ new Date(selectedDrone.lastHeartbeat).toLocaleString() }}</span>
                </div>
              </Descriptions.Item>
            </Descriptions>
          </Tabs.TabPane>

          <!-- 控制指令标签 -->
          <Tabs.TabPane key="2" tab="控制指令">
            <div class="space-y-4">
              <div>
                <p class="mb-2 font-medium">发送控制指令:</p>
                <Input.TextArea
                  v-model:value="commandMessage"
                  placeholder="输入控制指令"
                  :rows="4"
                  class="mb-2"
                />
                <Button type="primary" @click="sendCommand">
                  <template #icon><SendOutlined /></template>
                  发送指令
                </Button>
              </div>

              <div class="mt-4">
                <p class="mb-2 font-medium">预设指令:</p>
                <Space>
                  <Button @click="commandMessage = JSON.stringify({ action: 'GOTO_HOME', parameters: {} })">
                    返航
                  </Button>
                  <Button @click="commandMessage = JSON.stringify({ action: 'LAND', parameters: {} })">
                    降落
                  </Button>
                  <Button @click="commandMessage = JSON.stringify({ action: 'HOVER', parameters: { duration: 30 } })">
                    悬停
                  </Button>
                </Space>
              </div>
            </div>
          </Tabs.TabPane>

          <!-- 地理围栏标签 -->
          <Tabs.TabPane key="3" tab="地理围栏">
            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <span class="font-medium">地理围栏状态:</span>
                <Switch :checked="geofenceActive" @change="toggleGeofence" />
              </div>

              <div>
                <p class="mb-2 font-medium">围栏半径: {{ geofenceRadius }}米</p>
                <Slider
                  v-model:value="geofenceRadius"
                  :min="100"
                  :max="2000"
                  :step="100"
                  :disabled="!geofenceActive"
                />
              </div>

              <div class="mt-4">
                <Button type="primary" :disabled="!geofenceActive" @click="toggleGeofence">
                  <template #icon><BorderOutlined /></template>
                  应用围栏
                </Button>
              </div>
            </div>
          </Tabs.TabPane>

          <!-- MQTT通信标签 -->
          <Tabs.TabPane key="4" tab="MQTT通信">
            <Descriptions bordered :column="1">
              <Descriptions.Item label="MQTT用户名">{{ selectedDrone.mqtt.username }}</Descriptions.Item>
              <Descriptions.Item label="遥测主题">{{ selectedDrone.mqtt.topicTelemetry }}</Descriptions.Item>
              <Descriptions.Item label="指令主题">{{ selectedDrone.mqtt.topicCommands }}</Descriptions.Item>
            </Descriptions>

            <div class="mt-4">
              <Button type="primary" @click="openMqttModal">
                <template #icon><SendOutlined /></template>
                发送MQTT消息
              </Button>
            </div>
          </Tabs.TabPane>

          <!-- 维护工具标签 -->
          <Tabs.TabPane key="5" tab="维护工具">
            <p class="text-gray-500">此处可添加无人机维护和调试工具</p>
            <div class="mt-4">
              <Button type="primary">
                <template #icon><ToolOutlined /></template>
                固件更新
              </Button>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </template>

      <template v-else>
        <p>未选择无人机</p>
      </template>
    </Drawer>

    <!-- MQTT消息对话框 -->
    <Modal
      title="发送MQTT消息"
      :visible="mqttModalVisible"
      @ok="sendMqttMessage"
      @cancel="mqttModalVisible = false"
    >
      <Form layout="vertical">
        <Form.Item label="主题">
          <Input v-model:value="mqttForm.topic" readOnly />
        </Form.Item>
        <Form.Item label="消息内容">
          <Input.TextArea v-model:value="mqttForm.message" :rows="4" placeholder="输入MQTT消息内容" />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
/* 自定义样式 */
.shadow-md {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

/* 确保信息窗口层级高于图例 */
:deep(.BMap_bubble_pop) {
  z-index: 20 !important;
}

/* 确保抽屉组件在地图上层 */
:deep(.ant-drawer) {
  z-index: 1001;
}
</style>
