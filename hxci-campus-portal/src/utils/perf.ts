/**
 * 性能监控工具
 * 
 * @description 性能测量、监控和优化相关工具函数
 * @author Claude Code AI Assistant  
 * @date 2025-09-14
 */

/**
 * 性能计时器类
 */
export class PerformanceTimer {
  private startTime: number = 0
  private marks: Map<string, number> = new Map()
  private measures: Map<string, number> = new Map()

  /**
   * 开始计时
   */
  start(): void {
    this.startTime = performance.now()
  }

  /**
   * 标记时间点
   * @param name 标记名称
   */
  mark(name: string): void {
    this.marks.set(name, performance.now())
  }

  /**
   * 测量两个标记之间的时间
   * @param name 测量名称
   * @param startMark 开始标记
   * @param endMark 结束标记
   * @returns 耗时（毫秒）
   */
  measure(name: string, startMark: string, endMark: string): number {
    const start = this.marks.get(startMark)
    const end = this.marks.get(endMark)
    
    if (!start || !end) {
      console.warn(`[PerformanceTimer] 标记 ${startMark} 或 ${endMark} 不存在`)
      return 0
    }
    
    const duration = end - start
    this.measures.set(name, duration)
    return duration
  }

  /**
   * 结束计时并返回总耗时
   * @returns 总耗时（毫秒）
   */
  end(): number {
    return performance.now() - this.startTime
  }

  /**
   * 获取所有测量结果
   * @returns 测量结果对象
   */
  getResults(): { total: number; marks: Record<string, number>; measures: Record<string, number> } {
    return {
      total: this.end(),
      marks: Object.fromEntries(this.marks),
      measures: Object.fromEntries(this.measures)
    }
  }

  /**
   * 清空所有标记和测量
   */
  clear(): void {
    this.marks.clear()
    this.measures.clear()
    this.startTime = 0
  }
}

/**
 * 性能监控器
 * 用于监控函数执行性能
 */
export class PerformanceMonitor {
  private metrics: Map<string, number[]> = new Map()
  private enabled: boolean

  constructor(enabled = true) {
    this.enabled = enabled
  }

  /**
   * 监控函数执行时间
   * @param name 监控名称
   * @param fn 要监控的函数
   * @returns 包装后的函数
   */
  monitor<T extends (...args: any[]) => any>(
    name: string,
    fn: T
  ): T {
    if (!this.enabled) {
      return fn
    }

    const self = this
    return function(this: any, ...args: Parameters<T>): ReturnType<T> {
      const start = performance.now()
      const result = fn.apply(this, args)
      const duration = performance.now() - start

      // 如果是Promise，等待其完成
      if (result instanceof Promise) {
        return result.finally(() => {
          const totalDuration = performance.now() - start
          self.record(name, totalDuration)
        }) as ReturnType<T>
      }

      self.record(name, duration)
      return result
    } as T
  }

  /**
   * 记录性能数据
   * @param name 监控名称
   * @param duration 执行时长
   */
  private record(name: string, duration: number): void {
    if (!this.metrics.has(name)) {
      this.metrics.set(name, [])
    }
    
    const records = this.metrics.get(name)!
    records.push(duration)
    
    // 只保留最近100条记录
    if (records.length > 100) {
      records.shift()
    }

    if (import.meta.env.DEV) {
      console.log(`⏱️ [Performance] ${name}: ${duration.toFixed(2)}ms`)
    }
  }

  /**
   * 获取性能统计
   * @param name 监控名称
   * @returns 统计信息
   */
  getStats(name: string): {
    count: number
    avg: number
    min: number
    max: number
    last: number
  } | null {
    const records = this.metrics.get(name)
    if (!records || records.length === 0) {
      return null
    }

    const sum = records.reduce((a, b) => a + b, 0)
    return {
      count: records.length,
      avg: sum / records.length,
      min: Math.min(...records),
      max: Math.max(...records),
      last: records[records.length - 1]
    }
  }

  /**
   * 获取所有性能统计
   * @returns 所有统计信息
   */
  getAllStats(): Record<string, ReturnType<typeof this.getStats>> {
    const stats: Record<string, ReturnType<typeof this.getStats>> = {}
    
    for (const [name] of this.metrics) {
      stats[name] = this.getStats(name)
    }
    
    return stats
  }

  /**
   * 清空指定监控的数据
   * @param name 监控名称
   */
  clear(name?: string): void {
    if (name) {
      this.metrics.delete(name)
    } else {
      this.metrics.clear()
    }
  }

  /**
   * 启用/禁用监控
   * @param enabled 是否启用
   */
  setEnabled(enabled: boolean): void {
    this.enabled = enabled
  }
}

/**
 * 创建全局性能监控器实例
 */
export const globalPerformanceMonitor = new PerformanceMonitor(
  import.meta.env.DEV // 仅在开发环境启用
)

/**
 * 装饰器：监控方法执行时间
 * @param name 监控名称（可选）
 */
export function MonitorPerformance(name?: string) {
  return function(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    const originalMethod = descriptor.value
    const monitorName = name || `${target.constructor.name}.${propertyKey}`
    
    descriptor.value = globalPerformanceMonitor.monitor(monitorName, originalMethod)
    
    return descriptor
  }
}

/**
 * 测量代码块执行时间
 * @param name 测量名称
 * @param fn 要测量的代码块
 * @returns 执行结果
 */
export async function measureTime<T>(
  name: string,
  fn: () => T | Promise<T>
): Promise<T> {
  const start = performance.now()
  
  try {
    const result = await fn()
    const duration = performance.now() - start
    
    if (import.meta.env.DEV) {
      console.log(`⏱️ [Measure] ${name}: ${duration.toFixed(2)}ms`)
    }
    
    return result
  } catch (error) {
    const duration = performance.now() - start
    
    if (import.meta.env.DEV) {
      console.error(`⏱️ [Measure] ${name}: ${duration.toFixed(2)}ms (失败)`)
    }
    
    throw error
  }
}

/**
 * FPS监控器
 * 用于监控页面帧率
 */
export class FPSMonitor {
  private frameCount = 0
  private lastTime = performance.now()
  private fps = 0
  private animationId: number | null = null
  private callback?: (fps: number) => void

  /**
   * 开始监控
   * @param callback FPS更新回调
   */
  start(callback?: (fps: number) => void): void {
    this.callback = callback
    this.lastTime = performance.now()
    this.frameCount = 0
    this.tick()
  }

  /**
   * 停止监控
   */
  stop(): void {
    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId)
      this.animationId = null
    }
  }

  /**
   * 获取当前FPS
   */
  getFPS(): number {
    return Math.round(this.fps)
  }

  private tick = (): void => {
    const currentTime = performance.now()
    this.frameCount++

    if (currentTime >= this.lastTime + 1000) {
      this.fps = (this.frameCount * 1000) / (currentTime - this.lastTime)
      this.frameCount = 0
      this.lastTime = currentTime
      
      if (this.callback) {
        this.callback(this.getFPS())
      }
    }

    this.animationId = requestAnimationFrame(this.tick)
  }
}