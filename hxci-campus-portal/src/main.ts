import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 导入QWeather专业天气图标CSS
import 'qweather-icons/font/qweather-icons.css'

import App from './App.vue'
import router from './router'
import api from './utils/request'

/**
 * 🛡️ 应用启动时初始化CSRF Token
 * @description 解决CSRF Token缺失导致的403错误
 */
async function initializeCSRFToken() {
  try {
    console.log('🛡️ [应用启动] 初始化CSRF Token...')

    // 调用CSRF Token端点，触发Token生成和Cookie设置
    const response = await fetch('/csrf-token', {
      method: 'GET',
      credentials: 'include',  // 确保Cookie被正确设置
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      const data = await response.json()
      console.log('✅ [应用启动] CSRF Token初始化成功')
      console.log('🔐 [应用启动] CSRF响应:', data)

      // 验证Cookie是否已设置
      const cookies = document.cookie
      if (cookies.includes('XSRF-TOKEN')) {
        console.log('🍪 [应用启动] XSRF-TOKEN Cookie已设置')
      } else {
        console.warn('⚠️ [应用启动] XSRF-TOKEN Cookie未找到')
      }
    } else {
      console.error('❌ [应用启动] CSRF Token初始化失败，HTTP状态:', response.status)
      // 不阻塞应用启动，但记录错误
    }
  } catch (error) {
    console.error('💥 [应用启动] CSRF Token初始化异常:', error)
    // 继续启动应用，将在第一次API调用时再次尝试获取
  }
}

/**
 * 🚀 异步初始化应用
 */
async function initializeApp() {
  // 🛡️ 第一步：初始化CSRF Token
  await initializeCSRFToken()

  // 🎨 第二步：创建Vue应用实例
  const app = createApp(App)

  // 注册所有Element Plus图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus)

  // 🎯 第三步：挂载应用
  app.mount('#app')

  console.log('🎉 [应用启动] Vue应用启动完成')
}

// 启动应用
initializeApp().catch(error => {
  console.error('💥 [应用启动] 应用初始化失败:', error)

  // 即使初始化失败，也尝试启动基础应用
  const app = createApp(App)
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }
  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
})