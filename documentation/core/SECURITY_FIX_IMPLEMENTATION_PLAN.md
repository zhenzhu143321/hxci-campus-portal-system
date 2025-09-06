# 哈尔滨信息工程学院校园门户系统 - 安全修复实施方案

## 🚨 紧急安全漏洞修复计划

基于Phase 2安全渗透测试结果，发现以下**6个严重风险**和**3个高风险**安全漏洞，需要立即修复。

## 1. 严重风险漏洞 (CRITICAL) - 立即修复

### 🔥 1.1 JWT None算法绕过漏洞
**漏洞描述**: 系统接受None算法的JWT Token，允许攻击者绕过签名验证
**风险等级**: 🚨 CRITICAL  
**OWASP类别**: A07-身份认证失效

#### 修复方案
```java
// 文件: yudao-boot-mini/yudao-server/src/main/java/cn/iocoder/yudao/server/config/SecurityConfig.java
@Configuration
public class JwtSecurityConfig {
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // 明确禁用None算法
        Set<String> allowedAlgorithms = Set.of("HS256", "RS256");
        
        return NimbusJwtDecoder.withJwkSetUri("your-jwk-uri")
                .jwsAlgorithms(algs -> algs.clear())
                .jwsAlgorithms(algs -> algs.addAll(allowedAlgorithms))
                .build();
    }
    
    // JWT验证拦截器
    @Component
    public class JwtValidationInterceptor {
        public boolean validateJwtAlgorithm(String token) {
            try {
                String[] chunks = token.split("\\.");
                String header = new String(Base64.getUrlDecoder().decode(chunks[0]));
                JsonNode headerNode = objectMapper.readTree(header);
                String alg = headerNode.get("alg").asText();
                
                // 严格检查算法
                if ("none".equalsIgnoreCase(alg)) {
                    throw new SecurityException("None算法被禁用");
                }
                
                return Arrays.asList("HS256", "RS256").contains(alg);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
```

### 🔥 1.2 JWT签名验证绕过漏洞
**漏洞描述**: 系统接受被篡改的JWT Token
**修复方案**: 实施严格的JWT签名验证

```java
// 在MockSchoolUserServiceImpl中加强JWT验证
@Service
public class MockSchoolUserServiceImpl {
    
    private final String JWT_SECRET = "your-strong-secret-key-256-bits-minimum";
    
    public boolean validateJwtToken(String token) {
        try {
            // 使用强密钥验证签名
            Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            
            // 检查过期时间
            if (jwt.getExpiresAt().before(new Date())) {
                throw new SecurityException("Token已过期");
            }
            
            return true;
        } catch (JWTVerificationException e) {
            log.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }
}
```

### 🔥 1.3 认证绕过漏洞  
**漏洞描述**: 无Token可访问受保护API
**修复方案**: 强制所有API端点进行认证检查

```java
// 全局认证拦截器
@Component
public class GlobalAuthenticationInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        
        // 跳过公开端点
        if (isPublicEndpoint(requestPath)) {
            return true;
        }
        
        String token = extractTokenFromRequest(request);
        if (token == null || !validateToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问\"}");
            return false;
        }
        
        return true;
    }
    
    private boolean isPublicEndpoint(String path) {
        // 只有认证端点是公开的
        return path.contains("/auth/authenticate") || 
               path.contains("/auth/register") ||
               path.contains("/health");
    }
}
```

### 🔥 1.4 学生权限越权漏洞
**漏洞描述**: 学生可发布超出权限范围的通知
**修复方案**: 实施严格的权限验证矩阵

```java
@Component
public class NotificationPermissionValidator {
    
    public boolean validatePublishPermission(UserInfo user, NotificationRequest request) {
        String roleCode = user.getRoleCode();
        int level = request.getLevel();
        String targetScope = request.getTargetScope();
        
        // 权限验证矩阵
        switch (roleCode) {
            case "STUDENT":
                return level == 4 && "CLASS".equals(targetScope);
            case "TEACHER":
                return level >= 3 && Arrays.asList("CLASS", "DEPARTMENT").contains(targetScope);
            case "CLASS_TEACHER":
                return level >= 3 && Arrays.asList("CLASS", "GRADE").contains(targetScope);
            case "ACADEMIC_ADMIN":
                return level >= 2; // 1级需要审批流程
            case "PRINCIPAL":
            case "SYSTEM_ADMIN":
                return true; // 全权限
            default:
                return false;
        }
    }
}

// 在通知发布API中使用
@PostMapping("/publish-database")
@RequiresPermission("notification:publish")
public ResponseEntity<Object> publishNotification(@RequestBody NotificationRequest request, HttpServletRequest httpRequest) {
    UserInfo currentUser = getCurrentUser(httpRequest);
    
    // 权限验证
    if (!permissionValidator.validatePublishPermission(currentUser, request)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", 403, "message", "权限不足，无法发布此级别通知"));
    }
    
    // 继续处理...
}
```

### 🔥 1.5 垂直越权漏洞
**漏洞描述**: 低权限用户可执行管理员操作
**修复方案**: 细粒度权限控制

```java
@Component
public class AdminOperationGuard {
    
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void requireSystemAdmin() {
        // 系统管理员专用操作标记
    }
    
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PRINCIPAL')")  
    public void requireAdminLevel() {
        // 管理员级别操作标记
    }
}

// 在敏感API中使用
@DeleteMapping("/permission-cache/api/clear-cache")
@RequiresPermission("system:admin")
public ResponseEntity<Object> clearPermissionCache(HttpServletRequest request) {
    UserInfo currentUser = getCurrentUser(request);
    
    // 严格验证管理员权限
    if (!"SYSTEM_ADMIN".equals(currentUser.getRoleCode()) && 
        !"PRINCIPAL".equals(currentUser.getRoleCode())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", 403, "message", "仅系统管理员可执行此操作"));
    }
    
    // 执行清理操作...
}
```

## 2. 高风险漏洞 (HIGH) - 优先修复

### 🔴 2.1 水平越权漏洞
**修复方案**: 实施资源所有权验证

```java
@Component  
public class ResourceOwnershipValidator {
    
    public boolean validateResourceAccess(UserInfo user, String resourceType, Long resourceId) {
        switch (resourceType) {
            case "TODO":
                return validateTodoAccess(user, resourceId);
            case "NOTIFICATION":
                return validateNotificationAccess(user, resourceId);
            default:
                return false;
        }
    }
    
    private boolean validateTodoAccess(UserInfo user, Long todoId) {
        // 检查待办是否属于当前用户或其管辖范围
        Todo todo = todoRepository.findById(todoId).orElse(null);
        if (todo == null) return false;
        
        // 创建者可访问
        if (user.getUserId().equals(todo.getCreatorId())) {
            return true;
        }
        
        // 根据角色检查管辖范围
        return checkJurisdiction(user, todo.getTargetScope(), todo.getTargetUsers());
    }
}
```

## 3. 中风险漏洞 (MEDIUM) - 计划修复

### 🟡 3.1 CSRF防护缺失
**修复方案**: 实施CSRF Token机制

```java
@Configuration
@EnableWebSecurity
public class CsrfSecurityConfig {
    
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
            .csrfTokenRepository(csrfTokenRepository())
            .and()
            .headers()
            .frameOptions().deny()
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
    }
}
```

### 🟡 3.2 JWT信息泄露
**修复方案**: 移除敏感信息，实施信息最小化

```java
// JWT载荷只包含必要信息
public String generateJwtToken(UserInfo user) {
    return Jwts.builder()
            .setSubject(user.getUserId())
            .claim("username", user.getUsername())
            .claim("roleCode", user.getRoleCode())
            // 移除敏感信息如：admin、password、secret等
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
            .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
            .compact();
}
```

## 4. 安全加固措施

### 4.1 安全响应头配置
```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SecurityHeadersInterceptor());
            }
        };
    }
    
    public class SecurityHeadersInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // 安全响应头
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.setHeader("Content-Security-Policy", "default-src 'self'");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            return true;
        }
    }
}
```

### 4.2 输入验证和输出编码
```java
@Component
public class SecurityInputValidator {
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        ".*(<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=|<iframe|<object|<embed).*",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(union|select|insert|update|delete|drop|create|alter|exec|script).*",
        Pattern.CASE_INSENSITIVE
    );
    
    public String sanitizeInput(String input) {
        if (input == null) return null;
        
        // XSS防护
        if (XSS_PATTERN.matcher(input).matches()) {
            throw new SecurityException("检测到XSS攻击载荷");
        }
        
        // SQL注入防护
        if (SQL_INJECTION_PATTERN.matcher(input).matches()) {
            throw new SecurityException("检测到SQL注入载荷");
        }
        
        // HTML编码
        return StringEscapeUtils.escapeHtml4(input);
    }
}
```

## 5. 实施时间表

| 优先级 | 修复项目 | 预计工作量 | 目标完成时间 |
|--------|----------|------------|--------------|
| P0 | JWT None算法禁用 | 4小时 | 立即 |
| P0 | JWT签名验证加固 | 6小时 | 24小时内 |
| P0 | 认证绕过修复 | 8小时 | 48小时内 |
| P0 | 权限越权修复 | 12小时 | 72小时内 |
| P1 | CSRF防护实施 | 6小时 | 1周内 |
| P1 | 安全响应头配置 | 4小时 | 1周内 |
| P2 | 输入验证增强 | 8小时 | 2周内 |
| P2 | 安全监控建设 | 16小时 | 1个月内 |

## 6. 验证测试计划

### 6.1 修复验证脚本
```python
# 创建修复验证测试脚本
def verify_security_fixes():
    # 1. 验证JWT None算法被禁用
    test_jwt_none_algorithm_blocked()
    
    # 2. 验证JWT签名验证有效
    test_jwt_signature_validation()
    
    # 3. 验证认证强制执行
    test_authentication_enforcement()
    
    # 4. 验证权限矩阵有效性
    test_permission_matrix()
    
    # 5. 验证CSRF防护
    test_csrf_protection()
```

### 6.2 持续安全监控
- 每日自动安全扫描
- 异常访问模式检测
- 权限滥用行为监控
- JWT Token异常使用告警

## 7. 合规性对照

| 安全标准 | 修复前状态 | 修复后目标 | 验证方式 |
|----------|------------|------------|----------|
| **OWASP A07** | ❌ 认证绕过 | ✅ 强制认证 | 渗透测试 |
| **OWASP A01** | ❌ 权限失效 | ✅ 严格授权 | 权限矩阵测试 |
| **OWASP A02** | ❌ JWT安全 | ✅ 安全算法 | JWT安全测试 |
| **ISO 27001** | 🟡 部分合规 | ✅ 全面合规 | 第三方审计 |

---

**文档版本**: v1.0  
**创建时间**: 2025-08-24  
**责任人**: Security Team  
**审核状态**: 待审核

**紧急联系**: 发现安全漏洞请立即联系安全团队进行应急响应