<template>
  <div class="notification-archive-section">
    <!-- 归档区域标题 -->
    <div class="archive-header">
      <h4 class="archive-title">
        <el-icon><FolderOpened /></el-icon>
        📋 已读归档
      </h4>
      <div class="archive-stats">
        <el-tag type="info" size="small" effect="plain">
          {{ archivedNotifications.length }}条已归档
        </el-tag>
        <el-button 
          v-if="archivedNotifications.length > 0"
          type="text" 
          size="small"
          @click="handleClearAll"
          class="clear-all-btn"
        >
          清空归档
        </el-button>
      </div>
    </div>
    
    <!-- 归档列表 -->
    <div v-if="archivedNotifications.length > 0" class="archive-list">
      <div 
        v-for="notification in displayedNotifications" 
        :key="notification.id"
        class="archived-notification-item"
        :class="{
          'level-1': notification.level === 1,
          'level-2': notification.level === 2,
          'level-3': notification.level === 3,
          'level-4': notification.level === 4
        }"
        @click="handleNotificationClick(notification)"
      >
        <!-- 通知状态指示器 -->
        <NotificationArchiveIndicator
          :is-read="true"
          :archive-time="notification.readTime || notification.createTime"
          :theme="getIndicatorTheme(notification.level)"
          :show-hover-effect="false"
          @click="handleNotificationClick(notification)"
        />
        
        <!-- 通知内容 -->
        <div class="archived-notification-content">
          <div class="notification-header">
            <el-tag 
              :type="getLevelTagType(notification.level)" 
              size="small" 
              effect="plain"
              class="level-tag"
            >
              {{ getLevelText(notification.level) }}
            </el-tag>
            <div class="notification-time">
              {{ formatNotificationTime(notification.createTime) }}
            </div>
          </div>
          
          <div class="notification-title">{{ notification.title }}</div>
          
          <div v-if="notification.summary" class="notification-summary">
            {{ notification.summary }}
          </div>
          <div v-else class="notification-preview">
            {{ getContentPreview(notification.content) }}
          </div>
          
          <div class="notification-meta">
            <span class="publisher">{{ notification.publisherName }}</span>
            <span class="scope">{{ getScopeText(notification.scope) }}</span>
          </div>
        </div>
        
        <!-- 归档操作 -->
        <div class="archived-actions">
          <el-button 
            type="text" 
            size="small" 
            @click.stop="handleRestoreFromArchive(notification.id)"
            class="restore-btn"
          >
            <el-icon><RefreshRight /></el-icon>
            撤销已读
          </el-button>
          <el-button 
            type="text" 
            size="small"
            @click.stop="handlePermanentDelete(notification.id)"
            class="delete-btn"
          >
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="archive-empty-state">
      <el-empty 
        description="暂无已读归档" 
        :image-size="80"
        class="empty-content"
      >
        <template #image>
          <el-icon class="empty-icon"><FolderOpened /></el-icon>
        </template>
        <template #description>
          <span class="empty-text">暂无已读归档通知</span>
          <p class="empty-hint">标记通知为已读后，将在此处显示</p>
        </template>
      </el-empty>
    </div>
    
    <!-- 查看更多 -->
    <div v-if="hasMoreArchived" class="archive-more">
      <el-button 
        type="text" 
        size="small" 
        @click="handleShowMore"
        class="show-more-btn"
      >
        查看更多归档 ({{ totalArchivedCount }}条)
        <el-icon><ArrowDown /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  FolderOpened, RefreshRight, Delete, ArrowDown 
} from '@element-plus/icons-vue'
import type { NotificationItem } from '@/api/notification'
import NotificationArchiveIndicator from './NotificationArchiveIndicator.vue'
import dayjs from 'dayjs'

interface Props {
  /** 已归档的通知列表 */
  archivedNotifications: NotificationItem[]
  /** 最大显示数量 */
  maxDisplayCount?: number
  /** 是否显示操作按钮 */
  showActions?: boolean
  /** 是否启用悬停效果 */
  enableHoverEffects?: boolean
}

interface Emits {
  /** 通知点击事件 */
  (e: 'notification-click', notification: NotificationItem): void
  /** 从归档中恢复 */
  (e: 'restore-from-archive', notificationId: number): void
  /** 永久删除 */
  (e: 'permanent-delete', notificationId: number): void
  /** 清空所有归档 */
  (e: 'clear-all-archive'): void
  /** 显示更多归档 */
  (e: 'show-more'): void
}

const props = withDefaults(defineProps<Props>(), {
  maxDisplayCount: 5,
  showActions: true,
  enableHoverEffects: true
})

const emit = defineEmits<Emits>()

// 显示的通知列表
const displayedNotifications = computed(() => {
  return props.archivedNotifications.slice(0, props.maxDisplayCount)
})

// 总归档数量
const totalArchivedCount = computed(() => props.archivedNotifications.length)

// 是否有更多归档
const hasMoreArchived = computed(() => {
  return props.archivedNotifications.length > props.maxDisplayCount
})

// 获取级别文本
const getLevelText = (level: number): string => {
  switch (level) {
    case 1: return '紧急'
    case 2: return '重要'
    case 3: return '常规'
    case 4: return '提醒'
    default: return '未知'
  }
}

// 获取级别标签类型
const getLevelTagType = (level: number): string => {
  switch (level) {
    case 1: return 'danger'
    case 2: return 'warning'
    case 3: return 'info'
    case 4: return 'success'
    default: return 'info'
  }
}

// 获取指示器主题
const getIndicatorTheme = (level: number): 'success' | 'info' | 'warning' => {
  switch (level) {
    case 1: return 'warning'
    case 2: return 'warning'
    case 3: return 'info'
    case 4: return 'success'
    default: return 'info'
  }
}

// 获取范围文本
const getScopeText = (scope: string): string => {
  switch (scope) {
    case 'SCHOOL_WIDE': return '全校'
    case 'DEPARTMENT': return '部门'
    case 'GRADE': return '年级'
    case 'CLASS': return '班级'
    default: return scope
  }
}

// 格式化通知时间
const formatNotificationTime = (dateStr: string): string => {
  if (!dateStr) return ''
  try {
    return dayjs(dateStr).format('MM-DD HH:mm')
  } catch (error) {
    return dateStr
  }
}

// 获取内容预览
const getContentPreview = (content: string, maxLength: number = 60): string => {
  if (!content) return ''
  const cleanContent = content.replace(/\\n/g, ' ').replace(/\n/g, ' ')
  return cleanContent.length > maxLength ? 
    cleanContent.substring(0, maxLength) + '...' : 
    cleanContent
}

// 处理通知点击
const handleNotificationClick = (notification: NotificationItem) => {
  emit('notification-click', notification)
}

// 处理从归档中恢复
const handleRestoreFromArchive = async (notificationId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要撤销该通知的已读状态吗？',
      '撤销确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    emit('restore-from-archive', notificationId)
    ElMessage.success('已撤销已读状态')
  } catch (error) {
    // 用户取消操作
  }
}

// 处理永久删除
const handlePermanentDelete = async (notificationId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要永久删除该通知吗？此操作不可恢复。',
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        buttonSize: 'small'
      }
    )
    
    emit('permanent-delete', notificationId)
    ElMessage.success('已永久删除通知')
  } catch (error) {
    // 用户取消操作
  }
}

// 处理清空所有归档
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要清空所有 ${totalArchivedCount.value} 条归档通知吗？此操作不可恢复。`,
      '清空确认',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    emit('clear-all-archive')
    ElMessage.success('已清空所有归档')
  } catch (error) {
    // 用户取消操作
  }
}

// 处理显示更多
const handleShowMore = () => {
  emit('show-more')
}
</script>

<style scoped>
.notification-archive-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  border: 1px solid rgba(59, 130, 246, 0.1);
  transition: all 0.3s ease;
}

.notification-archive-section:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.1);
}

/* 归档区域头部 - 🔧 修复Bug2: 清空归档按钮超出卡片问题 */
.archive-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px 10px 18px;  /* 缩小内边距 */
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  min-height: 50px; /* 缩小最小高度，增加空间利用率 */
  flex-wrap: nowrap; /* 防止换行 */
  gap: 8px; /* 增加间距控制 */
}

.archive-title {
  margin: 0;
  font-size: 15px;  /* 轻微减小字体 */
  font-weight: 600;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 6px;  /* 缩小间距 */
  white-space: nowrap;  /* 防止换行 */
  min-width: 100px;     /* 减小最小宽度 */
  flex-shrink: 0;       /* 防止压缩 */
}

.archive-stats {
  display: flex;
  align-items: center;
  gap: 8px;  /* 缩小间距 */
  flex-shrink: 0;  /* 防止压缩 */
  max-width: 60%; /* 限制最大宽度，防止超出 */
}

.clear-all-btn {
  color: #f56565;
  font-size: 11px;  /* 减小字体 */
  padding: 2px 6px;  /* 缩小内边距 */
  white-space: nowrap;  /* 防止换行 */
  min-width: auto;      /* 去除最小宽度限制 */
}

.clear-all-btn:hover {
  color: #e53e3e;
  background-color: rgba(245, 101, 101, 0.1);
}

/* 归档列表 */
.archive-list {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.archived-notification-item {
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
}

.archived-notification-item:hover {
  background: #f5f5f5;
  border-color: #d1d5db;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 不同级别的归档项样式 */
.archived-notification-item.level-1 {
  border-left: 4px solid #f56565;
}

.archived-notification-item.level-2 {
  border-left: 4px solid #ed8936;
}

.archived-notification-item.level-3 {
  border-left: 4px solid #4299e1;
}

.archived-notification-item.level-4 {
  border-left: 4px solid #48bb78;
}

/* 通知内容 */
.archived-notification-content {
  padding: 12px 16px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.level-tag {
  font-size: 11px;
}

.notification-time {
  font-size: 11px;
  color: #999;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 6px;
  line-height: 1.4;
}

.notification-summary,
.notification-preview {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 4px;
  border-left: 3px solid #e2e8f0;
}

.notification-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #999;
}

.publisher {
  font-weight: 500;
  color: #4a5568;
}

/* 归档操作 */
.archived-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 16px 12px 16px;
  background: rgba(255, 255, 255, 0.5);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.archived-notification-item:hover .archived-actions {
  opacity: 1;
}

.restore-btn {
  color: #4299e1;
  font-size: 12px;
}

.restore-btn:hover {
  color: #3182ce;
  background-color: rgba(66, 153, 225, 0.1);
}

.delete-btn {
  color: #f56565;
  font-size: 12px;
}

.delete-btn:hover {
  color: #e53e3e;
  background-color: rgba(245, 101, 101, 0.1);
}

/* 空状态 */
.archive-empty-state {
  padding: 40px 20px;
  text-align: center;
}

.empty-content {
  --el-empty-padding: 20px 0;
}

.empty-icon {
  font-size: 64px;
  color: #cbd5e0;
}

.empty-text {
  color: #718096;
  font-size: 16px;
  font-weight: 500;
}

.empty-hint {
  color: #a0aec0;
  font-size: 13px;
  margin-top: 8px;
  margin-bottom: 0;
}

/* 查看更多 */
.archive-more {
  text-align: center;
  padding: 12px 20px 16px 20px;
  border-top: 1px solid rgba(59, 130, 246, 0.1);
}

.show-more-btn {
  color: #4299e1;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0 auto;
}

.show-more-btn:hover {
  color: #3182ce;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .archive-header {
    padding: 12px 16px 8px 16px;
    min-height: 56px; /* 确保在小屏幕下也有足够高度 */
    /* 保持横向布局，不换行 */
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }
  
  .archive-title {
    font-size: 14px; /* 稍微减小字体 */
    min-width: 100px; /* 调整最小宽度 */
    white-space: nowrap; /* 强制不换行 */
  }
  
  .archive-stats {
    flex-shrink: 0; /* 防止统计信息被压缩 */
    align-self: center;
  }
  
  .archive-list {
    padding: 12px 16px;
    gap: 10px;
  }
  
  .archived-notification-content {
    padding: 10px 12px;
  }
  
  .notification-title {
    font-size: 13px;
  }
  
  .notification-summary,
  .notification-preview {
    font-size: 11px;
  }
  
  .archived-actions {
    padding: 6px 12px 10px 12px;
  }
}
</style>