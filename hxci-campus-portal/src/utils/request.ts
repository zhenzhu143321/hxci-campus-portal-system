import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 🛡️ CSRF Token管理器
class CsrfTokenManager {
  private static instance: CsrfTokenManager
  private csrfToken: string | null = null
  private tokenExpiry: number = 0
  private refreshPromise: Promise<string> | null = null

  static getInstance(): CsrfTokenManager {
    if (!CsrfTokenManager.instance) {
      CsrfTokenManager.instance = new CsrfTokenManager()
    }
    return CsrfTokenManager.instance
  }

  /**
   * 🔐 获取有效的CSRF Token
   */
  async getValidToken(): Promise<string> {
    // 检查是否有有效的缓存Token
    if (this.csrfToken && Date.now() < this.tokenExpiry) {
      return this.csrfToken
    }

    // 如果正在刷新Token，等待刷新完成
    if (this.refreshPromise) {
      return this.refreshPromise
    }

    // 开始刷新Token
    this.refreshPromise = this.fetchNewToken()
    try {
      const token = await this.refreshPromise
      return token
    } finally {
      this.refreshPromise = null
    }
  }

  /**
   * 🔄 从服务器获取新的CSRF Token
   */
  private async fetchNewToken(): Promise<string> {
    try {
      console.log('🔐 [CSRF] 正在获取新的CSRF Token...')
      
      const response = await axios.get('/csrf-token', {
        timeout: 5000,
        withCredentials: true  // 确保Cookie能正确发送和接收
      })

      if (response.data?.code === 0 && response.data?.data?.token) {
        this.csrfToken = response.data.data.token
        // 设置Token过期时间 (55分钟，比服务端1小时略短)
        this.tokenExpiry = Date.now() + (55 * 60 * 1000)
        
        console.log('✅ [CSRF] CSRF Token获取成功，长度:', this.csrfToken.length)
        console.log('🕒 [CSRF] Token过期时间:', new Date(this.tokenExpiry).toLocaleTimeString())
        
        return this.csrfToken
      } else {
        throw new Error('CSRF Token响应格式错误')
      }
    } catch (error) {
      console.error('💥 [CSRF] CSRF Token获取失败:', error)
      throw new Error('无法获取CSRF Token，请检查网络连接')
    }
  }

  /**
   * 🚨 清除过期的CSRF Token
   */
  clearToken(): void {
    this.csrfToken = null
    this.tokenExpiry = 0
    console.log('🧹 [CSRF] 已清除CSRF Token缓存')
  }

  /**
   * 🔍 检查是否需要CSRF Token的请求
   */
  static requiresCsrfToken(config: AxiosRequestConfig): boolean {
    if (!config.method) return false
    
    const method = config.method.toUpperCase()
    const writeMethods = ['POST', 'PUT', 'DELETE', 'PATCH']
    
    // 只有写操作需要CSRF Token
    if (!writeMethods.includes(method)) {
      return false
    }

    // 排除不需要CSRF保护的端点
    const exemptPaths = [
      '/csrf-token',
      '/csrf-status', 
      '/csrf-config',
      '/mock-school-api'
    ]
    
    const url = config.url || ''
    return !exemptPaths.some(path => url.includes(path))
  }
}

// 创建axios实例
const api: AxiosInstance = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  },
  withCredentials: true  // 🔐 支持CSRF Token Cookie
})

// 🛡️ CSRF Token管理器实例
const csrfManager = CsrfTokenManager.getInstance()

// 请求拦截器
api.interceptors.request.use(
  async (config: AxiosRequestConfig) => {
    console.log('🚀 [API请求] 准备发送请求')
    console.log('📤 请求URL:', config.url)
    console.log('📤 请求方法:', config.method?.toUpperCase())
    console.log('📤 请求头:', config.headers)
    
    // 1️⃣ 添加JWT token到请求头
    const token = localStorage.getItem('campus_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('🔑 已添加Authorization头 (Token前50字符):', token.substring(0, 50) + '...')
    } else {
      console.log('⚠️ 没有找到Token或headers为空')
    }
    
    // 2️⃣ 如果是主通知服务的API，添加tenant-id
    if (config.url?.includes('/admin-api/')) {
      if (config.headers) {
        config.headers['tenant-id'] = '1'
        console.log('🏢 已添加tenant-id: 1 (主通知服务)')
      }
    }
    
    // 3️⃣ 🛡️ 为需要CSRF保护的请求添加CSRF Token
    if (CsrfTokenManager.requiresCsrfToken(config)) {
      try {
        const csrfToken = await csrfManager.getValidToken()
        if (config.headers) {
          config.headers['X-CSRF-TOKEN'] = csrfToken
          console.log('🛡️ [CSRF] 已添加CSRF Token到请求头 (前20字符):', csrfToken.substring(0, 20) + '...')
        }
      } catch (error) {
        console.error('💥 [CSRF] 获取CSRF Token失败:', error)
        // CSRF Token获取失败时，继续发送请求让服务端返回具体错误
        console.warn('⚠️ [CSRF] 继续发送请求，服务端将返回CSRF验证错误')
      }
    } else {
      console.log('ℹ️ [CSRF] 当前请求无需CSRF Token (GET请求或豁免路径)')
    }
    
    if (config.data) {
      console.log('📤 请求数据:', typeof config.data === 'string' ? config.data : JSON.stringify(config.data, null, 2))
    }
    
    return config
  },
  (error) => {
    console.error('💥 [API请求] 请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse) => {
    console.log('📥 [API响应] 收到响应')
    console.log('✅ 响应状态:', response.status, response.statusText)
    console.log('📥 响应头:', response.headers)
    console.log('📥 响应数据:', response.data)
    return response
  },
  (error) => {
    console.error('💥 [API响应] 响应拦截器捕获错误')
    console.error('错误对象:', error)
    
    if (error.response) {
      const { status, data, headers } = error.response
      console.error('🌐 HTTP错误响应:')
      console.error('状态码:', status)
      console.error('响应头:', headers)
      console.error('响应数据:', data)
      
      switch (status) {
        case 401:
          console.error('🔐 认证失败 - 401错误')
          ElMessage.error('认证失败，请重新登录')
          // 清除token并跳转到登录页
          localStorage.removeItem('campus_token')
          localStorage.removeItem('campus_user_info')
          console.log('🧹 已清除本地存储的认证信息')
          window.location.href = '/login'
          break
        case 403:
          // 🛡️ 检查是否为CSRF验证失败
          if (data?.type === 'CSRF_TOKEN_INVALID' || data?.message?.includes('CSRF')) {
            console.error('🛡️ CSRF验证失败 - 403错误')
            ElMessage.error('安全验证失败，正在刷新页面...')
            // 清除CSRF Token缓存并刷新页面
            csrfManager.clearToken()
            setTimeout(() => {
              window.location.reload()
            }, 2000)
          } else {
            console.error('🚫 权限不足 - 403错误')
            ElMessage.error('权限不足')
          }
          break
        case 404:
          console.error('🔍 资源不存在 - 404错误')
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          console.error('🔥 服务器内部错误 - 500错误')
          ElMessage.error('服务器内部错误')
          break
        default:
          console.error(`🌐 其他HTTP错误 - ${status}错误:`, data?.message)
          ElMessage.error(data?.message || '请求失败')
      }
    } else if (error.request) {
      console.error('🌐 网络请求错误 - 无响应')
      console.error('请求对象:', error.request)
      ElMessage.error('网络连接失败')
    } else {
      console.error('⚠️ 其他错误:', error.message)
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default api