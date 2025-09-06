<!--
  TodoNotificationContainer组件测试页面
  
  @description 用于测试第2层TodoNotificationContainer组件的基础功能
  @date 2025-08-22
  
  测试内容:
  ✅ Props传递测试
  ✅ 调试信息显示
  ✅ 不同Props组合验证
  ✅ 响应式更新测试
-->

<template>
  <div class="test-page">
    <div class="test-header">
      <h1>📋 TodoNotificationContainer 第2层测试</h1>
      <p>测试Props驱动的待办通知容器组件基础功能</p>
    </div>
    
    <!-- 测试控制面板 -->
    <div class="test-controls">
      <h3>🔧 测试参数控制</h3>
      <div class="control-grid">
        <div class="control-item">
          <label>学号:</label>
          <el-input v-model="testProps.studentId" placeholder="如：2023010105" />
        </div>
        <div class="control-item">
          <label>年级:</label>
          <el-select v-model="testProps.grade" placeholder="选择年级">
            <el-option label="2023级" value="2023" />
            <el-option label="2022级" value="2022" />
            <el-option label="2021级" value="2021" />
          </el-select>
        </div>
        <div class="control-item">
          <label>班级:</label>
          <el-input v-model="testProps.className" placeholder="如：计算机科学与技术1班" />
        </div>
        <div class="control-item">
          <label>用户角色:</label>
          <el-select v-model="testProps.userRole" placeholder="选择角色">
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
          <el-select v-model="testProps.displayMode" placeholder="选择模式">
            <el-option label="卡片模式" value="card" />
            <el-option label="列表模式" value="list" />
            <el-option label="紧凑模式" value="compact" />
            <el-option label="统计模式" value="statistics" />
            <el-option label="日历模式" value="calendar" />
          </el-select>
        </div>
        <div class="control-item">
          <label>最大显示数:</label>
          <el-input-number v-model="testProps.maxItems" :min="1" :max="100" />
        </div>
        <div class="control-item">
          <label>显示已完成:</label>
          <el-switch v-model="testProps.showCompleted" />
        </div>
        <div class="control-item">
          <label>显示调试信息:</label>
          <el-switch v-model="testProps.showDebugInfo" />
        </div>
      </div>
      
      <div class="control-actions">
        <el-button @click="resetProps" type="warning">重置参数</el-button>
        <el-button @click="loadPresetStudent" type="primary">学生预设</el-button>
        <el-button @click="loadPresetTeacher" type="success">教师预设</el-button>
      </div>
    </div>
    
    <!-- 第2层组件测试实例 -->
    <div class="test-instances">
      <h3>🧪 组件测试实例</h3>
      
      <!-- 测试实例1：当前配置 -->
      <div class="test-instance">
        <h4>实例1: 当前配置测试</h4>
        <TodoNotificationContainer
          :student-id="testProps.studentId"
          :grade="testProps.grade"
          :class-name="testProps.className"
          :user-role="testProps.userRole"
          :display-mode="testProps.displayMode"
          :max-items="testProps.maxItems"
          :show-completed="testProps.showCompleted"
          :show-debug-info="testProps.showDebugInfo"
        />
      </div>
      
      <!-- 测试实例2：学生默认配置 -->
      <div class="test-instance">
        <h4>实例2: 学生默认配置</h4>
        <TodoNotificationContainer
          student-id="2023010105"
          grade="2023"
          class-name="计算机科学与技术1班"
          user-role="STUDENT"
          display-mode="card"
          :max-items="5"
          :show-completed="false"
          :show-debug-info="true"
        />
      </div>
      
      <!-- 测试实例3：教师配置 -->
      <div class="test-instance">
        <h4>实例3: 教师配置</h4>
        <TodoNotificationContainer
          grade="2023"
          class-name="计算机科学与技术"
          user-role="TEACHER"
          display-mode="list"
          :max-items="10"
          :show-completed="true"
          :show-debug-info="true"
        />
      </div>
    </div>
    
    <!-- 控制台日志监控 -->
    <div class="console-monitor">
      <h3>📊 控制台日志监控</h3>
      <p>请打开浏览器开发者工具控制台，查看组件调试输出</p>
      <el-alert 
        title="第2层测试重点" 
        type="info" 
        :closable="false"
        description="验证Props接收、类型安全、调试输出和响应式更新功能"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import TodoNotificationContainer from '@/components/todo/TodoNotificationContainer.vue'
import type { UserRole, TodoDisplayMode } from '@/types/todo'

// ================ 测试数据 ================

interface TestProps {
  studentId?: string
  grade?: string
  className?: string
  userRole?: UserRole
  displayMode?: TodoDisplayMode
  maxItems?: number
  showCompleted?: boolean
  showDebugInfo?: boolean
}

const testProps = ref<TestProps>({
  studentId: '',
  grade: '',
  className: '',
  userRole: 'STUDENT',
  displayMode: 'card',
  maxItems: 10,
  showCompleted: false,
  showDebugInfo: true
})

// ================ 控制方法 ================

const resetProps = () => {
  testProps.value = {
    studentId: '',
    grade: '',
    className: '',
    userRole: 'STUDENT',
    displayMode: 'card',
    maxItems: 10,
    showCompleted: false,
    showDebugInfo: true
  }
  console.log('🔄 [TestPage] 参数已重置')
}

const loadPresetStudent = () => {
  testProps.value = {
    studentId: '2023010105',
    grade: '2023',
    className: '计算机科学与技术1班',
    userRole: 'STUDENT',
    displayMode: 'card',
    maxItems: 5,
    showCompleted: false,
    showDebugInfo: true
  }
  console.log('👨‍🎓 [TestPage] 加载学生预设配置')
}

const loadPresetTeacher = () => {
  testProps.value = {
    studentId: undefined,
    grade: '2023',
    className: '计算机科学与技术',
    userRole: 'TEACHER',
    displayMode: 'list',
    maxItems: 15,
    showCompleted: true,
    showDebugInfo: true
  }
  console.log('👨‍🏫 [TestPage] 加载教师预设配置')
}

// ================ 监听器 ================

watch(testProps, (newProps) => {
  console.log('🔄 [TestPage] 测试参数更新:', newProps)
}, { deep: true })

console.log('📄 [TestPage] TodoNotificationContainer测试页面已加载')
</script>

<style scoped lang="scss">
.test-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.test-header {
  text-align: center;
  margin-bottom: 32px;
  
  h1 {
    color: #495057;
    margin-bottom: 8px;
  }
  
  p {
    color: #6c757d;
    font-size: 16px;
  }
}

.test-controls {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 32px;
  
  h3 {
    margin: 0 0 16px 0;
    color: #495057;
  }
  
  .control-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 16px;
    margin-bottom: 16px;
  }
  
  .control-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    label {
      font-weight: 500;
      color: #495057;
      font-size: 14px;
    }
  }
  
  .control-actions {
    display: flex;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px solid #dee2e6;
  }
}

.test-instances {
  .test-instance {
    margin-bottom: 32px;
    border: 1px solid #dee2e6;
    border-radius: 8px;
    overflow: hidden;
    
    h4 {
      background: #f8f9fa;
      margin: 0;
      padding: 16px;
      border-bottom: 1px solid #dee2e6;
      color: #495057;
      font-size: 16px;
    }
    
    > :deep(.todo-notification-container) {
      margin: 16px;
    }
  }
}

.console-monitor {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  
  h3 {
    margin: 0 0 16px 0;
    color: #495057;
  }
  
  p {
    color: #6c757d;
    margin-bottom: 16px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .test-page {
    padding: 16px;
  }
  
  .control-grid {
    grid-template-columns: 1fr;
  }
  
  .control-actions {
    flex-direction: column;
  }
}
</style>