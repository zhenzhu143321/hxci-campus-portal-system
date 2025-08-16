# Mock School API 验证环境设计方案

## 🎯 设计目标

基于前面的开发经验教训，**优先搭建Mock验证环境**，避免在真实API集成时重复遇到认证问题。

### 关键原则
- **简单优先**: 最小化实现复杂度
- **业务聚焦**: 专注教育场景的角色权限验证
- **平滑切换**: 为后续真实API集成做准备

## 🏗️ 架构设计

### Mock服务架构
```
yudao-boot-mini/
├── yudao-mock-school-api/          # 🆕 新增Mock服务模块
│   ├── src/main/java/
│   │   └── cn/iocoder/yudao/mock/school/
│   │       ├── controller/         # Mock API控制器
│   │       ├── service/           # Mock业务逻辑
│   │       ├── entity/            # Mock数据实体
│   │       └── config/            # Mock服务配置
│   └── pom.xml                    # Mock模块依赖
└── yudao-server/                  # 通知系统主服务
    └── src/main/java/.../controller/
        └── NotificationController.java  # 已完成✅
```

### 服务通信设计
```
通知系统 → Mock School API → Mock用户数据 → 权限验证结果
    ↓              ↓               ↓              ↓
[发布通知]    [验证token]      [查询角色]      [返回权限]
```

## 📊 Mock数据模型设计

### 1. Mock用户表（mock_school_users）
```sql
CREATE TABLE mock_school_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'School系统用户ID',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    department_id BIGINT COMMENT '部门ID',
    department_name VARCHAR(100) COMMENT '部门名称',
    permissions JSON COMMENT '权限列表',
    token VARCHAR(200) COMMENT 'Mock Token',
    token_expires_time DATETIME COMMENT 'Token过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 2. Mock角色权限表（mock_role_permissions）
```sql
CREATE TABLE mock_role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(30) NOT NULL,
    permission_code VARCHAR(50) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    notification_levels VARCHAR(20) COMMENT '可发布通知级别: 1,2,3,4',
    target_scope VARCHAR(100) COMMENT '通知范围: ALL,DEPARTMENT,CLASS等',
    approval_required TINYINT DEFAULT 0 COMMENT '是否需要审批',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_code, permission_code)
);
```

### 3. 预置测试数据
```sql
-- 插入测试用户
INSERT INTO mock_school_users (username, user_id, role_code, role_name, department_id, department_name, permissions, token) VALUES
('校长张三', 'principal_001', 'PRINCIPAL', '校长', 1, '校长办公室', '["EMERGENCY_NOTIFY","IMPORTANT_NOTIFY","REGULAR_NOTIFY","REMINDER_NOTIFY"]', 'mock_token_principal_001'),
('教务处长李四', 'academic_admin_001', 'ACADEMIC_ADMIN', '教务处长', 2, '教务处', '["EMERGENCY_NOTIFY","IMPORTANT_NOTIFY","REGULAR_NOTIFY","REMINDER_NOTIFY"]', 'mock_token_academic_001'),
('软件学院院长王五', 'dean_001', 'DEAN', '学院院长', 3, '软件学院', '["EMERGENCY_NOTIFY","IMPORTANT_NOTIFY","REGULAR_NOTIFY","REMINDER_NOTIFY"]', 'mock_token_dean_001'),
('辅导员赵六', 'counselor_001', 'COUNSELOR', '辅导员', 4, '软件学院学生工作办', '["REGULAR_NOTIFY","REMINDER_NOTIFY"]', 'mock_token_counselor_001'),
('任课教师孙七', 'teacher_001', 'TEACHER', '任课教师', 5, '软件工程系', '["REGULAR_NOTIFY","REMINDER_NOTIFY"]', 'mock_token_teacher_001'),
('学生张同学', 'student_001', 'STUDENT', '学生', 6, '软件工程2021级1班', '[]', 'mock_token_student_001');

-- 插入角色权限
INSERT INTO mock_role_permissions (role_code, permission_code, permission_name, notification_levels, target_scope, approval_required) VALUES
('PRINCIPAL', 'NOTIFY_ALL', '全校通知权限', '1,2,3,4', 'ALL_SCHOOL', 0),
('ACADEMIC_ADMIN', 'NOTIFY_ACADEMIC', '教务通知权限', '1,2,3,4', 'ACADEMIC_RELATED', 0),
('DEAN', 'NOTIFY_COLLEGE', '学院通知权限', '1,2,3,4', 'COLLEGE_SCOPE', 0),
('COUNSELOR', 'NOTIFY_STUDENTS', '学生通知权限', '3,4', 'STUDENT_SCOPE', 1),
('TEACHER', 'NOTIFY_COURSE', '课程通知权限', '3,4', 'COURSE_SCOPE', 0),
('STUDENT', 'VIEW_NOTIFY', '查看通知权限', '', 'SELF_ONLY', 0);
```

## 🔌 Mock API接口设计

### 1. Token验证接口
```java
@RestController
@RequestMapping("/mock-school-api/auth")
public class MockAuthController {
    
    /**
     * Token验证接口
     * POST /mock-school-api/auth/verify
     */
    @PostMapping("/verify")
    public MockApiResponse<UserInfo> verifyToken(@RequestBody TokenVerifyRequest request) {
        // 模拟token验证逻辑
        MockUser user = mockUserService.findByToken(request.getToken());
        if (user == null || isTokenExpired(user.getTokenExpiresTime())) {
            return MockApiResponse.error(401, "Token无效或已过期");
        }
        
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRoleCode(user.getRoleCode());
        userInfo.setRoleName(user.getRoleName());
        userInfo.setPermissions(user.getPermissions());
        
        return MockApiResponse.success(userInfo);
    }
    
    /**
     * 获取用户权限接口
     * GET /mock-school-api/auth/permissions/{userId}
     */
    @GetMapping("/permissions/{userId}")
    public MockApiResponse<List<Permission>> getUserPermissions(@PathVariable String userId) {
        // 返回用户权限列表
        List<Permission> permissions = mockUserService.getUserPermissions(userId);
        return MockApiResponse.success(permissions);
    }
}
```

### 2. 用户信息查询接口
```java
@RestController
@RequestMapping("/mock-school-api/users")
public class MockUserController {
    
    /**
     * 根据角色获取用户列表
     * GET /mock-school-api/users/by-role/{roleCode}
     */
    @GetMapping("/by-role/{roleCode}")
    public MockApiResponse<List<UserInfo>> getUsersByRole(@PathVariable String roleCode) {
        List<UserInfo> users = mockUserService.findUsersByRole(roleCode);
        return MockApiResponse.success(users);
    }
    
    /**
     * 根据部门获取用户列表  
     * GET /mock-school-api/users/by-department/{departmentId}
     */
    @GetMapping("/by-department/{departmentId}")
    public MockApiResponse<List<UserInfo>> getUsersByDepartment(@PathVariable Long departmentId) {
        List<UserInfo> users = mockUserService.findUsersByDepartment(departmentId);
        return MockApiResponse.success(users);
    }
}
```

### 3. 通知权限验证接口
```java
@RestController
@RequestMapping("/mock-school-api/notification")
public class MockNotificationAuthController {
    
    /**
     * 验证通知发布权限
     * POST /mock-school-api/notification/verify-publish-permission
     */
    @PostMapping("/verify-publish-permission")
    public MockApiResponse<PermissionResult> verifyPublishPermission(
            @RequestBody NotificationPermissionRequest request) {
        
        // request包含: userId, notificationLevel, targetScope
        PermissionResult result = mockNotificationService.verifyPublishPermission(
            request.getUserId(), 
            request.getNotificationLevel(), 
            request.getTargetScope()
        );
        
        return MockApiResponse.success(result);
    }
}
```

## 🔧 集成实现方案

### 1. 通知系统集成Mock API
```java
@Service
public class MockSchoolApiIntegration {
    
    @Value("${mock.school-api.base-url:http://localhost:48082}")
    private String mockApiBaseUrl;
    
    /**
     * 验证用户token并获取权限信息
     */
    public UserInfo verifyUserToken(String token) {
        try {
            String url = mockApiBaseUrl + "/mock-school-api/auth/verify";
            TokenVerifyRequest request = new TokenVerifyRequest(token);
            
            MockApiResponse<UserInfo> response = restTemplate.postForObject(
                url, request, MockApiResponse.class);
                
            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new AuthenticationException("Token验证失败");
        } catch (Exception e) {
            log.error("调用Mock School API失败", e);
            throw new AuthenticationException("认证服务不可用");
        }
    }
    
    /**
     * 验证通知发布权限
     */
    public boolean canPublishNotification(String userId, int notificationLevel, String targetScope) {
        try {
            String url = mockApiBaseUrl + "/mock-school-api/notification/verify-publish-permission";
            NotificationPermissionRequest request = new NotificationPermissionRequest(
                userId, notificationLevel, targetScope);
                
            MockApiResponse<PermissionResult> response = restTemplate.postForObject(
                url, request, MockApiResponse.class);
                
            return response != null && response.isSuccess() && 
                   response.getData().isPermissionGranted();
        } catch (Exception e) {
            log.error("权限验证失败", e);
            return false; // 默认拒绝
        }
    }
}
```

### 2. 更新NotificationController
```java
@RestController
@RequestMapping("/admin-api/server/notification")
@Slf4j
public class NotificationController {
    
    @Autowired
    private MockSchoolApiIntegration mockSchoolApi;
    
    /**
     * 发布通知接口（集成Mock权限验证）
     * POST /admin-api/server/notification/publish
     */
    @PostMapping("/publish")
    @PermitAll  // 先保持PermitAll，内部调用Mock API验证
    public CommonResult<String> publishNotification(
            @RequestHeader("Authorization") String authToken,
            @RequestBody NotificationPublishRequest request) {
        
        try {
            // 1. 验证用户token
            UserInfo userInfo = mockSchoolApi.verifyUserToken(authToken);
            if (userInfo == null) {
                return CommonResult.error(401, "用户认证失败");
            }
            
            // 2. 验证通知发布权限
            boolean hasPermission = mockSchoolApi.canPublishNotification(
                userInfo.getUserId(), 
                request.getNotificationLevel(), 
                request.getTargetScope()
            );
            
            if (!hasPermission) {
                return CommonResult.error(403, "没有发布此类型通知的权限");
            }
            
            // 3. 发布通知
            log.info("用户 {} 发布通知: {}", userInfo.getUsername(), request.getTitle());
            
            // TODO: 实现实际的通知发布逻辑
            
            return CommonResult.success("通知发布成功");
            
        } catch (Exception e) {
            log.error("通知发布失败", e);
            return CommonResult.error(500, "通知发布失败: " + e.getMessage());
        }
    }
}
```

## 📋 开发任务分解

### Phase 1: Mock服务搭建（2天）
- [ ] 创建yudao-mock-school-api模块
- [ ] 设计Mock数据库表结构  
- [ ] 实现Mock API控制器
- [ ] 插入测试数据

### Phase 2: 集成开发（1天）
- [ ] 开发MockSchoolApiIntegration服务
- [ ] 更新NotificationController集成Mock API
- [ ] 配置Mock服务端口（48082）

### Phase 3: 测试验证（1天）
- [ ] 编写不同角色权限测试用例
- [ ] 验证token认证流程
- [ ] 测试通知发布权限控制
- [ ] 完整业务流程演示

## 🔄 后续真实API切换策略

### 配置化切换
```yaml
# application.yaml
mock:
  school-api:
    enabled: true  # 开发阶段使用Mock
    base-url: http://localhost:48082
    
real:
  school-api:
    enabled: false  # 生产阶段使用真实API
    base-url: https://school.example.com/api
    auth-endpoint: /auth/verify
    user-endpoint: /users
```

### 适配器模式
```java
@Component
public class SchoolApiAdapter {
    
    @Autowired
    @Qualifier("mockSchoolApi")
    private SchoolApiService mockSchoolApi;
    
    @Autowired  
    @Qualifier("realSchoolApi")
    private SchoolApiService realSchoolApi;
    
    @Value("${mock.school-api.enabled:true}")
    private boolean mockEnabled;
    
    public UserInfo verifyToken(String token) {
        return mockEnabled ? mockSchoolApi.verifyToken(token) : realSchoolApi.verifyToken(token);
    }
}
```

## 🎯 预期收益

1. **快速验证**: 2天内完成完整认证流程验证
2. **风险降低**: 避免真实API集成时的未知问题
3. **开发效率**: 不依赖外部系统，加快开发速度  
4. **平滑过渡**: 为真实API集成提供清晰的接口规范

通过这个Mock验证环境，我们可以在不依赖真实School API的情况下，快速验证整个通知系统的认证和权限控制逻辑，避免重复前面的开发时间浪费。