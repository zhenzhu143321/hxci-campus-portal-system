<template>
  <div class="todo-management-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h1><el-icon><Document /></el-icon>待办管理</h1>
          <p class="header-description">管理您的所有待办任务，高效完成学习和生活安排</p>
        </div>
        <div class="header-stats">
          <el-card class="stat-card pending">
            <div class="stat-content">
              <div class="stat-number">{{ pendingCount }}</div>
              <div class="stat-label">待完成</div>
            </div>
          </el-card>
          <el-card class="stat-card overdue">
            <div class="stat-content">
              <div class="stat-number">{{ overdueCount }}</div>
              <div class="stat-label">逾期</div>
            </div>
          </el-card>
          <el-card class="stat-card completed">
            <div class="stat-content">
              <div class="stat-number">{{ completedCount }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 筛选和操作区域 -->
    <div class="filter-section">
      <div class="filter-left">
        <el-button-group>
          <el-button 
            :type="filterStatus === 'all' ? 'primary' : ''"
            @click="filterStatus = 'all'"
          >
            全部 ({{ todoStore.todoNotifications.length }})
          </el-button>
          <el-button 
            :type="filterStatus === 'pending' ? 'primary' : ''"
            @click="filterStatus = 'pending'"
          >
            待完成 ({{ pendingCount }})
          </el-button>
          <el-button 
            :type="filterStatus === 'overdue' ? 'primary' : ''"
            @click="filterStatus = 'overdue'"
          >
            逾期 ({{ overdueCount }})
          </el-button>
          <el-button 
            :type="filterStatus === 'completed' ? 'primary' : ''"
            @click="filterStatus = 'completed'"
          >
            已完成 ({{ completedCount }})
          </el-button>
        </el-button-group>
      </div>
      <div class="filter-right">
        <el-select v-model="sortBy" placeholder="排序方式" style="width: 140px;">
          <el-option label="按截止时间" value="dueDate" />
          <el-option label="按优先级" value="priority" />
          <el-option label="按创建时间" value="createTime" />
        </el-select>
        <el-button @click="goBack" icon="ArrowLeft">返回首页</el-button>
      </div>
    </div>

    <!-- 待办列表区域 -->
    <div class="todo-list-section">
      <div v-if="filteredTodos.length === 0" class="empty-state">
        <el-empty 
          :description="getEmptyDescription()"
          :image-size="100"
        >
          <template #image>
            <el-icon :size="100" style="color: #8B5CF6;"><Document /></el-icon>
          </template>
        </el-empty>
      </div>
      
      <div v-else class="todo-grid">
        <TodoNotificationWidget
          :notifications="paginatedTodos"
          :max-display-items="pageSize"
          display-mode="management"
          @complete="handleTodoComplete"
          @view-all="() => {}"
        />
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="filteredTodos.length > pageSize" class="pagination-section">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredTodos.length"
        layout="total, prev, pager, next, jumper"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TodoNotificationWidget from '@/components/TodoNotificationWidget.vue'
import type { TodoNotificationItem } from '@/types/todo'
import { useTodoStore } from '@/stores/todo'

// 路由实例
const router = useRouter()
const todoStore = useTodoStore()

// 筛选和分页状态
const filterStatus = ref<'all' | 'pending' | 'overdue' | 'completed'>('all')
const sortBy = ref<'dueDate' | 'priority' | 'createTime'>('dueDate')
const currentPage = ref(1)
const pageSize = ref(10)

// 计算属性 - 统计数据 (使用store)
const pendingCount = computed(() => todoStore.pendingCount)
const overdueCount = computed(() => todoStore.overdueCount)
const completedCount = computed(() => todoStore.completedCount)

// 判断是否逾期
const isOverdue = (todo: TodoNotificationItem): boolean => {
  if (todo.isCompleted) return false
  return new Date(todo.dueDate) < new Date()
}

// 计算属性 - 筛选后的待办列表
const filteredTodos = computed(() => {
  let filtered = todoStore.todoNotifications

  // 状态筛选
  switch (filterStatus.value) {
    case 'pending':
      filtered = filtered.filter(todo => !todo.isCompleted && !isOverdue(todo))
      break
    case 'overdue':
      filtered = filtered.filter(todo => !todo.isCompleted && isOverdue(todo))
      break
    case 'completed':
      filtered = filtered.filter(todo => todo.isCompleted)
      break
    case 'all':
    default:
      // 显示全部
      break
  }

  // 排序
  return filtered.sort((a, b) => {
    switch (sortBy.value) {
      case 'dueDate':
        return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime()
      case 'priority':
        const priorityOrder = { high: 3, medium: 2, low: 1 }
        return priorityOrder[b.priority] - priorityOrder[a.priority]
      case 'createTime':
      default:
        return b.id - a.id // 按ID倒序，模拟创建时间
    }
  })
})

// 分页后的数据
const paginatedTodos = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTodos.value.slice(start, end)
})

// 获取空状态描述
const getEmptyDescription = () => {
  switch (filterStatus.value) {
    case 'pending':
      return '暂无待处理的任务'
    case 'overdue':
      return '暂无逾期任务'
    case 'completed':
      return '暂无已完成的任务'
    default:
      return '暂无待办任务'
  }
}

// 事件处理函数
const handleTodoComplete = (id: number, completed: boolean) => {
  todoStore.updateTodoStatus(id, completed)
  ElMessage.success(completed ? '待办已标记为完成' : '待办已标记为未完成')
}

const handleViewDetail = (item: TodoNotificationItem) => {
  // 这里可以实现详情查看逻辑，比如打开弹窗或跳转到详情页
  console.log('查看待办详情:', item.title)
  ElMessage.info('详情查看功能开发中...')
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const goBack = () => {
  router.push('/home')
}

// 组件挂载时初始化数据
onMounted(() => {
  // 使用store初始化待办数据
  todoStore.initializeTodos()
})
</script>

<style scoped lang="scss">
.todo-management-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
  
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 24px;
  }
  
  .header-left {
    flex: 1;
    
    h1 {
      color: white;
      font-size: 28px;
      font-weight: 600;
      margin: 0 0 8px 0;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .header-description {
      color: rgba(255, 255, 255, 0.8);
      font-size: 16px;
      margin: 0;
    }
  }
  
  .header-stats {
    display: flex;
    gap: 16px;
    
    .stat-card {
      min-width: 100px;
      
      .stat-content {
        text-align: center;
        
        .stat-number {
          font-size: 24px;
          font-weight: 700;
          margin-bottom: 4px;
        }
        
        .stat-label {
          font-size: 12px;
          color: #666;
        }
      }
      
      &.pending .stat-number {
        color: #8B5CF6;
      }
      
      &.overdue .stat-number {
        color: #EF4444;
      }
      
      &.completed .stat-number {
        color: #10B981;
      }
    }
  }
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  
  .filter-left {
    .el-button-group {
      .el-button {
        font-size: 14px;
      }
    }
  }
  
  .filter-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.todo-list-section {
  .empty-state {
    background: rgba(255, 255, 255, 0.95);
    border-radius: 12px;
    padding: 40px;
    text-align: center;
    backdrop-filter: blur(10px);
  }
  
  .todo-grid {
    display: grid;
    gap: 12px;
    
    // 每个待办项占满宽度
    .todo-notification-item {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
    }
  }
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

// 响应式设计
@media (max-width: 768px) {
  .todo-management-page {
    padding: 16px;
  }
  
  .page-header {
    .header-content {
      flex-direction: column;
      gap: 16px;
    }
    
    .header-stats {
      width: 100%;
      justify-content: space-around;
      
      .stat-card {
        flex: 1;
        min-width: auto;
      }
    }
  }
  
  .filter-section {
    flex-direction: column;
    gap: 16px;
    
    .filter-left, .filter-right {
      width: 100%;
      justify-content: center;
    }
  }
}
</style>