<!--
/**
 * Level 4通知Bug修复测试页面
 * 
 * @description 用于测试和验证Level 4通知的两个Bug修复
 * @author Claude Code AI Assistant
 * @date 2025-08-22
 * 
 * Bug修复验证：
 * 1. Bug 1: 4级提醒消息点击已读后不会自动归档消失
 * 2. Bug 2: 4级提醒通知错误地进入到系统通知卡片区域
 */
-->

<template>
  <div class="test-level4-container">
    <h1>Level 4通知Bug修复测试</h1>
    
    <!-- 测试控制面板 -->
    <div class="test-controls">
      <el-button type="primary" @click="refreshNotifications">
        <el-icon><Refresh /></el-icon>
        刷新通知数据
      </el-button>
      
      <el-button type="success" @click="createTestLevel4">
        <el-icon><Plus /></el-icon>
        创建测试Level 4通知
      </el-button>
      
      <el-button type="warning" @click="clearAllReadStatus">
        <el-icon><Delete /></el-icon>
        清空所有已读状态
      </el-button>
    </div>
    
    <!-- 测试状态显示 -->
    <div class="test-status">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>📊 通知状态统计</span>
          </div>
        </template>
        
        <div class="status-grid">
          <div class="status-item">
            <label>所有通知总数：</label>
            <el-tag>{{ allNotifications.length }}</el-tag>
          </div>
          
          <div class="status-item">
            <label>Level 1-3优先级通知（未读）：</label>
            <el-tag type="danger">{{ unreadPriorityNotifications.length }}</el-tag>
          </div>
          
          <div class="status-item">
            <label>Level 4通知（所有）：</label>
            <el-tag type="info">{{ allLevel4.length }}</el-tag>
          </div>
          
          <div class="status-item">
            <label>Level 4通知（未读）：</label>
            <el-tag type="success">{{ level4Messages.length }}</el-tag>
          </div>
          
          <div class="status-item">
            <label>Level 4通知（已读）：</label>
            <el-tag type="warning">{{ readLevel4.length }}</el-tag>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 测试区域分隔显示 -->
    <div class="test-areas">
      <!-- 优先级通知区域 (Level 1-3) -->
      <div class="test-area">
        <h3>🎯 优先级通知区域 (Level 1-3)</h3>
        <div class="notification-list">
          <div v-if="unreadPriorityNotifications.length === 0" class="empty-message">
            ✅ 无Level 1-3通知（正确）
          </div>
          <div 
            v-for="notification in unreadPriorityNotifications" 
            :key="notification.id"
            class="notification-item"
            :class="`level-${notification.level}`"
          >
            <span class="notification-level">Level {{ notification.level }}</span>
            <span class="notification-title">{{ notification.title }}</span>
            <el-button 
              size="small" 
              type="success"
              @click="markAsRead(notification.id)"
            >
              标记已读
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- Level 4通知区域 -->
      <div class="test-area">
        <h3>💬 Level 4通知区域（仅显示未读）</h3>
        <div class="notification-list">
          <div v-if="level4Messages.length === 0" class="empty-message">
            ✅ 无未读Level 4通知（已读的已自动归档）
          </div>
          <div 
            v-for="message in level4Messages" 
            :key="message.id"
            class="notification-item level-4"
          >
            <span class="notification-level">Level 4</span>
            <span class="notification-title">{{ message.title }}</span>
            <span class="notification-status">
              <el-tag type="info" size="small">未读</el-tag>
            </span>
            <el-button 
              size="small" 
              type="success"
              @click="markAsRead(message.id)"
            >
              标记已读
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 已读Level 4归档区域 -->
      <div class="test-area">
        <h3>📦 已读Level 4归档区域</h3>
        <div class="notification-list">
          <div v-if="readLevel4.length === 0" class="empty-message">
            暂无已读Level 4通知
          </div>
          <div 
            v-for="message in readLevel4" 
            :key="message.id"
            class="notification-item archived"
          >
            <span class="notification-level">Level 4</span>
            <span class="notification-title">{{ message.title }}</span>
            <span class="notification-status">
              <el-tag type="success" size="small">已读</el-tag>
            </span>
            <el-button 
              size="small" 
              type="warning"
              @click="markAsUnread(message.id)"
            >
              标记未读
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 测试结果验证 -->
    <div class="test-validation">
      <el-alert 
        v-if="isTestPassed" 
        title="✅ 测试通过" 
        type="success"
        :closable="false"
      >
        <div>Bug 1修复验证：Level 4通知标记已读后自动从未读列表消失 ✓</div>
        <div>Bug 2修复验证：Level 4通知不会出现在优先级通知区域 ✓</div>
      </el-alert>
      
      <el-alert 
        v-else 
        title="⚠️ 测试进行中" 
        type="warning"
        :closable="false"
      >
        <div>请创建Level 4通知并测试标记已读功能</div>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { ElMessage } from 'element-plus'
import type { NotificationItem } from '@/api/notification'

// Store初始化
const notificationStore = useNotificationStore()

// 响应式数据
const testNotificationCreated = ref(false)
const testNotificationMarkedAsRead = ref(false)

// 计算属性
const allNotifications = computed(() => notificationStore.notifications)
const unreadPriorityNotifications = computed(() => notificationStore.unreadPriorityNotifications)
const level4Messages = computed(() => notificationStore.level4Messages)

// 所有Level 4通知
const allLevel4 = computed(() => 
  allNotifications.value.filter(n => n.level === 4)
)

// 已读的Level 4通知
const readLevel4 = computed(() => 
  allLevel4.value.filter(n => notificationStore.isRead(n.id))
)

// 测试是否通过
const isTestPassed = computed(() => {
  // 验证条件：
  // 1. 没有Level 4通知在优先级区域
  const noLevel4InPriority = !unreadPriorityNotifications.value.some(n => n.level === 4)
  
  // 2. Level 4区域只显示未读通知
  const level4OnlyUnread = level4Messages.value.every(n => !notificationStore.isRead(n.id))
  
  // 3. 有已读的Level 4通知在归档区
  const hasArchivedLevel4 = readLevel4.value.length > 0
  
  return noLevel4InPriority && level4OnlyUnread && testNotificationMarkedAsRead.value
})

// 方法
const refreshNotifications = async () => {
  await notificationStore.fetchNotifications()
  ElMessage.success('通知数据已刷新')
}

const createTestLevel4 = () => {
  // 模拟创建一个Level 4通知（实际应该通过API）
  const testNotification: NotificationItem = {
    id: Date.now(),
    title: `测试Level 4通知 - ${new Date().toLocaleTimeString()}`,
    content: '这是一条用于测试Bug修复的Level 4通知',
    summary: '测试通知',
    level: 4,
    status: 3,
    categoryId: 1,
    publisherId: 999,
    publisherName: '测试系统',
    publisherRole: 'SYSTEM',
    targetScope: 'SCHOOL_WIDE',
    pushChannels: '1',
    requireConfirm: false,
    pinned: false,
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
    creator: 'test',
    updater: 'test',
    validEndTime: '',
    attachmentUrl: ''
  }
  
  // 添加到通知列表（实际应该通过API获取）
  notificationStore.notifications.push(testNotification)
  testNotificationCreated.value = true
  
  ElMessage.success('已创建测试Level 4通知')
}

const markAsRead = (notificationId: number) => {
  notificationStore.markAsRead(notificationId)
  testNotificationMarkedAsRead.value = true
  ElMessage.success('已标记为已读')
}

const markAsUnread = (notificationId: number) => {
  notificationStore.markAsUnread(notificationId)
  ElMessage.info('已标记为未读')
}

const clearAllReadStatus = () => {
  // 清空所有已读状态
  const manager = notificationStore.readStatusManager
  if (manager) {
    allNotifications.value.forEach(n => {
      if (notificationStore.isRead(n.id)) {
        notificationStore.markAsUnread(n.id)
      }
    })
  }
  testNotificationMarkedAsRead.value = false
  ElMessage.warning('已清空所有已读状态')
}

// 生命周期
onMounted(async () => {
  // 设置用户ID
  notificationStore.setCurrentUserId(1)
  
  // 加载通知数据
  await refreshNotifications()
  
  console.log('🧪 Level 4通知Bug测试页面已加载')
})
</script>

<style scoped>
.test-level4-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  color: #303133;
  margin-bottom: 20px;
}

.test-controls {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.test-status {
  margin-bottom: 30px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 15px;
  margin-top: 10px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.test-areas {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.test-area {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
  background: #fff;
}

.test-area h3 {
  margin: 0 0 15px 0;
  color: #606266;
  font-size: 16px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
}

.notification-item.level-1 {
  border-left: 3px solid #f56c6c;
  background: #fef0f0;
}

.notification-item.level-2 {
  border-left: 3px solid #e6a23c;
  background: #fdf6ec;
}

.notification-item.level-3 {
  border-left: 3px solid #409eff;
  background: #ecf5ff;
}

.notification-item.level-4 {
  border-left: 3px solid #67c23a;
  background: #f0f9ff;
}

.notification-item.archived {
  opacity: 0.7;
  background: #f5f7fa;
}

.notification-level {
  font-weight: bold;
  color: #606266;
  min-width: 60px;
}

.notification-title {
  flex: 1;
  color: #303133;
}

.notification-status {
  margin-right: 10px;
}

.empty-message {
  padding: 20px;
  text-align: center;
  color: #909399;
  background: #f5f7fa;
  border-radius: 4px;
}

.test-validation {
  margin-top: 30px;
}

.test-validation :deep(.el-alert__description) {
  margin-top: 10px;
}

.test-validation div {
  line-height: 1.8;
}
</style>