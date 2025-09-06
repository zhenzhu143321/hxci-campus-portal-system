package cn.iocoder.yudao.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Arrays;

/**
 * 🛡️ CSRF跨站请求伪造防护配置
 * 
 * 🚨 中风险安全漏洞修复：CVE-HXCI-2025-010
 * 1. 为所有写操作(POST/PUT/DELETE)启用CSRF Token验证
 * 2. 保持GET操作的无状态特性，兼容现有JWT认证
 * 3. 使用Cookie-based CSRF Token，支持SPA应用
 * 4. 提供友好的CSRF验证失败错误处理
 * 
 * @author Security Team  
 * @version 1.0
 * @since 2025-08-24
 */
@Configuration
@EnableWebSecurity
public class CsrfSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(CsrfSecurityConfig.class);

    /**
     * 🔓 公开端点安全过滤器链配置 - CSRF Token获取等无需认证的端点
     * 
     * Order(-3) 最高优先级，确保CSRF Token端点能够公开访问
     */
    @Bean
    @Order(-3)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        log.info("🔓 [PUBLIC_CONFIG] 初始化公开端点安全过滤器链");
        
        return http
            .securityMatcher("/csrf-token", "/csrf-status", "/csrf-config")
            // 🚀 CORS配置：支持跨域请求
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 🔐 启用CSRF但不强制验证 - 为了生成Token
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // 不对这些公开端点进行CSRF验证，但允许Token生成
                .ignoringRequestMatchers("/**")
            )
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 添加CSRF Token响应头过滤器
            .addFilterAfter(new CsrfTokenResponseHeaderFilter(), HeaderWriterFilter.class)
            .build();
    }
    
    /**
     * 🔐 CSRF防护安全过滤器链配置 - 保护API端点
     * 
     * Order(-2) 确保在yudao框架配置之前执行，专门处理需要CSRF验证的API端点
     */
    @Bean
    @Order(-2)
    public SecurityFilterChain csrfFilterChain(HttpSecurity http) throws Exception {
        log.info("🛡️ [CSRF_CONFIG] 初始化CSRF防护安全过滤器链");
        
        return http
            .securityMatcher("/admin-api/**")
            // 🚀 CORS配置：支持跨域请求
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 🛡️ CSRF配置：启用防护机制
            .csrf(csrf -> csrf
                // 使用Cookie存储CSRF Token
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // 配置CSRF Token处理器
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // 忽略GET请求和豁免路径，保持RESTful API的无状态特性
                .ignoringRequestMatchers(
                    request -> {
                        String method = request.getMethod();
                        String uri = request.getRequestURI();
                        
                        // GET请求保持无状态
                        if ("GET".equals(method)) {
                            log.debug("🔓 [CSRF] 跳过GET请求: {}", uri);
                            return true;
                        }
                        
                        // 豁免特定端点
                        if (uri.contains("/mock-school-api")) {
                            log.debug("🔓 [CSRF] 跳过豁免路径: {}", uri);
                            return true;
                        }
                        
                        log.debug("🛡️ [CSRF] 需要验证的请求: {} {}", method, uri);
                        return false;
                    }
                )
            )
            
            // 🚨 异常处理：自定义CSRF失败处理
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("🚨 [CSRF_SECURITY] CSRF验证失败 - IP: {}, URL: {}, Method: {}", 
                        getClientIp(request), request.getRequestURI(), request.getMethod());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("""
                        {
                            "code": 403,
                            "message": "CSRF验证失败，请刷新页面重试",
                            "data": null,
                            "type": "CSRF_TOKEN_INVALID"
                        }""");
                })
            )
            
            // 🔓 会话管理：保持无状态，兼容JWT
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 🎯 请求授权：允许所有请求通过，CSRF验证独立进行
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/csrf-token", "/csrf-status", "/csrf-config").permitAll()
                .anyRequest().permitAll()  // 其他验证由现有JWT系统处理
            )
            
            // 添加CSRF Token响应头过滤器
            .addFilterAfter(new CsrfTokenResponseHeaderFilter(), HeaderWriterFilter.class)
            .build();
    }

    /**
     * 🌐 CORS跨域配置
     * 支持前端SPA应用的跨域请求
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("🌐 [CORS_CONFIG] 配置CORS跨域支持");
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));  // 开发环境允许所有来源
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);  // 支持Cookie传输
        configuration.setExposedHeaders(Arrays.asList("X-CSRF-TOKEN", "X-CSRF-HEADER", "X-CSRF-PARAMETER"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * 🔍 CSRF Token响应头过滤器
     * 在每个响应中包含CSRF Token，方便前端获取
     */
    private static class CsrfTokenResponseHeaderFilter extends OncePerRequestFilter {
        
        private static final Logger log = LoggerFactory.getLogger(CsrfTokenResponseHeaderFilter.class);
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, IOException {
            
            // 获取CSRF Token
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                // 在响应头中包含CSRF Token
                response.setHeader("X-CSRF-TOKEN", csrfToken.getToken());
                response.setHeader("X-CSRF-HEADER", csrfToken.getHeaderName());
                response.setHeader("X-CSRF-PARAMETER", csrfToken.getParameterName());
                
                log.debug("🔐 [CSRF_TOKEN] 已设置CSRF Token响应头 - Token长度: {}", 
                    csrfToken.getToken().length());
            }
            
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * 🌐 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}