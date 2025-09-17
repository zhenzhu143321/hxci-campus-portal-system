/**
 * 待办通知API接口
 * 
 * @description 提供待办通知的完整API调用服务
 * @author Frontend AI Assistant
 * @date 2025-08-23
 */

import api from '@/utils/request'
import type { TodoNotificationItem, TodoPriority, TodoStatus } from '@/types/todo'

/**
 * 待办发布请求参数接口
 */
export interface PublishTodoRequest {
  title: string
  content: string
  priority: TodoPriority
  dueDate: string
  targetScope: 'SCHOOL_WIDE' | 'DEPARTMENT' | 'GRADE' | 'CLASS'
  targetStudentIds?: string[]      // 【第4层】目标学生ID列表
  targetGradeIds?: string[]        // 【第5层】目标年级ID列表  
  targetClassIds?: string[]        // 【第5层】目标班级ID列表
  targetDepartmentIds?: string[]   // 目标部门ID列表
}

/**
 * 待办列表查询参数接口
 */
export interface TodoListQuery {
  page?: number
  pageSize?: number
  status?: TodoStatus | 'all'
  priority?: TodoPriority | 'all'
  studentId?: string     // 【第4层】学号过滤
  grade?: string         // 【第5层】年级过滤
  className?: string     // 【第5层】班级过滤
}

/**
 * 待办API响应数据接口
 */
export interface TodoApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

/**
 * 待办列表响应数据接口
 */
export interface TodoListResponse {
  todos: TodoNotificationItem[]
  pagination: {
    current: number
    pageSize: number
    total: number
    totalPages: number
  }
  user: {
    username: string
    roleCode: string
    roleName: string
  }
  timestamp: number
}

// ================== API方法定义 ==================

/**
 * 获取我的待办列表
 * @description 支持分页、状态过滤、优先级过滤、学号/年级/班级精确过滤
 */
export const getMyTodoList = async (params: TodoListQuery = {}): Promise<TodoApiResponse<TodoListResponse>> => {
  console.log('🌐 [TodoAPI] 调用获取待办列表API:', params)
  
  const queryParams = new URLSearchParams()
  
  // 基础分页参数
  if (params.page) queryParams.append('page', params.page.toString())
  if (params.pageSize) queryParams.append('pageSize', params.pageSize.toString())
  
  // 过滤参数
  if (params.status && params.status !== 'all') queryParams.append('status', params.status)
  if (params.priority && params.priority !== 'all') queryParams.append('priority', params.priority)
  
  // 【第4-5层】精确过滤参数
  if (params.studentId) queryParams.append('studentId', params.studentId)
  if (params.grade) queryParams.append('grade', params.grade)
  if (params.className) queryParams.append('className', params.className)
  
  const response = await api.get(`/admin-api/test/todo-new/api/my-list?${queryParams.toString()}`)
  
  console.log('✅ [TodoAPI] 待办列表API响应:', response.data)
  return response.data
}

/**
 * 发布待办通知
 * @description 支持完整的目标定向参数，确保精确权限控制
 */
export const publishTodoNotification = async (data: PublishTodoRequest): Promise<TodoApiResponse> => {
  console.log('🌐 [TodoAPI] 调用发布待办API:', data)
  
  // 🎯 **关键修复**: 确保目标定向字段完整传递
  const requestData = {
    title: data.title,
    content: data.content,
    priority: data.priority,
    dueDate: data.dueDate,
    targetScope: data.targetScope,
    // **目标定向字段** - 修复权限过滤缺陷
    targetStudentIds: data.targetStudentIds || [],
    targetGradeIds: data.targetGradeIds || [],
    targetClassIds: data.targetClassIds || [],
    targetDepartmentIds: data.targetDepartmentIds || []
  }
  
  console.log('🎯 [TodoAPI] 发布请求数据 (含目标定向):', requestData)
  
  const response = await api.post('/admin-api/test/todo-new/api/publish', requestData)
  
  console.log('✅ [TodoAPI] 发布待办API响应:', response.data)
  return response.data
}

/**
 * 标记待办完成
 * @param id 待办项ID
 */
export const completeTodoNotification = async (id: number): Promise<TodoApiResponse> => {
  console.log('🌐 [TodoAPI] 调用完成待办API:', id)
  
  const response = await api.post(`/admin-api/test/todo-new/api/${id}/complete`, {
    completed: true
  })
  
  console.log('✅ [TodoAPI] 完成待办API响应:', response.data)
  return response.data
}

/**
 * 更新待办状态
 * @param id 待办项ID
 * @param completed 是否完成
 */
export const updateTodoStatusApi = async (id: number, completed: boolean): Promise<TodoApiResponse> => {
  console.log('🌐 [TodoAPI] 调用更新待办状态API:', { id, completed })

  const response = await api.post(`/admin-api/test/todo-new/api/${id}/complete`, {
    completed
  })

  console.log('✅ [TodoAPI] 更新待办状态API响应:', response.data)
  return response.data
}

/**
 * 删除待办项
 * @param id 待办项ID
 */
export const removeTodoApi = async (id: number): Promise<TodoApiResponse> => {
  console.log('🌐 [TodoAPI] 调用删除待办API:', id)

  const response = await api.delete(`/admin-api/test/todo-new/api/${id}`)

  console.log('✅ [TodoAPI] 删除待办API响应:', response.data)
  return response.data
}

/**
 * 获取待办统计
 * @param id 待办项ID
 */
export const getTodoStats = async (id: number): Promise<TodoApiResponse> => {
  console.log('🌐 [TodoAPI] 调用待办统计API:', id)

  const response = await api.get(`/admin-api/test/todo-new/api/${id}/stats`)

  console.log('✅ [TodoAPI] 待办统计API响应:', response.data)
  return response.data
}

/**
 * Ping测试接口
 * @description 测试待办服务连通性
 */
export const pingTodoService = async (): Promise<TodoApiResponse> => {
  console.log('🌐 [TodoAPI] 调用待办服务Ping测试')
  
  const response = await api.get('/admin-api/test/todo-new/api/ping')
  
  console.log('✅ [TodoAPI] Ping测试响应:', response.data)
  return response.data
}

// ================== 快速发布方法 ==================

/**
 * 快速发布全校待办
 * @description 便捷方法，用于发布全校范围的待办通知
 */
export const publishSchoolWideTodo = async (data: {
  title: string
  content: string
  priority: TodoPriority
  dueDate: string
}): Promise<TodoApiResponse> => {
  return await publishTodoNotification({
    ...data,
    targetScope: 'SCHOOL_WIDE'
  })
}

/**
 * 快速发布班级待办
 * @description 便捷方法，用于发布特定班级的待办通知
 */
export const publishClassTodo = async (data: {
  title: string
  content: string
  priority: TodoPriority
  dueDate: string
  targetClassIds: string[]
  targetStudentIds?: string[]
}): Promise<TodoApiResponse> => {
  return await publishTodoNotification({
    ...data,
    targetScope: 'CLASS'
  })
}

/**
 * 快速发布年级待办
 * @description 便捷方法，用于发布特定年级的待办通知
 */
export const publishGradeTodo = async (data: {
  title: string
  content: string
  priority: TodoPriority
  dueDate: string
  targetGradeIds: string[]
}): Promise<TodoApiResponse> => {
  return await publishTodoNotification({
    ...data,
    targetScope: 'GRADE'
  })
}

/**
 * 快速发布部门待办
 * @description 便捷方法，用于发布特定部门的待办通知
 */
export const publishDepartmentTodo = async (data: {
  title: string
  content: string
  priority: TodoPriority
  dueDate: string
  targetDepartmentIds: string[]
}): Promise<TodoApiResponse> => {
  return await publishTodoNotification({
    ...data,
    targetScope: 'DEPARTMENT'
  })
}

// ================== 导出默认对象 ==================

export default {
  // 基础API方法
  getMyTodoList,
  publishTodoNotification,
  completeTodoNotification,
  updateTodoStatusApi,
  removeTodoApi,
  getTodoStats,
  pingTodoService,
  
  // 快速发布方法
  publishSchoolWideTodo,
  publishClassTodo,
  publishGradeTodo,
  publishDepartmentTodo
}