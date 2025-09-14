/**
 * 用户认证管理 Composable
 * 负责登录、登出、Token管理、用户信息管理等认证相关功能
 */

import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Ref, ComputedRef } from 'vue'

// TypeScript 类型定义
interface LoginCredentials {
  employeeId: string
  name: string
  password: string
}

interface UserInfo {
  userId: string
  username: string
  roleName: string
  roleCode: string
  departmentName?: string
  classId?: string
  gradeId?: string
}

interface AuthState {
  loading: boolean
  error: string | null
}

interface UseAuthReturn {
  // 状态
  user: ComputedRef<UserInfo | null>
  token: ComputedRef<string | null>
  isAuthenticated: ComputedRef<boolean>
  loading: Ref<boolean>
  error: Ref<string | null>

  // 方法
  login: (credentials: LoginCredentials) => Promise<void>
  logout: (options?: { silent?: boolean }) => Promise<void>
  refreshToken: () => Promise<void>
  loadUserFromStorage: () => Promise<boolean>
  clearAuth: () => void

  // 路由守卫
  setupAuthGuard: () => void
}

export function useAuth(): UseAuthReturn {
  const router = useRouter()
  const userStore = useUserStore()

  // 本地状态
  const state = ref<AuthState>({
    loading: false,
    error: null
  })

  // 计算属性 - 从store获取
  const user = computed(() => userStore.userInfo)
  const token = computed(() => userStore.token)
  const isAuthenticated = computed(() => !!userStore.token && !!userStore.userInfo)

  // 登录方法
  const login = async (credentials: LoginCredentials): Promise<void> => {
    state.value.loading = true
    state.value.error = null

    try {
      // 调用store的登录方法
      await userStore.login(credentials)

      // 登录成功提示
      ElMessage.success('登录成功')

      // 跳转到首页或之前的页面
      const redirect = router.currentRoute.value.query.redirect as string
      await router.push(redirect || '/')

    } catch (error: any) {
      state.value.error = error.message || '登录失败'
      ElMessage.error(state.value.error)
      throw error
    } finally {
      state.value.loading = false
    }
  }

  // 登出方法
  const logout = async (options?: { silent?: boolean }): Promise<void> => {
    try {
      // 如果不是静默登出，显示确认对话框
      if (!options?.silent) {
        await ElMessageBox.confirm(
          '您确定要退出登录吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
      }

      // 调用store的登出方法
      await userStore.logout()

      // 清除本地缓存
      clearAuth()

      if (!options?.silent) {
        ElMessage.success('退出成功')
      }

      // 跳转到登录页
      await router.push('/login')

    } catch (error) {
      // 用户取消登出不算错误
      if (error !== 'cancel') {
        console.error('登出失败:', error)
        ElMessage.error('登出操作失败')
      }
    }
  }

  // 刷新Token
  const refreshToken = async (): Promise<void> => {
    try {
      // TODO: 实现Token刷新逻辑
      // await userStore.refreshToken()
      console.log('Token刷新功能待实现')
    } catch (error) {
      console.error('Token刷新失败:', error)
      // Token刷新失败，强制登出
      await logout({ silent: true })
    }
  }

  // 从本地存储加载用户信息
  const loadUserFromStorage = async (): Promise<boolean> => {
    try {
      // 检查本地是否有token
      const localToken = localStorage.getItem('campus_token')
      const localUserInfo = localStorage.getItem('campus_user_info')  // 修复：使用正确的key名

      if (localToken && localUserInfo) {
        // 恢复到store
        userStore.token = localToken
        userStore.userInfo = JSON.parse(localUserInfo)

        // TODO: 验证token是否仍然有效
        // await userStore.validateToken()

        return true
      }

      return false
    } catch (error) {
      console.error('加载用户信息失败:', error)
      return false
    }
  }

  // 清除认证信息
  const clearAuth = (): void => {
    // 清除localStorage
    localStorage.removeItem('campus_token')
    localStorage.removeItem('campus_user_info')  // 修复：使用正确的key名

    // 清除sessionStorage
    sessionStorage.removeItem('campus_token')
    sessionStorage.removeItem('campus_user_info')  // 修复：使用正确的key名

    // 重置store
    userStore.$reset()
  }

  // 设置认证守卫
  const setupAuthGuard = (): void => {
    // 监听token变化
    watch(
      () => userStore.token,
      (newToken, oldToken) => {
        // Token从有到无，说明已登出或token失效
        if (oldToken && !newToken) {
          ElMessage.warning('登录状态已失效，请重新登录')
          router.push({
            path: '/login',
            query: { redirect: router.currentRoute.value.fullPath }
          })
        }
      }
    )

    // 监听401响应（需要在axios拦截器中触发）
    // window.addEventListener('auth:unauthorized', handleUnauthorized)
  }

  // 处理跨标签页同步
  const setupCrossTabSync = (): void => {
    if (typeof window === 'undefined') return

    window.addEventListener('storage', (e) => {
      if (e.key === 'campus_token') {
        if (!e.newValue) {
          // 其他标签页登出
          clearAuth()
          router.push('/login')
        } else if (e.newValue !== token.value) {
          // Token更新
          userStore.token = e.newValue
        }
      }

      if (e.key === 'campus_user_info' && e.newValue) {  // 修复：使用正确的key名
        try {
          userStore.userInfo = JSON.parse(e.newValue)
        } catch (error) {
          console.error('解析用户信息失败:', error)
        }
      }
    })
  }

  // 初始化
  setupAuthGuard()
  setupCrossTabSync()

  return {
    // 状态
    user,
    token,
    isAuthenticated,
    loading: computed(() => state.value.loading),
    error: computed(() => state.value.error),

    // 方法
    login,
    logout,
    refreshToken,
    loadUserFromStorage,
    clearAuth,

    // 守卫
    setupAuthGuard
  }
}