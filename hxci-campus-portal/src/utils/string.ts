/**
 * 字符串处理工具函数集合
 * 统一项目中所有字符串处理逻辑，避免重复代码
 */

/**
 * 截断长文本并添加省略号
 * @param text 需要截断的文本
 * @param maxLength 最大长度，默认为100
 * @param ellipsis 省略号，默认为'...'
 * @returns 截断后的文本
 */
export const truncate = (text: string, maxLength: number = 100, ellipsis: string = '...'): string => {
  if (!text || typeof text !== 'string') return ''
  
  if (text.length <= maxLength) {
    return text
  }
  
  return text.slice(0, maxLength) + ellipsis
}

/**
 * 首字母大写
 * @param text 需要处理的文本
 * @returns 首字母大写的文本
 */
export const capitalize = (text: string): string => {
  if (!text || typeof text !== 'string') return ''
  
  return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase()
}

/**
 * 驼峰命名转换为连字符命名
 * @param text 驼峰命名字符串
 * @returns 连字符命名字符串
 */
export const camelToKebab = (text: string): string => {
  if (!text || typeof text !== 'string') return ''
  
  return text.replace(/([A-Z])/g, '-$1').toLowerCase().replace(/^-/, '')
}

/**
 * 连字符命名转换为驼峰命名
 * @param text 连字符命名字符串
 * @returns 驼峰命名字符串
 */
export const kebabToCamel = (text: string): string => {
  if (!text || typeof text !== 'string') return ''
  
  return text.replace(/-([a-z])/g, (match, letter) => letter.toUpperCase())
}

/**
 * 移除字符串中的HTML标签
 * @param html HTML字符串
 * @returns 纯文本字符串
 */
export const stripHtml = (html: string): string => {
  if (!html || typeof html !== 'string') return ''
  
  return html.replace(/<[^>]*>/g, '')
}

/**
 * 转义HTML特殊字符
 * @param text 需要转义的文本
 * @returns 转义后的文本
 */
export const escapeHtml = (text: string): string => {
  if (!text || typeof text !== 'string') return ''
  
  const entityMap: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '/': '&#x2F;'
  }
  
  return text.replace(/[&<>"'\/]/g, (char) => entityMap[char])
}

/**
 * 反转义HTML特殊字符
 * @param html 需要反转义的HTML文本
 * @returns 反转义后的文本
 */
export const unescapeHtml = (html: string): string => {
  if (!html || typeof html !== 'string') return ''
  
  const entityMap: Record<string, string> = {
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&#x27;': "'",
    '&#x2F;': '/'
  }
  
  return html.replace(/&(amp|lt|gt|quot|#x27|#x2F);/g, (entity) => entityMap[entity])
}

/**
 * 生成随机字符串
 * @param length 字符串长度，默认为8
 * @param characters 可用字符集，默认为字母数字
 * @returns 随机字符串
 */
export const randomString = (length: number = 8, characters: string = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'): string => {
  let result = ''
  for (let i = 0; i < length; i++) {
    result += characters.charAt(Math.floor(Math.random() * characters.length))
  }
  return result
}

/**
 * 检查字符串是否为空或只包含空白字符
 * @param text 需要检查的字符串
 * @returns 是否为空白字符串
 */
export const isBlank = (text: string | null | undefined): boolean => {
  return !text || text.trim().length === 0
}

/**
 * 安全的字符串插值替换
 * @param template 模板字符串，使用 {key} 作为占位符
 * @param data 替换数据
 * @returns 替换后的字符串
 */
export const template = (template: string, data: Record<string, any>): string => {
  if (!template || typeof template !== 'string') return ''
  
  return template.replace(/\{(\w+)\}/g, (match, key) => {
    return data[key] !== undefined ? String(data[key]) : match
  })
}