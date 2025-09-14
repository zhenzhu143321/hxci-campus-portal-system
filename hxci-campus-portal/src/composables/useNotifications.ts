/**
 * 通知管理 Composable
 * 提供通知的UI层管理功能，作为Store和组件之间的桥梁
 */

import { computed, ref, watch, onMounted, type Ref, type ComputedRef } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import type { NotificationItem } from '@/api/notification'
import { formatDate } from '@/utils'
import { ElMessage } from 'element-plus'

// TypeScript 类型定义
export type NotificationScope = 'SCHOOL_WIDE' | 'DEPARTMENT' | 'GRADE' | 'CLASS' | string
export type NotificationSortBy = 'time_desc' | 'time_asc' | 'level_then_time' | 'publisher'

export interface NotificationFilter {
  level: 1 | 2 | 3 | 4 | null
  scope: NotificationScope | null
  readStatus: 'all' | 'unread' | 'read'
  search: string
  dateRange?: { start?: Date; end?: Date }
  sortBy: NotificationSortBy
  page: number
  pageSize: number
}

export interface UseNotificationsOptions {
  userId?: string | number
  autoFetch?: boolean
  pageSize?: number
  syncRouteQuery?: boolean
}

export interface UseNotifications {
  // 原始数据和分类数据 (从Store代理)
  notifications: ComputedRef<NotificationItem[]>
  loading: ComputedRef<boolean>
  error: ComputedRef<string | null>
  unreadStats: ComputedRef<{ total: number; emergency: number; important: number; level4: number }>
  unreadPriority: ComputedRef<NotificationItem[]>
  systemAnnouncements: ComputedRef<NotificationItem[]>
  readArchive: ComputedRef<NotificationItem[]>
  level4Messages: ComputedRef<NotificationItem[]>
  emergency: ComputedRef<NotificationItem[]>
  important: ComputedRef<NotificationItem[]>

  // UI状态
  filter: Ref<NotificationFilter>
  filtered: ComputedRef<NotificationItem[]>
  paged: ComputedRef<NotificationItem[]>
  totalPages: ComputedRef<number>
  selected: Ref<NotificationItem | null>
  showAllDialog: Ref<boolean>
  showDetailDialog: Ref<boolean>

  // 操作方法
  initialize(opts?: Partial<UseNotificationsOptions>): Promise<void>
  refresh(): Promise<void>
  openAll(): void
  closeAll(): void
  openDetail(item: NotificationItem): Promise<void>
  closeDetail(): void
  onClick(item: NotificationItem, autoMarkRead?: boolean): Promise<void>
  markRead(id: number): void
  markUnread(id: number): void
  markBulkRead(ids: number[]): void
  markFilteredAsRead(): void
  hide(id: number): void
  clearArchive(): void
  setFilter(patch: Partial<NotificationFilter>): void
  resetFilter(): void
  search(term: string): void
  nextPage(): void
  prevPage(): void
  goToPage(page: number): void

  // 辅助方法
  getLevelText(level: number): string
  getAnnouncementType(level: number): 'danger' | 'warning' | 'info' | 'success'
  getScopeText(scope: string): string
  getContentPreview(content: string, max?: number): string
  formatNotificationContent(content: string): string
}

export function useNotifications(options: UseNotificationsOptions = {}): UseNotifications {
  const store = useNotificationStore()

  // 过滤器状态
  const filter = ref<NotificationFilter>({
    level: null,
    scope: null,
    readStatus: 'all',
    search: '',
    sortBy: 'time_desc',
    page: 1,
    pageSize: options.pageSize ?? 20
  })

  // 绑定Store数据
  const notifications = computed(() => store.notifications)
  const loading = computed(() => store.loading)
  const error = computed(() => store.error)
  const unreadStats = computed(() => store.unreadStats)
  const unreadPriority = computed(() => store.unreadPriorityNotifications)
  const systemAnnouncements = computed(() => store.systemAnnouncements)
  const readArchive = computed(() => store.readArchivedNotifications)
  const level4Messages = computed(() => store.level4Messages)
  const emergency = computed(() => store.emergencyNotifications)
  const important = computed(() => store.importantNotifications)

  // 过滤管道
  const filtered = computed(() => {
    let list = notifications.value
    const f = filter.value

    // 级别过滤
    if (f.level !== null) {
      list = list.filter(n => n.level === f.level)
    }

    // 范围过滤
    if (f.scope) {
      list = list.filter(n => n.scope === f.scope)
    }

    // 已读状态过滤
    if (f.readStatus !== 'all') {
      list = list.filter(n => {
        const isRead = store.isRead(n.id)
        return f.readStatus === 'read' ? isRead : !isRead
      })
    }

    // 搜索过滤
    if (f.search) {
      const searchTerm = f.search.toLowerCase()
      list = list.filter(n =>
        n.title.toLowerCase().includes(searchTerm) ||
        n.content?.toLowerCase().includes(searchTerm) ||
        n.publisherName?.toLowerCase().includes(searchTerm)
      )
    }

    // 日期范围过滤
    if (f.dateRange?.start || f.dateRange?.end) {
      list = list.filter(n => {
        const date = new Date(n.createTime)
        if (f.dateRange?.start && date < f.dateRange.start) return false
        if (f.dateRange?.end && date > f.dateRange.end) return false
        return true
      })
    }

    // 排序
    list = [...list]
    switch (f.sortBy) {
      case 'level_then_time':
        list.sort((a, b) => {
          if (a.level !== b.level) return a.level - b.level
          return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
        })
        break
      case 'time_asc':
        list.sort((a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime())
        break
      case 'publisher':
        list.sort((a, b) => (a.publisherName || '').localeCompare(b.publisherName || ''))
        break
      default: // time_desc
        list.sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
    }

    return list
  })

  // 分页计算
  const totalPages = computed(() => {
    return Math.ceil(filtered.value.length / filter.value.pageSize)
  })

  const paged = computed(() => {
    const { page, pageSize } = filter.value
    const start = (page - 1) * pageSize
    return filtered.value.slice(start, start + pageSize)
  })

  // UI状态
  const selected = ref<NotificationItem | null>(null)
  const showAllDialog = ref(false)
  const showDetailDialog = ref(false)

  // 初始化方法
  const initialize = async (opts: Partial<UseNotificationsOptions> = {}) => {
    const userId = opts.userId ?? options.userId
    if (userId) {
      store.setCurrentUserId(String(userId))
    }

    if ((opts.autoFetch ?? options.autoFetch) !== false) {
      await store.fetchNotifications({ pageSize: 100 })
    }
  }

  // 刷新数据
  const refresh = async () => {
    await store.refreshNotifications()
  }

  // 对话框控制
  const openAll = () => {
    showAllDialog.value = true
  }

  const closeAll = () => {
    showAllDialog.value = false
  }

  const openDetail = async (item: NotificationItem) => {
    try {
      // 获取详情
      const detail = await store.getNotificationDetail(item.id)
      if (detail) {
        selected.value = detail
        showDetailDialog.value = true
      } else {
        ElMessage.error('获取通知详情失败')
      }
    } catch (error) {
      console.error('获取通知详情失败:', error)
      ElMessage.error('获取通知详情失败')
    }
  }

  const closeDetail = () => {
    showDetailDialog.value = false
    selected.value = null
  }

  // 点击通知
  const onClick = async (item: NotificationItem, autoMarkRead = false) => {
    await openDetail(item)
    if (autoMarkRead && !store.isRead(item.id)) {
      store.markAsRead(item.id)
    }
  }

  // 已读/未读操作
  const markRead = (id: number) => {
    store.markAsRead(id)
  }

  const markUnread = (id: number) => {
    store.markAsUnread(id)
  }

  const markBulkRead = (ids: number[]) => {
    ids.forEach(id => store.markAsRead(id))
  }

  const markFilteredAsRead = () => {
    const unreadIds = filtered.value.filter(n => !store.isRead(n.id)).map(n => n.id)
    markBulkRead(unreadIds)
    ElMessage.success(`已标记${unreadIds.length}条通知为已读`)
  }

  // 隐藏和归档操作
  const hide = (id: number) => {
    store.hideNotification(id)
  }

  const clearArchive = () => {
    store.clearArchive()
  }

  // 过滤器操作
  const setFilter = (patch: Partial<NotificationFilter>) => {
    filter.value = {
      ...filter.value,
      ...patch,
      page: 1 // 重置到第一页
    }
  }

  const resetFilter = () => {
    filter.value = {
      ...filter.value,
      level: null,
      scope: null,
      readStatus: 'all',
      search: '',
      sortBy: 'time_desc',
      page: 1,
      pageSize: filter.value.pageSize
    }
  }

  const search = (term: string) => {
    setFilter({ search: term })
  }

  // 分页操作
  const nextPage = () => {
    if (filter.value.page < totalPages.value) {
      filter.value.page++
    }
  }

  const prevPage = () => {
    if (filter.value.page > 1) {
      filter.value.page--
    }
  }

  const goToPage = (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      filter.value.page = page
    }
  }

  // 辅助方法
  const getLevelText = (level: number): string => {
    switch (level) {
      case 1: return '紧急'
      case 2: return '重要'
      case 3: return '常规'
      case 4: return '提醒'
      default: return '未知'
    }
  }

  const getAnnouncementType = (level: number): 'danger' | 'warning' | 'info' | 'success' => {
    switch (level) {
      case 1: return 'danger'  // 紧急
      case 2: return 'warning' // 重要
      case 3: return 'info'    // 常规
      case 4: return 'success' // 提醒
      default: return 'info'
    }
  }

  const getScopeText = (scope: string): string => {
    switch (scope) {
      case 'SCHOOL_WIDE': return '全校'
      case 'DEPARTMENT': return '部门'
      case 'GRADE': return '年级'
      case 'CLASS': return '班级'
      default: return scope
    }
  }

  const formatNotificationContent = (content: string): string => {
    if (!content) return ''
    // 将\n转换为实际换行符，处理各种换行格式
    return content
      .replace(/\\n/g, '\n')  // 转义的\n转为真换行
      .replace(/\n\s*\n/g, '\n\n')  // 规范化多重换行
      .replace(/^\s+|\s+$/g, '')  // 去除首尾空白
      .trim()
  }

  const getContentPreview = (content: string, max = 80): string => {
    const formatted = formatNotificationContent(content)
    const preview = formatted.replace(/\n{2,}/g, ' | ').replace(/\n/g, ' ')
    return preview.length > max ? preview.slice(0, max) + '...' : preview
  }

  // 监听用户变化
  watch(
    () => options.userId,
    (newUserId) => {
      if (newUserId) {
        initialize({ userId: newUserId })
      }
    }
  )

  // 组件挂载时初始化
  onMounted(async () => {
    if (options.autoFetch !== false) {
      await initialize()
    }
  })

  return {
    // 数据
    notifications,
    loading,
    error,
    unreadStats,
    unreadPriority,
    systemAnnouncements,
    readArchive,
    level4Messages,
    emergency,
    important,

    // UI状态
    filter,
    filtered,
    paged,
    totalPages,
    selected,
    showAllDialog,
    showDetailDialog,

    // 操作方法
    initialize,
    refresh,
    openAll,
    closeAll,
    openDetail,
    closeDetail,
    onClick,
    markRead,
    markUnread,
    markBulkRead,
    markFilteredAsRead,
    hide,
    clearArchive,
    setFilter,
    resetFilter,
    search,
    nextPage,
    prevPage,
    goToPage,

    // 辅助方法
    getLevelText,
    getAnnouncementType,
    getScopeText,
    getContentPreview,
    formatNotificationContent
  }
}