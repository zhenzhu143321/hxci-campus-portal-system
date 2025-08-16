package cn.iocoder.yudao.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🛡️ SE-2.2: 内容安全策略配置
 * 
 * 配置安全头信息，防止XSS、点击劫持等攻击
 * 
 * @author Claude
 */
@Slf4j
@Configuration
public class ContentSecurityPolicyConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityHeaderInterceptor())
                .addPathPatterns("/admin-api/test/notification/**")
                .addPathPatterns("/mock-school-api/**");
    }

    /**
     * 安全头信息拦截器
     */
    public static class SecurityHeaderInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            log.debug("🛡️ [CSP] 添加安全头信息 - URL: {}", request.getRequestURI());
            
            // 🔐 Content Security Policy - 防止XSS攻击
            response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self'; " +
                "connect-src 'self' http://localhost:48081 http://localhost:48082; " +
                "frame-ancestors 'none';"
            );
            
            // 🛡️ X-Frame-Options - 防止点击劫持
            response.setHeader("X-Frame-Options", "DENY");
            
            // 🔒 X-Content-Type-Options - 防止MIME类型混淆
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // 🚫 X-XSS-Protection - 启用XSS保护
            response.setHeader("X-XSS-Protection", "1; mode=block");
            
            // 🔐 Referrer Policy - 控制引用信息泄露
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // 🛡️ Strict-Transport-Security - 强制HTTPS（开发环境可选）
            if (request.isSecure()) {
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }
            
            // 🔍 Cache-Control - 敏感页面不缓存
            if (request.getRequestURI().contains("/api/")) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
            
            log.debug("🛡️ [CSP] 安全头信息设置完成");
            return true;
        }
    }
    
    /**
     * 🚨 安全事件监听器Bean
     */
    @Bean
    public SecurityEventListener securityEventListener() {
        return new SecurityEventListener();
    }
    
    /**
     * 安全事件监听器
     */
    public static class SecurityEventListener {
        
        public void onSecurityViolation(String event, String details) {
            log.warn("🚨 [SECURITY-EVENT] 安全违规事件: {} | 详情: {}", event, details);
            
            // 在生产环境中，这里可以：
            // 1. 发送安全告警邮件
            // 2. 记录到安全审计数据库
            // 3. 触发自动封禁机制
            // 4. 发送到安全监控中心
        }
        
        public void onSuspiciousActivity(String userId, String activity, String risk) {
            log.warn("🔍 [SUSPICIOUS] 可疑活动: 用户={}, 活动={}, 风险等级={}", userId, activity, risk);
            
            // 可疑活动处理逻辑
            switch (risk) {
                case "HIGH":
                    log.error("🚨 [HIGH-RISK] 高风险活动detected，建议立即处理");
                    break;
                case "MEDIUM":
                    log.warn("⚠️ [MEDIUM-RISK] 中风险活动detected，建议监控");
                    break;
                case "LOW":
                    log.info("ℹ️ [LOW-RISK] 低风险活动detected，记录备查");
                    break;
            }
        }
    }
}