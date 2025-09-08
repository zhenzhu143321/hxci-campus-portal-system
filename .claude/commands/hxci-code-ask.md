---
allowed-tools: Bash(cx:*), Bash(find:*), Bash(git:*), Read, Grep
argument-hint: 技术问题或编码疑问 [具体的技术问题描述]
description: 编码咨询工作流 - Codex GPT-5实时技术支持
model: claude-4-sonnet
---

# HXCI编码咨询工作流 - AI技术专家支持

**技术问题**: $ARGUMENTS

## 上下文自动收集

### 当前开发上下文
- 工作目录: !`pwd`
- Git工作状态: !`git status --porcelain | head -10`
- 最近修改的文件: !`git diff --name-only HEAD~1..HEAD | head -5`
- 当前分支: !`git branch --show-current`

### 相关代码定位
基于问题内容 "$ARGUMENTS" 自动定位相关文件：

- Java文件搜索: !`find yudao-boot-mini -name "*.java" -type f -exec grep -l "$ARGUMENTS" {} \\; 2>/dev/null | head -5`
- Vue文件搜索: !`find hxci-campus-portal -name "*.vue" -type f -exec grep -l "$ARGUMENTS" {} \\; 2>/dev/null | head -3`
- 配置文件搜索: !`find . -name "*.yml" -o -name "*.properties" | xargs grep -l "$ARGUMENTS" 2>/dev/null | head -3`

### 项目技术栈信息
- 项目依赖: !`head -20 yudao-boot-mini/pom.xml | grep -E "<groupId>|<artifactId>|<version>"`
- 前端依赖: !`head -20 hxci-campus-portal/package.json | grep -E "dependencies|devDependencies" -A5`
- Spring配置: !`ls yudao-boot-mini/yudao-server/src/main/resources/*.yml | head -3`

## Codex GPT-5 专业咨询

使用 **Codex GPT-5** 获取专业技术建议：

**咨询请求**: !`cx -p "基于HXCI校园门户系统项目，提供以下技术问题的专业建议：

### 问题描述
$ARGUMENTS

### 项目技术背景
- **架构**: Spring Boot 3.4.5 + Vue 3 + yudao框架
- **认证系统**: JWT + CSRF + Basic 三重Token认证
- **数据库**: MySQL + Redis缓存 + MyBatis-Plus
- **前端**: Vue 3 + Vite + TypeScript + Element Plus
- **安全要求**: P0级安全标准，防SQL注入、XSS、CSRF等
- **性能要求**: 5000+ QPS并发处理能力

### 当前开发状态
[基于上述收集的Git状态和文件变更信息]

### 咨询要求
1. **技术方案建议**: 针对问题提供最佳实践方案
2. **代码实现指导**: 提供具体的代码示例和实现步骤
3. **潜在风险警告**: 识别可能的技术风险和陷阱
4. **性能优化建议**: 相关的性能考虑和优化措施
5. **安全考虑**: 确保方案符合P0安全标准
6. **集成兼容性**: 与现有yudao框架和三重认证的兼容性

请提供详细、可执行的技术建议，包含：
✅ 推荐的解决方案
⚠️ 需要注意的风险点
🔧 具体的代码实现示例
📋 实施步骤清单" --mode analyze`

## 上下文增强分析

### 相关代码片段收集
如果找到相关文件，自动读取关键代码片段提供给Codex分析：

**自动文件内容分析**: 基于搜索结果，智能读取最相关的代码文件片段（前50行），为Codex提供更完整的上下文信息。

### 历史问题检索
- 检查是否有类似问题的历史记录
- 查看Git提交历史中的相关变更
- 分析项目文档中的相关说明

## 实用建议生成

### 立即可行的解决方案
基于Codex分析结果，提供：

1. **快速解决方案**: 最直接的问题解决方法
2. **代码示例**: 可以直接使用的代码模板
3. **配置调整**: 需要修改的配置文件和参数
4. **测试验证**: 验证解决方案的测试步骤

### 深入优化建议
1. **架构改进**: 长期架构优化建议
2. **性能提升**: 相关的性能优化机会
3. **扩展性考虑**: 未来扩展的技术准备
4. **最佳实践**: 业界标准和最佳实践

## 问题解决跟踪

### 解决方案记录
将本次咨询的问题和解决方案记录到项目知识库：

- **问题分类**: 技术栈/架构/性能/安全等
- **解决方案**: Codex提供的核心建议
- **实施结果**: 后续实施效果追踪
- **经验总结**: 可复用的解决经验

### 后续跟进建议
- 实施解决方案后建议执行: `/hxci-code-done [修改的文件]`
- 如有更多技术问题可继续: `/hxci-code-ask [新问题]`
- 完成编码工作后: `/hxci-progress-save [工作描述]`

## 专家级咨询特色

### 上下文感知
- 自动收集项目相关代码和配置
- 理解当前开发进度和技术选择
- 结合项目特定的架构和约束

### 实践导向
- 提供可直接使用的代码解决方案
- 考虑项目现实约束和技术债务
- 平衡理想方案与实施可行性

### 持续学习
- 记录问题解决模式，提升后续咨询质量
- 积累项目特定的技术知识库
- 支持复杂问题的多轮咨询

**执行方式**: `/hxci-code-ask "JWT Token验证失败如何调试"` 或 `/hxci-code-ask "Vue组件间通信最佳实践"`