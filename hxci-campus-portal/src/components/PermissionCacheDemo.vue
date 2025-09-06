<!--
  🔐 权限缓存系统测试组件
  
  用途: 演示P0权限缓存API的使用方法和效果
  特点: 实时权限测试 + 性能监控 + 用户权限矩阵显示
-->
<template>
  <div class="permission-cache-demo">
    <div class="demo-header">
      <h2>🔐 P0权限缓存系统演示</h2>
      <p class="demo-description">
        实时测试权限验证性能，查看66%性能提升效果 (108ms → 37ms)
      </p>
    </div>

    <!-- 系统状态显示 -->
    <div class="status-section">
      <h3>🏓 系统状态</h3>
      <div class="status-card" :class="systemStatus.ok ? 'status-ok' : 'status-error'">
        <span class="status-indicator"></span>
        <span>{{ systemStatus.message }}</span>
        <button @click="checkSystemStatus" :disabled="loading.system">
          {{ loading.system ? '检查中...' : '重新检查' }}
        </button>
      </div>
    </div>

    <!-- 权限测试区域 -->
    <div class="permission-tests">
      <h3>🧪 权限级别测试</h3>
      <div class="test-grid">
        
        <!-- CLASS级别权限测试 -->
        <div class="test-card">
          <div class="test-header">
            <h4>📚 CLASS权限 (Level 4)</h4>
            <span class="permission-badge level-4">学生可访问</span>
          </div>
          <div class="test-result" v-if="testResults.class">
            <div :class="['result-status', testResults.class.success ? 'success' : 'failed']">
              {{ testResults.class.success ? '✅ 通过' : '❌ 失败' }}
            </div>
            <div class="result-details">
              <div>响应时间: {{ testResults.class.responseTime }}ms</div>
              <div>缓存命中: {{ testResults.class.fromCache ? '是' : '否' }}</div>
            </div>
          </div>
          <button @click="testClassPermission" :disabled="loading.class">
            {{ loading.class ? '测试中...' : '测试权限' }}
          </button>
        </div>

        <!-- DEPARTMENT级别权限测试 -->
        <div class="test-card">
          <div class="test-header">
            <h4>🏫 DEPARTMENT权限 (Level 3)</h4>
            <span class="permission-badge level-3">教师可访问</span>
          </div>
          <div class="test-result" v-if="testResults.department">
            <div :class="['result-status', testResults.department.success ? 'success' : 'failed']">
              {{ testResults.department.success ? '✅ 通过' : '❌ 失败' }}
            </div>
            <div class="result-details">
              <div>响应时间: {{ testResults.department.responseTime }}ms</div>
              <div>缓存命中: {{ testResults.department.fromCache ? '是' : '否' }}</div>
            </div>
          </div>
          <button @click="testDepartmentPermission" :disabled="loading.department">
            {{ loading.department ? '测试中...' : '测试权限' }}
          </button>
        </div>

        <!-- SCHOOL级别权限测试 -->
        <div class="test-card">
          <div class="test-header">
            <h4>🏛️ SCHOOL权限 (Level 1)</h4>
            <span class="permission-badge level-1">校长可访问</span>
          </div>
          <div class="test-result" v-if="testResults.school">
            <div :class="['result-status', testResults.school.success ? 'success' : 'failed']">
              {{ testResults.school.success ? '✅ 通过' : '❌ 失败' }}
            </div>
            <div class="result-details">
              <div>响应时间: {{ testResults.school.responseTime }}ms</div>
              <div>缓存命中: {{ testResults.school.fromCache ? '是' : '否' }}</div>
            </div>
          </div>
          <button @click="testSchoolPermission" :disabled="loading.school">
            {{ loading.school ? '测试中...' : '测试权限' }}
          </button>
        </div>

      </div>
    </div>

    <!-- 批量测试和用户权限矩阵 -->
    <div class="batch-test-section">
      <h3>⚡ 批量权限测试</h3>
      <div class="batch-controls">
        <button @click="runBatchTest" :disabled="loading.batch" class="batch-test-btn">
          {{ loading.batch ? '测试中...' : '🚀 一键测试所有权限' }}
        </button>
        <button @click="clearCache" :disabled="loading.clear" class="clear-cache-btn">
          {{ loading.clear ? '清理中...' : '🗑️ 清空缓存 (管理员)' }}
        </button>
      </div>

      <!-- 用户权限矩阵显示 -->
      <div v-if="permissionMatrix" class="permission-matrix">
        <h4>📊 用户权限矩阵</h4>
        <div class="matrix-summary">
          <div class="summary-item">
            <label>用户级别:</label>
            <span class="user-level">{{ permissionMatrix.summary.userLevelName }}</span>
          </div>
          <div class="summary-item">
            <label>平均响应:</label>
            <span :class="['response-time', permissionMatrix.summary.cacheOptimized ? 'fast' : 'slow']">
              {{ permissionMatrix.summary.avgResponseTime.toFixed(1) }}ms
            </span>
          </div>
          <div class="summary-item">
            <label>缓存优化:</label>
            <span :class="['cache-status', permissionMatrix.summary.cacheOptimized ? 'enabled' : 'disabled']">
              {{ permissionMatrix.summary.cacheOptimized ? '✅ 有效' : '⚠️ 需要优化' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 性能监控区域 -->
    <div class="metrics-section">
      <h3>📊 性能监控</h3>
      <div v-if="cacheMetrics" class="metrics-grid">
        <div class="metric-card">
          <div class="metric-value">{{ cacheMetrics.hitRate }}</div>
          <div class="metric-label">缓存命中率</div>
        </div>
        <div class="metric-card">
          <div class="metric-value">{{ cacheMetrics.cacheHits }}</div>
          <div class="metric-label">缓存命中次数</div>
        </div>
        <div class="metric-card">
          <div class="metric-value">{{ cacheMetrics.dbFallbacks }}</div>
          <div class="metric-label">数据库降级次数</div>
        </div>
        <div class="metric-card">
          <div class="metric-value">{{ cacheMetrics.estimatedMemoryMB }}MB</div>
          <div class="metric-label">预估内存使用</div>
        </div>
      </div>
      <button @click="loadCacheMetrics" :disabled="loading.metrics" class="metrics-refresh-btn">
        {{ loading.metrics ? '加载中...' : '🔄 刷新指标' }}
      </button>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  pingPermissionCache,
  testClassPermission as apiTestClass,
  testDepartmentPermission as apiTestDepartment,
  testSchoolPermission as apiTestSchool,
  testUserPermissionMatrix,
  getPermissionCacheMetrics,
  clearPermissionCache,
  type PermissionTestResult,
  type PermissionCacheMetrics
} from '../api/permission-cache'

// 响应式数据
const systemStatus = ref({ ok: false, message: '未检查' })
const testResults = ref<{
  class?: PermissionTestResult
  department?: PermissionTestResult
  school?: PermissionTestResult
}>({})
const permissionMatrix = ref<any>(null)
const cacheMetrics = ref<PermissionCacheMetrics | null>(null)

// 加载状态
const loading = ref({
  system: false,
  class: false,
  department: false,
  school: false,
  batch: false,
  clear: false,
  metrics: false
})

// 检查系统状态
const checkSystemStatus = async () => {
  loading.value.system = true
  try {
    const message = await pingPermissionCache()
    systemStatus.value = { ok: true, message }
  } catch (error: any) {
    systemStatus.value = { ok: false, message: error.message || '系统异常' }
  } finally {
    loading.value.system = false
  }
}

// 单独权限测试方法
const testClassPermission = async () => {
  loading.value.class = true
  try {
    testResults.value.class = await apiTestClass()
  } catch (error) {
    console.error('CLASS权限测试失败:', error)
  } finally {
    loading.value.class = false
  }
}

const testDepartmentPermission = async () => {
  loading.value.department = true
  try {
    testResults.value.department = await apiTestDepartment()
  } catch (error) {
    console.error('DEPARTMENT权限测试失败:', error)
  } finally {
    loading.value.department = false
  }
}

const testSchoolPermission = async () => {
  loading.value.school = true
  try {
    testResults.value.school = await apiTestSchool()
  } catch (error) {
    console.error('SCHOOL权限测试失败:', error)
  } finally {
    loading.value.school = false
  }
}

// 批量权限测试
const runBatchTest = async () => {
  loading.value.batch = true
  try {
    const matrix = await testUserPermissionMatrix()
    permissionMatrix.value = matrix
    
    // 更新单独测试结果
    testResults.value = {
      class: matrix.classLevel,
      department: matrix.departmentLevel,
      school: matrix.schoolLevel
    }
  } catch (error) {
    console.error('批量权限测试失败:', error)
  } finally {
    loading.value.batch = false
  }
}

// 加载缓存性能指标
const loadCacheMetrics = async () => {
  loading.value.metrics = true
  try {
    cacheMetrics.value = await getPermissionCacheMetrics()
  } catch (error) {
    console.error('缓存指标加载失败:', error)
  } finally {
    loading.value.metrics = false
  }
}

// 清空缓存
const clearCache = async () => {
  if (!confirm('确定要清空权限缓存吗？这将暂时影响系统性能。')) {
    return
  }
  
  loading.value.clear = true
  try {
    const message = await clearPermissionCache()
    alert('缓存清空成功: ' + message)
    // 清空缓存后重新加载指标
    await loadCacheMetrics()
  } catch (error: any) {
    alert('清空缓存失败: ' + error.message)
  } finally {
    loading.value.clear = false
  }
}

// 组件挂载时自动检查系统状态和加载指标
onMounted(async () => {
  await checkSystemStatus()
  await loadCacheMetrics()
})
</script>

<style scoped>
.permission-cache-demo {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.demo-header {
  text-align: center;
  margin-bottom: 30px;
}

.demo-header h2 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.demo-description {
  color: #7f8c8d;
  font-size: 14px;
}

.status-section {
  margin-bottom: 30px;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.status-card.status-ok {
  background: #f0f9ff;
  border-color: #22c55e;
}

.status-card.status-error {
  background: #fef2f2;
  border-color: #ef4444;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-ok .status-indicator {
  background: #22c55e;
}

.status-error .status-indicator {
  background: #ef4444;
}

.test-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.test-card {
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  padding: 20px;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.permission-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.permission-badge.level-1 {
  background: #fee2e2;
  color: #dc2626;
}

.permission-badge.level-3 {
  background: #fef3c7;
  color: #d97706;
}

.permission-badge.level-4 {
  background: #ecfdf5;
  color: #059669;
}

.test-result {
  margin: 15px 0;
  padding: 12px;
  border-radius: 6px;
  background: #f8f9fa;
}

.result-status.success {
  color: #22c55e;
  font-weight: 600;
}

.result-status.failed {
  color: #ef4444;
  font-weight: 600;
}

.result-details {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}

.batch-test-section {
  margin: 30px 0;
}

.batch-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.batch-test-btn {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
}

.clear-cache-btn {
  background: #ef4444;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
}

.permission-matrix {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

.matrix-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-level {
  font-weight: 600;
  color: #2563eb;
}

.response-time.fast {
  color: #22c55e;
  font-weight: 600;
}

.response-time.slow {
  color: #f59e0b;
}

.cache-status.enabled {
  color: #22c55e;
}

.cache-status.disabled {
  color: #ef4444;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.metric-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  border: 1px solid #e5e5e5;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 5px;
}

.metric-label {
  font-size: 12px;
  color: #7f8c8d;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.metrics-refresh-btn {
  background: #10b981;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

button:not(:disabled):hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
</style>