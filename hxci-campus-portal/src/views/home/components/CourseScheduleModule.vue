<!--
/**
 * 今日课程安排模块组件
 * 
 * @description 展示今日课程安排，包含课程状态管理和时间控制
 * @author Frontend-Developer AI Assistant
 * @date 2025-08-21
 * @stage Stage 6 - Home.vue组件拆分架构
 * 
 * @responsibilities
 * - 展示今日课程列表
 * - 管理课程状态(已结束/进行中/即将开始)
 * - 提供课程点击交互
 * - 实时更新课程状态
 * 
 * @features
 * - 智能状态判断：根据当前时间自动更新课程状态
 * - 响应式设计：适配移动端显示
 * - 可视化状态：不同状态使用不同颜色标识
 * - 空状态处理：无课程时的友好提示
 */
-->

<template>
  <div class="workspace-module-card course-module">
    <!-- 模块头部 -->
    <div class="module-header">
      <h4><el-icon><Clock /></el-icon>📚 今日课程安排</h4>
      <el-tag type="info" size="small">{{ courses.length }}节课</el-tag>
    </div>
    
    <!-- 课程列表 -->
    <div class="course-schedule-list" v-loading="isLoading">
      <!-- 课程项 -->
      <div 
        v-for="course in courses" 
        :key="course.id" 
        class="course-schedule-item"
        :class="{
          'course-completed': course.status === 'completed',
          'course-current': course.status === 'current',
          'course-upcoming': course.status === 'upcoming'
        }"
        @click="handleCourseClick(course)"
      >
        <!-- 时间信息 -->
        <div class="course-time-info">{{ course.time }}</div>
        
        <!-- 课程详情 -->
        <div class="course-details">
          <div class="course-name-main">{{ course.name }}</div>
          <div class="course-location-teacher">
            {{ course.location }} · {{ course.teacher }}
          </div>
        </div>
        
        <!-- 状态标签 -->
        <el-tag 
          :type="getStatusTagType(course.status)" 
          size="small"
          class="course-status-tag"
        >
          {{ getStatusText(course.status) }}
        </el-tag>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-if="courses.length === 0 && !isLoading" class="empty-courses">
      <el-empty description="今日无课程安排" :image-size="60">
        <template #description>
          <p style="color: #909399; font-size: 14px;">今天没有安排课程</p>
          <p style="color: #c0c4cc; font-size: 12px;">享受美好的自由时光吧！</p>
        </template>
      </el-empty>
    </div>
    
    <!-- 快捷操作 -->
    <div v-if="courses.length > 0" class="course-actions">
      <el-button type="text" size="small" @click="handleViewSchedule">
        查看完整课表
      </el-button>
      <el-button 
        v-if="currentCourse"
        type="primary" 
        size="small" 
        @click="handleJoinClass(currentCourse)"
      >
        进入当前课程
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { Clock } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

// ================== 类型定义 ==================

/** 课程状态类型 */
type CourseStatus = 'completed' | 'current' | 'upcoming'

/** 课程项接口 */
interface CourseItem {
  id: number | string
  /** 课程名称 */
  name: string
  /** 任课教师 */
  teacher: string
  /** 上课地点 */
  location: string
  /** 上课时间 */
  time: string
  /** 课程状态 */
  status: CourseStatus
  /** 开始时间 (用于状态计算) */
  startTime?: string
  /** 结束时间 (用于状态计算) */
  endTime?: string
}

// ================== Props定义 ==================

interface Props {
  /** 课程列表 */
  courses: CourseItem[]
  /** 当前时间 (用于状态计算) */
  currentTime?: Date
  /** 是否加载中 */
  isLoading?: boolean
  /** 是否自动更新状态 */
  autoUpdateStatus?: boolean
  /** 状态更新间隔 (毫秒) */
  updateInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  currentTime: () => new Date(),
  isLoading: false,
  autoUpdateStatus: true,
  updateInterval: 60000 // 1分钟
})

// ================== Emits定义 ==================

interface Emits {
  /** 课程点击事件 */
  (e: 'course-click', course: CourseItem): void
  /** 查看完整课表 */
  (e: 'view-schedule'): void
  /** 进入课程 */
  (e: 'join-class', course: CourseItem): void
  /** 状态更新事件 */
  (e: 'status-update', courses: CourseItem[]): void
}

const emit = defineEmits<Emits>()

// ================== 计算属性 ==================

/** 当前正在进行的课程 */
const currentCourse = computed(() => {
  return props.courses.find(course => course.status === 'current')
})

/** 下一节课程 */
const nextCourse = computed(() => {
  const upcomingCourses = props.courses.filter(course => course.status === 'upcoming')
  return upcomingCourses[0] // 返回最早的即将开始课程
})

/** 已结束课程数量 */
const completedCount = computed(() => {
  return props.courses.filter(course => course.status === 'completed').length
})

// ================== 状态管理 ==================

let statusUpdateTimer: NodeJS.Timeout | null = null

/** 更新课程状态 */
const updateCourseStatus = () => {
  if (!props.autoUpdateStatus) return
  
  const currentTime = props.currentTime
  const currentHour = currentTime.getHours()
  const currentMinute = currentTime.getMinutes()
  const currentTimeMinutes = currentHour * 60 + currentMinute
  
  const updatedCourses = props.courses.map(course => {
    const [startTime, endTime] = course.time.split('-')
    const [startHour, startMinute] = startTime.split(':').map(Number)
    const [endHour, endMinute] = endTime.split(':').map(Number)
    
    const startTimeMinutes = startHour * 60 + startMinute
    const endTimeMinutes = endHour * 60 + endMinute
    
    let status: CourseStatus
    if (currentTimeMinutes >= endTimeMinutes) {
      status = 'completed'
    } else if (currentTimeMinutes >= startTimeMinutes) {
      status = 'current'
    } else {
      status = 'upcoming'
    }
    
    return { ...course, status }
  })
  
  emit('status-update', updatedCourses)
}

// ================== 工具函数 ==================

/** 获取状态标签类型 */
const getStatusTagType = (status: CourseStatus): string => {
  switch (status) {
    case 'current': return 'warning'   // 进行中 - 橙色
    case 'upcoming': return 'success'  // 即将开始 - 绿色
    case 'completed': return 'info'    // 已结束 - 灰色
    default: return 'info'
  }
}

/** 获取状态文本 */
const getStatusText = (status: CourseStatus): string => {
  switch (status) {
    case 'current': return '进行中'
    case 'upcoming': return '即将开始'
    case 'completed': return '已结束'
    default: return '未知'
  }
}

/** 格式化时间 */
const formatTime = (timeStr: string): string => {
  return dayjs(`2024-01-01 ${timeStr}`).format('HH:mm')
}

// ================== 事件处理器 ==================

/** 处理课程点击 */
const handleCourseClick = (course: CourseItem) => {
  emit('course-click', course)
}

/** 处理查看课表 */
const handleViewSchedule = () => {
  emit('view-schedule')
}

/** 处理进入课程 */
const handleJoinClass = (course: CourseItem) => {
  emit('join-class', course)
}

// ================== 生命周期 ==================

onMounted(() => {
  // 初始更新状态
  updateCourseStatus()
  
  // 启动定时器
  if (props.autoUpdateStatus) {
    statusUpdateTimer = setInterval(updateCourseStatus, props.updateInterval)
  }
  
  console.log('📚 [CourseScheduleModule] 组件初始化完成')
})

onUnmounted(() => {
  // 清理定时器
  if (statusUpdateTimer) {
    clearInterval(statusUpdateTimer)
    statusUpdateTimer = null
  }
  
  console.log('📚 [CourseScheduleModule] 组件销毁完成')
})
</script>

<style scoped>
/* 课程模块样式 */
.course-module {
  background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
  border: 1px solid #dbeafe;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
}

/* 课程列表 */
.course-schedule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 课程项 */
.course-schedule-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.course-schedule-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 状态特定样式 */
.course-completed {
  opacity: 0.7;
  background: #f9fafb;
}

.course-current {
  background: linear-gradient(135deg, #fef3c7 0%, #ffffff 100%);
  border-color: #f59e0b;
  box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.2);
}

.course-upcoming {
  background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%);
  border-color: #10b981;
}

/* 时间信息 */
.course-time-info {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  min-width: 80px;
  text-align: center;
  background: #f3f4f6;
  padding: 4px 8px;
  border-radius: 4px;
}

.course-current .course-time-info {
  background: #fef3c7;
  color: #92400e;
}

.course-upcoming .course-time-info {
  background: #d1fae5;
  color: #065f46;
}

.course-completed .course-time-info {
  background: #f3f4f6;
  color: #6b7280;
}

/* 课程详情 */
.course-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-name-main {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
}

.course-location-teacher {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.3;
}

/* 状态标签 */
.course-status-tag {
  font-size: 11px;
  font-weight: 500;
}

/* 快捷操作 */
.course-actions {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

/* 空状态 */
.empty-courses {
  text-align: center;
  padding: 40px 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .course-schedule-item {
    padding: 10px 12px;
    gap: 10px;
  }
  
  .course-time-info {
    min-width: 70px;
    font-size: 12px;
  }
  
  .course-name-main {
    font-size: 13px;
  }
  
  .course-location-teacher {
    font-size: 11px;
  }
  
  .course-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .course-module {
    background: linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 100%);
    border-color: #3a3a3a;
  }
  
  .course-schedule-item {
    background: #2a2a2a;
    border-color: #3a3a3a;
  }
  
  .course-completed {
    background: #1f1f1f;
  }
  
  .course-current {
    background: linear-gradient(135deg, #2d2d1a 0%, #2a2a2a 100%);
  }
  
  .course-upcoming {
    background: linear-gradient(135deg, #1a2d1a 0%, #2a2a2a 100%);
  }
  
  .course-name-main {
    color: #e0e0e0;
  }
  
  .course-location-teacher {
    color: #9ca3af;
  }
  
  .course-time-info {
    background: #3a3a3a;
    color: #e0e0e0;
  }
}

/* 动画效果 */
.course-schedule-item {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 状态转换动画 */
.course-schedule-item {
  transition: all 0.5s ease;
}

.course-current {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.2);
  }
  50% {
    box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1);
  }
}
</style>