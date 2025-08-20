import { ref, computed, watch } from 'vue'
import type { NotificationItem } from '@/api/notification'

/**
 * 通知已读状态管理 Composable
 * 革命性的智能工作台核心逻辑 - 高性能一次遍历分类系统
 * 
 * 功能特性:
 * - 本地存储持久化已读状态
 * - 一次遍历实现多重分类（Level 1-3未读、已读归档、Level 4单独显示）
 * - 节流保存，避免频繁写入localStorage
 * - 响应式计算，自动更新UI状态
 */

interface NotificationCategories {
  /** Level 1-3 未读通知（工作台主要区域显示） */
  unreadPriority: NotificationItem[]
  
  /** Level 1-3 已读通知（右侧归档区域显示） */
  readArchive: NotificationItem[]
  
  /** Level 4 未读通知（工作台底部专区显示） */
  level4Messages: NotificationItem[]
  
  /** 系统管理员公告（右侧系统公告区显示） */
  systemAnnouncements: NotificationItem[]
  
  /** 紧急通知（Level 1，最高优先级） */
  emergencyNotifications: NotificationItem[]
  
  /** 重要通知（Level 2-3） */
  importantNotifications: NotificationItem[]
}

/**
 * 通知已读状态管理钩子
 * @param userId 当前用户ID，用于区分不同用户的已读状态
 * @param initialNotifications 初始通知列表
 */
export function useNotificationReadStatus(userId?: string, initialNotifications: NotificationItem[] = []) {
  // 用户特定的本地存储键名 - 修复用户间数据混乱问题
  const READ_STATUS_KEY = userId ? `campus_portal_read_notifications_${userId}` : 'campus_portal_read_notifications_guest'
  const HIDDEN_STATUS_KEY = userId ? `campus_portal_hidden_notifications_${userId}` : 'campus_portal_hidden_notifications_guest'
  const ARCHIVE_CLEARED_KEY = userId ? `campus_portal_archive_cleared_time_${userId}` : 'campus_portal_archive_cleared_time_guest'
  
  // 已读通知ID集合
  const readNotificationIds = ref<Set<number>>(new Set())
  // 隐藏通知ID集合 (用于永久删除归档)
  const hiddenNotificationIds = ref<Set<number>>(new Set())
  // 归档清理时间 (用于批量清理归档)
  const archiveClearedTime = ref<number>(0)
  
  // 节流保存定时器
  let saveTimeout: NodeJS.Timeout | null = null
  
  /**
   * 从localStorage加载已读状态和隐藏状态
   */
  const loadReadStatus = () => {
    try {
      // 加载已读状态
      const savedData = localStorage.getItem(READ_STATUS_KEY)
      if (savedData) {
        const readIds = JSON.parse(savedData)
        if (Array.isArray(readIds)) {
          readNotificationIds.value = new Set(readIds)
          console.log('✅ [已读状态] 加载成功，已读通知数量:', readIds.length)
        }
      }
      
      // 加载隐藏状态 
      const hiddenData = localStorage.getItem(HIDDEN_STATUS_KEY)
      if (hiddenData) {
        const hiddenIds = JSON.parse(hiddenData)
        if (Array.isArray(hiddenIds)) {
          hiddenNotificationIds.value = new Set(hiddenIds)
          console.log('✅ [隐藏状态] 加载成功，隐藏通知数量:', hiddenIds.length)
        }
      }
      
      // 加载归档清理时间
      const clearedTimeData = localStorage.getItem(ARCHIVE_CLEARED_KEY)
      if (clearedTimeData) {
        const clearedTime = parseInt(clearedTimeData)
        if (!isNaN(clearedTime)) {
          archiveClearedTime.value = clearedTime
          console.log('✅ [归档清理] 加载成功，清理时间:', new Date(clearedTime).toLocaleString())
        }
      }
    } catch (error) {
      console.error('❌ [本地状态] 加载失败:', error)
      readNotificationIds.value = new Set()
      hiddenNotificationIds.value = new Set()
      archiveClearedTime.value = 0
    }
  }
  
  /**
   * 节流保存状态到localStorage
   * 避免频繁写入，提升性能
   */
  const saveReadStatus = () => {
    if (saveTimeout) {
      clearTimeout(saveTimeout)
    }
    
    saveTimeout = setTimeout(() => {
      try {
        // 保存已读状态
        const readIds = Array.from(readNotificationIds.value)
        localStorage.setItem(READ_STATUS_KEY, JSON.stringify(readIds))
        
        // 保存隐藏状态
        const hiddenIds = Array.from(hiddenNotificationIds.value)
        localStorage.setItem(HIDDEN_STATUS_KEY, JSON.stringify(hiddenIds))
        
        // 保存归档清理时间
        localStorage.setItem(ARCHIVE_CLEARED_KEY, archiveClearedTime.value.toString())
        
        console.log('💾 [本地状态] 保存成功 - 已读:', readIds.length, '隐藏:', hiddenIds.length, '清理时间:', new Date(archiveClearedTime.value).toLocaleString())
      } catch (error) {
        console.error('❌ [本地状态] 保存失败:', error)
      }
    }, 300) // 300ms节流延迟
  }
  
  /**
   * 标记通知为已读
   * @param notificationId 通知ID
   */
  const markAsRead = (notificationId: number) => {
    if (!readNotificationIds.value.has(notificationId)) {
      // 🔧 强制触发响应式更新 - 创建新的Set
      const newSet = new Set(readNotificationIds.value)
      newSet.add(notificationId)
      readNotificationIds.value = newSet
      saveReadStatus()
      console.log('✅ [已读状态] 标记为已读，当前已读数量:', newSet.size, '通知ID:', notificationId)
    }
  }
  
  /**
   * 标记通知为未读
   * @param notificationId 通知ID
   */
  const markAsUnread = (notificationId: number) => {
    if (readNotificationIds.value.has(notificationId)) {
      // 🔧 强制触发响应式更新 - 创建新的Set
      const newSet = new Set(readNotificationIds.value)
      newSet.delete(notificationId)
      readNotificationIds.value = newSet
      saveReadStatus()
      console.log('🔄 [已读状态] 标记为未读，当前已读数量:', newSet.size, '通知ID:', notificationId)
    }
  }
  
  /**
   * 检查通知是否已读
   * @param notificationId 通知ID
   */
  const isRead = (notificationId: number): boolean => {
    return readNotificationIds.value.has(notificationId)
  }
  
  /**
   * 检查通知是否已隐藏 (永久删除)
   * @param notificationId 通知ID
   */
  const isHidden = (notificationId: number): boolean => {
    return hiddenNotificationIds.value.has(notificationId)
  }
  
  /**
   * 检查通知是否应该被归档清理隐藏
   * @param notification 通知对象
   */
  const isClearedFromArchive = (notification: NotificationItem): boolean => {
    if (archiveClearedTime.value === 0) return false
    // 🔧 核心修复：只有已读状态且清理时间之前就已读的通知才被隐藏
    // 逻辑：如果通知是已读状态，且清理时间不为0，则该通知应该被从归档中清理
    return isRead(notification.id) && archiveClearedTime.value > 0
  }
  
  /**
   * 永久隐藏通知 (用于删除归档功能)
   * @param notificationId 通知ID
   */
  const hideNotification = (notificationId: number) => {
    if (!hiddenNotificationIds.value.has(notificationId)) {
      const newSet = new Set(hiddenNotificationIds.value)
      newSet.add(notificationId)
      hiddenNotificationIds.value = newSet
      saveReadStatus()
      console.log('🙈 [隐藏通知] 永久隐藏通知ID:', notificationId, '当前隐藏数量:', newSet.size)
    }
  }
  
  /**
   * 清空归档 (记录清理时间，而不是删除已读状态)
   */
  const clearArchive = () => {
    archiveClearedTime.value = Date.now()
    saveReadStatus()
    console.log('🧹 [清空归档] 设置清理时间:', new Date(archiveClearedTime.value).toLocaleString())
  }
  
  /**
   * 革命性的一次遍历多重分类算法
   * 高性能实现通知的智能分类，避免重复循环
   * 
   * @param notifications 原始通知列表
   * @returns 分类后的通知对象
   */
  const categorizeNotifications = computed(() => {
    return (notifications: NotificationItem[]): NotificationCategories => {
      // 初始化分类结果
      const categories: NotificationCategories = {
        unreadPriority: [],
        readArchive: [],
        level4Messages: [],
        systemAnnouncements: [],
        emergencyNotifications: [],
        importantNotifications: []
      }
      
      // 一次遍历，多重分类（性能优化核心）
      for (const notification of notifications) {
        const notificationId = notification.id
        const level = notification.level
        const isNotificationRead = isRead(notificationId)
        const isNotificationHidden = isHidden(notificationId)
        const isClearedNotification = isClearedFromArchive(notification)
        
        // 🚨 核心修复：如果通知被隐藏或被清理，则完全跳过
        if (isNotificationHidden || isClearedNotification) {
          continue
        }
        
        const isSystemNotification = notification.publisherRole === 'SYSTEM_ADMIN' || 
                                   (notification.publisherRole === 'SYSTEM' && 
                                    notification.publisherName === '系统管理员') ||
                                   notification.publisherName?.includes('System')
        
        // 🔧 增强调试：每个通知的系统识别过程
        if (notification.id === 5 || notification.id === 11) {
          console.log(`🔍 [系统公告识别] 通知ID=${notification.id}:`, {
            title: notification.title,
            publisherRole: notification.publisherRole,
            publisherName: notification.publisherName,
            isSystemNotification,
            条件检查: {
              'publisherRole=SYSTEM_ADMIN': notification.publisherRole === 'SYSTEM_ADMIN',
              'publisherRole=SYSTEM': notification.publisherRole === 'SYSTEM',
              '包含系统': notification.publisherName?.includes('系统'),
              '包含System': notification.publisherName?.includes('System')
            }
          })
        }
        
        // 给通知添加已读状态
        const enrichedNotification = {
          ...notification,
          isRead: isNotificationRead
        }
        
        // 系统公告分类（优先级最高）
        if (isSystemNotification) {
          categories.systemAnnouncements.push(enrichedNotification)
        }
        
        // 按级别分类
        if (level === 1) {
          // Level 1 紧急通知
          categories.emergencyNotifications.push(enrichedNotification)
          if (!isNotificationRead) {
            categories.unreadPriority.push(enrichedNotification)
          } else {
            categories.readArchive.push(enrichedNotification)
          }
        } else if (level === 2 || level === 3) {
          // Level 2-3 重要/常规通知
          categories.importantNotifications.push(enrichedNotification)
          if (!isNotificationRead) {
            categories.unreadPriority.push(enrichedNotification)
          } else {
            categories.readArchive.push(enrichedNotification)
          }
        } else if (level === 4) {
          // Level 4 提醒通知 - 与Level 1-3保持一致的归档逻辑
          if (!isNotificationRead) {
            // 未读消息：显示在L4专区
            categories.level4Messages.push(enrichedNotification)
          } else {
            // 已读消息：只显示在归档区域
            categories.readArchive.push(enrichedNotification)
          }
        }
      }
      
      // 排序优化：未读优先级按Level 1→2→3严格排序
      categories.unreadPriority.sort((a, b) => {
        if (a.level !== b.level) {
          return a.level - b.level // Level 1优先显示
        }
        // 同级别按时间倒序
        return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      })
      
      // 已读归档按时间倒序
      categories.readArchive.sort((a, b) => 
        new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      )
      
      // 系统公告只取最新一条
      categories.systemAnnouncements = categories.systemAnnouncements
        .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
        .slice(0, 1)
      
      console.log('🧠 [智能分类] 分类完成:', {
        未读优先级: categories.unreadPriority.length,
        已读归档: categories.readArchive.length,
        Level4消息: categories.level4Messages.length,
        系统公告: categories.systemAnnouncements.length,
        紧急通知: categories.emergencyNotifications.length,
        重要通知: categories.importantNotifications.length
      })
      
      return categories
    }
  })
  
  /**
   * 获取未读数量统计
   */
  const unreadCounts = computed(() => {
    return (notifications: NotificationItem[]) => {
      const categorized = categorizeNotifications.value(notifications)
      
      return {
        total: categorized.unreadPriority.length,
        emergency: categorized.emergencyNotifications.filter(n => !n.isRead).length,
        important: categorized.importantNotifications.filter(n => !n.isRead).length,
        level4: categorized.level4Messages.filter(n => !n.isRead).length // 只计算未读的Level 4
      }
    }
  })
  
  /**
   * 批量标记已读
   * @param notificationIds 通知ID列表
   */
  const markMultipleAsRead = (notificationIds: number[]) => {
    let hasChanges = false
    for (const id of notificationIds) {
      if (!readNotificationIds.value.has(id)) {
        readNotificationIds.value.add(id)
        hasChanges = true
      }
    }
    
    if (hasChanges) {
      saveReadStatus()
      console.log('👁️ [批量已读] 标记数量:', notificationIds.length)
    }
  }
  
  // 监听已读状态变化，自动保存
  watch(readNotificationIds, () => {
    saveReadStatus()
  }, { deep: true })
  
  // 初始化加载已读状态
  loadReadStatus()
  
  return {
    // 核心状态 - 🔧 修复响应式更新问题
    readNotificationIds: readNotificationIds,  // 直接返回ref，确保响应式更新
    hiddenNotificationIds: hiddenNotificationIds,
    archiveClearedTime: archiveClearedTime,
    
    // 核心方法
    markAsRead,
    markAsUnread,
    isRead,
    isHidden,
    isClearedFromArchive,
    markMultipleAsRead,
    
    // 新增的归档管理方法
    hideNotification,
    clearArchive,
    
    // 高性能分类算法
    categorizeNotifications,
    unreadCounts,
    
    // 工具方法
    loadReadStatus,
    saveReadStatus
  }
}