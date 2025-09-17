# 全面后端API测试计划

## 🎯 测试概述
**创建时间**: 2025-09-15
**基于**: Sequential Thinking深度分析 + scripts/notifications现有脚本整合
**目标**: 全面验证校园通知系统所有API功能、安全性和性能
**覆盖范围**: 主服务(48081) + Mock School API(48082) + 新实现的待办通知优化功能

## 📊 现有测试脚本分析

### ✅ 已有脚本覆盖范围
| 脚本名称 | 测试类型 | 覆盖功能 | 优先级 |
|----------|----------|----------|--------|
| `api_test_fixed.sh` | 基础API连通性 | JWT+CSRF双重认证流程 | P0 |
| `test_roles.sh` | 权限验证 | 6种角色权限矩阵测试 | P0 |
| `publish_level1-4_*.sh` | 业务功能 | Level 1-4通知发布完整流程 | P1 |
| `publish_todo_notification.sh` | 新功能验证 | 待办通知优化功能 | P1 |
| `cache_clear_utils.sh` | 性能优化 | 权限缓存系统验证 | P2 |

### 🚨 识别的测试空白
1. **并发性测试** - 缺少多用户同时访问压力测试
2. **安全漏洞测试** - 缺少SQL注入、XSS等攻击测试
3. **异常恢复测试** - 缺少服务异常情况下的恢复能力测试
4. **数据完整性测试** - 缺少数据库事务和一致性验证

## 🏗️ 测试架构设计

### 测试分层策略
```
P0: 基础连通性测试 (阻塞性测试)
├── 服务启动验证
├── API端点可达性
└── 基础认证流程

P1: 权限和业务逻辑测试 (核心功能)
├── 角色权限矩阵验证
├── 通知发布业务流程
└── 待办通知优化功能

P2: 性能和集成测试 (稳定性)
├── 权限缓存系统性能
├── 并发访问压力测试
└── 系统集成完整性

P3: 安全和边界测试 (健壮性)
├── 安全漏洞扫描
├── 异常输入处理
└── 攻击防护验证

P4: 回归和持续监控 (维护性)
├── 功能回归验证
├── 监控指标收集
└── 故障恢复能力
```

## 📋 详细测试计划

### 🔥 P0级: 基础连通性测试 (阻塞性 - 必须100%通过)

#### P0.1 服务健康检查
```bash
# 使用现有脚本 + 自定义验证
./api_test_fixed.sh  # 验证48081+48082服务状态
```

**补充测试脚本**: `P0_service_health_check.sh`
```bash
#!/bin/bash
# 服务健康检查脚本
# 验证所有端口和基础API响应时间

echo "🏥 P0级 - 服务健康检查"

# 端口连通性检查
check_port() {
    local port=$1
    local service=$2
    if nc -z localhost $port; then
        echo "✅ $service ($port) - 连通正常"
        return 0
    else
        echo "❌ $service ($port) - 连通失败"
        return 1
    fi
}

check_port 48081 "主通知服务"
check_port 48082 "Mock School API"

# API响应时间检查
check_response_time() {
    local url=$1
    local name=$2
    local max_time=$3

    response_time=$(curl -o /dev/null -s -w "%{time_total}" $url)
    if (( $(echo "$response_time < $max_time" | bc -l) )); then
        echo "✅ $name 响应时间: ${response_time}s (< ${max_time}s)"
        return 0
    else
        echo "❌ $name 响应时间过长: ${response_time}s (> ${max_time}s)"
        return 1
    fi
}

check_response_time "http://localhost:48081/csrf-token" "CSRF Token API" 2.0
check_response_time "http://localhost:48082/mock-school-api/ping" "Mock School API" 2.0
```

#### P0.2 认证流程完整性验证
```bash
# 使用现有脚本验证
./api_test_fixed.sh  # JWT+CSRF双重认证完整流程
```

### 🎯 P1级: 权限和业务逻辑测试 (核心功能验证)

#### P1.1 权限矩阵完整验证
```bash
# 使用现有脚本
./test_roles.sh  # 6种角色权限边界测试
```

**补充测试**: `P1_permission_boundary_test.sh`
```bash
#!/bin/bash
# 权限边界测试 - 验证越权访问防护
# 测试学生尝试发布Level 1通知等越权操作

echo "🛡️ P1级 - 权限边界测试"

test_unauthorized_access() {
    local role=$1
    local forbidden_level=$2
    local expected_code=$3

    echo "测试 $role 尝试发布 Level $forbidden_level 通知..."

    # 认证为指定角色
    AUTH_RESPONSE=$(curl -s -X POST "http://localhost:48082/mock-school-api/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\": \"${role}\", \"name\": \"Test User\", \"password\": \"admin123\"}")

    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.data.accessToken')

    # 获取CSRF Token
    CSRF_TOKEN=$(curl -s "http://localhost:48081/csrf-token")

    # 尝试越权发布
    RESPONSE=$(curl -s -w "%{http_code}" -X POST "http://localhost:48081/admin-api/test/notification/api/publish-database" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
        -d "{\"title\": \"越权测试\", \"level\": $forbidden_level, \"targetScope\": \"SCHOOL_WIDE\"}")

    HTTP_CODE="${RESPONSE: -3}"
    if [ "$HTTP_CODE" = "$expected_code" ]; then
        echo "✅ 越权防护成功 - 返回 $HTTP_CODE"
    else
        echo "❌ 越权防护失败 - 预期 $expected_code，实际 $HTTP_CODE"
    fi
}

# 测试学生越权发布Level 1通知
test_unauthorized_access "STUDENT_001" 1 "403"

# 测试教师越权发布Level 1通知
test_unauthorized_access "TEACHER_001" 1 "403"
```

#### P1.2 业务流程完整性测试
```bash
# 使用现有脚本验证各级别通知发布
./publish_level1_emergency.sh      # Level 1 紧急通知
./publish_level2_important.sh       # Level 2 重要通知
./publish_level3_regular.sh         # Level 3 常规通知
./publish_level4_complete_fixed.sh  # Level 4 提醒通知

# 新功能测试
./publish_todo_notification.sh      # 待办通知优化功能
```

#### P1.3 待办通知优化功能专项测试
**新增测试脚本**: `P1_todo_optimization_test.sh`
```bash
#!/bin/bash
# 待办通知优化功能完整性测试
# 验证用户状态隔离、乐观锁、性能优化等核心功能

echo "📝 P1级 - 待办通知优化功能测试"

# 测试用户状态隔离
test_user_isolation() {
    echo "🔒 测试用户状态隔离..."

    # 学生A完成待办
    # 学生B查看待办列表，应该看不到A的完成状态
    # 验证tenant_id + user_id双重隔离机制
}

# 测试乐观锁机制
test_optimistic_locking() {
    echo "🔄 测试乐观锁机制..."

    # 模拟并发更新同一待办状态
    # 验证version字段乐观锁是否防止数据冲突
}

# 测试性能优化效果
test_performance_improvement() {
    echo "⚡ 测试性能优化效果..."

    # 对比优化前后的响应时间
    # 验证30-40%性能提升是否达成
}

test_user_isolation
test_optimistic_locking
test_performance_improvement
```

### 🚀 P2级: 性能和集成测试 (稳定性验证)

#### P2.1 权限缓存系统性能测试
```bash
# 使用现有缓存工具
source ./cache_clear_utils.sh
test_cache_performance  # 验证权限缓存系统30-40%性能提升
```

#### P2.2 并发压力测试
**新增测试脚本**: `P2_concurrent_load_test.sh`
```bash
#!/bin/bash
# 并发压力测试
# 验证5000+ QPS并发处理能力

echo "🔥 P2级 - 并发压力测试"

# 并发用户登录测试
concurrent_login_test() {
    echo "👥 并发登录压力测试..."

    local concurrent_users=50
    local requests_per_user=100

    for ((i=1; i<=concurrent_users; i++)); do
        {
            for ((j=1; j<=requests_per_user; j++)); do
                curl -s -X POST "http://localhost:48082/mock-school-api/auth/authenticate" \
                    -H "Content-Type: application/json" \
                    -d "{\"employeeId\": \"STUDENT_$(printf %03d $i)\", \"name\": \"Student-$i\", \"password\": \"admin123\"}" \
                    > /dev/null
            done
        } &
    done

    wait  # 等待所有后台任务完成
    echo "✅ 并发登录测试完成: ${concurrent_users} 用户 x ${requests_per_user} 请求"
}

# 并发通知查询测试
concurrent_query_test() {
    echo "📋 并发查询压力测试..."

    # 实现并发查询通知列表的压力测试
    # 验证系统在高并发查询下的响应能力
}

concurrent_login_test
concurrent_query_test
```

### 🛡️ P3级: 安全和边界测试 (健壮性验证)

#### P3.1 安全漏洞扫描
**新增测试脚本**: `P3_security_vulnerability_test.sh`
```bash
#!/bin/bash
# 安全漏洞扫描测试
# 验证SQL注入、XSS、CSRF等攻击防护

echo "🛡️ P3级 - 安全漏洞扫描"

# SQL注入攻击测试
test_sql_injection() {
    echo "💉 SQL注入攻击测试..."

    # 测试各种SQL注入payload
    local payloads=(
        "'; DROP TABLE notification_info; --"
        "' UNION SELECT password FROM users --"
        "1' OR '1'='1"
    )

    for payload in "${payloads[@]}"; do
        RESPONSE=$(curl -s -X POST "http://localhost:48081/admin-api/test/notification/api/publish-database" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer test-token" \
            -H "tenant-id: 1" \
            -d "{\"title\": \"$payload\", \"content\": \"test\"}")

        if [[ $RESPONSE == *"error"* ]] || [[ $RESPONSE == *"500"* ]]; then
            echo "✅ SQL注入防护生效: $payload"
        else
            echo "❌ SQL注入防护失败: $payload"
        fi
    done
}

# XSS攻击测试
test_xss_attack() {
    echo "🎭 XSS攻击测试..."

    local xss_payloads=(
        "<script>alert('XSS')</script>"
        "javascript:alert('XSS')"
        "<img src=x onerror=alert('XSS')>"
    )

    # 测试XSS payload是否被正确过滤和转义
}

# CSRF攻击测试
test_csrf_attack() {
    echo "🔒 CSRF攻击测试..."

    # 测试无CSRF Token的请求是否被拒绝
    # 测试错误CSRF Token的请求是否被拒绝
}

test_sql_injection
test_xss_attack
test_csrf_attack
```

#### P3.2 异常输入边界测试
**新增测试脚本**: `P3_boundary_input_test.sh`
```bash
#!/bin/bash
# 边界输入测试
# 验证各种异常输入的处理能力

echo "🎯 P3级 - 边界输入测试"

# 超长输入测试
test_oversized_input() {
    echo "📏 超长输入测试..."

    # 生成超长标题(>200字符)和内容(>2000字符)
    local long_title=$(printf 'A%.0s' {1..300})  # 300字符标题
    local long_content=$(printf 'B%.0s' {1..3000})  # 3000字符内容

    # 测试API是否正确拒绝超长输入
}

# 特殊字符输入测试
test_special_characters() {
    echo "🔣 特殊字符输入测试..."

    local special_chars=(
        "emoji: 🔥🎯📋✅❌"
        "unicode: ℃°±×÷"
        "symbols: !@#$%^&*()_+-=[]{}|;:,.<>?"
        "chinese: 测试中文字符处理"
    )

    # 测试各种特殊字符的处理能力
}

# NULL和空值测试
test_null_empty_values() {
    echo "∅ NULL和空值测试..."

    # 测试各种NULL、空字符串、空对象的处理
}

test_oversized_input
test_special_characters
test_null_empty_values
```

### 🔄 P4级: 回归和持续监控 (维护性验证)

#### P4.1 功能回归测试套件
**新增测试脚本**: `P4_regression_test_suite.sh`
```bash
#!/bin/bash
# 功能回归测试套件
# 执行所有核心功能的快速验证

echo "🔄 P4级 - 功能回归测试套件"

# 执行所有P0-P3测试的快速版本
echo "执行P0基础测试..."
./P0_service_health_check.sh --quick

echo "执行P1核心功能测试..."
./test_roles.sh --quick

echo "执行P2性能测试..."
./P2_concurrent_load_test.sh --light

echo "执行P3安全测试..."
./P3_security_vulnerability_test.sh --essential

echo "🎉 回归测试套件执行完成"
```

#### P4.2 监控指标收集
**新增测试脚本**: `P4_monitoring_metrics.sh`
```bash
#!/bin/bash
# 监控指标收集
# 收集系统性能和健康状况指标

echo "📊 P4级 - 监控指标收集"

collect_performance_metrics() {
    echo "⚡ 收集性能指标..."

    # API响应时间统计
    # 权限缓存命中率
    # 数据库连接池状态
    # 内存和CPU使用情况
}

collect_business_metrics() {
    echo "📈 收集业务指标..."

    # 通知发布成功率
    # 用户活跃度统计
    # 待办完成率统计
    # 错误率统计
}

generate_health_report() {
    echo "📋 生成健康状况报告..."

    # 生成JSON格式的系统健康报告
    # 包含所有关键指标和建议
}

collect_performance_metrics
collect_business_metrics
generate_health_report
```

## 🚀 测试执行策略

### 执行顺序和依赖关系
```
Phase 1: P0基础测试 (阻塞性)
├── 必须100%通过才能继续
└── 失败立即停止后续测试

Phase 2: P1+P2并行执行 (核心功能+性能)
├── P1: 核心业务逻辑验证
└── P2: 性能和并发测试

Phase 3: P3安全测试 (独立执行)
├── 安全漏洞扫描
└── 边界条件测试

Phase 4: P4持续监控 (定期执行)
├── 回归测试套件
└── 监控指标收集
```

### 一键执行脚本
**主控脚本**: `run_comprehensive_api_tests.sh`
```bash
#!/bin/bash
# 全面API测试主控脚本

echo "🚀 启动全面后端API测试计划"
echo "========================================="

# 测试环境检查
echo "🔍 测试环境检查..."
if ! ./P0_service_health_check.sh; then
    echo "❌ P0基础测试失败，停止执行"
    exit 1
fi

# 执行核心功能测试
echo "🎯 执行P1核心功能测试..."
./test_roles.sh
./publish_todo_notification.sh
./P1_todo_optimization_test.sh

# 执行性能测试
echo "⚡ 执行P2性能测试..."
./P2_concurrent_load_test.sh

# 执行安全测试
echo "🛡️ 执行P3安全测试..."
./P3_security_vulnerability_test.sh
./P3_boundary_input_test.sh

# 生成测试报告
echo "📊 生成测试报告..."
./P4_monitoring_metrics.sh

echo "🎉 全面API测试执行完成"
```

## 📈 成功标准和验收条件

### 测试通过标准
| 测试级别 | 通过标准 | 关键指标 |
|----------|----------|----------|
| **P0基础测试** | 100%通过 | 所有API端点响应正常，认证流程无异常 |
| **P1功能测试** | ≥95%通过 | 核心业务功能正确，权限控制有效 |
| **P2性能测试** | ≥90%通过 | 并发5000+ QPS，响应时间<100ms |
| **P3安全测试** | ≥95%通过 | 无严重安全漏洞，边界处理正确 |
| **P4监控测试** | 持续执行 | 系统稳定性指标正常 |

### 关键性能指标(KPI)
- **认证性能**: JWT+CSRF双重认证<50ms
- **权限缓存**: 缓存命中率>90%，性能提升>30%
- **并发处理**: 支持5000+ QPS并发访问
- **错误率**: API调用错误率<1%
- **可用性**: 系统可用性>99.9%

## 🔧 测试工具和环境配置

### 必需工具依赖
```bash
# 基础工具
curl          # HTTP请求测试
jq            # JSON数据处理
bc            # 数值计算
netcat (nc)   # 端口连通性检查

# 性能测试工具
ab            # Apache Bench - HTTP性能测试
wrk           # 现代HTTP基准测试工具

# 安全测试工具
sqlmap        # SQL注入测试
nmap          # 端口扫描和安全检查
```

### 测试数据准备
```sql
-- 测试数据库初始化脚本
-- 插入测试用户和权限数据
INSERT INTO todo_completions (tenant_id, user_id, todo_id, status, version)
VALUES (1, 'TEST_USER_001', 1, 0, 1);

-- 插入测试通知数据
INSERT INTO notification_info (tenant_id, title, content, level, status, target_scope)
VALUES (1, '测试通知', '测试内容', 4, 3, 'SCHOOL_WIDE');
```

## 📋 测试报告和文档输出

### 自动化测试报告
- **实时日志**: 所有测试执行过程的详细日志
- **性能报告**: 响应时间、并发能力、资源使用统计
- **安全报告**: 漏洞扫描结果和风险评估
- **业务报告**: 功能完整性和用户体验验证

### 测试结果存储
```
documentation/test-results/
├── P0_basic_connectivity.log        # P0基础测试结果
├── P1_functional_business.log       # P1功能测试结果
├── P2_performance_load.log          # P2性能测试结果
├── P3_security_vulnerability.log    # P3安全测试结果
├── P4_monitoring_regression.log     # P4监控测试结果
└── comprehensive_test_summary.json  # 综合测试报告(JSON格式)
```

---

**📅 创建时间**: 2025年9月15日
**📝 创建者**: Claude Code AI Assistant
**🎯 测试范围**: 校园通知系统全栈API (主服务48081 + Mock API 48082)
**🚀 执行方式**: 结合现有scripts/notifications脚本 + 新增补充测试脚本
**💎 核心价值**: 确保待办通知组件优化功能的稳定性、安全性和高性能