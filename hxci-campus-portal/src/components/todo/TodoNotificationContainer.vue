<!--
  待办通知容器组件 - 第3层实现
  
  @description Props驱动的待办通知容器组件，集成真实数据源并支持完整功能
  @author Frontend Development Team
  @date 2025-08-22
  
  第3层特性:
  ✅ 集成useTodoData composable数据获取
  ✅ 真实API数据集成 (/admin-api/test/todo-new/api/my-list)
  ✅ 用户认证和权限系统集成
  ✅ 数据格式转换 (TodoNotificationItem → TodoItem)
  ✅ Loading/Error状态管理
  ✅ 自动刷新和缓存机制
  ✅ 真实待办数据显示
  
  技术栈:
  - Vue 3 Composition API
  - TypeScript严格模式
  - useTodoData composable
  - Element Plus UI组件
-->

<template>
  <div class="todo-notification-container" :class="containerClasses">
    <!-- 第3层调试信息区域 - 显示真实数据状态 -->
    <div class="debug-info" v-if="showDebugInfo">
      <div class="debug-header">
        <h4>📋 Props驱动的待办组件 - 第3层数据集成</h4>
        <el-tag :type="dataLoadStatus.type" size="small">{{ dataLoadStatus.text }}</el-tag>
      </div>
      
      <div class="debug-content">
        <!-- Props参数验证 -->
        <div class="prop-section">
          <h5>🔧 Props参数验证:</h5>
          <div class="prop-grid">
            <div class="prop-item">
              <span class="prop-label">学号:</span>
              <span class="prop-value">{{ studentId || '未指定' }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">年级:</span>
              <span class="prop-value">{{ grade || '未指定' }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">班级:</span>
              <span class="prop-value">{{ className || '未指定' }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">用户角色:</span>
              <span class="prop-value">{{ userRole || '未指定' }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">显示模式:</span>
              <span class="prop-value">{{ displayMode || '未指定' }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">最大显示数:</span>
              <span class="prop-value">{{ maxItems || '未指定' }}</span>
            </div>
          </div>
        </div>
        
        <!-- 数据状态信息 -->
        <div class="data-section">
          <h5>📊 数据状态:</h5>
          <div class="data-stats">
            <div class="stat-item">
              <span class="stat-label">总数:</span>
              <span class="stat-value">{{ todoStats.total }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待处理:</span>
              <span class="stat-value pending">{{ todoStats.pending }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已完成:</span>
              <span class="stat-value completed">{{ todoStats.completed }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已查看:</span>
              <span class="stat-value viewed">{{ todoStats.viewed }}</span>
            </div>
          </div>
        </div>
        
        <!-- 用户信息 -->
        <div class="user-section" v-if="currentUser">
          <h5>👤 当前用户:</h5>
          <div class="user-info">
            <span class="user-name">{{ currentUser.username }}</span>
            <el-tag size="small">{{ currentUser.roleName }}</el-tag>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 第3层真实数据显示区域 -->
    <div class="todo-content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <span>正在加载待办数据...</span>
      </div>
      
      <!-- 错误状态 -->
      <div v-else-if="error" class="error-state">
        <el-icon class="error-icon"><WarningFilled /></el-icon>
        <div class="error-content">
          <h4>数据加载失败</h4>
          <p>{{ error }}</p>
          <el-button type="primary" size="small" @click="refreshData">
            <el-icon><Refresh /></el-icon>
            重试
          </el-button>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="todos.length === 0" class="empty-state">
        <el-icon class="empty-icon"><DocumentRemove /></el-icon>
        <div class="empty-content">
          <h4>暂无待办事项</h4>
          <p>当前没有符合条件的待办通知</p>
        </div>
      </div>
      
      <!-- 真实待办数据展示 -->
      <div v-else class="todo-list">
        <div class="list-header">
          <h4>
            <el-icon><List /></el-icon>
            待办通知列表 ({{ todos.length }})
          </h4>
          <div class="header-actions">
            <el-button size="small" @click="refreshData" :loading="isLoading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
        
        <div class="todo-items">
          <div 
            v-for="todo in todos" 
            :key="todo.id" 
            class="todo-item"
            :class="getTodoItemClasses(todo)"
          >
            <div class="todo-priority">
              <span class="priority-badge" :class="`priority-${todo.priority}`">
                {{ getPriorityText(todo.priority) }}
              </span>
            </div>
            
            <div class="todo-content-main">
              <h5 class="todo-title">{{ todo.title }}</h5>
              <p class="todo-desc">{{ todo.content }}</p>
              <div class="todo-meta">
                <span class="todo-creator">{{ todo.createdBy }}</span>
                <span class="todo-time" v-if="todo.dueDate">
                  截止: {{ formatDate(todo.dueDate) }}
                </span>
              </div>
            </div>
            
            <div class="todo-actions">
              <el-button 
                v-if="todo.status === 'pending'"
                type="success" 
                size="small"
                @click="markComplete(todo.id)"
              >
                <el-icon><Check /></el-icon>
                完成
              </el-button>
              
              <el-button 
                v-if="todo.status === 'pending'"
                type="info" 
                size="small"
                @click="markViewed(todo.id)"
              >
                <el-icon><View /></el-icon>
                已读
              </el-button>
              
              <el-tag v-else :type="getStatusTagType(todo.status)" size="small">
                {{ getStatusText(todo.status) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  InfoFilled, SuccessFilled, Clock, List, Refresh, 
  WarningFilled, DocumentRemove, Check, View, Loading
} from '@element-plus/icons-vue'
import type { TodoFilterParams, UserRole, TodoDisplayMode, TodoItem } from '@/types/todo'
import { useTodoData } from '@/composables/useTodoData'

/**
 * Props接口定义 - 基于第1层的TodoFilterParams类型
 * @description 使用用户要求的Props接口，支持完整的过滤和显示配置
 */
interface Props {
  /** 学生ID - 过滤指定学生的待办 */
  studentId?: string
  /** 年级 - 过滤指定年级的待办 */
  grade?: string  
  /** 班级名称 - 过滤指定班级的待办 */
  className?: string
  /** 用户角色 - 过滤指定角色的待办 */
  userRole?: UserRole
  /** 显示模式 - 控制组件的展示形式 */
  displayMode?: TodoDisplayMode
  /** 最大显示数量 - 限制显示的待办数量 */
  maxItems?: number
  /** 是否显示已完成项 */
  showCompleted?: boolean
  /** 是否显示调试信息 */
  showDebugInfo?: boolean
  /** 是否启用自动刷新 */
  enableAutoRefresh?: boolean
  /** 自动刷新间隔(秒) */
  autoRefreshInterval?: number
}

/**
 * 定义Props - 使用withDefaults提供默认值
 */
const props = withDefaults(defineProps<Props>(), {
  displayMode: 'card',
  maxItems: 10,
  showCompleted: false,
  showDebugInfo: true,
  enableAutoRefresh: false,
  autoRefreshInterval: 30
})

/**
 * 定义事件
 */
const emit = defineEmits<{
  'todo-complete': [id: number]
  'todo-view': [id: number]
  'data-loaded': [todos: TodoItem[]]
  'data-error': [error: string]
}>()

// ================ 数据获取集成 ================

/**
 * 构建过滤参数
 */
const filterParams = computed((): TodoFilterParams => ({
  studentId: props.studentId,
  grade: props.grade,
  className: props.className,
  userRole: props.userRole,
  showCompleted: props.showCompleted,
  maxItems: props.maxItems
}))

/**
 * 使用useTodoData获取数据
 */
const {
  isLoading,
  error,
  lastUpdateTime,
  todos,
  todoStats,
  currentUser,
  fetchTodos,
  refreshTodos,
  markTodoComplete,
  markTodoViewed,
  initialize,
  cleanup
} = useTodoData(filterParams, {
  autoRefresh: props.enableAutoRefresh,
  refreshInterval: props.autoRefreshInterval * 1000, // 转换为毫秒
  enableCache: true,
  debugMode: props.showDebugInfo
})

// ================ 响应式数据 ================

/** 组件创建时间 */
const createTime = ref<string>(new Date().toLocaleString())

/**
 * 数据加载状态显示
 */
const dataLoadStatus = computed(() => {
  if (isLoading.value) {
    return { type: 'warning', text: '数据加载中...' }
  } else if (error.value) {
    return { type: 'danger', text: '加载失败' }
  } else if (todos.value.length === 0) {
    return { type: 'info', text: '暂无数据' }
  } else {
    return { type: 'success', text: `已加载 ${todos.value.length} 条数据` }
  }
})

/**
 * Props接收统计
 */
const propsReceived = computed(() => {
  const received: Record<string, any> = {}
  
  if (props.studentId) received.studentId = props.studentId
  if (props.grade) received.grade = props.grade
  if (props.className) received.className = props.className
  if (props.userRole) received.userRole = props.userRole
  if (props.displayMode) received.displayMode = props.displayMode
  if (props.maxItems) received.maxItems = props.maxItems
  if (props.showCompleted !== undefined) received.showCompleted = props.showCompleted
  
  return received
})

/** 容器CSS类名 */
const containerClasses = computed(() => {
  return [
    'todo-container',
    `display-mode-${props.displayMode}`,
    props.userRole ? `role-${props.userRole.toLowerCase()}` : '',
    props.showDebugInfo ? 'debug-mode' : '',
    isLoading.value ? 'loading' : '',
    error.value ? 'has-error' : ''
  ].filter(Boolean)
})

// ================ 方法定义 ================

/**
 * 刷新数据
 */
const refreshData = async () => {
  console.log('🔄 [TodoNotificationContainer] 手动刷新数据...')
  await refreshTodos()
}

/**
 * 标记待办完成
 */
const markComplete = async (id: number) => {
  try {
    await markTodoComplete(id)
    emit('todo-complete', id)
    console.log('✅ [TodoNotificationContainer] 待办标记完成:', id)
  } catch (err) {
    console.error('💥 [TodoNotificationContainer] 标记完成失败:', err)
  }
}

/**
 * 标记待办已查看
 */
const markViewed = async (id: number) => {
  try {
    await markTodoViewed(id)
    emit('todo-view', id)
    console.log('👁️ [TodoNotificationContainer] 待办标记已查看:', id)
  } catch (err) {
    console.error('💥 [TodoNotificationContainer] 标记已查看失败:', err)
  }
}

/**
 * 获取待办项的CSS类名
 */
const getTodoItemClasses = (todo: TodoItem) => {
  return [
    `status-${todo.status}`,
    `priority-${todo.priority}`,
    todo.status === 'completed' ? 'completed' : ''
  ].filter(Boolean)
}

/**
 * 获取优先级文字
 */
const getPriorityText = (priority: number): string => {
  const priorityMap: Record<number, string> = {
    1: '最高',
    2: '高',
    3: '中',
    4: '低'
  }
  return priorityMap[priority] || '低'
}

/**
 * 获取状态文字
 */
const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    'pending': '待处理',
    'viewed': '已查看',
    'completed': '已完成'
  }
  return statusMap[status] || '未知'
}

/**
 * 获取状态标签类型
 */
const getStatusTagType = (status: string): string => {
  const typeMap: Record<string, string> = {
    'pending': 'warning',
    'viewed': 'info',
    'completed': 'success'
  }
  return typeMap[status] || 'info'
}

/**
 * 格式化日期
 */
const formatDate = (dateStr: string): string => {
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}

// ================ 生命周期 ================

onMounted(async () => {
  console.log('🚀 [TodoNotificationContainer] 第3层容器组件已挂载')
  console.log('📋 [TodoNotificationContainer] Props接收:', props)
  console.log('🔧 [TodoNotificationContainer] 过滤参数:', filterParams.value)
  
  // 初始化数据获取
  await initialize()
  
  console.log('✅ [TodoNotificationContainer] 第3层组件初始化完成')
})

onUnmounted(() => {
  console.log('🧹 [TodoNotificationContainer] 组件卸载，清理资源...')
  cleanup()
})

// ================ 监听器 ================

// 监听待办数据变化，触发事件
watch(todos, (newTodos) => {
  emit('data-loaded', newTodos)
  console.log('📊 [TodoNotificationContainer] 待办数据更新:', newTodos.length)
}, { deep: true })

// 监听错误状态变化
watch(error, (newError) => {
  if (newError) {
    emit('data-error', newError)
    console.error('💥 [TodoNotificationContainer] 数据错误:', newError)
  }
})

// 监听Props变化
watch(() => props, (newProps) => {
  console.log('🔄 [TodoNotificationContainer] Props更新:', newProps)
}, { deep: true })

watch(() => props.studentId, (newStudentId) => {
  if (newStudentId) {
    console.log('👨‍🎓 [TodoNotificationContainer] 学生ID变更:', newStudentId)
  }
})

watch(() => props.userRole, (newRole) => {
  if (newRole) {
    console.log('👤 [TodoNotificationContainer] 用户角色变更:', newRole)
  }
})

console.log('📦 [TodoNotificationContainer] 第3层容器组件定义完成')
</script>

<style scoped lang="scss">
.todo-notification-container {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  
  // 调试模式样式
  &.debug-mode {
    border-color: #17a2b8;
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  }
  
  // 加载状态
  &.loading {
    opacity: 0.8;
    pointer-events: none;
  }
  
  // 错误状态
  &.has-error {
    border-color: #dc3545;
    background: linear-gradient(135deg, #f8f9fa 0%, #ffeaea 100%);
  }
  
  // 不同显示模式的样式
  &.display-mode-card {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
  
  &.display-mode-list {
    border-radius: 4px;
    box-shadow: none;
  }
  
  &.display-mode-compact {
    padding: 12px;
  }
}

// ================ 调试信息样式 ================

.debug-info {
  background: white;
  border: 1px solid #17a2b8;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
  
  .debug-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #e9ecef;
    
    h4 {
      margin: 0;
      color: #17a2b8;
      font-size: 16px;
    }
  }
  
  .debug-content {
    display: grid;
    gap: 16px;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  }
}

.prop-section, .data-section, .user-section {
  h5 {
    margin: 0 0 8px 0;
    color: #495057;
    font-size: 14px;
    font-weight: 600;
  }
}

.prop-grid {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.prop-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  
  .prop-label {
    font-weight: 500;
    color: #6c757d;
    min-width: 80px;
  }
  
  .prop-value {
    color: #495057;
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f8f9fa;
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 12px;
  }
}

// 数据统计样式
.data-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(80px, 1fr));
  gap: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #e9ecef;
  
  .stat-label {
    font-size: 12px;
    color: #6c757d;
    margin-bottom: 4px;
  }
  
  .stat-value {
    font-size: 18px;
    font-weight: bold;
    color: #495057;
    
    &.pending {
      color: #ffc107;
    }
    
    &.completed {
      color: #28a745;
    }
    
    &.viewed {
      color: #17a2b8;
    }
  }
}

// 用户信息样式
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .user-name {
    font-weight: 500;
    color: #495057;
  }
}

// ================ 数据显示区域样式 ================

.todo-content {
  background: white;
  border-radius: 6px;
  overflow: hidden;
}

// 加载状态
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #6c757d;
  
  .loading-icon {
    font-size: 24px;
    margin-bottom: 12px;
    color: #17a2b8;
    
    &.is-loading {
      animation: rotating 2s linear infinite;
    }
  }
  
  span {
    font-size: 14px;
  }
}

@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

// 错误状态
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  
  .error-icon {
    font-size: 48px;
    color: #dc3545;
    margin-bottom: 16px;
  }
  
  .error-content {
    h4 {
      margin: 0 0 8px 0;
      color: #dc3545;
      font-size: 18px;
    }
    
    p {
      margin: 0 0 16px 0;
      color: #6c757d;
      font-size: 14px;
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  
  .empty-icon {
    font-size: 48px;
    color: #6c757d;
    margin-bottom: 16px;
  }
  
  .empty-content {
    h4 {
      margin: 0 0 8px 0;
      color: #6c757d;
      font-size: 18px;
    }
    
    p {
      margin: 0;
      color: #adb5bd;
      font-size: 14px;
    }
  }
}

// ================ 待办列表样式 ================

.todo-list {
  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #e9ecef;
    
    h4 {
      margin: 0;
      color: #495057;
      font-size: 16px;
      display: flex;
      align-items: center;
      gap: 8px;
      
      .el-icon {
        color: #17a2b8;
      }
    }
    
    .header-actions {
      display: flex;
      gap: 8px;
    }
  }
  
  .todo-items {
    max-height: 400px;
    overflow-y: auto;
  }
}

.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f1f3f4;
  transition: background-color 0.2s ease;
  
  &:hover {
    background-color: #f8f9fa;
  }
  
  &:last-child {
    border-bottom: none;
  }
  
  &.completed {
    opacity: 0.7;
    
    .todo-title {
      text-decoration: line-through;
      color: #6c757d;
    }
  }
  
  // 优先级指示器
  .todo-priority {
    margin-top: 4px;
    
    .priority-badge {
      display: inline-block;
      padding: 2px 6px;
      border-radius: 3px;
      font-size: 10px;
      font-weight: bold;
      text-transform: uppercase;
      
      &.priority-1 {
        background: #dc3545;
        color: white;
      }
      
      &.priority-2 {
        background: #ffc107;
        color: #000;
      }
      
      &.priority-3 {
        background: #17a2b8;
        color: white;
      }
      
      &.priority-4 {
        background: #6c757d;
        color: white;
      }
    }
  }
  
  // 主要内容区域
  .todo-content-main {
    flex: 1;
    min-width: 0;
    
    .todo-title {
      margin: 0 0 4px 0;
      font-size: 14px;
      font-weight: 500;
      color: #495057;
      line-height: 1.4;
    }
    
    .todo-desc {
      margin: 0 0 8px 0;
      font-size: 13px;
      color: #6c757d;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .todo-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 12px;
      color: #adb5bd;
      
      .todo-creator {
        font-weight: 500;
      }
      
      .todo-time {
        &::before {
          content: '⏰';
          margin-right: 4px;
        }
      }
    }
  }
  
  // 操作按钮区域
  .todo-actions {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-top: 4px;
  }
}

// ================ 用户角色特定样式 ================

.role-student {
  .debug-info {
    border-color: #28a745;
  }
  
  .debug-header h4 {
    color: #28a745;
  }
}

.role-teacher, .role-class_teacher {
  .debug-info {
    border-color: #ffc107;
  }
  
  .debug-header h4 {
    color: #ffc107;
  }
}

.role-principal, .role-system_admin {
  .debug-info {
    border-color: #dc3545;
  }
  
  .debug-header h4 {
    color: #dc3545;
  }
}

// ================ 响应式设计 ================

@media (max-width: 768px) {
  .todo-notification-container {
    padding: 12px;
  }
  
  .debug-content {
    grid-template-columns: 1fr;
  }
  
  .prop-grid {
    grid-template-columns: 1fr;
  }
  
  .prop-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .data-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .list-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .todo-item {
    flex-direction: column;
    gap: 8px;
    
    .todo-actions {
      flex-direction: row;
      justify-content: flex-end;
    }
  }
}

@media (max-width: 480px) {
  .debug-info {
    padding: 12px;
  }
  
  .todo-content {
    border-radius: 4px;
  }
  
  .todo-item {
    padding: 12px;
    
    .todo-priority {
      order: 1;
    }
    
    .todo-content-main {
      order: 2;
    }
    
    .todo-actions {
      order: 3;
      margin-top: 8px;
    }
  }
}
</style>