/**
 * 通用类型定义
 * 
 * @description 项目通用的TypeScript类型定义，枚举和工具类型
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

/** 标签类型统一 */
export type TagType = 'success' | 'warning' | 'info' | 'danger'

/** 通知级别枚举 */
export enum NotificationLevel {
  Emergency = 1,
  Important = 2,
  Normal = 3,
  Reminder = 4,
}

/** 通知范围 */
export type NotificationScope = 'SCHOOL_WIDE' | 'DEPARTMENT' | 'GRADE' | 'CLASS'

/** 值或格式化器工具类型 */
export type ValueOrFn<T, I = unknown> = T | ((input: I) => T)

/** 测试结果接口 */
export interface TestResults {
  health?: { ok: boolean; message?: string }
  verify?: { ok: boolean; user?: string }
  notification?: { ok: boolean; count?: number }
}

/** 分页参数 */
export interface PaginatedParams {
  page: number
  pageSize: number
  [key: string]: any
}

/** API分页响应 */
export interface ApiPage<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

/** 基础数据项 */
export interface FeedItemBase {
  id: string | number
  title: string
  publishedAt?: string
  url?: string
}