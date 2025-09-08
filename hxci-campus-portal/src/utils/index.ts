/**
 * 统一工具函数导出
 * 这里导出所有工具函数，提供统一的导入入口
 * 使用方式: import { formatDate, truncate, formatNumber } from '@/utils'
 */

// 导出日期时间工具函数
export * from './date'

// 导出字符串处理工具函数
export * from './string'

// 导出数字处理工具函数
export * from './number'

// 导出现有的工具函数（保持向后兼容）
export { performanceAnalyzer } from './performanceAnalyzer'

// 如果request.ts导出了具名导出，也可以导出
// export * from './request'

/**
 * 工具函数类型定义
 */
export type DateInput = Date | number | string
export type StringProcessor = (text: string) => string
export type NumberProcessor = (num: number) => number | string

/**
 * 常用常量定义
 */
export const DATE_FORMATS = {
  FULL_DATETIME: 'YYYY-MM-DD HH:mm:ss',
  SHORT_DATETIME: 'MM-DD HH:mm',
  DATE_ONLY: 'YYYY-MM-DD',
  TIME_ONLY: 'HH:mm:ss',
  CHINESE_DATE: 'YYYY年MM月DD日',
  CHINESE_DATETIME: 'YYYY年MM月DD日 HH:mm'
} as const

export const CURRENCY_SYMBOLS = {
  CNY: '¥',
  USD: '$',
  EUR: '€',
  JPY: '¥',
  GBP: '£'
} as const