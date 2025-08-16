import api from '@/utils/request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/user'

// Mock School API 认证服务
export const authAPI = {
  // 用户登录
  async login(loginData: LoginRequest): Promise<LoginResponse> {
    try {
      const response = await api.post('/mock-school-api/auth/authenticate', loginData)
      return response.data
    } catch (error) {
      console.error('登录请求失败:', error)
      throw error
    }
  },

  // 获取用户信息
  async getUserInfo(token: string): Promise<{ success: boolean; data: UserInfo }> {
    try {
      const response = await api.post('/mock-school-api/auth/user-info', {}, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      return response.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    }
  },

  // 验证Token
  async verifyToken(token: string): Promise<{ success: boolean; message: string }> {
    try {
      // 🔧 修复：后端期望token在请求体中，不是在Authorization头中
      const response = await api.post('/mock-school-api/auth/verify', {
        token: token
      })
      return response.data
    } catch (error) {
      console.error('Token验证失败:', error)
      throw error
    }
  },

  // 健康检查
  async healthCheck(): Promise<{ status: string }> {
    try {
      const response = await api.get('/mock-school-api/auth/health')
      return response.data
    } catch (error) {
      console.error('健康检查失败:', error)
      throw error
    }
  }
}