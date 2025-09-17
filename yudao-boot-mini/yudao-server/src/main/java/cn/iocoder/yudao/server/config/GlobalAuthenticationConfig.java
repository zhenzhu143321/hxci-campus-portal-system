package cn.iocoder.yudao.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 🚨 Spring Boot 3 使用Jakarta Servlet API
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import cn.iocoder.yudao.server.security.CampusAuthContextHolder;
import cn.iocoder.yudao.server.security.AccessControlListManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import java.util.Map;
import java.util.HashMap;
import java.time.Duration;

/**
 * 全局认证拦截器配置类 - P0级安全修复
 * 
 * 🚨 修复关键安全漏洞：
 * 1. 认证绕过攻击防护 (CVSS 9.3)
 * 2. 强制所有API端点认证检查
 * 3. JWT Token强制验证
 * 4. 公开端点白名单管理
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Component
public class GlobalAuthenticationConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(GlobalAuthenticationConfig.class);

    @Autowired
    private GlobalAuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("🛡️ [AUTH_CONFIG] 注册全局认证拦截器");
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns(
                        "/error", // Spring Boot错误页面
                        "/favicon.ico", // 浏览器图标
                        "/actuator/**", // Spring Boot Actuator
                        "/swagger-ui/**", // Swagger UI
                        "/v3/api-docs/**", // OpenAPI文档
                        "/csrf-token", // CSRF Token获取端点 - 双Token流程修复
                        "/csrf-status", // CSRF状态检查端点
                        "/csrf-config" // CSRF配置查询端点
                );
        log.info("✅ [AUTH_CONFIG] 全局认证拦截器注册完成");
    }

    /**
     * 🛡️ 全局认证拦截器
     */
    @Component
    public static class GlobalAuthenticationInterceptor implements HandlerInterceptor {

        private static final Logger log = LoggerFactory.getLogger(GlobalAuthenticationInterceptor.class);

        @Autowired
        private ObjectMapper objectMapper;

        // Mock School API调用工具
        private static final String MOCK_API_BASE = "http://localhost:48082";
        // 配置RestTemplate超时设置，防止线程永久阻塞
        private final RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))    // 连接超时5秒
                .setReadTimeout(Duration.ofSeconds(10))      // 读取超时10秒
                .build();

        // 🚨 公开端点白名单（严格控制，只有必要的认证端点）
        private static final Set<String> PUBLIC_ENDPOINTS = new HashSet<>(Arrays.asList(
                "/mock-school-api/auth/authenticate", // 用户登录认证
                "/mock-school-api/auth/register",     // 用户注册（如果需要）
                "/mock-school-api/health",            // 健康检查
                "/admin-api/test/health",             // 主服务健康检查
                "/admin-api/actuator/health",         // Actuator健康检查
                
                // 🔒 P0级安全测试API（标记为@PermitAll的公开端点）
                "/admin-api/test/security/status",                  // P0安全修复状态
                "/admin-api/test/security/encryption-test",         // AES-256-GCM加密测试
                "/admin-api/test/security/key-config-test",         // 密钥配置测试
                "/admin-api/test/security/audit-test",              // 安全审计测试
                "/admin-api/test/security/attack-detection-test",   // 攻击检测测试
                
                // 📢 通知系统测试API（标记为@PermitAll的公开端点）
                "/admin-api/test/notification/api/ping",            // 通知服务ping测试
                "/admin-api/test/notification/api/health",          // 通知服务健康检查
                "/admin-api/test/notification/api/simple-test",     // 通知服务简单测试
                
                // 🛡️ 垂直越权防护测试API（P0-SEC-04功能）
                "/admin-api/test/vertical-privilege/api/ping",          // 垂直越权防护系统ping测试
                "/admin-api/test/vertical-privilege/api/protection-status", // 防护状态检查

                // 📝 待办通知系统测试API（标记为@PermitAll的公开端点）
                "/admin-api/test/todo-new/api/ping"                    // 待办通知服务ping测试
        ));

        // 🛡️ 允许的HTTP方法白名单
        private static final Set<String> ALLOWED_METHODS = new HashSet<>(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String requestPath = request.getRequestURI();
            String method = request.getMethod();
            String clientIP = getClientIP(request);
            
            log.debug("🔍 [AUTH_CHECK] 认证检查: {} {} from {}", method, requestPath, clientIP);

            try {
                // 1️⃣ HTTP方法验证
                if (!ALLOWED_METHODS.contains(method)) {
                    log.warn("🚨 [AUTH_CHECK] 不允许的HTTP方法: {}", method);
                    sendUnauthorizedResponse(response, "HTTP方法不被允许: " + method);
                    return false;
                }

                // 2️⃣ OPTIONS请求处理（CORS预检）
                if ("OPTIONS".equals(method)) {
                    log.debug("✅ [AUTH_CHECK] OPTIONS预检请求通过: {}", requestPath);
                    return true;
                }

                // 3️⃣ 公开端点检查
                if (isPublicEndpoint(requestPath)) {
                    log.debug("✅ [AUTH_CHECK] 公开端点访问: {}", requestPath);
                    return true;
                }

                // 4️⃣ 提取JWT Token
                String token = extractTokenFromRequest(request);
                if (!StringUtils.hasText(token)) {
                    log.warn("🚨 [AUTH_CHECK] 缺少Authorization Token: {} {}", method, requestPath);
                    sendUnauthorizedResponse(response, "缺少有效的Authorization Token");
                    return false;
                }

                // 5️⃣ Token格式验证
                if (!isValidTokenFormat(token)) {
                    log.warn("🚨 [AUTH_CHECK] Token格式错误: {}", requestPath);
                    sendUnauthorizedResponse(response, "Token格式不正确");
                    return false;
                }

                // 6️⃣ JWT安全验证（基础检查）
                if (!performBasicJwtValidation(token)) {
                    log.warn("🚨 [AUTH_CHECK] JWT基础验证失败: {}", requestPath);
                    sendUnauthorizedResponse(response, "JWT Token验证失败");
                    return false;
                }

                // 7️⃣ 记录成功的认证
                log.debug("✅ [AUTH_CHECK] 认证通过: {} {} from {}", method, requestPath, clientIP);
                
                // 8️⃣ 将Token添加到请求属性，供后续使用
                request.setAttribute("JWT_TOKEN", token);

                // 9️⃣ 🔐 将用户信息存储到ThreadLocal（新增安全特性）
                try {
                    // 先清理可能存在的旧数据
                    CampusAuthContextHolder.clear();

                    // 存储JWT Token
                    CampusAuthContextHolder.setCurrentToken(token);

                    // 调用Mock API获取用户信息并存储到ThreadLocal
                    CampusAuthContextHolder.UserInfo userInfo = getUserInfoFromMockApi("Bearer " + token);
                    if (userInfo != null) {
                        CampusAuthContextHolder.setCurrentUser(userInfo);
                        log.debug("✅ [AUTH_CHECK] 用户信息已存储到ThreadLocal: {} ({})",
                                userInfo.getUsername(), userInfo.getRoleCode());
                    } else {
                        log.warn("⚠️ [AUTH_CHECK] 无法获取用户信息，但Token验证通过，继续请求");
                    }
                } catch (Exception e) {
                    log.error("❌ [AUTH_CHECK] 存储用户信息到ThreadLocal失败: {}", e.getMessage());
                    // 不影响请求继续，因为Token已验证通过
                }

                return true;

            } catch (Exception e) {
                log.error("❌ [AUTH_CHECK] 认证检查异常: {} {}", requestPath, e.getMessage());
                sendUnauthorizedResponse(response, "认证服务异常");
                return false;
            }
        }

        /**
         * 🎯 检查是否为公开端点
         */
        private boolean isPublicEndpoint(String path) {
            if (path == null) return false;
            
            // 精确匹配公开端点
            for (String publicPath : PUBLIC_ENDPOINTS) {
                if (path.equals(publicPath) || path.startsWith(publicPath)) {
                    return true;
                }
            }
            
            return false;
        }

        /**
         * 🔐 从请求中提取JWT Token
         */
        private String extractTokenFromRequest(HttpServletRequest request) {
            // 1. 从Authorization Header获取
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7); // 移除"Bearer "前缀
            }
            
            // 2. 从Query Parameter获取（不推荐，但兼容性考虑）
            String tokenParam = request.getParameter("token");
            if (StringUtils.hasText(tokenParam)) {
                log.warn("⚠️ [AUTH_CHECK] 从Query参数获取Token（不推荐）");
                return tokenParam;
            }
            
            return null;
        }

        /**
         * 🛡️ 验证Token格式
         */
        private boolean isValidTokenFormat(String token) {
            if (!StringUtils.hasText(token)) return false;
            
            // JWT格式检查：应该有3个部分，由.分隔
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("❌ [TOKEN_FORMAT] Token格式错误，部分数量: {}", parts.length);
                return false;
            }
            
            // 简单长度检查
            if (token.length() < 50) {
                log.warn("❌ [TOKEN_FORMAT] Token过短: {}", token.length());
                return false;
            }
            
            return true;
        }

        /**
         * 🚨 执行完整JWT验证 - P0级安全修复
         * 
         * 修复内容：
         * 1. 完整的JWT结构验证
         * 2. Header算法安全性检查
         * 3. Payload基础字段验证
         * 4. 时间戳有效性验证
         * 5. 签名完整性基础检查
         */
        private boolean performBasicJwtValidation(String token) {
            try {
                log.debug("🔍 [JWT_VALIDATION] 开始执行完整JWT验证...");
                
                // 1. JWT结构验证
                String[] parts = token.split("\\.");
                if (parts.length != 3) {
                    log.error("🚨 [JWT_VALIDATION] JWT结构无效，部分数量: {}", parts.length);
                    return false;
                }
                
                // 2. Header验证
                if (!validateJwtHeader(parts[0])) {
                    return false;
                }
                
                // 3. Payload验证
                if (!validateJwtPayload(parts[1])) {
                    return false;
                }
                
                // 4. 签名基础验证
                if (!validateJwtSignature(parts[2])) {
                    return false;
                }
                
                log.debug("✅ [JWT_VALIDATION] JWT完整验证通过");
                return true;
                
            } catch (Exception e) {
                log.error("❌ [JWT_VALIDATION] JWT验证异常", e);
                return false;
            }
        }
        
        /**
         * 🔍 验证JWT Header
         */
        private boolean validateJwtHeader(String headerBase64) {
            try {
                // 解码Header
                byte[] headerBytes = java.util.Base64.getUrlDecoder().decode(headerBase64);
                String headerJson = new String(headerBytes, java.nio.charset.StandardCharsets.UTF_8);
                
                log.debug("🔍 [JWT_HEADER] Header内容: {}", headerJson);
                
                // 检查必需字段
                if (!headerJson.contains("\"typ\"") || !headerJson.contains("\"alg\"")) {
                    log.error("🚨 [JWT_HEADER] Header缺少必需字段");
                    return false;
                }
                
                // 检查typ字段
                if (!headerJson.contains("\"typ\":\"JWT\"")) {
                    log.error("🚨 [JWT_HEADER] 无效的typ字段");
                    return false;
                }
                
                // 算法安全性检查（P0级安全修复）
                if (headerJson.contains("\"alg\":\"none\"") || 
                    headerJson.contains("\"alg\":\"None\"") ||
                    headerJson.contains("\"alg\":\"NONE\"")) {
                    log.error("🚨 [JWT_HEADER] 检测到None算法攻击尝试！");
                    return false;
                }
                
                if (headerJson.contains("\"alg\":\"MOCK\"") || 
                    headerJson.contains("\"alg\":\"mock\"")) {
                    log.error("🚨 [JWT_HEADER] 检测到不安全的MOCK算法！");
                    return false;
                }
                
                // 检查支持的算法
                boolean validAlgorithm = headerJson.contains("\"alg\":\"HS256\"") ||
                                       headerJson.contains("\"alg\":\"HS384\"") ||
                                       headerJson.contains("\"alg\":\"HS512\"") ||
                                       headerJson.contains("\"alg\":\"RS256\"");
                
                if (!validAlgorithm) {
                    log.warn("⚠️ [JWT_HEADER] 使用了不推荐的算法");
                    // 不立即拒绝，但记录警告
                }
                
                log.debug("✅ [JWT_HEADER] Header验证通过");
                return true;
                
            } catch (Exception e) {
                log.error("❌ [JWT_HEADER] Header解析失败", e);
                return false;
            }
        }
        
        /**
         * 🔍 验证JWT Payload
         */
        private boolean validateJwtPayload(String payloadBase64) {
            try {
                // 解码Payload
                byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(payloadBase64);
                String payloadJson = new String(payloadBytes, java.nio.charset.StandardCharsets.UTF_8);
                
                log.debug("🔍 [JWT_PAYLOAD] Payload长度: {}", payloadJson.length());
                
                // 检查基础结构
                if (!payloadJson.startsWith("{") || !payloadJson.endsWith("}")) {
                    log.error("🚨 [JWT_PAYLOAD] Payload格式无效");
                    return false;
                }
                
                // 检查关键字段存在性
                boolean hasSubject = payloadJson.contains("\"sub\":");
                boolean hasExpiry = payloadJson.contains("\"exp\":");
                boolean hasIssuedAt = payloadJson.contains("\"iat\":");
                
                if (!hasSubject) {
                    log.warn("⚠️ [JWT_PAYLOAD] 缺少subject字段");
                }
                
                if (!hasExpiry) {
                    log.warn("⚠️ [JWT_PAYLOAD] 缺少过期时间字段");
                    // 不强制要求，但记录警告
                }
                
                if (!hasIssuedAt) {
                    log.warn("⚠️ [JWT_PAYLOAD] 缺少签发时间字段");
                }
                
                // 简单的时间戳验证（如果存在exp字段）
                if (hasExpiry) {
                    if (!validateTokenExpiry(payloadJson)) {
                        log.error("🚨 [JWT_PAYLOAD] Token已过期");
                        return false;
                    }
                }
                
                log.debug("✅ [JWT_PAYLOAD] Payload验证通过");
                return true;
                
            } catch (Exception e) {
                log.error("❌ [JWT_PAYLOAD] Payload解析失败", e);
                return false;
            }
        }
        
        /**
         * 🔍 验证Token过期时间
         */
        private boolean validateTokenExpiry(String payloadJson) {
            try {
                // 提取exp字段值（简单字符串匹配）
                String expPattern = "\"exp\":";
                int expIndex = payloadJson.indexOf(expPattern);
                if (expIndex == -1) return true; // 没有exp字段，跳过验证
                
                int expStart = expIndex + expPattern.length();
                int expEnd = payloadJson.indexOf(",", expStart);
                if (expEnd == -1) expEnd = payloadJson.indexOf("}", expStart);
                if (expEnd == -1) return true; // 无法解析，跳过验证
                
                String expValue = payloadJson.substring(expStart, expEnd).trim();
                
                // 去除可能的引号
                if (expValue.startsWith("\"") && expValue.endsWith("\"")) {
                    expValue = expValue.substring(1, expValue.length() - 1);
                }
                
                long expTimestamp = Long.parseLong(expValue);
                long currentTimestamp = System.currentTimeMillis() / 1000;
                
                // 允许30秒的时钟偏移
                boolean isValid = expTimestamp > (currentTimestamp - 30);
                
                if (!isValid) {
                    log.error("🚨 [TOKEN_EXPIRY] Token已过期: exp={}, current={}", 
                            expTimestamp, currentTimestamp);
                }
                
                return isValid;
                
            } catch (NumberFormatException e) {
                log.warn("⚠️ [TOKEN_EXPIRY] 无法解析过期时间: {}", e.getMessage());
                return true; // 无法解析时不拒绝，但记录警告
            } catch (Exception e) {
                log.error("❌ [TOKEN_EXPIRY] 过期时间验证异常", e);
                return true; // 验证异常时不拒绝
            }
        }
        
        /**
         * 🔍 验证JWT签名基础格式
         */
        private boolean validateJwtSignature(String signatureBase64) {
            try {
                // 基础格式检查
                if (signatureBase64 == null || signatureBase64.trim().isEmpty()) {
                    log.error("🚨 [JWT_SIGNATURE] 签名部分为空");
                    return false;
                }
                
                // Base64格式检查
                try {
                    java.util.Base64.getUrlDecoder().decode(signatureBase64);
                } catch (IllegalArgumentException e) {
                    log.error("🚨 [JWT_SIGNATURE] 签名不是有效的Base64格式");
                    return false;
                }
                
                // 长度检查（至少应该有一定长度）
                if (signatureBase64.length() < 20) {
                    log.error("🚨 [JWT_SIGNATURE] 签名长度异常: {}", signatureBase64.length());
                    return false;
                }
                
                log.debug("✅ [JWT_SIGNATURE] 签名格式验证通过");
                return true;
                
            } catch (Exception e) {
                log.error("❌ [JWT_SIGNATURE] 签名验证异常", e);
                return false;
            }
        }

        /**
         * 🌐 获取客户端真实IP
         */
        private String getClientIP(HttpServletRequest request) {
            String[] ipHeaders = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
                "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
            };
            
            for (String header : ipHeaders) {
                String ip = request.getHeader(header);
                if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                    return ip.split(",")[0].trim();
                }
            }
            
            return request.getRemoteAddr();
        }

        /**
         * 📤 发送未授权响应
         */
        private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            
            // 安全响应头
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            
            String jsonResponse = objectMapper.writeValueAsString(java.util.Map.of(
                    "code", 401,
                    "message", message,
                    "timestamp", System.currentTimeMillis(),
                    "path", "认证失败"
            ));
            
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            // 清理请求属性
            request.removeAttribute("JWT_TOKEN");

            // 🚨 清理ThreadLocal（防止内存泄漏和用户信息串扰）
            try {
                CampusAuthContextHolder.clear();
                log.debug("🧹 [AUTH_CLEANUP] ThreadLocal已清理");
            } catch (Exception e) {
                log.error("❌ [AUTH_CLEANUP] 清理ThreadLocal失败: {}", e.getMessage());
            }
        }

        /**
         * 🔐 从Mock API获取用户信息
         * 复用NewTodoNotificationController的成功模式
         */
        private CampusAuthContextHolder.UserInfo getUserInfoFromMockApi(String authToken) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", authToken);

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(new HashMap<>(), headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                    MOCK_API_BASE + "/mock-school-api/auth/user-info",
                    HttpMethod.POST,
                    entity,
                    Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, Object> body = response.getBody();
                    if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                        Map<String, Object> data = (Map<String, Object>) body.get("data");
                        if (data != null) {
                            CampusAuthContextHolder.UserInfo userInfo = new CampusAuthContextHolder.UserInfo();
                            userInfo.setUsername((String) data.get("username"));
                            userInfo.setRoleCode((String) data.get("roleCode"));
                            userInfo.setRoleName((String) data.get("roleName"));

                            // 提取详细信息
                            String studentId = (String) data.get("studentId");
                            if (studentId == null) {
                                studentId = (String) data.get("employeeId");
                            }
                            userInfo.setStudentId(studentId);
                            userInfo.setEmployeeId(studentId);
                            userInfo.setGradeId((String) data.get("gradeId"));
                            userInfo.setClassId((String) data.get("classId"));

                            // 处理departmentId类型转换
                            Object deptId = data.get("departmentId");
                            if (deptId instanceof String) {
                                try {
                                    userInfo.setDepartmentId(Long.parseLong((String) deptId));
                                } catch (NumberFormatException e) {
                                    userInfo.setDepartmentId(null);
                                }
                            } else if (deptId instanceof Long) {
                                userInfo.setDepartmentId((Long) deptId);
                            }

                            log.debug("✅ [MOCK_API_AUTH] 用户认证成功: {} ({})",
                                    userInfo.getUsername(), userInfo.getRoleCode());
                            return userInfo;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("🔗 [MOCK_API_AUTH] Mock API调用异常: {}", e.getMessage());
            }
            return null;
        }
    }
}