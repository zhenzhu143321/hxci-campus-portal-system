<!--
/**
 * 通知消息组件 (Level 4 专用)
 * 
 * @description 专门展示Level 4通知消息的组件，温馨提醒类通知
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21
 * @stage Stage 6 - Home.vue组件拆分架构
 * 
 * @responsibilities
 * - 展示Level 4级别的通知消息
 * - 处理已读状态的展示和操作
 * - 提供简洁的消息列表界面
 * - 支持消息点击和标记已读
 * 
 * @features
 * - 简洁设计：Level 4消息以轻量级方式展示
 * - 已读状态：支持标记已读和视觉反馈
 * - 响应式布局：适配移动端显示
 * - 绿色主题：温馨提醒的视觉风格
 */
-->

<template>
  <div class="level4-message-container">
    <!-- 🚨 核心修复：移除workspace-module-card类，避免与父容器双重卡片嵌套 -->
    <!-- 标题由父容器 IntelligentNotificationWorkspace.vue 提供 -->
    
    <!-- Level 4 消息列表 -->
    <div class="level4-messages-list" v-loading="isLoading">
      <div 
        v-for="message in displayMessages" 
        :key="message.id"
        class="level4-message-item"
        :class="{ 
          'level4-read': isMessageRead(message.id),
          'marking-read': isMarkingRead(message.id)
        }"
        @click="handleMessageClick(message)"
      >
        <!-- 消息图标 -->
        <div class="level4-icon">
          <el-icon :style="{ color: '#67C23A' }"><Bell /></el-icon>
        </div>
        
        <!-- 消息内容 -->
        <div class="level4-content">
          <div class="level4-title">{{ message.title }}</div>
          <div class="level4-time">{{ formatDate(message.createTime) }}</div>
        </div>
        
        <!-- 操作按钮 -->
        <div class="level4-actions">
          <el-button 
            v-if="!isMessageRead(message.id)"
            type="success" 
            size="small" 
            :loading="isMarkingRead(message.id)"
            @click.stop="handleMarkAsRead(message.id)"
            class="mark-read-btn"
          >
            <el-icon><Check /></el-icon>
            标记已读
          </el-button>
          
          <el-tag 
            v-else
            type="info" 
            size="small"
            effect="plain"
            class="level4-read-tag"
          >
            <el-icon><CircleCheck /></el-icon>
            已读
          </el-tag>
        </div>
      </div>
    </div>
    
    <!-- 查看更多 -->
    <div v-if="hasMore" class="show-more-level4">
      <el-button type="text" size="small" @click="handleShowMore">
        查看全部{{ messages.length }}条消息 →
      </el-button>
    </div>
    
    <!-- 空状态 -->
    <div v-if="messages.length === 0 && !isLoading" class="empty-messages">
      <el-empty description="暂无通知消息" :image-size="60">
        <template #description>
          <p style="color: #909399; font-size: 14px;">暂无温馨提醒消息</p>
          <p style="color: #c0c4cc; font-size: 12px;">新的提醒消息会显示在这里</p>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Bell, Check, CircleCheck } from '@element-plus/icons-vue'
import type { NotificationItem } from '@/api/notification'
import dayjs from 'dayjs'

// ================== Props定义 ==================

interface Props {
  /** Level 4 消息列表 */
  messages: NotificationItem[]
  /** 最大显示数量 */
  maxDisplay?: number
  /** 是否加载中 */
  isLoading?: boolean
  /** 标记已读中的消息ID列表 */
  markingReadIds?: number[]
  /** 已读状态检查函数 */
  readStatusChecker?: (id: number) => boolean
}

const props = withDefaults(defineProps<Props>(), {
  maxDisplay: 4,
  isLoading: false,
  markingReadIds: () => [],
  readStatusChecker: () => false
})

// ================== Emits定义 ==================

interface Emits {
  /** 消息点击事件 */
  (e: 'notification-click', message: NotificationItem, markAsRead?: boolean): void
  /** 标记已读事件 */
  (e: 'mark-read', messageId: number): void
  /** 显示更多消息 */
  (e: 'show-more'): void
}

const emit = defineEmits<Emits>()

// ================== 计算属性 ==================

/** 显示的消息列表（限制数量） */
const displayMessages = computed(() => {
  return props.messages.slice(0, props.maxDisplay)
})

/** 是否有更多消息 */
const hasMore = computed(() => {
  return props.messages.length > props.maxDisplay
})

// ================== 工具函数 ==================

/** 格式化日期 */
const formatDate = (dateStr: string): string => {
  return dayjs(dateStr).format('MM-DD HH:mm')
}

/** 检查消息是否已读 */
const isMessageRead = (messageId: number): boolean => {
  return props.readStatusChecker(messageId)
}

/** 检查是否正在标记已读 */
const isMarkingRead = (messageId: number): boolean => {
  return props.markingReadIds.includes(messageId)
}

// ================== 事件处理器 ==================

/** 处理消息点击 */
const handleMessageClick = (message: NotificationItem) => {
  emit('notification-click', message, false)
}

/** 处理标记已读 */
const handleMarkAsRead = (messageId: number) => {
  console.log('🔄 [NotificationMessagesWidget] 标记已读请求:', messageId)
  emit('mark-read', messageId)
  
  // 🔧 新增调试：输出当前消息列表状态
  console.log('📝 [NotificationMessagesWidget] 当前消息数量:', props.messages.length)
  console.log('📝 [NotificationMessagesWidget] 未读消息:', 
    props.messages.filter(m => !props.readStatusChecker(m.id)).length)
}

/** 处理显示更多 */
const handleShowMore = () => {
  emit('show-more')
}
</script>

<style scoped>
/* 🚨 核心修复：Level 4 容器样式 - 移除卡片样式避免嵌套 */
.level4-message-container {
  /* 移除背景和边框，由父容器提供卡片样式 */
  padding: 0;
  /* 保留核心布局功能 */
}

/* 消息列表 */
.level4-messages-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 消息项 */
.level4-message-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.level4-message-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.15);
  border-color: #22c55e;
}

/* 已读状态 */
.level4-read {
  opacity: 0.7;
  background: #f9fafb;
}

.level4-read:hover {
  opacity: 0.85;
}

/* 标记已读中状态 */
.marking-read {
  opacity: 0.6;
  pointer-events: none;
}

/* 消息图标 */
.level4-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #f0fdf4;
  border-radius: 50%;
  flex-shrink: 0;
}

.level4-read .level4-icon {
  background: #f3f4f6;
  opacity: 0.7;
}

/* 消息内容 */
.level4-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.level4-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  line-height: 1.4;
  
  /* 文本溢出处理 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.level4-read .level4-title {
  color: #6b7280;
}

.level4-time {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.3;
}

.level4-read .level4-time {
  color: #9ca3af;
}

/* 操作按钮 */
.level4-actions {
  flex-shrink: 0;
}

.mark-read-btn {
  --el-button-size: 28px;
  font-size: 12px;
  padding: 4px 8px;
}

.level4-read-tag {
  font-size: 11px;
  --el-tag-border-color: #d1fae5;
  --el-tag-bg-color: #f0fdf4;
}

/* 查看更多 */
.show-more-level4 {
  margin-top: 12px;
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

/* 空状态 */
.empty-messages {
  text-align: center;
  padding: 40px 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .level4-message-item {
    padding: 10px 12px;
    gap: 10px;
  }
  
  .level4-icon {
    width: 28px;
    height: 28px;
  }
  
  .level4-title {
    font-size: 13px;
  }
  
  .level4-time {
    font-size: 11px;
  }
  
  .mark-read-btn {
    --el-button-size: 24px;
    font-size: 11px;
    padding: 3px 6px;
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .level4-module {
    background: linear-gradient(135deg, #1a2d1a 0%, #2a2a2a 100%);
    border-color: #3a3a3a;
  }
  
  .level4-message-item {
    background: #2a2a2a;
    border-color: #3a3a3a;
  }
  
  .level4-message-item:hover {
    border-color: #22c55e;
  }
  
  .level4-read {
    background: #1f1f1f;
  }
  
  .level4-icon {
    background: #2d3a2d;
  }
  
  .level4-read .level4-icon {
    background: #3a3a3a;
  }
  
  .level4-title {
    color: #e0e0e0;
  }
  
  .level4-read .level4-title {
    color: #9ca3af;
  }
  
  .level4-time {
    color: #9ca3af;
  }
  
  .level4-read .level4-time {
    color: #6b7280;
  }
  
  .level4-read-tag {
    --el-tag-border-color: #2d3a2d;
    --el-tag-bg-color: #1a2d1a;
    --el-tag-text-color: #22c55e;
  }
}

/* 动画效果 */
.level4-message-item {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 状态转换动画 */
.level4-message-item {
  transition: all 0.3s ease;
}

/* 悬浮效果 */
.level4-message-item:hover .level4-icon {
  transform: scale(1.05);
  transition: transform 0.2s ease;
}

/* Focus状态 */
.level4-message-item:focus {
  outline: 2px solid #22c55e;
  outline-offset: 2px;
}
</style>