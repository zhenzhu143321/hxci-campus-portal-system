<!--
/**
 * 通知详情弹窗组件
 * 
 * @description 展示通知完整详情的弹窗组件，包含通知元数据和内容
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21
 * @stage Stage 6 - Home.vue组件拆分架构
 * 
 * @responsibilities
 * - 展示通知的完整详情信息
 * - 提供标记已读操作
 * - 支持关闭弹窗功能
 * - 响应式布局适配
 * 
 * @features
 * - 完整信息展示：级别、发布者、范围、时间、内容
 * - 可视化级别标识：不同级别使用不同颜色
 * - 操作按钮：标记已读、关闭弹窗
 * - 格式化内容：支持富文本内容显示
 */
-->

<template>
  <el-dialog 
    v-model="dialogVisible" 
    :title="notification?.title || '通知详情'" 
    width="60%"
    class="notification-detail-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    append-to-body
    @close="handleClose"
  >
    <!-- 通知详情内容 -->
    <div v-if="notification" class="notification-detail" v-loading="isLoading">
      <!-- 通知元数据 -->
      <div class="notification-meta">
        <div class="meta-row">
          <span class="meta-label">级别：</span>
          <el-tag 
            :type="getLevelTagType(notification.level)" 
            effect="plain" 
            size="small"
            class="level-tag-detail"
          >
            {{ getLevelText(notification.level) }}
          </el-tag>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">发布者：</span>
          <span class="meta-value">{{ notification.publisherName }}</span>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">发布角色：</span>
          <el-tag size="small" type="info">{{ getRoleText(notification.publisherRole) }}</el-tag>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">发布范围：</span>
          <el-tag size="small" type="info">{{ getScopeText(notification.targetScope) }}</el-tag>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">发布时间：</span>
          <span class="meta-value">{{ formatDateTime(notification.createTime) }}</span>
        </div>
        
        <div v-if="notification.summary" class="meta-row">
          <span class="meta-label">摘要：</span>
          <span class="meta-value meta-summary">{{ notification.summary }}</span>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">状态：</span>
          <el-tag 
            :type="isNotificationRead ? 'success' : 'warning'" 
            size="small"
          >
            {{ isNotificationRead ? '已读' : '未读' }}
          </el-tag>
        </div>
      </div>
      
      <!-- 通知内容 -->
      <div class="notification-content-detail">
        <h4>通知内容：</h4>
        <div class="content-text formatted-content">
          {{ formatNotificationContent(notification.content) }}
        </div>
      </div>
      
      <!-- 附加信息 -->
      <div v-if="notification.requireConfirm" class="notification-additional">
        <el-alert
          title="此通知需要确认"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            请确认您已阅读并理解此通知内容
          </template>
        </el-alert>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else-if="!isLoading" class="empty-notification">
      <el-empty description="通知不存在或已被删除" :image-size="80">
        <template #description>
          <p style="color: #909399;">通知信息无法加载</p>
        </template>
      </el-empty>
    </div>
    
    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button 
          v-if="!isNotificationRead"
          type="primary" 
          :loading="isMarkingRead"
          @click="handleMarkAsRead"
        >
          <el-icon><Check /></el-icon>
          标记已读
        </el-button>
        <el-button 
          v-else
          type="success" 
          disabled
        >
          <el-icon><CircleCheck /></el-icon>
          已读
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Check, CircleCheck } from '@element-plus/icons-vue'
import type { NotificationItem } from '@/api/notification'
import dayjs from 'dayjs'
import { formatDateTime } from '@/utils'

// ================== Props定义 ==================

interface Props {
  /** 弹窗显示状态 */
  visible: boolean
  /** 通知详情数据 */
  notification?: NotificationItem | null
  /** 是否加载中 */
  isLoading?: boolean
  /** 是否正在标记已读 */
  isMarkingRead?: boolean
  /** 已读状态检查函数 */
  readStatusChecker?: (id: number) => boolean
}

const props = withDefaults(defineProps<Props>(), {
  notification: null,
  isLoading: false,
  isMarkingRead: false,
  readStatusChecker: () => false
})

// ================== Emits定义 ==================

interface Emits {
  /** 更新弹窗显示状态 */
  (e: 'update:visible', visible: boolean): void
  /** 关闭弹窗 */
  (e: 'close'): void
  /** 标记已读 */
  (e: 'mark-read', notificationId: number): void
}

const emit = defineEmits<Emits>()

// ================== 响应式状态 ==================

/** 内部弹窗显示状态 */
const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => {
    emit('update:visible', value)
  }
})

/** 通知是否已读 */
const isNotificationRead = computed(() => {
  return props.notification ? props.readStatusChecker(props.notification.id) : false
})

// ================== 工具函数 ==================

/** 获取级别标签类型 */
const getLevelTagType = (level: number): string => {
  switch (level) {
    case 1: return 'danger'   // 紧急 - 红色
    case 2: return 'warning'  // 重要 - 橙色
    case 3: return 'primary'  // 常规 - 蓝色
    case 4: return 'success'  // 提醒 - 绿色
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

/** 获取角色文本 */
const getRoleText = (role: string): string => {
  const roleMap: Record<string, string> = {
    'SYSTEM_ADMIN': '系统管理员',
    'PRINCIPAL': '校长',
    'ACADEMIC_ADMIN': '教务主任',
    'TEACHER': '教师',
    'CLASS_TEACHER': '班主任',
    'STUDENT': '学生',
    'SYSTEM': '系统'
  }
  return roleMap[role] || role
}

/** 获取范围文本 */
const getScopeText = (scope: string): string => {
  const scopeMap: Record<string, string> = {
    'SCHOOL_WIDE': '全校',
    'DEPARTMENT': '部门',
    'GRADE': '年级',
    'CLASS': '班级'
  }
  return scopeMap[scope] || scope
}

// formatDateTime函数已迁移到 @/utils

/** 格式化通知内容 */
const formatNotificationContent = (content: string): string => {
  if (!content) return '暂无内容'
  
  // 简单的格式化处理
  return content
    .replace(/\n/g, '<br>')  // 换行处理
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // 粗体处理
    .replace(/\*(.*?)\*/g, '<em>$1</em>')  // 斜体处理
}

// ================== 事件处理器 ==================

/** 处理关闭弹窗 */
const handleClose = () => {
  emit('close')
  emit('update:visible', false)
}

/** 处理标记已读 */
const handleMarkAsRead = () => {
  if (props.notification && !isNotificationRead.value) {
    emit('mark-read', props.notification.id)
  }
}

// ================== 监听器 ==================

/** 监听通知变化，重置滚动位置 */
watch(() => props.notification, (newNotification) => {
  if (newNotification) {
    // 重置弹窗滚动位置到顶部
    setTimeout(() => {
      const dialogContent = document.querySelector('.notification-detail-dialog .el-dialog__body')
      if (dialogContent) {
        dialogContent.scrollTop = 0
      }
    }, 100)
  }
}, { deep: true })
</script>

<style scoped>
/* 弹窗样式 */
:deep(.notification-detail-dialog) {
  --el-dialog-border-radius: 12px;
}

:deep(.notification-detail-dialog .el-dialog__header) {
  background: linear-gradient(135deg, #f8f9ff 0%, #ffffff 100%);
  border-bottom: 1px solid #e4e7ed;
  border-radius: 12px 12px 0 0;
}

:deep(.notification-detail-dialog .el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 通知详情容器 */
.notification-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
}

/* 通知元数据 */
.notification-meta {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.meta-label {
  font-size: 14px;
  font-weight: 600;
  color: #495057;
  min-width: 80px;
  flex-shrink: 0;
}

.meta-value {
  font-size: 14px;
  color: #212529;
  flex: 1;
  line-height: 1.5;
}

.meta-summary {
  font-style: italic;
  color: #6c757d;
}

.level-tag-detail {
  font-weight: 600;
}

/* 通知内容 */
.notification-content-detail {
  background: #ffffff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 20px;
}

.notification-content-detail h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #212529;
  padding-bottom: 8px;
  border-bottom: 2px solid #e9ecef;
}

.content-text {
  font-size: 15px;
  color: #495057;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.formatted-content {
  /* 支持富文本内容 */
}

.formatted-content :deep(strong) {
  font-weight: 600;
  color: #212529;
}

.formatted-content :deep(em) {
  font-style: italic;
  color: #6c757d;
}

/* 附加信息 */
.notification-additional {
  margin-top: 16px;
}

/* 空状态 */
.empty-notification {
  text-align: center;
  padding: 60px 20px;
}

/* 底部操作栏 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e9ecef;
}

/* 响应式设计 */
@media (max-width: 768px) {
  :deep(.notification-detail-dialog) {
    width: 95% !important;
    margin: 20px auto !important;
  }
  
  .notification-detail {
    max-height: 60vh;
  }
  
  .notification-meta,
  .notification-content-detail {
    padding: 12px;
  }
  
  .meta-row {
    flex-direction: column;
    gap: 4px;
  }
  
  .meta-label {
    min-width: auto;
  }
  
  .content-text {
    font-size: 14px;
  }
  
  .dialog-footer {
    flex-direction: column;
    gap: 8px;
  }
  
  .dialog-footer .el-button {
    width: 100%;
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  :deep(.notification-detail-dialog .el-dialog__header) {
    background: linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 100%);
    border-bottom-color: #3a3a3a;
  }
  
  :deep(.notification-detail-dialog .el-dialog__title) {
    color: #e0e0e0;
  }
  
  .notification-meta {
    background: #2a2a2a;
    border-color: #3a3a3a;
  }
  
  .meta-label {
    color: #e0e0e0;
  }
  
  .meta-value {
    color: #d0d0d0;
  }
  
  .meta-summary {
    color: #9ca3af;
  }
  
  .notification-content-detail {
    background: #2a2a2a;
    border-color: #3a3a3a;
  }
  
  .notification-content-detail h4 {
    color: #e0e0e0;
    border-bottom-color: #3a3a3a;
  }
  
  .content-text {
    color: #d0d0d0;
  }
  
  .dialog-footer {
    border-top-color: #3a3a3a;
  }
}

/* 滚动条样式 */
.notification-detail::-webkit-scrollbar {
  width: 6px;
}

.notification-detail::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.notification-detail::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.notification-detail::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 加载状态 */
:deep(.el-loading-mask) {
  border-radius: 8px;
}
</style>