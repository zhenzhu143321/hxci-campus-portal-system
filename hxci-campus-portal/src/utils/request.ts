import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const api: AxiosInstance = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    console.log('🚀 [API请求] 准备发送请求')
    console.log('📤 请求URL:', config.url)
    console.log('📤 请求方法:', config.method?.toUpperCase())
    console.log('📤 请求头:', config.headers)
    
    // 添加token到请求头
    const token = localStorage.getItem('campus_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('🔑 已添加Authorization头 (Token前50字符):', token.substring(0, 50) + '...')
    } else {
      console.log('⚠️ 没有找到Token或headers为空')
    }
    
    // 如果是主通知服务的API，添加tenant-id
    if (config.url?.includes('/admin-api/')) {
      if (config.headers) {
        config.headers['tenant-id'] = '1'
        console.log('🏢 已添加tenant-id: 1 (主通知服务)')
      }
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
          console.error('🚫 权限不足 - 403错误')
          ElMessage.error('权限不足')
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