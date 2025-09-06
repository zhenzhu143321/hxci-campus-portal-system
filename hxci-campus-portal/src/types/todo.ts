/**
 * 待办通知系统类型定义
 * 
 * @description 统一管理待办通知相关的TypeScript类型定义，支持Props驱动的组件系统
 * @author Frontend AI Assistant
 * @date 2025-08-15
 * @updated 2025-08-22 - 扩展支持Props驱动的待办通知组件系统 + 用户要求的基础类型定义
 * 
 * 🎯 第1层 - 基础类型定义已完成:
 * ✅ TodoItem - 用户要求的待办通知项数据结构
 * ✅ TodoFilterParams - 用户要求的过滤参数接口
 * ✅ UserRole - 6种用户角色枚举
 * ✅ TodoDisplayMode - 5种显示模式类型
 * ✅ TodoStats - 用户要求的统计接口
 * 
 * 📋 特性:
 * - 完整JSDoc注释
 * - 与现有notification类型兼容
 * - TypeScript严格模式
 * - 向后兼容性保持
 */

// 待办优先级类型 (支持数字和字符串两种模式)
export type TodoPriority = 'high' | 'medium' | 'low'
export type TodoPriorityLevel = 1 | 2 | 3 | 4

// 待办状态类型 (扩展支持viewed状态)
export type TodoStatus = 'pending' | 'completed' | 'overdue'
export type TodoStatusExtended = 'pending' | 'viewed' | 'completed'

// ================ Props驱动组件系统新增类型 ================

/**
 * 用户角色类型
 * @description 支持校园门户系统的6种角色类型
 */
export type UserRole = 
  | 'SYSTEM_ADMIN'    // 系统管理员
  | 'PRINCIPAL'       // 校长
  | 'ACADEMIC_ADMIN'  // 教务主任
  | 'TEACHER'         // 教师
  | 'CLASS_TEACHER'   // 班主任
  | 'STUDENT'         // 学生

/**
 * 待办显示模式类型
 * @description 支持多种展示形式的待办组件
 */
export type TodoDisplayMode = 
  | 'card'        // 卡片模式 - 适合首页展示
  | 'list'        // 列表模式 - 适合详细管理
  | 'compact'     // 紧凑模式 - 适合侧边栏
  | 'statistics'  // 统计模式 - 仅显示数量统计
  | 'calendar'    // 日历模式 - 按日期展示

/**
 * 范围类型
 * @description 待办通知的作用范围
 */
export type TodoScope = 
  | 'SCHOOL_WIDE'  // 全校范围
  | 'DEPARTMENT'   // 部门范围
  | 'GRADE'        // 年级范围
  | 'CLASS'        // 班级范围

/**
 * 用户要求的TodoItem接口 (第1层 - 基础类型定义)
 * @description Props驱动的待办通知组件的基础数据结构
 */
export interface TodoItem {
  /** 待办项唯一ID */
  id: number
  
  /** 待办标题 */
  title: string
  
  /** 待办内容描述 */
  content: string
  
  /** 优先级 (1=最高，4=最低) */
  priority: 1 | 2 | 3 | 4
  
  /** 当前状态 */
  status: 'pending' | 'viewed' | 'completed'
  
  /** 目标学生ID列表 (可选) */
  targetStudents?: string[]
  
  /** 目标年级列表 (可选) */
  targetGrades?: string[]
  
  /** 目标班级列表 (可选) */
  targetClasses?: string[]
  
  /** 可见角色列表 (可选) */
  visibleRoles?: UserRole[]
  
  /** 创建者 */
  createdBy: string
  
  /** 创建时间 (ISO格式字符串) */
  createTime: string
  
  /** 截止日期 (ISO格式字符串，可选) */
  dueDate?: string
  
  /** 分类标签 (可选) */
  category?: string
}

/**
 * 用户要求的TodoFilterParams接口 (第1层 - 基础类型定义)
 * @description 支持多维度过滤的参数配置
 */
export interface TodoFilterParams {
  /** 学生ID - 过滤指定学生的待办 */
  studentId?: string
  
  /** 年级 - 过滤指定年级的待办 */
  grade?: string
  
  /** 班级名称 - 过滤指定班级的待办 */
  className?: string
  
  /** 用户角色 - 过滤指定角色的待办 */
  userRole?: UserRole
  
  /** 是否显示已完成项 */
  showCompleted?: boolean
  
  /** 最大显示数量 */
  maxItems?: number
}

/**
 * 待办统计信息接口 (第1层 - 基础类型定义)
 * @description 待办项的统计数据
 */
export interface TodoStats {
  /** 总数 */
  total: number
  
  /** 待处理数量 */
  pending: number
  
  /** 已查看数量 */
  viewed: number
  
  /** 已完成数量 */
  completed: number
  
  /** 按优先级统计 */
  byPriority: {
    level1: number  // 最高优先级
    level2: number  // 高优先级
    level3: number  // 中优先级
    level4: number  // 低优先级
  }
  
  /** 按角色统计 */
  byRole: Record<UserRole, number>
}

/**
 * 扩展的待办通知项接口 (向后兼容)
 * @description 支持Props驱动组件的完整数据结构
 * @deprecated 建议使用上面的TodoItem接口
 */
export interface TodoItemExtended {
  /** 待办项唯一ID */
  id: number
  
  /** 待办标题 */
  title: string
  
  /** 待办内容描述 */
  content: string
  
  /** 通知级别 (固定为5级待办通知) */
  level: 5
  
  /** 优先级 (使用字符串类型，向后兼容) */
  priority: TodoPriority
  
  /** 截止日期 (ISO格式字符串) */
  dueDate: string
  
  /** 当前状态 (使用扩展状态类型) */
  status: TodoStatus
  
  /** 分配人姓名 */
  assignerName: string
  
  /** 分配人角色 */
  assignerRole: UserRole
  
  /** 作用范围 */
  scope: TodoScope
  
  /** 目标年级 (如果范围是GRADE或CLASS) */
  targetGrade?: string
  
  /** 目标班级 (如果范围是CLASS) */
  targetClass?: string
  
  /** 目标部门 (如果范围是DEPARTMENT) */
  targetDepartment?: string
  
  /** 是否已完成 */
  isCompleted: boolean
  
  /** 完成时间 (ISO格式字符串，可选) */
  completedAt?: string
  
  /** 创建时间 (ISO格式字符串) */
  createTime: string
  
  /** 更新时间 (ISO格式字符串) */
  updateTime: string
  
  /** 附加元数据 */
  metadata?: {
    /** 预计完成时间(分钟) */
    estimatedDuration?: number
    /** 关联课程/科目 */
    relatedSubject?: string
    /** 是否需要提交材料 */
    requiresSubmission?: boolean
    /** 提交文件类型限制 */
    allowedFileTypes?: string[]
  }
}

/**
 * 扩展的待办过滤参数接口 (向后兼容)
 * @description 支持多维度过滤的参数配置
 * @deprecated 建议使用上面的TodoFilterParams接口
 */
export interface TodoFilterParamsExtended {
  /** 用户ID - 过滤指定用户的待办 */
  userId?: string
  
  /** 用户角色 - 过滤指定角色的待办 */
  userRole?: UserRole
  
  /** 年级 - 过滤指定年级的待办 */
  grade?: string
  
  /** 班级 - 过滤指定班级的待办 */
  className?: string
  
  /** 部门 - 过滤指定部门的待办 */
  department?: string
  
  /** 优先级过滤 */
  priority?: TodoPriority | 'all'
  
  /** 状态过滤 */
  status?: TodoStatus | 'all'
  
  /** 作用范围过滤 */
  scope?: TodoScope | 'all'
  
  /** 截止日期范围过滤 */
  dueDateRange?: {
    start?: string  // ISO格式
    end?: string    // ISO格式
  }
  
  /** 搜索关键词 */
  searchKeyword?: string
  
  /** 分配人过滤 */
  assignerName?: string
  
  /** 是否包含已完成项 */
  includeCompleted?: boolean
  
  /** 是否包含逾期项 */
  includeOverdue?: boolean
}

/**
 * 待办统计信息接口
 * @description 完整的统计数据结构
 */
export interface TodoStats {
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
  
  /** 中优先级数量 */
  mediumPriority: number
  
  /** 低优先级数量 */
  lowPriority: number
  
  /** 按范围统计 */
  byScope: {
    schoolWide: number
    department: number
    grade: number
    class: number
  }
  
  /** 按角色统计 */
  byAssigner: {
    systemAdmin: number
    principal: number
    academicAdmin: number
    teacher: number
    classTeacher: number
    student: number
  }
  
  /** 今日截止数量 */
  dueToday: number
  
  /** 本周截止数量 */
  dueThisWeek: number
}

// ================ Props驱动组件接口定义 ================

/**
 * Props驱动待办组件的Props接口
 * @description 支持完全通过Props控制的待办通知组件
 */
export interface PropsBasedTodoProps {
  /** 用户信息 - 用于过滤和权限控制 */
  userInfo?: {
    userId: string
    role: UserRole
    grade?: string
    className?: string
    department?: string
  }
  
  /** 过滤参数 - 控制显示哪些待办 */
  filterParams?: TodoFilterParams
  
  /** 显示模式 - 控制组件的展示形式 */
  displayMode?: TodoDisplayMode
  
  /** 最大显示数量 - 限制显示的待办数量 */
  maxItems?: number
  
  /** 是否显示统计信息 */
  showStats?: boolean
  
  /** 是否显示操作按钮(标记完成等) */
  showActions?: boolean
  
  /** 是否显示优先级指示器 */
  showPriorityIndicator?: boolean
  
  /** 是否启用实时刷新 */
  enableAutoRefresh?: boolean
  
  /** 自动刷新间隔(秒) */
  autoRefreshInterval?: number
  
  /** 自定义CSS类名 */
  customClass?: string
  
  /** 自定义样式 */
  customStyle?: Record<string, any>
  
  /** 标题文字 */
  title?: string
  
  /** 是否显示空状态提示 */
  showEmptyState?: boolean
  
  /** 空状态提示文字 */
  emptyStateText?: string
  
  /** 是否只读模式(不允许交互) */
  readonly?: boolean
}

/**
 * Props驱动待办组件的Emits接口
 * @description 组件对外暴露的事件
 */
export interface PropsBasedTodoEmits {
  /** 待办项状态变更事件 */
  (event: 'todo-status-change', id: number, status: TodoStatus): void
  
  /** 待办项完成事件 */
  (event: 'todo-complete', id: number): void
  
  /** 待办项点击事件 */
  (event: 'todo-click', todo: TodoItem): void
  
  /** 更多操作点击事件 */
  (event: 'more-actions-click'): void
  
  /** 数据加载完成事件 */
  (event: 'data-loaded', todos: TodoItem[]): void
  
  /** 数据加载错误事件 */
  (event: 'data-error', error: string): void
  
  /** 统计数据更新事件 */
  (event: 'stats-updated', stats: TodoStats): void
}

// ================ 向后兼容性保持 ================

/**
 * 原有待办通知项接口 (保持向后兼容 + 第4-5层扩展)
 * @deprecated 建议使用新的 TodoItem 接口
 */
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
  
  /** 【第4层新增】目标学生ID列表 (JSON字符串格式) */
  targetStudentIds?: string | string[]
  
  /** 【第5层新增】目标年级列表 (JSON字符串格式) */
  targetGrades?: string | string[]
  
  /** 【第5层新增】目标班级列表 (JSON字符串格式) */
  targetClasses?: string | string[]
}

// 原有的组件接口定义(向后兼容)
export interface TodoNotificationItemProps {
  /** 待办通知项数据 */
  item: TodoNotificationItem
}

export interface TodoNotificationItemEmits {
  /** 状态变更事件 */
  (event: 'status-change', id: number, completed: boolean): void
}

export interface TodoFilterOptions {
  /** 【第4层新增】按学号过滤 - 核心功能 */
  studentId?: string
  
  /** 【第5层新增】按年级过滤 - 同年级所有班级学生都能看到 */
  grade?: string
  
  /** 【第5层新增】按班级过滤 - 只有同班学生能看到 */
  className?: string
  
  /** 按优先级过滤 */
  priority?: TodoPriority | 'all'
  
  /** 按状态过滤 */
  status?: TodoStatus | 'all'
  
  /** 搜索关键词 */
  searchKeyword?: string
}

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

// ================ 工具类型和高级类型定义 ================

/**
 * 排序配置接口
 * @description 定义待办列表的排序规则
 */
export interface TodoSortConfig {
  /** 排序字段 */
  field: keyof TodoItem
  
  /** 排序顺序 */
  order: 'asc' | 'desc'
  
  /** 排序优先级(数字越小优先级越高) */
  priority: number
}

/**
 * 分页配置接口
 * @description 支持分页显示的配置
 */
export interface TodoPaginationConfig {
  /** 当前页码(从1开始) */
  currentPage: number
  
  /** 每页显示数量 */
  pageSize: number
  
  /** 是否显示分页控件 */
  showPagination: boolean
  
  /** 总数据量 */
  total?: number
}

/**
 * 数据源配置接口
 * @description 定义数据获取的配置
 */
export interface TodoDataSourceConfig {
  /** 数据源类型 */
  type: 'api' | 'static' | 'mock'
  
  /** API配置 */
  apiConfig?: {
    /** API端点 */
    endpoint: string
    /** 请求方法 */
    method: 'GET' | 'POST'
    /** 请求参数 */
    params?: Record<string, any>
    /** 请求头 */
    headers?: Record<string, string>
  }
  
  /** 静态数据 */
  staticData?: TodoItem[]
  
  /** 缓存配置 */
  cacheConfig?: {
    /** 是否启用缓存 */
    enabled: boolean
    /** 缓存过期时间(毫秒) */
    expireTime: number
    /** 缓存键前缀 */
    keyPrefix: string
  }
}

/**
 * 主题配置接口
 * @description 自定义组件主题配置
 */
export interface TodoThemeConfig {
  /** 主色调 */
  primaryColor?: string
  
  /** 优先级颜色配置 */
  priorityColors?: {
    high: string
    medium: string
    low: string
  }
  
  /** 状态颜色配置 */
  statusColors?: {
    pending: string
    completed: string
    overdue: string
  }
  
  /** 字体配置 */
  typography?: {
    titleSize?: string
    contentSize?: string
    fontFamily?: string
  }
  
  /** 间距配置 */
  spacing?: {
    padding?: string
    margin?: string
    gap?: string
  }
  
  /** 边框配置 */
  border?: {
    radius?: string
    width?: string
    color?: string
  }
}

// ================ 工具类型别名 ================

/** 待办项ID类型 */
export type TodoId = TodoItem['id']

/** 部分待办项类型(用于更新操作) */
export type PartialTodoItem = Partial<TodoItem> & { id: TodoId }

/** 待办项创建类型(排除自动生成字段) */
export type CreateTodoItem = Omit<TodoItem, 'id' | 'createTime' | 'updateTime' | 'isCompleted' | 'completedAt'>

/** 待办项更新类型(排除不可变字段) */
export type UpdateTodoItem = Partial<Omit<TodoItem, 'id' | 'createTime'>>

/** 待办过滤函数类型 */
export type TodoFilterFunction = (todo: TodoItem) => boolean

/** 待办排序函数类型 */
export type TodoSortFunction = (a: TodoItem, b: TodoItem) => number

/** 待办转换函数类型(用于数据格式转换) */
export type TodoTransformFunction<T = any> = (input: T) => TodoItem

// ================ 常量定义 ================

/** 默认显示模式 */
export const DEFAULT_DISPLAY_MODE: TodoDisplayMode = 'card'

/** 默认最大显示数量 */
export const DEFAULT_MAX_ITEMS = 10

/** 默认自动刷新间隔(秒) */
export const DEFAULT_AUTO_REFRESH_INTERVAL = 30

/** 优先级权重映射(用于排序) */
export const PRIORITY_WEIGHTS: Record<TodoPriority, number> = {
  high: 3,
  medium: 2,
  low: 1
}

/** 状态权重映射(用于排序) */
export const STATUS_WEIGHTS: Record<TodoStatus, number> = {
  overdue: 3,
  pending: 2,
  completed: 1
}