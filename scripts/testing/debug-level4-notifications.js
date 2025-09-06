/**
 * Level 4 通知调试脚本
 * 在浏览器控制台中运行，调试Vue前端的数据状态
 */

console.log('🔍 [调试脚本] 开始分析 Level 4 通知不显示问题...')

// 1. 检查Vue应用实例和Store
try {
  const app = window.__VUE_APP__
  if (!app) {
    console.error('❌ Vue应用实例未找到')
    return
  }
  
  // 2. 获取NotificationStore
  const notificationStore = app.config.globalProperties.$pinia.state.value.notification
  if (!notificationStore) {
    console.error('❌ NotificationStore未找到')
    return
  }
  
  console.log('✅ Store实例获取成功')
  
  // 3. 分析原始通知数据
  const allNotifications = notificationStore.notifications
  console.log('📊 [原始数据] 通知总数:', allNotifications.length)
  
  const level4Raw = allNotifications.filter(n => n.level === 4)
  console.log('📊 [原始数据] Level 4 通知数量:', level4Raw.length)
  console.table(level4Raw.map(n => ({
    id: n.id,
    title: n.title.substring(0, 30) + '...',
    level: n.level,
    createTime: n.createTime,
    publisherRole: n.publisherRole
  })))
  
  // 4. 分析已读状态管理器
  const readStatusManager = notificationStore.readStatusManager
  if (readStatusManager) {
    const readIds = readStatusManager.readNotificationIds?.value || new Set()
    console.log('📊 [已读状态] 已读通知ID数量:', readIds.size)
    console.log('📊 [已读状态] 已读ID列表:', Array.from(readIds))
    
    // 检查Level 4通知的已读状态
    level4Raw.forEach(notification => {
      const isRead = readIds.has(notification.id)
      console.log(`📋 [Level4已读检查] ID=${notification.id}, 标题="${notification.title.substring(0, 20)}...", 已读=${isRead}`)
    })
  } else {
    console.warn('⚠️ [已读状态] readStatusManager未初始化')
  }
  
  // 5. 分析3天过滤逻辑
  const threeDaysAgo = Date.now() - (3 * 24 * 60 * 60 * 1000)
  console.log('📊 [时间过滤] 3天前时间戳:', new Date(threeDaysAgo).toLocaleString())
  
  level4Raw.forEach(notification => {
    const createTime = new Date(notification.createTime).getTime()
    const isRecent = createTime > threeDaysAgo
    const isRead = readStatusManager ? readStatusManager.readNotificationIds?.value?.has(notification.id) : false
    
    console.log(`📋 [时间过滤检查] ID=${notification.id}:`)
    console.log(`  - 创建时间: ${new Date(createTime).toLocaleString()}`)
    console.log(`  - 是否最近3天: ${isRecent}`)
    console.log(`  - 是否已读: ${isRead}`)
    console.log(`  - 应该显示: ${!isRead || isRecent}`)
  })
  
  // 6. 分析计算属性结果
  const level4Messages = notificationStore.level4Messages
  console.log('📊 [计算属性] level4Messages数量:', level4Messages.length)
  console.table(level4Messages.map(n => ({
    id: n.id,
    title: n.title.substring(0, 30) + '...',
    level: n.level,
    createTime: new Date(n.createTime).toLocaleString()
  })))
  
  // 7. 检查其他级别的通知数量对比
  console.log('📊 [级别对比] 各级别通知数量:')
  console.log('  - Level 1:', allNotifications.filter(n => n.level === 1).length)
  console.log('  - Level 2:', allNotifications.filter(n => n.level === 2).length)  
  console.log('  - Level 3:', allNotifications.filter(n => n.level === 3).length)
  console.log('  - Level 4:', level4Raw.length)
  
} catch (error) {
  console.error('❌ [调试脚本] 执行失败:', error)
  console.log('请在Vue应用页面中运行此脚本')
}