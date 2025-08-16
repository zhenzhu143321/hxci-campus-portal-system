<template>
  <div 
    class="todo-notification-item"
    :class="{
      'todo-completed': item.isCompleted,
      'todo-overdue': isOverdue && !item.isCompleted,
      'todo-high': item.priority === 'high' && !item.isCompleted,
      'todo-medium': item.priority === 'medium' && !item.isCompleted,
      'todo-low': item.priority === 'low' && !item.isCompleted
    }"
  >
    <!-- 优先级指示器 -->
    <div class="todo-priority-indicator">
      <div 
        class="priority-dot"
        :class="[
          `priority-${item.priority}`,
          {
            'priority-high': item.priority === 'high',
            'priority-medium': item.priority === 'medium', 
            'priority-low': item.priority === 'low'
          }
        ]"
      ></div>
    </div>

    <!-- 待办内容区域 -->
    <div class="todo-content-area">
      <div class="todo-title-row">
        <span class="todo-title" :class="{ 'completed-todo': item.isCompleted }">
          {{ item.title }}
        </span>
        <el-tag 
          v-if="isOverdue && !item.isCompleted"
          type="danger" 
          size="small"
          class="overdue-tag"
        >
          🔴 逾期
        </el-tag>
      </div>
      
      <div class="todo-meta-info">
        <div class="todo-assigner">来自: {{ item.assignerName }}</div>
        <div class="todo-due-date">
          <el-icon><Calendar /></el-icon>
          截止: {{ formatDueDate(item.dueDate) }}
        </div>
      </div>
      
      <div class="todo-content-preview" v-if="item.content">
        {{ item.content.length > 50 ? item.content.substring(0, 50) + '...' : item.content }}
      </div>
    </div>

    <!-- 操作区域 -->
    <div class="todo-action-area">
      <el-checkbox 
        v-model="item.isCompleted"
        class="todo-checkbox"
        :disabled="item.isCompleted"
        @change="handleComplete"
      />
      <el-button 
        type="text" 
        size="small" 
        class="todo-detail-btn"
        @click="handleViewDetail"
      >
        详情
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Calendar } from '@element-plus/icons-vue'
import type { TodoNotificationItem } from '@/types/todo'

// Props定义
const props = defineProps<{
  item: TodoNotificationItem
}>()

// Emits定义
const emit = defineEmits<{
  complete: [id: number, completed: boolean]
  viewDetail: [item: TodoNotificationItem]
}>()

// 计算是否逾期
const isOverdue = computed(() => {
  const dueDate = new Date(props.item.dueDate)
  const now = new Date()
  return dueDate.getTime() < now.getTime() && !props.item.isCompleted
})

// 格式化截止日期
const formatDueDate = (dueDate: string): string => {
  const date = new Date(dueDate)
  const now = new Date()
  const diffTime = date.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays < 0) {
    return `逾期${Math.abs(diffDays)}天`
  } else if (diffDays === 0) {
    return '今天截止'
  } else if (diffDays === 1) {
    return '明天截止'
  } else if (diffDays <= 7) {
    return `${diffDays}天后截止`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

// 完成待办事件
const handleComplete = () => {
  emit('complete', props.item.id, props.item.isCompleted)
}

// 查看详情事件
const handleViewDetail = () => {
  emit('viewDetail', props.item)
}
</script>

<style scoped lang="scss">
.todo-notification-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(139, 92, 246, 0.1);
  margin-bottom: 8px;
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    background: rgba(139, 92, 246, 0.05);
    border-color: rgba(139, 92, 246, 0.2);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.1);
  }

  &.todo-completed {
    background: rgba(108, 117, 125, 0.1);
    opacity: 0.7;
    
    .todo-title {
      text-decoration: line-through;
      color: #6c757d;
    }
  }

  &.todo-overdue {
    background: rgba(239, 68, 68, 0.05);
    border-color: rgba(239, 68, 68, 0.2);
  }

  &.todo-high {
    border-left: 4px solid #EF4444;
  }

  &.todo-medium {
    border-left: 4px solid #F59E0B;
  }

  &.todo-low {
    border-left: 4px solid #10B981;
  }
}

.todo-priority-indicator {
  display: flex;
  align-items: center;
  margin-right: 12px;
  margin-top: 2px;
  min-width: 20px; /* 确保足够的空间 */
  justify-content: center;

  .priority-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: block; /* 强制显示 */
    position: relative; /* 确保z-index生效 */
    z-index: 1;
    
    /* 默认样式（防止无匹配类时不显示） */
    background-color: #9CA3AF;
    
    /* 高优先级 - 红色 */
    &.priority-high {
      background-color: #EF4444 !important;
      box-shadow: 0 0 6px rgba(239, 68, 68, 0.4);
    }
    
    /* 中优先级 - 橙色 */
    &.priority-medium {
      background-color: #F59E0B !important;
      box-shadow: 0 0 6px rgba(245, 158, 11, 0.4);
    }
    
    /* 低优先级 - 绿色 */
    &.priority-low {
      background-color: #10B981 !important;
      box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
    }
  }
}

.todo-content-area {
  flex: 1;
  min-width: 0;
}

.todo-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: #2d3748;
  line-height: 1.4;
  
  &.completed-todo {
    text-decoration: line-through;
    color: #6c757d;
  }
}

.overdue-tag {
  font-size: 10px;
  padding: 2px 6px;
  margin-left: 8px;
}

.todo-meta-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #718096;
}

.todo-assigner {
  color: #8B5CF6;
  font-weight: 500;
}

.todo-due-date {
  display: flex;
  align-items: center;
  gap: 4px;
}

.todo-content-preview {
  font-size: 12px;
  color: #718096;
  line-height: 1.4;
  margin-top: 4px;
}

.todo-action-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
}

.todo-checkbox {
  .el-checkbox__input.is-checked .el-checkbox__inner {
    background-color: #8B5CF6;
    border-color: #8B5CF6;
  }
}

.todo-detail-btn {
  color: #8B5CF6;
  font-size: 12px;
  padding: 4px 8px;
  
  &:hover {
    background-color: rgba(139, 92, 246, 0.1);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .todo-notification-item {
    padding: 10px;
  }
  
  .todo-meta-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .todo-action-area {
    flex-direction: row;
    align-items: center;
  }
}
</style>