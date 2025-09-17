<template>
  <div class="priority-test-container">
    <h3>🔍 优先级指示器测试页面</h3>
    <p>用于验证修复后的优先级圆点显示效果</p>
    
    <!-- 测试数据 -->
    <div class="test-items">
      <TodoNotificationItem
        v-for="item in testTodos"
        :key="item.id"
        :item="item"
        @complete="handleComplete"
        @view-detail="handleViewDetail"
      />
    </div>
    
    <!-- 调试信息 -->
    <div class="debug-panel">
      <h4>🛠️ 调试信息</h4>
      <div class="debug-item" v-for="item in testTodos" :key="item.id">
        <strong>{{ item.title }}</strong><br>
        - 优先级: {{ item.priority }}<br>
        - CSS类: <code>priority-{{ item.priority }}</code><br>
        - 预期颜色: {{ getPriorityColor(item.priority) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TodoNotificationItem from './TodoNotificationItem.vue'
import type { TodoNotificationItem as TodoItem } from '@/types/todo'

// 测试数据
const testTodos = ref<TodoItem[]>([
  {
    id: 1,
    title: '🔴 高优先级任务',
    content: '这是一个高优先级任务，应该显示红色圆点',
    level: 5,
    priority: 'high',
    dueDate: '2025-08-20',
    status: 'pending',
    assignerName: '测试用户',
    isCompleted: false
  },
  {
    id: 2,
    title: '🟠 中优先级任务',
    content: '这是一个中优先级任务，应该显示橙色圆点',
    level: 5,
    priority: 'medium',
    dueDate: '2025-08-21',
    status: 'pending',
    assignerName: '测试用户',
    isCompleted: false
  },
  {
    id: 3,
    title: '🟢 低优先级任务',
    content: '这是一个低优先级任务，应该显示绿色圆点',
    level: 5,
    priority: 'low',
    dueDate: '2025-08-22',
    status: 'pending',
    assignerName: '测试用户',
    isCompleted: false
  },
  {
    id: 4,
    title: '✅ 已完成高优先级任务',
    content: '这是一个已完成的高优先级任务，应该显示红色圆点',
    level: 5,
    priority: 'high',
    dueDate: '2025-08-19',
    status: 'completed',
    assignerName: '测试用户',
    isCompleted: true
  }
])

// 获取优先级颜色描述
const getPriorityColor = (priority: string) => {
  switch (priority) {
    case 'high': return '#EF4444 (红色)'
    case 'medium': return '#F59E0B (橙色)'
    case 'low': return '#10B981 (绿色)'
    default: return '#9CA3AF (灰色)'
  }
}

// 处理完成事件
const handleComplete = (id: number, completed: boolean) => {
  console.log('✅ 测试完成事件:', { id, completed })
  const todo = testTodos.value.find(item => item.id === id)
  if (todo) {
    todo.isCompleted = completed
    todo.status = completed ? 'completed' : 'pending'
  }
}

// 处理查看详情事件
const handleViewDetail = (item: TodoItem) => {
  console.log('👁️ 测试查看详情:', item.title)
}
</script>

<style scoped lang="scss">
.priority-test-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  
  h3 {
    color: #8B5CF6;
    margin-bottom: 10px;
  }
  
  p {
    color: #666;
    margin-bottom: 20px;
  }
}

.test-items {
  margin-bottom: 30px;
}

.debug-panel {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  
  h4 {
    color: #495057;
    margin-bottom: 15px;
  }
  
  .debug-item {
    margin-bottom: 15px;
    padding: 10px;
    background: white;
    border-radius: 4px;
    border-left: 3px solid #8B5CF6;
    
    code {
      background: #e9ecef;
      padding: 2px 4px;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
      font-size: 12px;
    }
  }
}
</style>