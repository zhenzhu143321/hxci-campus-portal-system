import * as dayjs from 'dayjs'

/**
 * 日期时间格式化工具函数集合
 * 统一项目中所有日期时间处理逻辑，避免重复代码
 */

/**
 * 将日期/时间戳/字符串格式化为完整日期时间格式
 * @param value 需要格式化的日期，可以是 Date 对象、时间戳或ISO格式字符串
 * @param format 目标格式，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期字符串，出错时返回空字符串
 */
export const formatDate = (value: Date | number | string, format: string = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!value) return ''
  
  try {
    return dayjs(value).format(format)
  } catch (error) {
    console.warn('formatDate error:', error, 'value:', value)
    return ''
  }
}

/**
 * 将日期/时间戳/字符串格式化为完整日期时间格式
 * @param value 需要格式化的日期，可以是 Date 对象、时间戳或ISO格式字符串
 * @returns 格式化后的日期时间字符串 (YYYY-MM-DD HH:mm:ss)，出错时返回空字符串
 */
export const formatDateTime = (value: Date | number | string): string => {
  return formatDate(value, 'YYYY-MM-DD HH:mm:ss')
}

/**
 * 将日期/时间戳/字符串格式化为简短时间格式
 * @param value 需要格式化的日期，可以是 Date 对象、时间戳或ISO格式字符串
 * @returns 格式化后的时间字符串 (MM-DD HH:mm)，出错时返回空字符串
 */
export const formatTime = (value: Date | number | string): string => {
  return formatDate(value, 'MM-DD HH:mm')
}

/**
 * 将日期字符串格式化为本地化格式
 * @param dateStr 日期字符串
 * @returns 本地化格式的日期字符串，出错时返回空字符串
 */
export const formatDateLocale = (dateStr: string): string => {
  if (!dateStr) return ''
  
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch (error) {
    console.warn('formatDateLocale error:', error, 'dateStr:', dateStr)
    return ''
  }
}

/**
 * 将日期字符串格式化为本地化日期时间格式
 * @param dateStr 日期字符串
 * @returns 本地化格式的日期时间字符串，出错时返回空字符串
 */
export const formatDateTimeLocale = (dateStr: string): string => {
  if (!dateStr) return ''
  
  try {
    return new Date(dateStr).toLocaleString('zh-CN')
  } catch (error) {
    console.warn('formatDateTimeLocale error:', error, 'dateStr:', dateStr)
    return ''
  }
}

/**
 * 计算相对时间 (如：刚刚、3分钟前、2小时前、1天前)
 * @param value 需要计算的日期，可以是 Date 对象、时间戳或ISO格式字符串
 * @returns 相对时间字符串，出错时返回原字符串
 */
export const timeAgo = (value: Date | number | string): string => {
  if (!value) return ''
  
  try {
    const time = new Date(value)
    const now = new Date()
    const diffMs = now.getTime() - time.getTime()
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
    const diffDays = Math.floor(diffHours / 24)
    
    if (diffDays > 0) {
      return `${diffDays}天前`
    } else if (diffHours > 0) {
      return `${diffHours}小时前`
    } else {
      const diffMinutes = Math.floor(diffMs / (1000 * 60))
      return diffMinutes > 0 ? `${diffMinutes}分钟前` : '刚刚'
    }
  } catch (error) {
    console.warn('timeAgo error:', error, 'value:', value)
    return String(value)
  }
}

/**
 * 检查日期是否为今天
 * @param value 需要检查的日期
 * @returns 是否为今天
 */
export const isToday = (value: Date | number | string): boolean => {
  if (!value) return false
  
  try {
    return dayjs().isSame(dayjs(value), 'day')
  } catch (error) {
    console.warn('isToday error:', error, 'value:', value)
    return false
  }
}

/**
 * 检查日期是否为本周
 * @param value 需要检查的日期
 * @returns 是否为本周
 */
export const isThisWeek = (value: Date | number | string): boolean => {
  if (!value) return false
  
  try {
    return dayjs().isSame(dayjs(value), 'week')
  } catch (error) {
    console.warn('isThisWeek error:', error, 'value:', value)
    return false
  }
}

/**
 * 格式化时间为智能时间显示（今天 HH:mm、昨天 HH:mm、MM-DD HH:mm）
 * @param timeStr 时间字符串
 * @returns 智能格式化后的时间字符串，无效输入返回'未知时间'
 */
export const formatTimeIntelligent = (timeStr: string): string => {
  if (!timeStr) return '未知时间'
  
  try {
    const date = new Date(timeStr)
    // 增强的日期有效性检查
    if (isNaN(date.getTime())) {
      console.warn('formatTimeIntelligent: Invalid date input:', timeStr)
      return '未知时间'
    }
    
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
    
    if (diffDays === 0) {
      return `今天 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    } else if (diffDays === 1) {
      return `昨天 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    } else if (diffDays < 7) {
      return `${diffDays}天前 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    } else {
      return formatTime(timeStr)
    }
  } catch (error) {
    console.warn('formatTimeIntelligent error:', error, 'timeStr:', timeStr)
    return '未知时间'
  }
}

/**
 * 格式化时间字符串为HH:mm格式（仅限时间部分）
 * @param timeStr 时间字符串（如：'09:30'、'14:15'）
 * @returns 格式化后的时间字符串（HH:mm）
 */
export const formatTimeOnly = (timeStr: string): string => {
  if (!timeStr) return ''
  
  try {
    return dayjs(`2024-01-01 ${timeStr}`).format('HH:mm')
  } catch (error) {
    console.warn('formatTimeOnly error:', error, 'timeStr:', timeStr)
    return timeStr
  }
}