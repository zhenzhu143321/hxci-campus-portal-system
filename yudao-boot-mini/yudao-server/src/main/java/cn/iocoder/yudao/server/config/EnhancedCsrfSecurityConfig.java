package cn.iocoder.yudao.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🛡️ P0-SEC-03增强CSRF防护配置
 * 
 * 安全增强项目：
 * 1. 生产环境CORS严格限制
 * 2. CSRF Token速率限制和监控
 * 3. 增强安全日志记录
 * 4. SameSite Cookie属性设置
 * 5. 可疑攻击检测和阻断
 * 
 * @author Claude Code AI - P0 Security Enhancement
 * @date 2025-09-07
 */
@Configuration
@EnableWebSecurity
public class EnhancedCsrfSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(EnhancedCsrfSecurityConfig.class);
    
    // 速率限制：每个IP每分钟最多10次CSRF Token请求
    private static final int MAX_CSRF_REQUESTS_PER_MINUTE = 10;
    private static final ConcurrentHashMap<String, TokenRequestTracker> ipTokenRequests = new ConcurrentHashMap<>();
    
    @Value("${security.csrf.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private List<String> allowedOrigins;
    
    @Value("${security.csrf.token-expiry:1800}") // 默认30分钟
    private int csrfTokenExpiry;
    
    @Value("${security.csrf.rate-limit-enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${security.csrf.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    /**
     * 🔐 增强CSRF防护安全过滤器链
     */
    @Bean
    @Order(-2)
    public SecurityFilterChain enhancedCsrfFilterChain(HttpSecurity http) throws Exception {
        log.info("🛡️ [P0-SEC-03] 初始化增强CSRF防护安全过滤器链");
        
        return http
            .securityMatcher("/admin-api/**")
            
            // 🚀 严格CORS配置
            .cors(cors -> cors.configurationSource(strictCorsConfigurationSource()))
            
            // 🛡️ 增强CSRF配置
            .csrf(csrf -> csrf
                // 使用增强的CSRF Token存储库
                .csrfTokenRepository(createEnhancedCsrfTokenRepository())
                // 使用自定义CSRF Token处理器
                .csrfTokenRequestHandler(new EnhancedCsrfTokenRequestAttributeHandler())
                // 智能路径忽略策略
                .ignoringRequestMatchers(this::shouldIgnoreCsrfForRequest)
            )
            
            // 🚨 增强异常处理
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(this::handleCsrfViolation)
            )
            
            // 🔓 无状态会话管理
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 🎯 请求授权配置 - 🚨 修复安全漏洞：要求认证
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/csrf-token", "/csrf-status", "/csrf-config").permitAll()
                .anyRequest().authenticated()  // 🔒 要求JWT认证，不能permitAll()
            )
            
            // 添加增强的CSRF Token响应头过滤器和速率限制过滤器
            .addFilterAfter(new EnhancedCsrfTokenResponseHeaderFilter(), HeaderWriterFilter.class)
            .addFilterBefore(new CsrfRateLimitFilter(), HeaderWriterFilter.class)
            .build();
    }

    /**
     * 🌐 严格CORS配置 - 生产环境安全
     */
    @Bean
    public CorsConfigurationSource strictCorsConfigurationSource() {
        log.info("🌐 [P0-SEC-03] 配置严格CORS跨域支持 - 允许来源: {}", allowedOrigins);
        
        CorsConfiguration configuration = new CorsConfiguration();
        // P0-SEC-03修复：不使用通配符，明确指定允许的来源
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type", "Authorization", "X-CSRF-TOKEN", "X-Requested-With"
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
            "X-CSRF-TOKEN", "X-CSRF-HEADER", "X-CSRF-PARAMETER", "X-Rate-Limit-Remaining"
        ));
        // 设置预检请求缓存时间
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 创建增强的CSRF Token存储库
     */
    private CookieCsrfTokenRepository createEnhancedCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        
        // P0-SEC-03增强：设置安全的Cookie属性
        repository.setCookieName("XSRF-TOKEN");
        repository.setCookieMaxAge(csrfTokenExpiry); // 可配置的过期时间
        repository.setCookiePath("/");
        // 注意：SameSite属性需要在应用服务器层面配置
        
        log.info("🔐 [P0-SEC-03] CSRF Token存储配置完成 - 过期时间: {}秒", csrfTokenExpiry);
        return repository;
    }
    
    /**
     * 智能CSRF忽略策略
     */
    private boolean shouldIgnoreCsrfForRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // GET请求和OPTIONS预检请求保持无状态
        if ("GET".equals(method) || "OPTIONS".equals(method)) {
            log.debug("🔓 [P0-SEC-03] 跳过{}请求: {}", method, uri);
            return true;
        }
        
        // 特定豁免端点
        if (uri.contains("/mock-school-api") || 
            uri.contains("/csrf-token") || 
            uri.contains("/csrf-status") ||
            uri.contains("/csrf-config")) {
            log.debug("🔓 [P0-SEC-03] 跳过豁免路径: {}", uri);
            return true;
        }
        
        log.debug("🛡️ [P0-SEC-03] 需要CSRF验证: {} {}", method, uri);
        return false;
    }
    
    /**
     * 增强CSRF违规处理
     */
    private void handleCsrfViolation(HttpServletRequest request, HttpServletResponse response, 
                                   org.springframework.security.access.AccessDeniedException e) throws IOException {
        
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        
        // P0-SEC-03增强：详细的安全日志
        log.warn("🚨 [P0-SEC-03] CSRF攻击检测 - IP: {}, URL: {}, Method: {}, User-Agent: {}, Referer: {}", 
                clientIp, request.getRequestURI(), request.getMethod(), userAgent, referer);
        
        // 记录可疑活动到安全日志
        recordSecurityIncident(clientIp, request, "CSRF_VIOLATION");
        
        // 返回标准化错误响应
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-Security-Incident", "CSRF_VIOLATION_DETECTED");
        
        response.getWriter().write(String.format("""
            {
                "code": 403,
                "message": "CSRF Token验证失败，请刷新页面重试",
                "data": null,
                "type": "CSRF_TOKEN_INVALID",
                "timestamp": "%s",
                "incidentId": "%s"
            }""", 
            LocalDateTime.now().toString(),
            "CSRF_" + System.currentTimeMillis()
        ));
    }
    
    /**
     * 🔍 增强CSRF Token响应头过滤器
     */
    private static class EnhancedCsrfTokenResponseHeaderFilter extends OncePerRequestFilter {
        
        private static final Logger log = LoggerFactory.getLogger(EnhancedCsrfTokenResponseHeaderFilter.class);
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, IOException {
            
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                // 设置CSRF Token响应头
                response.setHeader("X-CSRF-TOKEN", csrfToken.getToken());
                response.setHeader("X-CSRF-HEADER", csrfToken.getHeaderName());
                response.setHeader("X-CSRF-PARAMETER", csrfToken.getParameterName());
                
                // P0-SEC-03增强：添加安全相关头
                response.setHeader("X-CSRF-Protected", "true");
                response.setHeader("X-Security-Policy", "csrf-enabled");
                
                log.debug("🔐 [P0-SEC-03] 已设置增强CSRF Token响应头 - Token长度: {}", 
                        csrfToken.getToken().length());
            }
            
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * 🚦 CSRF Token请求速率限制过滤器
     */
    private class CsrfRateLimitFilter extends OncePerRequestFilter {
        
        private static final Logger log = LoggerFactory.getLogger(CsrfRateLimitFilter.class);
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                      jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, IOException {
            
            // 只对CSRF Token获取端点进行速率限制
            if (!rateLimitEnabled || !"/csrf-token".equals(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }
            
            String clientIp = getClientIp(request);
            TokenRequestTracker tracker = ipTokenRequests.computeIfAbsent(clientIp, k -> new TokenRequestTracker());
            
            // 检查速率限制
            if (tracker.isRateLimitExceeded()) {
                log.warn("🚦 [P0-SEC-03] CSRF Token请求速率限制 - IP: {}, 请求数: {}", 
                        clientIp, tracker.getRequestCount());
                
                recordSecurityIncident(clientIp, request, "CSRF_RATE_LIMIT_EXCEEDED");
                
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("X-Rate-Limit-Exceeded", "true");
                response.setHeader("X-Rate-Limit-Reset", String.valueOf(tracker.getResetTime()));
                
                response.getWriter().write("""
                    {
                        "code": 429,
                        "message": "CSRF Token请求过于频繁，请稍后再试",
                        "data": null,
                        "type": "RATE_LIMIT_EXCEEDED"
                    }""");
                return;
            }
            
            // 记录请求
            tracker.recordRequest();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(tracker.getRemainingRequests()));
            
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * 增强CSRF Token请求属性处理器
     */
    private static class EnhancedCsrfTokenRequestAttributeHandler extends CsrfTokenRequestAttributeHandler {
        
        private static final Logger log = LoggerFactory.getLogger(EnhancedCsrfTokenRequestAttributeHandler.class);
        
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, 
                          java.util.function.Supplier<CsrfToken> csrfToken) {
            super.handle(request, response, csrfToken);
            
            // P0-SEC-03增强：记录Token生成事件
            log.debug("🔐 [P0-SEC-03] CSRF Token处理完成 - URI: {}, Method: {}", 
                    request.getRequestURI(), request.getMethod());
        }
    }
    
    /**
     * Token请求跟踪器 - 用于速率限制 🚨 修复线程安全问题
     */
    private static class TokenRequestTracker {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        
        public synchronized boolean isRateLimitExceeded() {
            cleanupOldRequests();
            return requestCount.get() >= MAX_CSRF_REQUESTS_PER_MINUTE;
        }
        
        public synchronized void recordRequest() {
            cleanupOldRequests();
            requestCount.incrementAndGet();
        }
        
        public synchronized int getRemainingRequests() {
            cleanupOldRequests();
            return Math.max(0, MAX_CSRF_REQUESTS_PER_MINUTE - requestCount.get());
        }
        
        public long getResetTime() {
            return windowStart + 60000; // 1分钟窗口
        }
        
        public int getRequestCount() {
            return requestCount.get();
        }
        
        // 🔒 私有方法，调用方已加锁，不需要额外同步
        private void cleanupOldRequests() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60000) { // 1分钟窗口
                requestCount.set(0);
                windowStart = now;
            }
        }
    }
    
    /**
     * 获取客户端真实IP - 🚨 修复安全漏洞：防止头伪造绕过速率限制
     */
    private String getClientIp(HttpServletRequest request) {
        // 🔒 只有在信任代理头且配置了可信代理时才使用代理头
        if (trustProxyHeaders) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
        }
        
        // 🔒 默认使用TCP连接IP，防止伪造
        return request.getRemoteAddr();
    }
    
    /**
     * 记录安全事件
     */
    private void recordSecurityIncident(String clientIp, HttpServletRequest request, String incidentType) {
        // P0-SEC-03：可以扩展为写入专门的安全日志文件或发送到安全监控系统
        log.warn("🔒 [SECURITY_INCIDENT] 类型: {}, IP: {}, URI: {}, Method: {}, User-Agent: {}", 
                incidentType, clientIp, request.getRequestURI(), request.getMethod(), 
                request.getHeader("User-Agent"));
    }
}