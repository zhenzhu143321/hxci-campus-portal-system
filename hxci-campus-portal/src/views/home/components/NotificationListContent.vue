<!--
/**
 * 通知列表内容组件
 * 
 * @description 可复用的通知列表展示组件，用于显示各种筛选后的通知列表
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21
 * @stage Stage 6 - Home.vue组件拆分架构
 * 
 * @responsibilities
 * - 展示通知列表（统一的卡片样式）
 * - 处理通知项的交互（点击、标记已读/未读）
 * - 支持加载状态和空状态展示
 * - 响应式布局适配
 * 
 * @design-principles
 * - 纯展示组件：仅通过props接收数据，通过emits通信
 * - 高度可复用：适用于各种通知列表场景
 * - 无状态管理：不直接访问Store，保持纯净性
 */
-->

<template>
  <div class="notification-list-content">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>
    
    <!-- 空状态 -->
    <el-empty 
      v-else-if="notifications.length === 0" 
      description="暂无通知"
      :image-size="120"
    >
      <template #image>
        <el-icon size="120" color="var(--el-color-info)">
          <Bell />
        </el-icon>
      </template>
    </el-empty>
    
    <!-- 通知列表 -->
    <div v-else class="notification-list">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-item"
        :class="{
          'notification-read': isRead(notification.id),
          'notification-unread': !isRead(notification.id),
          'level-1-emergency': notification.level === 1,
          'level-2-important': notification.level === 2,
          'level-3-regular': notification.level === 3,
          'level-4-reminder': notification.level === 4
        }"
        @click="handleNotificationClick(notification)"
      >
        <!-- 通知卡片头部 -->
        <div class="notification-header">
          <div class="notification-title-section">
            <h4 class="notification-title">{{ notification.title }}</h4>
            <div class="notification-meta">
              <el-tag 
                :type="getLevelTagType(notification.level)" 
                size="small"
                class="level-tag"
              >
                {{ getLevelText(notification.level) }}
              </el-tag>
              <span class="notification-time">{{ formatApiDate(notification.createTime, { fallback: '时间未知' }) }}</span>
            </div>
          </div>
          
          <!-- 操作按钮区 -->
          <div class="notification-actions">
            <!-- 已读状态切换 -->
            <el-tooltip 
              :content="isRead(notification.id) ? '标记为未读' : '标记为已读'" 
              placement="top"
            >
              <el-button
                :type="isRead(notification.id) ? 'info' : 'success'"
                :icon="isRead(notification.id) ? 'Refresh' : 'Check'"
                size="small"
                circle
                @click.stop="handleReadStatusToggle(notification)"
              />
            </el-tooltip>
            
            <!-- 详情按钮 -->
            <el-tooltip content="查看详情" placement="top">
              <el-button
                type="primary"
                icon="View"
                size="small"
                circle
                @click.stop="handleNotificationClick(notification)"
              />
            </el-tooltip>
          </div>
        </div>
        
        <!-- 通知内容摘要 -->
        <div class="notification-content">
          <div
            class="notification-summary"
            v-html="getContentSummary(notification.content)"
          ></div>
          
          <!-- 发布者信息 -->
          <div class="notification-publisher">
            <el-icon class="publisher-icon"><User /></el-icon>
            <span class="publisher-name">{{ notification.publisherName }}</span>
            <span class="publisher-role">({{ getRoleDisplayName(notification.publisherRole) }})</span>
          </div>
        </div>
        
        <!-- 通知范围和渠道 -->
        <div class="notification-footer">
          <div class="notification-scope">
            <el-icon class="scope-icon"><Location /></el-icon>
            <span>{{ getScopeDisplayName(notification.targetScope) }}</span>
          </div>
          
          <div class="notification-channels">
            <el-icon class="channel-icon"><Message /></el-icon>
            <span>{{ getChannelsDisplayName(notification.pushChannels) }}</span>
          </div>
        </div>
        
        <!-- 未读指示器 -->
        <div v-if="!isRead(notification.id)" class="unread-indicator" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Bell, User, Location, Message, Check, Refresh, View } from '@element-plus/icons-vue'
import type { NotificationItem } from '@/api/types/notification'
import { formatApiDate } from '@/utils'
import { useNotificationStore } from '@/stores/notification'
import { renderNotificationSummary } from '@/utils/markdown'

// =====================================================
// Props & Emits 定义
// =====================================================
interface Props {
  notifications: NotificationItem[]
  loading?: boolean
}

interface Emits {
  (e: 'notification-click', notification: NotificationItem): void
  (e: 'mark-read', id: number): void
  (e: 'mark-unread', id: number): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

// =====================================================
// Store & Helpers
// =====================================================
const notificationStore = useNotificationStore()

// 已读状态检查 - 添加防御性检查
const isRead = (id: number): boolean => {
  try {
    return notificationStore.readStatusManager?.value?.isRead(id) || false
  } catch (error) {
    console.warn('⚠️ [NotificationListContent] 检查已读状态失败:', error)
    return false
  }
}

// =====================================================
// Event Handlers
// =====================================================
const handleNotificationClick = (notification: NotificationItem) => {
  emit('notification-click', notification)
}

const handleReadStatusToggle = (notification: NotificationItem) => {
  if (isRead(notification.id)) {
    emit('mark-unread', notification.id)
  } else {
    emit('mark-read', notification.id)
  }
}

// =====================================================
// Display Formatters
// =====================================================
const getLevelText = (level: number): string => {
  const levelMap = {
    1: '紧急',
    2: '重要', 
    3: '常规',
    4: '提醒'
  }
  return levelMap[level as keyof typeof levelMap] || '未知'
}

const getLevelTagType = (level: number): string => {
  const typeMap = {
    1: 'danger',   // 紧急 - 红色
    2: 'warning',  // 重要 - 橙色
    3: 'primary',  // 常规 - 蓝色
    4: 'success'   // 提醒 - 绿色
  }
  return typeMap[level as keyof typeof typeMap] || 'info'
}

// 🔧 P0级修复：使用formatApiDate替代formatTimeIntelligent，提供更好的null值处理

const getContentSummary = (content: string): string => {
  // 🚀 使用markdown渲染，支持换行、粗体、斜体、链接等格式
  return renderNotificationSummary(content, 100)
}

const getRoleDisplayName = (role: string): string => {
  const roleMap = {
    'SYSTEM_ADMIN': '系统管理员',
    'PRINCIPAL': '校长',
    'ACADEMIC_ADMIN': '教务主任', 
    'TEACHER': '教师',
    'CLASS_TEACHER': '班主任',
    'STUDENT': '学生',
    'SYSTEM': '系统'
  }
  return roleMap[role as keyof typeof roleMap] || role
}

const getScopeDisplayName = (scope: string): string => {
  const scopeMap = {
    'SCHOOL_WIDE': '全校范围',
    'DEPARTMENT': '部门范围',
    'GRADE': '年级范围', 
    'CLASS': '班级范围'
  }
  return scopeMap[scope as keyof typeof scopeMap] || scope
}

const getChannelsDisplayName = (channels: string): string => {
  if (!channels) return '未指定'
  
  const channelMap = {
    '1': '站内信',
    '2': '邮件',
    '3': '短信',
    '4': '微信',
    '5': '系统推送'
  }
  
  const channelList = channels.split(',')
    .map(id => channelMap[id as keyof typeof channelMap])
    .filter(Boolean)
  
  return channelList.length > 0 ? channelList.join(', ') : '未指定'
}
</script>

<style scoped>
/* =====================================================
 * 容器样式
 * ===================================================== */
.notification-list-content {
  height: 100%;
  overflow: auto;
}

.loading-container {
  padding: 20px;
}

/* =====================================================
 * 通知列表样式
 * ===================================================== */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}

.notification-item {
  position: relative;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
}

.notification-item:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

/* =====================================================
 * 已读/未读状态样式
 * ===================================================== */
.notification-unread {
  border-left: 4px solid var(--el-color-primary);
  background: linear-gradient(135deg, 
    var(--el-bg-color) 0%, 
    rgba(64, 158, 255, 0.03) 100%
  );
}

.notification-read {
  opacity: 0.75;
  border-left: 4px solid var(--el-color-info-light-5);
}

/* =====================================================
 * 级别样式
 * ===================================================== */
.level-1-emergency {
  border-left-color: var(--el-color-danger) !important;
}

.level-2-important {
  border-left-color: var(--el-color-warning) !important;
}

.level-3-regular {
  border-left-color: var(--el-color-primary) !important;
}

.level-4-reminder {
  border-left-color: var(--el-color-success) !important;
}

/* =====================================================
 * 通知卡片头部样式
 * ===================================================== */
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.notification-title-section {
  flex: 1;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.notification-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.notification-time {
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.notification-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* =====================================================
 * 通知内容样式
 * ===================================================== */
.notification-content {
  margin-bottom: 12px;
}

.notification-summary {
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  margin: 0 0 8px 0;

  /* 🚀 Markdown渲染支持样式 */
  /* 确保内嵌HTML标签正确显示 */
  word-wrap: break-word;
  overflow-wrap: break-word;

  /* 内嵌标签样式 */
  & strong, & b {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  & em, & i {
    font-style: italic;
    color: var(--el-text-color-regular);
  }

  & code {
    background: var(--el-fill-color-light);
    padding: 2px 4px;
    border-radius: 3px;
    font-family: 'Monaco', 'Consolas', monospace;
    font-size: 12px;
  }

  & a {
    color: var(--el-color-primary);
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }

  /* 处理段落和换行 */
  & p {
    margin: 0;
    &:not(:last-child) {
      margin-bottom: 4px;
    }
  }

  & br {
    line-height: 1.2;
  }
}

.notification-publisher {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.publisher-icon {
  font-size: 12px;
}

.publisher-name {
  font-weight: 500;
}

.publisher-role {
  opacity: 0.8;
}

/* =====================================================
 * 通知底部样式
 * ===================================================== */
.notification-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.notification-scope,
.notification-channels {
  display: flex;
  align-items: center;
  gap: 4px;
}

.scope-icon,
.channel-icon {
  font-size: 12px;
}

/* =====================================================
 * 未读指示器
 * ===================================================== */
.unread-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: var(--el-color-primary);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.7);
  }
  
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 10px rgba(64, 158, 255, 0);
  }
  
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}

/* =====================================================
 * 响应式设计
 * ===================================================== */
@media (max-width: 768px) {
  .notification-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .notification-actions {
    justify-content: flex-end;
  }
  
  .notification-footer {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
  
  .notification-item {
    padding: 12px;
  }
}

/* =====================================================
 * 暗色主题适配
 * ===================================================== */
@media (prefers-color-scheme: dark) {
  .notification-item {
    border-color: var(--el-border-color-darker);
  }
  
  .notification-item:hover {
    border-color: var(--el-color-primary-light-3);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.25);
  }
  
  .notification-unread {
    background: linear-gradient(135deg, 
      var(--el-bg-color) 0%, 
      rgba(64, 158, 255, 0.05) 100%
    );
  }
}
</style>