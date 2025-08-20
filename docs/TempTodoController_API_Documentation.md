# 📋 T13待办通知系统 - TempTodoController API文档

> ⚠️ **重要通知 (2025-08-19)**: TempTodoController已被弃用  
> **新API**: 请使用 `NewTodoNotificationController` - 路径为 `/admin-api/test/todo-new/`  
> **弃用原因**: 修复emoji编码问题 + tenant_id缺失问题  
> **前端已更新**: todo.ts已切换到新API路径

## 🎯 项目概述

TempTodoController是专为T13待办通知系统设计的控制器，从TempNotificationController解耦而来，专门处理Level 5待办通知功能。基于TempWeatherController的架构设计，采用双重认证模式。

## 🔑 核心特性

- ✅ **双重认证系统**: @PermitAll + @TenantIgnore + getUserInfoFromMockApi()认证
- 🛡️ **企业级安全**: 完整的输入验证、HTML转义、SQL注入防护
- 📊 **完善的权限控制**: 基于角色的发布权限和查看权限
- 🎯 **前后端契合**: API响应格式完美匹配前端TodoNotificationItem接口
- 📈 **生产就绪**: 完整的错误处理、日志记录、数据验证

## 📋 API接口清单

### 基础测试接口
```
GET /admin-api/test/todo/api/ping
```

### 核心业务接口

#### 1. 📝 获取我的待办列表
```
GET /admin-api/test/todo/api/my-list
```

**参数:**
- `page` (int, 可选): 页码，默认1
- `pageSize` (int, 可选): 每页数量，默认20
- `status` (string, 可选): 状态过滤 (pending/completed/overdue)
- `priority` (string, 可选): 优先级过滤 (low/medium/high)

**响应格式:**
```json
{
  "code": 200,
  "data": {
    "todos": [
      {
        "id": 123,
        "title": "完成课程作业",
        "content": "请完成第5章练习题",
        "level": 5,
        "priority": "high",
        "dueDate": "2025-08-20",
        "status": "pending",
        "assignerName": "张老师",
        "isCompleted": false
      }
    ],
    "pagination": {
      "current": 1,
      "pageSize": 20,
      "total": 15,
      "totalPages": 1
    },
    "user": {
      "username": "系统管理员",
      "roleCode": "SYSTEM_ADMIN",
      "roleName": "系统管理员"
    },
    "timestamp": 1734261110000
  }
}
```

#### 2. ✅ 标记待办完成
```
POST /admin-api/test/todo/api/{id}/complete
```

**路径参数:**
- `id` (long): 待办通知ID

**请求体:** `{}` (空对象)

**响应格式:**
```json
{
  "code": 200,
  "data": {
    "todoId": 123,
    "title": "完成课程作业",
    "completedBy": "学生张三",
    "completedTime": "2025-08-15 14:30:00",
    "isCompleted": true,
    "timestamp": 1734261110000
  }
}
```

#### 3. 📝 发布待办通知
```
POST /admin-api/test/todo/api/publish
```

**请求体:**
```json
{
  "title": "完成课程作业",
  "content": "请在本周内完成第5-7章的练习题",
  "priority": "high",
  "dueDate": "2025-08-20",
  "targetScope": "CLASS"
}
```

**字段说明:**
- `title` (string, 必需): 待办标题，最大200字符
- `content` (string, 必需): 待办内容，最大10000字符
- `priority` (string, 必需): 优先级 (low/medium/high)
- `dueDate` (string, 必需): 截止日期，格式YYYY-MM-DD
- `targetScope` (string, 可选): 目标范围，默认CLASS

**响应格式:**
```json
{
  "code": 200,
  "data": {
    "id": 124,
    "title": "完成课程作业",
    "level": 5,
    "priority": "high",
    "dueDate": "2025-08-20",
    "status": "pending",
    "assignerName": "张老师",
    "targetScope": "CLASS",
    "publishedBy": "张老师",
    "publishedTime": "2025-08-15 14:25:00",
    "timestamp": 1734261110000
  }
}
```

#### 4. 📊 获取待办统计
```
GET /admin-api/test/todo/api/{id}/stats
```

**路径参数:**
- `id` (long): 待办通知ID

**响应格式:**
```json
{
  "code": 200,
  "data": {
    "todoInfo": {
      "id": "123",
      "title": "完成课程作业",
      "publisher_name": "张老师",
      "target_scope": "CLASS",
      "create_time": "2025-08-15 14:25:00",
      "due_date": "2025-08-20"
    },
    "stats": {
      "totalCompleted": 15,
      "studentCompleted": 12,
      "teacherCompleted": 2,
      "classTeacherCompleted": 1
    },
    "recentCompletions": [
      {
        "user_name": "学生李四",
        "user_role": "STUDENT",
        "completed_time": "2025-08-15 16:30:00"
      }
    ],
    "requestedBy": "系统管理员",
    "timestamp": 1734261110000
  }
}
```

## 🔐 认证方式

所有API都需要在请求头中包含以下认证信息：

```http
Authorization: Bearer {jwt_token}
Content-Type: application/json
tenant-id: 1
```

**JWT Token获取:**
1. 调用Mock School API登录: `POST http://localhost:48082/mock-school-api/auth/authenticate`
2. 使用返回的Token进行API调用

## 📊 数据库映射

### 优先级映射
```
前端值 -> 数据库值
"low" -> 1
"medium" -> 2  
"high" -> 3
```

### 状态映射
```
前端值 -> 数据库值
"pending" -> 0
"completed" -> 2
"overdue" -> 3
```

### 表结构
- **主表**: `notification_info` (level=5的记录)
- **完成记录表**: `notification_todo_completion`

## 🎯 权限控制

### 发布权限矩阵
| 角色 | 可发布范围 |
|------|-----------|
| SYSTEM_ADMIN | SCHOOL_WIDE, DEPARTMENT, GRADE, CLASS |
| PRINCIPAL | SCHOOL_WIDE, DEPARTMENT, GRADE, CLASS |
| ACADEMIC_ADMIN | SCHOOL_WIDE, DEPARTMENT, GRADE, CLASS |
| TEACHER | DEPARTMENT, CLASS |
| CLASS_TEACHER | GRADE, CLASS |
| STUDENT | CLASS |

### 查看权限
- **学生**: 只能看到班级和年级相关的待办，以及自己发布的待办
- **其他角色**: 可以查看所有相关范围的待办

## 🧪 测试工具

### 1. 命令行测试脚本
```bash
D:\ClaudeCode\AI_Web\scripts\tests\test_todo_controller.bat
```

### 2. 网页测试界面
```
D:\ClaudeCode\AI_Web\demo\phases\test_todo_controller.html
```

### 3. 手动API测试
```bash
# Ping测试
curl -X GET "http://localhost:48081/admin-api/test/todo/api/ping" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1"

# 获取待办列表  
curl -X GET "http://localhost:48081/admin-api/test/todo/api/my-list?page=1&pageSize=10" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1"
```

## 🔧 技术实现

### 架构特点
- **基于TempWeatherController**: 复用双重认证和错误处理机制
- **安全增强**: 使用SecurityEnhancementUtil进行输入验证和转义
- **企业级日志**: 完整的请求/响应日志记录
- **降级机制**: 异常情况下的优雅降级

### 数据库操作
- **查询**: 使用executeQueryAndReturnList/executeQueryAndReturnSingle
- **更新**: 使用executeSQLUpdate
- **事务**: 自动提交模式，确保数据一致性

### 错误处理
- **参数验证**: 完整的请求参数校验
- **权限检查**: 基于角色的操作权限验证  
- **异常捕获**: 所有异常都会被捕获并返回友好错误信息

## 📅 开发信息

- **创建时间**: 2025-08-15
- **开发者**: Claude AI
- **版本**: T13.1
- **依赖**: TempWeatherController架构、SecurityEnhancementUtil、NotificationScopeManager

## 🚀 部署说明

1. **编译项目**: `mvn clean compile`
2. **启动服务**: 启动yudao-server (48081端口)
3. **启动Mock API**: 启动yudao-mock-school-api (48082端口)
4. **测试验证**: 使用提供的测试工具验证功能

---

**注意**: 本控制器专门用于待办通知功能，与通用通知系统相互独立，确保功能解耦和代码可维护性。