/**
 * 通用列表组件类型定义
 * 
 * @description InfoListPanel组件的类型定义
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

export interface InfoListPanelProps {
  /** 面板标题 */
  title?: string
  /** 列表数据项 */
  items: unknown[]
  /** 是否加载中 */
  loading?: boolean
  /** 是否降级到缓存数据 */
  isFallback?: boolean
  /** 是否可重试 */
  retryable?: boolean
  /** 降级提示消息 */
  fallbackMessage?: string
  /** 空数据描述 */
  emptyDescription?: string
  /** 是否显示空数据提示 */
  showEmptyHint?: boolean
  /** 最大高度 */
  maxHeight?: number | string
  /** 最小高度 */
  minHeight?: number | string
  /** 高度模式：'auto'自适应(默认) | 'max'最大高度限制 | 'fixed'固定高度 */
  heightMode?: 'auto' | 'max' | 'fixed'
  /** 是否显示顶部分割线 */
  showDivider?: boolean
}