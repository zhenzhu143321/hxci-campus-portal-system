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
import { updateTodoStatusApi, removeTodoApi } from '@/api/todo'

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
  
  /** 本地完成状态缓存键前缀 */
  const LOCAL_STORAGE_KEY_PREFIX = 'todo-completed-'
  
  /** 本地状态缓存有效期（毫秒）*/
  const LOCAL_CACHE_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000 // 7天
  
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
   * 初始化待办数据 (修复版)
   * @description 从真实API加载待办通知数据，合并本地持久化状态
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
        let todos = convertBackendToFrontend(backendTodos)
        
        // 🔧 关键修复：合并本地持久化状态
        todos = mergeWithLocalState(todos)
        
        todoNotifications.value = todos
        lastUpdateTime.value = new Date()
        
        console.log('✅ [TodoStore] 真实API数据加载成功 (含本地状态合并)', {
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
      let mockTodos = getMockTodos()
      
      // 即使是Mock数据也需要合并本地状态
      mockTodos = mergeWithLocalState(mockTodos)
      
      todoNotifications.value = mockTodos
      lastUpdateTime.value = new Date()
      error.value = err instanceof Error ? err.message : '加载待办数据失败，使用本地数据'
      
      console.log('🔄 [TodoStore] Mock数据降级完成 (含本地状态)', {
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
   * 更新待办状态 (修复版)
   * @param id 待办项ID
   * @param completed 是否完成
   */
  const updateTodoStatus = async (id: number, completed: boolean) => {
    console.log('🔄 [TodoStore] 更新待办状态:', { id, completed })
    
    // 🔧 关键修复：先更新本地持久化状态
    persistTodoState(id, completed)
    
    try {
      // 🌐 真实API调用
      const response = await api.post(`/admin-api/test/todo-new/api/${id}/complete`, {
        completed
      })
      
      if (response.data.code === 0) {
        // 更新本地内存状态
        const todo = todoNotifications.value.find(item => item.id === id)
        if (todo) {
          todo.isCompleted = completed
          todo.status = completed ? 'completed' : 'pending'
          lastUpdateTime.value = new Date()
          
          console.log('✅ [TodoStore] 待办状态更新成功 (API + 本地持久化):', {
            id: todo.id,
            title: todo.title,
            status: todo.status,
            completed: todo.isCompleted
          })
        }
      } else {
        // API失败时回滚本地持久化状态
        persistTodoState(id, !completed)
        throw new Error(response.data.msg || '更新失败')
      }
      
    } catch (err) {
      // 🛡️ 降级到本地更新 (保持本地持久化状态)
      console.warn('⚠️ [TodoStore] API更新失败，使用本地更新:', err)
      
      const todo = todoNotifications.value.find(item => item.id === id)
      if (todo) {
        todo.isCompleted = completed
        todo.status = completed ? 'completed' : 'pending'
        lastUpdateTime.value = new Date()
        
        console.log('🔄 [TodoStore] 本地状态更新完成 (含持久化):', {
          id: todo.id,
          title: todo.title,
          status: todo.status,
          completed: todo.isCompleted,
          persistedLocally: true
        })
      } else {
        console.warn('⚠️ [TodoStore] 未找到ID为', id, '的待办项')
      }
    }
  }

  /**
   * 标记待办为已完成 (便捷方法)
   * @param id 待办项ID
   */
  const markAsCompleted = async (id: number) => {
    console.log('✅ [TodoStore] 标记待办为已完成:', id)
    return await updateTodoStatus(id, true)
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
  const removeTodo = async (id: number) => {
    console.log('🗑️ [TodoStore] 开始删除待办项:', id)

    const todo = todoNotifications.value.find(item => item.id === id)
    if (!todo) {
      console.warn('⚠️ [TodoStore] 待办项不存在:', id)
      return
    }

    try {
      // 🌐 调用删除API
      const response = await removeTodoApi(id)

      if (response.code === 0) {
        // API成功，从本地列表中移除
        const index = todoNotifications.value.findIndex(item => item.id === id)
        if (index > -1) {
          todoNotifications.value.splice(index, 1)
          lastUpdateTime.value = new Date()
        }

        console.log('✅ [TodoStore] 待办项删除成功 (API + 本地):', {
          id: todo.id,
          title: todo.title
        })
      } else {
        throw new Error(response.msg || '删除失败')
      }

    } catch (err) {
      // 🛡️ API失败时的降级处理
      console.warn('⚠️ [TodoStore] API删除失败，执行本地删除:', err)

      // 仍然从本地列表中移除（降级策略）
      const index = todoNotifications.value.findIndex(item => item.id === id)
      if (index > -1) {
        todoNotifications.value.splice(index, 1)
        lastUpdateTime.value = new Date()

        console.log('🔄 [TodoStore] 本地删除完成 (降级模式):', {
          id: todo.id,
          title: todo.title,
          localOnly: true
        })
      }

      // 可以在这里添加用户通知，告知删除可能未同步到服务器
      throw err
    }
  }
  
  /**
   * 根据条件过滤待办项 (增强版 - 支持学号/年级/班级过滤)
   * @param options 过滤选项
   */
  const getFilteredTodos = (options: TodoFilterOptions) => {
    let filtered = [...todoNotifications.value]
    
    console.log('🔍 [TodoStore] 开始过滤待办数据:', {
      原始数量: filtered.length,
      过滤条件: options
    })
    
    // 🎯 【第4层核心功能】按学号过滤 - 精确匹配目标学生
    if (options.studentId && options.studentId.trim()) {
      const targetStudentId = options.studentId.trim()
      
      filtered = filtered.filter(todo => {
        // 从后端获取目标学生ID列表（JSON字符串格式）
        if (todo.targetStudentIds) {
          try {
            let targetIds: string[] = []
            
            // 处理不同的数据格式
            if (typeof todo.targetStudentIds === 'string') {
              // JSON字符串格式: '["2023010105", "2023010106"]'
              targetIds = JSON.parse(todo.targetStudentIds)
            } else if (Array.isArray(todo.targetStudentIds)) {
              // 已经是数组格式
              targetIds = todo.targetStudentIds
            }
            
            const isMatched = targetIds.includes(targetStudentId)
            
            console.log('🎯 [TodoStore] 学号过滤检查:', {
              待办ID: todo.id,
              待办标题: todo.title,
              目标学生列表: targetIds,
              查询学号: targetStudentId,
              匹配结果: isMatched
            })
            
            return isMatched
          } catch (error) {
            console.warn('⚠️ [TodoStore] 解析目标学生ID失败:', {
              待办ID: todo.id,
              targetStudentIds: todo.targetStudentIds,
              错误: error
            })
            return false
          }
        }
        
        // 如果没有目标学生信息，则不匹配
        console.log('⚠️ [TodoStore] 待办缺少目标学生信息:', {
          待办ID: todo.id,
          待办标题: todo.title
        })
        return false
      })
      
      console.log('🎯 [TodoStore] 学号过滤完成:', {
        查询学号: targetStudentId,
        过滤后数量: filtered.length
      })
    }
    
    // 🎯 【第5层新增功能】按年级过滤 - 同年级所有班级学生都能看到
    if (options.grade && options.grade.trim()) {
      const targetGrade = options.grade.trim()
      
      filtered = filtered.filter(todo => {
        // 从后端获取目标年级列表（JSON字符串格式）
        if (todo.targetGrades) {
          try {
            let targetGrades: string[] = []
            
            // 处理不同的数据格式
            if (typeof todo.targetGrades === 'string') {
              // JSON字符串格式: '["2023级", "2024级"]'
              targetGrades = JSON.parse(todo.targetGrades)
            } else if (Array.isArray(todo.targetGrades)) {
              // 已经是数组格式
              targetGrades = todo.targetGrades
            }
            
            const isMatched = targetGrades.includes(targetGrade)
            
            console.log('🎓 [TodoStore] 年级过滤检查:', {
              待办ID: todo.id,
              待办标题: todo.title,
              目标年级列表: targetGrades,
              查询年级: targetGrade,
              匹配结果: isMatched
            })
            
            return isMatched
          } catch (error) {
            console.warn('⚠️ [TodoStore] 解析目标年级失败:', {
              待办ID: todo.id,
              targetGrades: todo.targetGrades,
              错误: error
            })
            return false
          }
        }
        
        // 如果没有年级信息，则不匹配
        return false
      })
      
      console.log('🎓 [TodoStore] 年级过滤完成:', {
        查询年级: targetGrade,
        过滤后数量: filtered.length
      })
    }
    
    // 🎯 【第5层新增功能】按班级过滤 - 只有同班学生能看到
    if (options.className && options.className.trim()) {
      const targetClassName = options.className.trim()
      
      filtered = filtered.filter(todo => {
        // 从后端获取目标班级列表（JSON字符串格式）
        if (todo.targetClasses) {
          try {
            let targetClasses: string[] = []
            
            // 处理不同的数据格式
            if (typeof todo.targetClasses === 'string') {
              // JSON字符串格式: '["计算机1班", "计算机2班"]'
              targetClasses = JSON.parse(todo.targetClasses)
            } else if (Array.isArray(todo.targetClasses)) {
              // 已经是数组格式
              targetClasses = todo.targetClasses
            }
            
            const isMatched = targetClasses.includes(targetClassName)
            
            console.log('🏫 [TodoStore] 班级过滤检查:', {
              待办ID: todo.id,
              待办标题: todo.title,
              目标班级列表: targetClasses,
              查询班级: targetClassName,
              匹配结果: isMatched
            })
            
            return isMatched
          } catch (error) {
            console.warn('⚠️ [TodoStore] 解析目标班级失败:', {
              待办ID: todo.id,
              targetClasses: todo.targetClasses,
              错误: error
            })
            return false
          }
        }
        
        // 如果没有班级信息，则不匹配
        return false
      })
      
      console.log('🏫 [TodoStore] 班级过滤完成:', {
        查询班级: targetClassName,
        过滤后数量: filtered.length
      })
    }
    
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
    
    console.log('✅ [TodoStore] 最终过滤结果:', {
      原始数量: todoNotifications.value.length,
      过滤后数量: filtered.length,
      过滤条件: options
    })
    
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
  
  // ================== 本地持久化方法 (新增) ==================
  
  /**
   * 持久化待办状态到本地存储
   * @param todoId 待办项ID
   * @param completed 完成状态
   */
  const persistTodoState = (todoId: number, completed: boolean) => {
    try {
      const key = `${LOCAL_STORAGE_KEY_PREFIX}${todoId}`
      const stateData = {
        completed,
        timestamp: Date.now()
      }
      
      localStorage.setItem(key, JSON.stringify(stateData))
      
      console.log('💾 [TodoStore] 本地持久化成功:', {
        todoId,
        completed,
        key
      })
      
    } catch (error) {
      console.warn('⚠️ [TodoStore] 本地持久化失败:', error)
    }
  }
  
  /**
   * 从本地存储获取待办状态
   * @param todoId 待办项ID
   * @returns 完成状态或null
   */
  const getPersistedTodoState = (todoId: number): boolean | null => {
    try {
      const key = `${LOCAL_STORAGE_KEY_PREFIX}${todoId}`
      const stored = localStorage.getItem(key)
      
      if (!stored) {
        return null
      }
      
      const stateData = JSON.parse(stored)
      const now = Date.now()
      
      // 检查是否过期
      if (now - stateData.timestamp > LOCAL_CACHE_EXPIRE_TIME) {
        // 清理过期数据
        localStorage.removeItem(key)
        return null
      }
      
      return stateData.completed
      
    } catch (error) {
      console.warn('⚠️ [TodoStore] 读取本地持久化状态失败:', error)
      return null
    }
  }
  
  /**
   * 清理过期的本地持久化状态
   */
  const cleanExpiredLocalStates = () => {
    try {
      const now = Date.now()
      
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && key.startsWith(LOCAL_STORAGE_KEY_PREFIX)) {
          const stored = localStorage.getItem(key)
          if (stored) {
            const stateData = JSON.parse(stored)
            if (now - stateData.timestamp > LOCAL_CACHE_EXPIRE_TIME) {
              localStorage.removeItem(key)
              console.log('🧹 [TodoStore] 清理过期本地状态:', key)
            }
          }
        }
      }
      
    } catch (error) {
      console.warn('⚠️ [TodoStore] 清理过期状态失败:', error)
    }
  }
  
  /**
   * 合并API数据与本地持久化状态 (核心修复方法)
   * @param apiTodos API返回的待办数据
   * @returns 合并后的待办数据
   */
  const mergeWithLocalState = (apiTodos: TodoNotificationItem[]): TodoNotificationItem[] => {
    console.log('🔀 [TodoStore] 开始合并本地持久化状态...')
    
    // 先清理过期的本地状态
    cleanExpiredLocalStates()
    
    let mergeCount = 0
    
    const mergedTodos = apiTodos.map(todo => {
      const localState = getPersistedTodoState(todo.id)
      
      if (localState !== null && localState !== todo.isCompleted) {
        // 本地状态与API状态不一致，优先使用本地状态
        console.log('🔀 [TodoStore] 合并本地状态:', {
          todoId: todo.id,
          title: todo.title,
          apiState: todo.isCompleted,
          localState: localState,
          finalState: localState
        })
        
        mergeCount++
        
        return {
          ...todo,
          isCompleted: localState,
          status: localState ? 'completed' : 'pending'
        }
      }
      
      return todo
    })
    
    console.log('✅ [TodoStore] 本地状态合并完成:', {
      总待办数: apiTodos.length,
      合并状态数: mergeCount
    })
    
    return mergedTodos
  }

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
   * 后端数据格式转换为前端格式 (支持第4-5层字段: targetStudentIds + targetGrades + targetClasses)
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
      isCompleted: item.isCompleted,
      targetStudentIds: item.targetStudentIds, // 【第4层】目标学生ID列表
      targetGrades: item.targetGrades, // 【第5层新增】目标年级列表
      targetClasses: item.targetClasses // 【第5层新增】目标班级列表
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
    markAsCompleted,
    addTodo,
    removeTodo,
    getFilteredTodos,
    clearError,
    refreshTodos,
    
    // 本地持久化方法 (新增)
    persistTodoState,
    getPersistedTodoState,
    cleanExpiredLocalStates,
    
    // 内部工具函数 (用于测试和调试)
    getMockTodos,
    convertBackendToFrontend,
    mergeWithLocalState
  }
})