<!--
/**
 * 校园服务卡片组件
 *
 * @description 从Home.vue中提取的校园服务展示组件，显示食堂、图书馆、校园巴士等服务信息
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 * 
 * @refactored 从Home.vue提取，解决God Component问题
 */
-->

<template>
  <div class="campus-services-card">
    <div class="card-header">
      <h4 class="card-title">
        <span class="icon">{{ titleIcon }}</span>
        {{ title }}
      </h4>
      <el-button 
        v-if="refreshable"
        :icon="Refresh" 
        size="small" 
        circle 
        @click="handleRefresh"
        :loading="loading"
      />
    </div>
    
    <div class="service-list" v-loading="loading">
      <!-- 默认服务列表 -->
      <div 
        v-for="service in displayServices" 
        :key="service.id"
        class="service-item"
        @click="handleServiceClick(service)"
      >
        <div class="service-icon">
          <component 
            :is="getServiceIcon(service.type)" 
            class="icon-component"
          />
        </div>
        <div class="service-content">
          <div class="service-title">{{ service.title }}</div>
          <div class="service-desc">{{ service.description }}</div>
        </div>
        <div class="service-arrow" v-if="service.clickable">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
      
      <!-- 自定义插槽 -->
      <slot name="custom-service"></slot>
    </div>
    
    <!-- 空态提示 -->
    <el-empty 
      v-if="!loading && displayServices.length === 0"
      :description="emptyText"
      :image-size="60"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Bell, User, Setting, Refresh, ArrowRight,
  Calendar, Location, Notebook, Monitor,
  School, Coffee, Van, Reading
} from '@element-plus/icons-vue'

defineOptions({ name: 'CampusServicesCard' })

// 服务类型定义
export interface CampusService {
  id: string | number
  type: string
  title: string
  description: string
  clickable?: boolean
  url?: string
  status?: 'normal' | 'warning' | 'offline'
  updateTime?: string
}

interface Props {
  title?: string
  titleIcon?: string
  services?: CampusService[]
  loading?: boolean
  refreshable?: boolean
  emptyText?: string
  maxDisplay?: number
}

const props = withDefaults(defineProps<Props>(), {
  title: '校园服务',
  titleIcon: '🌟',
  services: () => [],
  loading: false,
  refreshable: true,
  emptyText: '暂无服务信息',
  maxDisplay: 6
})

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'service-click', service: CampusService): void
}>()

// 默认服务数据
const defaultServices: CampusService[] = [
  {
    id: 'canteen',
    type: 'canteen',
    title: '食堂菜单',
    description: '今日推荐：宫保鸡丁',
    clickable: true,
    status: 'normal'
  },
  {
    id: 'library',
    type: 'library', 
    title: '图书馆',
    description: '开放时间：8:00-22:00',
    clickable: true,
    status: 'normal'
  },
  {
    id: 'bus',
    type: 'bus',
    title: '校园巴士',
    description: '下班班次：15分钟后',
    clickable: true,
    status: 'normal'
  },
  {
    id: 'calendar',
    type: 'calendar',
    title: '校历安排',
    description: '本周：第8教学周',
    clickable: true,
    status: 'normal'
  },
  {
    id: 'health',
    type: 'health',
    title: '医务室',
    description: '值班医生：张医生',
    clickable: true,
    status: 'normal'
  },
  {
    id: 'sports',
    type: 'sports',
    title: '体育场馆',
    description: '羽毛球场：有空位',
    clickable: true,
    status: 'normal'
  }
]

// 计算展示的服务列表
const displayServices = computed(() => {
  const list = props.services.length > 0 ? props.services : defaultServices
  return list.slice(0, props.maxDisplay)
})

// 获取服务图标
const getServiceIcon = (type: string) => {
  const iconMap: Record<string, any> = {
    canteen: Coffee,
    library: Reading,
    bus: Van,
    calendar: Calendar,
    health: Monitor,
    sports: Location,
    study: Notebook,
    admin: Setting,
    default: School
  }
  return iconMap[type] || iconMap.default
}

// 处理刷新
const handleRefresh = () => {
  console.log('🔄 [校园服务] 刷新服务信息')
  emit('refresh')
}

// 处理服务点击
const handleServiceClick = (service: CampusService) => {
  if (!service.clickable) return
  
  console.log('🔗 [校园服务] 点击服务:', service.title)
  emit('service-click', service)
  
  // 如果有URL，可以直接跳转
  if (service.url) {
    window.open(service.url, '_blank')
  } else {
    ElMessage.info(`即将跳转到${service.title}`)
  }
}
</script>

<style scoped lang="scss">
// 导入CSS变量
@import '@/styles/variables.scss';

.campus-services-card {
  background: var(--color-bg-base);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-card);
  transition: all var(--duration-base) var(--ease-in-out);
  
  &:hover {
    box-shadow: var(--shadow-card-hover);
  }
}

// 卡片头部
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
  
  .card-title {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
    margin: 0;
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    
    .icon {
      font-size: var(--font-size-xl);
    }
  }
}

// 服务列表
.service-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  min-height: 60px;
}

// 服务项
.service-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-md);
  background: var(--color-bg-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-in-out);
  
  &:hover {
    background: var(--color-bg-hover);
    transform: translateX(4px);
    
    .service-arrow {
      opacity: 1;
      transform: translateX(2px);
    }
  }
  
  &:active {
    transform: scale(0.98);
  }
}

// 服务图标
.service-icon {
  width: 36px;
  height: 36px;
  background: var(--color-bg-base);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: var(--spacing-md);
  flex-shrink: 0;
  
  .icon-component,
  .el-icon {
    font-size: var(--font-size-xl);
    color: var(--color-primary);
    line-height: 1;
  }
}

// 服务内容
.service-content {
  flex: 1;
  min-width: 0;
  
  .service-title {
    font-size: var(--font-size-base);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
    margin-bottom: var(--spacing-xs);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .service-desc {
    font-size: var(--font-size-xs);
    color: var(--color-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 箭头图标
.service-arrow {
  margin-left: var(--spacing-sm);
  color: var(--color-text-placeholder);
  opacity: 0;
  transition: all var(--duration-fast) var(--ease-in-out);
  
  .el-icon {
    font-size: var(--font-size-lg);
  }
}

// 响应式布局
@media (max-width: 768px) {
  .campus-services-card {
    padding: var(--spacing-lg);
  }
  
  .service-list {
    gap: var(--spacing-sm);
  }
  
  .service-item {
    padding: var(--spacing-sm);
  }
  
  .service-icon {
    width: 32px;
    height: 32px;
    
    .icon-component {
      font-size: calc(var(--font-size-lg) + 2px);
    }
  }
}

// 暗黑模式支持 - CSS变量系统会自动处理
@media (prefers-color-scheme: dark) {
  // CSS变量会自动切换到暗黑模式值
  // 特殊调整可以在这里添加
  .campus-services-card {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
}
</style>