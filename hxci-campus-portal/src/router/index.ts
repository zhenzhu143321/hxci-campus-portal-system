import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '登录 - 哈信工校园门户'
    }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: {
      title: '首页 - 哈信工校园门户',
      requiresAuth: true
    }
  },
  {
    path: '/todo-management',
    name: 'TodoManagement',
    component: () => import('@/views/TodoManagement.vue'),
    meta: {
      title: '待办管理 - 哈信工校园门户',
      requiresAuth: true
    }
  },
  {
    path: '/qweather-test',
    name: 'QWeatherTest',
    component: () => import('@/views/QWeatherIconTest.vue'),
    meta: {
      title: 'QWeather图标测试 - 哈信工校园门户',
      requiresAuth: false
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫 - 检查登录状态
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta?.title) {
    document.title = to.meta.title as string
  }
  
  // 检查是否需要登录
  if (to.meta?.requiresAuth) {
    const token = localStorage.getItem('campus_token')
    if (!token) {
      next('/login')
      return
    }
  }
  
  next()
})

export default router