import { marked } from 'marked'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

/**
 * Markdown渲染工具函数
 *
 * @description 将Markdown格式的文本安全地转换为HTML，支持基础格式化
 * @author Frontend-Developer AI Assistant
 * @date 2025-09-13
 *
 * 特性：
 * - 支持基础Markdown语法（换行、粗体、斜体、链接、列表等）
 * - XSS安全防护（使用DOMPurify清理HTML）
 * - 适配通知内容显示场景
 * - 向后兼容纯文本内容
 */

/**
 * 配置marked渲染器选项
 */
const markedOptions: marked.MarkedOptions = {
  // 启用换行符转换为<br>
  breaks: true,
  // 禁用HTML标签（防止XSS，交给DOMPurify处理）
  sanitize: false,
  // 启用GitHub风格的markdown
  gfm: true,
  // 禁用headerIds（避免DOM id冲突）
  headerIds: false,
  // 禁用mangle（保持链接地址原样）
  mangle: false
}

// 配置marked
marked.setOptions(markedOptions)

/**
 * DOMPurify配置选项
 */
const purifyConfig = {
  // 允许的标签
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'em', 'b', 'i', 'u',
    'ul', 'ol', 'li', 'blockquote', 'code',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'a', 'span'
  ],
  // 允许的属性
  ALLOWED_ATTR: ['href', 'title', 'target', 'class'],
  // 自动添加target="_blank"到外部链接
  ADD_ATTR: ['target'],
  // 禁止data:和javascript:协议
  FORBID_CONTENTS: ['script', 'style'],
  // 移除空白标签
  REMOVE_EMPTY: ['p', 'div', 'span']
}

/**
 * 将Markdown文本渲染为安全的HTML
 *
 * @param markdownText 需要渲染的Markdown文本
 * @param options 渲染选项
 * @returns 安全的HTML字符串
 */
export const renderMarkdown = (
  markdownText: string | null | undefined,
  options: {
    maxLength?: number;      // 最大字符长度（渲染前截取）
    fallback?: string;       // 空值时的备用文本
    enableSummary?: boolean; // 是否启用摘要模式（移除标题、列表等复杂元素）
  } = {}
): string => {
  const { maxLength, fallback = '', enableSummary = false } = options

  console.debug('🔍 [renderMarkdown] 调用参数:', {
    markdownText: markdownText?.substring(0, 50) + '...',
    maxLength,
    enableSummary,
    fallback
  })

  // 🔧 P0级防护：处理空值
  if (!markdownText || markdownText.trim() === '') {
    console.debug('renderMarkdown: 空输入，返回fallback')
    return fallback
  }

  try {
    let processedText = markdownText.trim()

    // 📏 长度限制（在渲染前处理，提升性能）
    if (maxLength && processedText.length > maxLength) {
      processedText = processedText.substring(0, maxLength) + '...'
      console.debug('renderMarkdown: 应用长度限制:', maxLength)
    }

    // 📝 渲染Markdown为HTML
    let htmlContent = marked.parse(processedText) as string

    // 🎯 摘要模式：简化HTML结构
    if (enableSummary) {
      // 移除标题标签，转换为普通段落
      htmlContent = htmlContent.replace(/<h[1-6][^>]*>(.*?)<\/h[1-6]>/gi, '<p><strong>$1</strong></p>')

      // 简化列表为普通文本
      htmlContent = htmlContent.replace(/<ul[^>]*>(.*?)<\/ul>/gis, '<p>$1</p>')
      htmlContent = htmlContent.replace(/<ol[^>]*>(.*?)<\/ol>/gis, '<p>$1</p>')
      htmlContent = htmlContent.replace(/<li[^>]*>(.*?)<\/li>/gi, '• $1<br>')

      // 移除引用块的特殊格式
      htmlContent = htmlContent.replace(/<blockquote[^>]*>(.*?)<\/blockquote>/gis, '<p><em>$1</em></p>')
    }

    // 🛡️ 安全防护：使用DOMPurify清理HTML
    const safeHtml = DOMPurify.sanitize(htmlContent, purifyConfig)

    // 🔗 处理外部链接（添加target="_blank"）
    const finalHtml = safeHtml.replace(
      /<a\s+href="https?:\/\/[^"]*"[^>]*>/gi,
      (match) => {
        if (!match.includes('target=')) {
          return match.replace('>', ' target="_blank" rel="noopener noreferrer">')
        }
        return match
      }
    )

    console.debug('✅ [renderMarkdown] 渲染成功:', {
      原始长度: markdownText.length,
      HTML长度: finalHtml.length,
      摘要模式: enableSummary
    })

    return finalHtml

  } catch (error) {
    console.error('❌ [renderMarkdown] 渲染失败:', error)
    console.error('输入内容:', markdownText)

    // 🔧 降级处理：返回纯文本（保留换行）
    const fallbackHtml = markdownText
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')

    return DOMPurify.sanitize(fallbackHtml, purifyConfig)
  }
}

/**
 * 专门用于通知内容摘要的markdown渲染
 * 🔧 修复版：集成转义字符处理，解决显示原始markdown问题
 *
 * @param content 通知内容
 * @param maxLength 最大长度，默认100字符
 * @returns 渲染后的HTML字符串
 */
export const renderNotificationSummary = (
  content: string | null | undefined,
  maxLength: number = 100
): string => {
  if (!content || content.trim() === '') {
    return '<span class="empty-content">暂无内容</span>'
  }

  console.debug('🔍 [renderNotificationSummary] 原始内容:', content.substring(0, 50) + '...')

  try {
    // 🔧 核心修复：先解码转义字符，再进行Markdown渲染
    const normalizedContent = decodeEscapes(content)
    console.debug('🔧 [renderNotificationSummary] 转义解码后:', normalizedContent.substring(0, 50) + '...')

    return renderMarkdown(normalizedContent, {
      maxLength,
      fallback: '',
      enableSummary: true  // 启用摘要模式，简化复杂元素
    })
  } catch (error) {
    console.error('❌ [renderNotificationSummary] 渲染失败:', error)
    // 降级处理：返回纯文本
    return content.replace(/</g, '&lt;').replace(/>/g, '&gt;').substring(0, maxLength) + (content.length > maxLength ? '...' : '')
  }
}

/**
 * 专门用于通知详情的完整markdown渲染
 *
 * @param content 通知内容
 * @returns 渲染后的HTML字符串
 */
export const renderNotificationDetail = (
  content: string | null | undefined
): string => {
  return renderMarkdown(content, {
    fallback: '无内容',
    enableSummary: false  // 禁用摘要模式，保留完整格式
  })
}

/**
 * 检测文本是否包含Markdown语法
 *
 * @param text 待检测的文本
 * @returns 是否包含Markdown语法
 */
export const containsMarkdown = (text: string | null | undefined): boolean => {
  if (!text) return false

  // 检测常见的Markdown语法特征
  const markdownPatterns = [
    /\*\*.*?\*\*/,           // 粗体 **text**
    /\*.*?\*/,               // 斜体 *text*
    /_.*?_/,                 // 斜体 _text_
    /`.*?`/,                 // 行内代码 `code`
    /^\s*[-*+]\s+/m,         // 无序列表
    /^\s*\d+\.\s+/m,         // 有序列表
    /^\s*#+\s+/m,            // 标题
    /\[.*?\]\(.*?\)/,        // 链接 [text](url)
    /^\s*>\s+/m              // 引用块
  ]

  return markdownPatterns.some(pattern => pattern.test(text))
}

/**
 * 从markdown文本中提取纯文本（用于搜索、预览等）
 *
 * @param markdownText markdown文本
 * @returns 纯文本字符串
 */
export const extractPlainText = (markdownText: string | null | undefined): string => {
  if (!markdownText) return ''

  try {
    // 先渲染为HTML
    const html = marked.parse(markdownText) as string
    // 移除所有HTML标签
    const plainText = html.replace(/<[^>]*>/g, '')
    // 清理多余的空白字符
    return plainText.replace(/\s+/g, ' ').trim()
  } catch (error) {
    console.warn('extractPlainText: 解析失败，使用简单清理', error)
    // 降级处理：简单清理markdown语法
    return markdownText
      .replace(/[*_`#>\[\]()]/g, '')
      .replace(/\s+/g, ' ')
      .trim()
  }
}

// ========== 新增：专用于通知详情对话框的增强版渲染器 ==========

// 创建专用的MarkdownIt实例，用于通知详情对话框
const mdAdvanced = new MarkdownIt({
  html: false,        // 禁用HTML标签，防止XSS
  linkify: true,      // 自动转换URL为链接
  breaks: true,       // 换行符转为<br>
  typographer: true   // 启用排版优化
})

// 配置链接安全属性
mdAdvanced.renderer.rules.link_open = (tokens, idx, options, _env, self) => {
  const token = tokens[idx]

  // 添加target="_blank"
  const targetIndex = token.attrIndex('target')
  if (targetIndex < 0) {
    token.attrPush(['target', '_blank'])
  } else {
    token.attrs![targetIndex][1] = '_blank'
  }

  // 添加rel="noopener nofollow"
  const relIndex = token.attrIndex('rel')
  if (relIndex < 0) {
    token.attrPush(['rel', 'noopener nofollow'])
  } else {
    token.attrs![relIndex][1] = 'noopener nofollow'
  }

  return self.renderToken(tokens, idx, options)
}

/**
 * 解码转义字符
 * 处理 \\n \\t \\r \" 等转义序列，以及HTML实体
 */
export const decodeEscapes = (text: string): string => {
  if (!text) return ''

  let result = text

  // 检测并处理JSON转义序列
  if (/(\\n|\\t|\\r|\\u[0-9a-fA-F]{4}|\\\\|\\")/.test(text)) {
    try {
      // 尝试作为JSON字符串解析
      result = JSON.parse('"' + text.replace(/"/g, '\\"') + '"')
    } catch {
      // 如果JSON解析失败，手动替换常见转义字符
      result = text
        .replace(/\\\\/g, '\\')
        .replace(/\\n/g, '\n')
        .replace(/\\r/g, '\r')
        .replace(/\\t/g, '\t')
        .replace(/\\"/g, '"')
    }
  }

  // 解码HTML实体（&amp; &lt; &gt; 等）
  if (result.includes('&')) {
    const textarea = document.createElement('textarea')
    textarea.innerHTML = result
    result = textarea.value
  }

  return result.trim()
}

/**
 * 专用于通知详情对话框的增强版Markdown渲染
 * 解决转义字符显示、正确渲染Markdown格式
 *
 * @param rawContent 原始内容（可能包含转义字符）
 * @returns 安全的HTML字符串
 */
export const renderNotificationDialog = (rawContent: string): string => {
  if (!rawContent) {
    return '<p class="empty-content">暂无内容</p>'
  }

  console.debug('🔍 [renderNotificationDialog] 原始内容:', rawContent.substring(0, 100) + '...')

  try {
    // 1. 解码转义字符
    const normalizedContent = decodeEscapes(rawContent)
    console.debug('🔧 [renderNotificationDialog] 转义解码后:', normalizedContent.substring(0, 100) + '...')

    // 2. 处理特殊的通知内容格式
    let processedContent = normalizedContent
      .replace(/\\n\\n\\n+/g, '\n\n')  // 处理多余的换行符
      .replace(/&gt;/g, '>')          // 处理转义的大于号
      .replace(/&lt;/g, '<')          // 处理转义的小于号
      .replace(/&amp;/g, '&')         // 处理转义的&符号

    // 3. 使用MarkdownIt渲染
    const renderedHtml = mdAdvanced.render(processedContent)
    console.debug('📝 [renderNotificationDialog] Markdown渲染后长度:', renderedHtml.length)

    // 4. 使用DOMPurify清理HTML，防止XSS攻击
    const sanitizedHtml = DOMPurify.sanitize(renderedHtml, {
      ALLOWED_TAGS: [
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'p', 'br', 'hr',
        'ul', 'ol', 'li',
        'strong', 'b', 'em', 'i',
        'code', 'pre',
        'blockquote',
        'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'a', 'span'
      ],
      ALLOWED_ATTR: ['href', 'target', 'rel', 'class'],
      ADD_ATTR: ['target']
    })

    console.debug('✅ [renderNotificationDialog] 最终HTML长度:', sanitizedHtml.length)
    return sanitizedHtml

  } catch (error) {
    console.error('❌ [renderNotificationDialog] 渲染失败:', error)
    console.error('输入内容:', rawContent)

    // 降级处理：返回纯文本（保留换行）
    const fallbackHtml = rawContent
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')
      .replace(/\\n/g, '<br>')  // 处理转义的换行符

    return `<div class="fallback-content">${DOMPurify.sanitize(fallbackHtml)}</div>`
  }
}