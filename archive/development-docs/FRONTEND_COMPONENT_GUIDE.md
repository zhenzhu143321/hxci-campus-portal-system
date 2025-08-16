# FRONTEND_COMPONENT_GUIDE.md - 前端组件指南

## 🎯 项目概述

**智能通知系统前端组件** - 基于原生HTML+JavaScript的通知管理界面
- **项目名称**: yudao-boot-mini 智能通知系统前端
- **技术栈**: HTML5 + 原生JavaScript + CSS3 (Bootstrap风格)
- **认证方式**: JWT Token双重认证
- **状态**: 🔧 **状态显示BUG已修复** - 原错误将status=2显示为"已拒绝"，现已修正为"待审批"

---

## 📁 核心组件目录结构

```
D:\ClaudeCode\AI_Web\demo\frontend-tests\
├── 📄 notification-list-display-fix.html     # 🔧 通知列表显示修复版 (推荐)
├── 📄 notification-system-test.html          # 🧪 完整系统功能测试
├── 📄 day2-refactoring-test.html            # 📋 权限矩阵验证测试
├── 📄 chinese-encoding-test.html            # 🌏 中文编码测试
└── 📄 test-html-fix.html                    # 🔧 审批流程测试修复版
```

---

## 🔧 **FRONTEND_COMPONENT_GUIDE.md 关键修复说明**

### 🚨 **状态显示BUG修复** (2025-08-11)

#### **问题描述**
- **原问题**: 教务主任发布1级通知后，前端错误显示"已拒绝"状态
- **实际情况**: 后端正确设置status=2(待审批)，问题出在前端状态显示逻辑
- **影响范围**: 所有通知列表显示界面

#### **修复方案**
```javascript
// ❌ 错误的状态显示 (原代码可能存在的问题)
function getStatusText(statusCode) {
    // 可能存在错误的状态映射导致status=2显示为"已拒绝"
}

// ✅ 修复后的正确状态显示函数
function getStatusText(statusCode) {
    switch (parseInt(statusCode)) {
        case 1: return { text: '草稿', class: 'status-pending' };
        case 2: return { text: '⏳ 待审批', class: 'status-pending' }; // 🔧 关键修复
        case 3: return { text: '✅ 已发布', class: 'status-published' };
        case 4: return { text: '已撤回', class: 'status-rejected' };
        case 5: return { text: '已过期', class: 'status-rejected' };
        case 6: return { text: '❌ 已拒绝', class: 'status-rejected' };
        default: return { text: '未知状态(' + statusCode + ')', class: 'status-pending' };
    }
}
```

---

## 🌟 核心组件功能说明

### 1. 📋 **notification-list-display-fix.html** (🔧 状态显示修复版)
**功能**: 修复状态显示错误的通知列表组件
**特点**: 
- ✅ 正确显示 status=2 为"⏳ 待审批"
- 🔒 基于角色的权限过滤
- 🎯 支持4种用户身份切换 (校长/教务主任/教师/学生)
- 📊 详细的状态说明和修复记录

**使用方法**:
```bash
# 1. 确保后端服务运行 (端口 48081, 48082)
# 2. 打开浏览器访问
file:///D:/ClaudeCode/AI_Web/demo/frontend-tests/notification-list-display-fix.html

# 3. 选择身份查看通知列表
# 4. 验证状态显示是否正确
```

### 2. 🧪 **notification-system-test.html** (完整功能测试)
**功能**: 全面的双重认证系统测试组件
**特点**:
- 🔐 JWT Token认证测试
- 📊 权限矩阵验证
- 📝 通知发布功能测试
- 🎯 级别权限测试 (1-4级)

### 3. 📋 **day2-refactoring-test.html** (权限矩阵)
**功能**: 系统性权限验证组件
**特点**:
- 🏆 完整的权限矩阵测试
- 📊 角色-级别权限组合验证
- 🔍 权限拒绝情况测试

### 4. 🌏 **chinese-encoding-test.html** (中文支持)
**功能**: 中文编码兼容性测试
**特点**:
- ✅ UTF-8中文输入支持
- 🔧 编码问题验证和修复

---

## 🔐 **认证系统集成**

### JWT Token获取
```javascript
// 标准认证流程
async function getJwtToken(roleCode) {
    const credentials = {
        'PRINCIPAL': { employeeId: 'PRINCIPAL_001', name: 'Principal-Zhang', password: 'admin123' },
        'ACADEMIC_ADMIN': { employeeId: 'ACADEMIC_ADMIN_001', name: 'Director-Li', password: 'admin123' },
        'TEACHER': { employeeId: 'TEACHER_001', name: 'Teacher-Wang', password: 'admin123' },
        'STUDENT': { employeeId: 'STUDENT_001', name: 'Student-Zhang', password: 'admin123' }
    };

    const response = await fetch(`${MOCK_API_BASE}/mock-school-api/auth/authenticate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials[roleCode])
    });

    return response.data.token;
}
```

### API调用标准格式
```javascript
const response = await fetch(`${API_BASE}/admin-api/test/notification/api/*`, {
    method: 'GET/POST',
    headers: {
        'Authorization': `Bearer ${token}`,      // 🔐 JWT认证
        'Content-Type': 'application/json',
        'tenant-id': '1'                        // ⚠️ 必需！yudao框架要求
    }
});
```

---

## 🚀 **项目运行说明**

### 📋 前置条件
1. **后端服务必须运行**:
   - 主通知服务: http://localhost:48081
   - Mock API服务: http://localhost:48082

2. **服务启动方法**:
   ```bash
   # 打开两个CMD窗口分别执行
   
   # 窗口1: 主服务
   cd /d D:\ClaudeCode\AI_Web\yudao-boot-mini
   mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local
   
   # 窗口2: Mock API
   mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local
   ```

### 🌐 前端访问
```bash
# 方法1: 直接打开HTML文件
file:///D:/ClaudeCode/AI_Web/demo/frontend-tests/notification-list-display-fix.html

# 方法2: 使用本地服务器 (推荐)
# 使用 Live Server 或其他HTTP服务器避免CORS问题
```

---

## 🔧 **故障排除指南**

### 常见问题解决方案

| 问题类型 | 现象 | 解决方案 |
|---------|------|----------|
| **🚨 状态显示错误** | **status=2显示为"已拒绝"** | **使用修复版组件notification-list-display-fix.html** |
| 401 认证失败 | Token无效或已过期 | 检查Mock API服务状态 (端口48082) |
| 404 接口不存在 | API路径错误 | 确认主服务运行 (端口48081) |
| CORS跨域错误 | 浏览器阻止请求 | 使用HTTP服务器而非file://协议 |
| 通知列表为空 | 权限过滤或数据库无数据 | 检查角色权限和数据库内容 |

### 🔍 调试技巧
```javascript
// 1. 开启浏览器开发者工具
// 2. 查看Network标签页的API调用
// 3. 检查Console标签页的错误信息
// 4. 验证Request/Response数据格式
```

---

## 📈 **开发计划 & 待完善功能**

### ✅ **已完成功能 (60%)**
- 🔐 双重认证系统
- 📋 通知列表查看 (状态显示已修复)
- 🎯 权限矩阵验证
- 📝 通知发布功能
- 🏆 审批工作流
- 🔧 状态显示BUG修复

### 🔄 **开发中功能**
- 🎨 Vue 3正式前端项目 (计划中)
- 📱 响应式设计优化
- 🔔 实时通知推送

### 📋 **待开发功能 (40%)**
- ✏️ 通知编辑功能
- 📊 数据统计分析
- 👥 用户管理界面
- 🔧 系统设置面板

---

## 📞 **技术支持**

**维护团队**: Claude Code AI  
**最后更新**: 2025年8月11日  
**版本**: v1.1 (状态显示修复版)

**重要提醒**: 
- 🔧 原前端存在状态显示错误，status=2被错误显示为"已拒绝"
- ✅ 请使用修复版组件 `notification-list-display-fix.html`
- 🎯 后端逻辑完全正确，问题仅在前端显示层

**联系方式**: 通过项目Issues提交问题反馈