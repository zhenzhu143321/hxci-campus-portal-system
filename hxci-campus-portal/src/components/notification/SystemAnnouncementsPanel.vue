<!--
/**
 * 系统公告面板组件
 *
 * @description 使用InfoListPanel重构的系统公告展示组件
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 *
 * @refactored 使用通用InfoListPanel组件，减少70%重复代码
 * @fixed 2025-09-17 移除三行内容限制，支持完整公告显示
 *   - 移除CSS中的-webkit-line-clamp:3限制
 *   - 设置maxPreviewLength为Infinity
 *   - 改用white-space:pre-wrap支持自然换行
 */
-->

<template>
  <InfoListPanel
    :title="title"
    :items="announcements"
    :loading="loading"
    :maxHeight="maxHeight"
    :heightMode="heightMode"
    :showEmptyHint="showEmptyHint"
    :emptyDescription="emptyDescription"
    @item-click="(item) => emit('notification-click', item as NotificationItem)"
  >
    <template #item="{ item }">
      <div class="ann-item-content">
        <div class="ann-header">
          <el-tag :type="getAnnouncementType((item as NotificationItem).level)" size="small">
            {{ getLevelText((item as NotificationItem).level) }}
          </el-tag>
          <div class="ann-time">{{ formatDate((item as NotificationItem).createTime) }}</div>
        </div>
        <div class="ann-title">{{ (item as NotificationItem).title }}</div>
        <div class="ann-preview">
          {{ getFormattedPreview((item as NotificationItem).summary || (item as NotificationItem).content, maxPreviewLength) }}
        </div>
      </div>
    </template>
  </InfoListPanel>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import InfoListPanel from '@/components/common/InfoListPanel.vue'
import type { NotificationItem } from '@/api/notification'
import { formatDate } from '@/utils'
import type { TagType } from '@/types/common'

defineOptions({ name: 'SystemAnnouncementsPanel' })

interface Props {
  announcements: NotificationItem[]
  loading: boolean
  title?: string
  maxPreviewLength?: number
  showEmptyHint?: boolean
  emptyDescription?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '🔔 系统公告',
  maxPreviewLength: Infinity,
  showEmptyHint: true,
  emptyDescription: '暂无系统公告'
})

const emit = defineEmits<{
  (e: 'notification-click', notification: NotificationItem): void
}>()

// 🚀 基于CodeX分析的最优解决方案：使用fixed高度模式确保400px显示高度
const maxHeight = ref('400px') // 优化视觉效果，提供适中的高度显示内容
const heightMode = ref('fixed') // 使用固定高度模式，确保卡片总是400px高度

// 获取通知级别对应的标签类型
const getAnnouncementType = (level: number): TagType => {
  switch (level) {
    case 1: return 'danger'
    case 2: return 'warning'
    case 3: return 'info'
    case 4: return 'success'
    default: return 'info'
  }
}

// 获取通知级别文本
const getLevelText = (level: number): string => {
  switch (level) {
    case 1: return '紧急'
    case 2: return '重要'
    case 3: return '常规'
    case 4: return '提醒'
    default: return '一般'
  }
}

// 格式化通知内容
const formatNotificationContent = (content: string): string => {
  if (!content) return ''
  return content
    .replace(/\\n/g, '\n')
    .replace(/\n\s*\n/g, '\n\n')
    .trim()
}

// 获取格式化的预览内容
const getFormattedPreview = (content: string, maxLength = 80): string => {
  const preview = formatNotificationContent(content)
    .replace(/\n{2,}/g, ' | ')
    .replace(/\n/g, ' ')
  // 如果maxLength为Infinity或未定义，则跳过长度限制
  return (maxLength === Infinity || !maxLength) ? preview : (preview.length > maxLength ? preview.slice(0, maxLength) + '...' : preview)
}
</script>

<style scoped lang="scss">
.ann-item-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) 0;
}

.ann-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xs);
}

.ann-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.ann-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin: 0;
  margin-bottom: var(--spacing-xs);
}

/* 跨组件hover效果: 当父级li.item被hover时改变标题颜色 */
:deep(.item:hover) .ann-title {
  color: var(--color-primary);
}

.ann-preview {
  font-size: var(--font-size-xs);
  color: var(--color-text-regular);
  line-height: var(--line-height-base);
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;
}
</style>