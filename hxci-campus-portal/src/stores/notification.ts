/**
 * 通知状态管理Store
 * 
 * @description 基于已完成的NotificationService，建立企业级通知状态管理架构
 * @author Frontend-Developer AI Assistant  
 * @date 2025-08-21
 * @stage Stage 7 - Pinia状态管理架构建立
 */

import { defineStore } from 'pinia'
import { ref, computed, watchEffect } from 'vue'
import type { NotificationItem } from '@/api/notification'
import notificationService from '@/services/notificationService'
import { useNotificationReadStatus } from '@/composables/useNotificationReadStatus'

export const useNotificationStore = defineStore('notification', () => {
  // ================== 状态定义 ==================
  
  /** 通知列表数据 */
  const notifications = ref<NotificationItem[]>([])
  
  /** 最近通知列表（用于向后兼容） */
  const recentNotifications = ref<NotificationItem[]>([])
  
  /** 数据加载状态 */
  const loading = ref<boolean>(false)
  
  /** 错误信息 */
  const error = ref<string | null>(null)
  
  /** 最后更新时间 */
  const lastUpdateTime = ref<Date | null>(null)
  
  /** 当前用户ID（用于已读状态管理） */
  const currentUserId = ref<number | null>(null)
  
  // ================== 已读状态管理器 - 使用watchEffect管理生命周期 ==================
  
  /** 已读状态管理器实例（响应式管理） */
  const readStatusManager = ref<ReturnType<typeof useNotificationReadStatus> | null>(null)
  
  /** 使用watchEffect管理readStatusManager的生命周期 */
  watchEffect(() => {
    if (currentUserId.value !== null) {
      readStatusManager.value = useNotificationReadStatus(String(currentUserId.value))
      console.log('🔧 [NotificationStore] 已读状态管理器初始化完成，用户ID:', currentUserId.value)
    } else {
      readStatusManager.value = null
      console.log('🗑️ [NotificationStore] 用户ID为空，已读状态管理器已清理')
    }
  })
  
  // ================== 计算属性 (Getters) ==================
  
  /** 未读优先级通知 (Level 1-3未读，按Level 1→2→3严格排序) */
  const unreadPriorityNotifications = computed(() => {
    // 🚀 核心修复：确保始终返回数组，防止undefined导致的length访问错误
    try {
      const manager = readStatusManager.value
      if (!manager || !notifications.value || !Array.isArray(notifications.value)) {
        console.log('⚠️ [NotificationStore] 管理器或通知数据未就绪，返回空数组')
        return []
      }
      
      // 🔧 防御性编程：安全访问readNotificationIds
      const readIds = manager.readNotificationIds
      if (!readIds) {
        console.warn('⚠️ [NotificationStore] readNotificationIds未初始化，所有Level1-3通知视为未读')
        // 如果没有已读状态，返回所有Level 1-3通知
        const allPriority = notifications.value.filter(n => n && n.level >= 1 && n.level <= 3) || []
        return allPriority.sort((a, b) => {
          if (a.level !== b.level) {
            return a.level - b.level
          }
          return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
        })
      }
      
      // 🔧 防御性访问readIds.value.size - 修正：readIds是ref，需要.value访问
      let readCount = 0
      try {
        readCount = readIds.value.size || 0
      } catch (sizeError) {
        console.error('❌ [NotificationStore] 访问readIds.size失败:', sizeError)
        readCount = 0
      }
      
      // 🔧 明确过滤：只包含Level 1-3，严格排除Level 4
      const unreadPriority = notifications.value
        .filter(n => {
          if (!n) return false
          
          const levelValid = n.level >= 1 && n.level <= 3
          const isUnread = !manager.isRead(n.id)
          
          // 🔧 调试Level 4误入情况
          if (n.level === 4) {
            console.log(`🚫 [优先级过滤] Level 4通知被正确排除: ID=${n.id}, 标题="${n.title}"`)
            return false
          }
          
          if (levelValid && isUnread) {
            console.log(`✅ [优先级包含] 通知ID=${n.id}, Level=${n.level}, 标题="${n.title}"`)
          }
          
          return levelValid && isUnread
        })
        .sort((a, b) => {
          // Level 1→2→3排序，同级别按时间倒序
          if (a.level !== b.level) {
            return a.level - b.level
          }
          return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
        })
      
      console.log('🔍 [NotificationStore] 未读优先级计算:', {
        已读ID数量: readCount,
        未读优先级数量: unreadPriority.length,
        Level分布: {
          Level1: unreadPriority.filter(n => n.level === 1).length,
          Level2: unreadPriority.filter(n => n.level === 2).length,
          Level3: unreadPriority.filter(n => n.level === 3).length
        },
        全部通知数量: notifications.value.length
      })
      
      return unreadPriority || []
    } catch (error) {
      console.error('❌ [NotificationStore] 未读优先级计算异常:', error)
      return []
    }
  })
  
  /** 系统公告 (publisherRole为SYSTEM_ADMIN或SYSTEM的通知) */
  const systemAnnouncements = computed(() => {
    // 🚀 最外层错误边界：防止任何异常影响Vue响应式系统
    try {
      // 🔧 立即返回空数组的安全条件检查
      if (!notifications.value || !Array.isArray(notifications.value)) {
        console.log('⚠️ [NotificationStore] 系统公告-通知数据未就绪，返回空数组')
        return []
      }
      
      const manager = readStatusManager.value
      if (!manager) {
        console.log('⚠️ [NotificationStore] 系统公告-管理器未就绪，返回空数组')
        return []
      }
      
      // 🔧 防御性编程：安全访问readNotificationIds
      const readIds = manager.readNotificationIds
      if (!readIds) {
        console.warn('⚠️ [NotificationStore] readNotificationIds未初始化，使用默认值')
        // 仍然可以计算公告，只是不依赖已读状态
      }
      
      const announcements = notifications.value.filter(n => {
        if (!n) return false
        
        // 🔧 核心修复: 系统公告不能包含Level 4通知
        // Level 4通知只应该在NotificationMessagesWidget显示
        if (n.level === 4) {
          console.log(`🚫 [系统公告过滤] Level 4通知被排除: ID=${n.id}, 标题="${n.title}"`)
          return false
        }
        
        const isSystemNotification = n.publisherRole === 'SYSTEM_ADMIN' || n.publisherRole === 'SYSTEM'
        
        if (isSystemNotification) {
          console.log(`✅ [系统公告] 通知ID=${n.id}, Level=${n.level}, 发布者="${n.publisherName}", 角色="${n.publisherRole}"`)
        }
        
        return isSystemNotification
      })
      
      // 🔧 核心修复：安全访问size属性，防止undefined错误 
      let readCount = 0
      try {
        // 🚀 直接访问修复：避免复杂类型推导问题
        if (readIds && readIds.value instanceof Set) {
          readCount = readIds.value.size
        } else {
          readCount = 0
        }
      } catch (sizeError) {
        console.error('❌ [NotificationStore] 系统公告-访问readIds.size失败:', sizeError)
        readCount = 0
      }
      
      console.log('🔍 [NotificationStore] 系统公告计算:', {
        系统公告数量: announcements.length,
        已读ID数量: readCount,
        全部通知数量: notifications.value.length,
        Level分布: {
          Level1: announcements.filter(n => n.level === 1).length,
          Level2: announcements.filter(n => n.level === 2).length,
          Level3: announcements.filter(n => n.level === 3).length,
          Level4: announcements.filter(n => n.level === 4).length // 应该为0
        }
      })
      
      return announcements || []
    } catch (error) {
      console.error('❌ [NotificationStore] 系统公告计算异常:', error)
      return []
    }
  })
  
  /** 已读归档通知 (右侧归档区域显示) */
  const readArchivedNotifications = computed(() => {
    // 🚀 最外层错误边界：防止任何异常影响Vue响应式系统
    try {
      // 🔧 立即返回空数组的安全条件检查
      if (!notifications.value || !Array.isArray(notifications.value)) {
        console.log('⚠️ [NotificationStore] 已读归档-通知数据未就绪，返回空数组')
        return []
      }
      
      const manager = readStatusManager.value
      if (!manager) {
        console.log('⚠️ [NotificationStore] 已读归档-管理器未就绪，返回空数组')
        return []
      }
      
      // 🔧 防御性编程：安全访问readNotificationIds
      const readIds = manager.readNotificationIds
      if (!readIds) {
        console.warn('⚠️ [NotificationStore] readNotificationIds未初始化，返回空归档列表')
        return []
      }
      
      const archivedNotifications = notifications.value
        // 🔧 P0级修复：增加isClearedFromArchive过滤，排除已清空的归档通知
        .filter(n => n && manager.isRead(n.id) && !manager.isClearedFromArchive(n))
        .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
      
      // 🔧 核心修复：安全访问size属性，防止undefined错误
      let readCount = 0
      try {
        // 🚀 直接访问修复：避免复杂类型推导问题
        if (readIds && readIds.value instanceof Set) {
          readCount = readIds.value.size
        } else {
          readCount = 0
        }
      } catch (sizeError) {
        console.error('❌ [NotificationStore] 已读归档-访问readIds.size失败:', sizeError)
        readCount = 0
      }
      
      console.log('🔍 [NotificationStore] 已读归档计算:', {
        已读ID数量: readCount,
        归档通知数量: archivedNotifications.length,
        清空过滤生效: true  // 新增日志
      })
      
      return archivedNotifications || []
    } catch (error) {
      console.error('❌ [NotificationStore] 已读归档计算异常:', error)
      return []
    }
  })
  
  /** Level 4 通知消息 (工作台底部专区显示) - 🚨 核心修复：只显示未读通知 */
  const level4Messages = computed(() => {
    // 🚀 最外层错误边界：防止任何异常影响Vue响应式系统
    try {
      // 🔧 立即返回空数组的安全条件检查
      if (!notifications.value || !Array.isArray(notifications.value)) {
        console.log('⚠️ [NotificationStore] Level4-通知数据未就绪，返回空数组')
        return []
      }
      
      const manager = readStatusManager.value
      if (!manager) {
        console.log('⚠️ [NotificationStore] Level4-管理器未就绪，显示所有Level4通知')
        // 如果管理器未就绪，显示所有Level4通知作为未读处理
        const allLevel4 = notifications.value.filter(n => n && n.level === 4) || []
        return allLevel4.slice(0, 4)
      }
      
      // 🔧 核心修复：安全访问readNotificationIds，避免TypeScript类型错误
      const readIds = manager.readNotificationIds
      if (!readIds) {
        console.warn('⚠️ [NotificationStore] readNotificationIds未初始化，所有Level4作为未读')
        const allLevel4 = notifications.value.filter(n => n && n.level === 4) || []
        return allLevel4.slice(0, 4)
      }
      
      console.log('🔍 [Level4Messages] 计算开始 - 🚨 只显示未读通知:', {
        通知总数: notifications.value.length,
        管理器状态: '已就绪'
      })
      
      // 🚀 核心修复：💬通知消息区域只显示未读的Level 4通知
      const allLevel4 = notifications.value.filter(n => n && n.level === 4) || []
      if (!Array.isArray(allLevel4)) {
        console.error('❌ [NotificationStore] allLevel4不是数组:', typeof allLevel4)
        return []
      }
      
      // 🔧 核心逻辑修复：严格过滤未读通知
      const unreadLevel4 = allLevel4.filter(n => {
        const isRead = manager.isRead(n.id)
        const shouldShow = !isRead // 只显示未读通知
        
        if (shouldShow) {
          console.log(`📋 [Level4未读显示] 通知ID=${n.id}, 标题="${n.title}"`)
        } else {
          console.log(`🚫 [Level4已读隐藏] 通知ID=${n.id}, 标题="${n.title}", 已读通知不在消息区域显示`)
        }
        
        return shouldShow
      })
      
      // 按时间倒序排列（最新的在前）
      const sortedUnreadLevel4 = unreadLevel4.sort((a, b) => 
        new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      )
      
      // 限制显示数量
      const displayLevel4 = sortedUnreadLevel4.slice(0, 4)
      
      console.log('🔍 [NotificationStore] Level4消息计算完成 - 🚨 严格未读过滤:', {
        Level4总数: allLevel4.length,
        未读Level4数量: displayLevel4.length,
        显示策略: '仅未读通知',
        已读通知处理: '完全隐藏',
        修复状态: '已读通知卡片嵌套问题已解决'
      })
      
      return displayLevel4 || []
    } catch (error) {
      console.error('❌ [NotificationStore] Level4消息计算异常:', error)
      return []
    }
  })
  
  /** 紧急通知 (Level 1) */
  const emergencyNotifications = computed(() => {
    return notifications.value
      .filter(n => n.level === 1)
      .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
  })
  
  /** 重要通知 (Level 2-3) */
  const importantNotifications = computed(() => {
    return notifications.value
      .filter(n => n.level >= 2 && n.level <= 3)
      .sort((a, b) => {
        if (a.level !== b.level) {
          return a.level - b.level
        }
        return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      })
  })
  
  /** 未读数量统计 */
  const unreadStats = computed(() => {
    // 🚀 最外层错误边界：防止任何异常影响Vue响应式系统
    try {
      // 🔧 立即返回默认统计的安全条件检查
      if (!notifications.value || !Array.isArray(notifications.value)) {
        console.log('⚠️ [NotificationStore] 未读统计-通知数据未就绪，返回零统计')
        return { total: 0, emergency: 0, important: 0, level4: 0 }
      }
      
      const manager = readStatusManager.value
      if (!manager) {
        console.log('⚠️ [NotificationStore] 未读统计-管理器未就绪，返回零统计')
        return { total: 0, emergency: 0, important: 0, level4: 0 }
      }
      
      // 🔧 防御性编程：安全访问readNotificationIds，防止size访问错误
      const readIds = manager.readNotificationIds
      if (!readIds) {
        console.warn('⚠️ [NotificationStore] readNotificationIds未初始化，无法统计未读数量')
        // 如果没有已读状态，所有通知都算未读
        const allNotifications = notifications.value.filter(n => n)
        return {
          total: allNotifications.length,
          emergency: allNotifications.filter(n => n && n.level === 1).length,
          important: allNotifications.filter(n => n && n.level >= 2 && n.level <= 3).length,
          level4: allNotifications.filter(n => n && n.level === 4).length
        }
      }
      
      // 🔧 防御性访问size属性，增加多层安全检查
      let readCount = 0
      try {
        // 🚀 直接访问修复：避免复杂类型推导问题
        if (readIds && readIds.value instanceof Set) {
          readCount = readIds.value.size
        } else {
          console.warn('⚠️ [NotificationStore] readIds.value不是Set对象:', {
            readIds存在: !!readIds,
            readIds_value类型: readIds && readIds.value ? readIds.value.constructor.name : 'null/undefined'
          })
          readCount = 0
        }
      } catch (sizeError) {
        console.error('❌ [NotificationStore] 未读统计-访问readIds.size失败:', sizeError)
        readCount = 0
      }
      
      console.debug('📊 [NotificationStore] 当前已读数量:', readCount)
      
      const unreadNotifications = notifications.value.filter(n => n && !manager.isRead(n.id))
      
      const stats = {
        total: unreadNotifications.length,
        emergency: unreadNotifications.filter(n => n && n.level === 1).length,
        important: unreadNotifications.filter(n => n && n.level >= 2 && n.level <= 3).length,
        level4: unreadNotifications.filter(n => n && n.level === 4).length
      }
      
      console.log('🔍 [NotificationStore] 未读统计计算:', stats)
      
      return stats
    } catch (error) {
      console.error('❌ [NotificationStore] 未读统计计算异常:', error)
      return { total: 0, emergency: 0, important: 0, level4: 0 }
    }
  })
  
  /** 按级别分类的通知 */
  const notificationsByLevel = computed(() => {
    return {
      level1: notifications.value.filter(n => n.level === 1),
      level2: notifications.value.filter(n => n.level === 2),
      level3: notifications.value.filter(n => n.level === 3),
      level4: notifications.value.filter(n => n.level === 4)
    }
  })
  
  // ================== 操作方法 (Actions) ==================
  
  /** 设置当前用户ID（必须在获取通知前调用） */
  const setCurrentUserId = (userId: number | null) => {
    console.log('👤 [NotificationStore] 设置当前用户ID:', userId)
    currentUserId.value = userId
    
    // readStatusManager的生命周期应该由useNotificationReadStatus composable管理
    // NotificationStore只负责自己的状态，不应该干预readStatusManager
    
    // 如果设置为null，表示用户退出登录，清理Store自身状态
    if (userId === null) {
      console.log('🔓 [NotificationStore] 用户退出，重置Store状态')
      notifications.value = []
      loading.value = false
      error.value = null
    }
  }
  
  /** 获取通知列表 - 基于NotificationService */
  const fetchNotifications = async (params?: {
    pageSize?: number
    level?: number
    scope?: string
    useCache?: boolean
  }) => {
    console.log('📢 [NotificationStore] 开始获取通知数据...')
    loading.value = true
    error.value = null
    
    try {
      // 使用已完成的NotificationService
      const result = await notificationService.getNotifications({
        pageSize: 100,
        ...params
      })
      
      notifications.value = result
      recentNotifications.value = result.slice(0, 3) // 兼容原有逻辑
      lastUpdateTime.value = new Date()
      
      // 🚀 Stage 9性能优化: 在后台预加载高优先级通知详情
      if (result.length > 0) {
        // 使用setTimeout让预加载在下个事件循环执行，不阻塞当前渲染
        setTimeout(() => {
          notificationService.preloadPriorityNotificationDetails(result)
        }, 100)
      }
      
      console.log('✅ [NotificationStore] 通知数据获取成功:', {
        总数量: notifications.value.length,
        未读优先级: unreadPriorityNotifications.value.length,
        系统公告: systemAnnouncements.value.length,
        Level4消息: level4Messages.value.length
      })
      
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取通知数据失败'
      console.error('❌ [NotificationStore] 获取通知数据失败:', error.value)
    } finally {
      loading.value = false
    }
  }
  
  /** 获取通知详情 */
  const getNotificationDetail = async (id: number) => {
    try {
      console.log('📖 [NotificationStore] 获取通知详情, ID:', id)
      return await notificationService.getNotificationDetail(id)
    } catch (err) {
      console.error('❌ [NotificationStore] 获取通知详情失败:', err)
      throw err
    }
  }
  
  /** 标记通知为已读 */
  const markAsRead = (notificationId: number) => {
    const manager = readStatusManager.value
    if (manager) {
      manager.markAsRead(notificationId)
      console.log('✅ [NotificationStore] 通知已标记为已读:', notificationId)

      // 🔧 P0级修复: 强制刷新所有计算属性，确保Level 4消息立即从列表中移除
      // 通过创建新数组引用触发响应式更新
      notifications.value = [...notifications.value]
      console.log('🔄 [NotificationStore] 强制触发响应式更新，Level4消息数量:', level4Messages.value.length)
      
    } else {
      console.warn('⚠️ [NotificationStore] 已读状态管理器未初始化')
    }
  }
  
  /** 标记通知为未读 */
  const markAsUnread = (notificationId: number) => {
    const manager = readStatusManager.value
    if (manager) {
      manager.markAsUnread(notificationId)
      console.log('🔄 [NotificationStore] 通知已标记为未读:', notificationId)
      
      // 🔧 P0级修复: 同样强制刷新响应式
      notifications.value = [...notifications.value]
      console.log('🔄 [NotificationStore] 强制触发响应式更新')
    }
  }
  
  /** 检查通知是否已读 */
  const isRead = (notificationId: number): boolean => {
    const manager = readStatusManager.value
    return manager ? manager.isRead(notificationId) : false
  }
  
  /** 隐藏通知（本地隐藏机制） */
  const hideNotification = (notificationId: number) => {
    const manager = readStatusManager.value
    if (manager) {
      manager.hideNotification(notificationId)
      console.log('🗑️ [NotificationStore] 通知已隐藏:', notificationId)
    }
  }
  
  /** 清空归档 */
  const clearArchive = () => {
    const manager = readStatusManager.value
    if (manager) {
      manager.clearArchive()
      console.log('🧹 [NotificationStore] 归档已清空')
    }
  }
  
  /** 刷新通知数据 - 强制从服务器获取 */
  const refreshNotifications = async () => {
    console.log('🔄 [NotificationStore] 刷新通知数据...')
    await fetchNotifications({ useCache: false })
  }
  
  /** 获取未读通知数量 */
  const getUnreadCount = async (): Promise<number> => {
    try {
      return await notificationService.getUnreadNotificationsCount()
    } catch (err) {
      console.error('❌ [NotificationStore] 获取未读数量失败:', err)
      return 0
    }
  }
  
  /** 获取优先级通知 */
  const getPriorityNotifications = async (): Promise<NotificationItem[]> => {
    try {
      return await notificationService.getPriorityNotifications()
    } catch (err) {
      console.error('❌ [NotificationStore] 获取优先级通知失败:', err)
      return []
    }
  }
  
  /** 清除错误信息 */
  const clearError = () => {
    error.value = null
  }
  
  // ================== 导出 ==================
  
  return {
    // 状态
    notifications,
    recentNotifications,
    loading,
    error,
    lastUpdateTime,
    currentUserId,
    
    // 计算属性
    unreadPriorityNotifications,
    systemAnnouncements,
    readArchivedNotifications,
    level4Messages,
    emergencyNotifications,
    importantNotifications,
    unreadStats,
    notificationsByLevel,
    
    // 操作方法
    setCurrentUserId,
    fetchNotifications,
    getNotificationDetail,
    markAsRead,
    markAsUnread,
    isRead,
    hideNotification,
    clearArchive,
    refreshNotifications,
    getUnreadCount,
    getPriorityNotifications,
    clearError,
    
    // 内部状态（响应式管理器）
    readStatusManager
  }
})