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
    width="60%"
    class="notification-detail-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    append-to-body
    @close="handleClose"
  >
    <!-- 自定义对话框头部 -->
    <template #header>
      <div class="dialog-header">
        <div class="title-row">
          <el-tag
            :type="getLevelTagType(notification?.level || 0)"
            effect="dark"
            size="small"
            class="level-chip"
          >
            {{ getLevelText(notification?.level || 0) }}
          </el-tag>
          <h3 class="dialog-title">{{ notification?.title || '通知详情' }}</h3>
        </div>
        <div v-if="notification" class="subtitle">
          发布时间：{{ formatDateTime(notification.createTime) }}
          <el-divider direction="vertical" />
          状态：
          <el-tag
            :type="isNotificationRead ? 'success' : 'warning'"
            size="small"
          >
            {{ isNotificationRead ? '已读' : '未读' }}
          </el-tag>
        </div>
      </div>
    </template>
    <!-- 通知详情内容 -->
    <div v-if="notification" class="notification-detail" v-loading="isLoading">
      <!-- 元数据：使用描述表组件 -->
      <el-descriptions
        class="meta-descriptions"
        :column="2"
        border
        size="small"
      >
        <el-descriptions-item label="发布者">
          {{ notification.publisherName }}
        </el-descriptions-item>
        <el-descriptions-item label="发布角色">
          <el-tag size="small" type="info">{{ getRoleText(notification.publisherRole) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布范围">
          <el-tag size="small" type="info">{{ getScopeText(notification.targetScope || 'SCHOOL_WIDE') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="通知级别">
          <el-tag :type="getLevelTagType(notification.level)" effect="plain" size="small">
            {{ getLevelText(notification.level) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="notification.summary" label="摘要" :span="2">
          <span class="meta-summary">{{ notification.summary }}</span>
        </el-descriptions-item>
      </el-descriptions>
      
      <!-- 通知内容：使用增强版Markdown渲染 -->
      <div class="notification-content-detail">
        <div class="markdown-body" v-html="sanitizedHtml"></div>
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
import { renderNotificationDialog } from '@/utils/markdown'

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
  visible: false,
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

/** 使用增强版渲染器处理通知内容 */
const sanitizedHtml = computed(() => {
  if (!props.notification?.content) {
    return '<p class="empty-content">暂无内容</p>'
  }
  return renderNotificationDialog(props.notification.content)
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
// formatNotificationContent函数已由renderNotificationDialog替代

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

/* 自定义对话框头部 */
:deep(.notification-detail-dialog .el-dialog__header) {
  background: linear-gradient(135deg, #ffffff 0%, #f7f9fc 100%);
  border-bottom: 1px solid #e9ecef;
  margin-right: 0;
  padding: 16px 20px;
}

.dialog-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.level-chip {
  font-weight: 700;
  letter-spacing: .5px;
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.subtitle {
  font-size: 12px;
  color: #6b7280;
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

/* 元数据描述表 */
.meta-descriptions {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 8px;
}

.meta-summary {
  font-style: italic;
  color: #6c757d;
}

/* 通知内容 */
.notification-content-detail {
  background: #ffffff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 20px;
}

/* GitHub风格的Markdown样式 */
.markdown-body {
  color: #374151;
  line-height: 1.7;
  font-size: 15px;
  word-wrap: break-word;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  font-weight: 700;
  color: #111827;
  margin: 18px 0 10px;
}

.markdown-body :deep(h1) { font-size: 24px; }
.markdown-body :deep(h2) { font-size: 20px; }
.markdown-body :deep(h3) { font-size: 18px; }

.markdown-body :deep(p) { margin: 10px 0; }

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 8px 0 8px 22px;
}

.markdown-body :deep(li) { margin: 4px 0; }

.markdown-body :deep(a) {
  color: #2563eb;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: .9em;
  color: #b91c1c;
}

.markdown-body :deep(pre) {
  background: #111827;
  color: #e5e7eb;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-body :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #d1d5db;
  margin: 12px 0;
  padding: 6px 12px;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 4px;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 20px 0;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 14px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 8px 10px;
}

.markdown-body :deep(th) {
  background: #f3f4f6;
  font-weight: 600;
}

.empty-content {
  color: #9ca3af;
  font-style: italic;
}

.fallback-content {
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 6px;
  padding: 12px;
  color: #92400e;
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

  .dialog-title { color: #e5e7eb; }
  .subtitle { color: #9ca3af; }

  .meta-descriptions {
    border-color: #3a3a3a;
  }

  .meta-summary {
    color: #9ca3af;
  }

  .notification-content-detail {
    background: #2a2a2a;
    border-color: #3a3a3a;
  }

  .markdown-body { color: #d1d5db; }
  .markdown-body :deep(a) { color: #60a5fa; }
  .markdown-body :deep(code) {
    background: #1f2937;
    color: #fca5a5;
  }
  .markdown-body :deep(pre) {
    background: #0b0f16;
    color: #d1d5db;
  }
  .markdown-body :deep(blockquote) {
    background: #1f2937;
    border-left-color: #374151;
    color: #9ca3af;
  }
  .markdown-body :deep(th) {
    background: #1f2937;
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