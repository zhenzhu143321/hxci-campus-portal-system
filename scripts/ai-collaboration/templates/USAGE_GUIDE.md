# AI协作代码上下文模板使用指南

## 🚀 快速开始

### 1. 选择合适的模板
```bash
# 查看可用模板
ls /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/templates/

# 模板类型：
- school-api-integration.md  # 学校API集成
- bug-fix-template.md        # Bug修复
- performance-optimization.md # 性能优化
- code-review-template.md    # 代码审查
```

### 2. 自动收集代码上下文
```bash
# 使用上下文收集器
./ai-context-collector.sh java-api /path/to/MainFile.java

# 收集器会自动：
- 扫描相关Service和DTO
- 收集配置文件
- 验证完整性
- 生成报告
```

### 3. 填充模板内容
1. 复制对应的模板文件
2. 将收集到的代码粘贴到模板中
3. 填写具体的问题描述
4. 明确期望输出

### 4. 调用AI助手
```bash
# 使用包装器确保质量
./ai-agent-wrapper.sh gpt5 "$(cat filled-template.md)" 3000 0.3

# 或直接调用（已包含完整上下文）
./ai-assistant.sh auto "$(cat filled-template.md)"
```

## ⚠️ 重要原则

### 强制要求
- ✅ 必须包含完整代码文件，不能只有片段
- ✅ 必须包含所有相关依赖文件
- ✅ 必须说明项目背景和技术栈
- ✅ 必须明确问题和期望输出

### 绝对禁止
- ❌ 只传递代码片段
- ❌ 缺少项目上下文
- ❌ 问题描述模糊
- ❌ 没有期望输出

## 📊 质量检查清单

调用AI前，请确认：
- [ ] 主文件完整代码已包含
- [ ] 相关Service/DTO已包含
- [ ] 配置文件相关部分已包含
- [ ] 错误日志/堆栈跟踪已包含（如适用）
- [ ] 问题描述清晰具体
- [ ] 期望输出明确定义

## 🔧 故障排除

### 问题：上下文收集失败
```bash
# 手动收集
find /opt/hxci-campus-portal -name "YourFile.java" -exec cat {} \;
```

### 问题：AI响应不够具体
- 检查是否包含完整代码
- 增加更多上下文文件
- 明确具体需求

### 问题：模板不适合当前场景
- 创建自定义模板
- 组合多个模板内容
- 咨询团队最佳实践

## 💡 最佳实践

1. **分层传递**: Controller → Service → DTO → Config
2. **完整性优先**: 宁多勿少，完整代码比精简重要
3. **具体化需求**: 用示例说明期望的输出格式
4. **版本信息**: 包含框架和依赖版本信息
5. **测试用例**: 如有测试代码，一并提供

## 📈 持续改进

发现模板问题或有改进建议？
1. 记录在 `improvements.log`
2. 定期团队review
3. 更新模板内容
