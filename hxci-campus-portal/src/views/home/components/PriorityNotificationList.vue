<!--
/**
 * 优先通知列表容器组件 (重构版)
 * 
 * @description 重构后的组件容器，集成三个专用通知组件，实现模块化架构
 * @author Claude Code AI
 * @date 2025-09-08 (重构)
 * @stage P1-FRONTEND-REFACTOR - 组件模块化重构完成
 * 
 * @responsibilities
 * - 作为通知组件容器，集成三个专用组件
 * - 统一处理通知数据分发和事件管理
 * - 提供统一的已读状态管理和交互处理
 * - 保持向后兼容，维持原有接口不变
 * 
 * @architecture-change
 * - 从单一组件 → 组件容器模式
 * - Level 1紧急 → EmergencyNotificationWidget
 * - Level 2重要 → ImportantNotificationWidget  
 * - Level 3常规 → RegularNotificationWidget
 * 
 * @design-principles
 * - 组件容器：负责数据分发和事件聚合
 * - 单一职责：每个子组件专注特定级别通知
 * - 向后兼容：保持原有Props/Emits接口
 * - 响应式UI：继承子组件的响应式特性
 */
-->

<template>
  <div class="priority-workspace-section">
    <!-- 重构后的组件容器头部 -->
    <div class="workspace-priority-header">
      <h4>🎯 优先处理通知</h4>
      <el-tag type="info" size="small">{{ priorityNotificationsCount }}条优先</el-tag>
    </div>
    
    <!-- 优化后的统一通知组件容器 -->
    <div class="priority-components-container" v-loading="isLoading">
      <!-- 紧急通知区块 (Level 1) -->
      <div v-if="emergencyNotifications.length > 0" class="priority-level-section emergency-section">
        <div class="level-header emergency-header">
          <div class="level-title">
            <Warning class="level-icon emergency-icon pulse" />
            <h4>🚨 紧急通知</h4>
          </div>
          <div class="level-count emergency-count">
            {{ emergencyNotifications.length }}条紧急
          </div>
        </div>
        <div class="notification-list">
          <BaseNotificationWidget
            v-for="notification in displayEmergencyNotifications"
            :key="notification.id"
            type="emergency"
            :notification="convertToComponentNotificationData(notification)"
            @click="handleNotificationClick"
            @mark-as-read="handleMarkAsRead"
            @view-details="handleNotificationClick"
          />
        </div>
        <div v-if="hasMoreEmergency" class="show-more-section">
          <button class="show-more-btn emergency-btn" @click="handleShowAllPriority">
            查看全部{{ emergencyNotifications.length }}条紧急通知 ⚠️
          </button>
        </div>
      </div>
      
      <!-- 重要通知区块 (Level 2) -->
      <div v-if="importantNotifications.length > 0" class="priority-level-section important-section">
        <div class="level-header important-header">
          <div class="level-title">
            <InfoFilled class="level-icon important-icon" />
            <h4>⚡ 重要通知</h4>
          </div>
          <div class="level-count important-count">
            {{ importantNotifications.length }}条重要
          </div>
        </div>
        <div class="notification-list">
          <BaseNotificationWidget
            v-for="notification in displayImportantNotifications"
            :key="notification.id"
            type="important"
            :notification="convertToComponentNotificationData(notification)"
            @click="handleNotificationClick"
            @mark-as-read="handleMarkAsRead"
            @view-details="handleNotificationClick"
          />
        </div>
        <div v-if="hasMoreImportant" class="show-more-section">
          <button class="show-more-btn important-btn" @click="handleShowAllPriority">
            查看全部{{ importantNotifications.length }}条重要通知 ⚡
          </button>
        </div>
      </div>
      
      <!-- 常规通知区块 (Level 3) -->
      <div v-if="regularNotifications.length > 0" class="priority-level-section regular-section">
        <div class="level-header regular-header">
          <div class="level-title">
            <ChatDotRound class="level-icon regular-icon" />
            <h4>📢 常规通知</h4>
          </div>
          <div class="level-count regular-count">
            {{ regularNotifications.length }}条常规
          </div>
        </div>
        <div class="notification-list">
          <BaseNotificationWidget
            v-for="notification in displayRegularNotifications"
            :key="notification.id"
            type="regular"
            :notification="convertToComponentNotificationData(notification)"
            @click="handleNotificationClick"
            @mark-as-read="handleMarkAsRead"
            @view-details="handleNotificationClick"
          />
        </div>
        <div v-if="hasMoreRegular" class="show-more-section">
          <button class="show-more-btn regular-btn" @click="handleShowAllPriority">
            查看全部{{ regularNotifications.length }}条常规通知 📢
          </button>
        </div>
      </div>
    </div>
    
    <!-- 统一查看更多按钮 -->
    <div v-if="hasAnyPriorityNotifications" class="show-more-priority">
      <el-button type="text" size="small" @click="handleShowAllPriority">
        查看全部{{ priorityNotificationsCount }}条优先通知 →
      </el-button>
    </div>
    
    <!-- 统一空状态 -->
    <div v-if="!hasAnyPriorityNotifications && !isLoading" class="empty-priority">
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
import type { NotificationItem } from '@/api/notification'

// ================== 常量定义 ==================

/** 紧急通知最大显示数量 */
const EMERGENCY_MAX_DISPLAY = 3

/** 重要通知最大显示数量 */
const IMPORTANT_MAX_DISPLAY = 4

/** 常规通知最大显示数量 */
const REGULAR_MAX_DISPLAY = 5

/** 移动端断点 */
const MOBILE_BREAKPOINT = 768
// 导入统一的基础通知组件
import BaseNotificationWidget from '@/components/BaseNotificationWidget.vue'
import type { ComponentNotificationData, NotificationType } from '@/components/BaseNotificationWidget.vue'
// 导入图标
import { Warning, InfoFilled, ChatDotRound } from '@element-plus/icons-vue'

// ================== Props定义 ==================

interface Props {
  /** 通知列表数据 */
  notifications: NotificationItem[]
  /** 最大显示数量 (已弃用，由各子组件独立控制) */
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
  /** 通知点击事件 (向后兼容) */
  (e: 'notification-click', notification: NotificationItem): void
  /** 标记已读事件 */
  (e: 'mark-read', notificationId: number): void
  /** 显示全部优先通知 */
  (e: 'show-all-priority'): void
}

const emit = defineEmits<Emits>()

// ================== 计算属性 ==================

/** 紧急通知列表 (Level 1) */
const emergencyNotifications = computed(() => {
  return props.notifications.filter(n => n.level === 1)
})

/** 重要通知列表 (Level 2) */
const importantNotifications = computed(() => {
  return props.notifications.filter(n => n.level === 2)
})

/** 常规通知列表 (Level 3) */
const regularNotifications = computed(() => {
  return props.notifications.filter(n => n.level === 3)
})

/** 显示的紧急通知列表（限制数量） */
const displayEmergencyNotifications = computed(() => {
  return emergencyNotifications.value.slice(0, EMERGENCY_MAX_DISPLAY)
})

/** 显示的重要通知列表（限制数量） */
const displayImportantNotifications = computed(() => {
  return importantNotifications.value.slice(0, IMPORTANT_MAX_DISPLAY)
})

/** 显示的常规通知列表（限制数量） */
const displayRegularNotifications = computed(() => {
  return regularNotifications.value.slice(0, REGULAR_MAX_DISPLAY)
})

/** 是否有更多紧急通知 */
const hasMoreEmergency = computed(() => {
  return emergencyNotifications.value.length > EMERGENCY_MAX_DISPLAY
})

/** 是否有更多重要通知 */
const hasMoreImportant = computed(() => {
  return importantNotifications.value.length > IMPORTANT_MAX_DISPLAY
})

/** 是否有更多常规通知 */
const hasMoreRegular = computed(() => {
  return regularNotifications.value.length > REGULAR_MAX_DISPLAY
})

/** 优先通知总数 (Level 1-3) */
const priorityNotificationsCount = computed(() => {
  return emergencyNotifications.value.length + 
         importantNotifications.value.length + 
         regularNotifications.value.length
})

/** 是否有任何优先通知 */
const hasAnyPriorityNotifications = computed(() => {
  return priorityNotificationsCount.value > 0
})

// ================== 辅助函数 ==================

/** 将NotificationItem转换为BaseNotificationWidget所需的格式 */
const convertToComponentNotificationData = (notification: NotificationItem): ComponentNotificationData => {
  return {
    ...notification,
    isRead: props.readStatusChecker ? props.readStatusChecker(notification.id) : (notification.isRead ?? false)
  }
}

// ================== 事件处理器 ==================

/** 处理通知点击 (统一事件处理器) */
const handleNotificationClick = (notification: ComponentNotificationData) => {
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
/* 优先通知容器组件样式 (重构版) */
.priority-workspace-section {
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* 组件容器布局 */
.priority-components-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 优先级别区块样式 */
.priority-level-section {
  border-radius: 12px;
  padding: 16px;
  position: relative;
  overflow: hidden;
}

/* 紧急通知区块 */
.emergency-section {
  background: linear-gradient(135deg, #fef2f2 0%, #fecaca 20%, #ffffff 100%);
  border: 2px solid #dc2626;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.15);
}

.emergency-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #dc2626, #ef4444, #dc2626);
  animation: emergency-flash 2s ease-in-out infinite;
}

/* 重要通知区块 */
.important-section {
  background: linear-gradient(135deg, #fff7ed 0%, #fed7aa 20%, #ffffff 100%);
  border: 2px solid #f97316;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.15);
}

/* 常规通知区块 */
.regular-section {
  background: linear-gradient(135deg, #eff6ff 0%, #bfdbfe 20%, #ffffff 100%);
  border: 2px solid #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

/* 级别头部样式 */
.level-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.level-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-title h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.level-icon {
  width: 20px;
  height: 20px;
}

.emergency-header .level-title h4 {
  color: #dc2626;
  text-shadow: 0 1px 2px rgba(220, 38, 38, 0.1);
}

.emergency-icon {
  color: #dc2626;
}

.important-header .level-title h4 {
  color: #f97316;
  text-shadow: 0 1px 2px rgba(249, 115, 22, 0.1);
}

.important-icon {
  color: #f97316;
}

.regular-header .level-title h4 {
  color: #3b82f6;
  text-shadow: 0 1px 2px rgba(59, 130, 246, 0.1);
}

.regular-icon {
  color: #3b82f6;
}

/* 计数标签样式 */
.level-count {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.emergency-count {
  background: #dc2626;
}

.important-count {
  background: #f97316;
}

.regular-count {
  background: #3b82f6;
}

/* 通知列表样式 */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

/* 查看更多按钮区域 */
.show-more-section {
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.show-more-btn {
  background: transparent;
  border: 1px solid;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.emergency-btn {
  border-color: #dc2626;
  color: #dc2626;
}

.emergency-btn:hover {
  background: #dc2626;
  color: white;
}

.important-btn {
  border-color: #f97316;
  color: #f97316;
}

.important-btn:hover {
  background: #f97316;
  color: white;
}

.regular-btn {
  border-color: #3b82f6;
  color: #3b82f6;
}

.regular-btn:hover {
  background: #3b82f6;
  color: white;
}

/* 闪烁动画 */
.pulse {
  animation: emergency-pulse 1.5s ease-in-out infinite;
}

@keyframes emergency-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

@keyframes emergency-flash {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* 优先区块头部 */
.workspace-priority-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.workspace-priority-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

/* 查看更多按钮 */
.show-more-priority {
  margin-top: 16px;
  text-align: center;
  padding-top: 16px;
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
  
  .priority-components-container {
    gap: 20px;
  }
  
  .priority-level-section {
    padding: 12px;
    border-radius: 8px;
  }
  
  .level-title h4 {
    font-size: 14px;
  }
  
  .level-icon {
    width: 18px;
    height: 18px;
  }
  
  .level-count {
    font-size: 11px;
    padding: 3px 6px;
  }
  
  .notification-list {
    gap: 10px;
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
    color: #e0e0e0;
  }
  
  /* 紧急通知暗色主题 */
  .emergency-section {
    background: linear-gradient(135deg, #2c1810 0%, #3c1f1f 20%, #2a2a2a 100%);
    border-color: #ef4444;
  }
  
  .emergency-header .level-title h4 {
    color: #ef4444;
  }
  
  .emergency-icon {
    color: #ef4444;
  }
  
  .emergency-count {
    background: #ef4444;
  }
  
  .emergency-btn {
    border-color: #ef4444;
    color: #ef4444;
  }
  
  .emergency-btn:hover {
    background: #ef4444;
  }
  
  /* 重要通知暗色主题 */
  .important-section {
    background: linear-gradient(135deg, #2c1f10 0%, #3c2a1f 20%, #2a2a2a 100%);
    border-color: #fb923c;
  }
  
  .important-header .level-title h4 {
    color: #fb923c;
  }
  
  .important-icon {
    color: #fb923c;
  }
  
  .important-count {
    background: #fb923c;
  }
  
  .important-btn {
    border-color: #fb923c;
    color: #fb923c;
  }
  
  .important-btn:hover {
    background: #fb923c;
  }
  
  /* 常规通知暗色主题 */
  .regular-section {
    background: linear-gradient(135deg, #1e1f2c 0%, #1f2a3c 20%, #2a2a2a 100%);
    border-color: #60a5fa;
  }
  
  .regular-header .level-title h4 {
    color: #60a5fa;
  }
  
  .regular-icon {
    color: #60a5fa;
  }
  
  .regular-count {
    background: #60a5fa;
  }
  
  .regular-btn {
    border-color: #60a5fa;
    color: #60a5fa;
  }
  
  .regular-btn:hover {
    background: #60a5fa;
  }
  
  /* 级别头部暗色主题 */
  .level-header {
    border-bottom-color: #3a3a3a;
  }
  
  .show-more-section {
    border-top-color: #3a3a3a;
  }
}
</style>