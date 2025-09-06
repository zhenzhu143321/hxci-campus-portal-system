# 🚀 AI协作代码上下文传递工具集

## 📋 快速使用指南

### 1️⃣ 收集代码上下文（强制第一步）
```bash
# Java API开发
./ai-context-collector.sh java-api /path/to/Controller.java

# Vue组件开发
./ai-context-collector.sh vue-component /path/to/Component.vue

# Bug修复
./ai-context-collector.sh bug-fix /path/to/problematic-file.java
```

### 2️⃣ 生成标准模板
```bash
# 生成所有模板
./context-template-generator.sh all

# 生成特定模板
./context-template-generator.sh school-api
./context-template-generator.sh bug-fix
./context-template-generator.sh performance
./context-template-generator.sh review
```

### 3️⃣ AI调用（带验证）
```bash
# 使用拦截器（推荐 - 强制验证代码上下文）
./ai-agent-wrapper.sh gpt5 "$(cat context-temp/context-*.md)" 3000 0.3

# 或使用标准助手（基础调用）
./ai-assistant.sh auto "你的问题和代码上下文"
```

### 4️⃣ 监控和报告
```bash
# 生成质量报告
./agent-monitor.sh report

# 实时监控（10秒刷新）
./agent-monitor.sh monitor

# 清理旧日志
./agent-monitor.sh clean
```

## 🔧 核心工具说明

| 工具 | 功能 | 使用场景 |
|------|------|----------|
| **ai-context-collector.sh** | 自动收集完整代码上下文 | 调用AI前必须使用 |
| **ai-agent-wrapper.sh** | 拦截验证AI调用 | 强制执行代码上下文规则 |
| **context-template-generator.sh** | 生成标准化模板 | 不同场景的规范化 |
| **agent-monitor.sh** | 监控AI调用质量 | 定期检查和改进 |
| **ai-assistant.sh** | 基础AI调用工具 | 直接调用不同模型 |

## ⚠️ 强制规则

### ✅ 必须遵守
1. **调用AI前必须先收集代码上下文**
2. **使用完整文件，不是代码片段**
3. **包含所有相关依赖文件**
4. **明确说明问题和期望输出**

### ❌ 绝对禁止
1. **不传递代码直接问技术问题**
2. **只传递几行代码片段**
3. **忽略上下文收集直接调用**
4. **问题描述模糊不清**

## 📊 质量标准

- **代码上下文完整性**: 必须包含主文件+依赖
- **质量分数**: 60%及格，80%优秀
- **违规容忍度**: 最多3次，超过需要培训
- **响应准确率目标**: 85%+

## 🎯 典型工作流

```bash
# 示例：修复学校API集成bug
cd /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/

# 1. 收集代码上下文
./ai-context-collector.sh java-api \
    ../../yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/controller/MockAuthController.java

# 2. 查看收集的报告
cat context-temp/context-*.md

# 3. 使用模板组织问题（可选）
cp templates/school-api-integration.md my-request.md
# 编辑 my-request.md 添加具体问题

# 4. 调用AI（带验证）
./ai-agent-wrapper.sh gpt5 "$(cat my-request.md)" 3000 0.3

# 5. 查看监控报告
./agent-monitor.sh report
```

## 📁 目录结构

```
ai-collaboration/
├── ai-context-collector.sh       # 代码收集器
├── ai-agent-wrapper.sh          # 调用拦截器
├── context-template-generator.sh # 模板生成器
├── agent-monitor.sh             # 监控系统
├── ai-assistant.sh              # 基础调用工具
├── context-temp/                # 临时上下文存储
├── templates/                   # 标准化模板
├── logs/                        # 调用日志
└── reports/                     # 监控报告
```

## 🚨 故障排除

### 问题：收集器执行失败
```bash
# 使用绝对路径
./ai-context-collector.sh java-api /absolute/path/to/file.java

# 调试模式
bash -x ./ai-context-collector.sh java-api /path/to/file.java
```

### 问题：AI响应不准确
- 检查是否包含完整代码上下文
- 增加更多相关文件
- 使用模板规范化问题

### 问题：监控报告无数据
- 确保使用 ai-agent-wrapper.sh 调用
- 检查 logs/ 目录权限
- 运行 ./agent-monitor.sh clean 清理旧数据

## 📈 效果评估

实施此机制后预期效果：
- 🎯 代码上下文传递率: 95%+ 
- 🎯 AI响应准确率: 85%+
- 🎯 违规率: <5%
- 🎯 平均质量分数: 80%+

## 🔗 相关文档

- [完整机制说明](../AI_COLLABORATION_ENFORCEMENT_MECHANISM.md)
- [项目开发指南](../../CLAUDE.md)
- [模板使用指南](templates/USAGE_GUIDE.md)

---

**版本**: v1.0 | **更新日期**: 2025-01-05 | **维护者**: Claude Code AI
