# T18 API全面测试验证计划 - 详细执行方案

## 🎯 测试背景分析

### 系统当前状态
- **服务状态**: 主通知服务(48081)✅ + Mock School API(48082)✅ 正常运行
- **开发环境**: Linux系统迁移完成，UTF-8编码支持正常
- **P0权限缓存系统**: 已实施，性能提升66%，需验证实际效果
- **项目完成度**: 42%，核心功能实现，质量验证阶段

### 测试必要性评估
🚨 **关键原因**: Linux迁移过程中涉及大量核心代码修改：
- 文件路径系统 D:\ → /opt/ (全部更改)
- 命令行工具 PowerShell → Bash (语法差异)
- 编码处理 GBK/UTF-16 → UTF-8 (中文处理)
- 权限管理 Windows ACL → sudo/chmod (权限模型)

## 📋 测试范围和覆盖度

### 核心API接口清单 (15个主要接口)
```bash
# 主通知服务 (48081端口) - 9个核心接口
✅ POST /admin-api/test/notification/api/publish-database  # 发布通知
✅ GET  /admin-api/test/notification/api/list              # 通知列表  
✅ POST /admin-api/test/notification/api/approve           # 批准通知
✅ POST /admin-api/test/notification/api/reject            # 拒绝通知
✅ GET  /admin-api/test/notification/api/pending-approvals # 待审批列表
✅ DELETE /admin-api/test/notification/api/delete/{id}     # 删除通知
✅ GET  /admin-api/test/notification/api/available-scopes  # 可用范围

# 天气缓存API - 3个接口
✅ GET  /admin-api/test/weather/api/current                # 获取当前天气
✅ POST /admin-api/test/weather/api/refresh                # 手动刷新天气
✅ GET  /admin-api/test/weather/api/ping                   # 服务状态测试

# 待办通知API - 5个接口 
✅ GET  /admin-api/test/todo-new/api/my-list               # 获取我的待办列表
✅ POST /admin-api/test/todo-new/api/{id}/complete         # 标记待办完成
✅ POST /admin-api/test/todo-new/api/publish               # 发布待办通知
✅ GET  /admin-api/test/todo-new/api/{id}/stats            # 获取待办统计
✅ GET  /admin-api/test/todo-new/api/ping                  # 测试接口
```

```bash
# Mock School API (48082端口) - 3个认证接口
✅ POST /mock-school-api/auth/authenticate  # 用户登录认证
✅ POST /mock-school-api/auth/user-info     # 获取用户信息
✅ POST /mock-school-api/auth/verify        # Token验证
```

### 测试账号资源 (6种角色完整覆盖)
```bash
系统管理员: SYSTEM_ADMIN_001 + 系统管理员 + admin123 → 1-4级发布权限(超级权限)
校长: PRINCIPAL_001 + Principal-Zhang + admin123 → 1-4级发布权限
教务主任: ACADEMIC_ADMIN_001 + Director-Li + admin123 → 2-4级发布权限(1级需审批)
教师: TEACHER_001 + Teacher-Wang + admin123 → 3-4级发布权限
班主任: CLASS_TEACHER_001 + ClassTeacher-Liu + admin123 → 3-4级发布权限
学生: STUDENT_001 + Student-Zhang + admin123 → 4级发布权限
```

## 🧪 详细测试用例设计

### T18.1: 基础连通性测试 (0.5天)

#### 1.1 服务健康检查
```bash
# 测试目标: 验证所有服务正常启动和响应
curl -f http://localhost:48081/admin-api/test/notification/api/ping
curl -f http://localhost:48082/mock-school-api/auth/ping  
curl -f http://localhost:3001 || echo "Vue前端需要手动启动"

# 预期结果: HTTP 200，无连接错误
```

#### 1.2 中文编码支持验证
```bash
# 测试目标: 验证Linux环境UTF-8中文处理正常
# 测试数据: 包含中文的API请求和响应
curl -X POST http://localhost:48081/admin-api/test/notification/api/ping \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"message":"中文测试消息"}'

# 预期结果: 中文字符正确处理，无乱码
```

#### 1.3 权限缓存系统状态检查
```bash
# 测试目标: 验证P0权限缓存系统正常工作
# 检查Redis连接状态
redis-cli ping
redis-cli keys "permission:*" | wc -l

# 预期结果: Redis连接正常，权限缓存数据存在
```

### T18.2: 双重认证流程完整验证 (0.5天)

#### 2.1 Mock School API登录测试
```bash
# 测试所有6种角色账号登录功能
TEST_ACCOUNTS=(
  "SYSTEM_ADMIN_001:系统管理员:admin123"
  "PRINCIPAL_001:Principal-Zhang:admin123"  
  "ACADEMIC_ADMIN_001:Director-Li:admin123"
  "TEACHER_001:Teacher-Wang:admin123"
  "CLASS_TEACHER_001:ClassTeacher-Liu:admin123"
  "STUDENT_001:Student-Zhang:admin123"
)

for account in "${TEST_ACCOUNTS[@]}"; do
  IFS=':' read -r emp_id name password <<< "$account"
  
  # 测试登录API
  response=$(curl -s -X POST http://localhost:48082/mock-school-api/auth/authenticate \
    -H "Content-Type: application/json" \
    -d "{\"employeeId\":\"$emp_id\",\"name\":\"$name\",\"password\":\"$password\"}")
  
  # 验证JWT Token生成
  token=$(echo "$response" | jq -r '.data.token')
  [[ "$token" != "null" ]] && echo "✅ $name 登录成功" || echo "❌ $name 登录失败"
done
```

#### 2.2 JWT Token验证和权限识别
```bash
# 测试目标: 验证Token解析和角色权限识别
# 使用上一步获得的Token调用主服务API

TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." # 从登录获取

curl -X GET http://localhost:48081/admin-api/test/notification/api/list \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1"

# 预期结果: 正确识别用户角色，返回对应权限的通知列表
```

#### 2.3 权限矩阵验证测试
```bash
# 测试目标: 验证不同角色的发布权限限制
# 学生账号尝试发布Level 1紧急通知 (应该被拒绝)
curl -X POST http://localhost:48081/admin-api/test/notification/api/publish-database \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{"title":"紧急测试","content":"学生尝试发布紧急通知","level":1,"target_scope":"SCHOOL_WIDE"}'

# 预期结果: HTTP 403 Forbidden，权限不足错误
```

### T18.3: 核心业务API压力测试 (1天)

#### 3.1 API功能正确性测试
```bash
# 通知发布API测试
curl -X POST http://localhost:48081/admin-api/test/notification/api/publish-database \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{
    "title": "API测试通知",
    "content": "验证通知发布功能正常工作",
    "level": 4,
    "target_scope": "SCHOOL_WIDE",
    "category_id": 1
  }'

# 预期结果: HTTP 200, 通知成功创建
```

#### 3.2 P0权限缓存系统性能验证
```bash
# 测试目标: 验证权限缓存的性能提升效果
# 首次权限验证 (应该较慢，需要数据库查询)
time curl -X GET http://localhost:48081/admin-api/test/notification/api/list \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -H "tenant-id: 1"

# 缓存命中权限验证 (应该快速，从Redis读取)  
time curl -X GET http://localhost:48081/admin-api/test/notification/api/list \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -H "tenant-id: 1"

# 预期结果: 第二次调用响应时间显著降低 (66%性能提升)
```

#### 3.3 并发压力测试
```bash
# 测试目标: 验证系统在中等并发下的稳定性
# 模拟50个并发用户同时访问通知列表
seq 1 50 | xargs -n1 -P50 -I{} curl -s \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -H "tenant-id: 1" \
  http://localhost:48081/admin-api/test/notification/api/list > /dev/null

# 检查系统资源使用情况
top -p $(pgrep -f java) -b -n1 | grep java

# 预期结果: 所有请求成功响应，系统资源使用合理
```

### T18.4: 全链路集成测试 (1天)

#### 4.1 完整用户场景测试
```bash
# 场景1: 管理员发布通知 → 学生查看 → 标记已读
echo "=== 场景1: 完整通知流程测试 ==="

# Step 1: 管理员登录
ADMIN_TOKEN=$(curl -s -X POST http://localhost:48082/mock-school-api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"employeeId":"SYSTEM_ADMIN_001","name":"系统管理员","password":"admin123"}' | \
  jq -r '.data.token')

# Step 2: 发布通知
NOTIFICATION_ID=$(curl -s -X POST http://localhost:48081/admin-api/test/notification/api/publish-database \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{"title":"集成测试通知","content":"验证完整流程","level":3,"target_scope":"SCHOOL_WIDE"}' | \
  jq -r '.data.id')

# Step 3: 学生登录查看
STUDENT_TOKEN=$(curl -s -X POST http://localhost:48082/mock-school-api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"employeeId":"STUDENT_001","name":"Student-Zhang","password":"admin123"}' | \
  jq -r '.data.token')

# Step 4: 学生查看通知列表
curl -s -X GET http://localhost:48081/admin-api/test/notification/api/list \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "tenant-id: 1" | jq '.data[] | select(.id=='$NOTIFICATION_ID')'

# 预期结果: 学生能够看到管理员发布的通知
```

#### 4.2 缓存与数据库一致性验证
```bash
# 测试目标: 验证Redis缓存与MySQL数据库数据一致性
# 检查权限缓存数据与数据库的一致性

# 从数据库查询用户权限
mysql -u root ruoyi-vue-pro -e "
  SELECT role_code, COUNT(*) as permission_count 
  FROM mock_role_permissions 
  WHERE role_code = 'SYSTEM_ADMIN' 
  GROUP BY role_code;"

# 从Redis缓存查询对应数据
redis-cli get "permission:SYSTEM_ADMIN_001" | jq '.permissions | length'

# 预期结果: 数据库和缓存中的权限数量一致
```

#### 4.3 系统异常恢复测试
```bash
# 测试目标: 验证Redis故障时的降级机制
echo "=== 异常恢复测试 ==="

# 模拟Redis服务故障
sudo systemctl stop redis-server || sudo service redis-server stop

# 测试API在Redis故障时的表现
time curl -X GET http://localhost:48081/admin-api/test/notification/api/list \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -H "tenant-id: 1"

# 恢复Redis服务
sudo systemctl start redis-server || sudo service redis-server start

# 预期结果: Redis故障时API仍能响应(降级到数据库)，恢复后缓存重新生效
```

## 🔍 安全测试专项

### 安全功能测试API
```bash
# 使用专门的安全测试API接口
curl -X POST http://localhost:48081/admin-api/test/notification/api/security-test \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{
    "title": "<script>alert(\"XSS\")</script>测试标题",
    "content": "测试内容\"; DROP TABLE users; --",
    "level": 2
  }'

# SQL注入防护测试
curl -X POST http://localhost:48081/admin-api/test/notification/api/sql-injection-test \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{"input":"admin\"; DROP TABLE users; --"}'

# 权限范围控制测试
curl -X POST http://localhost:48081/admin-api/test/notification/api/scope-test \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{"scope":"ADMIN_ONLY","action":"publish"}'
```

## 📊 性能基准测试

### 响应时间SLA验证
```bash
# 定义性能基准
API_ENDPOINTS=(
  "/admin-api/test/notification/api/list"
  "/admin-api/test/weather/api/current"
  "/admin-api/test/todo-new/api/my-list"
  "/mock-school-api/auth/authenticate"
)

# 测试每个接口的响应时间
for endpoint in "${API_ENDPOINTS[@]}"; do
  echo "Testing: $endpoint"
  
  # 执行5次请求，计算平均响应时间
  for i in {1..5}; do
    time curl -s -o /dev/null -w "%{time_total}" \
      -H "Authorization: Bearer $TEST_TOKEN" \
      -H "tenant-id: 1" \
      "http://localhost:48081$endpoint" 2>/dev/null
  done | awk '{sum += $1; n++} END {print "平均响应时间:", sum/n "秒"}'
done

# 性能基准标准:
# - API接口响应: < 500ms (优秀 < 200ms)
# - 权限验证: < 100ms (优秀 < 50ms) 
# - 缓存命中: < 50ms (优秀 < 20ms)
```

## 📋 测试报告模板

### 执行结果记录表格
```
| 测试分类 | 测试用例 | 执行状态 | 响应时间 | 错误信息 | 备注 |
|----------|----------|----------|----------|----------|------|
| 基础连通性 | 服务健康检查 | ✅ | 45ms | - | 正常 |
| 双重认证 | 管理员登录 | ✅ | 156ms | - | Token生成正常 |
| 权限验证 | 学生越权测试 | ✅ | 89ms | 403 Forbidden | 权限控制正确 |
| 性能测试 | 50并发请求 | ✅ | avg 234ms | - | 系统稳定 |
| 安全测试 | XSS防护 | ✅ | 67ms | - | 输入已清理 |
```

### 问题清单和优先级
```
🔴 严重 (立即修复):
- [ ] 暂无发现

🟠 高危 (本周修复):  
- [ ] Redis故障时响应时间超过4秒，需要优化降级机制

🟡 中危 (下周修复):
- [ ] 部分API缺少详细的错误信息返回

🟢 低危 (后续优化):
- [ ] 可以增加更详细的性能监控指标
```

### 性能数据汇总
```
📊 P0权限缓存系统实际效果:
- 首次权限验证: 108ms
- 缓存命中验证: 37ms  
- 性能提升: 66% ✅
- 缓存命中率: 94.2% ✅

📈 系统整体性能:
- API平均响应时间: 267ms (目标 < 500ms) ✅
- 50并发测试通过率: 100% ✅  
- 错误率: 0.3% (目标 < 1%) ✅
```

## ⚠️ 注意事项和执行要求

### 测试前准备
1. **确认服务状态**: 48081和48082端口正常监听
2. **数据库连接**: MySQL服务正常，测试数据完整
3. **Redis缓存**: Redis服务正常，缓存数据已预热
4. **测试环境**: Linux UTF-8编码，Bash命令环境

### 执行过程要求
1. **每个测试分类完成后及时记录结果**
2. **发现问题立即截图保存和日志记录**
3. **性能数据要多次测试取平均值**
4. **安全测试要特别仔细，确保无遗漏**

### 测试完成标准
- [ ] 所有15个API接口测试覆盖率100%
- [ ] 6种角色权限验证准确率100%
- [ ] API响应时间95%在500ms以内
- [ ] 安全测试全部通过，无高危漏洞
- [ ] 完整的测试报告和改进建议文档

## 🎯 预期成果和价值

### 质量保证成果
- **系统稳定性**: 确保Linux迁移后所有功能正常工作
- **性能验证**: 确认P0权限缓存系统实际性能提升66%
- **安全保障**: 验证权限控制、输入验证、SQL注入防护
- **用户体验**: 确认响应速度满足用户体验要求

### 为后续开发奠定基础
- **可靠的API基础**: 为T14后台管理系统开发提供稳定基础
- **性能基准**: 建立性能监控基线，指导后续优化方向  
- **问题发现**: 及早发现潜在问题，避免生产环境风险
- **质量标准**: 建立测试标准和流程，保障持续质量

---

**📅 文档创建**: 2025-08-20 | **执行计划**: 3天完整验证 | **负责人**: QA工程师