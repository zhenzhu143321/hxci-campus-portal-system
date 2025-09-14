import dayjs from 'dayjs'

/**
 * 日期时间格式化工具函数集合
 * 统一项目中所有日期时间处理逻辑，避免重复代码
 */

/**
 * 将日期/时间戳/字符串格式化为完整日期时间格式
 * @param value 需要格式化的日期，可以是 Date 对象、时间戳或ISO格式字符串
 * @param format 目标格式，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期字符串，出错时返回默认值
 */
export const formatDate = (value: Date | number | string | null | undefined, format: string = 'YYYY-MM-DD HH:mm:ss'): string => {
  // 🔧 P0级修复：增强输入验证，处理null/undefined/空值情况
  if (!value || value === null || value === undefined) {
    console.debug('formatDate: Empty or null input, returning default')
    return '--'
  }

  // 🔧 处理空字符串和无效字符串
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (trimmed === '' || trimmed === 'null' || trimmed === 'undefined') {
      console.debug('formatDate: Invalid string input, returning default')
      return '--'
    }
    
    // 🔧 关键修复：检测相对时间字符串（如"1天前"、"2小时前"等）
    // 这些字符串不应该被当作日期来解析
    if (trimmed.includes('前') || trimmed === '刚刚') {
      console.debug('formatDate: Relative time string detected, returning as-is:', trimmed)
      return trimmed // 直接返回相对时间字符串，不尝试格式化
    }
  }
  
  try {
    const dayjsObj = dayjs(value)
    
    // 🔧 关键修复：检查dayjs对象的有效性
    if (!dayjsObj.isValid()) {
      // 如果是字符串，尝试返回原值而不是显示错误
      if (typeof value === 'string') {
        console.debug('formatDate: Invalid date but returning original string:', value)
        return value
      }
      console.warn('formatDate: Invalid date object created from value:', value)
      return '无效日期'
    }
    
    return dayjsObj.format(format)
  } catch (error) {
    console.warn('formatDate error:', error, 'value:', value, 'type:', typeof value)
    return '格式错误'
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
 * @returns 本地化格式的日期字符串，出错时返回默认值
 */
export const formatDateLocale = (dateStr: string | null | undefined): string => {
  // 🔧 P0级修复：增强输入验证
  if (!dateStr || dateStr === null || dateStr === undefined) {
    console.debug('formatDateLocale: Empty input, returning default')
    return '--'
  }

  // 🔧 处理无效字符串
  if (typeof dateStr === 'string' && (dateStr.trim() === '' || dateStr === 'null' || dateStr === 'undefined')) {
    console.debug('formatDateLocale: Invalid string input, returning default')
    return '--'
  }
  
  try {
    const date = new Date(dateStr)
    
    // 🔧 关键修复：检查日期有效性
    if (isNaN(date.getTime())) {
      console.warn('formatDateLocale: Invalid date created from dateStr:', dateStr)
      return '无效日期'
    }
    
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch (error) {
    console.warn('formatDateLocale error:', error, 'dateStr:', dateStr)
    return '日期错误'
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
export const formatTimeIntelligent = (timeStr: string | null | undefined): string => {
  console.debug('🔍 [formatTimeIntelligent] 调用参数:', timeStr, 'type:', typeof timeStr)

  // 🔧 P0级修复：增强输入验证
  if (!timeStr || timeStr === null || timeStr === undefined) {
    console.debug('formatTimeIntelligent: Empty input, returning default')
    return '--'
  }

  // 🔧 处理无效字符串
  if (typeof timeStr === 'string' && (timeStr.trim() === '' || timeStr === 'null' || timeStr === 'undefined')) {
    console.debug('formatTimeIntelligent: Invalid string input, returning default')
    return '--'
  }
  
  try {
    // 🔧 核心修复：使用安全的日期解析，而不是new Date()
    const date = parseBackendDateString(timeStr)
    if (!date) {
      console.warn('formatTimeIntelligent: Failed to parse date input:', timeStr)
      return '无效日期'
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
    return '时间错误'
  }
}

/**
 * 格式化时间字符串为HH:mm格式（仅限时间部分）
 * @param timeStr 时间字符串（如：'09:30'、'14:15'）
 * @returns 格式化后的时间字符串（HH:mm）
 */
export const formatTimeOnly = (timeStr: string): string => {
  if (!timeStr) return '--'
  
  try {
    return dayjs(`2024-01-01 ${timeStr}`).format('HH:mm')
  } catch (error) {
    console.warn('formatTimeOnly error:', error, 'timeStr:', timeStr)
    return timeStr || '--'
  }
}

/**
 * 专门处理API返回数据的日期格式化函数
 * 🔧 P0级修复：针对前端API数据显示异常的专用函数
 * @param apiDateValue API返回的日期数据（可能为null、undefined、空字符串或有效日期）
 * @param options 格式化选项，包括format和fallback
 * @returns 格式化后的日期字符串，异常情况返回用户友好的提示
 */
export const formatApiDate = (
  apiDateValue: any,
  options: {
    format?: string;
    fallback?: string;
    useIntelligent?: boolean;
  } = {}
): string => {
  const { format, fallback = '--', useIntelligent = true } = options

  console.debug('🔍 [formatApiDate] 调用参数:', {
    apiDateValue,
    type: typeof apiDateValue,
    format,
    fallback,
    useIntelligent
  })

  // 🔧 第一层：处理完全无效的输入
  if (apiDateValue === null || apiDateValue === undefined) {
    console.debug('formatApiDate: null/undefined input, returning fallback:', fallback)
    return fallback
  }

  // 🔧 第二层：处理空值或无效字符串
  if (typeof apiDateValue === 'string') {
    const trimmed = apiDateValue.trim()
    if (trimmed === '' || trimmed === 'null' || trimmed === 'undefined' || trimmed === 'Invalid Date') {
      console.debug('formatApiDate: empty/invalid string input, returning fallback:', fallback)
      return fallback
    }
  }

  // 🔧 第三层：尝试使用指定格式化函数
  try {
    if (format) {
      const result = formatDate(apiDateValue, format)
      // 如果formatDate返回"无效日期"，使用fallback
      return result === '无效日期' ? fallback : result
    } else if (useIntelligent) {
      // 默认使用智能时间显示
      const result = formatTimeIntelligent(apiDateValue)
      // 如果formatTimeIntelligent返回"无效日期"，使用fallback
      return result === '无效日期' ? fallback : result
    } else {
      const result = formatDate(apiDateValue)
      return result === '无效日期' ? fallback : result
    }
  } catch (error) {
    console.warn('formatApiDate: Formatting failed for', apiDateValue, error)
    return fallback
  }
}

/**
 * 安全的通知时间格式化函数
 * 🔧 专门用于通知系统的createTime、updateTime等字段
 * @param notificationTime 通知时间字段
 * @returns 格式化后的时间字符串
 */
export const formatNotificationTime = (notificationTime: any): string => {
  return formatApiDate(notificationTime, { format: 'YYYY-MM-DD HH:mm' })
}

/**
 * 🚨 核心修复：安全的日期字符串解析函数
 * 专门处理后端返回的 'YYYY-MM-DD HH:mm:ss' 格式字符串
 * 解决 new Date() 在不同浏览器下解析不一致的问题
 *
 * @param dateString 后端返回的日期字符串，如 '2025-09-13 12:30:45'
 * @returns Date对象或null（解析失败时）
 */
export const parseBackendDateString = (dateString: string | null | undefined): Date | null => {
  if (!dateString) {
    console.debug('parseBackendDateString: Empty input')
    return null
  }

  try {
    const trimmed = dateString.trim()

    // 🔧 核心修复：智能处理多种日期格式
    let isoString: string

    if (trimmed.includes('T')) {
      // 已经是ISO格式 (2025-09-13T10:13:30)
      isoString = trimmed
    } else if (trimmed.includes(' ')) {
      // 后端格式 (2025-09-13 10:13:30) -> ISO格式
      isoString = trimmed.replace(' ', 'T')
    } else {
      // 纯日期格式 (2025-09-13) -> 添加时间
      isoString = `${trimmed}T00:00:00`
    }

    console.debug('🔍 [parseBackendDateString] 解析:', dateString, '->', isoString)

    const date = new Date(isoString)

    // 🔧 验证解析结果
    if (isNaN(date.getTime())) {
      console.warn('parseBackendDateString: Failed to parse date string:', dateString)
      return null
    }

    return date
  } catch (error) {
    console.error('parseBackendDateString: Error parsing date string:', dateString, error)
    return null
  }
}

/**
 * 🚨 核心修复：安全的通知数据时间戳转换
 * 替换 NotificationStore 中的 new Date(n.createTime).getTime() 逻辑
 *
 * @param createTime 后端返回的 createTime 字段
 * @returns 时间戳（毫秒）或 0（解析失败时）
 */
export const getNotificationTimestamp = (createTime: string | null | undefined): number => {
  if (!createTime) {
    console.debug('getNotificationTimestamp: Empty createTime')
    return 0
  }

  const date = parseBackendDateString(createTime)
  if (!date) {
    console.warn('getNotificationTimestamp: Failed to parse createTime:', createTime)
    return 0
  }

  const timestamp = date.getTime()
  console.debug('getNotificationTimestamp: Parsed', createTime, 'to timestamp', timestamp)

  return timestamp
}

/**
 * 🚨 优化的通知时间显示函数
 * 专门用于前端组件显示通知的创建时间、更新时间等
 *
 * @param dateValue 日期值（字符串、时间戳或Date对象）
 * @param options 格式化选项
 * @returns 格式化后的日期字符串
 */
export const formatNotificationDate = (
  dateValue: string | number | Date | null | undefined,
  options: {
    format?: string;
    intelligent?: boolean;
    fallback?: string;
  } = {}
): string => {
  const { format = 'YYYY-MM-DD HH:mm', intelligent = false, fallback = '--' } = options

  if (!dateValue) {
    return fallback
  }

  try {
    // 🔧 如果是后端返回的字符串格式，使用安全解析
    if (typeof dateValue === 'string') {
      const parsedDate = parseBackendDateString(dateValue)
      if (!parsedDate) {
        return fallback
      }

      if (intelligent) {
        return formatTimeIntelligent(dateValue)
      } else {
        return dayjs(parsedDate).format(format)
      }
    }

    // 🔧 如果是时间戳或Date对象，直接使用dayjs处理
    const dayjsObj = dayjs(dateValue)
    if (!dayjsObj.isValid()) {
      return fallback
    }

    if (intelligent) {
      return formatTimeIntelligent(dayjsObj.toISOString())
    } else {
      return dayjsObj.format(format)
    }
  } catch (error) {
    console.error('formatNotificationDate: Error formatting date:', dateValue, error)
    return fallback
  }
}