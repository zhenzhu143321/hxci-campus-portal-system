<!--
  第3层TodoNotificationContainer测试页面
  
  @description 用于测试第3层实现的完整功能，包括真实数据获取、用户认证集成、状态管理等
  @author Frontend Development Team
  @date 2025-08-22
  
  测试用例:
  ✅ Props驱动的组件配置
  ✅ 真实API数据获取和显示
  ✅ Loading/Error状态处理
  ✅ 用户认证系统集成
  ✅ 数据格式转换验证
  ✅ 交互功能测试（完成、查看）
-->

<template>
  <div class="todo-container-test-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>📋 第3层TodoNotificationContainer测试</h1>
      <div class="page-meta">
        <el-tag type="success">第3层：数据集成层</el-tag>
        <el-tag type="info">{{ testTime }}</el-tag>
      </div>
    </div>
    
    <!-- 测试控制面板 -->
    <div class="control-panel">
      <div class="control-section">
        <h3>🔧 测试配置</h3>
        <div class="control-grid">
          <div class="control-item">
            <label>学生ID:</label>
            <el-input 
              v-model="testConfig.studentId" 
              placeholder="输入学生ID" 
              size="small"
              clearable
            />
          </div>
          
          <div class="control-item">
            <label>年级:</label>
            <el-select v-model="testConfig.grade" placeholder="选择年级" size="small" clearable>
              <el-option label="2021级" value="2021" />
              <el-option label="2022级" value="2022" />
              <el-option label="2023级" value="2023" />
              <el-option label="2024级" value="2024" />
            </el-select>
          </div>
          
          <div class="control-item">
            <label>班级:</label>
            <el-input 
              v-model="testConfig.className" 
              placeholder="输入班级名称" 
              size="small"
              clearable
            />
          </div>
          
          <div class="control-item">
            <label>用户角色:</label>
            <el-select v-model="testConfig.userRole" placeholder="选择角色" size="small" clearable>
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="班主任" value="CLASS_TEACHER" />
              <el-option label="教务主任" value="ACADEMIC_ADMIN" />
              <el-option label="校长" value="PRINCIPAL" />
              <el-option label="系统管理员" value="SYSTEM_ADMIN" />
            </el-select>
          </div>
          
          <div class="control-item">
            <label>显示模式:</label>
            <el-select v-model="testConfig.displayMode" placeholder="选择显示模式" size="small">
              <el-option label="卡片模式" value="card" />
              <el-option label="列表模式" value="list" />
              <el-option label="紧凑模式" value="compact" />
            </el-select>
          </div>
          
          <div class="control-item">
            <label>最大显示数:</label>
            <el-input-number 
              v-model="testConfig.maxItems" 
              :min="1" 
              :max="50" 
              size="small"
            />
          </div>
          
          <div class="control-item">
            <label>显示已完成:</label>
            <el-switch v-model="testConfig.showCompleted" size="small" />
          </div>
          
          <div class="control-item">
            <label>调试模式:</label>
            <el-switch v-model="testConfig.showDebugInfo" size="small" />
          </div>
          
          <div class="control-item">
            <label>自动刷新:</label>
            <el-switch v-model="testConfig.enableAutoRefresh" size="small" />
          </div>
          
          <div class="control-item">
            <label>刷新间隔(秒):</label>
            <el-input-number 
              v-model="testConfig.autoRefreshInterval" 
              :min="5" 
              :max="300" 
              size="small"
            />
          </div>
        </div>
      </div>
      
      <!-- 测试操作按钮 -->
      <div class="control-section">
        <h3>⚡ 测试操作</h3>
        <div class="button-group">
          <el-button type="primary" @click="applyTestConfig">
            <el-icon><Setting /></el-icon>
            应用配置
          </el-button>
          
          <el-button type="success" @click="resetTestConfig">
            <el-icon><RefreshLeft /></el-icon>
            重置配置
          </el-button>
          
          <el-button type="warning" @click="testErrorState">
            <el-icon><Warning /></el-icon>
            测试错误状态
          </el-button>
          
          <el-button type="info" @click="testEmptyState">
            <el-icon><Document /></el-icon>
            测试空状态
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 第3层组件测试区域 -->
    <div class="test-area">
      <div class="test-header">
        <h3>🧪 第3层组件测试</h3>
        <el-tag :type="componentStatus.type" size="small">
          {{ componentStatus.text }}
        </el-tag>
      </div>
      
      <!-- 使用第3层TodoNotificationContainer组件 -->
      <TodoNotificationContainer
        :student-id="appliedConfig.studentId"
        :grade="appliedConfig.grade"
        :class-name="appliedConfig.className"
        :user-role="appliedConfig.userRole"
        :display-mode="appliedConfig.displayMode"
        :max-items="appliedConfig.maxItems"
        :show-completed="appliedConfig.showCompleted"
        :show-debug-info="appliedConfig.showDebugInfo"
        :enable-auto-refresh="appliedConfig.enableAutoRefresh"
        :auto-refresh-interval="appliedConfig.autoRefreshInterval"
        @todo-complete="onTodoComplete"
        @todo-view="onTodoView"
        @data-loaded="onDataLoaded"
        @data-error="onDataError"
      />
    </div>
    
    <!-- 事件日志区域 -->
    <div class="event-log">
      <div class="log-header">
        <h3>📝 事件日志</h3>
        <el-button size="small" @click="clearEventLog">
          <el-icon><Delete /></el-icon>
          清空日志
        </el-button>
      </div>
      
      <div class="log-content">
        <div 
          v-for="(log, index) in eventLogs" 
          :key="index" 
          class="log-item"
          :class="`log-${log.type}`"
        >
          <span class="log-time">{{ log.time }}</span>
          <span class="log-type">{{ log.type.toUpperCase() }}</span>
          <span class="log-message">{{ log.message }}</span>
          <span class="log-data" v-if="log.data">{{ JSON.stringify(log.data) }}</span>
        </div>
        
        <div v-if="eventLogs.length === 0" class="log-empty">
          暂无事件日志
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Setting, RefreshLeft, Warning, Document, Delete
} from '@element-plus/icons-vue'
import TodoNotificationContainer from '@/components/todo/TodoNotificationContainer.vue'
import type { UserRole, TodoDisplayMode, TodoItem } from '@/types/todo'

/**
 * 测试配置
 */
interface TestConfig {
  studentId?: string
  grade?: string
  className?: string
  userRole?: UserRole
  displayMode: TodoDisplayMode
  maxItems: number
  showCompleted: boolean
  showDebugInfo: boolean
  enableAutoRefresh: boolean
  autoRefreshInterval: number
}

/**
 * 事件日志
 */
interface EventLog {
  time: string
  type: 'info' | 'success' | 'warning' | 'error'
  message: string
  data?: any
}

// ================ 响应式数据 ================

/** 测试页面创建时间 */
const testTime = ref<string>(new Date().toLocaleString())

/** 测试配置（可修改） */
const testConfig = reactive<TestConfig>({
  studentId: '',
  grade: '',
  className: '',
  userRole: undefined,
  displayMode: 'card',
  maxItems: 10,
  showCompleted: false,
  showDebugInfo: true,
  enableAutoRefresh: false,
  autoRefreshInterval: 30
})

/** 应用的配置（实际传给组件的） */
const appliedConfig = reactive<TestConfig>({ ...testConfig })

/** 事件日志 */
const eventLogs = ref<EventLog[]>([])

/** 组件状态 */
const componentStatus = computed(() => {
  if (eventLogs.value.length === 0) {
    return { type: 'info', text: '等待测试' }
  }
  
  const lastLog = eventLogs.value[eventLogs.value.length - 1]
  switch (lastLog.type) {
    case 'success':
      return { type: 'success', text: '运行正常' }
    case 'error':
      return { type: 'danger', text: '运行异常' }
    case 'warning':
      return { type: 'warning', text: '存在警告' }
    default:
      return { type: 'info', text: '运行中' }
  }
})

// ================ 方法定义 ================

/**
 * 添加事件日志
 */
const addEventLog = (type: EventLog['type'], message: string, data?: any) => {
  const log: EventLog = {
    time: new Date().toLocaleTimeString(),
    type,
    message,
    data
  }
  
  eventLogs.value.push(log)
  console.log(`[测试页面] ${type.toUpperCase()}: ${message}`, data)
}

/**
 * 应用测试配置
 */
const applyTestConfig = () => {
  Object.assign(appliedConfig, testConfig)
  addEventLog('info', '测试配置已应用', appliedConfig)
}

/**
 * 重置测试配置
 */
const resetTestConfig = () => {
  testConfig.studentId = ''
  testConfig.grade = ''
  testConfig.className = ''
  testConfig.userRole = undefined
  testConfig.displayMode = 'card'
  testConfig.maxItems = 10
  testConfig.showCompleted = false
  testConfig.showDebugInfo = true
  testConfig.enableAutoRefresh = false
  testConfig.autoRefreshInterval = 30
  
  addEventLog('info', '测试配置已重置')
}

/**
 * 测试错误状态
 */
const testErrorState = () => {
  addEventLog('warning', '手动触发错误状态测试')
  // 这里可以添加模拟错误的逻辑
}

/**
 * 测试空状态
 */
const testEmptyState = () => {
  addEventLog('info', '手动触发空状态测试')
  // 这里可以添加模拟空数据的逻辑
}

/**
 * 清空事件日志
 */
const clearEventLog = () => {
  eventLogs.value = []
  console.log('[测试页面] 事件日志已清空')
}

// ================ 组件事件处理 ================

/**
 * 待办完成事件
 */
const onTodoComplete = (id: number) => {
  addEventLog('success', `待办项${id}已标记完成`, { todoId: id })
}

/**
 * 待办查看事件
 */
const onTodoView = (id: number) => {
  addEventLog('info', `待办项${id}已标记查看`, { todoId: id })
}

/**
 * 数据加载完成事件
 */
const onDataLoaded = (todos: TodoItem[]) => {
  addEventLog('success', `数据加载成功，共${todos.length}条待办`, { 
    count: todos.length,
    todos: todos.map(t => ({ id: t.id, title: t.title, status: t.status }))
  })
}

/**
 * 数据加载错误事件
 */
const onDataError = (error: string) => {
  addEventLog('error', `数据加载失败: ${error}`, { error })
}

// ================ 生命周期 ================

onMounted(() => {
  console.log('🧪 [TodoContainerTestPage] 第3层测试页面已挂载')
  addEventLog('info', '测试页面初始化完成')
  
  // 应用默认配置
  applyTestConfig()
})

console.log('📦 [TodoContainerTestPage] 第3层测试页面组件定义完成')
</script>

<style scoped lang="scss">
.todo-container-test-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  background: #f8f9fa;
  min-height: 100vh;
}

// ================ 页面头部 ================

.page-header {
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  
  h1 {
    margin: 0 0 8px 0;
    color: #495057;
    font-size: 24px;
  }
  
  .page-meta {
    display: flex;
    gap: 8px;
  }
}

// ================ 控制面板 ================

.control-panel {
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  
  .control-section {
    margin-bottom: 20px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    h3 {
      margin: 0 0 12px 0;
      color: #495057;
      font-size: 16px;
    }
  }
  
  .control-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  
  .control-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    label {
      font-size: 13px;
      font-weight: 500;
      color: #6c757d;
    }
  }
  
  .button-group {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
}

// ================ 测试区域 ================

.test-area {
  margin-bottom: 24px;
  
  .test-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 16px 20px;
    background: white;
    border-radius: 8px 8px 0 0;
    border-bottom: 1px solid #e9ecef;
    
    h3 {
      margin: 0;
      color: #495057;
      font-size: 16px;
    }
  }
}

// ================ 事件日志 ================

.event-log {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  
  .log-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: #f8f9fa;
    border-bottom: 1px solid #e9ecef;
    
    h3 {
      margin: 0;
      color: #495057;
      font-size: 16px;
    }
  }
  
  .log-content {
    max-height: 400px;
    overflow-y: auto;
    
    .log-item {
      display: grid;
      grid-template-columns: auto auto 1fr auto;
      gap: 12px;
      padding: 12px 20px;
      border-bottom: 1px solid #f1f3f4;
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 12px;
      
      &:last-child {
        border-bottom: none;
      }
      
      .log-time {
        color: #adb5bd;
        white-space: nowrap;
      }
      
      .log-type {
        font-weight: bold;
        white-space: nowrap;
        padding: 2px 6px;
        border-radius: 3px;
      }
      
      .log-message {
        color: #495057;
      }
      
      .log-data {
        color: #6c757d;
        font-size: 11px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      &.log-info {
        border-left: 3px solid #2196f3;
        
        .log-type {
          background: #e3f2fd;
          color: #1976d2;
        }
      }
      
      &.log-success {
        border-left: 3px solid #4caf50;
        
        .log-type {
          background: #e8f5e8;
          color: #2e7d32;
        }
      }
      
      &.log-warning {
        border-left: 3px solid #ff9800;
        
        .log-type {
          background: #fff3e0;
          color: #f57c00;
        }
      }
      
      &.log-error {
        border-left: 3px solid #f44336;
        
        .log-type {
          background: #ffebee;
          color: #d32f2f;
        }
      }
    }
    
    .log-empty {
      padding: 40px 20px;
      text-align: center;
      color: #adb5bd;
      font-style: italic;
    }
  }
}

// ================ 响应式设计 ================

@media (max-width: 768px) {
  .todo-container-test-page {
    padding: 16px;
  }
  
  .control-grid {
    grid-template-columns: 1fr;
  }
  
  .button-group {
    justify-content: stretch;
    
    .el-button {
      flex: 1;
    }
  }
  
  .test-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .log-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .log-item {
    grid-template-columns: 1fr;
    gap: 4px;
    
    .log-data {
      white-space: normal;
      word-break: break-all;
    }
  }
}
</style>