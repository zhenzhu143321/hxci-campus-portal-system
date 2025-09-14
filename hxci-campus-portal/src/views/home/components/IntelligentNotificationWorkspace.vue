<!--
/**
 * 智能通知工作台容器组件
 * 
 * @description 智能通知工作台的容器组件，负责数据获取、状态管理和布局协调
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21
 * @stage Stage 6 - Home.vue组件拆分架构
 * 
 * @responsibilities
 * - 从NotificationStore获取通知数据
 * - 管理工作台整体布局和状态
 * - 协调子组件间的数据流
 * - 处理用户交互事件的业务逻辑
 * 
 * @dependencies
 * - NotificationStore: 通知数据和状态管理
 * - UIStore: UI状态和调试面板管理
 * - TodoStore: 待办事项数据管理
 */
-->

<template>
  <div class="intelligent-workspace">
    <!-- 工作台标题栏 -->
    <div class="section-header">
      <h3><el-icon><Bell /></el-icon>📋 智能通知工作台</h3>
      <el-button 
        type="text" 
        size="small" 
        @click="handleShowAllNotifications"
      >
        查看更多
      </el-button>
    </div>
    
    <!-- 🚨 优先处理通知区域 -->
    <PriorityNotificationList
      v-if="unreadPriorityNotifications && unreadPriorityNotifications.length > 0"
      :notifications="unreadPriorityNotifications"
      :max-display="5"
      :is-loading="isMarkingReadLoading"
      :read-status-checker="checkReadStatus"
      :marking-read-ids="Array.from(uiStore.operationLoading.markingRead)"
      @notification-click="handleNotificationClick"
      @mark-read="handleMarkAsRead"
      @show-all-priority="handleShowAllNotifications"
    />
    
    <!-- 📚 今日课程安排模块 -->
    <CourseScheduleModule
      :courses="todayCourses"
      :current-time="currentTime"
      @course-click="handleCourseClick"
    />
    
    <!-- 📋 待办通知模块 -->
    <div class="workspace-module-card todo-notification-module">
      <div class="module-header">
        <h4><el-icon><Document /></el-icon>📋 待办通知</h4>
        <el-tag type="primary" size="small">{{ todoStore.pendingCount }}项待办</el-tag>
      </div>
      <TodoNotificationWidget 
        :notifications="todoStore.pendingTodos" 
        :max-display-items="4"
        :is-loading="todoStore.isLoading"
        :error="todoStore.error"
        display-mode="homepage"
        @complete="handleTodoComplete"
        @view-all="handleViewAllTodos"
      />
    </div>
    
    <!-- 💬 通知消息模块 - 修复版：始终显示组件区域 -->
    <div class="workspace-module-card level4-module">
      <div class="module-header">
        <h4><el-icon><Document /></el-icon>💬 通知消息</h4>
        <el-tag type="success" size="small">{{ level4Messages.length }}条提醒</el-tag>
      </div>
      
      <NotificationMessagesWidget
        :messages="level4Messages"
        :maxDisplay="4"
        :readStatusChecker="checkReadStatus"
        :markingReadIds="Array.from(uiStore.operationLoading.markingRead)"
        @notification-click="handleNotificationClick"
        @mark-read="handleMarkAsRead"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Document } from '@element-plus/icons-vue'

// Store导入
import { useNotificationStore } from '@/stores/notification'
import { useUIStore } from '@/stores/ui'
import { useTodoStore } from '@/stores/todo'

// 组件导入
import PriorityNotificationList from './PriorityNotificationList.vue'
import CourseScheduleModule from './CourseScheduleModule.vue'
import TodoNotificationWidget from '@/components/TodoNotificationWidget.vue'
import NotificationMessagesWidget from './NotificationMessagesWidget.vue'

// 类型导入
import type { NotificationItem } from '@/api/notification'
import type { TodoNotificationItem } from '@/types/todo'

// ================== Props定义 ==================

interface Props {
  /** 当前用户信息 */
  userInfo?: any
  /** 是否显示调试信息 */
  debugMode?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  debugMode: false
})

// ================== Emits定义 ==================

interface Emits {
  /** 通知点击事件 */
  (e: 'notification-click', notification: NotificationItem, markAsRead?: boolean): void
  /** 显示全部通知 */
  (e: 'show-all-notifications'): void
  /** 课程点击事件 */
  (e: 'course-click', course: any): void
  /** 待办完成事件 */
  (e: 'todo-complete', todoId: number): void
  /** 查看全部待办 */
  (e: 'view-all-todos'): void
}

const emit = defineEmits<Emits>()

// ================== Store初始化 ==================

const notificationStore = useNotificationStore()
const uiStore = useUIStore()
const todoStore = useTodoStore()

// ================== 计算属性 - 直接使用Store ==================

/** 未读优先级通知 (Level 1-3) - 安全访问 */
const unreadPriorityNotifications = computed(() => {
  const notifications = notificationStore.unreadPriorityNotifications
  // 🚀 核心修复：确保始终返回数组，防止undefined导致的length访问错误
  return Array.isArray(notifications) ? notifications : []
})

/** 系统公告 - 安全访问 */
const systemAnnouncements = computed(() => {
  const announcements = notificationStore.systemAnnouncements
  return Array.isArray(announcements) ? announcements : []
})

/** Level 4 通知消息 - 安全访问 */
const level4Messages = computed(() => {
  const messages = notificationStore.level4Messages
  return Array.isArray(messages) ? messages : []
})

/** 已读状态管理器 */
const readStatusManager = computed(() => 
  notificationStore.readStatusManager
)

/** 标记已读loading状态 */
const isMarkingReadLoading = computed(() => 
  uiStore.operationLoading.markingRead.size > 0
)

/** 待办数量 - 直接使用TodoStore的pendingCount计算属性 (避免重复逻辑) */

// ================== 响应式数据 ==================

/** 当前时间 (用于课程状态) */
const currentTime = ref(new Date())

/** 已读状态检查函数 - 传递给子组件 */
const checkReadStatus = (notificationId: number): boolean => {
  return notificationStore.isRead(notificationId)
}

/** 今日课程安排 (模拟数据) */
const todayCourses = ref([
  {
    id: 1,
    name: '高等数学',
    teacher: '张教授',
    location: '教学楼A101',
    time: '08:30-10:10',
    status: 'completed'
  },
  {
    id: 2,
    name: '数据库原理',
    teacher: '李教授', 
    location: '教学楼B203',
    time: '10:30-12:10',
    status: 'current'
  },
  {
    id: 3,
    name: 'Web开发技术',
    teacher: '王教授',
    location: '机房C301',
    time: '14:00-15:40',
    status: 'upcoming'
  },
  {
    id: 4,
    name: '软件工程',
    teacher: '赵教授',
    location: '教学楼A205',
    time: '16:00-17:40',
    status: 'upcoming'
  }
])

// ================== 事件处理器 ==================

/** 处理通知点击 - 修复：默认不自动标记已读，避免查看详情时通知消失 */
const handleNotificationClick = (notification: NotificationItem, markAsRead = false) => {
  emit('notification-click', notification, markAsRead)
}

/** 处理显示全部通知 */
const handleShowAllNotifications = () => {
  emit('show-all-notifications')
}

/** 处理标记已读 */
const handleMarkAsRead = async (notificationId: number) => {
  try {
    await notificationStore.markAsRead(notificationId)
    ElMessage.success('已标记为已读')
  } catch (error) {
    console.error('标记已读失败:', error)
    ElMessage.error('标记已读失败')
  }
}

/** 处理课程点击 */
const handleCourseClick = (course: any) => {
  emit('course-click', course)
}

/** 处理待办完成 */
const handleTodoComplete = async (todoId: number) => {
  emit('todo-complete', todoId)
}

/** 处理查看全部待办 */
const handleViewAllTodos = () => {
  emit('view-all-todos')
}

// ================== 生命周期 ==================

onMounted(() => {
  // 初始化当前时间
  const updateTime = () => {
    currentTime.value = new Date()
  }
  
  // 每分钟更新一次时间
  updateTime()
  setInterval(updateTime, 60000)
  
  console.log('🎯 [IntelligentNotificationWorkspace] 组件初始化完成')
})

// ================== 调试信息 ==================

if (props.debugMode) {
  console.log('🔧 [调试] IntelligentNotificationWorkspace组件数据状态:')
  console.log('- 未读优先通知:', unreadPriorityNotifications.value?.length || 0)
  console.log('- Level 4消息:', level4Messages.value?.length || 0)
  console.log('- 待办事项:', pendingTodoCount.value)
  console.log('- 今日课程:', todayCourses.value.length)
}
</script>

<style scoped>
/* 智能工作台样式 */
.intelligent-workspace {
  flex: 2;
  max-width: 800px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 区块头部样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

/* 工作台模块卡片样式 */
.workspace-module-card {
  background: linear-gradient(135deg, #f8f9ff 0%, #ffffff 100%);
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.workspace-module-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

/* 模块头部样式 */
.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.module-header h4 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .intelligent-workspace {
    flex: 1;
    max-width: none;
    gap: 16px;
  }
  
  .workspace-module-card {
    padding: 16px;
    border-radius: 8px;
  }
  
  .section-header h3 {
    font-size: 16px;
  }
  
  .module-header h4 {
    font-size: 14px;
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .workspace-module-card {
    background: linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 100%);
    border-color: #3a3a3a;
    color: #e0e0e0;
  }
  
  .section-header h3,
  .module-header h4 {
    color: #e0e0e0;
  }
}
</style>