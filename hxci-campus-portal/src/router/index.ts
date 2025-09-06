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
  },
  {
    path: '/permission-test',
    name: 'PermissionTest',
    component: () => import('@/views/PermissionTest.vue'),
    meta: {
      title: 'P0权限缓存系统测试 - 哈信工校园门户',
      requiresAuth: false  // 测试页面可以无需登录访问，便于调试
    }
  },
  {
    path: '/test-level4-bug',
    name: 'TestLevel4Bug',
    component: () => import('@/views/TestLevel4Bug.vue'),
    meta: {
      title: 'Level 4通知Bug修复测试 - 哈信工校园门户',
      requiresAuth: false  // 测试页面可以无需登录访问，便于调试
    }
  },
  {
    path: '/test-level4-student-filter',
    name: 'TestLevel4StudentFilter',
    component: () => import('@/views/TestLevel4StudentFilter.vue'),
    meta: {
      title: '第4层学号过滤逻辑测试 - 哈信工校园门户',
      requiresAuth: false  // 测试页面可以无需登录访问，便于调试
    }
  },
  {
    path: '/todo-container-test',
    name: 'TodoContainerTest',
    component: () => import('@/views/TodoContainerTest.vue'),
    meta: {
      title: 'TodoNotificationContainer第2层测试 - 哈信工校园门户',
      requiresAuth: false  // 测试页面可以无需登录访问，便于调试
    }
  },
  {
    path: '/todo-container-layer3-test',
    name: 'TodoContainerLayer3Test',
    component: () => import('@/views/TodoContainerLayer3Test.vue'),
    meta: {
      title: 'TodoNotificationContainer第3层测试 - 哈信工校园门户',
      requiresAuth: false  // 测试页面可以无需登录访问，便于调试
    }
  },
  {
    path: '/test-level5-grade-class-filter',
    name: 'TestLevel5GradeClassFilter',
    component: () => import('@/views/TestLevel5GradeClassFilter.vue'),
    meta: {
      title: '第5层年级班级过滤逻辑测试 - 哈信工校园门户',
      requiresAuth: false  // 测试页面可以无需登录访问，便于调试
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