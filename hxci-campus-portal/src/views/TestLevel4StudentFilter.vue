<template>
  <div class="level4-test-page">
    <div class="header-section">
      <h1 class="page-title">第4层测试：核心学号过滤逻辑验证</h1>
      <p class="page-description">
        验证单一维度的学号过滤功能，确保指定学号的学生能够看到相关的待办通知。
      </p>
    </div>

    <!-- 测试控制面板 -->
    <el-card class="control-panel" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🎯 学号过滤测试控制台</span>
          <el-button @click="resetTest" type="primary" plain size="small">重置测试</el-button>
        </div>
      </template>
      
      <div class="filter-controls">
        <el-form :model="testForm" label-width="120px" @submit.prevent>
          <el-form-item label="学号过滤:">
            <el-input 
              v-model="testForm.studentId" 
              placeholder="输入学号，如: 2023010105"
              @input="handleStudentIdChange"
              style="width: 300px;"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
            <el-button 
              @click="applyFilter" 
              type="primary" 
              style="margin-left: 10px;"
            >
              应用过滤
            </el-button>
            <el-button 
              @click="clearFilter" 
              type="warning" 
              plain
            >
              清空过滤
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 测试状态显示 -->
      <div class="test-status">
        <el-alert
          :title="currentStatus"
          :type="alertType"
          :description="statusDescription"
          show-icon
          :closable="false"
        />
      </div>
    </el-card>

    <!-- 测试结果显示区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：过滤结果 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📋 过滤结果 ({{ filteredTodos.length }}条)</span>
              <el-tag v-if="testForm.studentId" type="primary" size="small">
                学号: {{ testForm.studentId }}
              </el-tag>
            </div>
          </template>
          
          <div v-if="isLoading" class="loading-state">
            <el-skeleton :rows="3" animated />
          </div>
          
          <div v-else-if="filteredTodos.length === 0" class="empty-state">
            <el-empty description="暂无匹配的待办通知">
              <el-button type="primary" @click="loadTestData">重新加载数据</el-button>
            </el-empty>
          </div>
          
          <div v-else class="todo-list">
            <div 
              v-for="todo in filteredTodos" 
              :key="todo.id" 
              class="todo-item"
              :class="{ 'completed': todo.isCompleted }"
            >
              <div class="todo-header">
                <h4 class="todo-title">{{ todo.title }}</h4>
                <div class="todo-meta">
                  <el-tag :type="getPriorityType(todo.priority)" size="small">
                    {{ getPriorityLabel(todo.priority) }}
                  </el-tag>
                  <el-tag :type="getStatusType(todo.status)" size="small">
                    {{ getStatusLabel(todo.status) }}
                  </el-tag>
                </div>
              </div>
              
              <p class="todo-content">{{ todo.content }}</p>
              
              <div class="todo-details">
                <div class="detail-row">
                  <span class="label">分配者:</span>
                  <span class="value">{{ todo.assignerName }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">截止时间:</span>
                  <span class="value">{{ formatDate(todo.dueDate) }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">目标学生:</span>
                  <span class="value target-students">
                    {{ getTargetStudentsList(todo.targetStudentIds) }}
                  </span>
                </div>
                <div v-if="testForm.studentId" class="detail-row highlight">
                  <span class="label">匹配状态:</span>
                  <span class="value">
                    <el-tag :type="isStudentMatched(todo, testForm.studentId) ? 'success' : 'danger'" size="small">
                      {{ isStudentMatched(todo, testForm.studentId) ? '✅ 匹配成功' : '❌ 不匹配' }}
                    </el-tag>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：调试信息 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>🔍 调试信息</span>
              <el-button @click="toggleDebugMode" type="info" plain size="small">
                {{ debugMode ? '关闭调试' : '开启调试' }}
              </el-button>
            </div>
          </template>
          
          <div class="debug-info">
            <div class="debug-section">
              <h5>📊 数据统计</h5>
              <ul class="stats-list">
                <li>原始待办数量: <strong>{{ rawTodosCount }}</strong></li>
                <li>过滤后数量: <strong>{{ filteredTodos.length }}</strong></li>
                <li>当前学号: <strong>{{ testForm.studentId || '未设置' }}</strong></li>
                <li>过滤状态: <strong>{{ filterStatus }}</strong></li>
              </ul>
            </div>
            
            <div v-if="debugMode" class="debug-section">
              <h5>🔧 过滤日志</h5>
              <div class="debug-logs">
                <div v-for="(log, index) in debugLogs" :key="index" class="debug-log">
                  <span class="log-time">{{ log.time }}</span>
                  <span class="log-message">{{ log.message }}</span>
                </div>
              </div>
            </div>
            
            <div class="debug-section">
              <h5>🧪 测试用例</h5>
              <div class="test-cases">
                <el-button 
                  v-for="testCase in testCases" 
                  :key="testCase.id"
                  @click="runTestCase(testCase)"
                  type="primary"
                  plain
                  size="small"
                  style="margin-bottom: 5px; width: 100%;"
                >
                  {{ testCase.name }}
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { User } from '@element-plus/icons-vue'
import { useTodoStore } from '@/stores/todo'
import type { TodoNotificationItem, TodoFilterOptions } from '@/types/todo'

// ================ 响应式数据 ================

const todoStore = useTodoStore()

const testForm = ref({
  studentId: ''
})

const isLoading = ref(false)
const debugMode = ref(true)
const debugLogs = ref<Array<{ time: string; message: string }>>([])

// 测试用例定义
const testCases = ref([
  {
    id: 'test1',
    name: '测试学号 2023010105',
    studentId: '2023010105',
    expectedCount: 5 // 预期匹配的待办数量
  },
  {
    id: 'test2', 
    name: '测试学号 2023010106',
    studentId: '2023010106',
    expectedCount: 2 // 预期匹配的待办数量
  },
  {
    id: 'test3',
    name: '测试不存在的学号',
    studentId: '9999999999',
    expectedCount: 0
  },
  {
    id: 'test4',
    name: '清空学号过滤',
    studentId: '',
    expectedCount: 0 // 不指定学号时应该显示0条
  }
])

// ================ 计算属性 ================

const rawTodosCount = computed(() => todoStore.todoNotifications.length)

const filteredTodos = computed(() => {
  const filterOptions: TodoFilterOptions = {
    studentId: testForm.value.studentId
  }
  
  // 移除computed中的debug日志，避免无限循环
  // addDebugLog(`开始过滤，学号: ${testForm.value.studentId || '未设置'}`)
  
  const result = todoStore.getFilteredTodos(filterOptions)
  
  // addDebugLog(`过滤完成，结果数量: ${result.length}`)
  
  return result
})

const currentStatus = computed(() => {
  if (!testForm.value.studentId) {
    return '等待输入学号进行过滤测试'
  }
  
  const count = filteredTodos.value.length
  if (count === 0) {
    return `学号 ${testForm.value.studentId} 无匹配的待办通知`
  } else {
    return `学号 ${testForm.value.studentId} 匹配到 ${count} 条待办通知`
  }
})

const alertType = computed(() => {
  if (!testForm.value.studentId) return 'info'
  return filteredTodos.value.length > 0 ? 'success' : 'warning'
})

const statusDescription = computed(() => {
  if (!testForm.value.studentId) {
    return '请输入学号进行过滤测试，验证学号过滤逻辑是否正确工作。'
  }
  
  const count = filteredTodos.value.length
  if (count === 0) {
    return '该学号可能没有相关的待办通知，或者过滤逻辑存在问题。'
  } else {
    return `找到 ${count} 条相关待办通知，过滤逻辑工作正常。`
  }
})

const filterStatus = computed(() => {
  if (!testForm.value.studentId) return '无过滤'
  return filteredTodos.value.length > 0 ? '匹配成功' : '无匹配'
})

// ================ 方法定义 ================

const addDebugLog = (message: string) => {
  if (!debugMode.value) return
  
  const timestamp = new Date().toLocaleTimeString()
  debugLogs.value.unshift({
    time: timestamp,
    message: message
  })
  
  // 保持日志数量在20条以内
  if (debugLogs.value.length > 20) {
    debugLogs.value = debugLogs.value.slice(0, 20)
  }
}

const handleStudentIdChange = (value: string) => {
  addDebugLog(`学号输入变更: ${value}`)
}

const applyFilter = () => {
  addDebugLog('手动应用过滤器')
  // 触发重新计算
  testForm.value = { ...testForm.value }
}

const clearFilter = () => {
  testForm.value.studentId = ''
  addDebugLog('清空学号过滤')
}

const resetTest = () => {
  testForm.value.studentId = ''
  debugLogs.value = []
  addDebugLog('重置测试环境')
}

const loadTestData = async () => {
  isLoading.value = true
  addDebugLog('开始加载测试数据')
  
  try {
    await todoStore.initializeTodos()
    addDebugLog(`数据加载完成，总数: ${todoStore.todoNotifications.length}`)
  } catch (error) {
    addDebugLog(`数据加载失败: ${error}`)
  } finally {
    isLoading.value = false
  }
}

const toggleDebugMode = () => {
  debugMode.value = !debugMode.value
  if (debugMode.value) {
    addDebugLog('开启调试模式')
  }
}

const runTestCase = (testCase: any) => {
  testForm.value.studentId = testCase.studentId
  addDebugLog(`运行测试用例: ${testCase.name}，预期结果: ${testCase.expectedCount}条`)
  
  // 等待一下让过滤生效
  setTimeout(() => {
    const actualCount = filteredTodos.value.length
    const passed = actualCount === testCase.expectedCount
    
    addDebugLog(`测试结果: ${passed ? '✅ 通过' : '❌ 失败'} (预期:${testCase.expectedCount}, 实际:${actualCount})`)
  }, 100)
}

// ================ 工具方法 ================

const getPriorityType = (priority: string) => {
  switch (priority) {
    case 'high': return 'danger'
    case 'medium': return 'warning'
    case 'low': return 'info'
    default: return 'info'
  }
}

const getPriorityLabel = (priority: string) => {
  switch (priority) {
    case 'high': return '高优先级'
    case 'medium': return '中优先级'
    case 'low': return '低优先级'
    default: return '未知'
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'completed': return 'success'
    case 'pending': return 'warning'
    case 'overdue': return 'danger'
    default: return 'info'
  }
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'completed': return '已完成'
    case 'pending': return '待处理'
    case 'overdue': return '已逾期'
    default: return '未知'
  }
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}

const getTargetStudentsList = (targetStudentIds: any) => {
  if (!targetStudentIds) return '未设置'
  
  try {
    let ids: string[] = []
    if (typeof targetStudentIds === 'string') {
      ids = JSON.parse(targetStudentIds)
    } else if (Array.isArray(targetStudentIds)) {
      ids = targetStudentIds
    }
    
    return ids.join(', ')
  } catch (error) {
    return '解析失败'
  }
}

const isStudentMatched = (todo: TodoNotificationItem, studentId: string) => {
  if (!todo.targetStudentIds || !studentId) return false
  
  try {
    let ids: string[] = []
    if (typeof todo.targetStudentIds === 'string') {
      ids = JSON.parse(todo.targetStudentIds)
    } else if (Array.isArray(todo.targetStudentIds)) {
      ids = todo.targetStudentIds
    }
    
    return ids.includes(studentId)
  } catch (error) {
    return false
  }
}

// ================ 生命周期 ================

onMounted(async () => {
  addDebugLog('第4层测试页面加载完成')
  await loadTestData()
})

// 监听学号变化
watch(() => testForm.value.studentId, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    addDebugLog(`学号过滤条件变更: "${oldVal}" -> "${newVal}"`)
    
    // 延迟记录过滤结果，避免计算属性中的副作用
    nextTick(() => {
      const count = filteredTodos.value.length
      addDebugLog(`过滤完成，学号: ${newVal || '未设置'}，结果数量: ${count}`)
    })
  }
}, { immediate: false })

// 监听过滤结果变化（用于调试）
watch(() => filteredTodos.value.length, (newCount) => {
  // 只在调试模式下记录，且避免初始化时的日志
  if (debugMode.value && testForm.value.studentId) {
    console.log(`过滤结果更新: ${newCount}条`)
  }
}, { flush: 'post' })

</script>

<style scoped>
.level4-test-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-section {
  margin-bottom: 24px;
  text-align: center;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 8px;
}

.page-description {
  font-size: 16px;
  color: #7f8c8d;
  margin-bottom: 0;
}

.control-panel {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-controls {
  margin-bottom: 20px;
}

.test-status {
  margin-top: 16px;
}

.todo-list {
  max-height: 600px;
  overflow-y: auto;
}

.todo-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  transition: all 0.3s;
}

.todo-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.todo-item.completed {
  opacity: 0.7;
  background: #f9f9f9;
}

.todo-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.todo-title {
  font-size: 16px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
  flex: 1;
}

.todo-meta {
  display: flex;
  gap: 8px;
}

.todo-content {
  color: #5a6c7d;
  line-height: 1.6;
  margin-bottom: 16px;
}

.todo-details {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.detail-row {
  display: flex;
  margin-bottom: 6px;
}

.detail-row.highlight {
  background: #f0f9ff;
  padding: 4px 8px;
  border-radius: 4px;
  border-left: 3px solid #409EFF;
}

.detail-row .label {
  width: 80px;
  font-weight: bold;
  color: #666;
}

.detail-row .value {
  color: #333;
}

.target-students {
  font-family: monospace;
  background: #f5f5f5;
  padding: 2px 4px;
  border-radius: 3px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 40px;
}

.debug-info {
  max-height: 500px;
  overflow-y: auto;
}

.debug-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.debug-section:last-child {
  border-bottom: none;
}

.debug-section h5 {
  margin: 0 0 12px 0;
  color: #409EFF;
  font-size: 14px;
  font-weight: bold;
}

.stats-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.stats-list li {
  padding: 4px 0;
  font-size: 13px;
  color: #666;
}

.debug-logs {
  max-height: 200px;
  overflow-y: auto;
  background: #f8f9fa;
  border-radius: 4px;
  padding: 8px;
}

.debug-log {
  font-size: 12px;
  margin-bottom: 4px;
  display: flex;
  gap: 8px;
}

.log-time {
  color: #999;
  font-family: monospace;
  min-width: 60px;
}

.log-message {
  color: #333;
}

.test-cases {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

@media (max-width: 768px) {
  .level4-test-page {
    padding: 10px;
  }
  
  .todo-header {
    flex-direction: column;
    gap: 8px;
  }
  
  .detail-row {
    flex-direction: column;
  }
  
  .detail-row .label {
    width: auto;
    margin-bottom: 2px;
  }
}
</style>