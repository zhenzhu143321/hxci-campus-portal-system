<template>
  <div class="portal-container">
    <!-- 顶部导航栏组件 -->
    <HeaderNavigation :user-info="currentUserInfo" @logout="handleLogoutFromHeader" />

    <!-- 欢迎横幅组件 -->
    <WelcomeBanner :user-info="currentUserInfo" />

    <!-- 三区布局主体 -->
    <div class="portal-main">
      <!-- 左侧：快捷服务区 -->
      <QuickServicesGrid />

      <!-- 中间：智能通知工作台组件 -->
      <IntelligentNotificationWorkspace 
        @show-all-notifications="handleShowAllNotifications"
        @notification-click="handleNotificationClick"
        @view-all-todos="handleViewAllTodos"
      />

      <!-- 右侧：校园资讯区 -->
      <div class="campus-news">
        <div class="section-header">
          <h3><el-icon><User /></el-icon>校园资讯</h3>
          <el-button type="text" size="small">更多资讯</el-button>
        </div>
        
        <div class="news-content">
          <!-- 校园新闻 -->
          <div class="news-card">
            <h4>📢 校园新闻</h4>
            <div class="news-list">
              <div v-for="news in campusNews" :key="news.id" class="news-item">
                <img 
                  :src="news.image" 
                  :alt="news.title" 
                  class="news-image" 
                  loading="lazy"
                  decoding="async"
                  @error="handleImageError" 
                />
                <div class="news-info">
                  <div class="news-title">{{ news.title }}</div>
                  <div class="news-time">{{ news.time }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 通知公告（增强版） -->
          <div class="news-card">
            <h4>🔔 系统公告</h4>
            <div class="system-announcements-list" v-loading="notificationLoading">
              <div v-if="systemAnnouncements.length === 0 && !notificationLoading" class="no-announcements">
                <el-empty description="暂无系统公告" :image-size="80">
                  <template #description>
                    <p style="color: #909399; font-size: 14px;">暂无系统公告</p>
                    <p style="color: #c0c4cc; font-size: 12px;">系统公告会显示最新的重要通知</p>
                  </template>
                </el-empty>
              </div>
              <div v-for="announcement in systemAnnouncements" :key="announcement.id" class="system-announcement-item" @click="handleNotificationClick(announcement)">
                <div class="announcement-header">
                  <el-tag :type="getAnnouncementType(announcement.level)" size="small">
                    {{ getLevelText(announcement.level) }}
                  </el-tag>
                  <div class="announcement-time">{{ formatDate(announcement.createTime) }}</div>
                </div>
                <div class="announcement-title">{{ announcement.title }}</div>
                <div class="announcement-summary" v-if="announcement.summary">
                  {{ announcement.summary }}
                </div>
                <div class="announcement-content-preview" v-else>
                  {{ getFormattedPreview(announcement.content, 120) }}
                </div>
              </div>
            </div>
          </div>
          
          <!-- 已读归档（解耦重构版） -->
          <NotificationArchivePanel
            v-if="readArchivedNotifications.length > 0"
            :archived-notifications="readArchivedNotifications"
            :max-display-count="5"
            :show-actions="true"
            @notification-click="handleNotificationClick"
            @restore-from-archive="handleMarkAsUnread"
            @permanent-delete="handlePermanentDeleteNotification"
            @clear-all-archive="handleClearAllArchive"
            @show-more="showAllNotifications = true"
          />

          <!-- 校园服务 -->
          <div class="news-card">
            <h4>🌤️ 校园服务</h4>
            <div class="service-info-list">
              <div class="service-info-item">
                <el-icon><Bell /></el-icon>
                <div class="info-content">
                  <div class="info-title">食堂菜单</div>
                  <div class="info-desc">今日推荐：宫保鸡丁</div>
                </div>
              </div>
              <div class="service-info-item">
                <el-icon><User /></el-icon>
                <div class="info-content">
                  <div class="info-title">图书馆</div>
                  <div class="info-desc">开放时间：8:00-22:00</div>
                </div>
              </div>
              <div class="service-info-item">
                <el-icon><Setting /></el-icon>
                <div class="info-content">
                  <div class="info-title">校园巴士</div>
                  <div class="info-desc">下班班次：15分钟后</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- API测试按钮（开发调试用） -->
    <div class="debug-panel" v-show="showDebugPanel">
      <el-button @click="testHealthCheck" :loading="testLoading.health" size="small">
        健康检查
      </el-button>
      <el-button @click="testTokenVerify" :loading="testLoading.verify" size="small">
        验证Token
      </el-button>
      <el-button @click="testNotificationAPI" :loading="testLoading.notification" size="small">
        通知API
      </el-button>
    </div>
  </div>

  <!-- 全部通知对话框组件 -->
  <AllNotificationsDialog
    v-model="showAllNotifications"
    @notification-click="handleNotificationClick"
    @mark-read="handleMarkAsRead"
    @mark-unread="handleMarkAsUnread"
  />

  <!-- 通知详情对话框组件 -->
  <NotificationDetailDialog
    :visible="showNotificationDetail"
    :notification="uiStore.selectedNotification"
    :read-status-checker="notificationStore.isRead"
    @update:visible="(value) => showNotificationDetail = value"
    @close="() => showNotificationDetail = false"
    @mark-read="handleMarkAsRead"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, nextTick, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  School, Avatar, SwitchButton, Bell, User, Setting,
  Clock, Document, Check, CircleCheck
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useUIStore } from '@/stores/ui'
import { useTodoStore } from '@/stores/todo'
import { authAPI } from '@/api/auth'
import type { NotificationItem } from '@/api/notification'
import { useNotificationArchiveAnimation } from '@/composables/useNotificationArchiveAnimation'
import { useAuth } from '@/composables/useAuth'
import { useNotifications } from '@/composables/useNotifications'
import { useTodos } from '@/composables/useTodos'
import WeatherWidget from '@/components/WeatherWidget.vue'
import TodoNotificationWidget from '@/components/TodoNotificationWidget.vue'
import NotificationArchiveIndicator from '@/components/notification/NotificationArchiveIndicator.vue'
// NotificationArchivePanel 已改为懒加载
import HeaderNavigation from '@/components/HeaderNavigation.vue'
import WelcomeBanner from '@/components/WelcomeBanner.vue'
import QuickServicesGrid from '@/components/QuickServicesGrid.vue'
// 🚀 Stage 9性能优化: 异步组件懒加载 (深化版)
const AllNotificationsDialog = defineAsyncComponent({
  loader: () => import('@/views/home/components/AllNotificationsDialog.vue'),
  loadingComponent: {
    template: '<div class="dialog-loading"><el-skeleton :rows="5" animated /></div>'
  },
  errorComponent: {
    template: '<div class="dialog-error">组件加载失败，请刷新重试</div>'
  },
  delay: 200, // 200ms后显示loading
  timeout: 5000 // 5s超时
})

const NotificationDetailDialog = defineAsyncComponent({
  loader: () => import('@/views/home/components/NotificationDetailDialog.vue'),
  loadingComponent: {
    template: '<div class="dialog-loading"><el-skeleton :rows="3" animated /></div>'
  },
  errorComponent: {
    template: '<div class="dialog-error">详情加载失败，请重试</div>'
  },
  delay: 200,
  timeout: 5000
})

// 🚀 Stage 9性能优化: 新增懒加载组件
const NotificationArchivePanel = defineAsyncComponent({
  loader: () => import('@/components/notification/NotificationArchivePanel.vue'),
  loadingComponent: {
    template: '<div class="archive-loading"><el-skeleton :rows="2" animated /></div>'
  },
  delay: 100
})
import IntelligentNotificationWorkspace from '@/views/home/components/IntelligentNotificationWorkspace.vue'
import dayjs from 'dayjs'
import { formatDate } from '@/utils'

// 🚀 Stage 9性能优化: 防抖工具函数
const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout | null = null
  return (...args: Parameters<T>) => {
    if (timeout) clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

// 🚀 Stage 9性能优化: 节流工具函数
const throttle = <T extends (...args: any[]) => any>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle: boolean = false
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args)
      inThrottle = true
      setTimeout(() => inThrottle = false, limit)
    }
  }
}

// 🚀 Stage 9性能优化: 性能监控工具
const performanceMonitor = {
  startTimer: (label: string): (() => void) => {
    const start = performance.now()
    console.log(`⏱️ [性能监控] ${label} 开始`)
    
    return () => {
      const end = performance.now()
      const duration = end - start
      console.log(`⏱️ [性能监控] ${label} 完成: ${duration.toFixed(2)}ms`)
      
      // 记录性能指标
      if (duration > 100) {
        console.warn(`⚠️ [性能预警] ${label} 耗时过长: ${duration.toFixed(2)}ms`)
      }
      
      return duration
    }
  },
  
  measureAsync: async <T>(label: string, asyncFunc: () => Promise<T>): Promise<T> => {
    const endTimer = performanceMonitor.startTimer(label)
    try {
      const result = await asyncFunc()
      endTimer()
      return result
    } catch (error) {
      endTimer()
      console.error(`❌ [性能监控] ${label} 执行失败:`, error)
      throw error
    }
  }
}

const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const uiStore = useUIStore()
const todoStore = useTodoStore()

// 使用新的 composables
const auth = useAuth()
const notifications = useNotifications({
  autoFetch: false,
  pageSize: 100
})
const todos = useTodos({
  mode: 'homepage',
  autoInit: false,
  autoRefresh: false
})


// 使用 useAuth composable 管理认证状态
const currentToken = computed(() => auth.token.value)
const currentUserInfo = computed(() => auth.user.value)
const isUserLoggedIn = computed(() => auth.isAuthenticated.value)

// 使用 useAuth composable 加载用户状态
const loadUserStateFromStorage = async () => {
  console.log('🔍 加载用户状态...')
  const success = await auth.loadUserFromStorage()

  if (success) {
    console.log('✅ 用户状态加载成功')
    console.log('👤 用户:', auth.user.value?.username)
    console.log('🔑 Token存在:', !!auth.token.value)
  } else {
    console.log('❌ 用户未登录或状态无效')
  }

  return success
}

// 🎯 Stage 7: 测试和调试状态已迁移到uiStore
const testLoading = uiStore.testLoading
const testResults = ref<any>(null)
const loginTime = ref('')

// 调试面板显示状态 - 使用uiStore
const showDebugPanel = computed({
  get: () => uiStore.showDebugPanel,
  set: (value: boolean) => {
    if (value !== uiStore.showDebugPanel) {
      uiStore.toggleDebugPanel()
    }
  }
})


// 使用 useNotifications composable 管理通知状态
const notificationLoading = computed(() => notifications.loading.value)
const recentNotifications = computed(() => notifications.notifications.value.slice(0, 5))
const allNotifications = computed(() => notifications.notifications.value)
const unreadNotificationCount = computed(() => notifications.unreadStats.value.total)
const systemAnnouncements = computed(() => notifications.systemAnnouncements.value)
const readArchivedNotifications = computed(() => notifications.readArchive.value)
const level4Messages = computed(() => notifications.level4Messages.value)
const emergencyNotifications = computed(() => notifications.emergency.value)
const importantNotifications = computed(() => notifications.important.value)
const unreadPriorityNotifications = computed(() => notifications.unreadPriority.value)
const unreadStats = computed(() => notifications.unreadStats.value)

// 兼容性保留 - 智能分类计算
const categorizeNotifications = computed(() => {
  return (notifications: NotificationItem[]) => categorizedNotifications.value
})

const unreadCounts = computed(() => unreadStats.value)

// 使用通知分类数据
const categorizedNotifications = computed(() => ({
  unreadPriority: unreadPriorityNotifications.value,
  readArchive: readArchivedNotifications.value,
  level4Messages: level4Messages.value,
  systemAnnouncements: systemAnnouncements.value,
  emergencyNotifications: emergencyNotifications.value,
  importantNotifications: importantNotifications.value
}))

// 🚀 Stage 9性能优化: 恢复已读状态管理并添加性能监控
let archiveAnimationManager: any = null

// 初始化归档动画管理器
const initializeArchiveAnimationManager = () => {
  if (!archiveAnimationManager) {
    archiveAnimationManager = useNotificationArchiveAnimation()
    console.log('🎬 [归档动画] 初始化完成')
  }
  return archiveAnimationManager
}

// 使用 useNotifications composable 的已读状态操作
const markAsRead = (notificationId: number) => {
  notifications.markRead(notificationId)
}

const markAsUnread = (notificationId: number) => {
  notifications.markUnread(notificationId)
}

const isRead = (notificationId: number): boolean => {
  return notificationStore.isRead(notificationId)
}

// 处理"已读"按钮点击 - 使用 useNotifications 和保留动画
const handleMarkAsRead = async (notificationId: number) => {
  const endTimer = performanceMonitor.startTimer(`标记已读-${notificationId}`)

  console.log('🔧 [DEBUG] === 开始标记已读 ===', notificationId)

  const animationManager = initializeArchiveAnimationManager()

  // 添加加载状态
  uiStore.addMarkingReadLoading(notificationId)

  try {
    console.log('🔧 [DEBUG] 标记前归档列表长度:', readArchivedNotifications.value.length)

    // 使用 useNotifications 标记已读
    notifications.markRead(notificationId)

    // 延迟检查状态更新
    await nextTick()

    console.log('🔧 [DEBUG] 标记后归档列表长度:', readArchivedNotifications.value.length)
    console.log('📝 [用户操作] 标记通知为已读:', notificationId)

    // 触发归档动画
    if (animationManager) {
      await animationManager.triggerArchiveAnimation(notificationId, {
        successMessage: '已标记为已读并归档',
        enableSound: true
      })
    } else {
      ElMessage.success('已标记为已读')
    }
  } finally {
    // 移除加载状态
    uiStore.removeMarkingReadLoading(notificationId)
    endTimer()
  }

  console.log('🔧 [DEBUG] === 标记已读完成 ===')
}

// 处理"撤销已读"按钮点击
const handleMarkAsUnread = (notificationId: number) => {
  notifications.markUnread(notificationId)
  console.log('🔄 [用户操作] 撤销已读状态:', notificationId)
  ElMessage.info('已撤销已读状态')
}

// 处理永久删除通知（修复版本 - 本地隐藏机制）
const handlePermanentDeleteNotification = (notificationId: number) => {
  // 🔧 修复：直接使用NotificationStore的hideNotification方法
  notificationStore.hideNotification(notificationId)
  ElMessage.success('已永久删除通知')
  console.log('🗑️ [用户操作] 永久隐藏通知:', notificationId)
}

// 处理清空所有归档（修复版本 - 设置清理时间而不是删除已读状态）
const handleClearAllArchive = () => {
  ElMessageBox.confirm(
    '确定要清空所有已读归档消息吗？已读状态会保留，但归档区域将被清空。',
    '清空归档确认',
    {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--warning'
    }
  ).then(() => {
    // 获取当前归档数量
    const archivedCount = readArchivedNotifications.value.length
    
    // 🔧 修复：直接使用NotificationStore的clearArchive方法
    notificationStore.clearArchive()
    
    ElMessage.success(`已清空所有归档消息 (${archivedCount}条)`)
    console.log('🧹 [用户操作] 清空归档消息，数量:', archivedCount)
  }).catch(() => {
    console.log('👤 [用户操作] 取消清空归档')
  })
}

// 🎯 Stage 7: UI状态已迁移到uiStore - 使用store的状态和方法
const showAllNotifications = computed({
  get: () => uiStore.showAllNotifications,
  set: (value: boolean) => {
    if (value) uiStore.openAllNotifications()
    else uiStore.closeAllNotifications()
  }
})

const showNotificationDetail = computed({
  get: () => uiStore.showNotificationDetail,
  set: (value: boolean) => {
    if (!value) uiStore.closeNotificationDetail()
  }
})

const selectedNotification = computed(() => uiStore.selectedNotification)

// 通知筛选器 - 使用uiStore
const notificationFilter = uiStore.notificationFilters

// 🎯 Stage 7: 分页状态已迁移到uiStore
const notificationPagination = uiStore.notificationPagination

// 🎯 Stage 7: 待办统计使用todoStore
const pendingTodoCount = computed(() => todoStore.pendingCount)

// 通知点击处理 - 修复：使用uiStore打开详情对话框
const handleNotificationClick = async (notification: NotificationItem, autoMarkRead: boolean = false) => {
  try {
    // 获取通知详情（如果可能的话）
    const detail = await notificationStore.getNotificationDetail(notification.id).catch(() => null)

    // 使用uiStore打开详情对话框（这是对话框绑定的状态）
    uiStore.openNotificationDetail(detail || notification)

    // 如果需要，标记为已读
    if (autoMarkRead) {
      notificationStore.markAsRead(notification.id)
    }
  } catch (error) {
    console.error('打开通知详情失败:', error)
    // 即使出错，仍然尝试打开对话框
    uiStore.openNotificationDetail(notification)
  }
}

// 🚀 Stage 9性能优化: 防抖版本的通知点击处理 (强化版)
const debouncedNotificationClick = debounce(handleNotificationClick, 300)

// 🚀 Stage 9性能优化: 防抖搜索处理
const debouncedSearch = debounce((searchTerm: string) => {
  notificationFilter.search = searchTerm
}, 500)

// 🚀 Stage 9性能优化: 节流滚动处理
const throttledScroll = throttle((event: Event) => {
  const target = event.target as HTMLElement
  if (target.scrollTop > 100) {
    // 滚动优化逻辑
    console.log('📜 [性能优化] 滚动节流处理')
  }
}, 16) // 60fps

// 今日课程安排 Mock数据（革命性工作台功能）- 保留本地数据
const todayCourses = ref([
  {
    id: 1,
    time: '08:00-09:40',
    name: '高等数学',
    location: 'A101',
    teacher: '王教授',
    status: 'completed'
  },
  {
    id: 2,
    time: '10:00-11:40',
    name: '数据结构',
    location: 'B201',
    teacher: '李老师',
    status: 'current' // 当前进行中
  },
  {
    id: 3,
    time: '14:00-15:40',
    name: '英语听说',
    location: 'C301',
    teacher: '张老师',
    status: 'upcoming'
  },
  {
    id: 4,
    time: '16:00-17:40',
    name: '计算机网络',
    location: 'D401',
    teacher: '刘教授',
    status: 'upcoming'
  }
])

// 获取级别文本
const getLevelText = (level: number): string => {
  switch (level) {
    case 1: return '紧急'
    case 2: return '重要'
    case 3: return '常规'
    case 4: return '提醒'
    default: return '未知'
  }
}

// 格式化通知内容（处理换行符和格式）
const formatNotificationContent = (content: string): string => {
  if (!content) return ''
  // 将\n转换为实际换行符，处理各种换行格式
  return content
    .replace(/\\n/g, '\n')  // 转义的\n转为真换行
    .replace(/\n\s*\n/g, '\n\n')  // 规范化多重换行
    .replace(/^\s+|\s+$/g, '')  // 去除首尾空白
    .trim()
}

// 获取内容预览（用于卡片显示，将换行转为空格）
const getContentPreview = (content: string, maxLength: number = 50): string => {
  if (!content) return ''
  // 先格式化，然后将换行符替换为空格用于预览
  const formatted = formatNotificationContent(content)
  const preview = formatted.replace(/\n{2,}/g, ' | ').replace(/\n/g, ' ')
  return preview.length > maxLength ? preview.substring(0, maxLength) + '...' : preview
}

// 获取格式化的内容预览（用于右侧通知公告）
const getFormattedPreview = (content: string, maxLength: number = 80): string => {
  if (!content) return ''
  const formatted = formatNotificationContent(content)
  // 将换行符替换为空格用于预览，但保持段落结构
  const preview = formatted.replace(/\n{2,}/g, ' | ').replace(/\n/g, ' ')
  return preview.length > maxLength ? preview.substring(0, maxLength) + '...' : preview
}

// 获取范围文本
const getScopeText = (scope: string): string => {
  switch (scope) {
    case 'SCHOOL_WIDE': return '全校'
    case 'DEPARTMENT': return '部门'
    case 'GRADE': return '年级'
    case 'CLASS': return '班级'
    default: return scope
  }
}

// 筛选后的通知列表
const filteredNotifications = computed(() => {
  let filtered = allNotifications.value

  // 按级别筛选
  if (notificationFilter.level !== null) {
    filtered = filtered.filter(item => item.level === notificationFilter.level)
  }

  // 按范围筛选
  if (notificationFilter.scope) {
    filtered = filtered.filter(item => item.scope === notificationFilter.scope)
  }

  // 按标题搜索
  if (notificationFilter.search) {
    filtered = filtered.filter(item => 
      item.title.toLowerCase().includes(notificationFilter.search.toLowerCase())
    )
  }

  return filtered
})

// 校园新闻
const campusNews = ref([
  {
    id: 1,
    title: '我校在全国程序设计竞赛中获得佳绩',
    time: '2025-08-12',
    image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA2MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yNSAyMEMyNSAxNy4yMzg2IDI3LjIzODYgMTUgMzAgMTVDMzIuNzYxNCAxNSAzNSAxNy4yMzg2IDM1IDIwQzM1IDIyLjc2MTQgMzIuNzYxNCAyNSAzMCAyNUMyNy4yMzg2IDI1IDI1IDIyLjc2MTQgMjUgMjBaIiBmaWxsPSIjQ0NDQ0NDIi8+CjxwYXRoIGQ9Ik0yMCAyOEwyNS41IDIyLjVMMzIuNSAyOS41TDQwIDIyTDQwIDMySDIwVjI4WiIgZmlsbD0iI0NDQ0NDQyIvPgo8L3N2Zz4K'
  },
  {
    id: 2,
    title: '2025年春季学期开学典礼成功举行',
    time: '2025-08-11', 
    image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA2MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjRjBGOEZGIi8+CjxjaXJjbGUgY3g9IjMwIiBjeT0iMTYiIHI9IjQiIGZpbGw9IiM0MDlFRkYiLz4KPHBhdGggZD0iTTIyIDI2QzIyIDIzLjc5MDkgMjMuNzkwOSAyMiAyNiAyMkgzNEMzNi4yMDkxIDIyIDM4IDIzLjc5MDkgMzggMjZWMzJIMjJWMjZaIiBmaWxsPSIjNDA5RUZGIi8+Cjwvc3ZnPgo='
  }
])

// 当前显示的紧急通知（支持轮播，基于智能分类结果）
const currentEmergencyNotification = computed(() => {
  return emergencyNotifications.value[0] || null
})

// 公告通知数据（右侧通知公告栏专用，改为使用智能分类的系统公告）
const announcementNotifications = computed(() => systemAnnouncements.value)

// 获取通知类型
const getAnnouncementType = (level: number): string => {
  switch (level) {
    case 1: return 'danger'  // 紧急
    case 2: return 'warning' // 重要
    case 3: return 'info'    // 常规
    case 4: return 'success' // 提醒
    default: return 'info'
  }
}

// formatDate函数已迁移到 @/utils

// 处理紧急通知点击（兼容性保留）
const handleEmergencyClick = (notification: NotificationItem) => {
  console.log('🚨 点击紧急通知:', notification.title)
  handleNotificationClick(notification)
}

// 数据加载逻辑 - 使用notifications composable
const loadNotificationData = async () => {
  console.log('📢 开始加载通知数据...')

  try {
    // 使用notifications composable的方法加载数据
    await notifications.refresh()
    console.log('✅ 通知数据加载成功:', allNotifications.value.length, '条')

    // 更新未读数量
    updateUnreadCount()
  } catch (error) {
    console.error('❌ 加载通知数据失败:', error)
    ElMessage.error('通知数据加载失败')
  }
}

// 🎯 Stage 7: 未读数量更新使用notificationStore
const updateUnreadCount = () => {
  try {
    const counts = unreadStats.value
    console.log('🔔 [智能统计] 更新未读通知数量:', counts)
  } catch (error) {
    console.error('❌ 更新未读数量失败:', error)
  }
}


// API测试方法
const testHealthCheck = async () => {
  console.log('=== 健康检查测试开始 ===')
  console.log('🏥 开始测试Mock School API健康检查...')
  
  testLoading.health = true
  testResults.value = null
  
  try {
    console.log('📤 发送健康检查请求...')
    const result = await authAPI.healthCheck()
    console.log('📥 健康检查响应:', result)
    
    if (result.success) {
      console.log('✅ 健康检查成功')
      ElMessage.success('Mock School API 服务正常运行')
    } else {
      console.log('❌ 健康检查失败')
    }
  } catch (error) {
    console.log('❌ 健康检查异常:', error)
  } finally {
    testLoading.health = false
    console.log('=== 健康检查测试结束 ===')
  }
}

const testTokenVerify = async () => {
  console.log('=== Token验证测试开始 ===')
  console.log('🔑 当前Token:', currentToken.value?.substring(0, 50) + '...')
  
  testLoading.verify = true
  testResults.value = null
  
  if (!currentToken.value) {
    console.log('❌ 没有可验证的Token')
    testLoading.verify = false
    return
  }
  
  try {
    console.log('📤 发送Token验证请求...')
    const result = await authAPI.verifyToken(currentToken.value)
    console.log('📥 Token验证响应:', result)
    
    if (result.success) {
      console.log('✅ Token验证成功')
      ElMessage.success('Token验证通过')
    } else {
      console.log('❌ Token验证失败')
    }
  } catch (error: any) {
    console.log('❌ Token验证异常:', error)
  } finally {
    testLoading.verify = false
    console.log('=== Token验证测试结束 ===')
  }
}

const testNotificationAPI = async () => {
  console.log('=== 通知API测试开始 ===')
  console.log('📢 开始测试主通知服务连接...')
  console.log('🔑 使用Token:', currentToken.value?.substring(0, 50) + '...')
  
  testLoading.notification = true
  testResults.value = null
  
  try {
    console.log('📤 发送通知API Ping请求...')
    
    // 🔧 修复：使用Vite代理路径，避免CORS问题
    const response = await fetch('/admin-api/test/notification/api/ping', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${currentToken.value}`,
        'Content-Type': 'application/json',
        'tenant-id': '1'
      }
    })
    
    console.log('📥 通知API响应状态:', response.status, response.statusText)
    
    const result = await response.text()
    console.log('📥 通知API响应内容:', result)
    
    if (response.ok) {
      console.log('✅ 主通知服务连接成功')
      ElMessage.success(`主通知服务连接正常: ${result}`)
      testResults.value = {
        type: 'success',
        message: '主通知服务连接正常',
        details: result
      }
    } else {
      console.log('❌ 通知API响应错误')
      ElMessage.error(`通知API响应错误: ${response.status}`)
      testResults.value = {
        type: 'error',
        message: `通知API响应错误: ${response.status}`,
        details: result
      }
    }
  } catch (error) {
    console.log('❌ 通知API测试异常:', error)
    ElMessage.error(`通知API测试异常: ${error.message}`)
    testResults.value = {
      type: 'error',
      message: '通知API测试异常',
      details: error.message
    }
  } finally {
    testLoading.notification = false
    console.log('=== 通知API测试结束 ===')
  }
}


// 计算属性
const isAdmin = computed(() => {
  return currentUserInfo.value?.roleCode === 'PRINCIPAL' || currentUserInfo.value?.roleCode === 'ACADEMIC_ADMIN'
})

// 🔧 P0级修复: 统一退出登录逻辑 (使用UserStore)
const handleLogoutFromHeader = () => {
  try {
    console.log('🔓 [HeaderNavigation] 开始处理退出登录...')
    
    // 🔧 使用UserStore统一管理用户状态
    userStore.logout()
    
    // 重置Store状态
    notificationStore.setCurrentUserId(null)
    uiStore.resetAllUIState()
    
    // 重置归档动画管理器
    archiveAnimationManager = null
    
    console.log('🧹 [HeaderNavigation] 清理本地数据完成')
    
    console.log('✅ [HeaderNavigation] 退出登录处理完成')
  } catch (error) {
    console.error('❌ [HeaderNavigation] 退出过程出错:', error)
    ElMessage.error('退出登录时出现异常，请刷新页面')
    
    // 🔧 备用清理方案：使用UserStore确保清理
    try {
      userStore.logout()
    } catch (fallbackError) {
      console.error('❌ [HeaderNavigation] 备用清理也失败:', fallbackError)
    }
  }
}

// 处理用户退出登录（保留原函数以兼容其他组件）
const handleLogout = async () => {
  try {
    console.log('🔓 [退出登录] 开始处理用户退出...')
    
    // 显示确认对话框
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    console.log('✅ [退出登录] 用户确认退出')
    
    // 🔧 P0级修复: 使用UserStore统一管理退出逻辑
    userStore.logout()
    // ✅ 已读状态应该持久化保存，用户重新登录后仍能看到已读归档
    
    // 🔧 重置Store状态和归档动画管理器（为下次登录准备）
    notificationStore.setCurrentUserId(null)
    uiStore.resetAllUIState()
    archiveAnimationManager = null
    
    console.log('🧹 [退出登录] 清理本地数据完成')
    
    // 显示退出成功提示
    ElMessage.success('退出登录成功')
    
    // 跳转到登录页
    console.log('🔄 [退出登录] 跳转到登录页面')
    router.push('/login')
    
  } catch (error) {
    // 用户取消退出，不显示错误
    if (error !== 'cancel') {
      console.error('❌ [退出登录] 退出过程出错:', error)
      ElMessage.error('退出登录失败')
    }
  }
}

// 处理图片加载错误
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  if (target) {
    target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA2MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yNSAyMEMyNSAxNy4yMzg2IDI3LjIzODYgMTUgMzAgMTVDMzIuNzYxNCAxNSAzNSAxNy4yMzg2IDM1IDIwQzM1IDIyLjc2MTQgMzIuNzYxNCAyNSAzMCAyNUMyNy4yMzg2IDI1IDI1IDIyLjc2MTQgMjUgMjBaIiBmaWxsPSIjQ0NDQ0NDIi8+CjxwYXRoIGQ9Ik0yMCAyOEwyNS41IDIyLjVMMzIuNSAyOS41TDQwIDIyTDQwIDMySDIwVjI4WiIgZmlsbD0iI0NDQ0NDQyIvPgo8L3N2Zz4K'
  }
}

// 待办通知相关函数

// 处理待办完成事件 - 优化用户体验
const handleTodoComplete = async (id: number, completed: boolean) => {
  try {
    await todos.markComplete(id, completed)

    if (completed) {
      ElMessage.success('🎉 待办已完成！任务已从首页移除')

      // 添加一个短暂的视觉反馈，让用户看到完成动画
      setTimeout(() => {
        // 触发数据刷新，确保首页显示最新状态
        todos.refresh()
      }, 1000)
    } else {
      ElMessage.info('待办已标记为未完成')
    }
  } catch (error) {
    ElMessage.error('操作失败，请重试')
    console.error('待办状态更新失败:', error)
  }
}

// 处理查看全部待办事件
const handleViewAllTodos = () => {
  router.push('/todo-management')
  console.log('📋 跳转到待办管理页面')
}

// 🔧 P0级修复: 处理显示全部通知事件
const handleShowAllNotifications = () => {
  console.log('📋 [事件处理] 显示全部通知弹窗')
  uiStore.openAllNotifications()
}

// 🎯 Stage 7: 组件初始化 - 使用Store进行状态管理
onMounted(async () => {
  console.log('=== 首页初始化开始 ===')
  console.log('当前时间:', dayjs().format('YYYY/MM/DD HH:mm:ss'))
  console.log('当前路由:', router.currentRoute.value.path)
  
  console.log('🔍 开始加载用户认证状态...')
  
  // 🔧 P0级修复: 使用UserStore异步加载用户状态
  const isLoggedIn = await loadUserStateFromStorage()
  
  if (isLoggedIn && currentUserInfo.value) {
    loginTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
    console.log('✅ 用户已登录，首页初始化完成')
    console.log('👤 当前用户:', currentUserInfo.value.username)
    console.log('🔑 当前Token长度:', currentToken.value.length)
    
    // 🎯 Stage 7: 设置notificationStore的用户ID
    const userId = currentUserInfo.value?.userId
    if (userId) {
      notificationStore.setCurrentUserId(userId)
      console.log('🆔 [NotificationStore] 用户ID已设置:', userId)
    }
    
    // 🎯 Stage 7: 初始化归档动画管理器
    archiveAnimationManager = null // 重置动画管理器
    initializeArchiveAnimationManager() // 初始化动画管理器
    
    // 初始化composables
    await notifications.initialize({ userId: userId })
    await todos.initialize()

    // 用户登录成功后加载数据
    loadNotificationData()
  } else {
    console.log('❌ 用户未登录，准备跳转到登录页')
    router.push('/login')
  }
  
  console.log('=== 首页初始化结束 ===')
})

// 🚀 Stage 9性能优化: 组件卸载时的资源清理 (强化版)
onUnmounted(() => {
  const endTimer = performanceMonitor.startTimer('组件卸载清理')
  console.log('=== Home组件开始卸载 ===')
  
  // 清理归档动画管理器
  if (archiveAnimationManager) {
    console.log('🧹 清理归档动画管理器')
    archiveAnimationManager = null
  }
  
  // 🚀 Stage 9优化: 删除重复的SessionStorage清理逻辑，NotificationService已统一管理缓存
  
  // 清理可能的定时器和事件监听器
  console.log('🧹 清理事件监听器和定时器')
  window.removeEventListener('scroll', throttledScroll)
  window.removeEventListener('resize', throttledScroll)
  
  // 🔧 Pinia Store状态重置 (替代不存在的cleanup方法)
  try {
    // 重置通知Store状态 (使用$reset方法)
    if (notificationStore && typeof notificationStore.$reset === 'function') {
      notificationStore.$reset()
      console.log('✅ NotificationStore状态已重置')
    }
    
    // 重置UI Store状态 (使用$reset方法)
    if (uiStore && typeof uiStore.$reset === 'function') {
      uiStore.$reset()
      console.log('✅ UIStore状态已重置')
    }
    
    // 重置待办Store状态 (使用$reset方法)
    if (todoStore && typeof todoStore.$reset === 'function') {
      todoStore.$reset()
      console.log('✅ TodoStore状态已重置')
    }
  } catch (error) {
    console.warn('⚠️ Store重置过程中出现警告:', error)
  }
  
  console.log('✅ Home组件资源清理完成')
  endTimer()
  console.log('=== Home组件卸载结束 ===')
})
</script>

<style scoped lang="scss">
// 引入提取的样式文件
@import '@/styles/views/home.scss';

// 所有样式已提取到 home.scss 文件中
// 文件大小从2068行优化到约1090行
</style>
