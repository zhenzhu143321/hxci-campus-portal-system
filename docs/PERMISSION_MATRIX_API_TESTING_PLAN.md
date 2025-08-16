# 🧪 权限矩阵深度API测试计划

## 📋 测试背景

**发现问题**: NotificationScopeManager.java中存在关键权限逻辑Bug，教务主任发布1级紧急全校通知被直接拒绝，而不是提交审批。

**修复内容**: 
```java
// 🚨 修复前：直接拒绝教务主任
result.hasPermission = false;
result.reason = "1级紧急全校通知只有校长可以发布";

// ✅ 修复后：允许教务主任发布但需审批
if ("ACADEMIC_ADMIN".equals(roleCode)) {
    result.hasPermission = true;
    result.reason = "1级紧急全校通知需要校长审批";
    result.requiresApproval = true;
    result.approver = "校长";
}
```

**测试目标**: 确保后端稳定性100%，避免再次出现权限逻辑漏洞。

## 🎯 测试范围与策略

### 1️⃣ 权限矩阵3D测试 (角色×级别×范围)

#### 测试维度
- **角色维度**: PRINCIPAL, ACADEMIC_ADMIN, TEACHER, CLASS_TEACHER, STUDENT
- **级别维度**: 1级(紧急), 2级(重要), 3级(常规), 4级(提醒)
- **范围维度**: SCHOOL_WIDE, DEPARTMENT, CLASS, GRADE

#### 关键测试用例矩阵

| 角色 | 级别 | 范围 | 预期结果 | 审批要求 | 测试优先级 |
|------|------|------|----------|----------|------------|
| PRINCIPAL | 1 | SCHOOL_WIDE | ✅ 允许 | ❌ 无需审批 | 🔥 P0 |
| ACADEMIC_ADMIN | 1 | SCHOOL_WIDE | ✅ 允许 | ✅ 需要审批 | 🔥 P0-CRITICAL |
| TEACHER | 1 | SCHOOL_WIDE | ❌ 拒绝 | - | 🔥 P0 |
| STUDENT | 1 | SCHOOL_WIDE | ❌ 拒绝 | - | 🔥 P0 |
| ACADEMIC_ADMIN | 2 | SCHOOL_WIDE | ✅ 允许 | ❌ 无需审批 | ⚡ P1 |
| TEACHER | 3 | DEPARTMENT | ✅ 允许 | ❌ 无需审批 | ⚡ P1 |
| STUDENT | 4 | CLASS | ✅ 允许 | ❌ 无需审批 | ⚡ P1 |

#### 总计测试用例数: **60个** (5角色 × 4级别 × 3范围)

### 2️⃣ 审批工作流完整链路测试

#### 2.1 正常审批流程
1. **发布阶段**
   - 教务主任发布1级紧急通知
   - 验证状态: `PENDING_APPROVAL` (status=2)
   - 验证审批标记: `requiresApproval = true`

2. **审批阶段**  
   - 校长查看待审批列表
   - 校长批准通知
   - 验证状态变更: `APPROVED` (status=3)

3. **拒绝流程**
   - 校长拒绝通知
   - 验证状态变更: `REJECTED` (status=6)
   - 验证拒绝原因记录

#### 2.2 异常流程测试
- 非校长角色尝试审批 → 权限拒绝
- 审批不存在的通知 → 404错误
- 重复审批同一通知 → 状态冲突检测

### 3️⃣ 边界条件和异常情况测试

#### 3.1 输入参数边界测试
```javascript
// 测试用例示例
const boundaryTests = [
    // 空值测试
    { roleCode: null, level: 1, scope: "SCHOOL_WIDE", expected: "拒绝" },
    { roleCode: "", level: 1, scope: "SCHOOL_WIDE", expected: "拒绝" },
    
    // 非法角色测试
    { roleCode: "UNKNOWN_ROLE", level: 1, scope: "SCHOOL_WIDE", expected: "拒绝" },
    { roleCode: "HACKER", level: 1, scope: "SCHOOL_WIDE", expected: "拒绝" },
    
    // 非法级别测试
    { roleCode: "PRINCIPAL", level: 0, scope: "SCHOOL_WIDE", expected: "处理异常" },
    { roleCode: "PRINCIPAL", level: 999, scope: "SCHOOL_WIDE", expected: "处理异常" },
    { roleCode: "PRINCIPAL", level: -1, scope: "SCHOOL_WIDE", expected: "处理异常" },
    
    // 非法范围测试
    { roleCode: "PRINCIPAL", level: 1, scope: null, expected: "默认SCHOOL_WIDE" },
    { roleCode: "PRINCIPAL", level: 1, scope: "INVALID_SCOPE", expected: "默认SCHOOL_WIDE" },
    
    // SQL注入测试
    { roleCode: "'; DROP TABLE notification_info; --", level: 1, scope: "SCHOOL_WIDE", expected: "安全拒绝" },
];
```

#### 3.2 数据一致性测试
- 发布通知后立即查询，验证数据一致性
- 审批操作后状态同步验证
- 并发操作时的数据竞争处理

### 4️⃣ 并发和性能安全测试

#### 4.1 并发场景测试
```javascript
// 并发测试场景
const concurrencyTests = [
    {
        scenario: "多个教务主任同时发布1级紧急通知",
        concurrent: 5,
        expected: "全部成功，状态均为PENDING_APPROVAL"
    },
    {
        scenario: "校长同时处理多个审批请求", 
        concurrent: 3,
        expected: "全部成功，无状态冲突"
    },
    {
        scenario: "同一通知被多次审批",
        concurrent: 3,
        expected: "只有第一个成功，其他返回冲突错误"
    }
];
```

#### 4.2 性能压力测试
- 1000次权限验证API调用性能测试
- 数据库连接池压力测试
- 内存泄漏检测

### 5️⃣ API接口详细测试

#### 5.1 核心API列表
```bash
# 权限验证相关
POST /admin-api/test/notification/api/publish-database  # 发布通知（含权限验证）
GET  /admin-api/test/notification/api/available-scopes  # 获取可用范围
POST /admin-api/test/notification/api/scope-test        # 范围控制测试

# 审批工作流相关  
GET  /admin-api/test/notification/api/pending-approvals # 待审批列表
POST /admin-api/test/notification/api/approve           # 批准通知
POST /admin-api/test/notification/api/reject            # 拒绝通知

# 通知查询相关
GET  /admin-api/test/notification/api/list              # 通知列表（含权限过滤）
```

#### 5.2 每个API的测试维度
1. **功能正确性**: 基本功能是否正常
2. **权限验证**: 不同角色访问是否正确控制
3. **参数验证**: 非法参数是否正确拒绝  
4. **错误处理**: 异常情况是否优雅处理
5. **数据一致性**: 数据库状态是否正确更新
6. **性能表现**: 响应时间是否在合理范围

## 🧪 测试执行计划

### Phase 1: 关键路径测试 (优先级P0)
**预估时间**: 2小时
**测试用例数**: 15个核心场景
```javascript
// P0关键测试用例
const p0Tests = [
    // 🔥 最关键: 教务主任1级紧急通知权限修复验证
    { role: "ACADEMIC_ADMIN", level: 1, scope: "SCHOOL_WIDE", expected: { allow: true, approval: true } },
    
    // 🔥 校长权限验证
    { role: "PRINCIPAL", level: 1, scope: "SCHOOL_WIDE", expected: { allow: true, approval: false } },
    
    // 🔥 其他角色拒绝验证
    { role: "TEACHER", level: 1, scope: "SCHOOL_WIDE", expected: { allow: false } },
    { role: "STUDENT", level: 1, scope: "SCHOOL_WIDE", expected: { allow: false } },
    
    // 🔥 审批工作流链路
    "教务主任发布1级通知 → 校长批准 → 状态正确更新",
    "教务主任发布1级通知 → 校长拒绝 → 状态正确更新"
];
```

### Phase 2: 权限矩阵全覆盖测试 (优先级P1)  
**预估时间**: 4小时
**测试用例数**: 60个 (5角色×4级别×3范围)

### Phase 3: 边界和异常测试 (优先级P2)
**预估时间**: 2小时  
**测试用例数**: 25个边界case

### Phase 4: 并发和性能测试 (优先级P3)
**预估时间**: 1小时
**测试场景数**: 10个并发场景

## 📊 测试数据准备

### 测试账号矩阵
```javascript
const testAccounts = {
    PRINCIPAL: {
        token: "YD_SCHOOL_PRINCIPAL_001",
        name: "Principal-Zhang", 
        permissions: ["ALL"]
    },
    ACADEMIC_ADMIN: {
        token: "YD_SCHOOL_ACADEMIC_ADMIN_001", 
        name: "Director-Li",
        permissions: ["PUBLISH_L2-L4", "PUBLISH_L1_WITH_APPROVAL"]
    },
    TEACHER: {
        token: "YD_SCHOOL_TEACHER_001",
        name: "Teacher-Wang", 
        permissions: ["PUBLISH_L3-L4_DEPARTMENT"]
    },
    CLASS_TEACHER: {
        token: "YD_SCHOOL_CLASS_TEACHER_001",
        name: "ClassTeacher-Liu",
        permissions: ["PUBLISH_L3-L4_CLASS"]  
    },
    STUDENT: {
        token: "YD_SCHOOL_STUDENT_001",
        name: "Student-Zhang",
        permissions: ["PUBLISH_L4_CLASS"]
    }
};
```

### 测试数据状态 (已清理完成)
```sql
-- ✅ 数据库已清理，只保留5条关键测试数据:
-- ID=1: 校长发布的1级紧急通知（已发布）
-- ID=2: 教务主任发布的1级紧急通知（待审批）⭐ 关键测试数据
-- ID=3: 教务主任发布的1级紧急通知（已拒绝）
-- ID=4: 教务主任发布的2级重要通知（已发布） 
-- ID=5: 教师发布的3级常规通知（已发布）
```

## ✅ 成功标准

### 测试通过标准
1. **P0核心场景**: 100%通过率
2. **权限矩阵**: ≥95%通过率  
3. **异常处理**: ≥90%通过率
4. **性能要求**: API响应时间<500ms
5. **并发安全**: 无数据不一致问题

### 关键验证点
- ✅ 教务主任可以发布1级紧急通知，状态为PENDING_APPROVAL
- ✅ 校长可以批准/拒绝教务主任的1级紧急通知  
- ✅ 非授权角色无法发布1级紧急通知
- ✅ 所有权限检查逻辑正确无误
- ✅ 审批工作流状态转换正确
- ✅ 数据库操作原子性保证

## 🚨 测试重点提醒

### 必须重点验证的Bug修复
```java
// 🔥 这个修复必须100%验证通过
if ("ACADEMIC_ADMIN".equals(roleCode)) {
    result.hasPermission = true;  // ← 必须是true
    result.requiresApproval = true;  // ← 必须是true  
    result.approver = "校长";  // ← 必须正确设置
}
```

### 高风险场景
1. **权限绕过**: 确保不能通过参数篡改绕过权限检查
2. **状态不一致**: 确保审批操作后状态正确同步
3. **并发冲突**: 确保多用户同时操作不会导致数据混乱
4. **SQL注入**: 确保所有输入都经过安全验证

## 📈 预期输出

### 测试报告内容
1. **执行摘要**: 总体通过率、关键发现
2. **权限矩阵测试结果**: 60个用例的详细结果
3. **审批工作流验证**: 完整链路测试结果  
4. **性能基准**: API响应时间统计
5. **安全评估**: 安全漏洞检查结果
6. **修复验证**: Bug修复效果确认

### 交付物
- 详细测试报告 (PERMISSION_MATRIX_TEST_REPORT.md)
- 自动化测试脚本 (权限矩阵测试套件)
- 性能基准数据
- 安全漏洞评估报告

---

**测试执行者**: QA-Engineer / Full-StackEngineer  
**预估总时间**: 9小时  
**测试用例总数**: 110+个  
**测试优先级**: 🔥 CRITICAL - 必须100%通过后才能继续开发

> 💡 **重要提醒**: 这次测试发现的权限逻辑Bug说明我们之前的测试覆盖不够全面。这次深度测试的目的是建立完整的权限验证安全网，确保类似问题不再发生。