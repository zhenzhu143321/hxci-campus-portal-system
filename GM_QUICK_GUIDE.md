# 🔍 Gemini (gm) 快速使用指南 - 5秒学会

## 一行搞定
```bash
gm "你的问题"
```

## 核心命令
```bash
# 基础使用
gm "分析项目架构"                      # 快速分析
gm "扫描安全风险"                      # 安全扫描
gm "评估代码质量"                      # 质量评估
gm "找出性能瓶颈"                      # 性能分析

# 使用强大模型
gm "深度分析问题" --model gemini-2.5-pro

# 查看帮助
gm --help
```

## 实战示例
```bash
# 1. 架构分析
gm "分析微服务架构设计"

# 2. 安全扫描
gm "扫描CSRF和JWT安全配置"

# 3. 性能评估
gm "分析Redis缓存使用效率"

# 4. 代码质量
gm "评估Controller层代码质量"
```

## 工作原理
```bash
# gm命令实际执行:
GOOGLE_GEMINI_BASE_URL='http://127.0.0.1:8888' GEMINI_API_KEY='test' gemini -p "你的问题"
```

## 关键特性
- **默认模型**: gemini-2.5-flash (快速)
- **高级模型**: gemini-2.5-pro (深度分析)
- **代理服务**: localhost:8888 (OpenRouter)
- **无限额度**: 付费API，无速率限制

## 注意事项
✅ **正确用法**:
- 直接使用gm命令
- 使用分析性问题，避免执行性任务
- 忽略JSON错误（已知bug）
- 测试连接用: `gm-test` 命令

❌ **避免**:
- 使用--all-files参数
- 分开设置环境变量
- 超长输入(>1M tokens)
- 让gemini执行系统命令（如"测试连接"）

⚠️ **重要提醒**:
- Gemini CLI是"交互式CLI代理"，会尝试执行系统操作
- 避免用"测试"、"检查"、"执行"等动词，用"分析"、"评估"、"说明"
- 示例：❌ "测试连接" → ✅ "分析代码" 或 "说'你好'"

## 服务管理
```bash
# 检查代理状态
cd /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/
./start-gemini-proxy.sh status

# 重启代理(如遇问题)
./start-gemini-proxy.sh restart
```

## 位置
- 命令: `/opt/hxci-campus-portal/hxci-campus-portal-system/gm`
- 代理: `scripts/ai-collaboration/gemini-openrouter-proxy.py`

---
**记住**: gm分析项目，cx审查代码，Claude实施修改！