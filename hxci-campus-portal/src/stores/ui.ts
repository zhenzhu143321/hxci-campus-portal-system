/**
 * UI状态管理Store
 * 
 * @description 管理所有UI相关的状态，包括对话框、筛选器、调试面板等
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21  
 * @stage Stage 7 - Pinia状态管理架构建立
 */

import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import type { NotificationItem } from '@/api/notification'

export const useUIStore = defineStore('ui', () => {
  // ================== 对话框状态管理 ==================
  
  /** 显示全部通知对话框 */
  const showAllNotifications = ref<boolean>(false)
  
  /** 显示通知详情对话框 */
  const showNotificationDetail = ref<boolean>(false)
  
  /** 当前选中的通知（用于详情显示） */
  const selectedNotification = ref<NotificationItem | null>(null)
  
  // ================== 页面状态管理 ==================
  
  /** 调试面板显示状态 */
  const showDebugPanel = ref<boolean>(true)
  
  /** 归档面板显示状态 */
  const showArchivePanel = ref<boolean>(false)
  
  // ================== 通知筛选状态 ==================
  
  /** 通知筛选器状态 */
  const notificationFilters = reactive({
    level: null as number | null,      // 按级别筛选
    scope: '' as string,               // 按范围筛选  
    search: '' as string,              // 搜索关键词
    category: null as number | null,   // 按分类筛选
    dateRange: null as [string, string] | null  // 按时间范围筛选
  })
  
  /** 通知分页状态 */
  const notificationPagination = reactive({
    currentPage: 1,
    pageSize: 20
  })
  
  // ================== 工作台状态管理 ==================
  
  /** 工作台展开/收起状态 */
  const workspaceExpanded = reactive({
    prioritySection: true,      // 优先通知区域
    courseSection: true,        // 课程安排区域  
    todoSection: true,          // 待办通知区域
    level4Section: true         // Level4消息区域
  })
  
  /** 工作台显示模式 */
  const workspaceDisplayMode = ref<'compact' | 'standard' | 'detailed'>('standard')
  
  // ================== 交互状态管理 ==================
  
  /** 测试加载状态 */
  const testLoading = reactive({
    health: false,
    verify: false,
    notification: false
  })
  
  /** 操作加载状态 */
  const operationLoading = reactive({
    markingRead: new Set<number>(),     // 正在标记已读的通知ID集合
    refreshing: false,                  // 正在刷新数据
    archiving: false                    // 正在归档操作
  })
  
  // ================== 操作方法 (Actions) ==================
  
  /** 打开通知详情对话框 */
  const openNotificationDetail = (notification: NotificationItem) => {
    console.log('📖 [UIStore] 打开通知详情:', notification.title)
    selectedNotification.value = notification
    showNotificationDetail.value = true
  }
  
  /** 关闭通知详情对话框 */
  const closeNotificationDetail = () => {
    console.log('❌ [UIStore] 关闭通知详情')
    showNotificationDetail.value = false
    selectedNotification.value = null
  }
  
  /** 打开全部通知对话框 */
  const openAllNotifications = () => {
    console.log('📋 [UIStore] 打开全部通知对话框')
    showAllNotifications.value = true
  }
  
  /** 关闭全部通知对话框 */
  const closeAllNotifications = () => {
    console.log('❌ [UIStore] 关闭全部通知对话框')
    showAllNotifications.value = false
  }
  
  /** 切换调试面板显示状态 */
  const toggleDebugPanel = () => {
    showDebugPanel.value = !showDebugPanel.value
    console.log('🔧 [UIStore] 调试面板状态:', showDebugPanel.value ? '显示' : '隐藏')
  }
  
  /** 切换归档面板显示状态 */
  const toggleArchivePanel = () => {
    showArchivePanel.value = !showArchivePanel.value
    console.log('📁 [UIStore] 归档面板状态:', showArchivePanel.value ? '显示' : '隐藏')
  }
  
  /** 重置通知筛选器 */
  const resetNotificationFilters = () => {
    console.log('🔄 [UIStore] 重置通知筛选器')
    notificationFilters.level = null
    notificationFilters.scope = ''
    notificationFilters.search = ''
    notificationFilters.category = null
    notificationFilters.dateRange = null
  }
  
  /** 设置筛选器 */
  const setNotificationFilter = (key: keyof typeof notificationFilters, value: any) => {
    console.log('🔍 [UIStore] 设置筛选器:', key, '=', value)
    ;(notificationFilters as any)[key] = value
  }
  
  /** 重置分页状态 */
  const resetPagination = () => {
    console.log('📄 [UIStore] 重置分页状态')
    notificationPagination.currentPage = 1
    notificationPagination.pageSize = 20
  }
  
  /** 设置分页状态 */
  const setPagination = (currentPage?: number, pageSize?: number) => {
    if (currentPage !== undefined) {
      notificationPagination.currentPage = currentPage
    }
    if (pageSize !== undefined) {
      notificationPagination.pageSize = pageSize
    }
    console.log('📄 [UIStore] 分页状态更新:', notificationPagination)
  }
  
  /** 切换工作台区域展开状态 */
  const toggleWorkspaceSection = (section: keyof typeof workspaceExpanded) => {
    workspaceExpanded[section] = !workspaceExpanded[section]
    console.log('📋 [UIStore] 工作台区域状态:', section, '=', workspaceExpanded[section] ? '展开' : '收起')
  }
  
  /** 设置工作台显示模式 */
  const setWorkspaceDisplayMode = (mode: 'compact' | 'standard' | 'detailed') => {
    console.log('🎨 [UIStore] 工作台显示模式:', workspaceDisplayMode.value, '->', mode)
    workspaceDisplayMode.value = mode
  }
  
  /** 设置测试加载状态 */
  const setTestLoading = (test: keyof typeof testLoading, loading: boolean) => {
    testLoading[test] = loading
    console.log('🔧 [UIStore] 测试加载状态:', test, '=', loading)
  }
  
  /** 设置操作加载状态 */
  const setOperationLoading = (operation: 'refreshing' | 'archiving', loading: boolean) => {
    ;(operationLoading as any)[operation] = loading
    console.log('⚙️ [UIStore] 操作加载状态:', operation, '=', loading)
  }
  
  /** 添加标记已读加载状态 */
  const addMarkingReadLoading = (notificationId: number) => {
    operationLoading.markingRead.add(notificationId)
    console.log('⏳ [UIStore] 添加标记已读加载:', notificationId)
  }
  
  /** 移除标记已读加载状态 */
  const removeMarkingReadLoading = (notificationId: number) => {
    operationLoading.markingRead.delete(notificationId)
    console.log('✅ [UIStore] 移除标记已读加载:', notificationId)
  }
  
  /** 检查通知是否正在标记已读 */
  const isMarkingRead = (notificationId: number): boolean => {
    return operationLoading.markingRead.has(notificationId)
  }
  
  /** 重置所有UI状态 */
  const resetAllUIState = () => {
    console.log('🔄 [UIStore] 重置所有UI状态')
    
    // 关闭所有对话框
    showAllNotifications.value = false
    showNotificationDetail.value = false
    selectedNotification.value = null
    
    // 重置筛选器
    resetNotificationFilters()
    
    // 重置分页
    resetPagination()
    
    // 重置加载状态
    testLoading.health = false
    testLoading.verify = false
    testLoading.notification = false
    operationLoading.markingRead.clear()
    operationLoading.refreshing = false
    operationLoading.archiving = false
    
    console.log('✅ [UIStore] UI状态重置完成')
  }
  
  // ================== 导出 ==================
  
  return {
    // 对话框状态
    showAllNotifications,
    showNotificationDetail,
    selectedNotification,
    
    // 页面状态
    showDebugPanel,
    showArchivePanel,
    
    // 筛选和分页状态
    notificationFilters,
    notificationPagination,
    
    // 工作台状态
    workspaceExpanded,
    workspaceDisplayMode,
    
    // 交互状态
    testLoading,
    operationLoading,
    
    // 操作方法 - 对话框管理
    openNotificationDetail,
    closeNotificationDetail,
    openAllNotifications,
    closeAllNotifications,
    
    // 操作方法 - 面板管理
    toggleDebugPanel,
    toggleArchivePanel,
    
    // 操作方法 - 筛选器管理
    resetNotificationFilters,
    setNotificationFilter,
    
    // 操作方法 - 分页管理
    resetPagination,
    setPagination,
    
    // 操作方法 - 工作台管理
    toggleWorkspaceSection,
    setWorkspaceDisplayMode,
    
    // 操作方法 - 加载状态管理
    setTestLoading,
    setOperationLoading,
    addMarkingReadLoading,
    removeMarkingReadLoading,
    isMarkingRead,
    
    // 操作方法 - 全局状态管理
    resetAllUIState
  }
})