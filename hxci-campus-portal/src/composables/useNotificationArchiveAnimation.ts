import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 通知归档动画管理 Composable
 * 解耦设计：将归档动画逻辑从Home.vue中独立出来
 * 
 * 功能特性：
 * - 归档动画状态管理
 * - 成功提示信息控制
 * - 动画时序控制
 * - 批量归档动画支持
 */

export interface ArchiveAnimationState {
  /** 当前正在执行动画的通知ID列表 */
  animatingIds: Set<number>
  /** 动画完成的通知ID列表 */
  completedIds: Set<number>
  /** 是否正在执行批量动画 */
  isBatchAnimating: boolean
}

export interface ArchiveAnimationOptions {
  /** 动画持续时间 (ms) */
  duration?: number
  /** 动画延迟时间 (ms) */
  delay?: number
  /** 是否显示成功提示 */
  showSuccessMessage?: boolean
  /** 自定义成功提示文本 */
  successMessage?: string
  /** 是否启用声音效果 */
  enableSound?: boolean
}

/**
 * 归档动画管理钩子
 */
export function useNotificationArchiveAnimation() {
  // 动画状态
  const animationState = ref<ArchiveAnimationState>({
    animatingIds: new Set(),
    completedIds: new Set(),
    isBatchAnimating: false
  })
  
  // 默认配置
  const defaultOptions: Required<ArchiveAnimationOptions> = {
    duration: 1500,
    delay: 0,
    showSuccessMessage: true,
    successMessage: '已标记为已读并归档',
    enableSound: false
  }
  
  /**
   * 触发单个通知的归档动画
   * @param notificationId 通知ID
   * @param options 动画配置选项
   */
  const triggerArchiveAnimation = async (
    notificationId: number, 
    options: ArchiveAnimationOptions = {}
  ): Promise<void> => {
    const config = { ...defaultOptions, ...options }
    
    console.log('🎬 [归档动画] 开始执行，通知ID:', notificationId)
    
    // 检查是否已在动画中
    if (animationState.value.animatingIds.has(notificationId)) {
      console.log('⚠️ [归档动画] 通知已在动画中，跳过:', notificationId)
      return
    }
    
    try {
      // 添加到动画列表
      animationState.value.animatingIds.add(notificationId)
      
      // 延迟执行动画
      if (config.delay > 0) {
        await new Promise(resolve => setTimeout(resolve, config.delay))
      }
      
      // 等待下一个tick确保DOM更新
      await nextTick()
      
      // 播放音效（如果启用）
      if (config.enableSound) {
        playArchiveSound()
      }
      
      // 等待动画完成
      await new Promise(resolve => setTimeout(resolve, config.duration))
      
      // 从动画列表移除
      animationState.value.animatingIds.delete(notificationId)
      
      // 添加到完成列表
      animationState.value.completedIds.add(notificationId)
      
      // 显示成功提示
      if (config.showSuccessMessage) {
        ElMessage.success({
          message: config.successMessage,
          duration: 2000,
          showClose: false
        })
      }
      
      console.log('✅ [归档动画] 执行完成，通知ID:', notificationId)
      
    } catch (error) {
      console.error('❌ [归档动画] 执行失败:', error)
      
      // 清理失败的动画状态
      animationState.value.animatingIds.delete(notificationId)
      
      ElMessage.error('归档动画执行失败')
    }
  }
  
  /**
   * 批量触发归档动画
   * @param notificationIds 通知ID列表
   * @param options 动画配置选项
   */
  const triggerBatchArchiveAnimation = async (
    notificationIds: number[],
    options: ArchiveAnimationOptions = {}
  ): Promise<void> => {
    const config = { 
      ...defaultOptions, 
      ...options,
      successMessage: options.successMessage || `已批量归档 ${notificationIds.length} 条通知`
    }
    
    console.log('🎬 [批量归档动画] 开始执行，数量:', notificationIds.length)
    
    if (animationState.value.isBatchAnimating) {
      console.log('⚠️ [批量归档动画] 正在执行中，跳过')
      return
    }
    
    try {
      animationState.value.isBatchAnimating = true
      
      // 串行执行动画，避免UI卡顿
      for (let i = 0; i < notificationIds.length; i++) {
        const notificationId = notificationIds[i]
        const animationDelay = i * 200 // 每个动画间隔200ms
        
        await triggerArchiveAnimation(notificationId, {
          ...config,
          delay: animationDelay,
          showSuccessMessage: false // 批量模式下单独控制提示
        })
      }
      
      // 批量完成提示
      if (config.showSuccessMessage) {
        ElMessage.success({
          message: config.successMessage,
          duration: 3000,
          showClose: true
        })
      }
      
      console.log('✅ [批量归档动画] 全部完成')
      
    } catch (error) {
      console.error('❌ [批量归档动画] 执行失败:', error)
      ElMessage.error('批量归档动画执行失败')
    } finally {
      animationState.value.isBatchAnimating = false
    }
  }
  
  /**
   * 检查通知是否正在执行动画
   * @param notificationId 通知ID
   */
  const isAnimating = (notificationId: number): boolean => {
    return animationState.value.animatingIds.has(notificationId)
  }
  
  /**
   * 检查通知动画是否已完成
   * @param notificationId 通知ID
   */
  const isAnimationCompleted = (notificationId: number): boolean => {
    return animationState.value.completedIds.has(notificationId)
  }
  
  /**
   * 清理动画状态
   * @param notificationId 可选的特定通知ID，不传则清理所有
   */
  const clearAnimationState = (notificationId?: number): void => {
    if (notificationId !== undefined) {
      animationState.value.animatingIds.delete(notificationId)
      animationState.value.completedIds.delete(notificationId)
      console.log('🧹 [归档动画] 清理单个通知状态:', notificationId)
    } else {
      animationState.value.animatingIds.clear()
      animationState.value.completedIds.clear()
      animationState.value.isBatchAnimating = false
      console.log('🧹 [归档动画] 清理所有动画状态')
    }
  }
  
  /**
   * 播放归档音效
   */
  const playArchiveSound = (): void => {
    try {
      // 创建音频上下文和音频节点
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()
      
      // 配置音效参数
      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      
      // 成功归档音效：短促的上升音调
      oscillator.frequency.setValueAtTime(800, audioContext.currentTime)
      oscillator.frequency.exponentialRampToValueAtTime(1200, audioContext.currentTime + 0.1)
      
      gainNode.gain.setValueAtTime(0.1, audioContext.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.2)
      
      oscillator.start(audioContext.currentTime)
      oscillator.stop(audioContext.currentTime + 0.2)
      
      console.log('🔊 [归档动画] 播放音效')
    } catch (error) {
      console.warn('⚠️ [归档动画] 音效播放失败:', error)
    }
  }
  
  /**
   * 获取当前动画统计信息
   */
  const getAnimationStats = () => {
    return {
      animatingCount: animationState.value.animatingIds.size,
      completedCount: animationState.value.completedIds.size,
      isBatchAnimating: animationState.value.isBatchAnimating,
      animatingIds: Array.from(animationState.value.animatingIds),
      completedIds: Array.from(animationState.value.completedIds)
    }
  }
  
  return {
    // 状态
    animationState: animationState.value,
    
    // 核心方法
    triggerArchiveAnimation,
    triggerBatchArchiveAnimation,
    
    // 状态查询
    isAnimating,
    isAnimationCompleted,
    
    // 工具方法
    clearAnimationState,
    playArchiveSound,
    getAnimationStats
  }
}