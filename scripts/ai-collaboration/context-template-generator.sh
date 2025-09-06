#!/bin/bash
# 代码上下文模板生成器 - 为不同场景生成标准化的AI调用模板
# 确保所有智能体使用统一格式传递代码上下文

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# 配置
PROJECT_ROOT="/opt/hxci-campus-portal/hxci-campus-portal-system"
TEMPLATE_DIR="$PROJECT_ROOT/scripts/ai-collaboration/templates"

# 确保模板目录存在
mkdir -p "$TEMPLATE_DIR"

# 生成学校API集成模板
generate_school_api_template() {
    cat > "$TEMPLATE_DIR/school-api-integration.md" << 'EOF'
# 学校API集成 - AI协作模板

## 项目背景
- **项目**: 哈尔滨信息工程学院校园门户系统
- **技术栈**: Spring Boot 3.4.5 + Vue 3
- **当前任务**: 学校API集成 - Basic Token到JWT Token适配

## 核心代码文件

### 1. MockAuthController.java (完整代码)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/controller/MockAuthController.java

```java
[请使用 ai-context-collector.sh 自动收集此文件的完整内容]
```

### 2. SecurityTokenService.java (完整代码)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/SecurityTokenService.java

```java
[请使用 ai-context-collector.sh 自动收集此文件的完整内容]
```

### 3. UserInfo.java (DTO完整代码)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/dto/UserInfo.java

```java
[请使用 ai-context-collector.sh 自动收集此文件的完整内容]
```

### 4. application-local.yaml (配置片段)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/resources/application-local.yaml

```yaml
# 学校API相关配置
school:
  api:
    mode: ${SCHOOL_API_MODE:mock}  # mock 或 real
    real-endpoint: https://work.greathiit.com/api/user/loginWai
    mock-endpoint: http://localhost:48082/mock-school-api
    timeout: 5000
    retry-times: 3
```

## 真实学校API信息

### API端点
- URL: `https://work.greathiit.com/api/user/loginWai`
- Method: POST
- Content-Type: application/json

### 请求格式
```json
{
  "userNumber": "工号",
  "password": "密码",
  "autoLogin": true,
  "provider": "account"
}
```

### 响应格式
```json
{
  "success": true,
  "data": {
    "token": "uuid-format-basic-token",
    "userInfo": {
      "name": "用户姓名",
      "userNumber": "工号",
      "role": ["student", "teacher"],
      "grade": "2023",
      "className": "计科1班",
      "department": "信息工程系"
    }
  }
}
```

## 需要实现的功能

### 1. Basic Token认证接口
```java
@PostMapping("/auth/basic-authenticate")
public MockApiResponse<Map<String, Object>> basicAuthenticate(@RequestBody Map<String, String> request) {
    // 1. 调用真实学校API获取Basic Token
    // 2. 缓存Basic Token与用户信息映射
    // 3. 生成系统JWT Token
    // 4. 返回JWT Token给前端
}
```

### 2. Token转换接口
```java
@PostMapping("/auth/convert-token")
public MockApiResponse<String> convertToken(@RequestBody Map<String, String> request) {
    // 1. 验证Basic Token有效性
    // 2. 从缓存获取用户信息
    // 3. 生成对应的JWT Token
    // 4. 返回JWT Token
}
```

### 3. 配置驱动切换
```java
@Value("${school.api.mode}")
private String apiMode;

private boolean isRealMode() {
    return "real".equalsIgnoreCase(apiMode);
}
```

## 具体问题

1. **Token兼容性问题**: Basic Token (UUID格式) vs JWT Token (JSON格式)
2. **用户信息映射**: 学校API返回的role数组需要映射到系统的单一角色
3. **缓存策略**: Basic Token缓存时间和更新机制
4. **降级策略**: 学校API不可用时的处理方案

## 期望输出

请提供：
1. 完整的Basic Token认证实现代码
2. Token缓存管理方案（Redis或内存）
3. 配置驱动的模式切换实现
4. 错误处理和降级策略
5. 前端调用示例代码

## 架构约束

- 必须保持现有Mock API完全可用（向后兼容）
- 不能修改现有JWT Token验证逻辑
- 必须支持配置文件切换（不需要改代码）
- 要考虑学校API的稳定性问题
EOF
    
    echo -e "${GREEN}✅ 学校API集成模板已生成${NC}"
}

# 生成Bug修复模板
generate_bug_fix_template() {
    cat > "$TEMPLATE_DIR/bug-fix-template.md" << 'EOF'
# Bug修复 - AI协作模板

## 项目背景
- **项目**: 哈尔滨信息工程学院校园门户系统
- **技术栈**: Spring Boot 3.4.5 + Vue 3
- **Bug类型**: [请填写: API错误/前端显示/权限问题/性能问题]

## Bug描述

### 现象
[详细描述用户看到的错误现象]

### 复现步骤
1. [步骤1]
2. [步骤2]
3. [步骤3]

### 期望行为
[描述正确的行为应该是什么]

## 相关代码（完整文件）

### 1. 出错的主文件
文件路径: [完整路径]

```[语言]
[使用 ai-context-collector.sh 收集的完整代码]
```

### 2. 相关依赖文件
文件路径: [完整路径]

```[语言]
[使用 ai-context-collector.sh 收集的完整代码]
```

## 错误日志

### Java后端日志
```
[完整的错误堆栈跟踪]
```

### 浏览器控制台
```javascript
[浏览器错误信息]
```

## 已尝试的解决方案

1. [方案1及结果]
2. [方案2及结果]

## 调试信息

### 请求参数
```json
[实际发送的请求数据]
```

### 响应数据
```json
[服务器返回的响应]
```

## 期望的修复方案

请提供：
1. 问题根因分析
2. 具体的代码修改方案
3. 修改后的完整代码
4. 测试验证方法
5. 防止类似问题的建议
EOF
    
    echo -e "${GREEN}✅ Bug修复模板已生成${NC}"
}

# 生成性能优化模板
generate_performance_template() {
    cat > "$TEMPLATE_DIR/performance-optimization.md" << 'EOF'
# 性能优化 - AI协作模板

## 项目背景
- **项目**: 哈尔滨信息工程学院校园门户系统
- **技术栈**: Spring Boot 3.4.5 + Vue 3
- **优化目标**: [请填写: API响应时间/并发处理/内存占用/数据库查询]

## 性能问题描述

### 当前性能指标
- 响应时间: [具体数值]
- 并发能力: [QPS数值]
- 资源占用: [CPU/内存使用率]

### 目标性能指标
- 响应时间: [目标数值]
- 并发能力: [目标QPS]
- 资源占用: [目标使用率]

## 相关代码（完整实现）

### 1. 需要优化的核心代码
文件路径: [完整路径]

```java
[使用 ai-context-collector.sh 收集的完整代码]
```

### 2. 数据库查询相关
```sql
-- 当前的SQL查询
[实际SQL语句]

-- 执行计划
[EXPLAIN结果]
```

### 3. 配置文件
```yaml
# 当前配置
[相关配置内容]
```

## 性能分析数据

### JVM监控数据
```
堆内存使用: [数值]
GC频率: [数值]
线程数: [数值]
```

### 数据库性能
```
慢查询日志: [相关日志]
索引使用情况: [分析结果]
```

## 期望的优化方案

请提供：
1. 性能瓶颈分析
2. 具体的优化策略
3. 优化后的代码实现
4. 性能测试方案
5. 监控指标建议
EOF
    
    echo -e "${GREEN}✅ 性能优化模板已生成${NC}"
}

# 生成代码审查模板
generate_review_template() {
    cat > "$TEMPLATE_DIR/code-review-template.md" << 'EOF'
# 代码审查 - AI协作模板

## 项目背景
- **项目**: 哈尔滨信息工程学院校园门户系统
- **技术栈**: Spring Boot 3.4.5 + Vue 3
- **审查类型**: [新功能/重构/安全审查/架构审查]

## 待审查代码（完整文件）

### 1. 主要变更文件
文件路径: [完整路径]
变更类型: [新增/修改/删除]

```[语言]
[使用 ai-context-collector.sh 收集的完整代码]
```

### 2. 相关上下文文件
文件路径: [完整路径]

```[语言]
[使用 ai-context-collector.sh 收集的完整代码]
```

## 变更说明

### 功能需求
[说明这次变更要实现的功能]

### 技术方案
[说明采用的技术方案和理由]

### 影响范围
- 影响的模块: [列表]
- 影响的API: [列表]
- 数据库变更: [有/无]

## 测试覆盖

### 单元测试
```java
[相关的测试代码]
```

### 集成测试方案
[测试步骤和预期结果]

## 审查重点

请重点审查：
1. 代码质量和规范性
2. 安全性考虑
3. 性能影响
4. 架构合理性
5. 错误处理完整性

## 期望的审查反馈

请提供：
1. 代码质量评分（1-10）
2. 发现的问题和风险
3. 改进建议（具体到代码行）
4. 最佳实践建议
5. 安全性评估
EOF
    
    echo -e "${GREEN}✅ 代码审查模板已生成${NC}"
}

# 生成使用指南
generate_usage_guide() {
    cat > "$TEMPLATE_DIR/USAGE_GUIDE.md" << 'EOF'
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
EOF
    
    echo -e "${GREEN}✅ 使用指南已生成${NC}"
}

# 显示菜单
show_menu() {
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║          AI协作代码上下文模板生成器                         ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "请选择要生成的模板："
    echo "1) 学校API集成模板"
    echo "2) Bug修复模板"
    echo "3) 性能优化模板"
    echo "4) 代码审查模板"
    echo "5) 生成所有模板"
    echo "6) 查看使用指南"
    echo "0) 退出"
    echo ""
}

# 主函数
main() {
    if [[ $# -gt 0 ]]; then
        # 命令行参数模式
        case "$1" in
            school-api)
                generate_school_api_template
                ;;
            bug-fix)
                generate_bug_fix_template
                ;;
            performance)
                generate_performance_template
                ;;
            review)
                generate_review_template
                ;;
            all)
                generate_school_api_template
                generate_bug_fix_template
                generate_performance_template
                generate_review_template
                generate_usage_guide
                echo -e "${GREEN}✅ 所有模板已生成完成!${NC}"
                ;;
            *)
                echo "用法: $0 [school-api|bug-fix|performance|review|all]"
                exit 1
                ;;
        esac
    else
        # 交互模式
        while true; do
            show_menu
            read -p "请选择 (0-6): " choice
            
            case $choice in
                1)
                    generate_school_api_template
                    ;;
                2)
                    generate_bug_fix_template
                    ;;
                3)
                    generate_performance_template
                    ;;
                4)
                    generate_review_template
                    ;;
                5)
                    generate_school_api_template
                    generate_bug_fix_template
                    generate_performance_template
                    generate_review_template
                    generate_usage_guide
                    echo -e "${GREEN}✅ 所有模板已生成完成!${NC}"
                    ;;
                6)
                    generate_usage_guide
                    echo ""
                    echo -e "${YELLOW}使用指南已生成，请查看:${NC}"
                    echo "$TEMPLATE_DIR/USAGE_GUIDE.md"
                    ;;
                0)
                    echo "退出..."
                    exit 0
                    ;;
                *)
                    echo -e "${YELLOW}无效选择，请重试${NC}"
                    ;;
            esac
            
            echo ""
            read -p "按Enter继续..."
        done
    fi
    
    echo ""
    echo -e "${MAGENTA}模板文件位置: $TEMPLATE_DIR${NC}"
}

# 执行
main "$@"