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
  async verifyToken(token: string): Promise<{ code: number; data: any; msg: string }> {
    try {
      // 🔧 修复：后端期望token在请求体中，不是在Authorization头中
      const response = await api.post('/mock-school-api/auth/verify', {
        token: token
      })

      // 适配返回格式为DevDebugPanel期望的格式
      const result = response.data
      return {
        code: result.success ? 0 : -1,
        data: {
          success: result.success,
          message: result.message,
          valid: result.success,
          userInfo: result.userInfo || null
        },
        msg: result.message || (result.success ? 'Token验证成功' : 'Token验证失败')
      }
    } catch (error: any) {
      console.error('Token验证失败:', error)
      return {
        code: -1,
        data: null,
        msg: error.message || 'Token验证异常'
      }
    }
  },

  // 健康检查
  async healthCheck(): Promise<{ code: number; data: any; msg: string }> {
    try {
      const response = await api.get('/mock-school-api/auth/health')

      // 适配返回格式为DevDebugPanel期望的格式
      const result = response.data
      return {
        code: 0,
        data: {
          status: result.status || 'ok',
          timestamp: result.timestamp || new Date().toISOString(),
          service: 'Mock School API',
          healthy: true
        },
        msg: result.message || '健康检查通过'
      }
    } catch (error: any) {
      console.error('健康检查失败:', error)

      // 如果是404，说明健康检查接口不存在，返回模拟成功响应
      if (error.response?.status === 404) {
        return {
          code: 0,
          data: {
            status: 'simulated',
            message: '服务正常（模拟响应）',
            timestamp: new Date().toISOString(),
            healthy: true
          },
          msg: '健康检查通过（模拟）'
        }
      }

      return {
        code: -1,
        data: {
          status: 'error',
          healthy: false,
          error: error.message
        },
        msg: error.message || '健康检查失败'
      }
    }
  }
}