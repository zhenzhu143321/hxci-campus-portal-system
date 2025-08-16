/**
 * 待办通知系统类型定义
 * 
 * @description 统一管理待办通知相关的TypeScript类型定义
 * @author Frontend AI Assistant
 * @date 2025-08-15
 */

// 待办优先级类型
export type TodoPriority = 'high' | 'medium' | 'low'

// 待办状态类型
export type TodoStatus = 'pending' | 'completed' | 'overdue'

// 待办通知项接口
export interface TodoNotificationItem {
  /** 待办项唯一ID */
  id: number
  
  /** 待办标题 */
  title: string
  
  /** 待办内容描述 */
  content: string
  
  /** 通知级别 (固定为5级待办通知) */
  level: 5
  
  /** 优先级 */
  priority: TodoPriority
  
  /** 截止日期 (ISO格式字符串) */
  dueDate: string
  
  /** 当前状态 */
  status: TodoStatus
  
  /** 分配人姓名 */
  assignerName: string
  
  /** 是否已完成 */
  isCompleted: boolean
}

// 待办通知组件Props接口
export interface TodoNotificationItemProps {
  /** 待办通知项数据 */
  item: TodoNotificationItem
}

// 待办通知组件Emits接口
export interface TodoNotificationItemEmits {
  /** 状态变更事件 */
  (event: 'status-change', id: number, completed: boolean): void
}

// 待办通知列表过滤选项
export interface TodoFilterOptions {
  /** 按优先级过滤 */
  priority?: TodoPriority | 'all'
  
  /** 按状态过滤 */
  status?: TodoStatus | 'all'
  
  /** 搜索关键词 */
  searchKeyword?: string
}

// 待办统计信息接口
export interface TodoStatistics {
  /** 总数 */
  total: number
  
  /** 待处理数量 */
  pending: number
  
  /** 已完成数量 */
  completed: number
  
  /** 逾期数量 */
  overdue: number
  
  /** 高优先级数量 */
  highPriority: number
}