/**
 * 新闻状态管理Store
 *
 * @description 基于Pinia的校园新闻状态管理
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { NewsItem } from '@/types/news'
import { newsAPI } from '@/api/news'

export const useNewsStore = defineStore('news', () => {
  // ================== 状态定义 ==================

  /** 新闻列表 */
  const newsList = ref<NewsItem[]>([])

  /** 加载状态 */
  const loading = ref<boolean>(false)

  /** 错误信息 */
  const error = ref<string | null>(null)

  /** 是否使用降级数据 */
  const isFallback = ref<boolean>(false)

  /** 降级提示信息 */
  const fallbackMessage = ref<string | null>(null)

  /** 是否可重试 */
  const retryable = ref<boolean>(false)

  /** 最后更新时间 */
  const lastUpdateTime = ref<string | null>(null)

  /** 缓存TTL（5分钟） */
  const CACHE_TTL = 5 * 60 * 1000

  /** 当前正在进行的请求ID */
  const currentFetchId = ref<number>(0)

  /** 当前选中的新闻（用于详情展示） */
  const selectedNews = ref<NewsItem | null>(null)

  // ================== 辅助函数 ==================

  /**
   * 规范化新闻数据
   * 处理不同字段名的兼容性
   */
  const normalize = (raw: any): NewsItem => {
    // 兼容不同的图片字段名
    const image = raw.image || raw.imageUrl || raw.coverUrl || raw.cover || ''
    // 兼容不同的时间字段名
    const publishTime = raw.publishTime || raw.time || raw.date || raw.createTime || ''

    // ID处理：避免NaN，使用fallback
    let id = Number(raw.id)
    if (isNaN(id) || !id) {
      // 使用标题和时间的hash作为fallback ID
      const hashString = `${raw.title || ''}_${publishTime}`
      id = hashString.split('').reduce((acc, char) => {
        return ((acc << 5) - acc) + char.charCodeAt(0)
      }, 0)
    }

    return {
      id,
      title: String(raw.title || ''),
      image,
      coverUrl: raw.coverUrl || raw.imageUrl || image,
      publishTime,
      source: raw.source || raw.department || raw.publisher || '',
      url: raw.url || raw.link || '',
      summary: raw.summary || raw.description || '',
      content: raw.content || '',
      viewCount: raw.viewCount || raw.views || 0,
      pinned: raw.pinned || raw.isTop || false,
      category: raw.category || raw.type || '',
      tags: raw.tags || []
    }
  }

  // ================== 计算属性 ==================

  /**
   * 按时间排序的新闻列表
   */
  const sortedNews = computed(() => {
    const list = [...newsList.value]

    // 先置顶，再按时间倒序
    list.sort((a, b) => {
      // 置顶优先
      if (a.pinned && !b.pinned) return -1
      if (!a.pinned && b.pinned) return 1

      // 时间倒序
      const timeA = Date.parse(a.publishTime || '') || 0
      const timeB = Date.parse(b.publishTime || '') || 0
      return timeB - timeA
    })

    return list
  })

  /**
   * 获取前5条新闻（用于首页展示）
   */
  const topNews = computed(() => sortedNews.value.slice(0, 5))

  /**
   * 获取所有新闻
   */
  const allNews = computed(() => sortedNews.value)

  /**
   * 置顶新闻
   */
  const pinnedNews = computed(() =>
    sortedNews.value.filter(news => news.pinned)
  )

  /**
   * 按分类分组的新闻
   */
  const newsByCategory = computed(() => {
    const grouped = new Map<string, NewsItem[]>()

    sortedNews.value.forEach(news => {
      const category = news.category || '其他'
      if (!grouped.has(category)) {
        grouped.set(category, [])
      }
      grouped.get(category)!.push(news)
    })

    return grouped
  })

  // ================== 操作方法 ==================

  /**
   * 获取新闻列表（带TTL和SWR策略）
   */
  const fetchNews = async (params?: { pageNo?: number; pageSize?: number }, forceRefresh = false) => {
    // TTL检查：如果缓存还新鲜且有数据，直接返回
    if (!forceRefresh && lastUpdateTime.value && newsList.value.length > 0) {
      const lastUpdateMs = new Date(lastUpdateTime.value).getTime()
      const isStale = Date.now() - lastUpdateMs > CACHE_TTL

      if (!isStale) {
        if (import.meta.env.DEV) {
          console.log('📰 [NewsStore] 使用缓存数据，TTL剩余:',
            Math.round((CACHE_TTL - (Date.now() - lastUpdateMs)) / 1000), '秒')
        }
        return
      }

      // SWR策略：先返回缓存，后台刷新
      if (import.meta.env.DEV) {
        console.log('📰 [NewsStore] 缓存过期，后台刷新中...')
      }
      // 不设置loading，保持用户体验
      setTimeout(() => fetchNewsInternal(params, false), 0)
      return
    }

    // 首次加载或强制刷新
    await fetchNewsInternal(params, true)
  }

  /**
   * 内部获取新闻方法
   */
  const fetchNewsInternal = async (params?: { pageNo?: number; pageSize?: number }, showLoading = true) => {
    // 防止并发请求
    const fetchId = ++currentFetchId.value

    if (showLoading) {
      loading.value = true
    }
    error.value = null

    try {
      if (import.meta.env.DEV) {
        console.log('📰 [NewsStore] 开始获取新闻列表...')
      }

      const res = await newsAPI.getNewsList(params)

      // 忽略过期的响应
      if (fetchId !== currentFetchId.value) {
        if (import.meta.env.DEV) {
          console.log('⚠️ [NewsStore] 忽略过期响应')
        }
        return
      }

      if (res.success) {
        newsList.value = (res.data.list || []).map(normalize)
        lastUpdateTime.value = new Date().toISOString()

        // 检查是否使用降级数据
        if (res.isFallback) {
          isFallback.value = true
          fallbackMessage.value = res.message || '使用缓存数据'
          retryable.value = res.retryable || false

          if (import.meta.env.DEV) {
            console.log('⚠️ [NewsStore] 使用降级数据，共', newsList.value.length, '条')
            console.log('📝 [NewsStore] 降级原因:', fallbackMessage.value)
          }

          // 用户提示（仅在首次加载时显示）
          if (showLoading) {
            ElMessage.info({
              message: fallbackMessage.value,
              duration: 3000,
              showClose: true
            })
          }
        } else {
          // 真实数据获取成功，重置降级状态
          isFallback.value = false
          fallbackMessage.value = null
          retryable.value = false
          error.value = null

          if (import.meta.env.DEV) {
            console.log('✅ [NewsStore] 获取新闻成功，共', newsList.value.length, '条')
          }
        }
      } else {
        // API请求完全失败
        error.value = res.message || '新闻数据获取失败'
        if (import.meta.env.DEV) {
          console.error('❌ [NewsStore] API请求失败:', res.message)
        }
      }
    } catch (e: any) {
      // 如果是取消错误，静默处理
      if (e?.name === 'AbortError' || e?.code === 'ERR_CANCELED') {
        if (import.meta.env.DEV) {
          console.log('🚫 [NewsStore] 请求被取消')
        }
        return // 不进行降级处理
      }

      if (import.meta.env.DEV) {
        console.error('❌ [NewsStore] 获取新闻失败:', e)
      }
      error.value = e?.message || '网络错误'

      // 降级到默认新闻
      const fallback = newsAPI.getDefaultNews().map(normalize)
      newsList.value = fallback
      isFallback.value = true
      fallbackMessage.value = '网络连接失败，显示默认数据'
      retryable.value = true

      if (import.meta.env.DEV) {
        console.log('📰 [NewsStore] 使用默认新闻数据')
      }

      // 用户提示
      if (showLoading) {
        ElMessage.warning({
          message: '网络连接失败，显示默认数据',
          duration: 3000,
          showClose: true,
          onClose: () => {
            // 提供重试选项
            if (retryable.value) {
              ElMessage({
                message: '点击刷新按钮可重新尝试加载',
                type: 'info',
                duration: 2000
              })
            }
          }
        })
      }
    } finally {
      if (showLoading) {
        loading.value = false
      }
    }
  }

  /**
   * 刷新新闻列表
   */
  const refresh = async () => {
    if (import.meta.env.DEV) {
      console.log('🔄 [NewsStore] 强制刷新新闻列表')
    }

    // 重置降级状态
    isFallback.value = false
    fallbackMessage.value = null
    retryable.value = false

    return fetchNews(undefined, true) // 强制刷新
  }

  /**
   * 重试加载（用于降级后的手动重试）
   */
  const retry = async () => {
    if (!retryable.value) {
      console.log('⚠️ [NewsStore] 当前状态不支持重试')
      return
    }

    console.log('🔄 [NewsStore] 用户触发重试加载')
    ElMessage.loading({
      message: '正在重新加载新闻...',
      duration: 0
    })

    // 清除降级状态并重新加载
    isFallback.value = false
    fallbackMessage.value = null
    error.value = null

    try {
      await fetchNews(undefined, true)

      // 检查是否重试成功
      if (!isFallback.value) {
        ElMessage.closeAll()
        ElMessage.success('新闻加载成功！')
      } else {
        ElMessage.closeAll()
        ElMessage.warning('仍在使用缓存数据，请稍后重试')
      }
    } catch (e) {
      ElMessage.closeAll()
      ElMessage.error('重试失败，请检查网络连接')
    }
  }

  /**
   * 搜索新闻
   */
  const searchNews = async (keyword: string) => {
    loading.value = true
    error.value = null

    try {
      console.log('🔍 [NewsStore] 搜索新闻:', keyword)

      const res = await newsAPI.searchNews(keyword)

      if (res.success) {
        newsList.value = (res.data.list || []).map(normalize)

        // 处理搜索的降级情况
        if (res.isFallback) {
          isFallback.value = true
          fallbackMessage.value = res.message || '从缓存数据搜索'
          console.log('⚠️ [NewsStore] 从降级数据搜索，找到', newsList.value.length, '条')

          ElMessage.info('搜索服务暂不可用，从缓存数据搜索')
        } else {
          isFallback.value = false
          fallbackMessage.value = null
          console.log('✅ [NewsStore] 搜索到', newsList.value.length, '条新闻')
        }
      } else {
        error.value = res.message || '搜索失败'
        console.error('❌ [NewsStore] 搜索失败:', res.message)
      }
    } catch (e: any) {
      console.error('❌ [NewsStore] 搜索失败:', e)
      error.value = e?.message || '搜索出错'
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取新闻详情
   */
  const fetchNewsDetail = async (id: number) => {
    try {
      console.log('📖 [NewsStore] 获取新闻详情, ID:', id)

      const res = await newsAPI.getNewsDetail(id)

      if (res.success) {
        selectedNews.value = normalize(res.data)

        // 处理详情的降级情况
        if (res.isFallback) {
          console.log('⚠️ [NewsStore] 使用降级的新闻详情')
          ElMessage.info('从缓存加载新闻详情')
        } else {
          console.log('✅ [NewsStore] 获取新闻详情成功')
        }

        return selectedNews.value
      } else {
        console.warn('⚠️ [NewsStore] 获取新闻详情失败:', res.message)
        return null
      }
    } catch (e: any) {
      console.error('❌ [NewsStore] 获取新闻详情出错:', e)
      return null
    }
  }

  /**
   * 设置选中的新闻
   */
  const setSelectedNews = (news: NewsItem | null) => {
    selectedNews.value = news
  }

  /**
   * 清理资源（取消所有进行中的请求）
   */
  const cleanup = () => {
    console.log('🧹 [NewsStore] 清理资源，取消所有请求')
    newsAPI.cancelAllRequests()
  }

  /**
   * 重置Store状态
   */
  const $reset = () => {
    // 先取消所有请求
    cleanup()

    // 重置状态
    newsList.value = []
    loading.value = false
    error.value = null
    lastUpdateTime.value = null
    selectedNews.value = null
    isFallback.value = false
    fallbackMessage.value = null
    retryable.value = false
    console.log('🔄 [NewsStore] 状态已重置')
  }

  return {
    // 状态
    newsList,
    loading,
    error,
    lastUpdateTime,
    selectedNews,
    isFallback,
    fallbackMessage,
    retryable,

    // 计算属性
    sortedNews,
    topNews,
    allNews,
    pinnedNews,
    newsByCategory,

    // 方法
    fetchNews,
    refresh,
    retry,
    searchNews,
    fetchNewsDetail,
    setSelectedNews,
    cleanup,
    $reset
  }
})