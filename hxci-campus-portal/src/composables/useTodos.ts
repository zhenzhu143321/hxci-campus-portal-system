/**
 * 待办管理 Composable
 * 提供待办通知的UI层管理功能，作为Store和组件之间的桥梁
 */

import { ref, computed, onMounted, onUnmounted, type Ref, type ComputedRef } from 'vue'
import { storeToRefs } from 'pinia'
import { useTodoStore } from '@/stores/todo'
import type { TodoNotificationItem, TodoFilterOptions, TodoPriority, TodoStatus } from '@/types/todo'

// TypeScript 类型定义
export type UseTodosMode = 'homepage' | 'management'

export interface UseTodosSort {
  sortBy: 'priority' | 'dueDate' | 'status' | 'assignerName'
  order: 'asc' | 'desc'
}

export interface UseTodosFilters extends Partial<TodoFilterOptions> {
  showCompleted?: boolean
  maxItems?: number
  sort?: UseTodosSort
}

export interface UseTodosOptions {
  mode?: UseTodosMode
  autoInit?: boolean
  autoRefresh?: boolean
  refreshInterval?: number
  debug?: boolean
}

export interface UseTodosReturn {
  // 状态
  isLoading: Ref<boolean>
  error: Ref<string>
  lastUpdateTime: Ref<Date | null>
  detailVisible: Ref<boolean>
  selected: Ref<TodoNotificationItem | null>

  // 数据
  raw: ComputedRef<TodoNotificationItem[]>
  todos: ComputedRef<TodoNotificationItem[]>
  displayed: ComputedRef<TodoNotificationItem[]>
  pending: ComputedRef<TodoNotificationItem[]>
  overdue: ComputedRef<TodoNotificationItem[]>
  completed: ComputedRef<TodoNotificationItem[]>

  // 统计
  pendingCount: ComputedRef<number>
  overdueCount: ComputedRef<number>
  completedCount: ComputedRef<number>
  totalCount: ComputedRef<number>

  // 控制
  mode: Ref<UseTodosMode>
  filters: Ref<UseTodosFilters>

  // 操作方法
  initialize(): Promise<void>
  refresh(): Promise<void>
  markComplete(id: number, completed?: boolean): Promise<void>
  openDetail(item: TodoNotificationItem): void
  closeDetail(): void
  setFilters(partial: Partial<UseTodosFilters>): void
  setMode(next: UseTodosMode): void
  startAutoRefresh(): void
  stopAutoRefresh(): void
}

export function useTodos(options: UseTodosOptions = {}): UseTodosReturn {
  const {
    mode: initialMode = 'homepage',
    autoInit = true,
    autoRefresh = false,
    refreshInterval = 30000,
    debug = false
  } = options

  const todoStore = useTodoStore()
  const {
    todoNotifications,
    isLoading,
    error,
    lastUpdateTime,
    pendingTodos,
    completedTodos,
    overdueTodos
  } = storeToRefs(todoStore)

  // UI状态
  const mode = ref<UseTodosMode>(initialMode)
  const filters = ref<UseTodosFilters>({
    showCompleted: initialMode !== 'homepage',
    maxItems: initialMode === 'homepage' ? 5 : undefined,
    sort: { sortBy: 'priority', order: 'desc' }
  })

  const selected = ref<TodoNotificationItem | null>(null)
  const detailVisible = ref(false)
  const refreshTimer = ref<number | null>(null)
  const initialized = ref(false)

  // 原始数据
  const raw = computed(() => todoNotifications.value)

  // 过滤和排序逻辑
  const filteredBase = computed<TodoNotificationItem[]>(() => {
    // 使用Store的过滤方法处理服务端过滤
    const serverFilter: TodoFilterOptions = {
      studentId: filters.value.studentId,
      grade: filters.value.grade,
      className: filters.value.className,
      priority: filters.value.priority,
      status: filters.value.status
    }

    // 检查是否有服务端过滤条件
    const hasServerFilter = Object.values(serverFilter).some(v =>
      !!v && v !== 'all'
    )

    let list = hasServerFilter
      ? todoStore.getFilteredTodos(serverFilter)
      : [...raw.value]

    // 首页模式只显示待办（未完成）
    if (mode.value === 'homepage') {
      list = list.filter(item =>
        !item.isCompleted && item.status !== 'completed'
      )
    } else {
      // 管理模式根据showCompleted过滤
      if (filters.value.showCompleted === false) {
        list = list.filter(item => !item.isCompleted)
      }
    }

    // 排序
    if (filters.value.sort) {
      const { sortBy, order } = filters.value.sort
      const dir = order === 'desc' ? -1 : 1

      list.sort((a, b) => {
        switch (sortBy) {
          case 'priority': {
            // 优先级权重：high > medium > low
            const priorityWeight: Record<TodoPriority, number> = {
              high: 3,
              medium: 2,
              low: 1
            }
            return (priorityWeight[b.priority] - priorityWeight[a.priority]) * dir
          }
          case 'dueDate': {
            const dateA = new Date(a.dueDate).getTime()
            const dateB = new Date(b.dueDate).getTime()
            return (dateA - dateB) * dir
          }
          case 'status': {
            // 状态权重：overdue > pending > completed
            const statusWeight: Record<TodoStatus, number> = {
              overdue: 3,
              pending: 2,
              completed: 1
            }
            return (statusWeight[a.status] - statusWeight[b.status]) * dir
          }
          case 'assignerName':
            return a.assignerName.localeCompare(b.assignerName) * dir
          default:
            return 0
        }
      })
    }

    return list
  })

  // 应用maxItems限制
  const todos = computed(() => {
    const list = filteredBase.value
    if (filters.value.maxItems && filters.value.maxItems > 0) {
      return list.slice(0, filters.value.maxItems)
    }
    return list
  })

  // 显示的待办列表
  const displayed = computed(() => todos.value)

  // 分类数据（使用Store的计算属性）
  const pending = computed(() => pendingTodos.value)
  const overdue = computed(() => overdueTodos.value)
  const completed = computed(() => completedTodos.value)

  // 统计数据（基于显示的列表）
  const pendingCount = computed(() =>
    displayed.value.filter(i =>
      !i.isCompleted && i.status === 'pending'
    ).length
  )

  const overdueCount = computed(() =>
    displayed.value.filter(i =>
      !i.isCompleted && i.status === 'overdue'
    ).length
  )

  const completedCount = computed(() =>
    displayed.value.filter(i => i.isCompleted).length
  )

  const totalCount = computed(() => displayed.value.length)

  // 初始化方法
  async function initialize() {
    if (initialized.value) return

    if (debug) {
      console.log('[useTodos] 开始初始化...')
    }

    await todoStore.initializeTodos()
    initialized.value = true

    if (autoRefresh) {
      startAutoRefresh()
    }

    if (debug) {
      console.log('[useTodos] 初始化完成，待办数量:', raw.value.length)
    }
  }

  // 刷新数据
  async function refresh() {
    if (debug) {
      console.log('[useTodos] 刷新待办数据...')
    }
    await todoStore.refreshTodos()
  }

  // 标记完成状态
  async function markComplete(id: number, completed = true) {
    if (debug) {
      console.log('[useTodos] 标记待办完成状态:', { id, completed })
    }
    await todoStore.updateTodoStatus(id, completed)
  }

  // 打开详情
  function openDetail(item: TodoNotificationItem) {
    selected.value = item
    detailVisible.value = true
    if (debug) {
      console.log('[useTodos] 打开待办详情:', item.title)
    }
  }

  // 关闭详情
  function closeDetail() {
    detailVisible.value = false
    selected.value = null
  }

  // 设置过滤器
  function setFilters(partial: Partial<UseTodosFilters>) {
    filters.value = { ...filters.value, ...partial }
    if (debug) {
      console.log('[useTodos] 更新过滤器:', filters.value)
    }
  }

  // 设置模式
  function setMode(next: UseTodosMode) {
    mode.value = next

    // 根据模式调整默认过滤器
    if (next === 'homepage') {
      setFilters({
        showCompleted: false,
        maxItems: 5
      })
    } else {
      setFilters({
        showCompleted: true,
        maxItems: undefined
      })
    }

    if (debug) {
      console.log('[useTodos] 切换模式:', next)
    }
  }

  // 开始自动刷新
  function startAutoRefresh() {
    stopAutoRefresh()
    refreshTimer.value = window.setInterval(refresh, refreshInterval)
    if (debug) {
      console.log('[useTodos] 开始自动刷新，间隔:', refreshInterval, 'ms')
    }
  }

  // 停止自动刷新
  function stopAutoRefresh() {
    if (refreshTimer.value) {
      clearInterval(refreshTimer.value)
      refreshTimer.value = null
      if (debug) {
        console.log('[useTodos] 停止自动刷新')
      }
    }
  }

  // 生命周期钩子
  if (autoInit) {
    onMounted(initialize)
  }

  onUnmounted(() => {
    stopAutoRefresh()
    if (debug) {
      console.log('[useTodos] 组件卸载，清理资源')
    }
  })

  return {
    // 状态
    isLoading,
    error,
    lastUpdateTime,
    detailVisible,
    selected,

    // 数据
    raw,
    todos,
    displayed,
    pending,
    overdue,
    completed,

    // 统计
    pendingCount,
    overdueCount,
    completedCount,
    totalCount,

    // 控制
    mode,
    filters,

    // 操作方法
    initialize,
    refresh,
    markComplete,
    openDetail,
    closeDetail,
    setFilters,
    setMode,
    startAutoRefresh,
    stopAutoRefresh
  }
}

export default useTodos