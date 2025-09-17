/**
 * 错误处理工具函数
 *
 * @description 提供统一的错误类型判断和处理工具
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

/**
 * 判断是否为取消请求错误
 */
export const isAbortOrCanceled = (e: unknown): boolean => {
  if (typeof e === 'object' && e !== null) {
    // Axios取消错误
    if ('code' in e && (e as any).code === 'ERR_CANCELED') return true
    // DOM AbortController错误
    if ('name' in e && (e as any).name === 'AbortError') return true
  }
  return false
}

/**
 * 将错误转换为错误消息
 */
export const toErrorMessage = (e: unknown, fallback = '请求失败'): string => {
  if (e instanceof Error) return e.message
  if (typeof e === 'string') return e
  return fallback
}

/**
 * 判断是否为对象
 */
export const isObject = (v: unknown): v is Record<string, unknown> => {
  return typeof v === 'object' && v !== null && !Array.isArray(v)
}

/**
 * 安全的字符串转换
 */
export const asString = (v: unknown, fallback = ''): string => {
  if (typeof v === 'string') return v
  if (v == null) return fallback
  return String(v)
}

/**
 * 安全的数字转换
 */
export const asNumber = (v: unknown, fallback = 0): number => {
  if (typeof v === 'number' && Number.isFinite(v)) return v
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

/**
 * 安全的布尔转换
 */
export const asBool = (v: unknown, fallback = false): boolean => {
  if (typeof v === 'boolean') return v
  return fallback
}

/**
 * 安全的数组转换
 */
export const asArray = <T>(v: unknown, itemGuard?: (item: unknown) => item is T): T[] => {
  if (!Array.isArray(v)) return []
  if (!itemGuard) return v as T[]
  return v.filter(itemGuard)
}