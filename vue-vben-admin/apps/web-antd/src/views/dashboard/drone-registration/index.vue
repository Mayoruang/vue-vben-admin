<script lang="ts" setup>
import { computed, onMounted, ref, h } from 'vue';
import { notification, Card, Drawer, Button, Table, Statistic, Space, Tag, Input, Popconfirm } from 'ant-design-vue';
import { CheckCircleOutlined, CloseCircleOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons-vue';

// 定义无人机注册请求类型
interface DroneRegistrationRequest {
  requestId: string;
  serialNumber: string;
  model: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
  requestedAt: string;
  processedAt?: string;
  adminNotes?: string;
  droneId?: string;
}

// 定义 AdminAction 类型
interface AdminAction {
  requestId: string;
  action: 'APPROVE' | 'REJECT';
  notes?: string;
}

// 状态统计
const statistics = ref({
  pendingCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
});

// 抽屉状态
const drawerVisible = ref(false);
const selectedDrone = ref<DroneRegistrationRequest | null>(null);
const rejectReason = ref('');

// 表格配置
const loading = ref(false);
const registrationRequests = ref<DroneRegistrationRequest[]>([]);

// 搜索和筛选
const searchText = ref('');
const filterStatus = ref<string | null>(null);

// 根据搜索和筛选条件过滤列表
const filteredList = computed(() => {
  let result = [...registrationRequests.value];

  // 应用搜索
  if (searchText.value) {
    const search = searchText.value.toLowerCase();
    result = result.filter(
      item => item.serialNumber.toLowerCase().includes(search) ||
              item.model.toLowerCase().includes(search) ||
              item.requestId.toLowerCase().includes(search)
    );
  }

  // 应用状态筛选
  if (filterStatus.value) {
    result = result.filter(item => item.status === filterStatus.value);
  }

  return result;
});

// 获取列表数据
async function fetchRegistrationList() {
  loading.value = true;
  try {
    // 实际项目中替换为真实API调用
    const response = await fetch('/api/v1/drones/registration/list');
    if (!response.ok) {
      throw new Error('获取注册申请列表失败');
    }
    const data = await response.json();
    registrationRequests.value = data.content || [];

    // 更新统计信息
    updateStatistics();
  } catch (error) {
    console.error('获取注册申请列表出错:', error);
    notification.error({
      message: '获取数据失败',
      description: (error as Error).message,
    });
  } finally {
    loading.value = false;
  }
}

// 更新统计数据
function updateStatistics() {
  statistics.value = {
    pendingCount: registrationRequests.value.filter(item => item.status === 'PENDING_APPROVAL').length,
    approvedCount: registrationRequests.value.filter(item => item.status === 'APPROVED').length,
    rejectedCount: registrationRequests.value.filter(item => item.status === 'REJECTED').length,
  };
}

// 查看详情
function viewDetails(record: DroneRegistrationRequest) {
  selectedDrone.value = record;
  drawerVisible.value = true;
}

// 批准申请
async function approveRegistration(record: DroneRegistrationRequest) {
  try {
    const action: AdminAction = {
      requestId: record.requestId,
      action: 'APPROVE',
    };

    // 实际项目中替换为真实API调用
    const response = await fetch('/api/v1/admin/registrations/action', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(action),
    });

    if (!response.ok) {
      throw new Error('批准申请失败');
    }

    notification.success({
      message: '批准成功',
      description: `已批准序列号为 ${record.serialNumber} 的无人机注册申请`,
    });

    // 刷新列表
    await fetchRegistrationList();
  } catch (error) {
    notification.error({
      message: '操作失败',
      description: (error as Error).message,
    });
  }
}

// 拒绝申请
async function rejectRegistration(record: DroneRegistrationRequest) {
  try {
    const action: AdminAction = {
      requestId: record.requestId,
      action: 'REJECT',
      notes: rejectReason.value,
    };

    // 实际项目中替换为真实API调用
    const response = await fetch('/api/v1/admin/registrations/action', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(action),
    });

    if (!response.ok) {
      throw new Error('拒绝申请失败');
    }

    notification.success({
      message: '拒绝成功',
      description: `已拒绝序列号为 ${record.serialNumber} 的无人机注册申请`,
    });

    // 清空拒绝原因
    rejectReason.value = '';

    // 刷新列表
    await fetchRegistrationList();
  } catch (error) {
    notification.error({
      message: '操作失败',
      description: (error as Error).message,
    });
  }
}

// 自定义状态渲染
function getStatusTag(status: string) {
  switch (status) {
    case 'PENDING_APPROVAL':
      return { color: 'processing', text: '待审批' };
    case 'APPROVED':
      return { color: 'success', text: '已批准' };
    case 'REJECTED':
      return { color: 'error', text: '已拒绝' };
    default:
      return { color: '', text: status };
  }
}

// 表格列定义
const columns = [
  {
    title: '申请ID',
    dataIndex: 'requestId',
    key: 'requestId',
    width: 220,
    ellipsis: true,
  },
  {
    title: '序列号',
    dataIndex: 'serialNumber',
    key: 'serialNumber',
    width: 160,
  },
  {
    title: '型号',
    dataIndex: 'model',
    key: 'model',
    width: 120,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    align: 'center' as const,
    customRender: ({ text }: { text: string }) => {
      const { color, text: statusText } = getStatusTag(text);
      return h(Tag, { color }, () => statusText);
    },
  },
  {
    title: '申请时间',
    dataIndex: 'requestedAt',
    key: 'requestedAt',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 240,
    align: 'center' as const,
    customRender: ({ record }: { record: DroneRegistrationRequest }) => {
      if (record.status === 'PENDING_APPROVAL') {
        return h(Space, {}, [
          h(Button, {
            type: 'primary',
            size: 'small',
            onClick: () => viewDetails(record)
          }, [h(EyeOutlined), ' 详情']),
          h(Button, {
            type: 'primary',
            size: 'small',
            onClick: () => approveRegistration(record),
            style: { backgroundColor: '#52c41a', borderColor: '#52c41a' }
          }, [h(CheckCircleOutlined), ' 同意']),
          h(Popconfirm, {
            title: '拒绝申请',
            description: '确定要拒绝此申请吗？',
            onConfirm: () => rejectRegistration(record),
            okText: '确定',
            cancelText: '取消'
          }, [
            h(Button, {
              danger: true,
              size: 'small'
            }, [h(CloseCircleOutlined), ' 拒绝'])
          ])
        ]);
      } else {
        return h(Button, {
          type: 'primary',
          size: 'small',
          onClick: () => viewDetails(record)
        }, [h(EyeOutlined), '详情']);
      }
    },
  },
];

// 模拟WebSocket连接
function setupWebSocket() {
  console.log('WebSocket连接已建立，等待实时无人机注册请求...');
  // 实际项目中替换为真实WebSocket实现
  // const ws = new WebSocket('ws://your-backend-url/ws/registrations');
  // ws.onmessage = (event) => {
  //   const data = JSON.parse(event.data);
  //   // 处理新的注册请求
  //   if (data.type === 'NEW_REGISTRATION') {
  //     // 添加到列表并更新统计
  //     registrationRequests.value = [data.registration, ...registrationRequests.value];
  //     updateStatistics();
  //
  //     // 通知
  //     notification.info({
  //       message: '新注册申请',
  //       description: `收到新的无人机注册申请，序列号: ${data.registration.serialNumber}`,
  //     });
  //   }
  // };
  //
  // ws.onerror = (error) => {
  //   console.error('WebSocket error:', error);
  // };
  //
  // return ws;
}

// 生命周期钩子
onMounted(() => {
  fetchRegistrationList();
  const ws = setupWebSocket();

  // 清理
  // onUnmounted(() => {
  //   if (ws && ws.readyState === WebSocket.OPEN) {
  //     ws.close();
  //   }
  // });
});
</script>

<template>
  <div class="p-5">
    <div class="mb-5">
      <h2 class="text-lg font-bold mb-4">无人机注册管理</h2>
      <div class="flex justify-between gap-4">
        <!-- 待审批统计卡片 -->
        <Card class="flex-1 shadow-sm">
          <Statistic
            title="待审批"
            :value="statistics.pendingCount"
            :value-style="{ color: '#1890ff' }"
          />
        </Card>

        <!-- 已注册统计卡片 -->
        <Card class="flex-1 shadow-sm">
          <Statistic
            title="已注册"
            :value="statistics.approvedCount"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>

        <!-- 已拒绝统计卡片 -->
        <Card class="flex-1 shadow-sm">
          <Statistic
            title="已拒绝"
            :value="statistics.rejectedCount"
            :value-style="{ color: '#ff4d4f' }"
          />
        </Card>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="mb-4 flex justify-between items-center">
      <div class="flex items-center space-x-4">
        <Input
          v-model:value="searchText"
          placeholder="搜索序列号/型号"
          style="width: 250px;"
          allow-clear
        />
        <Space>
          <Button
            :type="filterStatus === null ? 'primary' : 'default'"
            @click="filterStatus = null"
          >
            全部
          </Button>
          <Button
            :type="filterStatus === 'PENDING_APPROVAL' ? 'primary' : 'default'"
            @click="filterStatus = 'PENDING_APPROVAL'"
          >
            待审批
          </Button>
          <Button
            :type="filterStatus === 'APPROVED' ? 'primary' : 'default'"
            @click="filterStatus = 'APPROVED'"
          >
            已批准
          </Button>
          <Button
            :type="filterStatus === 'REJECTED' ? 'primary' : 'default'"
            @click="filterStatus = 'REJECTED'"
          >
            已拒绝
          </Button>
        </Space>
      </div>

      <Button
        type="primary"
        @click="fetchRegistrationList"
        :loading="loading"
      >
        <template #icon><ReloadOutlined /></template>
        刷新
      </Button>
    </div>

    <!-- 列表 -->
    <Card class="shadow-sm">
      <Table
        :columns="columns"
        :data-source="filteredList"
        :loading="loading"
        :scroll="{ x: 1000 }"
        row-key="requestId"
        bordered
        size="middle"
        :pagination="{
          showSizeChanger: true,
          showQuickJumper: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: (total) => `共 ${total} 条`,
          defaultPageSize: 10,
          position: ['bottomRight']
        }"
        :locale="{
          emptyText: '暂无数据'
        }"
      />
    </Card>

    <!-- 详情抽屉 -->
    <Drawer
      title="无人机注册申请详情"
      placement="right"
      :width="500"
      :visible="drawerVisible"
      @close="drawerVisible = false"
      :footer-style="{ textAlign: 'right' }"
    >
      <div v-if="selectedDrone">
        <Card title="基本信息" class="mb-4">
          <p class="mb-2"><strong>申请ID:</strong> {{ selectedDrone.requestId }}</p>
          <p class="mb-2"><strong>序列号:</strong> {{ selectedDrone.serialNumber }}</p>
          <p class="mb-2"><strong>型号:</strong> {{ selectedDrone.model }}</p>
          <p class="mb-2">
            <strong>状态:</strong>
            <span v-if="selectedDrone.status === 'PENDING_APPROVAL'">
              <Tag color="processing">待审批</Tag>
            </span>
            <span v-else-if="selectedDrone.status === 'APPROVED'">
              <Tag color="success">已批准</Tag>
            </span>
            <span v-else-if="selectedDrone.status === 'REJECTED'">
              <Tag color="error">已拒绝</Tag>
            </span>
          </p>
          <p class="mb-2"><strong>申请时间:</strong> {{ selectedDrone.requestedAt }}</p>
          <p v-if="selectedDrone.processedAt" class="mb-2">
            <strong>处理时间:</strong> {{ selectedDrone.processedAt }}
          </p>
          <p v-if="selectedDrone.droneId" class="mb-2">
            <strong>无人机ID:</strong> {{ selectedDrone.droneId }}
          </p>
        </Card>

        <Card v-if="selectedDrone.adminNotes" title="管理员备注" class="mb-4">
          <p>{{ selectedDrone.adminNotes }}</p>
        </Card>

        <div v-if="selectedDrone.status === 'PENDING_APPROVAL'" class="mt-4">
          <h3 class="font-medium mb-2">审批操作</h3>
          <Space direction="vertical" style="width: 100%">
            <Button
              type="primary"
              block
              @click="approveRegistration(selectedDrone)"
            >
              <template #icon><CheckCircleOutlined /></template>
              批准申请
            </Button>

            <div>
              <Input.TextArea
                v-model:value="rejectReason"
                placeholder="请输入拒绝原因（可选）"
                :rows="3"
                class="mb-2"
              />
              <Button
                danger
                block
                @click="rejectRegistration(selectedDrone)"
              >
                <template #icon><CloseCircleOutlined /></template>
                拒绝申请
              </Button>
            </div>
          </Space>
        </div>
      </div>
      <template #footer>
        <Button @click="drawerVisible = false">关闭</Button>
      </template>
    </Drawer>
  </div>
</template>

<style scoped>
.ant-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.ant-table-wrapper {
  @apply overflow-hidden;
}

/* 表格行样式 */
.ant-table-tbody > tr.ant-table-row:hover > td {
  @apply bg-blue-50;
}

/* 统计卡片样式 */
.ant-statistic-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.ant-statistic-content {
  font-weight: 600;
}

/* 工具栏样式 */
.space-x-4 button {
  min-width: 70px;
}

/* 表格样式 */
.ant-table-thead > tr > th {
  font-weight: 600;
  background-color: #f5f7fa;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .flex {
    @apply flex-col;
  }

  .space-x-4 {
    @apply space-x-0 space-y-2;
  }

  .mb-4 {
    @apply mb-2;
  }
}
</style>
