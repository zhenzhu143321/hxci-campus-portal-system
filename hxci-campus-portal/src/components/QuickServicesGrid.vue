<template>
  <div class="quick-services">
    <div class="section-header">
      <h3><el-icon><Setting /></el-icon>快捷服务</h3>
    </div>
    <div class="service-grid">
      <div 
        v-for="service in quickServices" 
        :key="service.id" 
        class="service-item"
        :class="{ disabled: !service.available }"
        @click="handleServiceClick(service)"
      >
        <div class="service-icon" :style="{ color: service.color }">
          <el-icon :size="24">
            <Bell />
          </el-icon>
        </div>
        <div class="service-info">
          <div class="service-name">{{ service.name }}</div>
          <div class="service-desc">{{ service.desc }}</div>
        </div>
        <div class="service-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElIcon, ElMessage } from 'element-plus'
import { Setting, Bell, ArrowRight } from '@element-plus/icons-vue'

// 快捷服务列表
const quickServices = computed(() => [
  {
    id: 'education',
    name: '教务系统',
    desc: '课程查询、成绩管理',
    color: '#409EFF',
    available: false
  },
  {
    id: 'student-affairs',
    name: '学工系统',  
    desc: '学生管理、事务办理',
    color: '#67C23A',
    available: false
  },
  {
    id: 'library',
    name: '图书馆',
    desc: '图书借阅、座位预约',
    color: '#E6A23C',
    available: false
  },
  {
    id: 'finance',
    name: '财务查询',
    desc: '学费、奖学金查询',
    color: '#F56C6C',
    available: false
  },
  {
    id: 'dormitory',
    name: '宿舍管理',
    desc: '宿舍分配、报修',
    color: '#909399',
    available: false
  },
  {
    id: 'course-selection',
    name: '选课系统',
    desc: '课程选择、时间表',
    color: '#9C27B0',
    available: false
  }
])

// 处理服务点击
const handleServiceClick = (service: any) => {
  if (!service.available) {
    ElMessage.info(`${service.name} 功能即将上线，敬请期待`)
    return
  }
  
  console.log('🎯 [快捷服务] 点击服务:', service.name)
  ElMessage.info(`正在打开 ${service.name}...`)
}
</script>

<style scoped>
/* 左侧快捷服务区 - 现代化卡片 */
.quick-services {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(30, 58, 138, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.1);
  padding: 24px;
  height: fit-content;
  transition: all 0.3s ease;
}

.quick-services:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(30, 58, 138, 0.12);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  color: #262626;
}

.service-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  background: rgba(240, 249, 255, 0.3);
}

.service-item:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(16, 185, 129, 0.05) 100%);
  border-color: rgba(59, 130, 246, 0.2);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

.service-item.disabled {
  opacity: 0.7;
}

.service-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.service-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.service-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 4px;
}

.service-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

.service-arrow {
  color: #bfbfbf;
  transition: all 0.3s ease;
}

.service-item:hover .service-arrow {
  color: #1890ff;
  transform: translateX(2px);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .quick-services {
    padding: 16px;
  }
  
  .service-item {
    padding: 12px;
  }
  
  .service-icon {
    width: 36px;
    height: 36px;
  }
}

@media (max-width: 768px) {
  .service-grid {
    gap: 8px;
  }
  
  .service-name {
    font-size: 13px;
  }
  
  .service-desc {
    font-size: 11px;
  }
}
</style>