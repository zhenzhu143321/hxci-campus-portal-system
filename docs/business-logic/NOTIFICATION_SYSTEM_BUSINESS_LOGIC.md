# 智能通知系统业务逻辑完整梳理

## 🎯 系统架构概览

**核心理念**：自主开发的教育机构智能通知系统，拥有完整的业务逻辑和数据模型，前端需要适配我们的业务规则。

### 架构层次
```
前端应用 (Vue3 + Element Plus)
    ↓
主通知服务 (yudao-server:48081) 
    ↓  
Mock School API (yudao-mock-school-api:48082)
    ↓
MySQL数据库 (ruoyi-vue-pro)
```

## 🔐 认证业务流程 (修正版)

### 1. 用户登录认证流程
**业务规则**：双重验证模式 - Mock API身份认证 + 主服务权限验证

```
1. 前端发起登录请求 → 主通知服务
   POST /admin-api/server/notification/login
   {
     "username": "admin",
     "password": "admin123", 
     "tenantName": "默认租户"
   }

2. 主通知服务调用 → Mock School API身份验证
   POST /mock-school-api/auth/authenticate
   {
     "username": "admin",
     "password": "admin123"
   }
   
   目的：验证用户是否为学校的合法学生或教职工

3. Mock School API返回 → 学校身份信息
   {
     "code": 200,
     "data": {
       "userId": "ADMIN_001",        // 学校系统用户ID
       "username": "admin", 
       "roleCode": "TEACHER",        // 学校角色：TEACHER/STUDENT等
       "roleName": "任课教师",
       "studentId": "2021001001",    // 学号 (学生用户)
       "teacherId": "T2021001",      // 工号 (教师用户)  
       "departmentId": 1,
       "departmentName": "计算机系",
       "enabled": true
     }
   }

4. 主通知服务生成包含学号/工号的Token
   Token格式：YD_SCHOOL_{ROLE}_{学号/工号}_{TIMESTAMP}
   例如：YD_SCHOOL_TEACHER_T2021001_1723127456789

5. 主通知服务查询 → 通知系统权限数据库
   根据学号/工号在通知系统数据库中查找该用户的通知权限：
   - 查询用户在通知系统中的权限配置
   - 确认可发布的通知级别
   - 确认通知范围和审批需求

6. 返回最终登录响应 → 前端
   {
     "code": 0,
     "data": {
       "accessToken": "YD_SCHOOL_TEACHER_T2021001_1723127456789",
       "tokenType": "Bearer",
       "expiresIn": 7200,
       "refreshToken": "refresh_YD_SCHOOL_TEACHER_T2021001_1723127456789",
       "user": {
         "id": "T2021001",              // 工号或学号
         "username": "admin",
         "nickname": "张老师",
         "roleCode": "TEACHER",         // 学校角色
         "roleName": "任课教师", 
         "studentId": null,             // 学号 (教师为null)
         "teacherId": "T2021001",       // 工号 (学生为null)
         "deptId": 1,
         "deptName": "计算机系",
         // 通知系统权限 (从主服务数据库查询)
         "notificationPermissions": {
           "canPublishLevels": [3, 4],  // 可发布常规和提醒通知
           "targetScope": "DEPARTMENT",  // 通知范围：系内
           "needApproval": false        // 是否需要审批
         }
       }
     },
     "msg": ""
   }
```

### 2. 权限验证流程 (修正版)
**业务规则**：基于学号/工号的通知系统权限查询

```
1. 前端请求携带Token
   Authorization: Bearer YD_SCHOOL_TEACHER_T2021001_1723127456789

2. 主通知服务解析Token → 提取学号/工号
   从Token中提取：T2021001 (工号)

3. 主通知服务查询权限 → 通知系统数据库
   根据工号T2021001在通知系统用户权限表中查询：
   - 该教师在通知系统中的权限级别
   - 可发布的通知类型
   - 通知目标范围
   - 是否需要审批流程

4. 返回权限验证结果
   {
     "permissionGranted": true,
     "userInfo": {
       "teacherId": "T2021001",
       "username": "张老师",
       "notificationLevels": [3, 4],
       "targetScope": "DEPARTMENT"  
     }
   }
```

## 🏫 用户角色权限体系 (修正版)

### 身份认证层 (Mock School API)
**目的**：验证用户是学校合法的学生或教职工
```
STUDENT (学生)
    ├── 学号：如 2021001001
    ├── 班级：如 计算机21-1班
    └── 验证：确认为在校学生身份

TEACHER (任课教师)  
    ├── 工号：如 T2021001
    ├── 所属系：如 计算机系
    └── 验证：确认为在职教师身份

ADMIN (管理人员)
    ├── 工号：如 A2021001  
    ├── 部门：如 教务处、学工处
    └── 验证：确认为管理人员身份
```

### 权限控制层 (通知系统数据库)  
**目的**：基于学号/工号配置通知系统的操作权限
```
基于学号/工号的权限配置表：notification_user_permissions

学号 2021001001 (学生张三)
    ├── 权限等级：仅查看通知
    ├── 通知范围：CLASS (班级通知)
    └── 特殊权限：班干部可发布提醒通知

工号 T2021001 (教师李四)
    ├── 权限等级：3-4级通知 (常规/提醒)
    ├── 通知范围：COURSE (课程相关)
    └── 审批需求：无需审批

工号 A2021001 (教务处管理员)  
    ├── 权限等级：1-4级通知 (全部权限)
    ├── 通知范围：ALL_SCHOOL (全校)
    └── 审批需求：重要通知需校长审批
```

### 数据库用户表结构
**表名**: `mock_school_users`
```sql
- id: 主键ID
- username: 用户名 (如: admin, 校长张明)
- user_id: 系统用户ID (如: ADMIN_001, PRINCIPAL_001)
- role_code: 角色编码 (如: ADMIN, PRINCIPAL) 
- role_name: 角色名称 (如: 系统管理员, 校长)
- department_id: 部门ID
- department_name: 部门名称
- enabled: 是否启用
- token: 认证Token
- token_expires_at: Token过期时间
- create_time: 创建时间
```

**当前测试用户**:
```
admin / admin123 → ADMIN (系统管理员)
校长张明 / admin123 → PRINCIPAL (校长)
教务处主任李华 / admin123 → ACADEMIC_ADMIN (教务主任)
```

## 📢 通知业务逻辑

### 通知级别分类
```
1. EMERGENCY (紧急通知)
   - 颜色标识: 🔴 红色
   - 适用场景: 校园安全警报、紧急停课通知
   - 发布权限: PRINCIPAL, ADMIN
   
2. IMPORTANT (重要通知) 
   - 颜色标识: 🟠 橙色
   - 适用场景: 考试安排、重要政策变更
   - 发布权限: PRINCIPAL, ACADEMIC_ADMIN, ADMIN
   
3. REGULAR (常规通知)
   - 颜色标识: 🔵 蓝色  
   - 适用场景: 课程调整、活动安排
   - 发布权限: PRINCIPAL, ACADEMIC_ADMIN, TEACHER, CLASS_TEACHER, ADMIN
   
4. REMINDER (提醒通知)
   - 颜色标识: 🟣 紫色
   - 适用场景: 作业提醒、课程提醒
   - 发布权限: 所有角色 (除STUDENT)
```

### 通知状态流转
```
0. DRAFT (草稿)
   ↓
1. PENDING_APPROVAL (待审批) ← 需要审批的通知
   ↓
2. APPROVED (已审批)
   ↓  
3. PUBLISHED (已发布)
   ↓
4. CANCELLED (已取消)
   ↓
5. EXPIRED (已过期)
   ↓
6. ARCHIVED (已归档)
```

### 推送渠道配置
```
1: APP推送
2: 短信通知  
3: 邮件通知
4: 微信通知
5: 系统内消息

示例: "1,2,5" 表示同时通过APP、短信、系统内消息推送
```

## 🗄️ 数据库表结构 (修正版)

### Mock School API数据表 (身份验证层)
```sql
-- Mock学校用户表 (验证学生/教师身份)
mock_school_users:
  - id: 主键ID
  - username: 用户名 
  - user_id: 学校系统用户ID
  - role_code: 学校角色 (STUDENT/TEACHER/ADMIN)
  - role_name: 角色名称
  - student_id: 学号 (学生专用，教师为null)
  - teacher_id: 工号 (教师专用，学生为null)
  - department_id: 所属系/班级ID
  - department_name: 所属系/班级名称
  - enabled: 账户状态
  - token: 临时认证token
```

### 通知系统数据表 (权限控制层)
```sql
-- 通知系统用户权限表 (基于学号/工号的权限配置)
notification_user_permissions:
  - id: 主键ID
  - school_id: 学号或工号 (与Mock API关联的唯一标识)
  - user_type: 用户类型 (STUDENT/TEACHER/ADMIN)
  - username: 用户姓名
  - notification_levels: 可发布通知级别 JSON array [1,2,3,4]
  - target_scope: 通知范围 (CLASS/DEPARTMENT/ALL_SCHOOL)
  - need_approval: 是否需要审批
  - approval_roles: 审批者角色列表
  - max_daily_notifications: 每日最大发布数量
  - special_permissions: 特殊权限配置 JSON
  - enabled: 权限状态
  - create_time: 创建时间
  - update_time: 更新时间

-- 示例数据
INSERT INTO notification_user_permissions VALUES
('T2021001', 'TEACHER', '张老师', '[3,4]', 'DEPARTMENT', false, null, 10, null, true),
('2021001001', 'STUDENT', '李同学', '[]', 'CLASS', false, null, 0, '{"class_monitor": true}', true),
('A2021001', 'ADMIN', '王主任', '[1,2,3,4]', 'ALL_SCHOOL', true, '["PRINCIPAL"]', 50, null, true);
```

### 核心通知业务表
```sql
-- 通知信息表 (增加发布者学号/工号字段)
notification_info:
  - id: 通知编号
  - title: 通知标题
  - content: 通知内容  
  - level: 通知级别 (1-4)
  - status: 通知状态 (0-6)
  - publisher_school_id: 发布者学号/工号 (关联字段)
  - publisher_name: 发布者姓名
  - publisher_role: 发布者学校角色
  - target_scope: 目标范围
  - target_audience: 目标受众 JSON
  - push_channels: 推送渠道
  - require_confirm: 是否需要确认
  - expired_time: 过期时间
  - approval_status: 审批状态
  - approver_school_id: 审批者学号/工号
  - create_time: 创建时间
```

## 🔌 API接口规范

### 主通知服务接口 (48081)
```
POST /admin-api/server/notification/login
- 用途: 用户登录认证
- 请求头: tenant-id: 1
- 响应: 包含accessToken和用户信息

POST /admin-api/server/notification/publish  
- 用途: 发布通知
- 请求头: Authorization: Bearer {token}
- 权限: 根据用户角色和通知级别验证

GET /admin-api/server/notification/user-info
- 用途: 获取当前用户信息
- 请求头: Authorization: Bearer {token}

POST /admin-api/server/notification/verify-permission
- 用途: 验证用户权限
- 参数: notificationLevel, targetScope
```

### Mock School API接口 (48082)
```
POST /mock-school-api/auth/authenticate
- 用途: 用户名密码认证
- 参数: username, password

POST /mock-school-api/auth/verify
- 用途: Token验证
- 参数: token

POST /mock-school-api/notification/verify-publish-permission
- 用途: 验证发布权限
- 参数: userId, notificationLevel, targetScope
```

## 🎨 前端适配要求 (更新版)

### 必须适配的业务逻辑
1. **登录响应格式**: 适配我们的Token和用户信息结构 ✅ **已完成**
2. **权限验证**: 基于角色编码和通知级别的权限控制
3. **通知级别**: 四级通知分类和对应的UI展示
4. **状态流转**: 通知从草稿到发布的完整生命周期
5. **多渠道推送**: 支持多种推送方式的选择和配置

### 前端不应该改变的后端逻辑
- ❌ Token生成和验证机制
- ❌ 用户角色权限体系  
- ❌ 通知业务状态流转
- ❌ 数据库表结构和字段命名
- ❌ API接口路径和参数格式

### ✅ 已完成的前端适配工作

#### 1. **登录流程适配** `LoginForm.vue`
```javascript
// 🔧 适配我们的通知系统登录响应格式
// 构造前端期望的Token格式
const tokenData = {
  id: Date.now(),
  accessToken: res.accessToken,
  refreshToken: res.refreshToken,
  userId: res.user?.id || res.user?.userId,
  userType: 1,
  clientId: 'yudao-notification-ui',
  expiresTime: Date.now() + (res.expiresIn || 7200) * 1000
}

// 存储用户信息到localStorage
if (res.user) {
  localStorage.setItem('userInfo', JSON.stringify(res.user))
}
```

#### 2. **类型定义更新** `types.ts`
```typescript
export type TokenType = {
  id: number
  accessToken: string
  refreshToken: string
  userId: number | string // 支持字符串格式(学号工号)
  userType: number
  clientId: string
  expiresTime: number
}

export type UserVO = {
  id: number | string // 支持数字和字符串ID格式
  username: string
  nickname: string
  // 通知系统特有字段
  roleCode?: string // 角色编码 (如TEACHER, STUDENT)
  roleName?: string // 角色名称 (如任课教师, 学生)
  departmentId?: number // 系部ID
  departmentName?: string // 系部名称
  // ... 其他可选字段
}
```

#### 3. **API接口路径适配** `login/index.ts`
```typescript
// 登录接口 - 调用通知系统登录接口
export const login = (data: UserLoginVO) => {
  return request.post({ url: '/server/notification/login', data })
}

// 获取用户信息 - 调用通知系统接口
export const getInfo = () => {
  return request.get({ url: '/server/notification/user-info' })
}
```

#### 4. **后端用户信息接口** `NotificationController.java:284-329`
```java
@GetMapping("/user-info")
public CommonResult<Map<String, Object>> getUserInfo(HttpServletRequest request) {
    // 从Authorization头部获取Token
    String token = authHeader.substring(7);
    
    // 通过Mock School API验证token并获取用户信息
    MockSchoolApiIntegration.UserInfo userInfo = mockSchoolApi.verifyUserToken(token);
    
    // 构造标准化的用户信息响应
    Map<String, Object> responseData = new HashMap<>();
    responseData.put("user", userResponse);
    responseData.put("permissions", userInfo.getPermissions());
    responseData.put("roles", Arrays.asList(userInfo.getRoleCode()));
    
    return CommonResult.success(responseData);
}
```

### 🔄 登录流程完整时序图 (前端适配版)
```
前端登录页面 → 主通知服务 → Mock School API → 数据库 → 响应链
     ↓              ↓              ↓           ↓        ↓
1. 用户输入凭据    2. 调用Mock API   3. 验证用户    4. 查询权限  5. 标准化响应
   ↓              ↓              ↓           ↓        ↓
6. 前端适配处理 ← 7. Token+用户信息 ← 8. 学校身份确认 ← 9. 权限查询 ← 10. 构建响应

前端适配处理包括：
- 转换Token格式为前端期望结构
- 提取用户信息并存储到localStorage  
- 设置认证状态并跳转到主页面
```

### 📱 前端热重载说明
- ✅ **无需重启前端服务** - Vite开发服务器自动检测文件修改并热重载
- ✅ **实时预览修改效果** - 保存代码后浏览器自动刷新相关组件
- ✅ **保持开发状态** - 登录状态和调试信息保持不变

## 🚀 当前状态

### ✅ 已完成功能
- 双服务架构稳定运行 (48081 + 48082) ✅
- Mock School API认证接口正常工作 ✅  
- 前端登录适配代码完成 ✅
- TypeScript类型定义更新 ✅
- API接口路径配置完成 ✅
- 用户信息获取接口实现 ✅
- 数据库基础表结构创建 ✅

### 🔧 当前测试状态
- **双服务运行验证**: ✅ PASSED
  - Mock School API (48082): 正常响应
  - 主通知服务 (48081): 正常响应
  - 基础健康检查通过

- **Mock API认证测试**: ✅ PASSED  
  ```bash
  # 测试结果
  POST /mock-school-api/auth/authenticate
  Input: {"username":"admin","password":"admin123"}
  Output: {"code":200,"data":{"userId":"ADMIN_001","username":"admin",...}}
  ```

- **主服务登录接口测试**: ⚠️ 系统异常
  ```bash
  # 当前问题
  POST /admin-api/server/notification/login  
  返回: {"code":500,"data":null,"msg":"系统异常"}
  ```

### 🔧 下一步计划
1. 调试主通知服务登录接口的异常问题
2. 确保主服务能正确调用Mock API
3. 测试前端完整登录流程
4. 验证Token存储和页面跳转
5. 测试用户信息获取和权限验证

### 📋 待解决问题
- 主服务登录接口500错误（需要检查日志和调试）
- 前端登录流程完整测试
- 用户权限验证机制测试

---

**重要提醒**: 这是我们自主开发的通知系统，拥有完整的业务逻辑。前端框架必须适配我们的业务需求，而不是反过来修改我们的核心业务逻辑！