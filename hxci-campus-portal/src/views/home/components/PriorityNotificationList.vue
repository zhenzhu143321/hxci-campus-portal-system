<!--
/**
 * 优先通知列表展示组件
 * 
 * @description 专门展示Level 1-3优先处理通知的展示组件，按紧急程度排序
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21
 * @stage Stage 6 - Home.vue组件拆分架构
 * 
 * @responsibilities
 * - 渲染优先级通知列表(Level 1-3)
 * - 处理通知卡片的交互和点击
 * - 管理已读状态的视觉反馈
 * - 提供"查看更多"功能
 * 
 * @design-principles
 * - 纯展示组件：仅通过props接收数据，通过emits通信
 * - 不直接访问Store：保持组件的纯净和可测试性
 * - 响应式UI：适配移动端和暗色主题
 */
-->

<template>
  <div class="priority-workspace-section">
    <!-- 优先区块头部 -->
    <div class="workspace-priority-header">
      <h4>🎯 优先处理通知</h4>
      <el-tag type="info" size="small">{{ notifications.length }}条未读</el-tag>
    </div>
    
    <!-- 优先通知列表 -->
    <div class="priority-notification-list" v-loading="isLoading">
      <div 
        v-for="notification in displayNotifications" 
        :key="notification.id"
        class="priority-notification-card"
        :class="{
          'level-1-emergency': notification.level === 1,
          'level-2-important': notification.level === 2,
          'level-3-regular': notification.level === 3,
          'marking-read': isMarkingRead(notification.id)
        }"
        @click="handleNotificationClick(notification)"
      >
        <!-- 通知内容 -->
        <div class="notification-priority-content">
          <!-- 标题和操作栏 -->
          <div class="notification-header-row">
            <span class="notification-title-priority">{{ notification.title }}</span>
            <div class="notification-actions">
              <!-- 级别标签 -->
              <el-tag 
                :type="getLevelTagType(notification.level)" 
                size="small"
                class="level-tag"
              >
                {{ getLevelText(notification.level) }}
              </el-tag>
              
              <!-- 已读状态按钮 -->
              <el-button 
                v-if="!isNotificationRead(notification.id)"
                type="success" 
                size="small" 
                :loading="isMarkingRead(notification.id)"
                @click.stop="handleMarkAsRead(notification.id)"
                class="mark-read-btn"
              >
                <el-icon><Check /></el-icon>
                标记已读
              </el-button>
              
              <!-- 已读标识 -->
              <el-tag 
                v-else
                type="success" 
                size="small"
                effect="plain"
                class="read-tag"
              >
                <el-icon><CircleCheck /></el-icon>
                已读
              </el-tag>
            </div>
          </div>
          
          <!-- 内容摘要 -->
          <div class="notification-summary-priority">
            {{ getContentPreview(notification.content, 80) }}
          </div>
          
          <!-- 元数据信息 -->
          <div class="notification-meta-priority">
            <span class="notification-publisher-priority">{{ notification.publisherName }}</span>
            <span class="notification-time-priority">{{ formatDate(notification.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 查看更多按钮 -->
    <div v-if="hasMore" class="show-more-priority">
      <el-button type="text" size="small" @click="handleShowAllPriority">
        查看全部{{ notifications.length }}条优先通知 →
      </el-button>
    </div>
    
    <!-- 空状态 -->
    <div v-if="notifications.length === 0 && !isLoading" class="empty-priority">
      <el-empty description="暂无优先通知" :image-size="60">
        <template #description>
          <p style="color: #909399; font-size: 14px;">暂无需要优先处理的通知</p>
          <p style="color: #c0c4cc; font-size: 12px;">Level 1-3级别的重要通知会显示在这里</p>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Check, CircleCheck } from '@element-plus/icons-vue'
import type { NotificationItem } from '@/api/notification'
import dayjs from 'dayjs'

// ================== Props定义 ==================

interface Props {
  /** 通知列表数据 */
  notifications: NotificationItem[]
  /** 最大显示数量 */
  maxDisplay?: number
  /** 是否加载中 */
  isLoading?: boolean
  /** 标记已读中的通知ID列表 */
  markingReadIds?: number[]
  /** 已读状态检查函数 */
  readStatusChecker?: (id: number) => boolean
}

const props = withDefaults(defineProps<Props>(), {
  maxDisplay: 5,
  isLoading: false,
  markingReadIds: () => [],
  readStatusChecker: () => false
})

// ================== Emits定义 ==================

interface Emits {
  /** 通知点击事件 */
  (e: 'notification-click', notification: NotificationItem): void
  /** 标记已读事件 */
  (e: 'mark-read', notificationId: number): void
  /** 显示全部优先通知 */
  (e: 'show-all-priority'): void
}

const emit = defineEmits<Emits>()

// ================== 计算属性 ==================

/** 显示的通知列表（限制数量） */
const displayNotifications = computed(() => {
  return props.notifications.slice(0, props.maxDisplay)
})

/** 是否有更多通知 */
const hasMore = computed(() => {
  return props.notifications.length > props.maxDisplay
})

// ================== 工具函数 ==================

/** 获取级别标签类型 */
const getLevelTagType = (level: number): string => {
  switch (level) {
    case 1: return 'danger'   // 紧急 - 红色
    case 2: return 'warning'  // 重要 - 橙色  
    case 3: return 'primary'  // 常规 - 蓝色
    default: return 'info'    // 默认 - 灰色
  }
}

/** 获取级别文本 */
const getLevelText = (level: number): string => {
  switch (level) {
    case 1: return '紧急'
    case 2: return '重要'
    case 3: return '常规'
    case 4: return '提醒'
    default: return '未知'
  }
}

/** 获取内容预览 */
const getContentPreview = (content: string, maxLength: number): string => {
  if (!content) return '暂无内容'
  return content.length > maxLength 
    ? content.substring(0, maxLength) + '...'
    : content
}

/** 格式化日期 */
const formatDate = (dateStr: string): string => {
  return dayjs(dateStr).format('MM-DD HH:mm')
}

/** 检查通知是否已读 */
const isNotificationRead = (notificationId: number): boolean => {
  return props.readStatusChecker(notificationId)
}

/** 检查是否正在标记已读 */
const isMarkingRead = (notificationId: number): boolean => {
  return props.markingReadIds.includes(notificationId)
}

// ================== 事件处理器 ==================

/** 处理通知点击 */
const handleNotificationClick = (notification: NotificationItem) => {
  emit('notification-click', notification)
}

/** 处理标记已读 */
const handleMarkAsRead = (notificationId: number) => {
  emit('mark-read', notificationId)
}

/** 处理显示全部优先通知 */
const handleShowAllPriority = () => {
  emit('show-all-priority')
}
</script>

<style scoped>
/* 优先工作区样式 */
.priority-workspace-section {
  background: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
  border: 1px solid #fde2e2;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.08);
}

/* 优先区块头部 */
.workspace-priority-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #fecaca;
}

.workspace-priority-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #dc2626;
}

/* 通知卡片列表 */
.priority-notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 优先通知卡片 */
.priority-notification-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.priority-notification-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 级别特定样式 */
.level-1-emergency {
  border-left: 4px solid #dc2626;
  background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
}

.level-2-important {
  border-left: 4px solid #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #ffffff 100%);
}

.level-3-regular {
  border-left: 4px solid #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
}

/* 标记已读中的状态 */
.marking-read {
  opacity: 0.7;
  pointer-events: none;
}

/* 通知内容 */
.notification-priority-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 标题和操作栏 */
.notification-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.notification-title-priority {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
  flex: 1;
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 级别标签 */
.level-tag {
  font-weight: 500;
}

/* 操作按钮 */
.mark-read-btn {
  --el-button-size: 24px;
  font-size: 12px;
  padding: 4px 8px;
}

.read-tag {
  font-size: 12px;
}

/* 内容摘要 */
.notification-summary-priority {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

/* 元数据信息 */
.notification-meta-priority {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #9ca3af;
}

.notification-publisher-priority {
  font-weight: 500;
}

.notification-time-priority {
  color: #6b7280;
}

/* 查看更多按钮 */
.show-more-priority {
  margin-top: 12px;
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

/* 空状态 */
.empty-priority {
  text-align: center;
  padding: 40px 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .priority-workspace-section {
    padding: 16px;
    border-radius: 8px;
  }
  
  .priority-notification-card {
    padding: 12px;
  }
  
  .notification-header-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .notification-actions {
    justify-content: flex-end;
  }
  
  .notification-title-priority {
    font-size: 14px;
  }
  
  .notification-summary-priority {
    font-size: 12px;
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .priority-workspace-section {
    background: linear-gradient(135deg, #1f1f1f 0%, #2a2a2a 100%);
    border-color: #3a3a3a;
  }
  
  .workspace-priority-header {
    border-bottom-color: #3a3a3a;
  }
  
  .workspace-priority-header h4 {
    color: #ef4444;
  }
  
  .priority-notification-card {
    background: #2a2a2a;
    border-color: #3a3a3a;
  }
  
  .notification-title-priority {
    color: #e0e0e0;
  }
  
  .notification-summary-priority {
    color: #9ca3af;
  }
  
  .notification-meta-priority {
    color: #6b7280;
  }
}
</style>