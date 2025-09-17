/**
 * 主题配置
 * 
 * @description 应用主题配置，包含颜色、间距、字体等设计系统定义
 * @author Claude Code AI Assistant
 * @date 2025-09-14
 */

// 主题类型定义
export interface ThemeConfig {
  name: string
  colors: ColorConfig
  spacing: SpacingConfig
  typography: TypographyConfig
  borderRadius: BorderRadiusConfig
  shadows: ShadowConfig
  animations: AnimationConfig
  breakpoints: BreakpointConfig
}

// 颜色配置
export interface ColorConfig {
  primary: string
  success: string
  warning: string
  danger: string
  info: string
  text: {
    primary: string
    regular: string
    secondary: string
    placeholder: string
    disabled: string
  }
  border: {
    base: string
    light: string
    lighter: string
    extraLight: string
  }
  background: {
    base: string
    page: string
    light: string
    lighter: string
  }
}

// 间距配置
export interface SpacingConfig {
  unit: number
  xs: string
  sm: string
  md: string
  lg: string
  xl: string
  '2xl': string
  '3xl': string
  '4xl': string
}

// 字体配置
export interface TypographyConfig {
  fontFamily: {
    base: string
    mono: string
  }
  fontSize: {
    xs: string
    sm: string
    base: string
    md: string
    lg: string
    xl: string
    '2xl': string
    '3xl': string
    '4xl': string
  }
  fontWeight: {
    light: number
    regular: number
    medium: number
    semibold: number
    bold: number
  }
  lineHeight: {
    none: number
    tight: number
    normal: number
    relaxed: number
    loose: number
  }
}

// 圆角配置
export interface BorderRadiusConfig {
  none: string
  sm: string
  base: string
  md: string
  lg: string
  xl: string
  '2xl': string
  circle: string
  pill: string
}

// 阴影配置
export interface ShadowConfig {
  none: string
  sm: string
  base: string
  md: string
  lg: string
  xl: string
  '2xl': string
  inner: string
  card: string
  cardHover: string
  cardActive: string
}

// 动画配置
export interface AnimationConfig {
  duration: {
    fast: string
    base: string
    slow: string
    slower: string
  }
  easing: {
    linear: string
    in: string
    out: string
    inOut: string
    bounce: string
  }
}

// 断点配置
export interface BreakpointConfig {
  xs: string
  sm: string
  md: string
  lg: string
  xl: string
  '2xl': string
}

// 默认主题（亮色主题）
export const defaultTheme: ThemeConfig = {
  name: 'light',
  colors: {
    primary: '#409eff',
    success: '#67c23a',
    warning: '#e6a23c',
    danger: '#f56c6c',
    info: '#909399',
    text: {
      primary: '#303133',
      regular: '#606266',
      secondary: '#909399',
      placeholder: '#c0c4cc',
      disabled: '#c0c4cc'
    },
    border: {
      base: '#dcdfe6',
      light: '#e4e7ed',
      lighter: '#ebeef5',
      extraLight: '#f2f6fc'
    },
    background: {
      base: '#ffffff',
      page: '#f5f7fa',
      light: '#fafafa',
      lighter: '#fafbfc'
    }
  },
  spacing: {
    unit: 4,
    xs: '4px',
    sm: '8px',
    md: '12px',
    lg: '16px',
    xl: '20px',
    '2xl': '24px',
    '3xl': '32px',
    '4xl': '40px'
  },
  typography: {
    fontFamily: {
      base: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      mono: '"Monaco", "Menlo", "Ubuntu Mono", monospace'
    },
    fontSize: {
      xs: '12px',
      sm: '13px',
      base: '14px',
      md: '16px',
      lg: '18px',
      xl: '20px',
      '2xl': '24px',
      '3xl': '30px',
      '4xl': '38px'
    },
    fontWeight: {
      light: 300,
      regular: 400,
      medium: 500,
      semibold: 600,
      bold: 700
    },
    lineHeight: {
      none: 1,
      tight: 1.25,
      normal: 1.5,
      relaxed: 1.625,
      loose: 2
    }
  },
  borderRadius: {
    none: '0',
    sm: '2px',
    base: '4px',
    md: '6px',
    lg: '8px',
    xl: '12px',
    '2xl': '16px',
    circle: '50%',
    pill: '9999px'
  },
  shadows: {
    none: 'none',
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    base: '0 2px 4px 0 rgba(0, 0, 0, 0.06)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.07)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
    xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
    '2xl': '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
    inner: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)',
    card: '0 2px 8px rgba(0, 0, 0, 0.06)',
    cardHover: '0 4px 12px rgba(0, 0, 0, 0.1)',
    cardActive: '0 6px 16px rgba(0, 0, 0, 0.12)'
  },
  animations: {
    duration: {
      fast: '150ms',
      base: '250ms',
      slow: '350ms',
      slower: '500ms'
    },
    easing: {
      linear: 'linear',
      in: 'cubic-bezier(0.4, 0, 1, 1)',
      out: 'cubic-bezier(0, 0, 0.2, 1)',
      inOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
      bounce: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)'
    }
  },
  breakpoints: {
    xs: '480px',
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px',
    '2xl': '1536px'
  }
}

// 暗色主题
export const darkTheme: ThemeConfig = {
  ...defaultTheme,
  name: 'dark',
  colors: {
    ...defaultTheme.colors,
    text: {
      primary: '#e4e7ed',
      regular: '#cfd3dc',
      secondary: '#a3a6ad',
      placeholder: '#8b8e95',
      disabled: '#8b8e95'
    },
    border: {
      base: '#4c4d4f',
      light: '#414243',
      lighter: '#363637',
      extraLight: '#2b2b2c'
    },
    background: {
      base: '#1a1a1a',
      page: '#0f0f0f',
      light: '#262626',
      lighter: '#333333'
    }
  },
  shadows: {
    ...defaultTheme.shadows,
    card: '0 2px 8px rgba(0, 0, 0, 0.3)',
    cardHover: '0 4px 12px rgba(0, 0, 0, 0.4)',
    cardActive: '0 6px 16px rgba(0, 0, 0, 0.5)'
  }
}

// 获取CSS变量值
export function getCSSVariable(variable: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(variable).trim()
}

// 设置CSS变量值
export function setCSSVariable(variable: string, value: string): void {
  document.documentElement.style.setProperty(variable, value)
}

// 应用主题
export function applyTheme(theme: ThemeConfig): void {
  // 设置颜色变量
  setCSSVariable('--color-primary', theme.colors.primary)
  setCSSVariable('--color-success', theme.colors.success)
  setCSSVariable('--color-warning', theme.colors.warning)
  setCSSVariable('--color-danger', theme.colors.danger)
  setCSSVariable('--color-info', theme.colors.info)
  
  // 设置文本颜色
  setCSSVariable('--color-text-primary', theme.colors.text.primary)
  setCSSVariable('--color-text-regular', theme.colors.text.regular)
  setCSSVariable('--color-text-secondary', theme.colors.text.secondary)
  setCSSVariable('--color-text-placeholder', theme.colors.text.placeholder)
  
  // 设置边框颜色
  setCSSVariable('--color-border-base', theme.colors.border.base)
  setCSSVariable('--color-border-light', theme.colors.border.light)
  setCSSVariable('--color-border-lighter', theme.colors.border.lighter)
  
  // 设置背景颜色
  setCSSVariable('--color-bg-base', theme.colors.background.base)
  setCSSVariable('--color-bg-page', theme.colors.background.page)
  setCSSVariable('--color-bg-light', theme.colors.background.light)
  
  // 保存主题到localStorage
  localStorage.setItem('theme', theme.name)
}

// 获取当前主题
export function getCurrentTheme(): ThemeConfig {
  const themeName = localStorage.getItem('theme') || 'light'
  return themeName === 'dark' ? darkTheme : defaultTheme
}

// 切换主题
export function toggleTheme(): void {
  const currentTheme = getCurrentTheme()
  const newTheme = currentTheme.name === 'light' ? darkTheme : defaultTheme
  applyTheme(newTheme)
}

// 初始化主题
export function initTheme(): void {
  const theme = getCurrentTheme()
  applyTheme(theme)
}

// 导出所有主题
export const themes = {
  light: defaultTheme,
  dark: darkTheme
}