/**
 * 待办通知状态管理
 * 
 * @description 使用Pinia管理待办通知的全局状态，确保数据一致性
 * @author Frontend AI Assistant
 * @date 2025-08-15
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TodoNotificationItem, TodoPriority, TodoStatus, TodoFilterOptions, TodoStatistics } from '@/types/todo'
import api from '@/utils/request'

export const useTodoStore = defineStore('todo', () => {
  // ================== 状态定义 ==================
  
  /** 待办通知列表 */
  const todoNotifications = ref<TodoNotificationItem[]>([])
  
  /** 数据加载状态 */
  const isLoading = ref<boolean>(false)
  
  /** 错误信息 */
  const error = ref<string>('')
  
  /** 最后更新时间 */
  const lastUpdateTime = ref<Date | null>(null)
  
  // ================== 计算属性 (Getters) ==================
  
  /** 待处理的待办事项 */
  const pendingTodos = computed(() => 
    todoNotifications.value.filter(todo => !todo.isCompleted && todo.status !== 'overdue')
  )
  
  /** 已完成的待办事项 */
  const completedTodos = computed(() => 
    todoNotifications.value.filter(todo => todo.isCompleted)
  )
  
  /** 逾期的待办事项 */
  const overdueTodos = computed(() => 
    todoNotifications.value.filter(todo => !todo.isCompleted && todo.status === 'overdue')
  )
  
  /** 高优先级待办事项 */
  const highPriorityTodos = computed(() => 
    todoNotifications.value.filter(todo => todo.priority === 'high' && !todo.isCompleted)
  )
  
  /** 待办统计信息 */
  const statistics = computed((): TodoStatistics => ({
    total: todoNotifications.value.length,
    pending: pendingTodos.value.length,
    completed: completedTodos.value.length,
    overdue: overdueTodos.value.length,
    highPriority: highPriorityTodos.value.length
  }))
  
  /** 待处理数量 (用于首页显示) */
  const pendingCount = computed(() => pendingTodos.value.length)
  
  /** 逾期数量 (用于首页显示) */
  const overdueCount = computed(() => overdueTodos.value.length)
  
  // ================== 操作方法 (Actions) ==================
  
  /**
   * 初始化待办数据
   * @description 从真实API加载待办通知数据，降级到Mock数据
   */
  const initializeTodos = async () => {
    console.log('📋 [TodoStore] 初始化待办数据...')
    isLoading.value = true
    error.value = ''
    
    try {
      // 🔄 真实API调用
      console.log('🌐 [TodoStore] 尝试调用真实API...')
      const response = await api.get('/admin-api/test/todo-new/api/my-list')
      
      if (response.data.code === 0 && response.data.data?.todos) {
        // 转换后端数据格式为前端格式
        const backendTodos = response.data.data.todos
        todoNotifications.value = convertBackendToFrontend(backendTodos)
        lastUpdateTime.value = new Date()
        
        console.log('✅ [TodoStore] 真实API数据加载成功', {
          总数: todoNotifications.value.length,
          待处理: pendingTodos.value.length,
          逾期: overdueTodos.value.length,
          已完成: completedTodos.value.length
        })
        
        console.log('📊 [TodoStore] API返回的原始数据:', backendTodos)
        console.log('🔄 [TodoStore] 转换后的前端数据:', todoNotifications.value)
        
      } else {
        throw new Error(`API返回格式错误: ${response.data.msg || '未知错误'}`)
      }
      
    } catch (err) {
      // 🛡️ 降级到Mock数据
      console.warn('⚠️ [TodoStore] API调用失败，使用Mock数据降级', err)
      todoNotifications.value = getMockTodos()
      lastUpdateTime.value = new Date()
      error.value = err instanceof Error ? err.message : '加载待办数据失败，使用本地数据'
      
      console.log('🔄 [TodoStore] Mock数据降级完成', {
        总数: todoNotifications.value.length,
        待处理: pendingTodos.value.length,
        逾期: overdueTodos.value.length,
        已完成: completedTodos.value.length
      })
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 更新待办状态
   * @param id 待办项ID
   * @param completed 是否完成
   */
  const updateTodoStatus = async (id: number, completed: boolean) => {
    console.log('🔄 [TodoStore] 更新待办状态:', { id, completed })
    
    try {
      // 🌐 真实API调用
      const response = await api.post(`/admin-api/test/todo-new/api/${id}/complete`, {
        completed
      })
      
      if (response.data.code === 0) {
        // 更新本地状态
        const todo = todoNotifications.value.find(item => item.id === id)
        if (todo) {
          todo.isCompleted = completed
          todo.status = completed ? 'completed' : 'pending'
          lastUpdateTime.value = new Date()
          
          console.log('✅ [TodoStore] 待办状态更新成功 (API):', {
            id: todo.id,
            title: todo.title,
            status: todo.status,
            completed: todo.isCompleted
          })
        }
      } else {
        throw new Error(response.data.msg || '更新失败')
      }
      
    } catch (err) {
      // 🛡️ 降级到本地更新
      console.warn('⚠️ [TodoStore] API更新失败，使用本地更新:', err)
      
      const todo = todoNotifications.value.find(item => item.id === id)
      if (todo) {
        todo.isCompleted = completed
        todo.status = completed ? 'completed' : 'pending'
        lastUpdateTime.value = new Date()
        
        console.log('🔄 [TodoStore] 本地状态更新完成:', {
          id: todo.id,
          title: todo.title,
          status: todo.status,
          completed: todo.isCompleted
        })
      } else {
        console.warn('⚠️ [TodoStore] 未找到ID为', id, '的待办项')
      }
    }
  }
  
  /**
   * 添加新的待办项
   * @param todo 新的待办项数据
   */
  const addTodo = (todo: Omit<TodoNotificationItem, 'id'>) => {
    const newId = Math.max(...todoNotifications.value.map(t => t.id), 0) + 1
    const newTodo: TodoNotificationItem = {
      ...todo,
      id: newId
    }
    
    todoNotifications.value.unshift(newTodo)
    lastUpdateTime.value = new Date()
    
    console.log('➕ [TodoStore] 新增待办项:', newTodo.title)
  }
  
  /**
   * 删除待办项
   * @param id 待办项ID
   */
  const removeTodo = (id: number) => {
    const index = todoNotifications.value.findIndex(item => item.id === id)
    if (index > -1) {
      const todo = todoNotifications.value[index]
      todoNotifications.value.splice(index, 1)
      lastUpdateTime.value = new Date()
      
      console.log('🗑️ [TodoStore] 删除待办项:', todo.title)
    }
  }
  
  /**
   * 根据条件过滤待办项
   * @param options 过滤选项
   */
  const getFilteredTodos = (options: TodoFilterOptions) => {
    let filtered = [...todoNotifications.value]
    
    // 按优先级过滤
    if (options.priority && options.priority !== 'all') {
      filtered = filtered.filter(todo => todo.priority === options.priority)
    }
    
    // 按状态过滤
    if (options.status && options.status !== 'all') {
      filtered = filtered.filter(todo => todo.status === options.status)
    }
    
    // 搜索关键词过滤
    if (options.searchKeyword && options.searchKeyword.trim()) {
      const keyword = options.searchKeyword.toLowerCase()
      filtered = filtered.filter(todo => 
        todo.title.toLowerCase().includes(keyword) ||
        todo.content.toLowerCase().includes(keyword) ||
        todo.assignerName.toLowerCase().includes(keyword)
      )
    }
    
    return filtered
  }
  
  /**
   * 清空错误信息
   */
  const clearError = () => {
    error.value = ''
  }
  
  /**
   * 刷新数据
   */
  const refreshTodos = async () => {
    console.log('🔄 [TodoStore] 刷新待办数据...')
    await initializeTodos()
  }
  
  // ================== 导出 ==================
  
  // ================== 内部工具函数 ==================
  
  /**
   * 获取Mock数据
   * @description 提供降级时使用的模拟数据
   */
  const getMockTodos = (): TodoNotificationItem[] => {
    return [
      {
        id: 1,
        title: '期末考试安排确认',
        content: '请确认您所授课程的期末考试时间和地点安排，并及时通知学生。',
        level: 5,
        priority: 'high',
        dueDate: '2025-08-20',
        status: 'pending',
        assignerName: '教务处',
        isCompleted: false
      },
      {
        id: 2,
        title: '学生成绩录入',
        content: '请在系统中录入本学期学生成绩，截止时间为8月25日。',
        level: 5,
        priority: 'high',
        dueDate: '2025-08-25',
        status: 'pending',
        assignerName: '教务处',
        isCompleted: false
      },
      {
        id: 3,
        title: '教研室会议准备',
        content: '下周三下午2点教研室会议，请准备好本月工作总结。',
        level: 5,
        priority: 'medium',
        dueDate: '2025-08-21',
        status: 'pending',
        assignerName: '张教授',
        isCompleted: false
      },
      {
        id: 4,
        title: '课件更新',
        content: '根据教学大纲调整，请更新下学期的课件内容。',
        level: 5,
        priority: 'medium',
        dueDate: '2025-08-30',
        status: 'overdue',
        assignerName: '教务处',
        isCompleted: false
      },
      {
        id: 5,
        title: '实验室设备检查',
        content: '开学前需要检查实验室所有设备的运行状态。',
        level: 5,
        priority: 'low',
        dueDate: '2025-08-28',
        status: 'completed',
        assignerName: '实验室管理员',
        isCompleted: true
      }
    ]
  }
  
  /**
   * 后端数据格式转换为前端格式
   * @param backendData 后端返回的待办数据
   */
  const convertBackendToFrontend = (backendData: any[]): TodoNotificationItem[] => {
    return backendData.map(item => ({
      id: parseInt(item.id.toString()),
      title: item.title,
      content: item.content,
      level: 5, // 固定Level 5
      priority: convertPriorityFromBackend(item.priority),
      dueDate: item.dueDate,
      status: item.status as TodoStatus,
      assignerName: item.assignerName,
      isCompleted: item.isCompleted
    }))
  }
  
  /**
   * 优先级转换: backend -> frontend
   * @param priority 后端优先级 ("high"|"medium"|"low")
   */
  const convertPriorityFromBackend = (priority: string): TodoPriority => {
    switch (priority) {
      case 'high': return 'high'
      case 'medium': return 'medium'
      case 'low': return 'low'
      default: return 'medium'
    }
  }
  
  return {
    // 状态
    todoNotifications,
    isLoading,
    error,
    lastUpdateTime,
    
    // 计算属性
    pendingTodos,
    completedTodos,
    overdueTodos,
    highPriorityTodos,
    statistics,
    pendingCount,
    overdueCount,
    
    // 操作方法
    initializeTodos,
    updateTodoStatus,
    addTodo,
    removeTodo,
    getFilteredTodos,
    clearError,
    refreshTodos,
    
    // 内部工具函数 (用于测试和调试)
    getMockTodos,
    convertBackendToFrontend
  }
})