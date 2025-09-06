/**
 * 🚀 Stage 9性能优化: 性能分析器
 * 用于量化测量和报告前端性能优化效果
 */

export interface PerformanceMetric {
  name: string
  startTime: number
  endTime?: number
  duration?: number
  category: 'component' | 'api' | 'render' | 'interaction' | 'memory'
  severity: 'info' | 'warning' | 'error'
}

export interface PerformanceReport {
  totalMetrics: number
  averageDuration: number
  slowOperations: PerformanceMetric[]
  memoryUsage: {
    used: number
    total: number
    percentage: number
  }
  bundleAnalysis: {
    initialLoad: number
    lazyLoaded: number
    cacheHitRate: number
  }
  userExperience: {
    firstContentfulPaint: number
    largestContentfulPaint: number
    cumulativeLayoutShift: number
    firstInputDelay: number
  }
}

class PerformanceAnalyzer {
  private metrics: PerformanceMetric[] = []
  private observer?: PerformanceObserver
  private memoryInterval?: NodeJS.Timeout

  constructor() {
    this.initializeObserver()
    this.startMemoryMonitoring()
  }

  private initializeObserver() {
    if ('PerformanceObserver' in window) {
      try {
        this.observer = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            this.recordWebVital(entry)
          }
        })
        
        // 监控关键性能指标
        this.observer.observe({ entryTypes: ['navigation', 'paint', 'largest-contentful-paint', 'first-input', 'layout-shift'] })
      } catch (error) {
        console.warn('⚠️ PerformanceObserver初始化失败:', error)
      }
    }
  }

  private recordWebVital(entry: PerformanceEntry) {
    const metric: PerformanceMetric = {
      name: entry.name || entry.entryType,
      startTime: entry.startTime,
      endTime: entry.startTime + (entry.duration || 0),
      duration: entry.duration || 0,
      category: this.categorizeEntry(entry),
      severity: this.assessSeverity(entry)
    }

    this.metrics.push(metric)
    
    // 关键指标预警
    if (metric.severity === 'warning' || metric.severity === 'error') {
      console.warn(`⚠️ [性能预警] ${metric.name}: ${metric.duration?.toFixed(2)}ms`)
    }
  }

  private categorizeEntry(entry: PerformanceEntry): PerformanceMetric['category'] {
    const name = entry.name || entry.entryType
    
    if (name.includes('paint') || name.includes('render')) return 'render'
    if (name.includes('navigation') || name.includes('resource')) return 'api'
    if (name.includes('input') || name.includes('click')) return 'interaction'
    if (name.includes('layout') || name.includes('shift')) return 'render'
    
    return 'component'
  }

  private assessSeverity(entry: PerformanceEntry): PerformanceMetric['severity'] {
    const duration = entry.duration || 0
    
    // 根据不同类型的操作设置不同的阈值
    if (entry.entryType === 'paint') {
      return duration > 1000 ? 'error' : duration > 500 ? 'warning' : 'info'
    }
    
    if (entry.entryType === 'first-input') {
      return duration > 300 ? 'error' : duration > 100 ? 'warning' : 'info'
    }
    
    if (entry.entryType === 'largest-contentful-paint') {
      return duration > 4000 ? 'error' : duration > 2500 ? 'warning' : 'info'
    }
    
    return duration > 1000 ? 'error' : duration > 500 ? 'warning' : 'info'
  }

  private startMemoryMonitoring() {
    if ('memory' in performance) {
      this.memoryInterval = setInterval(() => {
        const memory = (performance as any).memory
        if (memory) {
          const memoryMetric: PerformanceMetric = {
            name: 'memory-usage',
            startTime: performance.now(),
            duration: memory.usedJSHeapSize,
            category: 'memory',
            severity: memory.usedJSHeapSize / memory.jsHeapSizeLimit > 0.8 ? 'warning' : 'info'
          }
          this.metrics.push(memoryMetric)
        }
      }, 10000) // 每10秒检查一次内存
    }
  }

  /**
   * 记录自定义性能指标
   */
  recordMetric(name: string, duration: number, category: PerformanceMetric['category'] = 'component') {
    const metric: PerformanceMetric = {
      name,
      startTime: performance.now() - duration,
      endTime: performance.now(),
      duration,
      category,
      severity: duration > 1000 ? 'error' : duration > 500 ? 'warning' : 'info'
    }

    this.metrics.push(metric)
    console.log(`📊 [性能记录] ${name}: ${duration.toFixed(2)}ms`)
  }

  /**
   * 生成性能报告
   */
  generateReport(): PerformanceReport {
    const now = performance.now()
    const recentMetrics = this.metrics.filter(m => (m.startTime > now - 60000)) // 最近1分钟的数据

    // 计算平均持续时间
    const durations = recentMetrics.map(m => m.duration || 0).filter(d => d > 0)
    const averageDuration = durations.length > 0 ? durations.reduce((a, b) => a + b, 0) / durations.length : 0

    // 找出慢操作 (>500ms)
    const slowOperations = recentMetrics.filter(m => (m.duration || 0) > 500)

    // 内存使用情况
    const memoryMetrics = recentMetrics.filter(m => m.category === 'memory')
    const latestMemory = memoryMetrics[memoryMetrics.length - 1]
    const memoryUsage = this.getMemoryUsage()

    // Web Vitals 指标
    const userExperience = this.calculateWebVitals()

    // Bundle 分析
    const bundleAnalysis = this.analyzeBundlePerformance()

    return {
      totalMetrics: recentMetrics.length,
      averageDuration,
      slowOperations,
      memoryUsage,
      bundleAnalysis,
      userExperience
    }
  }

  private getMemoryUsage() {
    if ('memory' in performance) {
      const memory = (performance as any).memory
      return {
        used: Math.round(memory.usedJSHeapSize / 1024 / 1024), // MB
        total: Math.round(memory.totalJSHeapSize / 1024 / 1024), // MB
        percentage: Math.round((memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100)
      }
    }
    return { used: 0, total: 0, percentage: 0 }
  }

  private calculateWebVitals() {
    const paintMetrics = this.metrics.filter(m => m.name?.includes('paint'))
    const lcpMetrics = this.metrics.filter(m => m.name?.includes('largest-contentful-paint'))
    const fidMetrics = this.metrics.filter(m => m.name?.includes('first-input'))
    const clsMetrics = this.metrics.filter(m => m.name?.includes('layout-shift'))

    return {
      firstContentfulPaint: paintMetrics.find(m => m.name?.includes('first-contentful'))?.duration || 0,
      largestContentfulPaint: lcpMetrics[lcpMetrics.length - 1]?.duration || 0,
      cumulativeLayoutShift: clsMetrics.reduce((sum, m) => sum + (m.duration || 0), 0),
      firstInputDelay: fidMetrics[0]?.duration || 0
    }
  }

  private analyzeBundlePerformance() {
    // 计算缓存命中率
    const cacheHits = sessionStorage.length
    const totalRequests = this.metrics.filter(m => m.category === 'api').length
    const cacheHitRate = totalRequests > 0 ? (cacheHits / totalRequests) * 100 : 0

    return {
      initialLoad: performance.timing ? performance.timing.loadEventEnd - performance.timing.navigationStart : 0,
      lazyLoaded: this.metrics.filter(m => m.name?.includes('lazy') || m.name?.includes('async')).length,
      cacheHitRate: Math.min(cacheHitRate, 100) // 限制在100%以内
    }
  }

  /**
   * 打印详细性能报告
   */
  printDetailedReport() {
    const report = this.generateReport()
    
    console.group('🚀 Stage 9 性能优化报告')
    console.log('📊 总体指标:')
    console.log(`   总测量次数: ${report.totalMetrics}`)
    console.log(`   平均响应时间: ${report.averageDuration.toFixed(2)}ms`)
    console.log(`   慢操作数量: ${report.slowOperations.length}`)
    
    console.log('🧠 内存使用:')
    console.log(`   已用内存: ${report.memoryUsage.used}MB`)
    console.log(`   总内存: ${report.memoryUsage.total}MB`)
    console.log(`   使用率: ${report.memoryUsage.percentage}%`)
    
    console.log('📦 Bundle优化:')
    console.log(`   初始加载时间: ${report.bundleAnalysis.initialLoad.toFixed(2)}ms`)
    console.log(`   懒加载组件数: ${report.bundleAnalysis.lazyLoaded}`)
    console.log(`   缓存命中率: ${report.bundleAnalysis.cacheHitRate.toFixed(1)}%`)
    
    console.log('👤 用户体验 (Web Vitals):')
    console.log(`   首次内容绘制: ${report.userExperience.firstContentfulPaint.toFixed(2)}ms`)
    console.log(`   最大内容绘制: ${report.userExperience.largestContentfulPaint.toFixed(2)}ms`)
    console.log(`   首次输入延迟: ${report.userExperience.firstInputDelay.toFixed(2)}ms`)
    console.log(`   累积布局偏移: ${report.userExperience.cumulativeLayoutShift.toFixed(3)}`)
    
    if (report.slowOperations.length > 0) {
      console.warn('⚠️ 慢操作详情:')
      report.slowOperations.forEach(op => {
        console.warn(`   ${op.name}: ${op.duration?.toFixed(2)}ms (${op.category})`)
      })
    }
    
    console.groupEnd()
    
    return report
  }

  /**
   * 清理资源
   */
  cleanup() {
    if (this.observer) {
      this.observer.disconnect()
    }
    if (this.memoryInterval) {
      clearInterval(this.memoryInterval)
    }
    this.metrics = []
  }

  /**
   * 获取优化建议
   */
  getOptimizationSuggestions(): string[] {
    const report = this.generateReport()
    const suggestions: string[] = []

    if (report.averageDuration > 500) {
      suggestions.push('平均响应时间偏高，建议检查计算属性缓存策略')
    }

    if (report.memoryUsage.percentage > 80) {
      suggestions.push('内存使用率过高，建议检查内存泄漏和优化组件生命周期')
    }

    if (report.bundleAnalysis.cacheHitRate < 50) {
      suggestions.push('缓存命中率较低，建议增加缓存策略')
    }

    if (report.userExperience.largestContentfulPaint > 2500) {
      suggestions.push('LCP指标超标，建议优化首屏渲染性能')
    }

    if (report.userExperience.firstInputDelay > 100) {
      suggestions.push('FID指标超标，建议优化交互响应性能')
    }

    if (report.slowOperations.length > 5) {
      suggestions.push('慢操作过多，建议使用防抖节流优化用户交互')
    }

    return suggestions.length > 0 ? suggestions : ['🎉 性能表现良好，无需额外优化建议']
  }
}

// 创建全局性能分析器实例
export const performanceAnalyzer = new PerformanceAnalyzer()

// 在开发模式下挂载到window对象，方便调试
if (import.meta.env.DEV) {
  (window as any).performanceAnalyzer = performanceAnalyzer
}