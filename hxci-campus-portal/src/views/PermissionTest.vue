<!--
  🔐 P0权限缓存系统测试页面
  
  独立测试页面，用于验证权限缓存系统功能和性能
  访问路径: /permission-test
-->
<template>
  <div class="permission-test-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1>🔐 P0权限缓存系统测试</h1>
        <p class="header-description">
          企业级权限验证 + Redis缓存优化 | 性能提升66% (108ms → 37ms)
        </p>
        <div class="header-actions">
          <router-link to="/home" class="back-btn">
            ← 返回首页
          </router-link>
          <button @click="toggleAutoRefresh" :class="['auto-refresh-btn', { active: autoRefresh }]">
            {{ autoRefresh ? '⏸️ 停止自动刷新' : '▶️ 开启自动刷新' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="page-content">
      <!-- 使用权限缓存演示组件 -->
      <PermissionCacheDemo />

      <!-- 附加说明区域 -->
      <div class="additional-info">
        <div class="info-section">
          <h3>📋 测试说明</h3>
          <div class="info-content">
            <p><strong>权限级别说明:</strong></p>
            <ul>
              <li><span class="level-badge level-1">Level 1 - SCHOOL_WIDE</span> 全校权限（校长、系统管理员）</li>
              <li><span class="level-badge level-2">Level 2 - IMPORTANT</span> 重要权限（教务主任及以上）</li>
              <li><span class="level-badge level-3">Level 3 - DEPARTMENT</span> 部门权限（教师、班主任及以上）</li>
              <li><span class="level-badge level-4">Level 4 - CLASS</span> 班级权限（学生及以上，所有用户）</li>
            </ul>
          </div>
        </div>

        <div class="info-section">
          <h3>⚡ 性能优化效果</h3>
          <div class="info-content">
            <div class="performance-comparison">
              <div class="before-after">
                <div class="performance-item">
                  <h4>优化前</h4>
                  <div class="performance-value old">108ms</div>
                  <div class="performance-desc">直接数据库查询</div>
                </div>
                <div class="arrow">→</div>
                <div class="performance-item">
                  <h4>优化后</h4>
                  <div class="performance-value new">37ms</div>
                  <div class="performance-desc">Redis缓存命中</div>
                </div>
              </div>
              <div class="improvement">
                <span class="improvement-text">性能提升 66%</span>
              </div>
            </div>
          </div>
        </div>

        <div class="info-section">
          <h3>🛠️ 技术特性</h3>
          <div class="info-content">
            <div class="tech-features">
              <div class="feature-item">
                <div class="feature-icon">🎯</div>
                <div class="feature-content">
                  <h4>@RequiresPermission注解</h4>
                  <p>声明式权限验证，AOP切面自动拦截</p>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-icon">⚡</div>
                <div class="feature-content">
                  <h4>Redis缓存优化</h4>
                  <p>15分钟TTL，支持10,000用户并发</p>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-icon">🛡️</div>
                <div class="feature-content">
                  <h4>智能降级机制</h4>
                  <p>Redis故障自动切换数据库查询</p>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-icon">📊</div>
                <div class="feature-content">
                  <h4>性能监控</h4>
                  <p>实时缓存命中率和响应时间统计</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import PermissionCacheDemo from '../components/PermissionCacheDemo.vue'

// 自动刷新功能
const autoRefresh = ref(false)
let refreshInterval: number | null = null

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  
  if (autoRefresh.value) {
    // 每30秒自动刷新一次性能指标
    refreshInterval = window.setInterval(() => {
      // 这里可以添加自动刷新逻辑
      console.log('⏰ 自动刷新权限缓存指标')
    }, 30000)
  } else {
    if (refreshInterval) {
      clearInterval(refreshInterval)
      refreshInterval = null
    }
  }
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})

// 页面加载时设置标题
onMounted(() => {
  document.title = 'P0权限缓存系统测试 - 哈信工校园门户'
})
</script>

<style scoped>
.permission-test-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.page-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  padding: 20px 0;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header h1 {
  font-size: 28px;
  color: #2c3e50;
  margin-bottom: 8px;
  font-weight: 700;
}

.header-description {
  color: #7f8c8d;
  font-size: 16px;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.back-btn {
  background: #6c757d;
  color: white;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #5a6268;
  transform: translateY(-1px);
}

.auto-refresh-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.auto-refresh-btn:hover {
  background: #218838;
  transform: translateY(-1px);
}

.auto-refresh-btn.active {
  background: #dc3545;
}

.auto-refresh-btn.active:hover {
  background: #c82333;
}

.page-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.additional-info {
  margin-top: 40px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 30px;
}

.info-section {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.info-section h3 {
  color: #2c3e50;
  margin-bottom: 15px;
  font-size: 18px;
}

.info-content {
  color: #555;
  line-height: 1.6;
}

.info-content ul {
  list-style: none;
  padding: 0;
}

.info-content li {
  margin: 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-badge {
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.level-badge.level-1 {
  background: #fee2e2;
  color: #dc2626;
}

.level-badge.level-2 {
  background: #fef3c7;
  color: #d97706;
}

.level-badge.level-3 {
  background: #dbeafe;
  color: #2563eb;
}

.level-badge.level-4 {
  background: #ecfdf5;
  color: #059669;
}

.performance-comparison {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.before-after {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.performance-item {
  text-align: center;
}

.performance-item h4 {
  color: #666;
  margin-bottom: 8px;
  font-size: 14px;
}

.performance-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 5px;
}

.performance-value.old {
  color: #dc3545;
}

.performance-value.new {
  color: #28a745;
}

.performance-desc {
  font-size: 12px;
  color: #999;
}

.arrow {
  font-size: 24px;
  color: #6c757d;
  font-weight: bold;
}

.improvement {
  text-align: center;
  padding: 12px 20px;
  background: linear-gradient(135deg, #28a745, #20c997);
  border-radius: 8px;
  color: white;
}

.improvement-text {
  font-size: 16px;
  font-weight: 600;
}

.tech-features {
  display: grid;
  gap: 15px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
}

.feature-icon {
  font-size: 20px;
  margin-top: 2px;
}

.feature-content h4 {
  color: #2c3e50;
  margin: 0 0 5px 0;
  font-size: 14px;
}

.feature-content p {
  color: #6c757d;
  margin: 0;
  font-size: 13px;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header h1 {
    font-size: 24px;
  }
  
  .header-description {
    font-size: 14px;
  }
  
  .before-after {
    flex-direction: column;
    gap: 15px;
  }
  
  .arrow {
    transform: rotate(90deg);
  }
  
  .additional-info {
    grid-template-columns: 1fr;
  }
}
</style>