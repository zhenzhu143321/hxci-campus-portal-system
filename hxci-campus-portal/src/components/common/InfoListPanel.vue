<template>
  <div class="info-card" :class="{ 'has-divider': showDivider }">
    <div class="info-header">
      <h4 class="info-title">{{ title }}</h4>

      <div class="header-right">
        <slot name="header-extra" />
        <template v-if="isFallback && !loading">
          <el-tag type="warning" size="small" effect="plain">
            {{ fallbackMessage || '已降级为缓存数据' }}
          </el-tag>
          <el-button
            v-if="retryable"
            type="text"
            size="small"
            @click="emit('retry')"
          >
            重试
          </el-button>
        </template>
      </div>
    </div>

    <div
      class="info-list"
      v-loading="loading"
      :style="computedListStyle"
      role="list"
    >
      <template v-if="!loading && items.length === 0">
        <slot name="empty">
          <el-empty :image-size="80">
            <template #description v-if="showEmptyHint">
              <p class="empty-main">{{ emptyDescription }}</p>
              <p class="empty-sub">添加数据后会显示在这里</p>
            </template>
          </el-empty>
        </slot>
      </template>

      <ul v-else class="items" role="presentation">
        <li
          v-for="(item, index) in items"
          :key="getKey(item, index)"
          class="item"
          role="button"
          tabindex="0"
          @click="onItemClick(item)"
          @keyup.enter="onItemClick(item)"
          @keyup.space.prevent="onItemClick(item)"
        >
          <slot name="item" :item="item" :index="index">
            <!-- 默认渲染（仅兜底，可被 #item 完全替换） -->
            <div class="item-default">
              <div class="item-title">{{ tryGet(item, ['title']) }}</div>
              <div v-if="tryGet(item, ['publishTime', 'time'])" class="item-sub">
                {{ tryGet(item, ['publishTime', 'time']) }}
              </div>
            </div>
          </slot>
        </li>
      </ul>
    </div>

    <slot name="footer" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { InfoListPanelProps } from '@/types/list'

const props = withDefaults(defineProps<InfoListPanelProps>(), {
  title: '信息列表',
  loading: false,
  isFallback: false,
  retryable: false,
  fallbackMessage: '',
  emptyDescription: '暂无数据',
  showEmptyHint: true,
  maxHeight: 400,
  minHeight: undefined,
  heightMode: 'max',
  showDivider: false,
})

const emit = defineEmits<{
  (e: 'retry'): void
  (e: 'item-click', item: unknown): void
}>()

/**
 * 转换尺寸值为CSS值
 */
const toCssSize = (size: number | string | undefined): string | undefined => {
  if (size === undefined) return undefined
  return typeof size === 'number' ? `${size}px` : size
}

/**
 * 根据heightMode和高度属性计算最终样式
 */
const computedListStyle = computed(() => {
  const style: Record<string, string> = {}

  switch (props.heightMode) {
    case 'fixed':
      // 固定高度模式：使用maxHeight作为固定高度
      if (props.maxHeight) {
        style.height = toCssSize(props.maxHeight)!
      }
      break

    case 'auto':
      // 自适应模式：仅设置最小高度（如果有）
      if (props.minHeight) {
        style.minHeight = toCssSize(props.minHeight)!
      }
      break

    case 'max':
    default:
      // 最大高度模式（默认）：设置最大高度限制
      if (props.maxHeight) {
        style.maxHeight = toCssSize(props.maxHeight)!
      }
      if (props.minHeight) {
        style.minHeight = toCssSize(props.minHeight)!
      }
      break
  }

  return style
})

const getKey = (item: unknown, index: number) => {
  // 如果父层没提供稳定 key 字段，使用下标兜底
  // 建议：父层在 item 上提供 id 或 uuid 字段
  const key = tryGet(item, ['id', 'key'])
  return key ?? index
}

const onItemClick = (item: unknown) => {
  emit('item-click', item)
}

/**
 * 安全读取常见字段（仅用于默认渲染兜底）
 */
function tryGet(obj: unknown, keys: Array<string>): any {
  if (!obj || typeof obj !== 'object') return undefined
  const record = obj as Record<string, unknown>
  for (const k of keys) {
    if (k in record) return record[k]
  }
  return undefined
}
</script>

<style scoped lang="scss">
// 导入CSS变量
@import '@/styles/variables.scss';

.info-card {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-base);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
  box-shadow: var(--shadow-card);
  transition: all var(--duration-base) var(--ease-in-out);

  &:hover {
    box-shadow: var(--shadow-card-hover);
  }

  &.has-divider {
    padding-top: var(--spacing-xs);
    border-top: 1px solid var(--color-border-lighter);
  }
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}

.info-title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);

  :deep(.el-tag) {
    font-size: 12px;
  }

  .el-button {
    padding: 0 4px;
    height: 20px;
    line-height: 20px;
  }
}

.info-list {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  overflow-y: auto;

  /* 平滑且低侵入的滚动条样式（隔离在组件内） */
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: var(--color-bg-page);
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--color-border-base);
    border-radius: 3px;
    &:hover {
      background: var(--color-text-placeholder);
    }
  }
}

.items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-auto-rows: min-content;
  gap: var(--spacing-sm);
}

.item {
  border-bottom: 1px solid var(--color-border-lighter);
  padding: var(--spacing-sm) var(--spacing-md);
  cursor: pointer;
  transition: background var(--duration-base) var(--ease-in-out);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--color-bg-light);
  }
}

.item-default {
  .item-title {
    font-size: var(--font-size-base);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
    margin-bottom: var(--spacing-xs);
  }
  .item-sub {
    font-size: var(--font-size-xs);
    color: var(--color-text-secondary);
  }
}

.empty-main {
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}
.empty-sub {
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}
</style>