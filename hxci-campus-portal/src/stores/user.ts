import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = ref<boolean>(false)

  // 设置token
  const setToken = (newToken: string) => {
    console.log('💾 [UserStore] 设置新Token')
    console.log('🔑 Token长度:', newToken.length)
    console.log('🔑 Token前50字符:', newToken.substring(0, 50) + '...')
    
    token.value = newToken
    localStorage.setItem('campus_token', newToken)
    isLoggedIn.value = true
    
    console.log('✅ Token已保存到localStorage和store')
  }

  // 设置用户信息
  const setUserInfo = (info: UserInfo) => {
    console.log('💾 [UserStore] 设置用户信息')
    console.log('👤 用户信息:', JSON.stringify(info, null, 2))
    
    userInfo.value = info
    const userInfoString = JSON.stringify(info)
    localStorage.setItem('campus_user_info', userInfoString)
    
    console.log('✅ 用户信息已保存到localStorage和store')
  }

  // 登出
  const logout = () => {
    console.log('🚪 [UserStore] 执行用户登出')
    console.log('🧹 清理用户状态...')
    
    token.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    
    localStorage.removeItem('campus_token')
    localStorage.removeItem('campus_user_info')
    
    console.log('✅ 用户状态已清理，localStorage已清空')
  }

  // 初始化 - 从localStorage恢复状态
  const initializeAuth = () => {
    console.log('🔄 [UserStore] 初始化认证状态')
    
    const savedToken = localStorage.getItem('campus_token')
    const savedUserInfo = localStorage.getItem('campus_user_info')
    
    console.log('🔍 检查localStorage中的认证信息:')
    console.log('Token存在:', !!savedToken)
    console.log('用户信息存在:', !!savedUserInfo)
    
    if (savedToken) {
      console.log('🔑 恢复Token (前50字符):', savedToken.substring(0, 50) + '...')
      token.value = savedToken
      isLoggedIn.value = true
      console.log('✅ Token已设置到store')
    } else {
      console.log('❌ 没有找到保存的Token')
      token.value = ''
      isLoggedIn.value = false
    }
    
    if (savedUserInfo) {
      try {
        const parsed = JSON.parse(savedUserInfo)
        console.log('👤 恢复用户信息:', JSON.stringify(parsed, null, 2))
        userInfo.value = parsed
        console.log('✅ 用户信息已设置到store')
      } catch (error) {
        console.error('💥 解析用户信息失败:', error)
        console.log('🧹 清除无效的用户信息')
        localStorage.removeItem('campus_user_info')
        userInfo.value = null
      }
    } else {
      console.log('❌ 没有找到保存的用户信息')
      userInfo.value = null
    }
    
    console.log('✅ 认证状态初始化完成')
    console.log('最终登录状态:', isLoggedIn.value)
    console.log('最终Token状态:', !!token.value)
    console.log('最终用户信息状态:', !!userInfo.value)
  }

  // 初始化认证状态的别名方法（向后兼容）
  const initAuth = async () => {
    return new Promise<void>((resolve) => {
      // 使用 nextTick 确保DOM更新完成
      setTimeout(() => {
        initializeAuth()
        resolve()
      }, 0)
    })
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout,
    initializeAuth,
    initAuth  // 向后兼容的别名
  }
})