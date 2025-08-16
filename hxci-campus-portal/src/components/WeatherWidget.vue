<template>
  <div class="weather-widget">
    <div class="weather-widget__content">
      <!-- 正常天气显示 -->
      <div class="weather-widget__main" 
           v-if="!weatherLoading && weatherData"
           :style="{ 
             background: getWeatherBackgroundGradient(weatherData.weatherText),
             '--text-primary': getWeatherTextColor(weatherData.weatherText).primary,
             '--text-secondary': getWeatherTextColor(weatherData.weatherText).secondary,
             '--icon-color': getWeatherTextColor(weatherData.weatherText).icon
           }">
        <div class="weather-widget__icon">
          <!-- 使用QWeather专业天气图标 -->
          <i :class="getWeatherIcon(weatherData.weatherText)" 
             :style="{ color: getWeatherTextColor(weatherData.weatherText).icon }"></i>
        </div>
        <div class="weather-widget__details">
          <div class="weather-widget__primary">
            <span class="weather-widget__location">
              <el-icon :size="12" class="weather-widget__location-icon"><Location /></el-icon>
              {{ weatherData.cityName }}
            </span>
            <span class="weather-widget__temperature">{{ weatherData.temperature }}°C</span>
            <span class="weather-widget__text">{{ weatherData.weatherText }}</span>
          </div>
          <div class="weather-widget__secondary" v-if="hasSecondaryWeatherInfo(weatherData)">
            <div class="weather-widget__detail-item" v-if="weatherData.feelsLike && weatherData.feelsLike !== weatherData.temperature">
              <!-- 体感温度使用qi-1024图标 -->
              <i class="qi-1024 weather-widget__detail-icon"></i>
              <span class="weather-widget__feels-like">体感{{ weatherData.feelsLike }}°C</span>
            </div>
            <div class="weather-widget__detail-item" v-if="weatherData.humidity">
              <!-- 湿度使用qi-399图标 -->
              <i class="qi-399 weather-widget__detail-icon"></i>
              <span class="weather-widget__humidity">{{ weatherData.humidity }}%</span>
            </div>
            <div class="weather-widget__detail-item" v-if="weatherData.windDir && weatherData.windScale">
              <!-- 风力使用qi-2001图标 -->
              <i class="qi-2001 weather-widget__detail-icon"></i>
              <span class="weather-widget__wind">{{ weatherData.windDir }}{{ weatherData.windScale.includes('级') ? weatherData.windScale : weatherData.windScale + '级' }}</span>
            </div>
            <div class="weather-widget__detail-item" v-if="weatherData.precipitation && weatherData.precipitation > 0">
              <!-- 降水使用qi-316图标 -->
              <i class="qi-316 weather-widget__detail-icon"></i>
              <span class="weather-widget__precipitation">{{ weatherData.precipitation }}mm</span>
            </div>
          </div>
        </div>
        
        <!-- 移除渐变背景，保持简洁 -->
      </div>
      
      <!-- 加载状态 -->
      <div class="weather-widget__loading" v-else-if="weatherLoading">
        <el-icon class="weather-widget__loading-icon"><Loading /></el-icon>
        <span>加载天气中...</span>
      </div>
      
      <!-- 默认状态 -->
      <div class="weather-widget__default" v-else>
        <i class="qi-100" style="font-size: 20px;"></i>
        <span>{{ weatherDisplay }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { 
  Loading,
  Location
} from '@element-plus/icons-vue'
import { useWeatherData } from '@/composables/useWeatherData'
import type { WeatherData } from '@/api/weather'

/**
 * 🌤️ 天气组件 - 专注于天气信息显示
 * 功能：和风天气专业图标 + 完整气象信息 + 动态渐变背景 + 企业级UI效果
 */

// 使用组合式函数管理天气数据
const {
  weatherLoading,
  weatherData,
  weatherDisplay
} = useWeatherData()

/**
 * 🌈 天气图标映射函数 (QWeather专业图标)
 * 根据天气文本返回对应的QWeather专业图标类名
 */
const getWeatherIcon = (weatherText: string): string => {
  // 晴天系列
  if (weatherText.includes('晴')) return 'qi-100'
  
  // 多云/阴天系列
  if (weatherText.includes('多云')) return 'qi-101'
  if (weatherText.includes('少云')) return 'qi-102'
  if (weatherText.includes('晴间多云')) return 'qi-103'
  if (weatherText.includes('阴')) return 'qi-104'
  
  // 雨天系列
  if (weatherText.includes('阵雨')) return 'qi-300'
  if (weatherText.includes('强阵雨')) return 'qi-301'
  if (weatherText.includes('雷阵雨')) return 'qi-302'
  if (weatherText.includes('强雷阵雨')) return 'qi-303'
  if (weatherText.includes('雷阵雨伴有冰雹')) return 'qi-304'
  if (weatherText.includes('小雨')) return 'qi-305'
  if (weatherText.includes('中雨')) return 'qi-306'
  if (weatherText.includes('大雨')) return 'qi-307'
  if (weatherText.includes('极端降雨')) return 'qi-308'
  if (weatherText.includes('毛毛雨')) return 'qi-309'
  if (weatherText.includes('暴雨')) return 'qi-310'
  if (weatherText.includes('大暴雨')) return 'qi-311'
  if (weatherText.includes('特大暴雨')) return 'qi-312'
  if (weatherText.includes('冻雨')) return 'qi-313'
  if (weatherText.includes('小到中雨')) return 'qi-314'
  if (weatherText.includes('中到大雨')) return 'qi-315'
  if (weatherText.includes('大到暴雨')) return 'qi-316'
  if (weatherText.includes('暴雨到大暴雨')) return 'qi-317'
  if (weatherText.includes('大暴雨到特大暴雨')) return 'qi-318'
  if (weatherText.includes('雨')) return 'qi-305' // 通用雨天
  
  // 雪天系列
  if (weatherText.includes('阵雪')) return 'qi-400'
  if (weatherText.includes('小雪')) return 'qi-401'
  if (weatherText.includes('中雪')) return 'qi-402'
  if (weatherText.includes('大雪')) return 'qi-403'
  if (weatherText.includes('暴雪')) return 'qi-404'
  if (weatherText.includes('雨夹雪')) return 'qi-405'
  if (weatherText.includes('雨雪天气')) return 'qi-406'
  if (weatherText.includes('阵雨夹雪')) return 'qi-407'
  if (weatherText.includes('雪')) return 'qi-401' // 通用雪天
  
  // 雾霾系列
  if (weatherText.includes('薄雾')) return 'qi-500'
  if (weatherText.includes('雾')) return 'qi-501'
  if (weatherText.includes('霾')) return 'qi-502'
  if (weatherText.includes('扬沙')) return 'qi-503'
  if (weatherText.includes('浮尘')) return 'qi-504'
  if (weatherText.includes('沙尘暴')) return 'qi-507'
  if (weatherText.includes('强沙尘暴')) return 'qi-508'
  
  // 特殊天气
  if (weatherText.includes('热')) return 'qi-900'
  if (weatherText.includes('冷')) return 'qi-901'
  if (weatherText.includes('风')) return 'qi-804'
  
  // 默认晴天图标
  return 'qi-100'
}

/**
 * 🎨 天气图标颜色映射
 * 根据天气状况返回对应的颜色值
 */
const getWeatherIconColor = (weatherText: string) => {
  if (weatherText.includes('晴')) return '#FFD700' // 金黄色
  if (weatherText.includes('雨') || weatherText.includes('阵雨')) return '#4A90E2' // 蓝色
  if (weatherText.includes('雷')) return '#9B59B6' // 紫色
  if (weatherText.includes('雪')) return '#87CEEB' // 天蓝色
  if (weatherText.includes('云') || weatherText.includes('多云')) return '#95A5A6' // 灰色
  if (weatherText.includes('阴')) return '#7F8C8D' // 深灰色
  if (weatherText.includes('风')) return '#16A085' // 青色
  return '#FFD700' // 默认金黄色
}

/**
 * 🎨 动态天气背景颜色
 * 根据天气状况返回对应的背景渐变色
 */
const getWeatherBackgroundGradient = (weatherText: string) => {
  // 晴天系列 - 金黄到天蓝渐变
  if (weatherText.includes('晴')) {
    return 'linear-gradient(135deg, #FFD700 0%, #87CEEB 100%)'
  }
  
  // 多云系列 - 蓝天到白云渐变
  if (weatherText.includes('多云') || weatherText.includes('少云') || weatherText.includes('晴间多云')) {
    return 'linear-gradient(135deg, #4A90E2 0%, #87CEEB 50%, #E6F3FF 100%)'
  }
  
  // 阴天 - 灰蓝渐变
  if (weatherText.includes('阴')) {
    return 'linear-gradient(135deg, #7F8C8D 0%, #95A5A6 100%)'
  }
  
  // 雨天系列 - 深蓝到灰蓝渐变
  if (weatherText.includes('雨') || weatherText.includes('阵雨') || weatherText.includes('雷')) {
    return 'linear-gradient(135deg, #2C3E50 0%, #4A90E2 100%)'
  }
  
  // 雪天系列 - 浅蓝到白色渐变
  if (weatherText.includes('雪')) {
    return 'linear-gradient(135deg, #87CEEB 0%, #F0F8FF 100%)'
  }
  
  // 雾霾系列 - 灰棕渐变
  if (weatherText.includes('雾') || weatherText.includes('霾')) {
    return 'linear-gradient(135deg, #95A5A6 0%, #BDC3C7 100%)'
  }
  
  // 默认 - 经典蓝色渐变
  return 'linear-gradient(135deg, #4A90E2 0%, #7B68EE 100%)'
}

/**
 * 🎨 动态文字颜色
 * 根据天气背景返回合适的文字颜色，确保可读性
 */
const getWeatherTextColor = (weatherText: string) => {
  // 多云/少云系列 - 蓝色背景需要白色文字确保可读性
  if (weatherText.includes('多云') || weatherText.includes('少云') || weatherText.includes('晴间多云')) {
    return {
      primary: '#FFFFFF',        // 纯白色 - 主要文字(温度等)
      secondary: 'rgba(255, 255, 255, 0.9)',  // 半透明白色 - 次要文字
      icon: '#FFFFFF'            // 白色图标
    }
  }
  
  // 晴天系列 - 金黄到蓝色渐变，使用深色文字
  if (weatherText.includes('晴')) {
    return {
      primary: '#2C3E50',    // 深蓝灰色 - 主要文字
      secondary: '#34495E',  // 中等深度 - 次要文字  
      icon: '#2C3E50'        // 图标颜色
    }
  }
  
  // 雪天背景较浅，使用深色文字
  if (weatherText.includes('雪')) {
    return {
      primary: '#2C3E50',
      secondary: '#34495E', 
      icon: '#3498DB'        // 蓝色图标突出雪天主题
    }
  }
  
  // 深色背景使用浅色文字
  if (weatherText.includes('雨') || weatherText.includes('阵雨') || weatherText.includes('雷') || 
      weatherText.includes('阴') || weatherText.includes('雾') || weatherText.includes('霾')) {
    return {
      primary: '#FFFFFF',
      secondary: 'rgba(255, 255, 255, 0.9)',
      icon: '#FFFFFF'
    }
  }
  
  // 默认白色文字（适合大多数背景）
  return {
    primary: '#FFFFFF',
    secondary: 'rgba(255, 255, 255, 0.9)',
    icon: '#FFFFFF'
  }
}

/**
 * 📊 检查是否有次要天气信息
 * 用于控制详细天气信息的显示
 */
const hasSecondaryWeatherInfo = (weather: WeatherData) => {
  return (
    (weather.feelsLike && weather.feelsLike !== weather.temperature) ||
    weather.humidity ||
    (weather.windDir && weather.windScale) ||
    (weather.precipitation && weather.precipitation > 0)
  )
}
</script>

<style scoped>
/* 🌤️ 天气组件根容器 - 使用BEM命名避免冲突 */
.weather-widget {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 280px;
}

.weather-widget__content {
  position: relative;
  background: transparent; /* 移除背景，让内层动态背景显示 */
  backdrop-filter: none;
  border-radius: 12px;
  padding: 0; /* 移除外层padding，由内层控制 */
  border: none;
  box-shadow: none;
  transition: all 0.3s ease;
}

.weather-widget__content:hover {
  transform: translateY(-2px);
}

.weather-widget__main {
  display: flex;
  align-items: center;
  gap: 14px;
  border-radius: 12px;
  padding: 16px 20px;
  /* 动态背景将由Vue样式绑定设置 */
  background: linear-gradient(135deg, #4A90E2 0%, #7B68EE 100%); /* 默认背景 */
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.weather-widget__main:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  transform: scale(1.02);
}

/* 🎯 天气图标容器样式 */
.weather-widget__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(3px);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

/* 🎯 QWeather专业天气图标样式 */
.weather-widget__icon i {
  font-size: 24px;
  color: var(--icon-color, #fff);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
  opacity: 0.95;
  display: inline-block;
  /* QWeather图标字体加载 */
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

.weather-widget__details {
  flex: 1;
}

.weather-widget__primary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.weather-widget__location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.9));
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.weather-widget__location-icon {
  opacity: 0.8;
  color: var(--icon-color, #fff);
}

.weather-widget__temperature {
  font-size: 19px;
  color: var(--text-primary, #fff);
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
  letter-spacing: -0.3px;
}

.weather-widget__text {
  font-size: 13px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.85));
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.weather-widget__secondary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 11px;
}

.weather-widget__detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.8));
  white-space: nowrap;
}

.weather-widget__detail-icon {
  font-size: 12px;
  opacity: 0.9;
  color: var(--icon-color, rgba(255, 255, 255, 0.9));
  margin-right: 2px;
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

.weather-widget__loading {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  padding: 8px 0;
}

.weather-widget__loading-icon {
  animation: weather-widget-spin 1s linear infinite;
}

.weather-widget__default {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  padding: 8px 0;
}

.weather-widget__default i {
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
  color: rgba(255, 255, 255, 0.8);
}

@keyframes weather-widget-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .weather-widget {
    min-width: 240px;
  }
  
  .weather-widget__content {
    padding: 12px 14px;
    border-radius: 10px;
  }
  
  .weather-widget__main {
    gap: 12px;
  }
  
  .weather-widget__icon {
    width: 38px;
    height: 38px;
    border-radius: 8px;
  }
  
  .weather-widget__icon i {
    font-size: 20px;
  }
  
  .weather-widget__temperature {
    font-size: 17px;
  }
  
  .weather-widget__secondary {
    gap: 8px;
    font-size: 10px;
  }
}
</style>