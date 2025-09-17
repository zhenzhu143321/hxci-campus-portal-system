/**
 * 新闻API接口
 *
 * @description 校园新闻相关的API请求封装
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

import api from '@/utils/request'
import type { NewsItem, NewsListResponse, NewsDetailResponse } from '@/types/news'

class NewsAPI {
  /** 当前正在进行的请求控制器（用于取消重复请求） */
  private currentListController: AbortController | null = null
  private currentDetailController: AbortController | null = null
  private currentSearchController: AbortController | null = null
  /**
   * 获取新闻列表
   */
  async getNewsList(params?: { pageNo?: number; pageSize?: number }): Promise<NewsListResponse> {
    // 取消之前的请求
    if (this.currentListController) {
      console.log('⚠️ [新闻API] 取消之前的列表请求')
      this.currentListController.abort()
    }

    // 创建新的控制器
    this.currentListController = new AbortController()

    try {
      console.log('📰 [新闻API] 获取新闻列表, 参数:', params)

      const response = await api.get('/admin-api/campus/news/list', {
        params: {
          pageNo: params?.pageNo || 1,
          pageSize: params?.pageSize || 10
        },
        signal: this.currentListController.signal
      })

      console.log('📰 [新闻API] 获取新闻列表成功:', response.data)

      return {
        success: true,
        data: {
          list: response.data.data?.list || [],
          total: response.data.data?.total || 0,
          pageNo: params?.pageNo || 1,
          pageSize: params?.pageSize || 10
        }
      }
    } catch (error: any) {
      // 如果是取消错误，不记录日志
      if (error?.name === 'AbortError' || error?.code === 'ERR_CANCELED') {
        console.log('🚫 [新闻API] 列表请求被取消')
        throw error // 继续抛出，让Store处理
      }

      console.error('❌ [新闻API] 获取新闻列表失败:', error)
      // 降级处理 - 返回默认数据
      return {
        success: true,  // 改为true，因为有可用数据
        isFallback: true,  // 标记为降级数据
        retryable: true,  // 可重试
        data: {
          list: this.getDefaultNews(),
          total: 2,
          pageNo: 1,
          pageSize: 10
        },
        message: '新闻服务暂不可用，已显示默认数据'
      }
    } finally {
      // 清除控制器引用
      this.currentListController = null
    }
  }

  /**
   * 获取新闻详情
   */
  async getNewsDetail(id: number): Promise<NewsDetailResponse> {
    // 取消之前的详情请求
    if (this.currentDetailController) {
      console.log('⚠️ [新闻API] 取消之前的详情请求')
      this.currentDetailController.abort()
    }

    // 创建新的控制器
    this.currentDetailController = new AbortController()

    try {
      console.log('📰 [新闻API] 获取新闻详情, ID:', id)

      const response = await api.get(`/admin-api/campus/news/${id}`, {
        signal: this.currentDetailController.signal
      })

      console.log('📰 [新闻API] 获取新闻详情成功:', response.data)

      return {
        success: true,
        data: response.data.data
      }
    } catch (error: any) {
      // 如果是取消错误，不记录日志
      if (error?.name === 'AbortError' || error?.code === 'ERR_CANCELED') {
        console.log('🚫 [新闻API] 详情请求被取消')
        throw error
      }

      console.error('❌ [新闻API] 获取新闻详情失败:', error)

      // 尝试从默认数据中查找
      const defaultNews = this.getDefaultNews()
      const found = defaultNews.find(news => news.id === id)

      if (found) {
        return {
          success: true,  // 改为true，因为有可用数据
          isFallback: true,  // 标记为降级数据
          retryable: true,  // 可重试
          data: found,
          message: '从默认数据获取'
        }
      }

      // 完全无数据时才返回false
      return {
        success: false,
        data: {} as NewsItem,
        message: '获取新闻详情失败'
      }
    } finally {
      // 清除控制器引用
      this.currentDetailController = null
    }
  }

  /**
   * 搜索新闻
   */
  async searchNews(keyword: string): Promise<NewsListResponse> {
    // 取消之前的搜索请求
    if (this.currentSearchController) {
      console.log('⚠️ [新闻API] 取消之前的搜索请求')
      this.currentSearchController.abort()
    }

    // 创建新的控制器
    this.currentSearchController = new AbortController()

    try {
      console.log('🔍 [新闻API] 搜索新闻, 关键词:', keyword)

      const response = await api.get('/admin-api/campus/news/search', {
        params: { keyword },
        signal: this.currentSearchController.signal
      })

      return {
        success: true,
        data: {
          list: response.data.data?.list || [],
          total: response.data.data?.total || 0,
          pageNo: 1,
          pageSize: 20
        }
      }
    } catch (error: any) {
      // 如果是取消错误，不记录日志
      if (error?.name === 'AbortError' || error?.code === 'ERR_CANCELED') {
        console.log('🚫 [新闻API] 搜索请求被取消')
        throw error
      }

      console.error('❌ [新闻API] 搜索新闻失败:', error)

      // 在默认数据中搜索
      const defaultNews = this.getDefaultNews()
      const filtered = defaultNews.filter(news =>
        news.title.toLowerCase().includes(keyword.toLowerCase())
      )

      return {
        success: true,  // 改为true，因为有可用数据
        isFallback: true,  // 标记为降级数据
        retryable: true,  // 可重试
        data: {
          list: filtered,
          total: filtered.length,
          pageNo: 1,
          pageSize: 20
        },
        message: '搜索服务暂不可用，从默认数据搜索'
      }
    } finally {
      // 清除控制器引用
      this.currentSearchController = null
    }
  }

  /**
   * 获取默认新闻数据（降级方案）
   * 当API请求失败时使用这些数据保证用户体验
   */
  getDefaultNews(): NewsItem[] {
    return [
      {
        id: 1,
        title: '我校在全国程序设计竞赛中获得佳绩',
        publishTime: '2025-08-12',
        image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA2MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yNSAyMEMyNSAxNy4yMzg2IDI3LjIzODYgMTUgMzAgMTVDMzIuNzYxNCAxNSAzNSAxNy4yMzg2IDM1IDIwQzM1IDIyLjc2MTQgMzIuNzYxNCAyNSAzMCAyNUMyNy4yMzg2IDI1IDI1IDIyLjc2MTQgMjUgMjBaIiBmaWxsPSIjQ0NDQ0NDIi8+CjxwYXRoIGQ9Ik0yMCAyOEwyNS41IDIyLjVMMzIuNSAyOS41TDQwIDIyTDQwIDMySDIwVjI4WiIgZmlsbD0iI0NDQ0NDQyIvPgo8L3N2Zz4K',
        source: '计算机学院',
        summary: '我校代表队在第XX届全国大学生程序设计竞赛中取得优异成绩，获得金奖1项、银奖2项、铜奖3项。',
        viewCount: 1024,
        category: '竞赛成果'
      },
      {
        id: 2,
        title: '2025年春季学期开学典礼成功举行',
        publishTime: '2025-08-11',
        image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA2MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjRjBGOEZGIi8+CjxjaXJjbGUgY3g9IjMwIiBjeT0iMTYiIHI9IjQiIGZpbGw9IiM0MDlFRkYiLz4KPHBhdGggZD0iTTIyIDI2QzIyIDIzLjc5MDkgMjMuNzkwOSAyMiAyNiAyMkgzNEMzNi4yMDkxIDIyIDM4IDIzLjc5MDkgMzggMjZWMzJIMjJWMjZaIiBmaWxsPSIjNDA5RUZGIi8+Cjwvc3ZnPgo=',
        source: '学校办公室',
        summary: '新学期新气象，全校师生共同开启新征程。校长在典礼上发表重要讲话，寄语新生。',
        viewCount: 2048,
        category: '学校活动'
      }
    ]
  }

  /**
   * 取消所有请求
   * 在组件卸载或路由切换时调用
   */
  cancelAllRequests(): void {
    if (this.currentListController) {
      this.currentListController.abort()
      this.currentListController = null
    }
    if (this.currentDetailController) {
      this.currentDetailController.abort()
      this.currentDetailController = null
    }
    if (this.currentSearchController) {
      this.currentSearchController.abort()
      this.currentSearchController = null
    }
    console.log('🚫 [新闻API] 已取消所有请求')
  }
}

// 导出单例
export const newsAPI = new NewsAPI()