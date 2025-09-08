/**
 * 数字处理工具函数集合
 * 统一项目中所有数字格式化和计算逻辑，避免重复代码
 */

/**
 * 格式化数字，添加千位分隔符
 * @param num 需要格式化的数字
 * @param separator 分隔符，默认为','
 * @returns 格式化后的数字字符串，无效输入返回'N/A'
 */
export const formatNumber = (num: number | string, separator: string = ','): string => {
  if (num === null || num === undefined || num === '') return 'N/A'
  
  const numValue = typeof num === 'string' ? parseFloat(num) : num
  if (isNaN(numValue)) return 'N/A'
  
  const numStr = String(numValue)
  const [integerPart, decimalPart] = numStr.split('.')
  
  // 添加千位分隔符
  const formattedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, separator)
  
  return decimalPart ? `${formattedInteger}.${decimalPart}` : formattedInteger
}

/**
 * 格式化货币
 * @param amount 金额
 * @param currency 货币符号，默认为'¥'
 * @param decimals 小数位数，默认为2
 * @returns 格式化后的货币字符串
 */
export const formatCurrency = (amount: number | string, currency: string = '¥', decimals: number = 2): string => {
  if (amount === null || amount === undefined || amount === '') return `${currency}0.00`
  
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return `${currency}0.00`
  
  return `${currency}${formatNumber(num.toFixed(decimals))}`
}

/**
 * 格式化百分比
 * @param value 数值
 * @param decimals 小数位数，默认为1
 * @returns 格式化后的百分比字符串
 */
export const formatPercent = (value: number | string, decimals: number = 1): string => {
  if (value === null || value === undefined || value === '') return '0.0%'
  
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0.0%'
  
  return `${(num * 100).toFixed(decimals)}%`
}

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @param decimals 小数位数，默认为2
 * @returns 格式化后的文件大小字符串
 */
export const formatFileSize = (bytes: number, decimals: number = 2): string => {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(decimals))} ${sizes[i]}`
}

/**
 * 格式化大数字（如：1K、1M、1B）
 * @param num 数字
 * @param decimals 小数位数，默认为1
 * @returns 格式化后的数字字符串
 */
export const formatLargeNumber = (num: number, decimals: number = 1): string => {
  if (num === 0) return '0'
  
  const absNum = Math.abs(num)
  const sign = num < 0 ? '-' : ''
  
  if (absNum >= 1e9) {
    return `${sign}${(absNum / 1e9).toFixed(decimals)}B`
  } else if (absNum >= 1e6) {
    return `${sign}${(absNum / 1e6).toFixed(decimals)}M`
  } else if (absNum >= 1e3) {
    return `${sign}${(absNum / 1e3).toFixed(decimals)}K`
  }
  
  return String(num)
}

/**
 * 生成随机数
 * @param min 最小值（包含）
 * @param max 最大值（包含）
 * @returns 随机数
 */
export const randomInt = (min: number, max: number): number => {
  min = Math.ceil(min)
  max = Math.floor(max)
  return Math.floor(Math.random() * (max - min + 1)) + min
}

/**
 * 生成随机浮点数
 * @param min 最小值（包含）
 * @param max 最大值（不包含）
 * @param decimals 小数位数，默认为2
 * @returns 随机浮点数
 */
export const randomFloat = (min: number, max: number, decimals: number = 2): number => {
  const num = Math.random() * (max - min) + min
  return parseFloat(num.toFixed(decimals))
}

/**
 * 限制数字在指定范围内
 * @param num 数字
 * @param min 最小值
 * @param max 最大值
 * @returns 限制后的数字
 * @throws Error 当 min > max 时抛出错误
 */
export const clamp = (num: number, min: number, max: number): number => {
  if (min > max) {
    throw new Error(`clamp: min (${min}) cannot be greater than max (${max})`)
  }
  
  return Math.min(Math.max(num, min), max)
}

/**
 * 安全的数字转换
 * @param value 需要转换的值
 * @param defaultValue 默认值，当转换失败时返回
 * @returns 转换后的数字
 */
export const toNumber = (value: any, defaultValue: number = 0): number => {
  if (value === null || value === undefined || value === '') {
    return defaultValue
  }
  
  const num = Number(value)
  return isNaN(num) ? defaultValue : num
}

/**
 * 检查是否为有效数字
 * @param value 需要检查的值
 * @returns 是否为有效数字
 */
export const isValidNumber = (value: any): boolean => {
  return typeof value === 'number' && !isNaN(value) && isFinite(value)
}

/**
 * 四舍五入到指定小数位
 * @param num 数字
 * @param decimals 小数位数
 * @returns 四舍五入后的数字
 */
export const round = (num: number, decimals: number = 0): number => {
  return Math.round(num * Math.pow(10, decimals)) / Math.pow(10, decimals)
}