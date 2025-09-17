/**
 * 通用工具函数
 * 
 * @description 防抖、节流等通用函数工具
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

/**
 * 防抖函数
 * 在事件被触发n毫秒后再执行回调，如果在这n毫秒内又被触发，则重新计时
 * 
 * @param func 要防抖的函数
 * @param wait 等待时间（毫秒）
 * @param immediate 是否立即执行
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number,
  immediate = false
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | null = null
  let result: any

  const debounced = function(this: any, ...args: Parameters<T>) {
    const context = this

    const later = () => {
      timeout = null
      if (!immediate) {
        result = func.apply(context, args)
      }
    }

    const callNow = immediate && !timeout
    
    if (timeout) {
      clearTimeout(timeout)
    }
    
    timeout = setTimeout(later, wait)
    
    if (callNow) {
      result = func.apply(context, args)
    }
    
    return result
  }

  // 添加取消方法
  debounced.cancel = () => {
    if (timeout) {
      clearTimeout(timeout)
      timeout = null
    }
  }

  return debounced
}

/**
 * 节流函数
 * 规定在一个单位时间内，只能触发一次函数。如果这个单位时间内触发多次函数，只有一次生效
 * 
 * @param func 要节流的函数
 * @param wait 等待时间（毫秒）
 * @param options 配置选项
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  wait: number,
  options: { leading?: boolean; trailing?: boolean } = {}
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | null = null
  let context: any
  let args: any
  let result: any
  let previous = 0

  const { leading = true, trailing = true } = options

  const later = () => {
    previous = leading === false ? 0 : Date.now()
    timeout = null
    result = func.apply(context, args)
    if (!timeout) {
      context = args = null
    }
  }

  const throttled = function(this: any, ...funcArgs: Parameters<T>) {
    const now = Date.now()
    if (!previous && leading === false) {
      previous = now
    }
    
    const remaining = wait - (now - previous)
    context = this
    args = funcArgs
    
    if (remaining <= 0 || remaining > wait) {
      if (timeout) {
        clearTimeout(timeout)
        timeout = null
      }
      previous = now
      result = func.apply(context, args)
      if (!timeout) {
        context = args = null
      }
    } else if (!timeout && trailing !== false) {
      timeout = setTimeout(later, remaining)
    }
    
    return result
  }

  throttled.cancel = () => {
    if (timeout) {
      clearTimeout(timeout)
    }
    previous = 0
    timeout = context = args = null
  }

  return throttled
}

/**
 * 延迟函数
 * 返回一个Promise，在指定时间后resolve
 * 
 * @param ms 延迟时间（毫秒）
 * @returns Promise
 */
export function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 重试函数
 * 在函数执行失败时自动重试
 * 
 * @param fn 要执行的函数
 * @param retries 重试次数
 * @param delayMs 重试间隔（毫秒）
 * @returns Promise
 */
export async function retry<T>(
  fn: () => Promise<T>,
  retries = 3,
  delayMs = 1000
): Promise<T> {
  let lastError: Error | unknown
  
  for (let i = 0; i <= retries; i++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error
      if (i < retries) {
        await delay(delayMs * Math.pow(2, i)) // 指数退避
      }
    }
  }
  
  throw lastError
}

/**
 * 深拷贝函数
 * 创建一个对象的深拷贝
 * 
 * @param obj 要拷贝的对象
 * @returns 深拷贝后的对象
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') {
    return obj
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime()) as any
  }
  
  if (obj instanceof Array) {
    return obj.map(item => deepClone(item)) as any
  }
  
  if (obj instanceof Object) {
    const clonedObj: any = {}
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key])
      }
    }
    return clonedObj
  }
  
  return obj
}