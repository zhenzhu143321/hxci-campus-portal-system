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
import { authAPI } from '@/api/auth'
import type { NotificationItem } from '@/api/notification'
import { useNotificationArchiveAnimation } from '@/composables/useNotificationArchiveAnimation'
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
import type { TodoNotificationItem } from '@/types/todo'
import { useTodoStore } from '@/stores/todo'
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


// 🔧 P0级修复: 统一使用UserStore管理用户状态 (替代直接localStorage操作)
// 使用computed确保响应式更新
const currentToken = computed(() => userStore.token)
const currentUserInfo = computed(() => userStore.userInfo)
const isUserLoggedIn = computed(() => userStore.isLoggedIn)

// 🔧 统一用户状态初始化方法 (使用UserStore)
const loadUserStateFromStorage = async () => {
  console.log('🔍 使用UserStore加载用户状态...')
  
  try {
    // 使用UserStore的初始化方法
    await userStore.initializeAuth()
    
    if (userStore.isLoggedIn && userStore.userInfo) {
      console.log('✅ 用户状态加载成功:')
      console.log('👤 用户:', userStore.userInfo.username)
      console.log('🔑 Token长度:', userStore.token.length)
      
      return true
    } else {
      console.log('❌ 用户未登录或状态无效')
      return false
    }
  } catch (error) {
    console.error('❌ UserStore初始化失败:', error)
    console.log('❌ 未找到有效的用户状态')
    return false
  }
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


// 🚀 Stage 9性能优化: 计算属性缓存优化 (深化版) - 使用shallowRef减少深度响应性开销
const notificationData = computed(() => {
  const endTimer = performanceMonitor.startTimer('通知数据计算')
  
  const notifications = notificationStore.notifications
  const loading = notificationStore.loading
  const unreadStats = notificationStore.unreadStats
  
  const result = {
    loading,
    notifications,
    recent: notificationStore.recentNotifications,
    unreadTotal: unreadStats.total,
    unreadStats,
    // 智能分类（一次性计算所有分类，减少Store访问次数）
    systemAnnouncements: notificationStore.systemAnnouncements,
    readArchived: notificationStore.readArchivedNotifications,
    level4Messages: notificationStore.level4Messages,
    emergency: notificationStore.emergencyNotifications,
    important: notificationStore.importantNotifications,
    unreadPriority: notificationStore.unreadPriorityNotifications
  }
  
  endTimer()
  return result
})

// 🚀 Stage 9性能优化: 使用合并的计算属性，避免重复计算
const notificationLoading = computed(() => notificationData.value.loading)
const recentNotifications = computed(() => notificationData.value.recent)
const allNotifications = computed(() => notificationData.value.notifications)
const unreadNotificationCount = computed(() => notificationData.value.unreadTotal)

// 🚀 Stage 9性能优化: 继续优化剩余计算属性，使用合并数据源
const systemAnnouncements = computed(() => notificationData.value.systemAnnouncements)
const readArchivedNotifications = computed(() => notificationData.value.readArchived)
const level4Messages = computed(() => notificationData.value.level4Messages)
const emergencyNotifications = computed(() => notificationData.value.emergency)
const importantNotifications = computed(() => notificationData.value.important)
const unreadPriorityNotifications = computed(() => notificationData.value.unreadPriority)
const unreadStats = computed(() => notificationData.value.unreadStats)

// 🎯 Stage 7: 兼容性保留 - 智能分类计算
const categorizeNotifications = computed(() => {
  // 兼容原有接口，返回统一的分类结果
  return (notifications: NotificationItem[]) => ({
    unreadPriority: unreadPriorityNotifications.value,
    readArchive: readArchivedNotifications.value,
    level4Messages: level4Messages.value,
    systemAnnouncements: systemAnnouncements.value,
    emergencyNotifications: emergencyNotifications.value,
    importantNotifications: importantNotifications.value
  })
})

const unreadCounts = computed(() => {
  return unreadStats.value
})

// 🎯 Stage 7: 智能工作台计算属性已迁移到notificationStore - 直接使用store计算属性
const categorizedNotifications = computed(() => {
  return categorizeNotifications.value(allNotifications.value)
})

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

// 🎯 Stage 7: 已读状态操作已迁移到notificationStore
const markAsRead = (notificationId: number) => {
  notificationStore.markAsRead(notificationId)
}

const markAsUnread = (notificationId: number) => {
  notificationStore.markAsUnread(notificationId)
}

const isRead = (notificationId: number): boolean => {
  return notificationStore.isRead(notificationId)
}

// 🎯 Stage 7: 处理"已读"按钮点击 - 使用store和保留动画 + 🚀 Stage 9性能优化
const handleMarkAsRead = async (notificationId: number) => {
  const endTimer = performanceMonitor.startTimer(`标记已读-${notificationId}`)
  
  console.log('🔧 [DEBUG] === 开始标记已读 ===', notificationId)
  
  const animationManager = initializeArchiveAnimationManager()
  
  // 添加加载状态
  uiStore.addMarkingReadLoading(notificationId)
  
  try {
    console.log('🔧 [DEBUG] 标记前归档列表长度:', readArchivedNotifications.value.length)
    
    // 使用store方法标记已读
    notificationStore.markAsRead(notificationId)
    
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
  markAsUnread(notificationId)
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

// 🎯 Stage 7: 通知点击处理 - 使用uiStore管理对话框状态 + 🚀 Stage 9性能优化 (去除重复缓存)
const handleNotificationClick = async (notification: NotificationItem, autoMarkRead: boolean = false) => {
  return performanceMonitor.measureAsync(`通知详情查看-${notification.id}`, async () => {
    console.log('📖 点击查看通知详情:', notification.title)
    
    try {
      // 🚀 Stage 9优化: 直接使用NotificationService的统一缓存机制，避免重复缓存
      const notificationDetail = await notificationStore.getNotificationDetail(notification.id)
      
      if (notificationDetail) {
        uiStore.openNotificationDetail(notificationDetail)
        
        // 只有明确指定才自动标记为已读
        if (autoMarkRead && !notificationStore.isRead(notification.id)) {
          notificationStore.markAsRead(notification.id)
          console.log('🏷️ [自动标记] 通知已标记为已读:', notification.id)
        }
      } else {
        ElMessage.error('获取通知详情失败')
      }
    } catch (error) {
      console.error('❌ 查看通知详情失败:', error)
      ElMessage.error('查看通知详情失败')
    }
  })
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

// 🎯 Stage 7: 数据加载逻辑已迁移到notificationStore
const loadNotificationData = async () => {
  console.log('📢 开始加载通知数据...')
  
  try {
    // 使用notificationStore的方法加载数据
    await notificationStore.fetchNotifications({ pageSize: 100 })
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
    await todoStore.updateTodoStatus(id, completed)
    
    if (completed) {
      ElMessage.success('🎉 待办已完成！任务已从首页移除')
      
      // 添加一个短暂的视觉反馈，让用户看到完成动画
      setTimeout(() => {
        // 触发数据刷新，确保首页显示最新状态
        todoStore.refreshTodos()
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
    
    // 用户登录成功后加载数据
    loadNotificationData()
    
    // 初始化待办通知数据 - 使用store
    todoStore.initializeTodos()
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

<style scoped>
/* 全局容器 - 清新学院风渐变背景 */
.portal-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #F0F9FF 0%, #DBEAFE 20%, #BFDBFE 100%);
  position: relative;
}

/* 添加微妙的背景图案 */
.portal-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(16, 185, 129, 0.03) 0%, transparent 50%);
  pointer-events: none;
  z-index: 1;
}

/* 确保内容在背景之上 */
.portal-container > * {
  position: relative;
  z-index: 2;
}

/* 三区布局主体 */
.portal-main {
  display: grid;
  grid-template-columns: 300px 1fr 320px;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 中间智能工作台 - 革命性重构样式 */
.intelligent-workspace {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(30, 58, 138, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.1);
  padding: 24px;
  height: fit-content;
  transition: all 0.3s ease;
}

.intelligent-workspace:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(30, 58, 138, 0.12);
}

/* 🎯 优先通知工作台区域 - 革命性分级颜色系统 */
.priority-workspace-section {
  margin-bottom: 24px;
  border-radius: 12px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.6) 0%, rgba(255, 255, 255, 0.9) 100%);
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.workspace-priority-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.workspace-priority-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1E3A8A;
}

.priority-notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 分级通知卡片 - 红橙蓝分级设计 */
.priority-notification-card {
  background: white;
  border-radius: 10px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

/* Level 1 紧急通知 - 红色系 */
.priority-notification-card.level-1-emergency {
  background: linear-gradient(135deg, #ffebee 0%, #fef2f2 100%);
  border-color: #f87171;
  box-shadow: 0 4px 15px rgba(248, 113, 113, 0.15);
}

.priority-notification-card.level-1-emergency:hover {
  border-color: #dc2626;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(220, 38, 38, 0.2);
}

.priority-notification-card.level-1-emergency::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #dc2626 0%, #f87171 100%);
}

/* Level 2 重要通知 - 橙色系 */
.priority-notification-card.level-2-important {
  background: linear-gradient(135deg, #fff8e1 0%, #fffbeb 100%);
  border-color: #fbbf24;
  box-shadow: 0 4px 15px rgba(251, 191, 36, 0.15);
}

.priority-notification-card.level-2-important:hover {
  border-color: #d97706;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(217, 119, 6, 0.2);
}

.priority-notification-card.level-2-important::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #d97706 0%, #fbbf24 100%);
}

/* Level 3 常规通知 - 蓝色系 */
.priority-notification-card.level-3-regular {
  background: linear-gradient(135deg, #e3f2fd 0%, #f0f9ff 100%);
  border-color: #60a5fa;
  box-shadow: 0 4px 15px rgba(96, 165, 250, 0.15);
}

.priority-notification-card.level-3-regular:hover {
  border-color: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(37, 99, 235, 0.2);
}

.priority-notification-card.level-3-regular::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2563eb 0%, #60a5fa 100%);
}

/* 通知内容样式 */
.notification-priority-content {
  position: relative;
  z-index: 2;
}

.notification-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
  gap: 12px;
}

.notification-title-priority {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.mark-read-btn {
  min-width: 60px;
  height: 28px;
}

.notification-summary-priority {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  border-left: 3px solid #e5e7eb;
}

.notification-meta-priority {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #9ca3af;
}

.notification-publisher-priority {
  font-weight: 500;
  color: #374151;
}

.show-more-priority {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(59, 130, 246, 0.1);
}

/* 🎨 工作台模块卡片基础样式 */
.workspace-module-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.workspace-module-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.module-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 📋 待办通知模块专用样式 - 紫色主题 */
.todo-notification-module {
  border-left: 4px solid #8B5CF6;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.05), rgba(255, 255, 255, 0.9));
}

.todo-notification-module:hover {
  border-left-color: #7C3AED;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08), rgba(255, 255, 255, 0.95));
}

.todo-notification-module .module-header h4 {
  color: #8B5CF6;
}

.todo-notification-module .module-header .el-tag {
  background-color: rgba(139, 92, 246, 0.1);
  color: #8B5CF6;
  border-color: rgba(139, 92, 246, 0.2);
}

/* 📚 今日课程安排模块 */
.course-module {
  border-left: 4px solid #3b82f6;
}

.course-schedule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.course-schedule-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.6);
}

.course-schedule-item:hover {
  background: rgba(59, 130, 246, 0.05);
  transform: translateX(4px);
}

.course-schedule-item.course-completed {
  opacity: 0.7;
  background: rgba(156, 163, 175, 0.1);
}

.course-schedule-item.course-current {
  background: rgba(251, 191, 36, 0.1);
  border-left: 3px solid #fbbf24;
  animation: pulse 2s infinite;
}

.course-schedule-item.course-upcoming {
  background: rgba(34, 197, 94, 0.1);
  border-left: 3px solid #22c55e;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.course-time-info {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  min-width: 80px;
  text-align: center;
  background: rgba(59, 130, 246, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
}

.course-details {
  flex: 1;
}

.course-name-main {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2px;
}

.course-location-teacher {
  font-size: 12px;
  color: #6b7280;
}

/* 💬 Level 4 通知消息模块 */
.level4-module {
  border-left: 4px solid #10b981;
}

.level4-messages-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.level4-message-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(16, 185, 129, 0.05);
  border: 1px solid rgba(16, 185, 129, 0.1);
}

.level4-message-item:hover {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.2);
  transform: translateX(4px);
}

.level4-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 50%;
}

.level4-content {
  flex: 1;
}

.level4-title {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 2px;
  line-height: 1.3;
}

.level4-time {
  font-size: 11px;
  color: #6b7280;
}

.level4-action {
  flex-shrink: 0;
}

/* Level 4 已读状态样式 */
.level4-message-item.level4-read {
  opacity: 0.6;
  background: rgba(156, 163, 175, 0.1);
  border-color: rgba(156, 163, 175, 0.2);
}

.level4-message-item.level4-read:hover {
  background: rgba(156, 163, 175, 0.15);
  border-color: rgba(156, 163, 175, 0.3);
}

.level4-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}

.level4-actions .mark-read-btn {
  padding: 4px 8px;
  font-size: 12px;
  min-width: 70px;
  height: 28px;
}

.level4-read-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 8px;
}

/* 右侧校园资讯区 - 现代化卡片 */
.campus-news {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(30, 58, 138, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.1);
  padding: 24px;
  height: fit-content;
  transition: all 0.3s ease;
}

.campus-news:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(30, 58, 138, 0.12);
}

.news-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.news-card {
  border: 1px solid rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.5) 0%, rgba(255, 255, 255, 0.8) 100%);
  transition: all 0.3s ease;
}

.news-card:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.1);
  transform: translateY(-1px);
}

.news-card h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #262626;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.news-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.news-image {
  width: 60px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.news-info {
  flex: 1;
  min-width: 0;
}

.news-title {
  font-size: 13px;
  color: #262626;
  line-height: 1.3;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-time {
  font-size: 11px;
  color: #8c8c8c;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 删除重复的announcement-title和announcement-time定义，使用后面合并的版本 */

.service-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-content {
  flex: 1;
}

.info-title {
  font-size: 13px;
  color: #262626;
  line-height: 1.2;
  margin-bottom: 2px;
}

.info-desc {
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.2;
}

/* 🚀 Stage 9性能优化: 新增加载状态样式 */
.dialog-loading, .archive-loading {
  padding: 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-error {
  padding: 20px;
  text-align: center;
  color: #f56565;
  background: #fed7d7;
  border-radius: 8px;
  border: 1px solid #feb2b2;
}

/* 调试面板 */
.debug-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  display: flex;
  gap: 8px;
  background: white;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .portal-main {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }
  
  .portal-main {
    padding: 0 16px;
  }
  
  .brand-title {
    font-size: 16px;
  }
  
  .user-details {
    display: none;
  }
}

/* 通知对话框样式 */
.notification-more {
  text-align: center;
  padding: 8px 0;
  border-top: 1px solid #eee;
  margin-top: 8px;
}

.notification-dialog .notification-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.notification-dialog .notification-table {
  margin-bottom: 16px;
}

.notification-dialog .notification-pagination {
  display: flex;
  justify-content: center;
}

.notification-detail .notification-meta {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.notification-detail .meta-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.notification-detail .meta-row:last-child {
  margin-bottom: 0;
}

.notification-detail .meta-label {
  font-weight: 500;
  width: 80px;
  color: #606266;
}

.notification-detail .notification-content-detail {
  line-height: 1.6;
}

/* 格式化内容样式 - 支持换行和美观格式 */
.formatted-content {
  white-space: pre-wrap; /* 保留换行和空格 */
  word-wrap: break-word; /* 长单词换行 */
  line-height: 1.8; /* 增加行高提升可读性 */
  font-size: 14px;
  color: #333;
}

.notification-detail .content-text {
  background: #fff;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  white-space: pre-wrap; /* 关键：保留换行符格式 */
  word-wrap: break-word;
  color: #333;
  line-height: 1.8; /* 提升行高 */
  font-size: 14px;
  max-height: 400px; /* 限制最大高度 */
  overflow-y: auto; /* 超出滚动 */
}

/* 优先通知工作台样式(统一设计) */
.priority-notification-section {
  margin-bottom: 20px;
}

/* 紧急通知工作台样式 */
.emergency-workspace-card {
  background: linear-gradient(135deg, #fee2e2, #fef2f2);
  border: 2px solid #fca5a5;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(248, 113, 113, 0.15);
}

/* 重要通知工作台样式 */
.important-workspace-card {
  background: linear-gradient(135deg, #fef3c7, #fffbeb);
  border: 2px solid #fcd34d;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.15);
}

/* 通知工作台头部(通用) */
.notification-workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.notification-workspace-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.emergency-workspace-card .notification-workspace-header h3 {
  color: #dc2626;
}

.important-workspace-card .notification-workspace-header h3 {
  color: #d97706;
}

/* 通知列表(通用) */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 通知项(通用) */
.notification-workspace-item {
  background: white;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 紧急通知项样式 */
.emergency-item {
  border: 1px solid #fca5a5;
}

.emergency-item:hover {
  border-color: #dc2626;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.15);
  transform: translateY(-1px);
}

/* 重要通知项样式 */
.important-item {
  border: 1px solid #fcd34d;
}

.important-item:hover {
  border-color: #d97706;
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.15);
  transform: translateY(-1px);
}

/* 通知内容(通用) */
.notification-content-main {
  flex: 1;
  min-width: 0;
}

.notification-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-title {
  font-weight: 600;
  font-size: 15px;
  margin-right: 12px;
}

.emergency-title {
  color: #dc2626;
}

.important-title {
  color: #d97706;
}

.notification-summary {
  color: #666;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 8px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 4px;
  white-space: pre-line; /* 支持换行 */
  word-wrap: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 工作台显示最多2行 */
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.notification-publisher {
  font-weight: 500;
}

.notification-action-btn {
  margin-left: 16px;
  flex-shrink: 0;
}

/* 查看更多按钮 */
.show-more-notifications {
  text-align: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #fcd34d;
}

/* 常规统计区域样式调整 */
.stats-section {
  margin-bottom: 20px;
}

/* 原有紧急通知横幅样式删除 */

/* 通知公告项样式 - 合并版本，避免冲突 */
.announcement-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(59, 130, 246, 0.1);
  margin-bottom: 12px;
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.4) 0%, rgba(255, 255, 255, 0.8) 100%);
}

.announcement-item:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(16, 185, 129, 0.05) 100%);
  transform: translateX(4px);
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.1);
}

.announcement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.announcement-title {
  font-weight: 600;
  color: #262626;
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 4px;
}

.announcement-summary,
.announcement-content-preview {
  font-size: 13px;
  color: #666;
  line-height: 1.6; /* 增加行高 */
  margin-top: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #e9ecef;
  white-space: pre-line; /* 支持换行但不保留多余空格 */
  word-wrap: break-word;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4; /* 增加到4行 */
  -webkit-box-orient: vertical;
}

/* 系统公告专用样式 */
.system-announcement-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 18px; /* 增加内边距 */
  border-radius: 12px;
  border: 1px solid rgba(59, 130, 246, 0.1);
  margin-bottom: 12px;
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.6) 0%, rgba(255, 255, 255, 0.9) 100%);
  min-height: 130px; /* 确保足够高度 */
  position: relative;
}

.system-announcement-item:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08) 0%, rgba(16, 185, 129, 0.08) 100%);
  transform: translateX(4px);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.15);
}

.system-announcement-item .announcement-title {
  font-weight: 600;
  font-size: 15px; /* 稍大标题 */
  line-height: 1.4;
  margin-bottom: 8px;
  color: #1a202c;
}

.system-announcement-item .announcement-content-preview {
  background: rgba(249, 250, 251, 0.8);
  border-left: 3px solid #3b82f6;
  font-size: 13px;
  line-height: 1.6;
  -webkit-line-clamp: 4; /* 系统公告允许更多行数 */
}

.announcement-time {
  font-size: 11px;
  color: #999;
}

.no-announcements {
  text-align: center;
  padding: 20px;
  color: #999;
}
</style>