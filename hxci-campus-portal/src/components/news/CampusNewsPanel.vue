<!--
/**
 * 校园新闻面板组件
 *
 * @description 使用InfoListPanel重构的校园新闻展示组件
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 * 
 * @refactored 使用通用InfoListPanel组件，减少70%重复代码
 */
-->

<template>
  <InfoListPanel
    :title="title"
    :items="news"
    :loading="loading"
    :isFallback="isFallback"
    :fallbackMessage="fallbackMessage"
    :retryable="retryable"
    :maxHeight="400"
    :showEmptyHint="showEmptyHint"
    :emptyDescription="emptyDescription"
    @retry="emit('retry')"
    @item-click="(item) => emit('news-click', item as NewsItem)"
  >
    <template #item="{ item }">
      <div class="news-item-row">
        <img
          :src="(item as NewsItem).image || (item as NewsItem).coverUrl || placeholder"
          :alt="(item as NewsItem).title"
          class="news-image"
          width="60"
          height="40"
          loading="lazy"
          decoding="async"
          @error="onImgError"
        />
        <div class="news-info">
          <div class="news-title">{{ (item as NewsItem).title }}</div>
          <div class="news-time">{{ getDisplayTime((item as NewsItem).publishTime) }}</div>
        </div>
      </div>
    </template>
  </InfoListPanel>
</template>

<script setup lang="ts">
import InfoListPanel from '@/components/common/InfoListPanel.vue'
import type { NewsItem } from '@/types/news'
import { formatDate } from '@/utils'

defineOptions({ name: 'CampusNewsPanel' })

interface Props {
  news: NewsItem[]
  loading: boolean
  isFallback?: boolean
  fallbackMessage?: string | null
  retryable?: boolean
  title?: string
  showEmptyHint?: boolean
  emptyDescription?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '📢 校园新闻',
  showEmptyHint: true,
  emptyDescription: '暂无校园新闻'
})

const emit = defineEmits<{
  (e: 'news-click', news: NewsItem): void
  (e: 'retry'): void
}>()

// 默认占位图片
const placeholder =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA2MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yNSAyMEMyNSAxNy4yMzg2IDI3LjIzODYgMTUgMzAgMTVDMzIuNzYxNCAxNSAzNSAxNy4yMzg2IDM1IDIwQzM1IDIyLjc2MTQgMzIuNzYxNCAyNSAzMCAyNUMyNy4yMzg2IDI1IDI1IDIyLjc2MTQgMjUgMjBaIiBmaWxsPSIjQ0NDQ0NDIi8+CjxwYXRoIGQ9Ik0yMCAyOEwyNS41IDIyLjVMMzIuNSAyOS41TDQwIDIyTDQwIDMySDIwVjI4WiIgZmlsbD0iI0NDQ0NDQyIvPgo8L3N2Zz4K'

const onImgError = (e: Event) => {
  const el = e.target as HTMLImageElement
  el.onerror = null
  el.src = placeholder
}

const getDisplayTime = (publishTime?: string) => {
  if (!publishTime) return ''
  if (/^\d{4}-\d{2}-\d{2}$/.test(publishTime)) return publishTime
  try {
    return formatDate(publishTime)
  } catch {
    return publishTime
  }
}
</script>

<style scoped lang="scss">
// 新闻行容器
.news-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

// 新闻图片
.news-image {
  width: 60px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  background: #f5f7fa;
  flex-shrink: 0;
  border: 1px solid #e4e7ed;
}

// 新闻信息
.news-info {
  flex: 1;
  min-width: 0;
}

// 新闻标题
.news-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.3s;
  
  .info-card .item:hover & {
    color: #409eff;
  }
}

// 新闻时间
.news-time {
  font-size: 12px;
  color: #909399;
}
</style>