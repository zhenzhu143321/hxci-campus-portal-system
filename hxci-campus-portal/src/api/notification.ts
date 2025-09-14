import api from '@/utils/request'
import { timeAgo } from '@/utils'

// 通知数据接口类型定义
export interface NotificationItem {
  id: number
  title: string
  content: string
  level: number
  levelColor: string
  publisherName: string
  publisherRole: string  // 🔧 新增：发布者角色，用于系统公告识别
  createTime: string
  scope: string
  status: string
  summary?: string       // 🔧 新增：通知摘要
  isRead?: boolean
}

export interface NotificationListResponse {
  success: boolean
  data: {
    list: NotificationItem[]
    total: number
  }
  message?: string
}

export interface NotificationDetailResponse {
  success: boolean
  data: NotificationItem
  message?: string
}

// 通知API调用服务
export const notificationAPI = {
  /**
   * 获取通知列表
   * 基于当前用户角色和权限过滤通知
   */
  async getNotificationList(params?: {
    pageNo?: number
    pageSize?: number
    level?: number
    scope?: string
  }): Promise<NotificationListResponse> {
    try {
      console.log('📢 [通知API] 开始获取通知列表...')
      console.log('📤 [通知API] 请求参数:', params)
      
      // 调用后端通知列表API
      const response = await api.get('/admin-api/test/notification/api/list', {
        params: {
          pageNo: 1,
          pageSize: 20,
          ...params
        }
      })
      
      console.log('📥 [通知API] 响应数据:', response.data)
      
      // 适配后端返回格式: {code: 0, data: {...}, msg: ''}
      // code: 0 表示成功，其他值表示失败
      if (response.data && (response.data.code === 0 || response.data.success)) {
        console.log('✅ [通知API] 通知列表获取成功')
        
        // 处理不同的数据结构
        let notificationData = response.data.data
        let notificationList = []
        
        if (Array.isArray(notificationData)) {
          // 如果data直接是数组
          notificationList = notificationData
        } else if (notificationData && Array.isArray(notificationData.notifications)) {
          // 真实API返回格式：data.notifications是数组
          notificationList = notificationData.notifications
        } else if (notificationData && Array.isArray(notificationData.list)) {
          // 如果data是对象且有list属性
          notificationList = notificationData.list
        } else if (notificationData && Array.isArray(notificationData.records)) {
          // 如果data是对象且有records属性（分页数据）
          notificationList = notificationData.records
        } else {
          console.log('⚠️ [通知API] 未识别的数据结构，使用默认数据')
          console.log('🔍 [通知API] 实际数据结构:', notificationData)
          notificationList = []
        }
        
        console.log('📋 [通知API] 解析到的通知列表:', notificationList)
        
        // 转换数据格式，添加显示需要的字段
        const notifications = notificationList.map((item: any) => ({
          id: item.id,
          title: item.title,
          content: item.content,
          level: item.level,
          levelColor: this.getLevelColor(item.level),
          publisherName: item.publisherName,
          publisherRole: item.publisherRole, // 🔧 新增：映射发布者角色
          createTime: item.createTime, // 🔧 核心修复：保持原始日期格式，让前端组件决定显示方式
          scope: item.targetScope || item.scope, // 适配真实API字段名
          status: item.status,
          summary: item.summary,              // 🔧 新增：映射摘要字段
          isRead: false // 暂时设为未读，后续可扩展
        }))
        
        return {
          success: true,
          data: {
            list: notifications,
            total: notificationData?.total || notificationData?.totalCount || notifications.length
          }
        }
      } else {
        console.log('⚠️ [通知API] 后端返回失败状态')
        return {
          success: false,
          data: {
            list: this.getDefaultNotifications(),
            total: 0
          },
          message: '通知数据获取失败'
        }
      }
    } catch (error: any) {
      console.error('❌ [通知API] 请求异常:', error)
      
      // 降级方案：返回默认通知数据
      return {
        success: false,
        data: {
          list: this.getDefaultNotifications(),
          total: 0
        },
        message: error.message || '网络连接失败'
      }
    }
  },

  /**
   * 获取未读通知数量
   */
  async getUnreadCount(): Promise<number> {
    try {
      const result = await this.getNotificationList({ pageSize: 100 })
      if (result.success) {
        // 这里简化处理，实际可以根据isRead字段统计
        return result.data.list.filter(item => !item.isRead).length
      }
      return 0
    } catch (error) {
      console.error('❌ [通知API] 获取未读数量失败:', error)
      return 0
    }
  },

  /**
   * 获取通知详情
   */
  async getNotificationDetail(id: number): Promise<NotificationDetailResponse> {
    try {
      console.log('📖 [通知API] 获取通知详情, ID:', id)
      
      // 如果后端有单独的详情接口，使用该接口
      // 这里先从列表中查找
      const listResult = await this.getNotificationList()
      
      if (listResult.success) {
        const notification = listResult.data.list.find(item => item.id === id)
        if (notification) {
          return {
            success: true,
            data: notification
          }
        }
      }
      
      return {
        success: false,
        data: this.getDefaultNotifications()[0],
        message: '通知不存在'
      }
    } catch (error: any) {
      console.error('❌ [通知API] 获取通知详情失败:', error)
      return {
        success: false,
        data: this.getDefaultNotifications()[0],
        message: error.message || '获取详情失败'
      }
    }
  },

  /**
   * 获取级别对应的颜色
   */
  getLevelColor(level: number): string {
    switch (level) {
      case 1: return '#F56C6C' // 红色 - 紧急
      case 2: return '#E6A23C' // 橙色 - 重要
      case 3: return '#409EFF' // 蓝色 - 常规
      case 4: return '#67C23A' // 绿色 - 提醒
      default: return '#909399' // 灰色 - 默认
    }
  },

  // formatTime函数已迁移到 @/utils，使用timeAgo替代

  /**
   * 获取默认通知数据 (降级方案)
   */
  getDefaultNotifications(): NotificationItem[] {
    return [
      {
        id: 1,
        title: '期末考试时间安排通知',
        content: '2025年春季学期期末考试将于1月15日开始，请各位同学做好准备...',
        level: 2,
        levelColor: '#E6A23C',
        publisherName: '教务处',
        publisherRole: 'ACADEMIC_ADMIN', // 🔧 新增默认角色
        createTime: '2小时前',
        scope: 'SCHOOL_WIDE',
        status: 'PUBLISHED',
        isRead: false
      },
      {
        id: 2,
        title: '校园安全提醒',
        content: '近期天气寒冷，路面结冰，请同学们注意出行安全...',
        level: 1,
        levelColor: '#F56C6C',
        publisherName: '保卫处',
        publisherRole: 'PRINCIPAL',       // 🔧 新增默认角色
        createTime: '4小时前',
        scope: 'SCHOOL_WIDE',
        status: 'PUBLISHED',
        isRead: false
      },
      {
        id: 3,
        title: '图书馆开放时间调整',
        content: '因系统维护，图书馆开放时间临时调整为9:00-21:00...',
        level: 3,
        levelColor: '#409EFF',
        publisherName: '图书馆',
        publisherRole: 'SYSTEM_ADMIN',    // 🔧 新增系统角色，便于测试
        createTime: '1天前',
        scope: 'SCHOOL_WIDE',
        status: 'PUBLISHED',
        isRead: true
      }
    ]
  }
}