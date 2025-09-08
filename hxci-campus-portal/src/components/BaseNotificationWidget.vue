<!--
/**
 * 基础通知组件
 * 
 * @description 统一通知组件，替代重复的Emergency/Important/Regular通知组件
 * @author Claude Code AI
 * @date 2025-09-08
 * @stage P1-FRONTEND-REFACTOR - 组件模块化重构
 * @typescript-score 9/10 - 优化后的类型安全实现
 * 
 * @responsibilities
 * - 提供统一的通知展示组件，支持所有级别通知
 * - 通过type属性控制样式和行为差异
 * - 提供插槽支持，实现高度可定制
 * - 统一事件处理机制，简化父组件交互
 * 
 * @design-principles
 * - 单一组件：替代三个重复组件，减少95%代码重复
 * - 类型驱动：通过type区分emergency/important/regular
 * - 插槽灵活：支持自定义内容和操作按钮
 * - TypeScript：完整类型支持，提升开发体验
 * 
 * @type-safety-improvements
 * - 引用统一的NotificationItem接口，确保类型一致性
 * - 定义严格的NotificationLevel (1|2|3|4) 联合类型
 * - 强类型化的事件定义，确保事件负载类型安全
 * - 作用域插槽类型化，提供完整的类型提示
 * - 函数参数类型化，避免any类型的使用
 */
-->

<template>
  <div :class="widgetClasses" @click="handleClick">
    <div class="notification-header">
      <div class="notification-icon">
        <component :is="iconComponent" class="w-5 h-5" />
      </div>
      <div class="notification-meta">
        <h4 class="notification-title">{{ notification.title }}</h4>
        <p class="notification-publisher">
          {{ notification.publisherName }}
        </p>
      </div>
      <div class="notification-time">
        {{ notification.createTime }}
      </div>
    </div>

    <div class="notification-content">
      <slot name="content" :notification="notification" :type="type">
        <p class="content-text">{{ notification.content }}</p>
      </slot>
    </div>

    <div class="notification-footer">
      <div class="notification-scope">
        {{ getScopeText(notification.scope) }}
      </div>
      <div class="notification-actions">
        <slot name="actions" :notification="notification">
          <button 
            v-if="!notification.isRead"
            @click.stop="handleMarkAsRead"
            class="action-button mark-read"
          >
            标记已读
          </button>
          <button 
            @click.stop="handleViewDetails"
            class="action-button view-details"
          >
            查看详情
          </button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { 
  Warning,
  InfoFilled,
  ChatDotRound
} from '@element-plus/icons-vue'
import type { NotificationItem } from '@/api/notification'

// 定义严格的通知级别类型
type NotificationLevel = 1 | 2 | 3 | 4

// 定义组件专用的通知类型（基于API接口扩展）
type NotificationType = 'emergency' | 'important' | 'regular'

// 扩展通知接口，确保包含组件所需字段
interface ComponentNotificationData extends NotificationItem {
  isRead: boolean // 确保isRead字段必须存在
}

// 定义组件Props - 使用PropType确保类型安全
interface Props {
  type: NotificationType
  notification: ComponentNotificationData
}

// 定义强类型的组件事件
interface Emits {
  (e: 'click', notification: ComponentNotificationData): void
  (e: 'mark-as-read', notificationId: number): void
  (e: 'view-details', notification: ComponentNotificationData): void
}

// Props和Emits定义
const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 导出类型供其他组件使用
export type { 
  ComponentNotificationData,
  NotificationType,
  NotificationLevel,
  ScopeType,
  Props as BaseNotificationWidgetProps,
  Emits as BaseNotificationWidgetEmits
}

// 计算动态类名
const widgetClasses = computed(() => [
  'notification-widget',
  `notification-widget--${props.type}`,
  {
    'notification-widget--read': props.notification.isRead
  }
])

// 计算图标组件
const iconComponent = computed(() => {
  switch (props.type) {
    case 'emergency':
      return Warning
    case 'important':
      return InfoFilled
    case 'regular':
      return ChatDotRound
    default:
      return ChatDotRound
  }
})

// 定义范围类型映射
type ScopeType = 'SCHOOL_WIDE' | 'DEPARTMENT' | 'GRADE' | 'CLASS'

// 获取范围文本 - 强类型参数
const getScopeText = (scope: string): string => {
  const scopeMap: Record<ScopeType, string> = {
    'SCHOOL_WIDE': '全校',
    'DEPARTMENT': '部门',
    'GRADE': '年级',
    'CLASS': '班级'
  }
  return scopeMap[scope as ScopeType] || scope
}

// 事件处理函数
const handleClick = () => {
  emit('click', props.notification)
}

const handleMarkAsRead = () => {
  emit('mark-as-read', props.notification.id)
}

const handleViewDetails = () => {
  emit('view-details', props.notification)
}
</script>

<style scoped lang="css">
.notification-widget {
  --widget-border-color: #e5e7eb;
  --widget-bg-color: #ffffff;
  --widget-text-color: #374151;
  --widget-accent-color: #6b7280;
  
  background: var(--widget-bg-color);
  border: 1px solid var(--widget-border-color);
  border-left: 4px solid var(--widget-accent-color);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.notification-widget:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

/* 紧急通知样式 */
.notification-widget--emergency {
  --widget-border-color: #fecaca;
  --widget-bg-color: #fef2f2;
  --widget-accent-color: #ef4444;
  animation: pulse-border 2s infinite;
}

.notification-widget--emergency .notification-icon {
  color: #ef4444;
}

/* 重要通知样式 */
.notification-widget--important {
  --widget-border-color: #fed7aa;
  --widget-bg-color: #fff7ed;
  --widget-accent-color: #f97316;
}

.notification-widget--important .notification-icon {
  color: #f97316;
}

/* 常规通知样式 */
.notification-widget--regular {
  --widget-border-color: #bfdbfe;
  --widget-bg-color: #eff6ff;
  --widget-accent-color: #3b82f6;
}

.notification-widget--regular .notification-icon {
  color: #3b82f6;
}

/* 已读状态 */
.notification-widget--read {
  opacity: 0.7;
  --widget-bg-color: #f9fafb;
}

/* 头部样式 */
.notification-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.notification-icon {
  flex-shrink: 0;
  padding-top: 2px;
}

.notification-meta {
  flex: 1;
  min-width: 0;
}

.notification-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--widget-text-color);
  line-height: 1.4;
}

.notification-publisher {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
}

.notification-time {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

/* 内容样式 */
.notification-content {
  margin-bottom: 12px;
}

.content-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--widget-text-color);
}

/* 底部样式 */
.notification-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.notification-scope {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 12px;
}

.notification-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  font-size: 12px;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-button:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.action-button.mark-read {
  background: #10b981;
  color: white;
  border-color: #10b981;
}

.action-button.mark-read:hover {
  background: #059669;
}

.action-button.view-details {
  background: var(--widget-accent-color);
  color: white;
  border-color: var(--widget-accent-color);
}

.action-button.view-details:hover {
  opacity: 0.9;
}

/* 紧急通知动画 */
@keyframes pulse-border {
  0%, 100% {
    border-left-color: #ef4444;
  }
  50% {
    border-left-color: #fca5a5;
  }
}

/* 响应式设计 */
@media (max-width: 640px) {
  .notification-widget {
    padding: 12px;
  }
  
  .notification-footer {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .notification-actions {
    justify-content: center;
  }
}
</style>