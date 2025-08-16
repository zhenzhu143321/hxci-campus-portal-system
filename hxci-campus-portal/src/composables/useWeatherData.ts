import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { weatherAPI } from '@/api/weather'
import type { WeatherData } from '@/api/weather'

/**
 * 天气数据管理组合式函数
 * 功能：状态管理 + API调用 + 定时刷新 + 页面可见性检测
 */
export const useWeatherData = () => {
  // 🌤️ 响应式状态
  const weatherLoading = ref(false)
  const weatherData = ref<WeatherData | null>(null)
  
  // 🕐 定时器管理
  let refreshTimer: NodeJS.Timeout | null = null
  let visibilityTimer: NodeJS.Timeout | null = null
  
  // 💾 缓存管理
  const CACHE_DURATION = 30 * 60 * 1000 // 30分钟缓存
  let lastFetchTime = 0
  
  // 📊 计算属性
  const weatherDisplay = computed(() => {
    if (weatherData.value) {
      return weatherAPI.formatWeatherDisplay(weatherData.value)
    }
    return '哈尔滨 晴 -5°C'
  })
  
  /**
   * 🌤️ 加载天气数据
   * 支持缓存机制，避免频繁API调用
   */
  const loadWeatherData = async (forceRefresh = false) => {
    const now = Date.now()
    
    // 检查缓存是否有效
    if (!forceRefresh && weatherData.value && (now - lastFetchTime < CACHE_DURATION)) {
      console.log('🌤️ [Weather] 使用缓存数据，避免重复请求')
      return
    }
    
    console.log('🌤️ [Weather] 开始加载天气数据...')
    weatherLoading.value = true
    
    try {
      const result = await weatherAPI.getCurrentWeather()
      
      if (result.success && result.data) {
        weatherData.value = result.data
        lastFetchTime = now
        console.log('✅ [Weather] 天气数据加载成功:', result.data)
        ElMessage.success('天气数据已更新')
      } else {
        console.log('⚠️ [Weather] API返回失败，使用默认数据')
        weatherData.value = weatherAPI.getDefaultWeatherData()
        ElMessage.warning('使用默认天气数据')
      }
    } catch (error: any) {
      console.error('❌ [Weather] 加载天气数据失败:', error)
      weatherData.value = weatherAPI.getDefaultWeatherData()
      ElMessage.error('天气数据加载失败，使用默认数据')
    } finally {
      weatherLoading.value = false
      console.log('🏁 [Weather] 天气数据加载完成')
    }
  }
  
  /**
   * ⏰ 启动定时刷新机制
   * 每30分钟自动刷新天气数据
   */
  const startAutoRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
    }
    
    refreshTimer = setInterval(() => {
      console.log('⏰ [Weather] 定时刷新天气数据')
      loadWeatherData(true)
    }, CACHE_DURATION)
    
    console.log('⏰ [Weather] 定时刷新已启动 (30分钟间隔)')
  }
  
  /**
   * 👁️ 页面可见性检测刷新
   * 页面重新可见时检查数据时效性
   */
  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      const now = Date.now()
      if (now - lastFetchTime > CACHE_DURATION) {
        console.log('👁️ [Weather] 页面重新可见，检测到数据过期，刷新天气')
        loadWeatherData(true)
      }
    }
  }
  
  /**
   * 🧪 测试天气API连接
   */
  const testWeatherAPI = async () => {
    console.log('=== 天气API测试开始 ===')
    console.log('🌤️ [Weather] 开始测试天气缓存服务连接...')
    
    const testLoading = ref(false)
    testLoading.value = true
    
    try {
      console.log('📤 [Weather] 发送天气API请求...')
      
      const result = await weatherAPI.getCurrentWeather()
      console.log('📥 [Weather] API响应:', result)
      
      if (result.success) {
        console.log('✅ [Weather] API连接成功')
        console.log('🌤️ [Weather] 天气数据:', result.data)
        ElMessage.success(`天气API正常 - ${result.data.cityName} ${result.data.weatherText} ${result.data.temperature}°C`)
      } else {
        console.log('⚠️ [Weather] API返回失败')
        ElMessage.warning(`天气API异常: ${result.message}`)
      }
    } catch (error: any) {
      console.log('❌ [Weather] API测试异常:', error)
      ElMessage.error(`天气API测试失败: ${error.message}`)
    } finally {
      testLoading.value = false
      console.log('=== 天气API测试结束 ===')
    }
  }
  
  /**
   * 🔧 组件挂载时初始化
   */
  onMounted(() => {
    console.log('🚀 [Weather] 天气数据管理初始化')
    
    // 立即加载数据
    loadWeatherData()
    
    // 启动定时刷新
    startAutoRefresh()
    
    // 监听页面可见性变化
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })
  
  /**
   * 🧹 组件卸载时清理
   */
  onUnmounted(() => {
    console.log('🧹 [Weather] 清理天气数据管理资源')
    
    // 清理定时器
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    
    if (visibilityTimer) {
      clearTimeout(visibilityTimer)
      visibilityTimer = null
    }
    
    // 移除事件监听
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })
  
  return {
    // 状态
    weatherLoading,
    weatherData,
    weatherDisplay,
    
    // 方法
    loadWeatherData,
    testWeatherAPI
  }
}