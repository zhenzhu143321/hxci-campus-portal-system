# 智能通知系统两步认证架构技术实现指南

## 🎯 **两步认证架构设计**

### 核心设计原则
**分离关注点**: 身份认证 ≠ 业务权限验证
- **Step 1 (Mock API)**: Who you are - 身份验证
- **Step 2 (Main Service)**: What you can do - 权限验证 + 业务操作

### 🏗️ **标准认证流程图**

```mermaid
sequenceDiagram
    participant U as 用户前端
    participant M as Mock School API<br/>(48082)
    participant N as 主通知服务<br/>(48081)
    participant D as 数据库

    Note over U,D: Phase 1: 身份认证 (Who you are)
    U->>M: 1. POST /auth/login<br/>{employeeId, name, password}
    M->>D: 验证用户身份
    D-->>M: 用户基础信息
    M->>M: 生成JWT Token<br/>(包含: userId, roleCode)
    M-->>U: {token, userInfo}<br/>不包含具体权限

    Note over U,D: Phase 2: 权限验证 + 业务操作 (What you can do)
    U->>N: 2. POST /admin-api/infra/messages/publish<br/>Header: Authorization: Bearer {token}
    N->>N: 3. JWT Token验证<br/>(提取userId, roleCode)
    N->>N: 4. 权限验证<br/>(基于roleCode + 操作类型)
    alt 有权限
        N->>D: 5. 执行业务操作
        D-->>N: 操作结果
        N-->>U: {success: true, data}
    else 无权限
        N-->>U: {error: "权限不足"}
    end
```

## 🔧 **技术实现详解**

### Phase 1: Mock School API 重构 (身份认证专责)

#### 1.1 精简的认证控制器
```java
@RestController
@RequestMapping("/mock-school-api/auth")
@Slf4j
public class AuthenticationController {
    
    private final AuthenticationService authService;
    private final JwtTokenService jwtTokenService;
    
    /**
     * 身份认证接口 - 仅验证身份，不返回权限
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody @Valid LoginRequest request) {
        
        try {
            // 1. 验证用户身份（工号+姓名+密码）
            MockSchoolUser user = authService.authenticateUser(
                request.getEmployeeId(), 
                request.getName(), 
                request.getPassword()
            );
            
            // 2. 生成JWT Token（只包含基础用户信息）
            String token = jwtTokenService.generateToken(user.getUserId(), user.getRoleCode());
            
            // 3. 返回认证结果（不包含具体权限列表）
            AuthenticationResponse response = AuthenticationResponse.builder()
                .success(true)
                .token(token)
                .userInfo(UserInfo.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .roleCode(user.getRoleCode())
                    .build())
                .build();
                
            log.info("用户认证成功: userId={}, roleCode={}", user.getUserId(), user.getRoleCode());
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationException e) {
            log.warn("用户认证失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthenticationResponse.builder()
                    .success(false)
                    .message("用户名或密码错误")
                    .build());
        }
    }
    
    /**
     * Token验证接口 - 仅验证Token有效性
     */
    @PostMapping("/verify-token")
    public ResponseEntity<TokenVerificationResponse> verifyToken(
            @RequestBody @Valid TokenRequest request) {
        
        try {
            // 解析Token获取用户信息
            JwtClaims claims = jwtTokenService.parseToken(request.getToken());
            
            TokenVerificationResponse response = TokenVerificationResponse.builder()
                .valid(true)
                .userId(claims.getUserId())
                .roleCode(claims.getRoleCode())
                .expiresAt(claims.getExpiresAt())
                .build();
                
            return ResponseEntity.ok(response);
            
        } catch (JwtException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return ResponseEntity.ok(TokenVerificationResponse.builder()
                .valid(false)
                .message("Token无效或已过期")
                .build());
        }
    }
}
```

#### 1.2 JWT Token服务优化
```java
@Service
@Slf4j
public class JwtTokenService {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400}") // 默认24小时
    private long jwtExpiration;
    
    /**
     * 生成JWT Token - 只包含必要的身份信息
     */
    public String generateToken(String userId, String roleCode) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("roleCode", roleCode)
            .claim("tokenType", "ACCESS_TOKEN")
            .setIssuedAt(now)
            .setExpiresAt(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * 解析JWT Token
     */
    public JwtClaims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
            
            return JwtClaims.builder()
                .userId(claims.getSubject())
                .roleCode(claims.get("roleCode", String.class))
                .issuedAt(claims.getIssuedAt())
                .expiresAt(claims.getExpiration())
                .build();
                
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token已过期");
        } catch (JwtException e) {
            throw new JwtException("Token解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证Token是否有效
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### Phase 2: 主通知服务重构 (权限验证+业务逻辑)

#### 2.1 Spring Security配置修复
```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfiguration {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtTokenFilter jwtTokenFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // 禁用CSRF（因为使用JWT Token）
            .csrf(csrf -> csrf.disable())
            
            // 配置Session策略
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置路径权限
            .authorizeHttpRequests(auth -> auth
                // 健康检查接口允许匿名访问
                .requestMatchers("/admin-api/**/health", "/admin-api/**/ping").permitAll()
                
                // 通知相关接口需要认证
                .requestMatchers("/admin-api/infra/messages/**").authenticated()
                .requestMatchers("/admin-api/server/notification/**").authenticated()
                
                // 其他接口需要认证
                .anyRequest().authenticated()
            )
            
            // 配置异常处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            
            // 添加JWT过滤器
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### 2.2 JWT认证过滤器
```java
@Component
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {
    
    private final MockSchoolApiClient mockApiClient;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 通过Mock API验证Token
                TokenVerificationResponse verification = mockApiClient.verifyToken(token);
                
                if (verification.isValid()) {
                    // 创建Authentication对象
                    UserPrincipal userPrincipal = UserPrincipal.builder()
                        .userId(verification.getUserId())
                        .roleCode(verification.getRoleCode())
                        .build();
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, getAuthorities(verification.getRoleCode())
                        );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("用户认证成功: userId={}", verification.getUserId());
                } else {
                    log.warn("Token验证失败: {}", verification.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("JWT Token处理异常", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(String roleCode) {
        // 将roleCode转换为Spring Security权限
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }
}
```

#### 2.3 权限验证服务
```java
@Service
@Slf4j
public class NotificationPermissionService {
    
    /**
     * 检查发布通知权限
     */
    public boolean hasPublishPermission(String userId, String roleCode, NotificationLevel level) {
        log.debug("权限检查: userId={}, roleCode={}, level={}", userId, roleCode, level);
        
        switch (roleCode.toUpperCase()) {
            case "PRINCIPAL":
                // 校长可以发布所有级别通知
                return true;
                
            case "ACADEMIC_ADMIN":
                // 教务主任可以发布重要、常规、提醒通知
                return level != NotificationLevel.EMERGENCY;
                
            case "TEACHER":
            case "CLASS_TEACHER":
                // 教师可以发布常规、提醒通知
                return level == NotificationLevel.REGULAR || level == NotificationLevel.REMINDER;
                
            case "STUDENT":
            default:
                // 学生无发布权限
                return false;
        }
    }
    
    /**
     * 检查访问消息权限
     */
    public boolean hasAccessPermission(String userId, String roleCode, Long messageId) {
        // 获取消息信息
        NotificationMessage message = getMessageById(messageId);
        if (message == null) {
            return false;
        }
        
        // 检查用户是否在目标受众范围内
        return isUserInTargetAudience(userId, roleCode, message.getTargetRoles());
    }
    
    /**
     * 获取用户可见的消息列表过滤条件
     */
    public MessageFilter getUserMessageFilter(String userId, String roleCode) {
        return MessageFilter.builder()
            .userId(userId)
            .allowedRoles(getAllowedRolesForUser(roleCode))
            .build();
    }
    
    private List<String> getAllowedRolesForUser(String roleCode) {
        // 根据角色层级返回可见的消息角色范围
        switch (roleCode.toUpperCase()) {
            case "PRINCIPAL":
                return Arrays.asList("PRINCIPAL", "ACADEMIC_ADMIN", "TEACHER", "CLASS_TEACHER", "STUDENT");
            case "ACADEMIC_ADMIN":
                return Arrays.asList("ACADEMIC_ADMIN", "TEACHER", "CLASS_TEACHER", "STUDENT");
            case "TEACHER":
            case "CLASS_TEACHER":
                return Arrays.asList("TEACHER", "CLASS_TEACHER", "STUDENT");
            case "STUDENT":
                return Arrays.asList("STUDENT");
            default:
                return Collections.emptyList();
        }
    }
}
```

#### 2.4 通知控制器重构
```java
@RestController
@RequestMapping("/admin-api/infra/messages")
@Slf4j
@Validated
public class NotificationMessageController {
    
    private final NotificationMessageService messageService;
    private final NotificationPermissionService permissionService;
    
    /**
     * 发布通知 - 需要权限验证
     */
    @PostMapping("/publish")
    public CommonResult<Long> publishNotification(
            @RequestBody @Valid NotificationPublishRequest request) {
        
        // 获取当前认证用户
        UserPrincipal currentUser = getCurrentUser();
        
        // 检查发布权限
        boolean hasPermission = permissionService.hasPublishPermission(
            currentUser.getUserId(), 
            currentUser.getRoleCode(), 
            request.getLevel()
        );
        
        if (!hasPermission) {
            log.warn("用户权限不足: userId={}, roleCode={}, level={}", 
                currentUser.getUserId(), currentUser.getRoleCode(), request.getLevel());
            return CommonResult.error(403, "权限不足，无法发布此级别的通知");
        }
        
        // 执行发布逻辑
        Long messageId = messageService.publishNotification(request, currentUser);
        
        log.info("通知发布成功: messageId={}, publisher={}", messageId, currentUser.getUserId());
        return CommonResult.success(messageId);
    }
    
    /**
     * 获取通知列表 - 按权限过滤
     */
    @GetMapping("/list")
    public CommonResult<PageResult<NotificationMessageVO>> getNotificationList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status) {
        
        UserPrincipal currentUser = getCurrentUser();
        
        // 获取用户权限过滤条件
        MessageFilter filter = permissionService.getUserMessageFilter(
            currentUser.getUserId(), 
            currentUser.getRoleCode()
        );
        filter.setLevel(level);
        filter.setStatus(status);
        
        PageResult<NotificationMessageVO> result = messageService.getUserMessages(filter, page, size);
        
        log.debug("获取消息列表: userId={}, count={}", currentUser.getUserId(), result.getTotal());
        return CommonResult.success(result);
    }
    
    /**
     * 获取通知详情
     */
    @GetMapping("/{messageId}")
    public CommonResult<NotificationMessageDetailVO> getNotificationDetail(
            @PathVariable Long messageId) {
        
        UserPrincipal currentUser = getCurrentUser();
        
        // 检查访问权限
        boolean hasAccess = permissionService.hasAccessPermission(
            currentUser.getUserId(), 
            currentUser.getRoleCode(), 
            messageId
        );
        
        if (!hasAccess) {
            return CommonResult.error(403, "无权访问此通知");
        }
        
        NotificationMessageDetailVO detail = messageService.getMessageDetail(messageId, currentUser.getUserId());
        return CommonResult.success(detail);
    }
    
    /**
     * 确认阅读通知
     */
    @PostMapping("/{messageId}/confirm")
    public CommonResult<Void> confirmMessage(@PathVariable Long messageId) {
        UserPrincipal currentUser = getCurrentUser();
        
        messageService.confirmMessage(messageId, currentUser.getUserId());
        log.info("用户确认消息: messageId={}, userId={}", messageId, currentUser.getUserId());
        
        return CommonResult.success();
    }
    
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new BusinessException("用户未认证");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }
}
```

### Phase 3: 前端集成 (标准两步认证)

#### 3.1 认证服务重构
```javascript
class AuthenticationService {
    
    /**
     * Step 1: 身份认证
     */
    async login(employeeId, name, password) {
        try {
            const response = await fetch('/mock-school-api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    employeeId: employeeId,
                    name: name,
                    password: password
                })
            });
            
            const result = await response.json();
            
            if (result.success) {
                // 保存认证信息
                localStorage.setItem('access_token', result.token);
                localStorage.setItem('user_info', JSON.stringify(result.userInfo));
                
                // 触发登录成功事件
                this.onLoginSuccess(result.userInfo);
                
                return {
                    success: true,
                    token: result.token,
                    userInfo: result.userInfo
                };
            } else {
                return {
                    success: false,
                    message: result.message || '登录失败'
                };
            }
        } catch (error) {
            console.error('登录请求失败:', error);
            return {
                success: false,
                message: '网络错误，请稍后重试'
            };
        }
    }
    
    /**
     * 获取当前Token
     */
    getToken() {
        return localStorage.getItem('access_token');
    }
    
    /**
     * 获取当前用户信息
     */
    getCurrentUser() {
        const userInfo = localStorage.getItem('user_info');
        return userInfo ? JSON.parse(userInfo) : null;
    }
    
    /**
     * 检查登录状态
     */
    isLoggedIn() {
        return !!this.getToken();
    }
    
    /**
     * 退出登录
     */
    logout() {
        localStorage.removeItem('access_token');
        localStorage.removeItem('user_info');
        window.location.href = '/login.html';
    }
    
    /**
     * 登录成功回调
     */
    onLoginSuccess(userInfo) {
        // 更新UI显示用户信息
        this.updateUserDisplay(userInfo);
        
        // 可以添加其他登录后的初始化操作
        console.log('用户登录成功:', userInfo);
    }
    
    updateUserDisplay(userInfo) {
        const userNameElement = document.getElementById('userName');
        const userRoleElement = document.getElementById('userRole');
        
        if (userNameElement) {
            userNameElement.textContent = userInfo.username;
        }
        if (userRoleElement) {
            userRoleElement.textContent = this.getRoleDisplayName(userInfo.roleCode);
        }
    }
    
    getRoleDisplayName(roleCode) {
        const roleMap = {
            'PRINCIPAL': '校长',
            'ACADEMIC_ADMIN': '教务主任',
            'TEACHER': '任课教师',
            'CLASS_TEACHER': '班主任',
            'STUDENT': '学生'
        };
        return roleMap[roleCode] || roleCode;
    }
}
```

#### 3.2 通知服务重构
```javascript
class NotificationService {
    
    constructor(authService) {
        this.authService = authService;
    }
    
    /**
     * Step 2: 使用Token进行业务操作
     */
    async publishNotification(notificationData) {
        const token = this.authService.getToken();
        if (!token) {
            throw new Error('用户未登录');
        }
        
        try {
            const response = await fetch('/admin-api/infra/messages/publish', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                    'tenant-id': '1'
                },
                body: JSON.stringify(notificationData)
            });
            
            const result = await response.json();
            
            if (response.status === 401) {
                // Token过期，需要重新登录
                this.authService.logout();
                throw new Error('登录已过期，请重新登录');
            }
            
            if (response.status === 403) {
                throw new Error(result.msg || '权限不足');
            }
            
            if (result.code === 0) {
                return {
                    success: true,
                    messageId: result.data,
                    message: '通知发布成功'
                };
            } else {
                throw new Error(result.msg || '发布失败');
            }
        } catch (error) {
            console.error('发布通知失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取通知列表
     */
    async getNotificationList(page = 1, size = 10, filters = {}) {
        const token = this.authService.getToken();
        if (!token) {
            throw new Error('用户未登录');
        }
        
        const params = new URLSearchParams({
            page: page,
            size: size,
            ...filters
        });
        
        try {
            const response = await fetch(`/admin-api/infra/messages/list?${params}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'tenant-id': '1'
                }
            });
            
            if (response.status === 401) {
                this.authService.logout();
                throw new Error('登录已过期，请重新登录');
            }
            
            const result = await response.json();
            
            if (result.code === 0) {
                return result.data;
            } else {
                throw new Error(result.msg || '获取通知列表失败');
            }
        } catch (error) {
            console.error('获取通知列表失败:', error);
            throw error;
        }
    }
    
    /**
     * 确认阅读通知
     */
    async confirmMessage(messageId) {
        const token = this.authService.getToken();
        if (!token) {
            throw new Error('用户未登录');
        }
        
        try {
            const response = await fetch(`/admin-api/infra/messages/${messageId}/confirm`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'tenant-id': '1'
                }
            });
            
            if (response.status === 401) {
                this.authService.logout();
                throw new Error('登录已过期，请重新登录');
            }
            
            const result = await response.json();
            
            if (result.code === 0) {
                return { success: true };
            } else {
                throw new Error(result.msg || '确认失败');
            }
        } catch (error) {
            console.error('确认消息失败:', error);
            throw error;
        }
    }
}
```

## 🧪 **测试验证方案**

### 单步测试流程
```bash
# 1. 测试身份认证 (Mock API)
curl -X POST "http://localhost:48082/mock-school-api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP001",
    "name": "张校长", 
    "password": "admin123"
  }'

# 期望返回:
# {
#   "success": true,
#   "token": "eyJhbGciOiJIUzUxMiJ9...",
#   "userInfo": {
#     "userId": "EMP001",
#     "username": "张校长",
#     "roleCode": "PRINCIPAL"
#   }
# }

# 2. 测试权限验证 + 业务操作 (主服务)
curl -X POST "http://localhost:48081/admin-api/infra/messages/publish" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "tenant-id: 1" \
  -d '{
    "title": "测试通知",
    "content": "这是一条测试通知",
    "level": "IMPORTANT",
    "targetRoles": ["TEACHER", "STUDENT"]
  }'

# 期望返回:
# {
#   "code": 0,
#   "data": 12345,
#   "msg": ""
# }
```

### 权限测试矩阵验证
| 测试用户 | 角色 | 通知级别 | 期望结果 | 验证命令 |
|----------|------|----------|----------|----------|
| 张校长 | PRINCIPAL | EMERGENCY | ✅ 成功 | 使用校长Token发布紧急通知 |
| 李主任 | ACADEMIC_ADMIN | EMERGENCY | ❌ 403权限不足 | 使用主任Token发布紧急通知 |
| 王老师 | TEACHER | REGULAR | ✅ 成功 | 使用教师Token发布常规通知 |
| 小明 | STUDENT | REMINDER | ❌ 403权限不足 | 使用学生Token发布提醒 |

## 📋 **部署检查清单**

### 环境配置检查
- [ ] Mock API服务正常启动 (端口48082)
- [ ] 主通知服务正常启动 (端口48081)
- [ ] JWT Token密钥已配置
- [ ] 数据库连接正常
- [ ] Redis缓存连接正常

### 功能验证检查
- [ ] 用户可以成功登录获得Token
- [ ] Token可以通过验证接口验证
- [ ] 不同角色的权限控制正确
- [ ] Spring Security不再阻止合法请求
- [ ] 异常情况处理正确（Token过期、权限不足等）

### 安全性检查
- [ ] JWT Token包含必要信息且签名正确
- [ ] Token有合理的过期时间
- [ ] 所有API调用都需要有效Token
- [ ] 权限验证覆盖所有业务操作
- [ ] 敏感操作有审计日志

这个两步认证架构确保了：
1. **职责分离**: 身份认证和权限验证分离
2. **安全性**: JWT Token机制保证安全
3. **扩展性**: 易于集成真实学校API
4. **维护性**: 清晰的代码结构便于维护

---
*📝 文档创建：2025年8月9日 | 🎯 用途：技术实现指导 | 📊 状态：待实施*