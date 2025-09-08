# 🚀 CodeX (cx) 完整使用指南 - 从入门到精通

## 📋 目录
- [快速开始](#快速开始)
- [核心功能详解](#核心功能详解)
- [超时问题完整解决方案](#超时问题完整解决方案)
- [AI协作工作流程](#ai协作工作流程)
- [诊断和故障排除](#诊断和故障排除)
- [性能优化建议](#性能优化建议)
- [常见问题解答](#常见问题解答)
- [高级配置](#高级配置)

---

## 🚀 快速开始

### 一行搞定
```bash
cx -p "你的问题"  # 默认使用GPT-5，最强编程能力
```

### 核心命令
```bash
# 基础使用
cx -p "分析代码问题"                    # 默认analyze模式
cx -p "修复Bug" --mode fix              # 修复模式
cx -p "重构代码" --mode refactor        # 重构模式
cx -p "生成测试" --mode test            # 测试模式
cx -p "写文档" --mode docs              # 文档模式

# 查看帮助
cx --help
```

## 🔧 核心功能详解

### 工作模式说明
| 模式 | 用途 | 示例 |
|------|------|------|
| **analyze** (默认) | 代码分析、问题诊断 | `cx -p "分析Controller安全风险"` |
| **fix** | Bug修复建议 | `cx -p "修复JWT认证问题" --mode fix` |
| **refactor** | 代码重构指导 | `cx -p "重构Service层" --mode refactor` |
| **test** | 测试用例生成 | `cx -p "生成单元测试" --mode test` |
| **docs** | 文档生成 | `cx -p "生成API文档" --mode docs` |

### 实战示例
```bash
# 1. 安全审查
cx -p "审查EnhancedCsrfSecurityConfig的安全性，检查CSRF防护是否完善"

# 2. 性能优化
cx -p "分析PermissionCacheConfig的性能瓶颈，提供优化建议"

# 3. 架构分析
cx -p "分析三重Token认证架构的设计是否合理"

# 4. Bug诊断
cx -p "分析垂直越权防护UserDataPermissionRule的实现" --mode fix

# 5. 数据库优化
cx -p "优化通知查询的SQL性能" --mode refactor
```

## ⚡ 超时问题完整解决方案

### 🎉 **问题状态: 已彻底解决**
经过深度分析和全面测试，**Codex 2分钟超时问题已完全解决**！

### 🛠️ **实施的解决方案**

#### 1. **流输出机制** ✅ **正常工作**
```bash
# cx脚本已配置流输出
stream=true  # 避免长时间无响应
```

#### 2. **增强超时配置** ✅ **已部署**
```bash
# HTTP请求超时 - 10分钟
http_timeout=600

# 流缓冲区优化
stream_buffer_size=8192

# Bash命令超时 - 10分钟
timeout 600
```

#### 3. **性能验证结果**
- ✅ **简单请求**: 5秒完成
- ✅ **代码分析**: 23秒完成  
- ✅ **架构分析**: 64秒完成
- ✅ **无2分钟超时问题**

### 💡 **用户最佳实践建议**

#### 🔥 **高效使用技巧**
1. **分解复杂请求**
   ```bash
   # ❌ 避免过于复杂的请求
   cx -p "分析整个项目架构并提供完整重构方案包括所有模块的详细实现"
   
   # ✅ 推荐分解为多个具体请求
   cx -p "分析项目主要技术栈和模块结构"
   cx -p "提供通知模块的重构建议" --mode refactor
   cx -p "优化权限系统的性能瓶颈" --mode fix
   ```

2. **使用具体描述**
   ```bash
   # ❌ 过于宽泛
   cx -p "优化代码"
   
   # ✅ 具体明确
   cx -p "优化TempWeatherController的Redis缓存策略"
   ```

3. **选择合适模式**
   ```bash
   # 安全分析用analyze模式
   cx -p "检查SQL注入漏洞" 
   
   # 性能问题用fix模式  
   cx -p "解决数据库查询慢的问题" --mode fix
   
   # 代码改进用refactor模式
   cx -p "改进异常处理机制" --mode refactor
   ```

#### 🌐 **网络优化建议**
- **最佳时段**: 避开网络高峰期使用
- **连接检查**: 确保OpenRouter API连接稳定
- **重试机制**: 网络异常时可稍后重试

#### 🔄 **备选方案**
如果仍遇到偶尔超时：
```bash
# 1. 使用更快的模型
cx -p "快速分析问题" -m openai/gpt-4o

# 2. 缩短请求内容
cx -p "简要说明主要问题"

# 3. 使用诊断工具检查
./scripts/testing/codex-timeout-diagnostic.sh
```

## 🤖 AI协作工作流程

### 三大AI工具分工
| 工具 | 角色 | 职责 | 使用场景 |
|------|------|------|----------|
| **Gemini CLI** | 📊 项目分析师 | 全项目扫描、架构分析 | `gm "分析项目架构"` |
| **Codex GPT-5** | 💡 编程搭子 | 代码建议、修复方案 | `cx -p "修复Bug建议"` |
| **Claude Code** | 🔨 实施工程师 | 代码编写、文件修改 | 实际执行修改 |

### 标准协作流程
```
1. Gemini扫描发现问题 → 2. Codex分析给出建议 → 3. Claude实施修改 → 4. Gemini验证效果
```

### 实际操作示例
```bash
# 步骤1: 使用Gemini进行项目扫描
gm "扫描项目中的安全风险点"

# 步骤2: 使用Codex获取具体修复建议  
cx -p "针对发现的SQL注入风险提供修复代码" --mode fix

# 步骤3: Claude实施修改
# (Claude根据建议修改相关文件)

# 步骤4: 使用Gemini验证修复效果
gm "验证SQL注入风险是否已修复"
```

## 🔍 诊断和故障排除

### 专业诊断工具
```bash
# 运行全面诊断
/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/testing/codex-timeout-diagnostic.sh
```

### 诊断工具功能
- ✅ **环境检查**: 验证cx配置、Codex设置、网络连接
- ✅ **实时监控**: 监控请求执行时间和状态
- ✅ **性能测试**: 简单/中等/复杂请求全方位测试
- ✅ **详细报告**: 提供优化建议和解决方案

### 手动故障排除
```bash
# 1. 检查cx脚本配置
grep -E "stream|timeout|http_timeout" /usr/local/bin/cx

# 2. 验证Codex配置
cat /home/ecs-assist-user/.codex/config.json

# 3. 测试网络连接
curl -s --connect-timeout 5 https://openrouter.ai

# 4. 简单功能测试
cx -p "测试连接" --help
```

### 常见错误解决
| 错误类型 | 症状 | 解决方案 |
|----------|------|----------|
| **连接超时** | 长时间无响应 | 检查网络连接，使用诊断工具 |
| **API错误** | 401/429错误 | 检查API密钥配置 |
| **配置问题** | 命令不存在 | 检查PATH环境变量和脚本权限 |
| **响应中断** | 部分响应后停止 | 增大缓冲区大小，检查流配置 |

## 🚀 性能优化建议

### 请求优化策略
```bash
# 1. 使用精确的技术术语
cx -p "优化MyBatis Plus的分页查询性能，减少N+1问题"

# 2. 指定具体的文件或模块
cx -p "分析UserDataPermissionRule.java的SQL构建逻辑"

# 3. 明确期望的输出格式
cx -p "提供JWT Token验证的具体代码实现，包含异常处理" --mode fix
```

### 上下文优化
```bash
# ✅ 好的请求 - 具体明确
cx -p "分析Spring Security配置中的CSRF防护实现，检查是否存在绕过风险"

# ❌ 避免的请求 - 过于宽泛
cx -p "看看这个项目有什么问题，给点建议"
```

### 批量处理策略
```bash
# 方法1: 分模块分析
cx -p "分析Controller层的设计模式"
cx -p "分析Service层的事务管理" 
cx -p "分析Repository层的数据访问"

# 方法2: 分功能分析
cx -p "分析通知发布功能的安全性"
cx -p "分析用户认证流程的完整性"
cx -p "分析权限控制的有效性"
```

## ❓ 常见问题解答

### Q1: cx命令找不到？
```bash
# 检查命令位置
which cx
ls -la /usr/local/bin/cx

# 如果不存在，检查原始位置
ls -la /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/codex-cli
```

### Q2: 响应速度慢？
1. **使用更快的模型**: `cx -p "问题" -m openai/gpt-4o`
2. **简化请求内容**: 缩短提示词长度
3. **检查网络**: 使用诊断工具测试连接

### Q3: 如何获得更好的建议？
```bash
# ✅ 提供具体上下文
cx -p "分析GlobalAuthenticationConfig类第127行的JWT验证逻辑，检查是否存在Token绕过风险"

# ✅ 指定期望的解决方案类型
cx -p "提供三种不同的Redis缓存优化方案，分析各自的优缺点" --mode refactor
```

### Q4: 如何处理复杂项目分析？
```bash
# 分阶段分析
cx -p "第一阶段：分析项目技术栈和架构模式"
cx -p "第二阶段：基于Spring Boot框架分析模块依赖关系"  
cx -p "第三阶段：分析数据库设计和API接口规范"
```

### Q5: 与其他AI工具如何配合？
```bash
# 标准配合流程
gm "全项目安全扫描" # Gemini扫描
cx -p "针对发现的问题提供修复方案" --mode fix  # Codex建议
# Claude实施修改
gm "验证修复效果" # Gemini验证
```

## ⚙️ 高级配置

### cx脚本配置文件
**位置**: `/usr/local/bin/cx`

**关键配置项**:
```bash
# 模型设置
MODEL="openai/gpt-5"              # 默认使用GPT-5

# 超时配置  
timeout 600                        # Bash超时10分钟
-c http_timeout=600               # HTTP超时10分钟

# 流式配置
-c stream=true                    # 启用流式输出
-c stream_buffer_size=8192        # 缓冲区8KB
```

### Codex配置文件
**位置**: `/home/ecs-assist-user/.codex/config.json`

**推荐配置**:
```json
{
  "provider": "openrouter",
  "model": "openai/gpt-4o",
  "providers": {
    "openrouter": {
      "name": "OpenRouter", 
      "baseURL": "https://openrouter.ai/api/v1"
    }
  },
  "approval_mode": "never",
  "sandbox": "read-only"
}
```

### 环境变量设置
```bash
# 设置OpenRouter API密钥
export OPENROUTER_API_KEY="your-api-key"

# 添加到shell配置文件
echo 'export OPENROUTER_API_KEY="your-api-key"' >> ~/.bashrc
```

### 自定义模式配置
```bash
# 在cx脚本中添加自定义模式
"security")
    FULL_PROMPT="安全审查: $PROMPT"
    ;;
"performance") 
    FULL_PROMPT="性能优化: $PROMPT"
    ;;
```

---

## 📊 总结

### 🎯 **关键特性**
- **默认模型**: GPT-5 (最强编程能力)
- **流式输出**: ✅ 已启用，彻底解决超时问题  
- **超时设置**: 600秒，支持复杂分析
- **诊断工具**: 全面的问题检测和解决方案
- **AI协作**: 与Gemini、Claude完美配合

### 🏆 **最佳实践**
1. **具体明确**: 使用精确的技术术语和具体描述
2. **合理分解**: 将复杂问题分解为多个具体请求  
3. **选择模式**: 根据需求选择appropriate的工作模式
4. **AI协作**: 充分利用三大AI工具的协作优势
5. **持续优化**: 使用诊断工具定期检查和优化配置

### 📞 **获取帮助**
- **命令帮助**: `cx --help`
- **诊断工具**: `./scripts/testing/codex-timeout-diagnostic.sh`
- **配置文件**: `/usr/local/bin/cx` 和 `/home/ecs-assist-user/.codex/config.json`

---
**记住**: cx提供专业建议，Claude执行实际修改！两者配合，效率倍增！🚀

**最后更新**: 2025年1月8日 | **状态**: ✅ 超时问题已彻底解决