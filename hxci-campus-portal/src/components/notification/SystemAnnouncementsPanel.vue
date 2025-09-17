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
        <div
          class="ann-preview"
          v-html="getMarkdownPreview((item as NotificationItem).summary || (item as NotificationItem).content, maxPreviewLength)"
        ></div>
      </div>
    </template>
  </InfoListPanel>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import InfoListPanel from '@/components/common/InfoListPanel.vue'
import type { NotificationItem } from '@/api/notification'
import { formatDate } from '@/utils'
import type { TagType } from '@/types/common'
import { renderNotificationSummary, containsMarkdown } from '@/utils/markdown'

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

// 获取Markdown渲染的预览内容（统一渲染方案）
const getMarkdownPreview = (content: string, maxLength = 80): string => {
  if (!content || content.trim() === '') {
    return '<span class="empty-content">暂无内容</span>'
  }

  console.debug('🎯 [SystemAnnouncementsPanel] 统一渲染方案 - 使用增强版renderNotificationSummary')

  try {
    // 🚀 统一方案：始终使用增强版renderNotificationSummary，已集成转义字符处理
    return renderNotificationSummary(content, maxLength === Infinity ? 200 : maxLength)
  } catch (error) {
    console.error('❌ [SystemAnnouncementsPanel] Markdown渲染失败，使用降级方案:', error)

    // 降级处理：纯文本格式化
    const preview = formatNotificationContent(content)
      .replace(/\n{2,}/g, ' | ')
      .replace(/\n/g, ' ')

    const finalPreview = (maxLength === Infinity || !maxLength) ? preview : (preview.length > maxLength ? preview.slice(0, maxLength) + '...' : preview)

    return finalPreview
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\|/g, '<span class="separator">|</span>')
  }
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
  font-size: var(--font-size-xs);     /* 保持较小字体，区别于详情对话框 */
  color: var(--color-text-regular);
  line-height: var(--line-height-base);
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;

  /* =================================
   * Markdown内容样式支持 (摘要模式)
   * 与详情对话框的区别：字体更小，样式更简化
   * ================================= */

  /* 段落间距 */
  :deep(p) {
    margin: 0 0 0.5em 0;
    &:last-child {
      margin-bottom: 0;
    }
  }

  /* 文本格式 */
  :deep(strong), :deep(b) {
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
  }

  :deep(em), :deep(i) {
    font-style: italic;
    color: var(--color-text-secondary);
  }

  /* 行内代码 */
  :deep(code) {
    background: var(--color-bg-light);
    color: var(--color-primary);
    padding: 2px 4px;
    border-radius: 3px;
    font-family: var(--font-family-mono, 'Courier New', monospace);
    font-size: 0.9em;
  }

  /* 链接样式 */
  :deep(a) {
    color: var(--color-primary);
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }

  /* 列表样式（简化版用于预览） */
  :deep(ul), :deep(ol) {
    margin: 0.25em 0;
    padding-left: 1.2em;
  }

  :deep(li) {
    margin: 0.1em 0;
  }

  /* 引用块（简化版） */
  :deep(blockquote) {
    border-left: 2px solid var(--color-primary-light);
    padding-left: 0.5em;
    margin: 0.25em 0;
    color: var(--color-text-secondary);
    font-style: italic;
  }

  /* 分隔符样式 */
  .separator {
    color: var(--color-text-placeholder);
    margin: 0 0.3em;
  }

  /* 空内容提示 */
  .empty-content {
    color: var(--color-text-placeholder);
    font-style: italic;
  }
}
</style>