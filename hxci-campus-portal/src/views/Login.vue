<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="login-background">
      <div class="bg-decoration"></div>
    </div>
    
    <!-- 登录表单卡片 -->
    <div class="login-card">
      <!-- 学校标题 -->
      <div class="school-header">
        <div class="school-logo">
          <el-icon><School /></el-icon>
        </div>
        <h1 class="school-title">哈尔滨信息工程学院</h1>
        <p class="school-subtitle">智慧校园门户系统</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        size="large"
      >
        <!-- 登录模式切换 -->
        <div class="login-mode-switch">
          <div class="mode-tabs">
            <div 
              class="mode-tab"
              :class="{ active: loginMode === 'employee' }"
              @click="loginMode = 'employee'"
            >
              <el-icon><User /></el-icon>
              <span>工号登录</span>
            </div>
            <div 
              class="mode-tab"
              :class="{ active: loginMode === 'username' }"
              @click="loginMode = 'username'"
            >
              <el-icon><Avatar /></el-icon>
              <span>用户名登录</span>
            </div>
          </div>
        </div>

        <!-- 工号+姓名模式 -->
        <div v-if="loginMode === 'employee'">
          <el-form-item prop="employeeId">
            <el-input
              v-model="loginForm.employeeId"
              placeholder="请输入工号 (如: PRINCIPAL_001)"
              prefix-icon="User"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="name">
            <el-input
              v-model="loginForm.name"
              placeholder="请输入姓名 (如: Principal-Zhang)"
              prefix-icon="Avatar"
              clearable
            />
          </el-form-item>
        </div>

        <!-- 用户名模式 -->
        <div v-else>
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              clearable
            />
          </el-form-item>
        </div>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            <template v-if="!loading">
              <el-icon><Position /></el-icon>
              登录校园门户
            </template>
            <template v-else>
              正在验证身份...
            </template>
          </el-button>
        </el-form-item>

        <!-- 测试账号提示 -->
        <div class="test-accounts">
          <p class="hint-title">
            <el-icon><InfoFilled /></el-icon>
            测试账号
          </p>
          <div class="account-list">
            <div class="account-item" @click="fillTestAccount('PRINCIPAL_001', 'Principal-Zhang')">
              <span class="role">校长</span>
              <span class="account">PRINCIPAL_001 / Principal-Zhang</span>
            </div>
            <div class="account-item" @click="fillTestAccount('TEACHER_001', 'Teacher-Wang')">
              <span class="role">教师</span>
              <span class="account">TEACHER_001 / Teacher-Wang</span>
            </div>
            <div class="account-item" @click="fillTestAccount('STUDENT_001', 'Student-Zhang')">
              <span class="role">学生</span>
              <span class="account">STUDENT_001 / Student-Zhang</span>
            </div>
          </div>
        </div>
      </el-form>
    </div>

    <!-- 页脚信息 -->
    <div class="login-footer">
      <p>© 2025 哈尔滨信息工程学院 智慧校园门户系统</p>
      <p>推荐使用 Chrome、Firefox、Edge 等现代浏览器</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authAPI } from '@/api/auth'
import type { LoginRequest } from '@/types/user'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 登录模式: employee(工号) | username(用户名)
const loginMode = ref<'employee' | 'username'>('employee')

// 加载状态
const loading = ref(false)

// 表单数据
const loginForm = reactive<LoginRequest>({
  employeeId: '',
  name: '',
  username: '',
  password: 'admin123' // 默认密码
})

// 表单验证规则
const loginRules: FormRules = {
  employeeId: [
    { required: true, message: '请输入工号', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

// 填充测试账号
const fillTestAccount = (employeeId: string, name: string) => {
  loginMode.value = 'employee'
  loginForm.employeeId = employeeId
  loginForm.name = name
  loginForm.password = 'admin123'
  ElMessage.success(`已填充 ${name} 账号信息`)
}

// 处理登录
const handleLogin = async () => {
  console.log('=== 登录流程开始 ===')
  console.log('当前时间:', new Date().toLocaleString())
  console.log('登录模式:', loginMode.value)
  console.log('表单引用状态:', !!loginFormRef.value)
  
  if (!loginFormRef.value) {
    console.error('❌ 表单引用为空，无法继续')
    return
  }

  try {
    console.log('🔍 开始表单验证...')
    // 表单验证
    await loginFormRef.value.validate()
    console.log('✅ 表单验证通过')
    
    loading.value = true
    console.log('⏳ 设置loading状态为true')

    // 准备登录数据
    const loginData: LoginRequest = {
      password: loginForm.password
    }

    if (loginMode.value === 'employee') {
      loginData.employeeId = loginForm.employeeId
      loginData.name = loginForm.name
      console.log('👔 使用工号登录模式')
    } else {
      loginData.username = loginForm.username
      console.log('👤 使用用户名登录模式')
    }

    console.log('📤 登录请求数据:', JSON.stringify(loginData, null, 2))
    console.log('🚀 开始调用登录API...')

    // 调用登录API
    const response = await authAPI.login(loginData)
    
    console.log('📥 登录API响应:', JSON.stringify(response, null, 2))
    console.log('响应成功状态:', response.success)
    console.log('响应消息:', response.message)

    if (response.success && response.data) {
      console.log('✅ 登录成功，开始处理响应数据')
      console.log('🔑 获取到的accessToken:', response.data.accessToken?.substring(0, 50) + '...')
      
      // 保存token和用户信息
      userStore.setToken(response.data.accessToken)
      console.log('💾 Token已保存到store')
      
      // 构建用户信息对象
      const userInfo = {
        userId: response.data.userId,
        username: response.data.username,
        roleCode: response.data.roleCode,
        roleName: response.data.roleName,
        departmentId: response.data.departmentId,
        departmentName: response.data.departmentName,
        enabled: response.data.enabled
      }
      
      console.log('👤 构建的用户信息:', JSON.stringify(userInfo, null, 2))
      
      userStore.setUserInfo(userInfo)
      console.log('💾 用户信息已保存到store')
      
      ElMessage.success(`欢迎，${response.data.username}！`)
      console.log('🎉 显示欢迎消息')
      
      // 跳转到首页
      console.log('🔄 准备跳转到首页...')
      await router.push('/home')
      console.log('✅ 页面跳转完成')
    } else {
      console.error('❌ 登录失败 - 响应success为false或data为空')
      console.error('失败原因:', response.message)
      ElMessage.error(response.message || '登录失败')
    }
  } catch (error: any) {
    console.error('💥 登录过程中发生异常:', error)
    console.error('错误类型:', error.constructor.name)
    console.error('错误消息:', error.message)
    
    if (error.response) {
      console.error('🌐 HTTP响应错误信息:')
      console.error('状态码:', error.response.status)
      console.error('响应头:', error.response.headers)
      console.error('响应数据:', error.response.data)
      
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message)
      } else {
        ElMessage.error('服务器返回错误')
      }
    } else if (error.request) {
      console.error('🌐 网络请求错误:', error.request)
      ElMessage.error('网络连接失败，请检查网络连接')
    } else {
      console.error('⚠️ 其他错误:', error.message)
      ElMessage.error('登录失败，请重试或联系管理员')
    }
  } finally {
    loading.value = false
    console.log('🔄 设置loading状态为false')
    console.log('=== 登录流程结束 ===\n')
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.bg-decoration {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="50" cy="50" r="1" fill="white" fill-opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  animation: float 20s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-10px, -10px) rotate(180deg); }
}

.login-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 480px;
  backdrop-filter: blur(10px);
}

.school-header {
  text-align: center;
  margin-bottom: 32px;
}

.school-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 16px;
  margin-bottom: 16px;
}

.school-logo .el-icon {
  font-size: 32px;
  color: white;
}

.school-title {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.school-subtitle {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0;
}

.login-form {
  margin-top: 24px;
}

/* 登录模式切换标签样式 */
.login-mode-switch {
  margin-bottom: 24px;
}

.mode-tabs {
  display: flex;
  gap: 8px;
  background: #f8f9fa;
  padding: 4px;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

.mode-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: transparent;
  color: #6c757d;
  font-size: 14px;
  font-weight: 500;
  min-height: 44px;
  box-sizing: border-box;
}

.mode-tab:hover {
  background: #e9ecef;
  color: #495057;
  transform: translateY(-1px);
}

.mode-tab.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transform: translateY(-2px);
}

.mode-tab .el-icon {
  font-size: 16px;
}

.mode-tab span {
  white-space: nowrap;
}

/* 响应式优化 */
@media (max-width: 480px) {
  .mode-tab {
    padding: 10px 12px;
    font-size: 13px;
    gap: 6px;
  }
  
  .mode-tab .el-icon {
    font-size: 14px;
  }
}

.login-button {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
}

.test-accounts {
  margin-top: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.hint-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.account-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e9ecef;
}

.account-item:hover {
  background: #e3f2fd;
  border-color: #2196f3;
  transform: translateY(-1px);
}

.role {
  font-size: 12px;
  padding: 2px 8px;
  background: #e3f2fd;
  color: #1976d2;
  border-radius: 12px;
  font-weight: 500;
}

.account {
  font-size: 13px;
  color: #495057;
  font-family: 'Courier New', monospace;
}

.login-footer {
  position: absolute;
  bottom: 24px;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  line-height: 1.6;
}

.login-footer p {
  margin: 4px 0;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    margin: 16px;
    padding: 24px;
  }
  
  .school-title {
    font-size: 20px;
  }
  
  .login-footer {
    position: static;
    margin-top: 32px;
  }
}
</style>