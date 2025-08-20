<template>
  <div class="portal-header">
    <div class="header-content">
      <div class="header-left">
        <div class="school-brand">
          <el-icon class="brand-icon" :size="28"><School /></el-icon>
          <div class="brand-info">
            <h1 class="brand-title">哈尔滨信息工程学院</h1>
            <span class="brand-subtitle">智慧校园门户</span>
          </div>
        </div>
      </div>
      
      <div class="header-right">
        <div class="user-panel" v-if="userInfo">
          <el-avatar 
            class="user-avatar" 
            :size="40"
            :icon="Avatar"
            :alt="userInfo.username"
          />
          <div class="user-details">
            <div class="user-name">{{ userInfo.username }}</div>
            <div class="user-role">{{ userInfo.roleName }}</div>
          </div>
          <el-button type="danger" size="small" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElIcon, ElAvatar, ElButton, ElMessage, ElMessageBox } from 'element-plus'
import { School, Avatar, SwitchButton } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

// Props
interface Props {
  userInfo: {
    username: string
    roleName: string
  } | null
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  logout: []
}>()

// 路由
const router = useRouter()

// 处理用户退出登录
const handleLogout = async () => {
  try {
    console.log('🔓 [HeaderNavigation] 开始处理用户退出...')
    
    // 显示确认对话框
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    console.log('✅ [HeaderNavigation] 用户确认退出')
    
    // 清理本地存储数据（保留已读状态）
    localStorage.removeItem('campus_token')
    localStorage.removeItem('campus_user_info')
    
    // 显示退出成功提示
    ElMessage.success('退出登录成功')
    
    // 通知父组件处理退出
    emit('logout')
    
    // 跳转到登录页
    console.log('🔄 [HeaderNavigation] 跳转到登录页面')
    router.push('/login')
    
  } catch (error) {
    // 用户取消退出，不显示错误
    if (error !== 'cancel') {
      console.error('❌ [HeaderNavigation] 退出过程出错:', error)
      ElMessage.error('退出登录失败')
    }
  }
}
</script>

<style scoped>
/* 顶部导航栏样式 */
.portal-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 16px 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
}

.header-left {
  flex: 1;
}

.school-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 8px;
  backdrop-filter: blur(10px);
}

.brand-info {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.brand-subtitle {
  font-size: 13px;
  opacity: 0.9;
  font-weight: 500;
}

.header-right {
  flex-shrink: 0;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.user-details {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
}

.user-role {
  font-size: 12px;
  opacity: 0.85;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .portal-header {
    padding: 12px 16px;
  }
  
  .brand-title {
    font-size: 18px;
  }
  
  .brand-subtitle {
    font-size: 12px;
  }
  
  .user-panel {
    gap: 8px;
    padding: 6px 12px;
  }
  
  .user-name {
    font-size: 13px;
  }
  
  .user-role {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .brand-title {
    font-size: 16px;
  }
  
  .school-brand {
    gap: 8px;
  }
  
  .user-details {
    display: none;
  }
}
</style>