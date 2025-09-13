import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: true
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      'vue': 'vue/dist/vue.esm-bundler.js'
    }
  },
  server: {
    host: '0.0.0.0',  // 允许外网访问
    port: 3000,
    open: true,
    proxy: {
      // 代理后端API
      '/admin-api': {
        target: 'http://localhost:48081',
        changeOrigin: true
      },
      '/mock-school-api': {
        target: 'http://localhost:48082', 
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    // 🚀 Stage 9性能优化: 生产环境自动移除console日志
    minify: 'terser',
    terserOptions: {
      compress: {
        // 移除所有console输出，提升生产环境性能
        drop_console: true,
        drop_debugger: true,
        // 移除未使用的代码
        dead_code: true,
        // 优化条件表达式
        conditionals: true,
        // 优化布尔值转换
        booleans: true
      }
    },
    // 代码分割优化，减少首屏加载时间
    rollupOptions: {
      output: {
        // 分离第三方库到独立chunk
        manualChunks: {
          'vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus'],
          'utils': ['axios', 'dayjs']
        }
      }
    }
  },
  // 🔧 开发环境优化
  esbuild: {
    // 开发环境保留console用于调试
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : []
  }
})