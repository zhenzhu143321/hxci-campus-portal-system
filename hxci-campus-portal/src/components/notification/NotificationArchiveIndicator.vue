<template>
  <div 
    class="notification-archive-indicator"
    :class="{
      'is-read': isRead,
      'is-animating': isAnimating,
      'show-hover-effect': showHoverEffect
    }"
    @click="handleClick"
  >
    <!-- 已读状态标识 -->
    <div v-if="isRead" class="read-indicator">
      <div class="read-overlay">
        <div class="read-checkmark">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="read-label">已读</div>
      </div>
      
      <!-- 归档时间显示 -->
      <div v-if="archiveTime" class="archive-time">
        归档于 {{ formatArchiveTime(archiveTime) }}
      </div>
    </div>
    
    <!-- 未读状态的标记已读按钮 -->
    <div v-else class="unread-actions">
      <el-button 
        type="success" 
        size="small" 
        @click.stop="$emit('mark-read')"
        class="mark-read-btn"
        :loading="isMarkingRead"
      >
        <el-icon><Check /></el-icon>
        标记已读
      </el-button>
    </div>
    
    <!-- 归档动画效果层 -->
    <div v-if="isAnimating" class="archive-animation-layer">
      <div class="animation-content">
        <el-icon class="archive-icon"><FolderChecked /></el-icon>
        <span class="animation-text">已归档</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { CircleCheck, Check, FolderChecked } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

interface Props {
  /** 是否已读 */
  isRead: boolean
  /** 归档时间 */
  archiveTime?: string | Date
  /** 是否正在标记已读 */
  isMarkingRead?: boolean
  /** 是否显示悬停效果 */
  showHoverEffect?: boolean
  /** 指示器主题色 */
  theme?: 'success' | 'info' | 'warning'
}

interface Emits {
  (e: 'mark-read'): void
  (e: 'click'): void
}

const props = withDefaults(defineProps<Props>(), {
  isMarkingRead: false,
  showHoverEffect: true,
  theme: 'success'
})

const emit = defineEmits<Emits>()

// 动画状态
const isAnimating = ref(false)

// 格式化归档时间
const formatArchiveTime = (time: string | Date): string => {
  const archiveDate = dayjs(time)
  const now = dayjs()
  
  // 如果是今天，显示相对时间
  if (archiveDate.isSame(now, 'day')) {
    const diffMinutes = now.diff(archiveDate, 'minute')
    if (diffMinutes < 1) return '刚刚'
    if (diffMinutes < 60) return `${diffMinutes}分钟前`
    return archiveDate.format('HH:mm')
  }
  
  // 如果是昨天
  if (archiveDate.isSame(now.subtract(1, 'day'), 'day')) {
    return `昨天 ${archiveDate.format('HH:mm')}`
  }
  
  // 其他情况显示日期
  return archiveDate.format('MM-DD HH:mm')
}

// 触发归档动画
const triggerArchiveAnimation = () => {
  isAnimating.value = true
  setTimeout(() => {
    isAnimating.value = false
  }, 1500)
}

// 处理点击事件
const handleClick = () => {
  emit('click')
}

// 主题颜色计算
const themeColor = computed(() => {
  switch (props.theme) {
    case 'success': return '#67C23A'
    case 'info': return '#409EFF'
    case 'warning': return '#E6A23C'
    default: return '#67C23A'
  }
})

// 暴露方法给父组件
defineExpose({
  triggerArchiveAnimation
})
</script>

<style scoped>
.notification-archive-indicator {
  position: relative;
  transition: all 0.3s ease;
}

/* 已读状态样式 */
.is-read {
  opacity: 0.85;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.05) 0%, rgba(255, 255, 255, 0.9) 100%);
  border-radius: 8px;
  border-left: 4px solid #67C23A;
}

.is-read.show-hover-effect:hover {
  opacity: 1;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.08) 0%, rgba(255, 255, 255, 0.95) 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.15);
}

/* 已读指示器 */
.read-indicator {
  position: relative;
}

.read-overlay {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(103, 194, 58, 0.1);
  border-radius: 6px;
  margin-bottom: 4px;
}

.read-checkmark {
  color: #67C23A;
  font-size: 16px;
  display: flex;
  align-items: center;
}

.read-label {
  font-size: 12px;
  color: #67C23A;
  font-weight: 500;
}

.archive-time {
  font-size: 11px;
  color: #909399;
  text-align: right;
  padding: 2px 8px;
  font-style: italic;
}

/* 未读状态操作按钮 */
.unread-actions {
  display: flex;
  justify-content: flex-end;
  padding: 8px;
}

.mark-read-btn {
  min-width: 80px;
  height: 32px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.mark-read-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.2);
}

/* 归档动画层 */
.archive-animation-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.9) 0%, rgba(67, 160, 71, 0.9) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  z-index: 10;
  animation: archiveSlideIn 1.5s ease-out forwards;
}

.animation-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: white;
  text-align: center;
}

.archive-icon {
  font-size: 28px;
  animation: iconBounce 0.6s ease-out;
}

.animation-text {
  font-size: 14px;
  font-weight: 600;
  animation: textFadeIn 0.8s ease-out 0.3s both;
}

/* 动画关键帧 */
@keyframes archiveSlideIn {
  0% {
    opacity: 0;
    transform: translateX(-100%);
  }
  40% {
    opacity: 1;
    transform: translateX(0);
  }
  100% {
    opacity: 0;
    transform: translateX(100%);
  }
}

@keyframes iconBounce {
  0%, 20%, 60%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  80% {
    transform: translateY(-5px);
  }
}

@keyframes textFadeIn {
  0% {
    opacity: 0;
    transform: translateY(10px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .read-overlay {
    padding: 6px 10px;
    gap: 6px;
  }
  
  .read-label {
    font-size: 11px;
  }
  
  .archive-time {
    font-size: 10px;
  }
  
  .mark-read-btn {
    min-width: 70px;
    height: 28px;
    font-size: 12px;
  }
}
</style>