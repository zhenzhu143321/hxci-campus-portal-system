<template>
  <div class="level5-test-page">
    <div class="header-section">
      <h1 class="page-title">第5层测试：年级班级过滤逻辑验证</h1>
      <p class="page-description">
        验证年级和班级过滤功能，确保同年级学生和同班学生能够正确看到相关的待办通知。
      </p>
    </div>

    <!-- 测试控制面板 -->
    <el-card class="control-panel" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🎓 年级班级过滤测试控制台</span>
          <el-button @click="resetTest" type="primary" plain size="small">重置测试</el-button>
        </div>
      </template>
      
      <div class="filter-controls">
        <el-form :model="testForm" label-width="120px" @submit.prevent>
          <el-form-item label="年级过滤:">
            <el-input 
              v-model="testForm.grade" 
              placeholder="输入年级，如: 2023级"
              @input="handleGradeChange"
              style="width: 300px;"
            >
              <template #prefix>
                <el-icon><Medal /></el-icon>
              </template>
            </el-input>
            <el-button 
              @click="applyGradeFilter" 
              type="primary" 
              style="margin-left: 10px;"
            >
              应用年级过滤
            </el-button>
          </el-form-item>

          <el-form-item label="班级过滤:">
            <el-input 
              v-model="testForm.className" 
              placeholder="输入班级，如: 计算机1班"
              @input="handleClassChange"
              style="width: 300px;"
            >
              <template #prefix>
                <el-icon><School /></el-icon>
              </template>
            </el-input>
            <el-button 
              @click="applyClassFilter" 
              type="success" 
              style="margin-left: 10px;"
            >
              应用班级过滤
            </el-button>
          </el-form-item>

          <el-form-item label="组合过滤:">
            <el-button 
              @click="applyBothFilters" 
              type="warning"
              :disabled="!testForm.grade && !testForm.className"
            >
              同时应用年级+班级过滤
            </el-button>
            <el-button 
              @click="clearAllFilters" 
              type="danger" 
              plain
            >
              清空所有过滤
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
              <div class="filter-tags">
                <el-tag v-if="testForm.grade" type="primary" size="small" closable @close="clearGradeFilter">
                  年级: {{ testForm.grade }}
                </el-tag>
                <el-tag v-if="testForm.className" type="success" size="small" closable @close="clearClassFilter">
                  班级: {{ testForm.className }}
                </el-tag>
              </div>
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
                  <span class="label">目标年级:</span>
                  <span class="value target-grades">
                    {{ getTargetGradesList(todo.targetGrades) }}
                  </span>
                </div>
                <div class="detail-row">
                  <span class="label">目标班级:</span>
                  <span class="value target-classes">
                    {{ getTargetClassesList(todo.targetClasses) }}
                  </span>
                </div>
                <div v-if="testForm.grade" class="detail-row highlight grade-match">
                  <span class="label">年级匹配:</span>
                  <span class="value">
                    <el-tag :type="isGradeMatched(todo, testForm.grade) ? 'success' : 'danger'" size="small">
                      {{ isGradeMatched(todo, testForm.grade) ? '✅ 年级匹配' : '❌ 年级不匹配' }}
                    </el-tag>
                  </span>
                </div>
                <div v-if="testForm.className" class="detail-row highlight class-match">
                  <span class="label">班级匹配:</span>
                  <span class="value">
                    <el-tag :type="isClassMatched(todo, testForm.className) ? 'success' : 'danger'" size="small">
                      {{ isClassMatched(todo, testForm.className) ? '✅ 班级匹配' : '❌ 班级不匹配' }}
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
                <li>当前年级: <strong>{{ testForm.grade || '未设置' }}</strong></li>
                <li>当前班级: <strong>{{ testForm.className || '未设置' }}</strong></li>
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
                  :type="testCase.type"
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
import { Medal, School } from '@element-plus/icons-vue'
import { useTodoStore } from '@/stores/todo'
import type { TodoNotificationItem, TodoFilterOptions } from '@/types/todo'
import api from '@/utils/request'

// ================ 响应式数据 ================

const todoStore = useTodoStore()

const testForm = ref({
  grade: '',
  className: ''
})

const isLoading = ref(false)
const debugMode = ref(true)
const debugLogs = ref<Array<{ time: string; message: string }>>([])

// 测试用例定义
const testCases = ref([
  {
    id: 'test-grade-2023',
    name: '测试2023级年级过滤',
    type: 'primary',
    grade: '2023级',
    className: '',
    expectedCount: 3
  },
  {
    id: 'test-grade-2024',
    name: '测试2024级年级过滤',
    type: 'primary', 
    grade: '2024级',
    className: '',
    expectedCount: 2
  },
  {
    id: 'test-class-cs1',
    name: '测试计算机1班过滤',
    type: 'success',
    grade: '',
    className: '计算机1班',
    expectedCount: 4
  },
  {
    id: 'test-class-cs2',
    name: '测试计算机2班过滤',
    type: 'success',
    grade: '',
    className: '计算机2班',
    expectedCount: 2
  },
  {
    id: 'test-both',
    name: '测试2023级+计算机1班',
    type: 'warning',
    grade: '2023级',
    className: '计算机1班',
    expectedCount: 2
  },
  {
    id: 'test-clear',
    name: '清空所有过滤',
    type: 'info',
    grade: '',
    className: '',
    expectedCount: 0
  }
])

// ================ 计算属性 ================

const rawTodosCount = computed(() => todoStore.todoNotifications.length)

const filteredTodos = computed(() => {
  const filterOptions: TodoFilterOptions = {
    grade: testForm.value.grade,
    className: testForm.value.className
  }
  
  const result = todoStore.getFilteredTodos(filterOptions)
  return result
})

const currentStatus = computed(() => {
  const hasGrade = !!testForm.value.grade
  const hasClass = !!testForm.value.className
  
  if (!hasGrade && !hasClass) {
    return '等待设置年级或班级过滤条件'
  }
  
  const count = filteredTodos.value.length
  let status = ''
  
  if (hasGrade && hasClass) {
    status = `年级"${testForm.value.grade}" + 班级"${testForm.value.className}"`
  } else if (hasGrade) {
    status = `年级"${testForm.value.grade}"`
  } else {
    status = `班级"${testForm.value.className}"`
  }
  
  if (count === 0) {
    return `${status} 无匹配的待办通知`
  } else {
    return `${status} 匹配到 ${count} 条待办通知`
  }
})

const alertType = computed(() => {
  const hasFilter = testForm.value.grade || testForm.value.className
  if (!hasFilter) return 'info'
  return filteredTodos.value.length > 0 ? 'success' : 'warning'
})

const statusDescription = computed(() => {
  const hasGrade = !!testForm.value.grade
  const hasClass = !!testForm.value.className
  
  if (!hasGrade && !hasClass) {
    return '请设置年级或班级过滤条件，验证第5层过滤逻辑是否正确工作。'
  }
  
  const count = filteredTodos.value.length
  if (count === 0) {
    return '该过滤条件可能没有相关的待办通知，或者过滤逻辑存在问题。'
  } else {
    let desc = `找到 ${count} 条相关待办通知，`
    if (hasGrade && hasClass) {
      desc += '年级+班级组合过滤逻辑工作正常。'
    } else if (hasGrade) {
      desc += '年级过滤逻辑工作正常。'
    } else {
      desc += '班级过滤逻辑工作正常。'
    }
    return desc
  }
})

const filterStatus = computed(() => {
  const hasGrade = !!testForm.value.grade
  const hasClass = !!testForm.value.className
  
  if (!hasGrade && !hasClass) return '无过滤'
  
  const count = filteredTodos.value.length
  if (count > 0) {
    if (hasGrade && hasClass) return '组合匹配成功'
    else if (hasGrade) return '年级匹配成功'
    else return '班级匹配成功'
  } else {
    return '无匹配'
  }
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

const handleGradeChange = (value: string) => {
  addDebugLog(`年级输入变更: ${value}`)
}

const handleClassChange = (value: string) => {
  addDebugLog(`班级输入变更: ${value}`)
}

const applyGradeFilter = () => {
  testForm.value.className = '' // 清空班级过滤
  addDebugLog(`应用年级过滤: ${testForm.value.grade}`)
  // 触发重新计算
  testForm.value = { ...testForm.value }
}

const applyClassFilter = () => {
  testForm.value.grade = '' // 清空年级过滤
  addDebugLog(`应用班级过滤: ${testForm.value.className}`)
  // 触发重新计算
  testForm.value = { ...testForm.value }
}

const applyBothFilters = () => {
  addDebugLog(`应用组合过滤: 年级=${testForm.value.grade}, 班级=${testForm.value.className}`)
  // 触发重新计算
  testForm.value = { ...testForm.value }
}

const clearGradeFilter = () => {
  testForm.value.grade = ''
  addDebugLog('清空年级过滤')
}

const clearClassFilter = () => {
  testForm.value.className = ''
  addDebugLog('清空班级过滤')
}

const clearAllFilters = () => {
  testForm.value.grade = ''
  testForm.value.className = ''
  addDebugLog('清空所有过滤条件')
}

const resetTest = () => {
  testForm.value.grade = ''
  testForm.value.className = ''
  debugLogs.value = []
  addDebugLog('重置第5层测试环境')
}

const loadTestData = async () => {
  isLoading.value = true
  addDebugLog('开始加载测试数据')
  
  try {
    // 🔥 直接调用API获取最新数据，避免缓存问题
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (!token) {
      throw new Error('用户未登录，无法获取待办数据')
    }
    
    addDebugLog('🌐 直接调用API获取待办数据...')
    const response = await api.get('/admin-api/test/todo-new/api/my-list')
    
    if (response.data.code === 0 && response.data.data?.todos) {
      const backendTodos = response.data.data.todos
      addDebugLog(`✅ API返回数据成功: ${backendTodos.length}条待办`)
      
      // 检查年级班级数据
      backendTodos.forEach((todo, index) => {
        addDebugLog(`📋 待办${index + 1}: ID=${todo.id}, 标题=${todo.title}`)  
        addDebugLog(`   年级: ${todo.targetGrades || '未设置'}`)  
        addDebugLog(`   班级: ${todo.targetClasses || '未设置'}`)  
        addDebugLog(`   学生ID: ${todo.targetStudentIds || '未设置'}`)
      })
      
      // 强制更新todoStore数据
      const convertedTodos = backendTodos.map(item => ({
        id: parseInt(item.id.toString()),
        title: item.title,
        content: item.content,
        level: 5, // 固定Level 5
        priority: (item.priority === 'high' ? 'high' : item.priority === 'medium' ? 'medium' : 'low') as 'high' | 'medium' | 'low',
        dueDate: item.dueDate,
        status: item.status as 'pending' | 'completed' | 'overdue',
        assignerName: item.assignerName,
        isCompleted: item.isCompleted,
        targetStudentIds: item.targetStudentIds, // 【第4层】目标学生ID列表
        targetGrades: item.targetGrades, // 【第5层新增】目标年级列表
        targetClasses: item.targetClasses // 【第5层新增】目标班级列表
      }))
      
      todoStore.todoNotifications = convertedTodos
      todoStore.lastUpdateTime = new Date()
      
      addDebugLog(`🔄 强制更新todoStore完成，总数: ${todoStore.todoNotifications.length}`)
    } else {
      throw new Error(`API返回错误: ${response.data.msg || '未知错误'}`)
    }
    
  } catch (error) {
    addDebugLog(`❌ 数据加载失败: ${error}`)
    // 降级到Store初始化
    await todoStore.initializeTodos()
    addDebugLog(`🔄 降级加载完成，总数: ${todoStore.todoNotifications.length}`)
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
  testForm.value.grade = testCase.grade
  testForm.value.className = testCase.className
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

const getTargetGradesList = (targetGrades: any) => {
  if (!targetGrades) return '未设置'
  
  try {
    let grades: string[] = []
    if (typeof targetGrades === 'string') {
      grades = JSON.parse(targetGrades)
    } else if (Array.isArray(targetGrades)) {
      grades = targetGrades
    }
    
    return grades.length > 0 ? grades.join(', ') : '未设置'
  } catch (error) {
    return '解析失败'
  }
}

const getTargetClassesList = (targetClasses: any) => {
  if (!targetClasses) return '未设置'
  
  try {
    let classes: string[] = []
    if (typeof targetClasses === 'string') {
      classes = JSON.parse(targetClasses)
    } else if (Array.isArray(targetClasses)) {
      classes = targetClasses
    }
    
    return classes.length > 0 ? classes.join(', ') : '未设置'
  } catch (error) {
    return '解析失败'
  }
}

const isGradeMatched = (todo: TodoNotificationItem, grade: string) => {
  if (!todo.targetGrades || !grade) return false
  
  try {
    let grades: string[] = []
    if (typeof todo.targetGrades === 'string') {
      grades = JSON.parse(todo.targetGrades)
    } else if (Array.isArray(todo.targetGrades)) {
      grades = todo.targetGrades
    }
    
    return grades.includes(grade)
  } catch (error) {
    return false
  }
}

const isClassMatched = (todo: TodoNotificationItem, className: string) => {
  if (!todo.targetClasses || !className) return false
  
  try {
    let classes: string[] = []
    if (typeof todo.targetClasses === 'string') {
      classes = JSON.parse(todo.targetClasses)
    } else if (Array.isArray(todo.targetClasses)) {
      classes = todo.targetClasses
    }
    
    return classes.includes(className)
  } catch (error) {
    return false
  }
}

// ================ 生命周期 ================

onMounted(async () => {
  addDebugLog('🚀 第5层测试页面加载完成')
  addDebugLog('🔍 开始诊断前端数据源不一致问题...')
  
  // 清空所有缓存，确保获取最新数据
  todoStore.todoNotifications = []
  addDebugLog('🧹 清空todoStore缓存')
  
  await loadTestData()
  
  // 显示数据源诊断信息
  addDebugLog('📊 数据源诊断:')
  addDebugLog(`   - todoStore数据: ${todoStore.todoNotifications.length}条`)
  addDebugLog(`   - filteredTodos数据: ${filteredTodos.value.length}条`)
  
  if (todoStore.todoNotifications.length > 0) {
    addDebugLog('📋 待办数据详情:')
    todoStore.todoNotifications.forEach((todo, index) => {
      addDebugLog(`   ${index + 1}. ID=${todo.id}, 标题=${todo.title}`)
      addDebugLog(`      年级=${todo.targetGrades || 'null'}, 班级=${todo.targetClasses || 'null'}`)
    })
  }
})

// 监听过滤条件变化
watch(() => [testForm.value.grade, testForm.value.className], ([newGrade, newClass], [oldGrade, oldClass]) => {
  if (newGrade !== oldGrade || newClass !== oldClass) {
    addDebugLog(`过滤条件变更: 年级"${oldGrade}"->"${newGrade}", 班级"${oldClass}"->"${newClass}"`)
    
    // 延迟记录过滤结果，避免计算属性中的副作用
    nextTick(() => {
      const count = filteredTodos.value.length
      addDebugLog(`过滤完成，年级: ${newGrade || '未设置'}，班级: ${newClass || '未设置'}，结果数量: ${count}`)
    })
  }
}, { immediate: false })

// 监听过滤结果变化（用于调试）
watch(() => filteredTodos.value.length, (newCount) => {
  // 只在调试模式下记录，且避免初始化时的日志
  if (debugMode.value && (testForm.value.grade || testForm.value.className)) {
    console.log(`第5层过滤结果更新: ${newCount}条`)
  }
}, { flush: 'post' })

</script>

<style scoped>
.level5-test-page {
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

.filter-tags {
  display: flex;
  gap: 8px;
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

.detail-row.grade-match {
  background: #f0f9ff;
  border-left-color: #409EFF;
}

.detail-row.class-match {
  background: #f0f8f0;
  border-left-color: #67C23A;
}

.detail-row .label {
  width: 80px;
  font-weight: bold;
  color: #666;
}

.detail-row .value {
  color: #333;
}

.target-grades, .target-classes {
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
  .level5-test-page {
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