// 用户信息接口
export interface UserInfo {
  userId: string
  username: string
  roleCode: string
  roleName: string
  departmentId?: number
  departmentName?: string
  enabled: boolean
}

// 登录请求接口
export interface LoginRequest {
  employeeId?: string
  name?: string
  username?: string
  password: string
}

// 登录响应接口
export interface LoginResponse {
  success: boolean
  message: string
  data: {
    accessToken: string
    userId: string
    username: string
    employeeId: string
    realName: string
    roleCode: string
    roleName: string
    departmentId?: number
    departmentName?: string
    enabled: boolean
    tokenExpireTime: string
    userType: string
  }
}