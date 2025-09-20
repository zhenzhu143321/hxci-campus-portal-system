# 🚀 AI工具使用指南 (Gemini CLI + CX)

## ⚠️ 重要：Claude实例必读

### 🔥 正确调用方式（直接复制使用）
```bash
# Gemini CLI - 必须在项目根目录执行
Bash(/opt/hxci-campus-portal/hxci-campus-portal-system/gm "分析项目架构，扫描src目录下所有Controller类的安全风险") timeout: 20m

# CX命令 - GPT-5较慢，需长超时
Bash(cx "分析TempNotificationController的权限验证逻辑，检查第156行是否存在越权风险") timeout: 15m
```

### 🚨 核心铁律
1. **Gemini/CX无状态**: 每次调用都是新实例，必须提供完整上下文
2. **超时设置**: timeout参数放在Bash()括号外，最少10分钟
3. **工作目录**: 必须在项目根目录执行才能读取代码

## 🔧 工具配置状态

### ✅ Gemini CLI (通过OpenRouter代理)
- **代理服务**: `http://127.0.0.1:8888` (运行中)
- **API密钥**: 已配置新密钥 (2025-09-17更新)
- **模型**: google/gemini-2.5-pro

### ✅ CX命令 (OpenRouter直连)
- **默认模型**: openai/gpt-5 (最强编程能力)
- **备选模型**: gpt-4o, gemini-2.5-flash
- **API密钥**: 已内置配置

---

## 🤖 Gemini CLI 完整调用示例

### Claude实例专用命令（带完整路径和环境变量）
```bash
# 标准调用格式 - 必须复制完整命令
Bash(cd /opt/hxci-campus-portal/hxci-campus-portal-system && GOOGLE_GEMINI_BASE_URL='http://127.0.0.1:8888' GEMINI_API_KEY='test' gemini -p "分析yudao-boot-mini目录下的Controller类，找出潜在的SQL注入风险点，需要扫描所有@RestController注解的类") timeout: 20m

# 使用gm快捷命令（已内置环境变量）
Bash(/opt/hxci-campus-portal/hxci-campus-portal-system/gm "扫描notification相关的所有Java文件，分析权限控制实现是否完善") timeout: 20m
```

### 人类用户快速使用
```bash
# 在项目根目录执行
cd /opt/hxci-campus-portal/hxci-campus-portal-system
gm "你的问题"
```

### 代理服务管理
```bash
# 检查状态
cd /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/
./start-gemini-proxy.sh status

# 重启服务（如果需要）
./start-gemini-proxy.sh restart

# 查看日志
tail -f /tmp/gemini-proxy.log
```

---

## 💡 CX命令完整调用示例

### Claude实例专用命令（GPT-5需要长超时）
```bash
# 标准GPT-5调用 - 默认模型，响应较慢
Bash(cd /opt/hxci-campus-portal/hxci-campus-portal-system && cx "分析TempNotificationController.java的publishToDatabase方法，检查SQL注入风险，特别关注第231-245行的参数处理逻辑") timeout: 15m

# 使用GPT-4o加速响应
Bash(cd /opt/hxci-campus-portal/hxci-campus-portal-system && cx -m openai/gpt-4o "快速分析PermissionCacheConfig的Redis配置是否合理") timeout: 10m

# 全局cx命令（如果已配置软链接）
Bash(cx "分析JWT Token验证逻辑，检查是否存在绕过风险，重点关注GlobalAuthenticationConfig类") timeout: 15m
```

### 人类用户快速使用
```bash
# 任意目录执行（全局命令）
cx "你的问题"  # 默认GPT-5
cx -m openai/gpt-4o "快速问题"  # 指定模型
```

### 查看帮助
```bash
cx --help  # 显示所有选项和示例
```

---

## 🎯 Claude实例最佳实践

### 1️⃣ 标准工作流（复制使用）
```bash
# 步骤1: Gemini扫描问题
Bash(/opt/hxci-campus-portal/hxci-campus-portal-system/gm "扫描yudao-boot-mini/yudao-server/src目录，找出所有安全漏洞，包括SQL注入、XSS、CSRF等") timeout: 20m

# 步骤2: CX提供修复方案
Bash(cx "针对TempNotificationController的SQL注入风险，提供具体的参数化查询修复代码") timeout: 15m

# 步骤3: Claude实施修改
# (根据建议修改文件)

# 步骤4: Gemini验证效果
Bash(/opt/hxci-campus-portal/hxci-campus-portal-system/gm "验证TempNotificationController的SQL注入问题是否已修复") timeout: 20m
```

### 2️⃣ 提问要点
- **提供文件路径**: "分析TempNotificationController.java"
- **指定行号范围**: "检查第156-180行"
- **说明具体问题**: "SQL注入风险"、"权限验证逻辑"
- **给足上下文**: "这是通知发布API，需要验证用户权限"

---

## 🔧 故障排除

### Gemini CLI问题
```bash
# 错误: "Model stream completed without any chunks"
# 解决: 检查代理服务
./start-gemini-proxy.sh status
./start-gemini-proxy.sh restart

# 错误: 404 Not Found
# 解决: 设置环境变量
export GOOGLE_GEMINI_BASE_URL='http://127.0.0.1:8888'
export GENINI_API_KEY='test'
```

### CX命令问题
```bash
# 错误: 获取响应失败
# 解决: 检查网络连接
curl -I https://openrouter.ai

# 错误: cx command not found
# 解决: 检查软链接
ls -la /usr/local/bin/cx
sudo ln -sf /opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration/cx /usr/local/bin/cx
```

---

## 📊 性能对比

| 模型 | 速度 | 能力 | 适用场景 |
|------|------|------|----------|
| **openai/gpt-5** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 复杂编程问题 |
| **openai/gpt-4o** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 快速代码分析 |
| **google/gemini-2.5-pro** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 架构设计 |
| **google/gemini-2.5-flash** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 简单查询 |

---

## 🚨 重要提醒

1. **API密钥安全**:
   - 当前密钥: `sk-or-v1-aafe6ee...` (截断显示)
   - 不要在公开代码中暴露完整密钥

2. **代理服务依赖**:
   - Gemini CLI必须通过代理服务使用
   - 代理服务崩溃时使用cx作为备份

3. **Token限制**:
   - 单次请求避免超过10k tokens
   - 复杂问题分解为多个小问题

4. **最新配置**:
   - 更新时间: 2025-09-17
   - 配置文件: `/scripts/ai-collaboration/cx`
   - 代理脚本: `/scripts/ai-collaboration/start-gemini-proxy.sh`

---

## 📝 Claude实例速查表

### 直接复制的完整命令
```bash
# Gemini扫描
Bash(/opt/hxci-campus-portal/hxci-campus-portal-system/gm "扫描问题描述") timeout: 20m

# CX分析（GPT-5慢）
Bash(cx "具体问题描述") timeout: 15m

# CX快速分析（GPT-4o）
Bash(cx -m openai/gpt-4o "问题描述") timeout: 10m

# 带环境变量的完整Gemini命令
Bash(cd /opt/hxci-campus-portal/hxci-campus-portal-system && GOOGLE_GEMINI_BASE_URL='http://127.0.0.1:8888' GEMINI_API_KEY='test' gemini -p "详细问题描述") timeout: 20m
```

### 记住要点
- **timeout放括号外**: `Bash(命令) timeout: 20m` ✅
- **GPT-5需15分钟+**: 默认模型响应慢
- **必须给足上下文**: 每次都是新实例，无记忆
- **在项目根目录执行**: 否则无法读取代码

---

**维护者**: Claude Code AI | **最后更新**: 2025-09-17 | **状态**: ✅ 所有工具正常工作