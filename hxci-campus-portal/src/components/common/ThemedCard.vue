<!--
/**
 * 主题化卡片组件
 *
 * @description 使用CSS变量系统的示例卡片组件，展示如何应用统一的设计系统
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */
-->

<template>
  <div 
    class="themed-card"
    :class="[
      `themed-card--${variant}`,
      `themed-card--${size}`,
      {
        'themed-card--hoverable': hoverable,
        'themed-card--bordered': bordered,
        'themed-card--shadow': shadow
      }
    ]"
  >
    <!-- 卡片头部 -->
    <div v-if="$slots.header || title" class="themed-card__header">
      <slot name="header">
        <h3 class="themed-card__title">{{ title }}</h3>
      </slot>
      <div v-if="$slots.extra" class="themed-card__extra">
        <slot name="extra"></slot>
      </div>
    </div>
    
    <!-- 卡片内容 -->
    <div class="themed-card__body" :style="bodyStyle">
      <slot></slot>
    </div>
    
    <!-- 卡片底部 -->
    <div v-if="$slots.footer" class="themed-card__footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, CSSProperties } from 'vue'

defineOptions({ name: 'ThemedCard' })

// 卡片变体类型
type CardVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info'
// 卡片尺寸类型
type CardSize = 'small' | 'medium' | 'large'

interface Props {
  title?: string
  variant?: CardVariant
  size?: CardSize
  hoverable?: boolean
  bordered?: boolean
  shadow?: boolean
  bodyStyle?: CSSProperties
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'medium',
  hoverable: false,
  bordered: true,
  shadow: true
})

// 计算body样式
const bodyStyle = computed(() => ({
  ...props.bodyStyle
}))
</script>

<style scoped lang="scss">
// 导入CSS变量
@import '@/styles/variables.scss';

.themed-card {
  background: var(--color-bg-base);
  border-radius: var(--radius-lg);
  transition: all var(--duration-base) var(--ease-in-out);
  position: relative;
  overflow: hidden;
  
  // 尺寸变体
  &--small {
    .themed-card__header {
      padding: var(--spacing-sm) var(--spacing-md);
    }
    .themed-card__body {
      padding: var(--spacing-md);
    }
    .themed-card__footer {
      padding: var(--spacing-sm) var(--spacing-md);
    }
    .themed-card__title {
      font-size: var(--font-size-sm);
    }
  }
  
  &--medium {
    .themed-card__header {
      padding: var(--spacing-md) var(--spacing-lg);
    }
    .themed-card__body {
      padding: var(--spacing-lg);
    }
    .themed-card__footer {
      padding: var(--spacing-md) var(--spacing-lg);
    }
    .themed-card__title {
      font-size: var(--font-size-base);
    }
  }
  
  &--large {
    .themed-card__header {
      padding: var(--spacing-lg) var(--spacing-xl);
    }
    .themed-card__body {
      padding: var(--spacing-xl);
    }
    .themed-card__footer {
      padding: var(--spacing-lg) var(--spacing-xl);
    }
    .themed-card__title {
      font-size: var(--font-size-md);
    }
  }
  
  // 颜色变体
  &--default {
    border: 1px solid var(--color-border-light);
  }
  
  &--primary {
    border: 1px solid var(--color-primary-light-7);
    background: var(--color-primary-light-9);
    
    .themed-card__title {
      color: var(--color-primary);
    }
  }
  
  &--success {
    border: 1px solid var(--color-success-light);
    background: var(--color-success-lighter);
    
    .themed-card__title {
      color: var(--color-success-dark);
    }
  }
  
  &--warning {
    border: 1px solid var(--color-warning-light);
    background: var(--color-warning-lighter);
    
    .themed-card__title {
      color: var(--color-warning-dark);
    }
  }
  
  &--danger {
    border: 1px solid var(--color-danger-light);
    background: var(--color-danger-lighter);
    
    .themed-card__title {
      color: var(--color-danger-dark);
    }
  }
  
  &--info {
    border: 1px solid var(--color-info-light);
    background: var(--color-info-lighter);
    
    .themed-card__title {
      color: var(--color-info-dark);
    }
  }
  
  // 交互状态
  &--hoverable {
    cursor: pointer;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-card-hover);
    }
    
    &:active {
      transform: translateY(0);
      box-shadow: var(--shadow-card-active);
    }
  }
  
  // 边框状态
  &--bordered {
    border-width: 1px;
    border-style: solid;
  }
  
  // 阴影状态
  &--shadow {
    box-shadow: var(--shadow-card);
  }
  
  // 卡片头部
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid var(--color-border-lighter);
    background: var(--color-bg-light);
  }
  
  // 卡片标题
  &__title {
    margin: 0;
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
    line-height: var(--line-height-tight);
  }
  
  // 卡片额外内容
  &__extra {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
  }
  
  // 卡片主体
  &__body {
    color: var(--color-text-regular);
    font-size: var(--font-size-base);
    line-height: var(--line-height-normal);
  }
  
  // 卡片底部
  &__footer {
    border-top: 1px solid var(--color-border-lighter);
    background: var(--color-bg-light);
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: var(--spacing-sm);
  }
}

// 暗黑模式适配
@media (prefers-color-scheme: dark) {
  .themed-card {
    &--primary {
      background: rgba(64, 158, 255, 0.1);
      border-color: rgba(64, 158, 255, 0.3);
    }
    
    &--success {
      background: rgba(103, 194, 58, 0.1);
      border-color: rgba(103, 194, 58, 0.3);
    }
    
    &--warning {
      background: rgba(230, 162, 60, 0.1);
      border-color: rgba(230, 162, 60, 0.3);
    }
    
    &--danger {
      background: rgba(245, 108, 108, 0.1);
      border-color: rgba(245, 108, 108, 0.3);
    }
    
    &--info {
      background: rgba(144, 147, 153, 0.1);
      border-color: rgba(144, 147, 153, 0.3);
    }
  }
}
</style>