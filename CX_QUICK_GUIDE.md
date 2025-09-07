# 🚀 CodeX (cx) 快速使用指南 - 5秒学会

## 一行搞定
```bash
cx -p "你的问题"
```

## 核心命令
```bash
# 基础使用
cx -p "分析代码问题"                    # 默认使用GPT-5
cx -p "修复Bug" --mode fix              # 修复模式
cx -p "重构代码" --mode refactor        # 重构模式
cx -p "生成测试" --mode test            # 测试模式
cx -p "写文档" --mode docs              # 文档模式

# 查看帮助
cx --help
```

## 实战示例
```bash
# 1. 代码审查
cx -p "审查EnhancedCsrfSecurityConfig的安全性"

# 2. Bug修复建议
cx -p "分析并修复JWT认证绕过问题" --mode fix

# 3. 性能优化
cx -p "优化数据库查询性能"

# 4. 安全分析
cx -p "查找SQL注入和XSS漏洞"
```

## 关键特性
- **默认模型**: GPT-5 (最强编程能力)
- **流式输出**: 已启用，避免超时
- **只读模式**: 提供建议，不自动修改
- **超时设置**: 600秒

## 注意事项
✅ **正确用法**:
- 具体描述问题
- 使用合适的mode
- 等待完整响应

❌ **避免**:
- 过于宽泛的问题
- 同时提多个无关问题
- 中断响应

## 工作流程
1. **cx分析** → 获取建议
2. **Claude实施** → 修改代码
3. **编译测试** → 验证效果

## 位置
- 命令: `/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/codex-cli`
- 已添加到PATH，可直接使用`cx`

---
**记住**: cx提供建议，Claude执行修改！