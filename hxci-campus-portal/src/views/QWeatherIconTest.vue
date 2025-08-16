<template>
  <div class="icon-test-page">
    <h1>QWeather图标测试页面</h1>
    
    <div class="test-section">
      <h2>主要天气图标</h2>
      <div class="icon-grid">
        <div class="icon-item" v-for="icon in mainWeatherIcons" :key="icon.class">
          <i :class="icon.class" class="test-icon"></i>
          <span class="icon-label">{{ icon.name }}</span>
          <code class="icon-class">{{ icon.class }}</code>
        </div>
      </div>
    </div>

    <div class="test-section">
      <h2>辅助信息图标</h2>
      <div class="icon-grid">
        <div class="icon-item" v-for="icon in auxiliaryIcons" :key="icon.class">
          <i :class="icon.class" class="test-icon"></i>
          <span class="icon-label">{{ icon.name }}</span>
          <code class="icon-class">{{ icon.class }}</code>
        </div>
      </div>
    </div>

    <div class="test-section">
      <h2>WeatherWidget预览</h2>
      <div class="widget-preview">
        <WeatherWidget />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import WeatherWidget from '@/components/WeatherWidget.vue'

// 主要天气图标
const mainWeatherIcons = [
  { class: 'qi-100', name: '晴天' },
  { class: 'qi-101', name: '多云' },
  { class: 'qi-102', name: '少云' },
  { class: 'qi-103', name: '晴间多云' },
  { class: 'qi-104', name: '阴天' },
  { class: 'qi-300', name: '阵雨' },
  { class: 'qi-301', name: '强阵雨' },
  { class: 'qi-302', name: '雷阵雨' },
  { class: 'qi-305', name: '小雨' },
  { class: 'qi-306', name: '中雨' },
  { class: 'qi-307', name: '大雨' },
  { class: 'qi-309', name: '毛毛雨' },
  { class: 'qi-400', name: '阵雪' },
  { class: 'qi-401', name: '小雪' },
  { class: 'qi-402', name: '中雪' },
  { class: 'qi-500', name: '薄雾' },
  { class: 'qi-501', name: '雾' },
  { class: 'qi-502', name: '霾' }
]

// 辅助信息图标
const auxiliaryIcons = [
  { class: 'qi-1001', name: '风向/风力' },
  { class: 'qi-1002', name: '湿度' },
  { class: 'qi-1003', name: '体感温度' }
]
</script>

<style scoped>
.icon-test-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 40px;
}

.test-section {
  margin-bottom: 40px;
}

h2 {
  color: #555;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 10px;
  margin-bottom: 20px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 20px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #f9f9f9;
  transition: all 0.3s ease;
}

.icon-item:hover {
  background: #f0f0f0;
  border-color: #d0d0d0;
  transform: translateY(-2px);
}

.test-icon {
  font-size: 32px;
  color: #4A90E2;
  margin-bottom: 10px;
  /* QWeather图标字体支持 */
  font-family: 'qweather-icons', sans-serif;
  font-style: normal;
  font-weight: normal;
  speak: none;
  text-decoration: inherit;
  text-transform: none;
  text-rendering: auto;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.icon-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
  text-align: center;
}

.icon-class {
  font-size: 12px;
  color: #999;
  background: #e8e8e8;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
}

.widget-preview {
  display: flex;
  justify-content: center;
  padding: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .icon-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 15px;
  }
  
  .test-icon {
    font-size: 24px;
  }
  
  .icon-item {
    padding: 15px;
  }
}
</style>