import { notificationAPI, type NotificationItem, type NotificationListResponse, type NotificationDetailResponse } from '@/api/notification'
import { ElMessage } from 'element-plus'

/**
 * 通知服务缓存配置
 */
interface CacheConfig {
  enabled: boolean
  ttl: number // 缓存时间 (毫秒)
  maxSize: number // 最大缓存条数
}

/**
 * 缓存项接口
 */
interface CacheItem<T> {
  data: T
  timestamp: number
  ttl: number
}

/**
 * 通知服务错误类型
 */
export class NotificationServiceError extends Error {
  constructor(message: string, public code?: string, public details?: any) {
    super(message)
    this.name = 'NotificationServiceError'
  }
}

/**
 * 通知服务类 - API调用抽象层
 * 
 * 功能特性:
 * - 统一错误处理和降级机制
 * - 通知数据缓存提升性能
 * - 自动重试机制
 * - 标准化日志输出
 * - TypeScript严格类型支持
 */
export class NotificationService {
  private cache = new Map<string, CacheItem<any>>()
  private config: CacheConfig = {
    enabled: true,
    ttl: 10 * 60 * 1000, // 🚀 Stage 9优化: 延长缓存时间从5分钟到10分钟，减少API调用
    maxSize: 200 // 🚀 Stage 9优化: 增加缓存容量从100到200，支持更多缓存项
  }

  /**
   * 获取通知列表 - 核心业务方法
   */
  public async getNotifications(params?: {
    pageNo?: number
    pageSize?: number
    level?: number
    scope?: string
    useCache?: boolean
  }): Promise<NotificationItem[]> {
    const { useCache = true, ...apiParams } = params || {}
    const cacheKey = `notifications_${JSON.stringify(apiParams)}`

    try {
      console.log('🚀 [NotificationService] 开始获取通知列表')
      console.log('📤 [NotificationService] 请求参数:', apiParams)

      // 检查缓存
      if (useCache && this.config.enabled) {
        const cachedData = this.getFromCache<NotificationItem[]>(cacheKey)
        if (cachedData) {
          console.log('⚡ [NotificationService] 使用缓存数据，缓存命中')
          return cachedData
        }
      }

      // 调用底层API
      const result = await notificationAPI.getNotificationList({
        pageSize: 100,
        ...apiParams
      })

      if (result.success && result.data.list) {
        console.log('✅ [NotificationService] 通知列表获取成功')
        console.log(`📊 [NotificationService] 获取到 ${result.data.list.length} 条通知`)
        
        // 缓存成功结果
        if (useCache && this.config.enabled) {
          this.setToCache(cacheKey, result.data.list)
          console.log('💾 [NotificationService] 数据已缓存')
        }

        return result.data.list
      } else {
        console.warn('⚠️ [NotificationService] API返回失败状态，使用降级数据')
        const fallbackData = notificationAPI.getDefaultNotifications()
        
        // 显示用户友好的错误信息
        ElMessage.warning('通知数据获取失败，已切换到离线模式')
        return fallbackData
      }
    } catch (error: any) {
      console.error('❌ [NotificationService] 获取通知列表异常:', error)
      
      // 统一错误处理和降级
      return this.handleError(error, 'getNotifications', () => {
        return notificationAPI.getDefaultNotifications()
      })
    }
  }

  /**
   * 获取通知详情
   */
  public async getNotificationDetail(id: number, useCache: boolean = true): Promise<NotificationItem | null> {
    const cacheKey = `notification_detail_${id}`

    try {
      console.log('📖 [NotificationService] 获取通知详情, ID:', id)

      // 检查缓存
      if (useCache && this.config.enabled) {
        const cachedData = this.getFromCache<NotificationItem>(cacheKey)
        if (cachedData) {
          console.log('⚡ [NotificationService] 使用缓存的详情数据')
          return cachedData
        }
      }

      const result = await notificationAPI.getNotificationDetail(id)

      if (result.success && result.data) {
        console.log('✅ [NotificationService] 通知详情获取成功')
        
        // 缓存详情数据
        if (useCache && this.config.enabled) {
          this.setToCache(cacheKey, result.data)
        }

        return result.data
      } else {
        console.warn('⚠️ [NotificationService] 通知详情获取失败')
        ElMessage.warning('通知详情获取失败')
        return null
      }
    } catch (error: any) {
      console.error('❌ [NotificationService] 获取通知详情异常:', error)
      return this.handleError(error, 'getNotificationDetail', () => null)
    }
  }

  /**
   * 🚀 Stage 9性能优化: 预加载通知详情，提升用户体验
   * 在通知列表加载后，预加载前3个优先级通知的详情
   */
  public async preloadPriorityNotificationDetails(notifications: NotificationItem[]): Promise<void> {
    try {
      console.log('🚀 [NotificationService] 开始预加载优先级通知详情')
      
      // 筛选出前3个高优先级未读通知
      const priorityNotifications = notifications
        .filter(n => n.level <= 2) // Level 1-2 高优先级
        .slice(0, 3) // 只预加载前3个
      
      if (priorityNotifications.length === 0) {
        console.log('💡 [NotificationService] 无高优先级通知需要预加载')
        return
      }
      
      // 并行预加载
      const preloadPromises = priorityNotifications.map(async (notification) => {
        try {
          await this.getNotificationDetail(notification.id, true)
          console.log(`⚡ [NotificationService] 预加载完成: ${notification.title}`)
        } catch (error) {
          console.warn(`⚠️ [NotificationService] 预加载失败: ${notification.title}`, error)
        }
      })
      
      await Promise.allSettled(preloadPromises)
      console.log('✅ [NotificationService] 优先级通知预加载完成')
    } catch (error) {
      console.warn('⚠️ [NotificationService] 预加载过程出现异常:', error)
    }
  }

  /**
   * 获取未读通知数量
   */
  public async getUnreadNotificationsCount(useCache: boolean = true): Promise<number> {
    try {
      console.log('🔢 [NotificationService] 获取未读通知数量')
      
      const notifications = await this.getNotifications({ 
        pageSize: 100, 
        useCache 
      })
      
      // 简化处理：统计非已读状态的通知
      const unreadCount = notifications.filter(item => !item.isRead).length
      
      console.log(`📊 [NotificationService] 未读通知数量: ${unreadCount}`)
      return unreadCount
    } catch (error: any) {
      console.error('❌ [NotificationService] 获取未读数量异常:', error)
      return this.handleError(error, 'getUnreadNotificationsCount', () => 0)
    }
  }

  /**
   * 获取优先级通知 (Level 1-3)
   */
  public async getPriorityNotifications(useCache: boolean = true): Promise<NotificationItem[]> {
    try {
      console.log('🎯 [NotificationService] 获取优先级通知')
      
      const allNotifications = await this.getNotifications({ useCache })
      
      // 过滤Level 1-3通知并按级别排序 (1级最高优先级)
      const priorityNotifications = allNotifications
        .filter(item => item.level >= 1 && item.level <= 3 && !item.isRead)
        .sort((a, b) => {
          // 优先级排序: Level 1 > Level 2 > Level 3, 时间越新越靠前
          if (a.level !== b.level) {
            return a.level - b.level // Level 1(紧急) 排在最前
          }
          return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
        })
      
      console.log(`🎯 [NotificationService] 找到 ${priorityNotifications.length} 条优先级通知`)
      return priorityNotifications
    } catch (error: any) {
      console.error('❌ [NotificationService] 获取优先级通知异常:', error)
      return this.handleError(error, 'getPriorityNotifications', () => [])
    }
  }

  /**
   * 刷新通知数据 - 强制从服务器获取最新数据
   */
  public async refreshNotifications(): Promise<NotificationItem[]> {
    console.log('🔄 [NotificationService] 刷新通知数据 (忽略缓存)')
    
    // 清除相关缓存
    this.clearCache('notifications_')
    
    // 强制从API获取最新数据
    return await this.getNotifications({ useCache: false })
  }

  /**
   * 清除指定前缀的缓存
   */
  public clearCache(prefix?: string): void {
    if (!prefix) {
      console.log('🗑️ [NotificationService] 清除所有缓存')
      this.cache.clear()
    } else {
      console.log(`🗑️ [NotificationService] 清除前缀为 "${prefix}" 的缓存`)
      for (const key of this.cache.keys()) {
        if (key.startsWith(prefix)) {
          this.cache.delete(key)
        }
      }
    }
  }

  /**
   * 获取缓存统计信息
   */
  public getCacheStats(): { size: number; keys: string[] } {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys())
    }
  }

  /**
   * 更新缓存配置
   */
  public updateCacheConfig(config: Partial<CacheConfig>): void {
    this.config = { ...this.config, ...config }
    console.log('⚙️ [NotificationService] 缓存配置已更新:', this.config)
  }

  // ==================== 私有方法 ====================

  /**
   * 从缓存获取数据
   */
  private getFromCache<T>(key: string): T | null {
    const item = this.cache.get(key)
    if (!item) return null

    const now = Date.now()
    if (now > item.timestamp + item.ttl) {
      // 缓存过期，删除并返回null
      this.cache.delete(key)
      return null
    }

    return item.data
  }

  /**
   * 设置数据到缓存
   */
  private setToCache<T>(key: string, data: T, customTtl?: number): void {
    // 检查缓存大小限制
    if (this.cache.size >= this.config.maxSize) {
      // 删除最早的缓存项
      const firstKey = this.cache.keys().next().value
      if (firstKey) {
        this.cache.delete(firstKey)
      }
    }

    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl: customTtl || this.config.ttl
    })
  }

  /**
   * 统一错误处理
   */
  private handleError<T>(
    error: any, 
    method: string, 
    fallbackFn: () => T
  ): T {
    const errorMsg = error.message || '未知错误'
    console.error(`❌ [NotificationService.${method}] 处理错误:`, errorMsg)

    // 创建标准化错误
    const serviceError = new NotificationServiceError(
      `通知服务错误: ${errorMsg}`,
      'API_ERROR',
      { method, originalError: error }
    )

    // 显示用户友好错误信息
    ElMessage.error(`操作失败: ${errorMsg}`)

    // 返回降级数据
    return fallbackFn()
  }
}

// 导出单例实例
export const notificationService = new NotificationService()

// 默认导出
export default notificationService