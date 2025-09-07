# 🎉 Codex CLI + OpenRouter 集成成功！

> **状态**: ✅ 完全可用  
> **更新时间**: 2025-09-06  
> **核心突破**: 使用`-c`参数配置自定义provider

## 一、成功方案

### 🔑 关键发现
- Codex CLI支持通过`-c`参数动态配置provider
- 可以覆盖baseURL和使用环境变量传递API密钥
- 无需修改配置文件，直接在命令行指定

### ✅ 工作配置
```bash
export OPENROUTER_API_KEY="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"

codex exec \
  -c model_provider=openrouter \
  -c model_providers.openrouter.name=OpenRouter \
  -c model_providers.openrouter.base_url=https://openrouter.ai/api/v1 \
  -c model_providers.openrouter.env_key=OPENROUTER_API_KEY \
  -c model=openai/gpt-4o \
  --skip-git-repo-check \
  --sandbox read-only \
  "你的任务"
```

## 二、快捷命令 cx

### 📦 已创建的全局命令
```bash
# 像使用gemini一样使用codex
cx -p "分析项目架构"
cx -p "修复bug" --mode fix --auto
cx -p "重构代码" --mode refactor --full-auto
```

### 🎯 命令位置
- 脚本位置: `/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/codex-cli`
- 全局链接: `/usr/local/bin/cx`

### 📋 支持的参数
```bash
cx --help

选项:
  -p, --prompt <描述>    任务描述（必需）
  -m, --model <模型>     使用的模型（默认: openai/gpt-4o）
  --mode <模式>          执行模式: analyze|fix|refactor|test|docs
  --auto                 允许修改工作区文件
  --full-auto           完全自动化（包括执行命令）
  --json                 输出JSON格式
  -o, --output <文件>    保存输出到文件
```

## 三、使用示例

### 基础分析（只读）
```bash
cx -p "分析三重Token认证架构"
cx -p "查找性能瓶颈"
cx -p "列出主要模块"
```

### 代码修复（自动编辑）
```bash
cx -p "修复JWT认证失败的问题" --mode fix --auto
cx -p "修复eslint错误" --mode fix --full-auto
```

### 代码重构
```bash
cx -p "重构NotificationController" --mode refactor --auto
cx -p "提取业务逻辑到Service层" --mode refactor --full-auto
```

### 生成测试
```bash
cx -p "为认证模块写单元测试" --mode test --auto
cx -p "生成集成测试" --mode test --full-auto
```

### 生成文档
```bash
cx -p "生成API文档" --mode docs -o api-docs.md
cx -p "生成README" --mode docs --auto
```

## 四、与Gemini CLI对比

| 特性 | Gemini CLI | Codex CLI (cx) |
|------|------------|---------------|
| **非交互执行** | ✅ gemini -p | ✅ cx -p |
| **OpenRouter集成** | ✅ 代理服务器 | ✅ 直接配置 |
| **代码分析** | ✅ 强大 | ✅ 可用 |
| **文件修改** | ❌ 只读 | ✅ 可写 |
| **命令执行** | ❌ 不支持 | ✅ 支持 |
| **配置难度** | 简单 | 中等 |
| **使用便捷性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

## 五、最佳实践

### 🎯 什么时候用cx (Codex)
- 需要修改文件
- 需要执行命令
- 需要自动化操作
- 简单的代码生成

### 🔍 什么时候用gemini
- 深度代码分析
- 架构审查
- 性能分析
- 安全扫描

### 🤝 协同使用
```bash
# 1. 先用gemini分析
gemini -p "分析性能瓶颈"

# 2. 再用cx修复
cx -p "根据分析结果优化性能" --mode fix --auto

# 3. 最后验证
gemini -p "验证优化效果"
```

## 六、注意事项

### ⚠️ 沙盒限制
- `read-only`: 某些命令受限（如find）
- `workspace-write`: 可以修改项目文件
- `danger-full-access`: 完全权限（谨慎）

### 🔒 安全建议
1. 默认使用只读模式
2. 修改前先备份
3. 使用Git跟踪变更
4. 审查生成的代码

### 🐛 已知问题
1. 沙盒模式下某些系统命令不可用
2. 大文件分析可能超时
3. JSON输出模式有时不稳定

## 七、完整工具链

### 现在可用的AI工具
1. **Gemini CLI** - 深度分析
   ```bash
   gemini -p "分析需求"
   ```

2. **Codex CLI (cx)** - 代码生成/修改
   ```bash
   cx -p "编码任务"
   ```

3. **GPT-5 直接调用** - 复杂问题
   ```bash
   curl OpenRouter API
   ```

4. **MCP工具** - 多模态分析
   ```bash
   mcp__openrouter__chat_with_model
   ```

## 八、故障排查

### 问题1：401认证错误
**解决**: 确保设置了环境变量
```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
```

### 问题2：命令执行失败
**解决**: 使用更宽松的沙盒模式
```bash
cx -p "任务" --full-auto
```

### 问题3：找不到文件
**解决**: 确保在项目目录执行
```bash
cd /opt/hxci-campus-portal/hxci-campus-portal-system
cx -p "任务"
```

## 九、配置文件参考

虽然使用`-c`参数更灵活，但也可以配置文件：

**~/.codex/config.toml**:
```toml
preferred_auth_method = "apikey"
model = "openai/gpt-4o"
model_provider = "openrouter"

[model_providers.openrouter]
name = "OpenRouter"
base_url = "https://openrouter.ai/api/v1"
env_key = "OPENROUTER_API_KEY"
```

## 十、总结

### ✅ 成功要点
1. 使用`-c`参数配置provider
2. 通过环境变量传递API密钥
3. 正确设置baseURL
4. 选择合适的沙盒模式

### 🎉 成就
- Codex CLI成功集成OpenRouter
- 创建了便捷的cx命令
- 实现了非交互式执行
- 完善了AI工具链

### 🚀 下一步
- 优化沙盒配置
- 创建更多预设模式
- 集成到CI/CD流程
- 编写自动化脚本

---

**集成状态**: ✅ 完全成功  
**可用性**: 🌍 全局可用  
**命令**: `cx -p "你的任务"`  
**维护者**: Claude Code AI