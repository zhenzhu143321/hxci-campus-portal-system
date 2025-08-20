<template>
  <div class="welcome-banner">
    <div class="welcome-content">
      <h2 class="greeting">{{ getGreeting() }}，{{ userInfo?.username }}！</h2>
      <p class="date-info">{{ getCurrentDate() }}</p>
    </div>
    <!-- 天气组件 -->
    <WeatherWidget />
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import WeatherWidget from '@/components/WeatherWidget.vue'

// Props
interface Props {
  userInfo: {
    username: string
    roleName: string
  } | null
}

const props = defineProps<Props>()

// 获取问候语
const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好'
  return '夜深了'
}

// 获取当前日期
const getCurrentDate = () => {
  return dayjs().format('YYYY年MM月DD日 dddd')
}
</script>

<style scoped>
/* 问候横幅 - 清新学院风设计 */
.welcome-banner {
  background: linear-gradient(135deg, #1E3A8A 0%, #3B82F6 100%);
  color: white;
  padding: 32px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-radius: 16px;
  margin: 0 24px 24px 24px;
  box-shadow: 0 8px 32px rgba(30, 58, 138, 0.2);
  position: relative;
  overflow: hidden;
}

/* 横幅装饰元素 */
.welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.15) 0%, transparent 70%);
  border-radius: 50%;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.15) 0%, transparent 70%);
  border-radius: 50%;
}

.greeting {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.date-info {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.welcome-content {
  z-index: 1;
  position: relative;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-banner {
    padding: 16px;
  }
  
  .greeting {
    font-size: 18px;
  }
}
</style>