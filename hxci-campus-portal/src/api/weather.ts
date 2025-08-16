import api from '@/utils/request'

// 天气数据接口类型定义
export interface WeatherData {
  cityCode: string
  cityName: string
  temperature: number
  weatherText: string
  feelsLike?: number           // 体感温度
  humidity?: number            // 湿度
  windDir?: string            // 风向
  windScale?: string          // 风力等级
  windSpeed?: number          // 风速
  pressure?: number           // 气压
  visibility?: number         // 能见度
  precipitation?: number      // 降水量
  cloudCover?: number         // 云量
  updateTime: string
  apiUpdateTime: string
}

export interface WeatherResponse {
  success: boolean
  data: WeatherData
  message?: string
}

// 天气API调用服务
export const weatherAPI = {
  /**
   * 获取当前天气缓存数据
   * 从后端数据库缓存中获取最新天气信息
   */
  async getCurrentWeather(): Promise<WeatherResponse> {
    try {
      console.log('🌤️ [天气API] 开始获取缓存天气数据...')
      
      // 调用后端天气缓存API
      const response = await api.get('/admin-api/test/weather/api/current')
      
      console.log('📥 [天气API] 响应数据:', response.data)
      
      // 适配后端返回格式: {code: 0, data: {...}, msg: ''}
      // code: 0 表示成功，其他值表示失败
      if (response.data && (response.data.code === 0 || response.data.success)) {
        console.log('✅ [天气API] 天气数据获取成功')
        return {
          success: true,
          data: response.data.data.weather,  // 提取weather子对象
          message: response.data.msg || response.data.message
        }
      } else {
        console.log('⚠️ [天气API] 后端返回失败状态，错误信息:', response.data?.msg || response.data?.message)
        return {
          success: false,
          data: this.getDefaultWeatherData(),
          message: response.data?.msg || response.data?.message || '天气数据获取失败'
        }
      }
    } catch (error: any) {
      console.error('❌ [天气API] 请求异常:', error)
      
      // 降级方案：返回默认天气数据
      return {
        success: false,
        data: this.getDefaultWeatherData(),
        message: error.message || '网络连接失败'
      }
    }
  },

  /**
   * 获取默认天气数据 (降级方案)
   * 当API失败时使用默认数据确保页面正常显示
   */
  getDefaultWeatherData(): WeatherData {
    return {
      cityCode: '101050101',
      cityName: '哈尔滨',
      temperature: -5,
      weatherText: '晴',
      feelsLike: -8,
      humidity: 45,
      windDir: '西北风',
      windScale: '3级',
      windSpeed: 12,
      pressure: 1013,
      visibility: 10,
      precipitation: 0,
      cloudCover: 20,
      updateTime: new Date().toISOString(),
      apiUpdateTime: new Date().toISOString()
    }
  },

  /**
   * 格式化天气显示文本
   */
  formatWeatherDisplay(weather: WeatherData): string {
    return `${weather.cityName} ${weather.weatherText} ${weather.temperature}°C`
  }
}