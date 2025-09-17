/**
 * 新闻模块类型定义
 *
 * @description 校园新闻相关的TypeScript类型定义
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

/**
 * 新闻项数据结构
 */
export interface NewsItem {
  /** 新闻ID */
  id: number
  /** 新闻标题 */
  title: string
  /** 封面图片URL */
  image?: string
  /** 备用图片字段 */
  coverUrl?: string
  /** 发布时间 */
  publishTime: string
  /** 新闻来源/部门 */
  source?: string
  /** 新闻链接 */
  url?: string
  /** 新闻摘要 */
  summary?: string
  /** 新闻内容（详情页使用） */
  content?: string
  /** 浏览次数 */
  viewCount?: number
  /** 是否置顶 */
  pinned?: boolean
  /** 新闻分类 */
  category?: string
  /** 标签 */
  tags?: string[]
}

/**
 * 新闻列表响应
 */
export interface NewsListResponse {
  success: boolean
  data: {
    list: NewsItem[]
    total: number
    pageNo: number
    pageSize: number
  }
  message?: string
  /** 是否使用降级数据 */
  isFallback?: boolean
  /** 是否可重试 */
  retryable?: boolean
}

/**
 * 新闻详情响应
 */
export interface NewsDetailResponse {
  success: boolean
  data: NewsItem
  message?: string
  /** 是否使用降级数据 */
  isFallback?: boolean
  /** 是否可重试 */
  retryable?: boolean
}

/**
 * 新闻查询参数
 */
export interface NewsQueryParams {
  /** 页码 */
  pageNo?: number
  /** 每页数量 */
  pageSize?: number
  /** 关键词搜索 */
  keyword?: string
  /** 分类筛选 */
  category?: string
  /** 时间范围开始 */
  startDate?: string
  /** 时间范围结束 */
  endDate?: string
  /** 是否只显示置顶 */
  pinnedOnly?: boolean
}