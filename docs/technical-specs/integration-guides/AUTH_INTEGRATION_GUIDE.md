# 认证集成指南 (Authentication Integration Guide)

## 🔐 认证架构概览

基于yudao-boot-mini框架的Spring Security + JWT认证体系，为教育机构通知系统提供企业级的安全认证方案。

### 核心架构组件
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端应用       │    │   认证服务       │    │   权限服务       │
│  (Web/Mobile)   │───▶│  Auth Service   │───▶│ Permission Svc  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       ▼
         │              ┌─────────────────┐    ┌─────────────────┐
         └─────────────▶│   JWT Token     │    │   Redis Cache   │
                        │   Management    │    │  (权限缓存)     │
                        └─────────────────┘    └─────────────────┘
```

## 🏫 教育行业权限模型

### 角色层级设计
```java
// 权限层级枚举
public enum PermissionLevel {
    SCHOOL_LEVEL(1, "校级权限"),     // 校长、副校长
    COLLEGE_LEVEL(2, "学院级权限"),  // 院长、党委书记
    DEPT_LEVEL(3, "部门级权限"),     // 系主任、处长
    CLASS_LEVEL(4, "班级权限"),      // 辅导员、班主任
    STUDENT_LEVEL(5, "学生权限");    // 学生、班干部
    
    private final int level;
    private final String description;
}

// 权限继承规则
@Component
public class PermissionInheritance {
    
    /**
     * 权限继承检查
     * 上级角色自动继承下级角色的所有权限
     */
    public boolean hasInheritedPermission(User user, String permission) {
        List<Role> userRoles = user.getRoles();
        
        // 检查直接权限
        if (hasDirectPermission(userRoles, permission)) {
            return true;
        }
        
        // 检查继承权限
        return checkInheritanceChain(userRoles, permission);
    }
}
```

### 多租户权限隔离
```java
// 租户上下文管理
@Component
public class TenantContextHolder {
    
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static Long getTenantId() {
        return TENANT_ID.get();
    }
    
    public static void clear() {
        TENANT_ID.remove();
    }
}

// MyBatis Plus多租户拦截器
@Configuration
public class TenantConfig {
    
    @Bean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(TenantContextHolder.getTenantId().toString());
            }
            
            @Override
            public boolean ignoreTable(String tableName) {
                // 系统表不需要租户隔离
                return "system_config".equals(tableName) || 
                       "system_dict".equals(tableName);
            }
        });
    }
}
```

## 🔑 JWT令牌管理

### 令牌结构设计
```java
// JWT载荷结构
public class JWTPayload {
    private Long userId;           // 用户ID
    private String username;       // 用户名
    private Long tenantId;         // 租户ID
    private List<String> roles;    // 角色列表
    private List<String> permissions; // 权限列表
    private String deviceType;     // 设备类型
    private Long issuedAt;         // 签发时间
    private Long expiresAt;        // 过期时间
    
    // 教育机构特有字段
    private String schoolCode;     // 学校编码
    private String departmentId;   // 部门ID
    private String studentId;      // 学号(学生用户)
    private String teacherId;      // 工号(教职工用户)
}

// JWT工具类
@Component
public class JWTManager {
    
    @Value("${yudao.security.jwt.secret}")
    private String jwtSecret;
    
    @Value("${yudao.security.jwt.expiration:7200}") // 2小时
    private int jwtExpiration;
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tenantId", user.getTenantId());
        claims.put("roles", user.getRoles().stream()
            .map(Role::getCode).collect(Collectors.toList()));
        claims.put("permissions", getPermissions(user));
        claims.put("schoolCode", user.getSchoolCode());
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000L))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * 生成刷新令牌（7天有效期）
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .signWith(SignatureAlgorithm.HS512, jwtSecret + "refresh")
            .compact();
    }
}
```

## 🌐 单点登录(SSO)集成

### CAS集成配置
```yaml
# application-cas.yml
cas:
  server:
    host: https://sso.university.edu.cn
    login: ${cas.server.host}/login
    logout: ${cas.server.host}/logout
  client:
    host: https://notification.university.edu.cn
    service: ${cas.client.host}/login/cas
    
spring:
  security:
    cas:
      server: ${cas.server.host}
      service: ${cas.client.service}
      send-renew: false
```

```java
// CAS安全配置
@Configuration
@EnableWebSecurity
public class CASSecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private CasProperties casProperties;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .requestMatchers("/admin-api/auth/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(casAuthenticationEntryPoint())
            .and()
            .addFilter(casAuthenticationFilter())
            .addFilterBefore(casValidationFilter(), CasAuthenticationFilter.class)
            .addFilterBefore(singleSignOutFilter(), CasAuthenticationFilter.class);
    }
    
    @Bean
    public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
        CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
        entryPoint.setLoginUrl(casProperties.getServer().getLogin());
        entryPoint.setServiceProperties(serviceProperties());
        return entryPoint;
    }
}
```

### OAuth2集成（钉钉/企业微信）
```java
// OAuth2配置
@Configuration
public class OAuth2Config {
    
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            dingTalkClientRegistration(),
            wechatWorkClientRegistration()
        );
    }
    
    private ClientRegistration dingTalkClientRegistration() {
        return ClientRegistration.withRegistrationId("dingtalk")
            .clientId("dingtalk-app-id")
            .clientSecret("dingtalk-app-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/oauth2/callback/dingtalk")
            .authorizationUri("https://oapi.dingtalk.com/connect/oauth2/sns_authorize")
            .tokenUri("https://oapi.dingtalk.com/sns/gettoken")
            .userInfoUri("https://oapi.dingtalk.com/sns/getuserinfo")
            .userNameAttributeName("nick")
            .clientName("DingTalk")
            .build();
    }
}

// OAuth2用户服务
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        // 根据不同平台处理用户信息
        switch (registrationId) {
            case "dingtalk":
                return loadDingTalkUser(userRequest);
            case "wechat-work":
                return loadWechatWorkUser(userRequest);
            default:
                throw new OAuth2AuthenticationException("不支持的OAuth2提供商: " + registrationId);
        }
    }
    
    private OAuth2User loadDingTalkUser(OAuth2UserRequest userRequest) {
        // 调用钉钉API获取用户信息
        // 映射到系统用户
        return new DefaultOAuth2User(authorities, attributes, "nick");
    }
}
```

## 🔒 多因素认证(MFA)

### 短信验证实现
```java
// MFA服务
@Service
public class MFAService {
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 发送短信验证码
     */
    public void sendSMSCode(String mobile, String userId) {
        // 生成6位随机码
        String code = RandomUtil.randomNumbers(6);
        
        // 发送短信
        smsService.sendCode(mobile, code);
        
        // 缓存验证码（5分钟有效）
        String key = "mfa:sms:" + userId;
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 验证短信验证码
     */
    public boolean verifySMSCode(String userId, String code) {
        String key = "mfa:sms:" + userId;
        String cachedCode = (String) redisTemplate.opsForValue().get(key);
        
        if (cachedCode != null && cachedCode.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}

// MFA认证过滤器
@Component
public class MFAAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            User user = (User) auth.getPrincipal();
            
            // 检查是否需要MFA验证
            if (requiresMFA(user) && !isMFACompleted(user)) {
                response.sendError(HttpStatus.PRECONDITION_REQUIRED.value(), "需要多因素认证");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

## 🏛️ 权限控制实现

### 基于注解的权限控制
```java
// 权限注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();
    PermissionLevel level() default PermissionLevel.CLASS_LEVEL;
    boolean requireAll() default false; // true=需要所有权限，false=需要任一权限
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
    boolean requireAll() default false;
}

// 权限切面
@Aspect
@Component
public class PermissionAspect {
    
    @Autowired
    private PermissionService permissionService;
    
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, 
                                  RequirePermission requirePermission) throws Throwable {
        
        User currentUser = getCurrentUser();
        String[] permissions = requirePermission.value();
        
        boolean hasPermission;
        if (requirePermission.requireAll()) {
            hasPermission = permissionService.hasAllPermissions(currentUser, permissions);
        } else {
            hasPermission = permissionService.hasAnyPermission(currentUser, permissions);
        }
        
        if (!hasPermission) {
            throw new AccessDeniedException("权限不足，需要权限: " + Arrays.toString(permissions));
        }
        
        return joinPoint.proceed();
    }
}

// 使用示例
@RestController
@RequestMapping("/admin-api/notification")
public class NotificationController {
    
    @PostMapping("/create")
    @RequirePermission({"notification:create"})
    @RequireRole({"TEACHER", "ADMIN"})
    public CommonResult<NotificationVO> createNotification(@RequestBody CreateNotificationReqVO reqVO) {
        // 创建通知逻辑
    }
    
    @PostMapping("/publish")
    @RequirePermission(value = {"notification:publish"}, level = PermissionLevel.DEPT_LEVEL)
    public CommonResult<Void> publishNotification(@RequestParam String id) {
        // 发布通知逻辑
    }
}
```

## 🔐 会话管理

### 分布式会话管理
```java
// 会话管理服务
@Service
public class SessionManager {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user_sessions:";
    
    /**
     * 创建用户会话
     */
    public String createSession(User user, String deviceType, String clientIP) {
        String sessionId = UUID.randomUUID().toString();
        
        UserSession session = UserSession.builder()
            .sessionId(sessionId)
            .userId(user.getId())
            .username(user.getUsername())
            .tenantId(user.getTenantId())
            .deviceType(deviceType)
            .clientIP(clientIP)
            .loginTime(System.currentTimeMillis())
            .lastActiveTime(System.currentTimeMillis())
            .build();
        
        // 存储会话信息
        redisTemplate.opsForValue().set(
            SESSION_KEY_PREFIX + sessionId, 
            session, 
            2, TimeUnit.HOURS
        );
        
        // 记录用户的所有会话
        redisTemplate.opsForSet().add(USER_SESSION_PREFIX + user.getId(), sessionId);
        
        return sessionId;
    }
    
    /**
     * 强制用户下线
     */
    public void forceLogout(Long userId) {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(USER_SESSION_PREFIX + userId);
        
        if (sessionIds != null) {
            for (Object sessionId : sessionIds) {
                // 删除会话
                redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
                
                // 通知前端用户被强制下线
                webSocketService.sendToUser(userId, "FORCE_LOGOUT", "您的账号在其他地方登录");
            }
        }
        
        // 清除用户会话记录
        redisTemplate.delete(USER_SESSION_PREFIX + userId);
    }
}
```

## 🛡️ 安全最佳实践

### 1. 密码安全策略
```java
@Component
public class PasswordValidator {
    
    /**
     * 教育行业密码策略：
     * - 最少8位，最多20位
     * - 包含大写字母、小写字母、数字、特殊字符中至少3种
     * - 不能包含用户名、手机号等个人信息
     * - 不能是常见弱密码
     */
    public boolean validatePassword(String password, User user) {
        // 长度检查
        if (password.length() < 8 || password.length() > 20) {
            return false;
        }
        
        // 复杂度检查
        int complexity = 0;
        if (password.matches(".*[a-z].*")) complexity++;
        if (password.matches(".*[A-Z].*")) complexity++;
        if (password.matches(".*[0-9].*")) complexity++;
        if (password.matches(".*[^a-zA-Z0-9].*")) complexity++;
        
        if (complexity < 3) {
            return false;
        }
        
        // 个人信息检查
        if (password.contains(user.getUsername()) || 
            password.contains(user.getMobile())) {
            return false;
        }
        
        // 弱密码检查
        return !isWeakPassword(password);
    }
}
```

### 2. 接口限流防护
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        
        String clientIP = getClientIP(request);
        String uri = request.getRequestURI();
        
        // 登录接口特殊限制：每分钟最多5次
        if ("/admin-api/auth/login".equals(uri)) {
            if (!checkLoginRate(clientIP)) {
                response.setStatus(429);
                response.getWriter().write("登录过于频繁，请稍后再试");
                return false;
            }
        }
        
        return true;
    }
    
    private boolean checkLoginRate(String clientIP) {
        String key = "rate_limit:login:" + clientIP;
        String countStr = (String) redisTemplate.opsForValue().get(key);
        
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        if (count >= 5) {
            return false;
        }
        
        // 增加计数
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        
        return true;
    }
}
```

### 3. 安全审计日志
```java
@Component
public class SecurityAuditLogger {
    
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    @EventListener
    public void handleLoginSuccess(AuthenticationSuccessEvent event) {
        User user = (User) event.getAuthentication().getPrincipal();
        
        AuditLog auditLog = AuditLog.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .tenantId(user.getTenantId())
            .action("LOGIN_SUCCESS")
            .clientIP(getClientIP())
            .userAgent(getUserAgent())
            .timestamp(System.currentTimeMillis())
            .build();
        
        AUDIT_LOG.info("LOGIN_SUCCESS: {}", JSON.toJSONString(auditLog));
    }
    
    @EventListener
    public void handleLoginFailure(AbstractAuthenticationFailureEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();
        
        AuditLog auditLog = AuditLog.builder()
            .username(username)
            .action("LOGIN_FAILURE")
            .reason(event.getException().getMessage())
            .clientIP(getClientIP())
            .timestamp(System.currentTimeMillis())
            .build();
        
        AUDIT_LOG.warn("LOGIN_FAILURE: {}", JSON.toJSONString(auditLog));
    }
}
```

## 📱 移动端认证

### App Token管理
```java
// 移动端专用Token管理
@Service
public class MobileTokenManager {
    
    /**
     * 生成移动端长期Token（30天）
     */
    public MobileTokenResponse generateMobileToken(User user, String deviceId) {
        // 生成访问Token（2小时）
        String accessToken = jwtManager.generateAccessToken(user);
        
        // 生成刷新Token（30天）
        String refreshToken = generateLongTermRefreshToken(user, deviceId);
        
        // 记录设备信息
        recordDeviceInfo(user.getId(), deviceId);
        
        return MobileTokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(7200)
            .tokenType("Bearer")
            .build();
    }
    
    /**
     * 刷新移动端Token
     */
    public MobileTokenResponse refreshMobileToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) {
            throw new AuthenticationException("刷新Token无效");
        }
        
        User user = getUserFromRefreshToken(refreshToken);
        return generateMobileToken(user, getDeviceIdFromToken(refreshToken));
    }
}
```

这个认证集成指南为教育机构通知系统提供了完整的安全认证方案，涵盖了复杂权限管理、多租户隔离、单点登录、多因素认证等关键功能，确保系统的安全性和可靠性。