# 🚀 Codex CLI 集成方案 - OpenRouter强力加持

> **状态**: 🔧 准备集成  
> **优势**: 直接支持环境变量配置，改造极其简单  
> **核心**: OpenAI官方编码助手 + OpenRouter无限额度

## 一、Codex CLI 是什么？

**Codex CLI** 是OpenAI官方推出的命令行编码代理工具，具有以下特点：

### 核心能力
- 🤖 **智能编码代理**: 可以读取、分析、修改、执行代码
- 🖼️ **多模态输入**: 支持文本、截图、图表输入
- 🔒 **本地执行**: 代码永不离开本地环境
- ⚡ **极速响应**: 原生终端体验，无需切换窗口
- 🎯 **精准执行**: 代码生成准确率高，开箱即用

### 与Gemini CLI对比

| 特性 | Codex CLI | Gemini CLI |
|------|-----------|------------|
| **主要用途** | 编码助手、代码生成 | 代码分析、架构审查 |
| **交互方式** | 交互式对话 | 单次查询 |
| **文件操作** | ✅ 可直接修改文件 | ❌ 只读分析 |
| **执行命令** | ✅ 可执行Shell命令 | ❌ 不能执行 |
| **配置方式** | JSON/TOML配置文件 | 环境变量 |
| **最佳场景** | 编码、调试、重构 | 分析、审查、报告 |

## 二、为什么要集成Codex CLI？

### 🎯 解决的痛点
1. **编码效率**: 自动生成代码，减少手动编写
2. **Bug修复**: 智能定位并修复问题
3. **代码重构**: 自动化重构，保持代码质量
4. **测试编写**: 快速生成测试用例
5. **文档生成**: 自动生成代码文档

### 💡 与现有工具协同
- **Gemini CLI**: 负责深度分析和架构审查
- **Codex CLI**: 负责具体编码和执行
- **GPT-5 API**: 负责复杂问题解决
- **完美配合**: 分析→编码→执行一条龙

## 三、集成方案

### 3.1 安装方式

```bash
# 方式1：自动安装脚本
cd /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/
./setup-codex-cli.sh

# 方式2：手动安装
npm install -g @openai/codex
```

### 3.2 配置OpenRouter

#### 配置文件位置
- `~/.codex/config.json` - 主配置文件
- `~/.codex/config.toml` - 备用配置格式

#### 配置内容

**config.json**:
```json
{
  "provider": "openrouter",
  "model": "openai/gpt-5",
  "providers": {
    "openrouter": {
      "name": "OpenRouter",
      "baseURL": "https://openrouter.ai/api/v1",
      "envKey": "OPENROUTER_API_KEY"
    }
  },
  "approval_mode": "suggest",
  "max_tokens": 3000,
  "temperature": 0.3
}
```

**config.toml**:
```toml
model = "openai/gpt-5"
model_provider = "openrouter"

[model_providers.openrouter]
name = "OpenRouter"
base_url = "https://openrouter.ai/api/v1"
api_key_env = "OPENROUTER_API_KEY"
```

### 3.3 环境变量设置

```bash
# 设置OpenRouter API密钥
export OPENROUTER_API_KEY="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"

# 添加到.bashrc永久生效
echo 'export OPENROUTER_API_KEY="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"' >> ~/.bashrc
source ~/.bashrc
```

## 四、使用教程

### 4.1 基础命令

```bash
# 启动Codex CLI
codex

# 直接执行任务
codex "分析并修复登录模块的bug"

# 指定审批模式
codex --suggest "重构通知模块"     # 建议模式（默认）
codex --auto-edit "格式化所有代码"  # 自动编辑模式
codex --auto "自动修复所有测试"     # 全自动模式
```

### 4.2 实战案例

#### 案例1：修复Bug
```bash
codex "项目中的JWT认证有问题，用户登录后无法访问API，请帮我定位并修复"

# Codex会：
# 1. 扫描认证相关代码
# 2. 找出问题所在
# 3. 提出修复方案
# 4. 征求你的同意
# 5. 自动修改代码
```

#### 案例2：实现新功能
```bash
codex "实现一个用户活跃度统计功能，包括：
1. 统计每日登录用户数
2. 统计API调用频率
3. 生成可视化报表
4. 添加定时任务每天凌晨执行"

# Codex会逐步实现每个需求
```

#### 案例3：代码重构
```bash
codex "重构NotificationController，将复杂的业务逻辑提取到Service层"

# Codex会：
# 1. 分析现有代码结构
# 2. 创建Service类
# 3. 迁移业务逻辑
# 4. 更新Controller
# 5. 确保功能不变
```

#### 案例4：写测试
```bash
codex "为三重Token认证系统写完整的单元测试和集成测试"

# Codex会生成全面的测试用例
```

#### 案例5：性能优化
```bash
codex "分析并优化数据库查询性能，特别是通知列表API的N+1问题"

# Codex会：
# 1. 识别性能瓶颈
# 2. 优化查询语句
# 3. 添加适当的索引
# 4. 实施缓存策略
```

### 4.3 高级功能

#### 多模态输入
```bash
# 截图后运行
codex "根据这个设计图实现前端界面" 
# 然后粘贴截图

# 或者引用文件
codex "参考design.png实现这个功能"
```

#### 批处理模式
```bash
# 创建任务文件
cat > tasks.txt <<EOF
1. 修复所有eslint错误
2. 更新过时的依赖
3. 优化打包配置
EOF

# 批量执行
codex < tasks.txt
```

## 五、审批模式详解

### Suggest Mode（建议模式）- 默认
- ✅ 每步都征求许可
- ✅ 完全控制权
- ✅ 适合关键代码修改

### Auto-Edit Mode（自动编辑模式）
- ✅ 自动创建/编辑文件
- ⚠️ 执行命令仍需确认
- ✅ 适合批量文件操作

### Auto Mode（全自动模式）
- ⚠️ 自动执行所有操作
- ⚠️ 包括运行Shell命令
- ✅ 适合可信任务

## 六、与项目集成

### 6.1 工作流程

```mermaid
graph LR
    A[需求分析] --> B[Gemini CLI分析]
    B --> C[Codex CLI编码]
    C --> D[代码执行]
    D --> E[测试验证]
    E --> F[代码提交]
```

### 6.2 快捷命令

已创建的快捷命令：

```bash
# 代码分析
/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/codex-analyze

# Bug修复
/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/codex-fix

# 代码重构
/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/codex-refactor
```

### 6.3 团队协作

```bash
# 代码审查
codex "审查最近的提交，找出潜在问题"

# 文档生成
codex "为新增的API生成文档"

# 知识传递
codex "解释这个复杂模块的工作原理"
```

## 七、注意事项

### ⚠️ 安全考虑
1. **代码审查**: 自动生成的代码要审查
2. **权限控制**: 不要在生产环境运行
3. **敏感信息**: 避免暴露密钥和密码
4. **备份代码**: 重要修改前先备份

### 🚫 使用限制
1. **平台支持**: macOS和Linux原生支持，Windows需要WSL
2. **Node版本**: 需要Node.js 16+
3. **网络要求**: 需要访问OpenRouter API
4. **Token限制**: 单次请求不超过上下文限制

### ✅ 最佳实践
1. **明确指令**: 提供清晰具体的任务描述
2. **逐步执行**: 复杂任务分步骤进行
3. **版本控制**: 使用Git跟踪所有变更
4. **测试验证**: 修改后运行测试确保正确
5. **文档同步**: 代码变更同步更新文档

## 八、故障排查

### 问题1：安装失败
```bash
# 检查Node版本
node --version  # 需要16+

# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com
npm install -g @openai/codex
```

### 问题2：连接失败
```bash
# 检查环境变量
echo $OPENROUTER_API_KEY

# 测试API连接
curl -H "Authorization: Bearer $OPENROUTER_API_KEY" \
     https://openrouter.ai/api/v1/models
```

### 问题3：配置不生效
```bash
# 检查配置文件
cat ~/.codex/config.json

# 删除缓存重试
rm -rf ~/.codex/cache
```

## 九、对比总结

### 🎯 什么时候用Codex CLI？
- ✅ 需要生成新代码
- ✅ 需要修复Bug
- ✅ 需要重构代码
- ✅ 需要执行命令
- ✅ 需要自动化操作

### 🔍 什么时候用Gemini CLI？
- ✅ 需要深度分析
- ✅ 需要架构审查
- ✅ 需要安全扫描
- ✅ 需要性能分析
- ✅ 需要生成报告

### 🤝 协同使用示例
```bash
# 1. 先用Gemini分析
gemini -p "分析通知模块的性能瓶颈"

# 2. 再用Codex修复
codex "根据分析结果优化通知模块性能"

# 3. 最后验证
gemini -p "验证优化后的性能提升"
```

## 十、快速开始

```bash
# 1. 运行安装脚本
cd /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/
./setup-codex-cli.sh

# 2. 设置环境变量
export OPENROUTER_API_KEY="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"

# 3. 开始使用
codex "Hello, 请介绍你自己"

# 4. 实战任务
codex "分析项目中的三重Token认证实现，并提出改进建议"
```

---

**文档状态**: 📝 待实施  
**下一步**: 运行`setup-codex-cli.sh`完成安装  
**维护者**: Claude Code AI  
**更新时间**: 2025-09-06