# 智能通知系统重构实施任务清单

## 🎯 **总体目标**
解决当前架构中职责混淆问题，实现标准的两步认证：
1. **Mock API**: 专责身份验证，返回JWT Token
2. **主服务**: 专责权限验证 + 业务逻辑处理

## 📋 **任务执行清单 (4天计划)**

### 🔴 **Day 1: Mock API重构 (优先级P0)**
**目标**: 简化Mock API，移除权限验证逻辑，专注身份认证

#### Task 1.1: 清理现有权限验证代码 (1小时)
**执行位置**: `yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/controller/`

```bash
# 需要删除或重构的文件:
# - 删除权限验证相关的Controller方法
# - 简化响应对象结构
# - 清理不必要的Service方法
```

**具体操作**:
- [ ] 删除`/verify-permission`相关接口
- [ ] 简化`AuthResponse`对象，移除`permissions`字段
- [ ] 保留`/login`接口，但只返回Token和基础用户信息

#### Task 1.2: JWT Token服务标准化 (2小时)
**创建文件**: `JwtTokenService.java`

```java
// 实现标准的JWT Token生成和验证
@Service
public class JwtTokenService {
    
    @Value("${jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400}")
    private long jwtExpiration;
    
    public String generateToken(String userId, String roleCode) {
        // 生成包含userId和roleCode的JWT Token
        // Token有效期24小时
    }
    
    public JwtClaims parseToken(String token) {
        // 解析Token，提取用户信息
        // 处理过期和无效Token异常
    }
}
```

**具体操作**:
- [ ] 创建`JwtTokenService`类
- [ ] 添加JWT依赖到`pom.xml`
- [ ] 配置Token密钥和有效期
- [ ] 实现Token生成和解析方法

#### Task 1.3: 身份验证接口重构 (2小时)
**修改文件**: `MockAuthController.java`

```java
@PostMapping("/login")
public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody LoginRequest request) {
    // 1. 验证用户身份（工号+姓名+密码）
    // 2. 生成JWT Token
    // 3. 返回Token和基础用户信息（不包含具体权限）
}

@PostMapping("/verify-token") // 新增接口
public ResponseEntity<TokenVerificationResponse> verifyToken(@RequestBody TokenRequest request) {
    // 验证Token有效性，返回用户基础信息
}
```

**具体操作**:
- [ ] 重构`/login`接口，简化返回数据
- [ ] 新增`/verify-token`接口
- [ ] 创建简化的响应对象
- [ ] 添加异常处理

#### Task 1.4: 测试Mock API简化功能 (1小时)
**测试命令**:
```bash
# 测试身份认证
curl -X POST "http://localhost:48082/mock-school-api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"employeeId": "EMP001", "name": "张校长", "password": "admin123"}'

# 测试Token验证
curl -X POST "http://localhost:48082/mock-school-api/auth/verify-token" \
  -H "Content-Type: application/json" \
  -d '{"token": "获得的JWT_TOKEN"}'
```

**验收标准**:
- [ ] 登录返回JWT Token和基础用户信息
- [ ] Token验证接口正常工作
- [ ] 不再返回权限列表
- [ ] 异常情况正确处理

---

### 🟡 **Day 2: 主服务Spring Security重构 (优先级P0)**
**目标**: 修复Spring Security配置，实现JWT Token认证

#### Task 2.1: 创建JWT认证过滤器 (3小时)
**创建文件**: `yudao-server/src/main/java/cn/iocoder/yudao/server/security/JwtTokenFilter.java`

```java
@Component
public class JwtTokenFilter extends OncePerRequestFilter {
    
    private final MockSchoolApiClient mockApiClient;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        // 1. 从请求头提取JWT Token
        // 2. 调用Mock API验证Token
        // 3. 创建Spring Security Authentication对象
        // 4. 设置到SecurityContext
    }
}
```

**具体操作**:
- [ ] 创建`JwtTokenFilter`类
- [ ] 实现Token提取逻辑
- [ ] 集成Mock API Token验证
- [ ] 创建`UserPrincipal`类存储用户信息
- [ ] 处理认证异常

#### Task 2.2: 修复Spring Security配置 (2小时)
**修改文件**: `yudao-framework/yudao-spring-boot-starter-security/src/main/java/cn/iocoder/yudao/framework/security/config/YudaoWebSecurityConfigurerAdapter.java`

```java
@Configuration
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin-api/infra/messages/**").authenticated()
                .requestMatchers("/admin-api/**/health", "/admin-api/**/ping").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

**具体操作**:
- [ ] 修改路径权限配置
- [ ] 添加JWT过滤器到过滤器链
- [ ] 配置异常处理
- [ ] 禁用默认的认证机制

#### Task 2.3: 创建Mock API客户端 (1小时)
**创建文件**: `MockSchoolApiClient.java`

```java
@Component
public class MockSchoolApiClient {
    
    @Value("${mock.school.api.base-url:http://localhost:48082}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    
    public TokenVerificationResponse verifyToken(String token) {
        // 调用Mock API的Token验证接口
        // 处理网络异常和API异常
    }
}
```

**具体操作**:
- [ ] 创建API客户端类
- [ ] 配置RestTemplate
- [ ] 实现Token验证方法
- [ ] 添加异常处理和重试机制

#### Task 2.4: 测试认证流程 (1小时)
**测试场景**:
1. 无Token访问 → 401 Unauthorized
2. 无效Token访问 → 401 Unauthorized  
3. 有效Token访问 → 正常响应

**测试命令**:
```bash
# 测试无Token访问
curl -X GET "http://localhost:48081/admin-api/infra/messages/list" -H "tenant-id: 1"

# 测试有效Token访问
curl -X GET "http://localhost:48081/admin-api/infra/messages/list" \
  -H "Authorization: Bearer JWT_TOKEN" -H "tenant-id: 1"
```

**验收标准**:
- [ ] Spring Security不再阻止合法请求
- [ ] JWT Token认证正常工作
- [ ] 用户信息正确设置到SecurityContext
- [ ] 异常情况返回正确的HTTP状态码

---

### 🟢 **Day 3: 权限验证与业务逻辑重构 (优先级P1)**
**目标**: 在主服务中实现完整的权限验证逻辑

#### Task 3.1: 创建权限验证服务 (3小时)
**创建文件**: `NotificationPermissionService.java`

```java
@Service
public class NotificationPermissionService {
    
    public boolean hasPublishPermission(String userId, String roleCode, NotificationLevel level) {
        // 根据角色和通知级别判断发布权限
        switch (roleCode) {
            case "PRINCIPAL": return true; // 校长可发布所有级别
            case "ACADEMIC_ADMIN": return level != NotificationLevel.EMERGENCY;
            case "TEACHER": return level == NotificationLevel.REGULAR || level == NotificationLevel.REMINDER;
            case "STUDENT": return false;
        }
    }
    
    public boolean hasAccessPermission(String userId, String roleCode, Long messageId) {
        // 检查用户是否有访问特定消息的权限
    }
}
```

**具体操作**:
- [ ] 创建权限验证服务类
- [ ] 实现发布权限检查逻辑
- [ ] 实现访问权限检查逻辑
- [ ] 添加角色层级逻辑
- [ ] 创建权限相关的枚举和常量

#### Task 3.2: 重构NotificationController (3小时)
**修改文件**: `yudao-server/src/main/java/cn/iocoder/yudao/server/controller/NotificationController.java`

```java
@RestController
@RequestMapping("/admin-api/infra/messages")
public class NotificationMessageController {
    
    @PostMapping("/publish")
    public CommonResult<Long> publishNotification(@RequestBody NotificationPublishRequest request) {
        // 1. 获取当前认证用户
        UserPrincipal currentUser = getCurrentUser();
        
        // 2. 检查发布权限
        boolean hasPermission = permissionService.hasPublishPermission(
            currentUser.getUserId(), currentUser.getRoleCode(), request.getLevel());
            
        if (!hasPermission) {
            return CommonResult.error(403, "权限不足");
        }
        
        // 3. 执行业务逻辑
        Long messageId = messageService.publishNotification(request, currentUser);
        return CommonResult.success(messageId);
    }
}
```

**具体操作**:
- [ ] 重构Controller方法
- [ ] 集成权限验证逻辑
- [ ] 添加用户信息获取方法
- [ ] 实现权限错误处理
- [ ] 添加业务操作日志

#### Task 3.3: 完善消息CRUD接口 (1小时)
**实现接口**:
- [ ] `GET /admin-api/infra/messages/list` - 获取消息列表（按权限过滤）
- [ ] `GET /admin-api/infra/messages/{id}` - 获取消息详情（权限检查）
- [ ] `POST /admin-api/infra/messages/{id}/confirm` - 确认阅读消息
- [ ] `DELETE /admin-api/infra/messages/{id}` - 删除消息（权限检查）

#### Task 3.4: 权限验证测试 (1小时)
**测试矩阵**:
```bash
# 校长发布紧急通知 - 应该成功
curl -X POST "http://localhost:48081/admin-api/infra/messages/publish" \
  -H "Authorization: Bearer PRINCIPAL_TOKEN" \
  -H "Content-Type: application/json" -H "tenant-id: 1" \
  -d '{"title":"紧急通知","level":"EMERGENCY","targetRoles":["ALL"]}'

# 教师发布紧急通知 - 应该失败403
curl -X POST "http://localhost:48081/admin-api/infra/messages/publish" \
  -H "Authorization: Bearer TEACHER_TOKEN" \
  -H "Content-Type: application/json" -H "tenant-id: 1" \
  -d '{"title":"紧急通知","level":"EMERGENCY","targetRoles":["STUDENT"]}'
```

**验收标准**:
- [ ] 不同角色的权限控制正确
- [ ] 权限不足时返回403错误
- [ ] 业务逻辑正常执行
- [ ] 错误信息清晰明确

---

### 🟣 **Day 4: 前端集成与端到端测试 (优先级P1)**
**目标**: 更新前端认证流程，完成整个系统集成

#### Task 4.1: 前端认证服务重构 (2小时)
**修改文件**: HTML测试页面中的JavaScript代码

```javascript
class AuthenticationService {
    async login(employeeId, name, password) {
        // Step 1: 调用Mock API进行身份认证
        const response = await fetch('/mock-school-api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ employeeId, name, password })
        });
        
        if (response.ok) {
            const result = await response.json();
            localStorage.setItem('access_token', result.token);
            localStorage.setItem('user_info', JSON.stringify(result.userInfo));
            return result;
        }
        throw new Error('登录失败');
    }
}

class NotificationService {
    async publishNotification(data) {
        // Step 2: 使用Token调用主服务进行业务操作
        const token = localStorage.getItem('access_token');
        const response = await fetch('/admin-api/infra/messages/publish', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
                'tenant-id': '1'
            },
            body: JSON.stringify(data)
        });
        
        return response.json();
    }
}
```

**具体操作**:
- [ ] 重构登录方法
- [ ] 实现Token存储和管理
- [ ] 更新业务操作方法
- [ ] 添加Token过期处理
- [ ] 改进错误处理和用户提示

#### Task 4.2: 更新HTML测试界面 (2小时)
**修改文件**: 现有的HTML测试页面

**界面更新**:
- [ ] 改进登录表单UI
- [ ] 添加用户信息显示区域
- [ ] 完善通知发布表单
- [ ] 添加消息列表展示
- [ ] 改进错误提示和状态显示

**功能增强**:
- [ ] 添加登录状态检查
- [ ] 实现自动Token刷新
- [ ] 添加权限级别提示
- [ ] 完善用户体验

#### Task 4.3: 端到端功能测试 (2小时)
**测试场景覆盖**:

1. **登录流程测试**:
   - [ ] 正确用户名密码登录成功
   - [ ] 错误密码登录失败
   - [ ] 登录后显示用户信息和角色

2. **权限控制测试**:
   - [ ] 校长可以发布所有级别通知
   - [ ] 教务主任不能发布紧急通知
   - [ ] 教师只能发布常规和提醒通知
   - [ ] 学生无法发布任何通知

3. **业务功能测试**:
   - [ ] 通知发布成功后显示消息ID
   - [ ] 通知列表正确显示（按权限过滤）
   - [ ] 通知详情查看正常
   - [ ] 消息确认功能正常

4. **异常情况测试**:
   - [ ] Token过期时重新登录
   - [ ] 网络异常时错误提示
   - [ ] 权限不足时正确提示
   - [ ] 参数错误时错误处理

#### Task 4.4: 性能和稳定性测试 (1小时)
**测试内容**:
- [ ] 并发登录测试（10个用户同时登录）
- [ ] 批量消息发布测试
- [ ] 长时间运行稳定性测试
- [ ] 内存和CPU使用率监控

**性能指标**:
- [ ] 登录响应时间 < 2秒
- [ ] 消息发布响应时间 < 3秒
- [ ] 消息列表查询 < 2秒
- [ ] 系统内存使用稳定

#### Task 4.5: 文档更新和部署验证 (1小时)
**文档更新**:
- [ ] 更新API接口文档
- [ ] 更新部署说明文档
- [ ] 创建用户操作手册
- [ ] 更新troubleshooting指南

**部署验证**:
- [ ] 验证双服务启动脚本
- [ ] 确认配置文件正确
- [ ] 测试生产环境兼容性
- [ ] 验证日志输出正常

## 🏁 **验收标准总览**

### 功能性验收标准
- [ ] **身份认证**: 用户能够通过工号+姓名+密码成功登录并获得JWT Token
- [ ] **权限验证**: 不同角色用户有正确的操作权限控制
- [ ] **业务功能**: 通知的发布、查看、确认等核心功能正常工作
- [ ] **错误处理**: 各种异常情况都有适当的错误提示和处理

### 技术性验收标准
- [ ] **架构清晰**: Mock API专责身份认证，主服务专责权限和业务
- [ ] **安全可靠**: JWT Token机制安全，权限控制严格
- [ ] **性能良好**: 响应时间符合要求，系统资源使用合理
- [ ] **易于维护**: 代码结构清晰，文档完善

### 用户体验验收标准
- [ ] **操作简便**: 登录和使用流程简单直观
- [ ] **反馈及时**: 操作结果和错误信息及时准确显示
- [ ] **界面友好**: UI界面美观，功能布局合理
- [ ] **稳定可靠**: 长时间使用无异常，功能稳定

## 🚨 **风险预警与应对**

### 高风险项 (需要特别关注)
1. **Spring Security配置复杂** - 准备回滚方案，分步骤测试
2. **JWT Token集成问题** - 提前准备调试工具和日志
3. **权限逻辑错误** - 建立完整的测试用例矩阵

### 应急预案
- **每天结束前创建代码备份**
- **关键配置文件版本控制**
- **准备快速回滚脚本**
- **预留额外调试时间**

## 📊 **执行跟踪**

### 日进度跟踪
- [ ] Day 1完成率: ___% (目标>90%)
- [ ] Day 2完成率: ___% (目标>90%)
- [ ] Day 3完成率: ___% (目标>85%)
- [ ] Day 4完成率: ___% (目标>85%)

### 质量跟踪
- [ ] 单元测试覆盖率: ___%
- [ ] 集成测试通过率: ___%
- [ ] 代码审查完成: ___/___
- [ ] 文档更新完成: ___/___

这个任务清单确保了架构重构的系统性和可执行性，每个任务都有明确的目标、具体的实现步骤和验收标准。

---
*📝 文档创建：2025年8月9日 | 🎯 用途：开发任务指导 | ⏰ 执行周期：4天*