<!--
/**
 * 全部通知对话框组件
 * 
 * @description 展示所有通知的全屏对话框组件，支持筛选、搜索和分页
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21
 * @stage Stage 6 - Home.vue组件拆分架构
 * 
 * @responsibilities
 * - 展示完整通知列表（支持筛选和搜索）
 * - 提供通知分类标签页（全部、未读、重要、系统）
 * - 管理对话框的显示/隐藏状态
 * - 支持通知详情查看和操作
 * 
 * @design-principles
 * - 容器组件：负责数据获取和状态管理
 * - 集成Store：直接从NotificationStore和UIStore获取数据
 * - 响应式设计：适配桌面端和移动端
 */
-->

<template>
  <el-dialog
    v-model="visible"
    title="📋 全部通知管理"
    width="90%"
    :close-on-click-modal="false"
    class="all-notifications-dialog"
    @close="handleClose"
  >
    <!-- 对话框头部：搜索和筛选 -->
    <div class="dialog-header">
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索通知标题或内容..."
          size="default"
          clearable
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="selectedLevel"
          placeholder="级别筛选"
          clearable
          size="default"
          class="level-filter"
        >
          <el-option label="全部级别" :value="null" />
          <el-option label="🔴 紧急 (Level 1)" :value="1" />
          <el-option label="🟠 重要 (Level 2)" :value="2" />
          <el-option label="🟡 常规 (Level 3)" :value="3" />
          <el-option label="🟢 提醒 (Level 4)" :value="4" />
        </el-select>
      </div>

      <!-- 统计信息 -->
      <div class="stats-section">
        <el-tag type="info">总计: {{ totalCount }}</el-tag>
        <el-tag type="warning">未读: {{ unreadCount }}</el-tag>
        <el-tag type="success">已读: {{ readCount }}</el-tag>
      </div>
    </div>

    <!-- 标签页导航 -->
    <el-tabs v-model="activeTab" class="notification-tabs">
      <el-tab-pane label="📋 全部通知" name="all">
        <NotificationListContent
          :notifications="filteredAllNotifications"
          :loading="notificationStore.loading"
          @notification-click="handleNotificationClick"
          @mark-read="handleMarkAsRead"
          @mark-unread="handleMarkAsUnread"
        />
      </el-tab-pane>
      
      <el-tab-pane name="unread">
        <template #label>
          <span>🔴 未读通知 <el-badge :value="unreadCount" :hidden="unreadCount === 0" /></span>
        </template>
        <NotificationListContent
          :notifications="filteredUnreadNotifications"
          :loading="notificationStore.loading"
          @notification-click="handleNotificationClick"
          @mark-read="handleMarkAsRead"
        />
      </el-tab-pane>
      
      <el-tab-pane name="important">
        <template #label>
          <span>⭐ 重要通知 <el-badge :value="importantCount" :hidden="importantCount === 0" /></span>
        </template>
        <NotificationListContent
          :notifications="filteredImportantNotifications"
          :loading="notificationStore.loading"
          @notification-click="handleNotificationClick"
          @mark-read="handleMarkAsRead"
          @mark-unread="handleMarkAsUnread"
        />
      </el-tab-pane>
      
      <el-tab-pane name="system">
        <template #label>
          <span>🔧 系统通知 <el-badge :value="systemCount" :hidden="systemCount === 0" /></span>
        </template>
        <NotificationListContent
          :notifications="filteredSystemNotifications"
          :loading="notificationStore.loading"
          @notification-click="handleNotificationClick"
          @mark-read="handleMarkAsRead"
          @mark-unread="handleMarkAsUnread"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 对话框底部：操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleMarkAllAsRead" :loading="batchLoading">
          📖 全部已读
        </el-button>
        <el-button @click="handleRefresh" :loading="notificationStore.loading">
          🔄 刷新数据
        </el-button>
        <el-button type="primary" @click="handleClose">
          关闭
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { useUIStore } from '@/stores/ui'
import { useTodoStore } from '@/stores/todo'
import NotificationListContent from './NotificationListContent.vue'
import type { NotificationItem } from '@/api/notification'
import type { TodoNotificationItem } from '@/types/todo'
import { getNotificationTimestamp } from '@/utils/date'

// =====================================================
// Props & Emits 定义
// =====================================================
interface Props {
  modelValue: boolean  // v-model支持
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'notification-click', notification: NotificationItem): void
  (e: 'mark-read', id: number): void
  (e: 'mark-unread', id: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// =====================================================
// Stores & Reactive State
// =====================================================
const notificationStore = useNotificationStore()
const uiStore = useUIStore()
const todoStore = useTodoStore()

// =====================================================
// 数据转换函数 - 将不同数据源统一转换为NotificationItem格式
// =====================================================

// 对话框显示状态
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

// 搜索和筛选状态
const searchKeyword = ref('')
const selectedLevel = ref<number | null>(null)
const activeTab = ref('all')
const batchLoading = ref(false)


// 将待办通知转换为NotificationItem格式 (修复类型不匹配问题)
const convertTodoToNotificationItem = (item: TodoNotificationItem): NotificationItem => ({
  id: parseInt(`9000${item.id}`), // 使用9000前缀避免ID冲突，确保数字类型
  title: `📝 ${item.title}`, // 添加待办图标前缀
  content: item.content,
  level: 5, // 待办通知统一为Level 5
  levelColor: '#909399', // 灰色
  publisherName: item.assignerName || '系统',
  publisherRole: 'SYSTEM',
  createTime: item.dueDate, // 使用截止时间作为显示时间
  scope: 'SCHOOL_WIDE', // 待办通知默认全校范围
  status: item.isCompleted ? 'COMPLETED' : 'PENDING',
  summary: `📋 待办事项 | 优先级: ${item.priority} | ${item.isCompleted ? '已完成' : '待处理'}`,
  isRead: item.isCompleted
})

// 合并所有通知的计算属性
const allUnifiedNotifications = computed(() => {
  const regularNotifications = notificationStore.notifications || []
  const todoNotifications = (todoStore.todoNotifications || []).map(convertTodoToNotificationItem)

  // 🔧 P0级修复: 确保所有数据都是NotificationItem类型，避免类型不匹配
  console.log('🔍 [AllNotificationsDialog] 数据合并调试:')
  console.log('  常规通知数量:', regularNotifications.length)
  console.log('  待办通知数量:', todoNotifications.length)
  console.log('  待办通知数据:', todoNotifications)

  // 合并并按时间排序
  return [...regularNotifications, ...todoNotifications]
    .sort((a, b) => getNotificationTimestamp(b.createTime) - getNotificationTimestamp(a.createTime))
})

// =====================================================
// Computed Properties - 数据统计 (基于合并后的数据)
// =====================================================
const totalCount = computed(() => allUnifiedNotifications.value.length)

const unreadCount = computed(() => {
  const stats = notificationStore.unreadStats
  return stats ? stats.total : 0
})

const readCount = computed(() => totalCount.value - unreadCount.value)

const importantCount = computed(() => {
  const notifications = notificationStore.importantNotifications
  return Array.isArray(notifications) ? notifications.length : 0
})

const systemCount = computed(() => {
  const announcements = notificationStore.systemAnnouncements
  return Array.isArray(announcements) ? announcements.length : 0
})

// =====================================================
// Computed Properties - 筛选逻辑 (支持NotificationItem类型)
// =====================================================
const filterNotifications = (notifications: NotificationItem[]) => {
  // 🔧 P0级修复: 确保notifications是数组且有内容
  if (!notifications || !Array.isArray(notifications)) {
    console.warn('⚠️ [AllNotificationsDialog] notifications不是有效数组:', notifications)
    return []
  }

  let filtered = [...notifications]

  // 关键词搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(n =>
      n.title.toLowerCase().includes(keyword) ||
      n.content.toLowerCase().includes(keyword)
    )
  }

  // 级别筛选
  if (selectedLevel.value !== null) {
    filtered = filtered.filter(n => n.level === selectedLevel.value)
  }

  return filtered.sort((a, b) => getNotificationTimestamp(b.createTime) - getNotificationTimestamp(a.createTime))
}

const filteredAllNotifications = computed(() =>
  filterNotifications(allUnifiedNotifications.value)
)

const filteredUnreadNotifications = computed(() => {
  const unreadNotifications = allUnifiedNotifications.value.filter(n =>
    n && !n.isRead
  )
  return filterNotifications(unreadNotifications)
})

const filteredImportantNotifications = computed(() => {
  const importantNotifications = allUnifiedNotifications.value.filter(n =>
    n.level <= 2 // Level 1和2为重要通知
  )
  return filterNotifications(importantNotifications)
})

const filteredSystemNotifications = computed(() => {
  const systemNotifications = allUnifiedNotifications.value.filter(n =>
    n.publisherRole === 'SYSTEM' || n.publisherRole === 'SYSTEM_ADMIN' || n.title.includes('📝')
  )
  return filterNotifications(systemNotifications)
})

// =====================================================
// Event Handlers - 用户交互
// =====================================================
const handleNotificationClick = (notification: NotificationItem) => {
  emit('notification-click', notification)
}

const handleMarkAsRead = async (id: number) => {
  emit('mark-read', id)
}

const handleMarkAsUnread = async (id: number) => {
  emit('mark-unread', id)
}

const handleMarkAllAsRead = async () => {
  batchLoading.value = true
  try {
    // 🔧 P0级修复: 获取当前标签页的未读通知
    let unreadInCurrentTab: NotificationItem[] = []
    
    switch (activeTab.value) {
      case 'all':
        unreadInCurrentTab = filteredAllNotifications.value.filter(n => 
          n && !notificationStore.isRead(n.id)
        )
        break
      case 'unread':
        unreadInCurrentTab = filteredUnreadNotifications.value
        break
      case 'important':
        unreadInCurrentTab = filteredImportantNotifications.value.filter(n => 
          n && !notificationStore.isRead(n.id)
        )
        break
      case 'system':
        unreadInCurrentTab = filteredSystemNotifications.value.filter(n => 
          n && !notificationStore.isRead(n.id)
        )
        break
    }
    
    // 批量标记为已读
    for (const notification of unreadInCurrentTab) {
      notificationStore.markAsRead(notification.id)
    }
    
    console.log(`✅ [AllNotificationsDialog] 批量标记已读完成: ${unreadInCurrentTab.length} 条`)
    // ElMessage.success(`已将 ${unreadInCurrentTab.length} 条通知标记为已读`)
  } catch (error) {
    console.error('❌ [AllNotificationsDialog] 批量标记已读失败:', error)
    // ElMessage.error('批量操作失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

const handleRefresh = async () => {
  try {
    // 并行获取常规通知和待办通知
    await Promise.all([
      notificationStore.fetchNotifications(),
      todoStore.initializeTodos()
    ])
    console.log('✅ [AllNotificationsDialog] 所有通知数据刷新成功')
    // ElMessage.success('通知数据已刷新')
  } catch (error) {
    console.error('❌ [AllNotificationsDialog] 刷新失败:', error)
  }
}

const handleClose = () => {
  visible.value = false
  
  // 重置筛选状态
  searchKeyword.value = ''
  selectedLevel.value = null
  activeTab.value = 'all'
}

// =====================================================
// Watchers - 响应式监听
// =====================================================
watch(visible, (newValue) => {
  if (newValue) {
    // 对话框打开时刷新数据 - 同时获取常规通知和待办通知
    Promise.all([
      notificationStore.fetchNotifications(),
      todoStore.initializeTodos()
    ]).catch(error => {
      console.error('❌ [AllNotificationsDialog] 数据加载失败:', error)
    })
  }
})
</script>

<style scoped>
/* =====================================================
 * 对话框整体样式
 * ===================================================== */
.all-notifications-dialog {
  --dialog-padding: 24px;
  --border-radius: 12px;
}

:deep(.el-dialog__body) {
  padding: var(--dialog-padding);
  max-height: 70vh;
  overflow: hidden;
}

/* =====================================================
 * 对话框头部样式
 * ===================================================== */
.dialog-header {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.search-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.level-filter {
  width: 200px;
}

.stats-section {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* =====================================================
 * 标签页样式
 * ===================================================== */
.notification-tabs {
  height: calc(70vh - 160px);
  overflow: hidden;
}

:deep(.el-tabs__content) {
  height: 100%;
  overflow: auto;
}

:deep(.el-tabs__item) {
  font-weight: 500;
}

:deep(.el-badge__content) {
  border: 1px solid #fff;
}

/* =====================================================
 * 对话框底部样式
 * ===================================================== */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
}

/* =====================================================
 * 响应式设计
 * ===================================================== */
@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }
  
  .search-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input,
  .level-filter {
    width: 100%;
    max-width: none;
  }
  
  .stats-section {
    justify-content: center;
  }
  
  .dialog-footer {
    flex-direction: column;
  }
}

/* =====================================================
 * 暗色主题适配
 * ===================================================== */
@media (prefers-color-scheme: dark) {
  .dialog-header {
    border-bottom-color: var(--el-border-color-darker);
  }
  
  .dialog-footer {
    border-top-color: var(--el-border-color-darker);
  }
}
</style>