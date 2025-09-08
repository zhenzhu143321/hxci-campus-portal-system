---
allowed-tools: Bash(export:*), Bash(gm:*), Bash(gemini:*), Bash(git:*), Bash(find:*), Read, Grep
argument-hint: 指定开发模块 [security|notification|auth|frontend|backend|具体文件路径]
description: 编码开始工作流 - Gemini冲突扫描和架构分析
model: sonnet
---

# HXCI编码开始工作流 - AI辅助架构分析

**开发目标**: $ARGUMENTS

## 环境准备和状态收集

### Gemini分析环境初始化
- 设置Gemini代理环境: !`export GOOGLE_GEMINI_BASE_URL='http://127.0.0.1:8888' && export GEMINI_API_KEY='test'`
- 验证Gemini代理状态: !`curl -s http://127.0.0.1:8888/health || echo "代理服务异常"`

### 当前项目状态收集
- Git工作区状态: !`git status --porcelain`
- 最近提交信息: !`git log --oneline -3`
- 当前分支: !`git branch --show-current`
- 待处理文件: !`git diff --name-only HEAD`

### 目标模块分析准备
- 相关Java文件定位: !`find yudao-boot-mini -name "*.java" -path "*$ARGUMENTS*" -type f | head -10`
- 相关Vue文件定位: !`find hxci-campus-portal -name "*.vue" -path "*$ARGUMENTS*" -type f | head -5`
- 配置文件检查: !`find . -name "*.yml" -o -name "*.properties" -o -name "package.json" | grep -v node_modules | head -5`

## Gemini深度架构分析

使用 **Gemini 2.5 Pro** 进行专业架构分析：

**分析任务**: "对HXCI校园门户系统进行深度架构分析，重点关注$ARGUMENTS模块的开发准备。请执行以下分析：

### 1. 架构冲突检测
- 扫描现有$ARGUMENTS相关代码，识别潜在的架构冲突点
- 分析与现有Spring Boot + yudao框架的集成兼容性
- 检查数据库设计和API接口的一致性问题
- 识别可能的循环依赖和耦合度过高的风险

### 2. 设计一致性验证
- 验证$ARGUMENTS模块与三重Token认证架构的兼容性
- 检查权限系统和RBAC模型的集成点
- 分析前后端数据格式和API契约的一致性
- 评估缓存策略和性能优化的协调性

### 3. 技术风险评估
- 识别$ARGUMENTS模块开发中的高风险技术选择
- 分析可能的性能瓶颈和并发问题
- 评估安全风险点，特别是P0级安全要求
- 检查第三方依赖的版本兼容性

### 4. 实施建议生成
- 提供$ARGUMENTS模块的具体开发路径建议
- 推荐最佳实践和设计模式选择
- 建议避免的反模式和常见陷阱
- 制定分阶段实施计划

**分析上下文**:
- 项目类型: 校园门户系统 (Spring Boot 3.4.5 + Vue 3)
- 技术栈: yudao框架 + JWT+CSRF+Basic三重认证 + Redis缓存
- 当前状态: Git工作区和最近提交信息如上
- 开发重点: 确保与现有架构的完美集成

请生成详细的分析报告，包含：
✅ 可以安全开发的功能点
⚠️  需要特别注意的风险点  
🔧 具体的实施建议和代码示例
📋 开发前的准备清单"

## 开发准备清单生成

基于Gemini分析结果，生成开发行动计划：

### 立即可执行的任务
- 需要创建/修改的核心文件清单
- 必须引入的依赖和配置变更
- 数据库表结构调整需求

### 风险缓解措施  
- 需要避免的代码模式和架构选择
- 必须遵循的安全编码规范
- 性能和并发考虑要点

### 质量保证检查点
- 代码完成后的验证步骤
- 需要执行的测试用例
- 文档更新和状态同步要求

## 后续建议

**下一步操作建议**:
1. 基于分析结果开始编码实施
2. 编码过程中遇到问题时使用: `/hxci-code-ask [具体问题]`
3. 编码完成后执行质量检查: `/hxci-code-done [文件/模块]`
4. 保存工作状态和更新文档: `/hxci-progress-save [工作描述]`

**执行方式**: `/hxci-code-start security` 或 `/hxci-code-start P0-SEC-04`