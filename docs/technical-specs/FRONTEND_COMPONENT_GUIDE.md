# 前端组件开发指南 (Frontend Component Guide)

## 📋 项目概述

基于 **Vue3 + TypeScript + Element Plus** 技术栈，为智能通知推送系统构建现代化的前端应用，支持25+角色的复杂权限体系和四级通知分类管理。

## 🏗️ 前端架构设计

### 技术栈选择
```
Vue.js 3.4+               // 现代化前端框架
TypeScript 5.0+           // 类型安全开发
Element Plus 2.4+         // 企业级UI组件库
Pinia 2.1+               // 状态管理
Vue Router 4.2+          // 路由管理
Axios 1.6+               // HTTP客户端
Socket.io-client 4.7+    // WebSocket实时通信
Echarts 5.4+             // 数据可视化
Vite 5.0+                // 构建工具
```

### 项目结构
```
src/
├── api/                    # API接口定义
├── components/             # 公共组件
│   ├── common/            # 通用组件
│   ├── notification/      # 通知相关组件
│   └── workflow/          # 工作流组件
├── composables/           # 组合式API工具
├── hooks/                 # 自定义Hooks
├── layouts/               # 布局组件
├── pages/                 # 页面组件
├── router/                # 路由配置
├── stores/                # Pinia状态管理
├── styles/                # 样式文件
├── types/                 # TypeScript类型定义
├── utils/                 # 工具函数
└── views/                 # 视图组件
```

## 🎨 设计系统与组件库

### 色彩系统
```scss
// 主题色彩
$primary-color: #2563eb;        // 教育蓝
$success-color: #16a34a;        // 成功绿
$warning-color: #f59e0b;        // 警告橙
$danger-color: #dc2626;         // 危险红

// 通知分类色彩
$emergency-color: #ef4444;      // 紧急通知 - 红色
$important-color: #f97316;      // 重要通知 - 橙色
$regular-color: #3b82f6;        // 常规通知 - 蓝色
$reminder-color: #8b5cf6;       // 提醒通知 - 紫色

// 角色权限色彩
$admin-color: #dc2626;          // 管理员 - 红色
$teacher-color: #2563eb;        // 教师 - 蓝色
$student-color: #16a34a;        // 学生 - 绿色
```

### 核心组件库

#### 1. 通知组件 (NotificationComponents)
```typescript
// NotificationCard.vue - 通知卡片组件
interface NotificationCardProps {
  notification: NotificationItem;
  showActions?: boolean;
  compact?: boolean;
}

// NotificationList.vue - 通知列表组件
interface NotificationListProps {
  notifications: NotificationItem[];
  loading?: boolean;
  pagination?: PaginationConfig;
}

// NotificationEditor.vue - 通知编辑器
interface NotificationEditorProps {
  modelValue: NotificationData;
  mode: 'create' | 'edit' | 'view';
  templates?: NotificationTemplate[];
}
```

#### 2. 权限组件 (PermissionComponents)
```typescript
// RoleSelector.vue - 角色选择器
interface RoleSelectorProps {
  modelValue: string[];
  roleTree: RoleTreeNode[];
  multiple?: boolean;
  checkStrictly?: boolean;
}

// PermissionMatrix.vue - 权限矩阵
interface PermissionMatrixProps {
  roles: Role[];
  permissions: Permission[];
  readonly?: boolean;
}
```

#### 3. 工作流组件 (WorkflowComponents)
```typescript
// WorkflowViewer.vue - 工作流查看器
interface WorkflowViewerProps {
  processInstance: ProcessInstance;
  currentTask?: Task;
  showHistory?: boolean;
}

// ApprovalActions.vue - 审批操作
interface ApprovalActionsProps {
  task: Task;
  actions: ApprovalAction[];
  loading?: boolean;
}
```

## 📱 响应式设计规范

### 断点配置
```scss
// 响应式断点
$breakpoints: (
  'xs': 0,
  'sm': 576px,
  'md': 768px,
  'lg': 992px,
  'xl': 1200px,
  'xxl': 1400px
);

// 移动端优化
@media (max-width: 768px) {
  .notification-list {
    .notification-card {
      padding: 12px;
      margin-bottom: 8px;
    }
  }
}
```

### 移动端适配策略
```vue
<template>
  <div class="notification-page">
    <!-- 桌面端布局 -->
    <el-row v-if="!isMobile" :gutter="24">
      <el-col :span="6">
        <sidebar-menu />
      </el-col>
      <el-col :span="18">
        <notification-content />
      </el-col>
    </el-row>
    
    <!-- 移动端布局 -->
    <div v-else class="mobile-layout">
      <mobile-header />
      <notification-content />
      <bottom-navigation />
    </div>
  </div>
</template>
```

## 🔄 状态管理 (Pinia)

### Store结构设计
```typescript
// stores/notification.ts - 通知状态管理
export const useNotificationStore = defineStore('notification', () => {
  // 状态
  const notifications = ref<NotificationItem[]>([]);
  const unreadCount = ref(0);
  const currentFilter = ref<NotificationFilter>({});
  
  // 计算属性
  const unreadNotifications = computed(() => 
    notifications.value.filter(n => !n.isRead)
  );
  
  // 操作方法
  const fetchNotifications = async (params: QueryParams) => {
    // API调用逻辑
  };
  
  const markAsRead = async (id: string) => {
    // 标记已读逻辑
  };
  
  return {
    notifications,
    unreadCount,
    unreadNotifications,
    fetchNotifications,
    markAsRead
  };
});

// stores/auth.ts - 用户权限状态
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null);
  const permissions = ref<Permission[]>([]);
  const roles = ref<Role[]>([]);
  
  const hasPermission = (permission: string) => {
    return permissions.value.some(p => p.code === permission);
  };
  
  const hasRole = (role: string) => {
    return roles.value.some(r => r.code === role);
  };
  
  return { user, permissions, roles, hasPermission, hasRole };
});
```

## 🌐 API 集成方案

### HTTP客户端配置
```typescript
// utils/request.ts
import axios from 'axios';
import { useAuthStore } from '@/stores/auth';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    // 统一错误处理
    handleApiError(error);
    return Promise.reject(error);
  }
);
```

### API接口定义
```typescript
// api/notification.ts
export interface NotificationApi {
  // 获取通知列表
  getNotifications(params: QueryParams): Promise<PageResult<NotificationItem>>;
  
  // 创建通知
  createNotification(data: CreateNotificationData): Promise<NotificationItem>;
  
  // 发布通知
  publishNotification(id: string, data: PublishData): Promise<void>;
  
  // 批量推送
  batchPush(ids: string[], targets: PushTarget[]): Promise<PushResult>;
}

export const notificationApi: NotificationApi = {
  getNotifications: (params) => request.get('/notifications', { params }),
  createNotification: (data) => request.post('/notifications', data),
  publishNotification: (id, data) => request.put(`/notifications/${id}/publish`, data),
  batchPush: (ids, targets) => request.post('/notifications/batch-push', { ids, targets }),
};
```

## 📡 实时通信 (WebSocket)

### WebSocket集成
```typescript
// composables/useWebSocket.ts
export const useWebSocket = () => {
  const socket = ref<Socket | null>(null);
  const isConnected = ref(false);
  
  const connect = () => {
    socket.value = io(import.meta.env.VITE_WEBSOCKET_URL, {
      auth: {
        token: useAuthStore().token
      }
    });
    
    socket.value.on('connect', () => {
      isConnected.value = true;
    });
    
    socket.value.on('disconnect', () => {
      isConnected.value = false;
    });
    
    // 监听通知推送
    socket.value.on('notification:push', (data: NotificationPush) => {
      const notificationStore = useNotificationStore();
      notificationStore.addNotification(data.notification);
      showNotificationToast(data.notification);
    });
  };
  
  const disconnect = () => {
    socket.value?.disconnect();
    socket.value = null;
    isConnected.value = false;
  };
  
  return { socket, isConnected, connect, disconnect };
};
```

### 实时通知处理
```typescript
// components/NotificationToast.vue
<template>
  <el-notification
    v-for="item in notifications"
    :key="item.id"
    :title="item.title"
    :message="item.content"
    :type="getNotificationType(item.level)"
    :duration="getDuration(item.level)"
    @close="handleClose(item.id)"
  />
</template>

<script setup lang="ts">
const notifications = ref<NotificationItem[]>([]);

const showNotification = (notification: NotificationItem) => {
  notifications.value.push(notification);
  
  // 紧急通知需要用户确认
  if (notification.level === 'EMERGENCY') {
    showConfirmDialog(notification);
  }
};

const getNotificationType = (level: NotificationLevel) => {
  const typeMap = {
    'EMERGENCY': 'error',
    'IMPORTANT': 'warning', 
    'REGULAR': 'info',
    'REMINDER': 'success'
  };
  return typeMap[level];
};
</script>
```

## ⚡ 性能优化策略

### 1. 虚拟滚动实现
```vue
<!-- components/VirtualList.vue -->
<template>
  <div class="virtual-list" :style="{ height: `${containerHeight}px` }">
    <div 
      class="virtual-list-phantom" 
      :style="{ height: `${totalHeight}px` }"
    ></div>
    <div 
      class="virtual-list-container" 
      :style="{ transform: `translateY(${offsetY}px)` }"
    >
      <div
        v-for="item in visibleItems"
        :key="item.id"
        class="virtual-list-item"
        :style="{ height: `${itemHeight}px` }"
      >
        <slot :item="item" :index="item.index"></slot>
      </div>
    </div>
  </div>
</template>
```

### 2. 组件懒加载
```typescript
// router/index.ts
const routes = [
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/NotificationManagement.vue'),
    meta: { keepAlive: true }
  },
  {
    path: '/workflow',
    name: 'Workflow', 
    component: () => import('@/views/WorkflowManagement.vue')
  }
];
```

### 3. 缓存策略
```typescript
// composables/useCache.ts
export const useCache = <T>(key: string, ttl: number = 300000) => {
  const cache = new Map<string, { data: T; timestamp: number }>();
  
  const get = (cacheKey: string): T | null => {
    const item = cache.get(cacheKey);
    if (item && Date.now() - item.timestamp < ttl) {
      return item.data;
    }
    return null;
  };
  
  const set = (cacheKey: string, data: T) => {
    cache.set(cacheKey, { data, timestamp: Date.now() });
  };
  
  return { get, set };
};
```

## 🎯 角色权限界面适配

### 权限指令
```typescript
// directives/permission.ts
export const permission = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding;
    const authStore = useAuthStore();
    
    if (!authStore.hasPermission(value)) {
      el.style.display = 'none';
    }
  },
  
  updated(el: HTMLElement, binding: DirectiveBinding) {
    // 权限更新处理
  }
};

// 使用示例
// <el-button v-permission="'notification:create'">创建通知</el-button>
```

### 角色适配组件
```vue
<!-- components/RoleAdapter.vue -->
<template>
  <div class="role-adapter">
    <!-- 校长界面 -->
    <principal-dashboard v-if="isPrincipal" />
    
    <!-- 辅导员界面 -->
    <counselor-dashboard v-else-if="isCounselor" />
    
    <!-- 学生界面 -->
    <student-dashboard v-else-if="isStudent" />
    
    <!-- 默认界面 -->
    <default-dashboard v-else />
  </div>
</template>

<script setup lang="ts">
const authStore = useAuthStore();

const isPrincipal = computed(() => authStore.hasRole('PRINCIPAL'));
const isCounselor = computed(() => authStore.hasRole('COUNSELOR'));
const isStudent = computed(() => authStore.hasRole('STUDENT'));
</script>
```

## 📊 数据可视化组件

### 统计图表组件
```vue
<!-- components/StatisticsChart.vue -->
<template>
  <div class="statistics-chart">
    <div ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';

interface Props {
  data: ChartData;
  type: 'line' | 'bar' | 'pie';
  loading?: boolean;
}

const props = defineProps<Props>();
const chartRef = ref<HTMLElement>();
let chartInstance: echarts.ECharts;

onMounted(() => {
  chartInstance = echarts.init(chartRef.value!);
  updateChart();
});

const updateChart = () => {
  const option = generateChartOption(props.data, props.type);
  chartInstance.setOption(option);
};

watch(() => props.data, updateChart, { deep: true });
</script>
```

## 🧪 测试策略

### 单元测试配置
```typescript
// tests/components/NotificationCard.test.ts
import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import NotificationCard from '@/components/NotificationCard.vue';

describe('NotificationCard', () => {
  it('renders notification content correctly', () => {
    const notification = {
      id: '1',
      title: '测试通知',
      content: '这是一条测试通知',
      level: 'IMPORTANT'
    };
    
    const wrapper = mount(NotificationCard, {
      props: { notification }
    });
    
    expect(wrapper.text()).toContain('测试通知');
    expect(wrapper.find('.notification-level-important')).toBeTruthy();
  });
});
```

### E2E测试示例
```typescript
// tests/e2e/notification.spec.ts
import { test, expect } from '@playwright/test';

test('通知发布流程', async ({ page }) => {
  await page.goto('/notifications');
  
  // 点击创建通知按钮
  await page.click('[data-testid="create-notification"]');
  
  // 填写通知信息
  await page.fill('[data-testid="notification-title"]', '测试通知');
  await page.fill('[data-testid="notification-content"]', '这是测试内容');
  
  // 选择通知级别
  await page.selectOption('[data-testid="notification-level"]', 'IMPORTANT');
  
  // 发布通知
  await page.click('[data-testid="publish-notification"]');
  
  // 验证发布成功
  await expect(page.locator('.success-message')).toContainText('发布成功');
});
```

## 🚀 构建与部署

### Vite配置优化
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', '@vueuse/core'],
      dts: true
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router'],
          elementPlus: ['element-plus'],
          utils: ['lodash', 'dayjs']
        }
      }
    },
    chunkSizeWarningLimit: 1000
  },
  
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      }
    }
  }
});
```

### Docker部署配置
```dockerfile
# Dockerfile
FROM node:18-alpine as builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 📝 开发规范

### 代码风格规范
```json
// .eslintrc.js
{
  "extends": [
    "@vue/typescript/recommended",
    "plugin:vue/vue3-recommended"
  ],
  "rules": {
    "vue/component-name-in-template-casing": ["error", "kebab-case"],
    "vue/prop-name-casing": ["error", "camelCase"],
    "@typescript-eslint/no-unused-vars": "error"
  }
}
```

### Git提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建或辅助工具变动

示例：
feat(notification): 添加批量推送功能
fix(auth): 修复权限验证bug
docs(readme): 更新安装说明
```

## 📋 开发里程碑

### Phase 1: 基础架构 (2周)
- [x] 项目初始化和环境搭建
- [x] 基础组件库开发
- [x] 路由和状态管理配置
- [x] API集成和WebSocket连接

### Phase 2: 核心功能 (4周)
- [ ] 通知管理页面开发
- [ ] 用户权限界面实现
- [ ] 工作流审批页面
- [ ] 实时通信集成

### Phase 3: 高级功能 (3周)
- [ ] 统计监控仪表板
- [ ] 移动端适配优化
- [ ] 性能优化和缓存
- [ ] 数据可视化组件

### Phase 4: 测试部署 (1周)
- [ ] 单元测试完善
- [ ] E2E测试执行
- [ ] 性能测试优化
- [ ] 生产部署配置

这份前端开发指南为智能通知推送系统提供了全面的技术架构和实施方案，确保能够构建出专业、高性能、用户体验优秀的前端应用。