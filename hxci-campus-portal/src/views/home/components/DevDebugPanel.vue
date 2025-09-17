<!--
/**
 * 开发调试面板组件
 *
 * @description 从Home.vue中提取的调试功能面板，用于开发环境的API测试和系统诊断
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 * 
 * @refactored 从Home.vue提取，解决God Component问题
 */
-->

<template>
  <div class="dev-debug-panel" v-show="visible">
    <div class="debug-header">
      <span class="debug-title">🛠️ 开发调试工具</span>
      <el-button 
        :icon="Close" 
        size="small" 
        circle 
        @click="emit('close')"
      />
    </div>
    
    <div class="debug-actions">
      <el-button 
        @click="handleHealthCheck" 
        :loading="loading.health" 
        size="small"
        type="primary"
      >
        <el-icon><Monitor /></el-icon>
        健康检查
      </el-button>
      
      <el-button 
        @click="handleTokenVerify" 
        :loading="loading.verify" 
        size="small"
        type="success"
      >
        <el-icon><Key /></el-icon>
        验证Token
      </el-button>
      
      <el-button 
        @click="handleNotificationAPI" 
        :loading="loading.notification" 
        size="small"
        type="warning"
      >
        <el-icon><Bell /></el-icon>
        通知API
      </el-button>
      
      <el-button 
        @click="handleClearCache" 
        :loading="loading.cache" 
        size="small"
        type="danger"
      >
        <el-icon><Delete /></el-icon>
        清空缓存
      </el-button>
    </div>
    
    <!-- 测试结果显示 -->
    <div class="debug-results" v-if="testResults">
      <div class="result-header">
        <span class="result-title">测试结果</span>
        <el-tag :type="testResults.success ? 'success' : 'danger'" size="small">
          {{ testResults.success ? '成功' : '失败' }}
        </el-tag>
      </div>
      <pre class="result-content">{{ formatTestResult(testResults) }}</pre>
    </div>
    
    <!-- 系统信息显示 -->
    <div class="debug-info">
      <div class="info-item">
        <span class="info-label">当前用户:</span>
        <span class="info-value">{{ userInfo?.name || '未登录' }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">用户角色:</span>
        <span class="info-value">{{ userInfo?.role || 'N/A' }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">登录时间:</span>
        <span class="info-value">{{ loginTime || 'N/A' }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">Token状态:</span>
        <el-tag :type="currentToken ? 'success' : 'info'" size="small">
          {{ currentToken ? '有效' : '未登录' }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Monitor, Key, Bell, Delete } from '@element-plus/icons-vue'
import { authAPI } from '@/api/auth'
import { notificationAPI } from '@/api/notification'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'DevDebugPanel' })

interface Props {
  visible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  visible: false
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'test-result', result: any): void
}>()

// Store引用
const userStore = useUserStore()

// 响应式数据
const loading = reactive({
  health: false,
  verify: false,
  notification: false,
  cache: false
})

const testResults = ref<any>(null)
const loginTime = ref<string>('')

// 计算属性
const currentToken = computed(() => userStore.token)
const userInfo = computed(() => userStore.userInfo)

// 健康检查
const handleHealthCheck = async () => {
  console.log('🏥 [调试面板] 开始健康检查...')
  loading.health = true
  testResults.value = null
  
  try {
    const result = await authAPI.healthCheck()
    console.log('✅ [调试面板] 健康检查响应:', result)
    
    testResults.value = {
      type: 'health',
      success: result.code === 0,
      data: result.data,
      message: result.msg || '健康检查完成'
    }
    
    if (result.code === 0) {
      ElMessage.success('✅ 健康检查通过')
    } else {
      ElMessage.error('❌ 健康检查失败')
    }
    
    emit('test-result', testResults.value)
  } catch (error) {
    console.error('❌ [调试面板] 健康检查异常:', error)
    testResults.value = {
      type: 'health',
      success: false,
      error: error,
      message: '健康检查异常'
    }
    ElMessage.error('健康检查异常')
  } finally {
    loading.health = false
  }
}

// Token验证
const handleTokenVerify = async () => {
  console.log('🔑 [调试面板] 开始Token验证...')
  loading.verify = true
  testResults.value = null
  
  if (!currentToken.value) {
    ElMessage.warning('请先登录')
    loading.verify = false
    return
  }
  
  try {
    const result = await authAPI.verifyToken(currentToken.value)
    console.log('✅ [调试面板] Token验证响应:', result)
    
    testResults.value = {
      type: 'token',
      success: result.code === 0,
      data: result.data,
      message: result.msg || 'Token验证完成'
    }
    
    if (result.code === 0) {
      ElMessage.success('✅ Token验证通过')
    } else {
      ElMessage.error('❌ Token验证失败')
    }
    
    emit('test-result', testResults.value)
  } catch (error: any) {
    console.error('❌ [调试面板] Token验证异常:', error)
    testResults.value = {
      type: 'token',
      success: false,
      error: error,
      message: 'Token验证异常'
    }
    ElMessage.error('Token验证异常')
  } finally {
    loading.verify = false
  }
}

// 通知API测试
const handleNotificationAPI = async () => {
  console.log('📢 [调试面板] 开始通知API测试...')
  loading.notification = true
  testResults.value = null
  
  try {
    const result = await notificationAPI.ping()
    console.log('✅ [调试面板] 通知API响应:', result)
    
    testResults.value = {
      type: 'notification',
      success: result.code === 0,
      data: result.data,
      message: result.msg || '通知API测试完成'
    }
    
    if (result.code === 0) {
      ElMessage.success('✅ 通知API连接正常')
    } else {
      ElMessage.error('❌ 通知API连接失败')
    }
    
    emit('test-result', testResults.value)
  } catch (error: any) {
    console.error('❌ [调试面板] 通知API异常:', error)
    testResults.value = {
      type: 'notification',
      success: false,
      error: error,
      message: '通知API异常'
    }
    ElMessage.error('通知API异常')
  } finally {
    loading.notification = false
  }
}

// 清空缓存
const handleClearCache = async () => {
  console.log('🗑️ [调试面板] 开始清空缓存...')
  loading.cache = true
  
  try {
    // 清空本地存储
    localStorage.clear()
    sessionStorage.clear()
    
    // 清空Store缓存
    await userStore.clearCache()
    
    testResults.value = {
      type: 'cache',
      success: true,
      message: '缓存已清空'
    }
    
    ElMessage.success('✅ 缓存清空成功')
    emit('test-result', testResults.value)
  } catch (error) {
    console.error('❌ [调试面板] 清空缓存失败:', error)
    testResults.value = {
      type: 'cache',
      success: false,
      error: error,
      message: '清空缓存失败'
    }
    ElMessage.error('清空缓存失败')
  } finally {
    loading.cache = false
  }
}

// 格式化测试结果
const formatTestResult = (result: any) => {
  if (!result) return ''
  
  const { type, success, data, error, message } = result
  
  let formatted = `测试类型: ${type}\n`
  formatted += `执行状态: ${success ? '成功' : '失败'}\n`
  formatted += `消息: ${message}\n`
  
  if (data) {
    formatted += `\n响应数据:\n${JSON.stringify(data, null, 2)}`
  }
  
  if (error) {
    formatted += `\n错误信息:\n${error.message || error}`
  }
  
  return formatted
}

// 更新登录时间
const updateLoginTime = () => {
  loginTime.value = new Date().toLocaleString('zh-CN')
}

// 暴露方法供父组件调用
defineExpose({
  updateLoginTime,
  handleHealthCheck,
  handleTokenVerify,
  handleNotificationAPI
})
</script>

<style scoped lang="scss">
// 导入CSS变量
@import '@/styles/variables.scss';

.dev-debug-panel {
  position: fixed;
  bottom: var(--spacing-xl);
  right: var(--spacing-xl);
  width: 400px;
  background: var(--color-bg-base);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card-hover);
  padding: var(--spacing-lg);
  z-index: var(--z-dialog);
  transition: all var(--duration-base) var(--ease-in-out);
  
  &:hover {
    box-shadow: var(--shadow-elevated);
  }
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border-base);
  
  .debug-title {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
  }
}

.debug-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  
  .el-button {
    width: 100%;
  }
}

.debug-results {
  background: var(--color-bg-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    
    .result-title {
      font-size: var(--font-size-base);
      font-weight: var(--font-weight-medium);
      color: var(--color-text-regular);
    }
  }
  
  .result-content {
    font-family: var(--font-family-mono);
    font-size: var(--font-size-xs);
    line-height: var(--line-height-base);
    color: var(--color-text-primary);
    background: var(--color-bg-base);
    border-radius: var(--radius-base);
    padding: var(--spacing-sm);
    margin: 0;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-wrap: break-word;
  }
}

.debug-info {
  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: calc(var(--spacing-xs) * 1.5) 0;
    font-size: var(--font-size-sm);
    
    &:not(:last-child) {
      border-bottom: 1px solid var(--color-border-lighter);
    }
    
    .info-label {
      color: var(--color-text-secondary);
      flex-shrink: 0;
    }
    
    .info-value {
      color: var(--color-text-primary);
      text-align: right;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 200px;
    }
  }
}

// 暗黑模式支持 - 使用CSS变量自动适配
@media (prefers-color-scheme: dark) {
  // CSS变量系统会自动处理暗黑模式，这里只需要特殊调整
  .dev-debug-panel {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  }
}
</style>