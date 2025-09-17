/**
 * 通知处理器组合式函数
 * 
 * @description 提取和复用通知相关的处理逻辑，包括标记已读、删除、归档等操作
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import { useUIStore } from '@/stores/ui'
import { useNotifications } from '@/composables/useNotifications'
import { useNotificationArchiveAnimation } from '@/composables/useNotificationArchiveAnimation'
import { performanceAnalyzer } from '@/utils/performanceAnalyzer'
import type { NotificationItem } from '@/api/notification'

export function useNotificationHandlers() {
  // Store引用
  const notificationStore = useNotificationStore()
  const uiStore = useUIStore()
  const notifications = useNotifications()
  
  // 动画管理器
  let archiveAnimationManager: ReturnType<typeof useNotificationArchiveAnimation> | null = null
  
  // 状态
  const markingReadLoading = ref<Set<number>>(new Set())
  const deletingLoading = ref<Set<number>>(new Set())
  
  // 初始化动画管理器
  const initializeArchiveAnimationManager = () => {
    if (!archiveAnimationManager) {
      archiveAnimationManager = useNotificationArchiveAnimation()
    }
    return archiveAnimationManager
  }
  
  /**
   * 标记通知为已读
   */
  const handleMarkAsRead = async (notificationId: number) => {
    const endTimer = performanceAnalyzer.startTimer(`标记已读-${notificationId}`)
    
    console.log('📖 [通知处理] 开始标记已读:', notificationId)
    
    const animationManager = initializeArchiveAnimationManager()
    
    // 添加加载状态
    markingReadLoading.value.add(notificationId)
    uiStore.addMarkingReadLoading(notificationId)
    
    try {
      // 使用动画效果标记已读
      if (animationManager) {
        await animationManager.animateToArchive(notificationId)
      }
      
      // 标记已读
      notifications.markRead(notificationId)
      
      // 关闭详情对话框（如果打开）
      if (uiStore.selectedNotification?.id === notificationId) {
        uiStore.closeNotificationDetail()
      }
      
      ElMessage.success('已标记为已读')
      console.log('✅ [通知处理] 标记已读成功:', notificationId)
      
    } catch (error) {
      console.error('❌ [通知处理] 标记已读失败:', error)
      ElMessage.error('标记已读失败')
    } finally {
      markingReadLoading.value.delete(notificationId)
      uiStore.removeMarkingReadLoading(notificationId)
      endTimer()
    }
  }
  
  /**
   * 撤销已读状态
   */
  const handleMarkAsUnread = (notificationId: number) => {
    console.log('🔄 [通知处理] 撤销已读状态:', notificationId)
    
    try {
      notifications.markUnread(notificationId)
      ElMessage.info('已撤销已读状态')
      console.log('✅ [通知处理] 撤销已读成功:', notificationId)
    } catch (error) {
      console.error('❌ [通知处理] 撤销已读失败:', error)
      ElMessage.error('撤销已读失败')
    }
  }
  
  /**
   * 永久删除通知
   */
  const handlePermanentDelete = async (notificationId: number) => {
    console.log('🗑️ [通知处理] 准备永久删除:', notificationId)
    
    try {
      await ElMessageBox.confirm(
        '确定要永久删除这条通知吗？此操作不可恢复。',
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      )
      
      deletingLoading.value.add(notificationId)
      
      // 执行删除
      notificationStore.hideNotification(notificationId)
      
      ElMessage.success('已永久删除通知')
      console.log('✅ [通知处理] 永久删除成功:', notificationId)
      
    } catch (error) {
      if (error !== 'cancel') {
        console.error('❌ [通知处理] 永久删除失败:', error)
        ElMessage.error('删除失败')
      }
    } finally {
      deletingLoading.value.delete(notificationId)
    }
  }
  
  /**
   * 清空所有归档
   */
  const handleClearAllArchive = async () => {
    console.log('🧹 [通知处理] 准备清空所有归档')
    
    try {
      const archivedCount = notificationStore.readArchivedNotifications.length
      
      if (archivedCount === 0) {
        ElMessage.info('归档区域已经为空')
        return
      }
      
      await ElMessageBox.confirm(
        `确定要清空所有已读归档消息吗？共有 ${archivedCount} 条归档消息。已读状态会保留，但归档区域将被清空。`,
        '清空归档确认',
        {
          confirmButtonText: '确定清空',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--warning'
        }
      )
      
      // 执行清空
      notificationStore.clearArchive()
      
      ElMessage.success(`已清空 ${archivedCount} 条归档消息`)
      console.log('✅ [通知处理] 清空归档成功，数量:', archivedCount)
      
    } catch (error) {
      if (error !== 'cancel') {
        console.error('❌ [通知处理] 清空归档失败:', error)
        ElMessage.error('清空归档失败')
      }
    }
  }
  
  /**
   * 处理通知点击
   */
  const handleNotificationClick = async (
    notification: NotificationItem, 
    autoMarkRead: boolean = false
  ) => {
    console.log('👆 [通知处理] 点击通知:', notification.title)
    
    try {
      // 获取通知详情
      const detail = await notificationStore.getNotificationDetail(notification.id).catch(() => null)
      
      // 打开详情对话框
      uiStore.openNotificationDetail(detail || notification)
      
      // 自动标记已读
      if (autoMarkRead && !notificationStore.isRead(notification.id)) {
        await handleMarkAsRead(notification.id)
      }
      
      console.log('✅ [通知处理] 打开通知详情成功')
      
    } catch (error) {
      console.error('❌ [通知处理] 打开通知详情失败:', error)
      // 即使出错也尝试打开对话框
      uiStore.openNotificationDetail(notification)
    }
  }
  
  /**
   * 处理紧急通知点击
   */
  const handleEmergencyClick = (notification: NotificationItem) => {
    console.log('🚨 [通知处理] 点击紧急通知:', notification.title)
    handleNotificationClick(notification, true) // 自动标记已读
  }
  
  /**
   * 批量标记已读
   */
  const handleBatchMarkAsRead = async (notificationIds: number[]) => {
    console.log('📚 [通知处理] 批量标记已读:', notificationIds.length, '条')
    
    const loadingMessage = ElMessage.info({
      message: `正在标记 ${notificationIds.length} 条通知为已读...`,
      duration: 0
    })
    
    try {
      // 批量标记
      await Promise.all(
        notificationIds.map(id => {
          notifications.markRead(id)
          return Promise.resolve()
        })
      )
      
      loadingMessage.close()
      ElMessage.success(`已标记 ${notificationIds.length} 条通知为已读`)
      console.log('✅ [通知处理] 批量标记已读成功')
      
    } catch (error) {
      loadingMessage.close()
      console.error('❌ [通知处理] 批量标记已读失败:', error)
      ElMessage.error('批量标记失败')
    }
  }
  
  /**
   * 标记全部已读
   */
  const handleMarkAllAsRead = async () => {
    console.log('📖 [通知处理] 准备标记全部已读')
    
    const unreadNotifications = notificationStore.unreadNotifications
    
    if (unreadNotifications.length === 0) {
      ElMessage.info('没有未读通知')
      return
    }
    
    try {
      await ElMessageBox.confirm(
        `确定要将所有 ${unreadNotifications.length} 条未读通知标记为已读吗？`,
        '标记全部已读',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info'
        }
      )
      
      const notificationIds = unreadNotifications.map(n => n.id)
      await handleBatchMarkAsRead(notificationIds)
      
    } catch (error) {
      if (error !== 'cancel') {
        console.error('❌ [通知处理] 标记全部已读失败:', error)
      }
    }
  }
  
  /**
   * 刷新通知数据
   */
  const refreshNotifications = async () => {
    console.log('🔄 [通知处理] 刷新通知数据')
    
    try {
      await notifications.refresh()
      ElMessage.success('通知已刷新')
      console.log('✅ [通知处理] 刷新成功')
    } catch (error) {
      console.error('❌ [通知处理] 刷新失败:', error)
      ElMessage.error('刷新失败，请稍后重试')
    }
  }
  
  // 计算属性
  const isMarkingRead = computed(() => (id: number) => markingReadLoading.value.has(id))
  const isDeleting = computed(() => (id: number) => deletingLoading.value.has(id))
  
  return {
    // 状态
    markingReadLoading,
    deletingLoading,
    isMarkingRead,
    isDeleting,
    
    // 处理函数
    handleMarkAsRead,
    handleMarkAsUnread,
    handlePermanentDelete,
    handleClearAllArchive,
    handleNotificationClick,
    handleEmergencyClick,
    handleBatchMarkAsRead,
    handleMarkAllAsRead,
    refreshNotifications,
    
    // 工具函数
    initializeArchiveAnimationManager
  }
}