<template>
  <div class="todo-notification-widget">
    <!-- 加载状态 -->
    <div v-if="props.isLoading" class="loading-container">
      <el-icon class="is-loading" :size="20" style="color: #8B5CF6;"><Loading /></el-icon>
      <span style="margin-left: 8px; color: #8B5CF6;">加载待办事项中...</span>
    </div>

    <!-- 错误状态 -->
    <el-alert
      v-else-if="props.error"
      :title="props.error"
      type="warning"
      :closable="false"
      class="mb-4"
      show-icon
    >
      <template #default>
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <span>{{ props.error }}</span>
          <span style="font-size: 12px; color: #666;">已自动切换到离线模式</span>
        </div>
      </template>
    </el-alert>

    <!-- 空状态 -->
    <div v-else-if="!notifications || notifications.length === 0" class="empty-state">
      <el-empty 
        description="暂无待办通知" 
        :image-size="60"
        class="todo-empty"
      >
        <template #image>
          <el-icon :size="60" style="color: #8B5CF6;"><Document /></el-icon>
        </template>
        <template #description>
          <span style="color: #8B5CF6;">暂无待办通知</span>
        </template>
      </el-empty>
    </div>

    <!-- 待办通知列表 -->
    <div v-else class="todo-notification-list">
      <!-- 统计信息 -->
      <div class="todo-stats" v-if="notifications.length > 0">
        <div class="stat-item" v-if="props.displayMode !== 'homepage'">
          <span class="stat-label">总数:</span>
          <span class="stat-value total">{{ notifications.length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">{{ props.displayMode === 'homepage' ? '待完成' : '待处理' }}:</span>
          <span class="stat-value pending">{{ pendingCount }}</span>
        </div>
        <div class="stat-item" v-if="overdueCount > 0">
          <span class="stat-label">逾期:</span>
          <span class="stat-value overdue">{{ overdueCount }}</span>
        </div>
        <div class="stat-item" v-if="props.displayMode !== 'homepage'">
          <span class="stat-label">已完成:</span>
          <span class="stat-value completed">{{ completedCount }}</span>
        </div>
      </div>

      <!-- 待办项列表 -->
      <div class="todo-items-container">
        <TodoNotificationItem
          v-for="item in displayedNotifications"
          :key="item.id"
          :item="item"
          @complete="handleComplete"
          @viewDetail="handleViewDetail"
        />
      </div>

      <!-- 查看更多按钮 -->
      <div v-if="(props.displayMode === 'homepage' && notifications.filter(item => !item.isCompleted).length > maxDisplayItems) || (props.displayMode !== 'homepage' && notifications.length > maxDisplayItems)" class="view-more-container">
        <el-button 
          type="text" 
          class="view-more-btn"
          @click="handleViewAll"
        >
          {{ props.displayMode === 'homepage' ? 
            `查看全部 ${notifications.filter(item => !item.isCompleted).length} 项待办` : 
            `查看全部 ${notifications.length} 项待办` 
          }}
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 待办详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedItem?.title"
      width="500px"
      class="todo-detail-dialog"
    >
      <div v-if="selectedItem" class="todo-detail-content">
        <div class="detail-header">
          <el-tag 
            :type="getPriorityType(selectedItem.priority)"
            size="small"
          >
            {{ getPriorityText(selectedItem.priority) }}
          </el-tag>
          <el-tag 
            v-if="selectedItem.status === 'overdue'"
            type="danger"
            size="small"
          >
            逾期
          </el-tag>
        </div>

        <div class="detail-meta">
          <div class="meta-item">
            <strong>发布人:</strong> {{ selectedItem.assignerName }}
          </div>
          <div class="meta-item">
            <strong>截止时间:</strong> {{ formatFullDate(selectedItem.dueDate) }}
          </div>
          <div class="meta-item">
            <strong>状态:</strong> 
            <span :class="getStatusClass(selectedItem.status)">
              {{ getStatusText(selectedItem.status) }}
            </span>
          </div>
        </div>

        <div class="detail-content">
          <h4>详细内容:</h4>
          <p>{{ selectedItem.content }}</p>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="!selectedItem?.isCompleted"
            type="primary"
            @click="handleCompleteFromDialog"
          >
            标记完成
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Document, ArrowRight, Loading } from '@element-plus/icons-vue'
import TodoNotificationItem from './TodoNotificationItem.vue'
import type { TodoNotificationItem as TodoItem } from '@/types/todo'

// Props定义
const props = defineProps<{
  notifications: TodoItem[]
  maxDisplayItems?: number
  isLoading?: boolean
  error?: string
  displayMode?: 'homepage' | 'management'  // 新增：显示模式控制
}>()

// Emits定义
const emit = defineEmits<{
  complete: [id: number, completed: boolean]
  viewAll: []
}>()

// 响应式数据
const detailDialogVisible = ref(false)
const selectedItem = ref<TodoItem | null>(null)

// 最大显示项数
const maxDisplayItems = computed(() => props.maxDisplayItems || 5)

// 显示的通知列表 - 根据显示模式过滤
const displayedNotifications = computed(() => {
  let filteredNotifications = props.notifications
  
  // 首页模式：只显示待处理的待办（排除已完成的）
  if (props.displayMode === 'homepage') {
    filteredNotifications = props.notifications.filter(item => !item.isCompleted)
  }
  // 管理页模式：显示所有待办（由父组件控制过滤）
  
  return filteredNotifications.slice(0, maxDisplayItems.value)
})

// 统计数据
const pendingCount = computed(() => {
  return props.notifications.filter(item => !item.isCompleted && item.status !== 'overdue').length
})

const overdueCount = computed(() => {
  return props.notifications.filter(item => !item.isCompleted && item.status === 'overdue').length
})

const completedCount = computed(() => {
  return props.notifications.filter(item => item.isCompleted).length
})

// 处理完成事件
const handleComplete = (id: number, completed: boolean) => {
  emit('complete', id, completed)
}

// 处理查看详情事件
const handleViewDetail = (item: TodoItem) => {
  selectedItem.value = item
  detailDialogVisible.value = true
}

// 处理查看全部事件
const handleViewAll = () => {
  emit('viewAll')
}

// 从弹窗中标记完成
const handleCompleteFromDialog = () => {
  if (selectedItem.value) {
    emit('complete', selectedItem.value.id, true)
    detailDialogVisible.value = false
  }
}

// 获取优先级类型
const getPriorityType = (priority: string) => {
  switch (priority) {
    case 'high': return 'danger'
    case 'medium': return 'warning'
    case 'low': return 'success'
    default: return 'info'
  }
}

// 获取优先级文本
const getPriorityText = (priority: string) => {
  switch (priority) {
    case 'high': return '高优先级'
    case 'medium': return '中优先级'
    case 'low': return '低优先级'
    default: return '普通'
  }
}

// 获取状态类名
const getStatusClass = (status: string) => {
  switch (status) {
    case 'completed': return 'status-completed'
    case 'overdue': return 'status-overdue'
    case 'pending': return 'status-pending'
    default: return ''
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'completed': return '已完成'
    case 'overdue': return '已逾期'
    case 'pending': return '待处理'
    default: return '未知'
  }
}

// 格式化完整日期
const formatFullDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.todo-notification-widget {
  width: 100%;
  
  .empty-state {
    padding: 20px;
    text-align: center;
    
    .todo-empty {
      .el-empty__description {
        color: #8B5CF6;
      }
    }
  }
}

.todo-notification-list {
  .todo-stats {
    display: flex;
    gap: 16px;
    margin-bottom: 12px;
    padding: 8px 12px;
    background: rgba(139, 92, 246, 0.05);
    border-radius: 6px;
    border: 1px solid rgba(139, 92, 246, 0.1);
    
    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      
      .stat-label {
        color: #718096;
      }
      
      .stat-value {
        font-weight: 600;
        
        &.pending {
          color: #8B5CF6;
        }
        
        &.overdue {
          color: #EF4444;
        }
        
        &.completed {
          color: #10B981;
        }
        
        &.total {
          color: #8B5CF6;
        }
      }
    }
  }
  
  .todo-items-container {
    max-height: 400px;
    overflow-y: auto;
    
    // 自定义滚动条样式
    &::-webkit-scrollbar {
      width: 6px;
    }
    
    &::-webkit-scrollbar-track {
      background: rgba(139, 92, 246, 0.1);
      border-radius: 3px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: rgba(139, 92, 246, 0.3);
      border-radius: 3px;
      
      &:hover {
        background: rgba(139, 92, 246, 0.5);
      }
    }
  }
  
  .view-more-container {
    text-align: center;
    padding: 12px;
    
    .view-more-btn {
      color: #8B5CF6;
      font-size: 13px;
      
      &:hover {
        background-color: rgba(139, 92, 246, 0.1);
      }
    }
  }
}

// 详情弹窗样式
.todo-detail-dialog {
  .todo-detail-content {
    .detail-header {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }
    
    .detail-meta {
      margin-bottom: 20px;
      
      .meta-item {
        display: flex;
        margin-bottom: 8px;
        font-size: 14px;
        
        strong {
          color: #2d3748;
          margin-right: 8px;
          min-width: 80px;
        }
        
        .status-completed {
          color: #10B981;
        }
        
        .status-overdue {
          color: #EF4444;
        }
        
        .status-pending {
          color: #F59E0B;
        }
      }
    }
    
    .detail-content {
      h4 {
        color: #2d3748;
        margin-bottom: 12px;
        font-size: 14px;
      }
      
      p {
        color: #4a5568;
        line-height: 1.6;
        margin: 0;
      }
    }
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .todo-notification-widget {
    .todo-stats {
      flex-direction: column;
      gap: 8px;
    }
    
    .todo-items-container {
      max-height: 300px;
    }
  }
}
</style>