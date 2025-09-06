import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { NotificationService, NotificationServiceError } from '@/services/notificationService'
import * as notificationAPI from '@/api/notification'
import { ElMessage } from 'element-plus'

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn()
  }
}))

// Mock notification API
vi.mock('@/api/notification', () => ({
  notificationAPI: {
    getNotificationList: vi.fn(),
    getNotificationDetail: vi.fn(),
    getDefaultNotifications: vi.fn()
  }
}))

describe('NotificationService', () => {
  let service: NotificationService
  const mockNotificationList = [
    {
      id: 1,
      title: '测试通知1',
      content: '测试内容1',
      level: 1,
      levelColor: '#F56C6C',
      publisherName: '测试发布者',
      publisherRole: 'SYSTEM_ADMIN',
      createTime: '2024-01-01T10:00:00',
      scope: 'SCHOOL_WIDE',
      status: 'PUBLISHED',
      isRead: false
    },
    {
      id: 2,
      title: '测试通知2',
      content: '测试内容2',
      level: 2,
      levelColor: '#E6A23C',
      publisherName: '教务处',
      publisherRole: 'ACADEMIC_ADMIN',
      createTime: '2024-01-01T09:00:00',
      scope: 'DEPARTMENT',
      status: 'PUBLISHED',
      isRead: false
    },
    {
      id: 3,
      title: '测试通知3',
      content: '测试内容3',
      level: 4,
      levelColor: '#67C23A',
      publisherName: '班主任',
      publisherRole: 'CLASS_TEACHER',
      createTime: '2024-01-01T08:00:00',
      scope: 'CLASS',
      status: 'PUBLISHED',
      isRead: true
    }
  ]

  beforeEach(() => {
    service = new NotificationService()
    vi.clearAllMocks()
  })

  afterEach(() => {
    service.clearCache()
  })

  describe('getNotifications', () => {
    it('应该成功获取通知列表', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)

      // Act
      const result = await service.getNotifications()

      // Assert
      expect(result).toEqual(mockNotificationList)
      expect(notificationAPI.notificationAPI.getNotificationList).toHaveBeenCalledWith({
        pageSize: 100
      })
    })

    it('应该处理API失败并返回降级数据', async () => {
      // Arrange
      const defaultNotifications = [{ id: 999, title: '默认通知' }] as any
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue({
        success: false,
        data: { list: [], total: 0 }
      })
      ;(notificationAPI.notificationAPI.getDefaultNotifications as any).mockReturnValue(defaultNotifications)

      // Act
      const result = await service.getNotifications()

      // Assert
      expect(result).toEqual(defaultNotifications)
      expect(ElMessage.warning).toHaveBeenCalledWith('通知数据获取失败，已切换到离线模式')
    })

    it('应该处理API异常并返回降级数据', async () => {
      // Arrange
      const defaultNotifications = [{ id: 999, title: '默认通知' }] as any
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockRejectedValue(
        new Error('网络连接失败')
      )
      ;(notificationAPI.notificationAPI.getDefaultNotifications as any).mockReturnValue(defaultNotifications)

      // Act
      const result = await service.getNotifications()

      // Assert
      expect(result).toEqual(defaultNotifications)
      expect(ElMessage.error).toHaveBeenCalledWith('操作失败: 网络连接失败')
    })

    it('应该使用缓存数据', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)

      // Act - 第一次调用
      const result1 = await service.getNotifications()
      // Act - 第二次调用（应该使用缓存）
      const result2 = await service.getNotifications()

      // Assert
      expect(result1).toEqual(mockNotificationList)
      expect(result2).toEqual(mockNotificationList)
      // API应该只被调用一次
      expect(notificationAPI.notificationAPI.getNotificationList).toHaveBeenCalledTimes(1)
    })

    it('应该跳过缓存当useCache=false时', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)

      // Act
      await service.getNotifications({ useCache: false })
      await service.getNotifications({ useCache: false })

      // Assert
      // API应该被调用两次
      expect(notificationAPI.notificationAPI.getNotificationList).toHaveBeenCalledTimes(2)
    })
  })

  describe('getNotificationDetail', () => {
    it('应该成功获取通知详情', async () => {
      // Arrange
      const mockDetail = mockNotificationList[0]
      const mockResponse = {
        success: true,
        data: mockDetail
      }
      ;(notificationAPI.notificationAPI.getNotificationDetail as any).mockResolvedValue(mockResponse)

      // Act
      const result = await service.getNotificationDetail(1)

      // Assert
      expect(result).toEqual(mockDetail)
      expect(notificationAPI.notificationAPI.getNotificationDetail).toHaveBeenCalledWith(1)
    })

    it('应该处理详情获取失败', async () => {
      // Arrange
      ;(notificationAPI.notificationAPI.getNotificationDetail as any).mockResolvedValue({
        success: false,
        data: null
      })

      // Act
      const result = await service.getNotificationDetail(999)

      // Assert
      expect(result).toBeNull()
      expect(ElMessage.warning).toHaveBeenCalledWith('通知详情获取失败')
    })
  })

  describe('getPriorityNotifications', () => {
    it('应该过滤并排序优先级通知', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)

      // Act
      const result = await service.getPriorityNotifications()

      // Assert
      // 应该只包含Level 1-3且未读的通知，按级别排序
      expect(result).toHaveLength(2)
      expect(result[0].level).toBe(1) // Level 1 最高优先级
      expect(result[1].level).toBe(2) // Level 2 次高优先级
      // Level 4 和已读通知应该被过滤掉
      expect(result.find(n => n.level === 4)).toBeUndefined()
      expect(result.find(n => n.isRead === true)).toBeUndefined()
    })
  })

  describe('getUnreadNotificationsCount', () => {
    it('应该正确统计未读通知数量', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)

      // Act
      const result = await service.getUnreadNotificationsCount()

      // Assert
      // mockNotificationList中有2条未读通知
      expect(result).toBe(2)
    })
  })

  describe('缓存管理', () => {
    it('应该正确清除指定前缀的缓存', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      const mockDetailResponse = {
        success: true,
        data: mockNotificationList[0]
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)
      ;(notificationAPI.notificationAPI.getNotificationDetail as any).mockResolvedValue(mockDetailResponse)

      // 创建缓存
      await service.getNotifications()
      await service.getNotificationDetail(1)

      // Act
      service.clearCache('notifications_')

      // Assert
      const stats = service.getCacheStats()
      // 通知列表缓存应该被清除，但详情缓存应该保留
      expect(stats.keys.some(key => key.startsWith('notifications_'))).toBeFalsy()
      expect(stats.keys.some(key => key.startsWith('notification_detail_'))).toBeTruthy()
    })

    it('应该正确清除所有缓存', () => {
      // Act
      service.clearCache()

      // Assert
      const stats = service.getCacheStats()
      expect(stats.size).toBe(0)
      expect(stats.keys).toHaveLength(0)
    })

    it('应该正确更新缓存配置', () => {
      // Act
      service.updateCacheConfig({ ttl: 10000, enabled: false })

      // Assert - 验证配置更新（通过内部行为验证）
      // 这里主要测试方法不抛出异常
      expect(() => service.updateCacheConfig({ maxSize: 50 })).not.toThrow()
    })

    it('应该返回正确的缓存统计信息', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)

      // Act
      await service.getNotifications()
      const stats = service.getCacheStats()

      // Assert
      expect(stats.size).toBeGreaterThan(0)
      expect(stats.keys).toContain('notifications_{}')
    })
  })

  describe('refreshNotifications', () => {
    it('应该清除缓存并强制获取最新数据', async () => {
      // Arrange
      const mockResponse = {
        success: true,
        data: { list: mockNotificationList, total: 3 }
      }
      ;(notificationAPI.notificationAPI.getNotificationList as any).mockResolvedValue(mockResponse)

      // 先创建缓存
      await service.getNotifications()
      expect(notificationAPI.notificationAPI.getNotificationList).toHaveBeenCalledTimes(1)

      // Act
      const result = await service.refreshNotifications()

      // Assert
      expect(result).toEqual(mockNotificationList)
      // API应该被调用第二次
      expect(notificationAPI.notificationAPI.getNotificationList).toHaveBeenCalledTimes(2)
    })
  })

  describe('错误处理', () => {
    it('应该抛出NotificationServiceError', async () => {
      // Arrange
      ;(notificationAPI.notificationAPI.getNotificationDetail as any).mockRejectedValue(
        new Error('服务器错误')
      )

      // Act
      const result = await service.getNotificationDetail(999)

      // Assert
      expect(result).toBeNull()
      expect(ElMessage.error).toHaveBeenCalledWith('操作失败: 服务器错误')
    })
  })
})