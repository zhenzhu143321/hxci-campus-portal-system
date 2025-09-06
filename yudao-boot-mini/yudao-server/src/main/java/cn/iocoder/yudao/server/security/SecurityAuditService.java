package cn.iocoder.yudao.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 安全审计日志服务 - P0级安全修复
 * 
 * 🔐 核心功能：
 * 1. 安全事件记录和审计
 * 2. 攻击行为检测和告警
 * 3. 用户行为审计追踪
 * 4. 安全统计和分析
 * 
 * 🛡️ 安全特性：
 * - 完整的安全事件日志记录
 * - 敏感信息脱敏处理
 * - 异常行为检测告警
 * - 统计分析和趋势监控
 * - 符合安全合规要求
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-01-05
 */
@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);
    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 统计计数器
    private final AtomicLong totalSecurityEvents = new AtomicLong(0);
    private final AtomicLong authenticationAttempts = new AtomicLong(0);
    private final AtomicLong authenticationFailures = new AtomicLong(0);
    private final AtomicLong suspiciousActivities = new AtomicLong(0);
    private final AtomicLong tokenValidationFailures = new AtomicLong(0);
    
    /**
     * 📝 记录认证成功事件
     */
    public void logAuthenticationSuccess(String userId, String clientIP, String userAgent, HttpServletRequest request) {
        try {
            authenticationAttempts.incrementAndGet();
            totalSecurityEvents.incrementAndGet();
            
            Map<String, Object> event = createBaseSecurityEvent("AUTH_SUCCESS", clientIP, userAgent);
            event.put("userId", maskSensitiveData(userId));
            event.put("endpoint", request.getRequestURI());
            event.put("method", request.getMethod());
            
            String eventJson = objectMapper.writeValueAsString(event);
            auditLog.info("✅ [AUTH_SUCCESS] {}", eventJson);
            
            log.info("✅ [SECURITY_AUDIT] 认证成功: user={}, ip={}", maskSensitiveData(userId), clientIP);
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 记录认证成功事件失败", e);
        }
    }
    
    /**
     * 📝 记录认证失败事件
     */
    public void logAuthenticationFailure(String userId, String reason, String clientIP, String userAgent, HttpServletRequest request) {
        try {
            authenticationAttempts.incrementAndGet();
            authenticationFailures.incrementAndGet();
            totalSecurityEvents.incrementAndGet();
            
            Map<String, Object> event = createBaseSecurityEvent("AUTH_FAILURE", clientIP, userAgent);
            event.put("userId", maskSensitiveData(userId));
            event.put("reason", reason);
            event.put("endpoint", request.getRequestURI());
            event.put("method", request.getMethod());
            
            String eventJson = objectMapper.writeValueAsString(event);
            auditLog.warn("❌ [AUTH_FAILURE] {}", eventJson);
            
            log.warn("❌ [SECURITY_AUDIT] 认证失败: user={}, reason={}, ip={}", 
                    maskSensitiveData(userId), reason, clientIP);
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 记录认证失败事件失败", e);
        }
    }
    
    /**
     * 📝 记录Token验证失败事件
     */
    public void logTokenValidationFailure(String token, String reason, String clientIP, String userAgent, HttpServletRequest request) {
        try {
            tokenValidationFailures.incrementAndGet();
            totalSecurityEvents.incrementAndGet();
            
            Map<String, Object> event = createBaseSecurityEvent("TOKEN_VALIDATION_FAILURE", clientIP, userAgent);
            event.put("token", maskToken(token));
            event.put("reason", reason);
            event.put("endpoint", request.getRequestURI());
            event.put("method", request.getMethod());
            
            String eventJson = objectMapper.writeValueAsString(event);
            auditLog.warn("🚨 [TOKEN_FAILURE] {}", eventJson);
            
            log.warn("🚨 [SECURITY_AUDIT] Token验证失败: reason={}, ip={}", reason, clientIP);
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 记录Token验证失败事件失败", e);
        }
    }
    
    /**
     * 📝 记录安全攻击尝试事件
     */
    public void logSecurityAttack(String attackType, String details, String clientIP, String userAgent, HttpServletRequest request) {
        try {
            suspiciousActivities.incrementAndGet();
            totalSecurityEvents.incrementAndGet();
            
            Map<String, Object> event = createBaseSecurityEvent("SECURITY_ATTACK", clientIP, userAgent);
            event.put("attackType", attackType);
            event.put("details", details);
            event.put("endpoint", request != null ? request.getRequestURI() : "unknown");
            event.put("method", request != null ? request.getMethod() : "unknown");
            event.put("severity", determineSeverity(attackType));
            
            String eventJson = objectMapper.writeValueAsString(event);
            auditLog.error("🚨 [SECURITY_ATTACK] {}", eventJson);
            
            log.error("🚨 [SECURITY_AUDIT] 安全攻击检测: type={}, ip={}, details={}", 
                    attackType, clientIP, details);
            
            // 如果是高危攻击，发送告警
            if (isHighRiskAttack(attackType)) {
                sendSecurityAlert(attackType, clientIP, details);
            }
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 记录安全攻击事件失败", e);
        }
    }
    
    /**
     * 📝 记录Basic Token加密操作
     */
    public void logBasicTokenOperation(String operation, String userId, boolean success, String details) {
        try {
            totalSecurityEvents.incrementAndGet();
            
            Map<String, Object> event = createBaseSecurityEvent("BASIC_TOKEN_OPERATION", "system", "system");
            event.put("operation", operation); // ENCRYPT, DECRYPT, STORE, RETRIEVE
            event.put("userId", maskSensitiveData(userId));
            event.put("success", success);
            event.put("details", details);
            
            String eventJson = objectMapper.writeValueAsString(event);
            if (success) {
                auditLog.info("🔐 [BASIC_TOKEN_OP] {}", eventJson);
            } else {
                auditLog.warn("🚨 [BASIC_TOKEN_OP] {}", eventJson);
            }
            
            log.debug("🔐 [SECURITY_AUDIT] Basic Token操作: op={}, user={}, success={}", 
                    operation, maskSensitiveData(userId), success);
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 记录Basic Token操作事件失败", e);
        }
    }
    
    /**
     * 📝 记录用户权限检查事件
     */
    public void logPermissionCheck(String userId, String resource, String action, boolean allowed, String reason) {
        try {
            totalSecurityEvents.incrementAndGet();
            
            Map<String, Object> event = createBaseSecurityEvent("PERMISSION_CHECK", "system", "system");
            event.put("userId", maskSensitiveData(userId));
            event.put("resource", resource);
            event.put("action", action);
            event.put("allowed", allowed);
            event.put("reason", reason);
            
            String eventJson = objectMapper.writeValueAsString(event);
            if (allowed) {
                auditLog.debug("✅ [PERMISSION_CHECK] {}", eventJson);
            } else {
                auditLog.warn("❌ [PERMISSION_CHECK] {}", eventJson);
            }
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 记录权限检查事件失败", e);
        }
    }
    
    /**
     * 📊 获取安全统计信息
     */
    public Map<String, Object> getSecurityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalSecurityEvents", totalSecurityEvents.get());
        stats.put("authenticationAttempts", authenticationAttempts.get());
        stats.put("authenticationFailures", authenticationFailures.get());
        stats.put("suspiciousActivities", suspiciousActivities.get());
        stats.put("tokenValidationFailures", tokenValidationFailures.get());
        
        // 计算成功率
        long totalAuth = authenticationAttempts.get();
        long failedAuth = authenticationFailures.get();
        double successRate = totalAuth > 0 ? ((double)(totalAuth - failedAuth) / totalAuth * 100) : 100.0;
        stats.put("authenticationSuccessRate", String.format("%.2f%%", successRate));
        
        stats.put("timestamp", LocalDateTime.now().format(timeFormatter));
        
        return stats;
    }
    
    /**
     * 📋 创建基础安全事件结构
     */
    private Map<String, Object> createBaseSecurityEvent(String eventType, String clientIP, String userAgent) {
        Map<String, Object> event = new HashMap<>();
        
        event.put("eventType", eventType);
        event.put("timestamp", LocalDateTime.now().format(timeFormatter));
        event.put("clientIP", clientIP);
        event.put("userAgent", maskUserAgent(userAgent));
        event.put("sessionId", generateSessionId());
        
        return event;
    }
    
    /**
     * 🛡️ 脱敏敏感数据
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "***";
        }
        
        if (data.length() <= 8) {
            return data.substring(0, 2) + "***";
        }
        
        return data.substring(0, 3) + "***" + data.substring(data.length() - 2);
    }
    
    /**
     * 🛡️ 脱敏Token
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "***INVALID_TOKEN***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 6);
    }
    
    /**
     * 🛡️ 脱敏User Agent
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() <= 50) {
            return userAgent;
        }
        return userAgent.substring(0, 50) + "...";
    }
    
    /**
     * 🎯 确定攻击严重程度
     */
    private String determineSeverity(String attackType) {
        switch (attackType.toUpperCase()) {
            case "JWT_NONE_ALGORITHM":
            case "SQL_INJECTION":
            case "XSS_ATTACK":
                return "HIGH";
            case "BRUTE_FORCE":
            case "TOKEN_MANIPULATION":
                return "MEDIUM";
            default:
                return "LOW";
        }
    }
    
    /**
     * 🚨 判断是否为高危攻击
     */
    private boolean isHighRiskAttack(String attackType) {
        return "HIGH".equals(determineSeverity(attackType));
    }
    
    /**
     * 📢 发送安全告警
     */
    private void sendSecurityAlert(String attackType, String clientIP, String details) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("alertType", "HIGH_RISK_SECURITY_ATTACK");
            alert.put("attackType", attackType);
            alert.put("clientIP", clientIP);
            alert.put("details", details);
            alert.put("timestamp", LocalDateTime.now().format(timeFormatter));
            alert.put("severity", "HIGH");
            
            String alertJson = objectMapper.writeValueAsString(alert);
            auditLog.error("🚨🚨🚨 [SECURITY_ALERT] {}", alertJson);
            
            // 这里可以集成外部告警系统
            // alertService.sendAlert(alert);
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 发送安全告警失败", e);
        }
    }
    
    /**
     * 🎲 生成会话ID
     */
    private String generateSessionId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    /**
     * 📊 输出安全统计报告
     */
    public void printSecurityReport() {
        try {
            Map<String, Object> stats = getSecurityStatistics();
            String reportJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats);
            
            log.info("📊 [SECURITY_REPORT] 安全统计报告:\n{}", reportJson);
            auditLog.info("📊 [SECURITY_REPORT] {}", objectMapper.writeValueAsString(stats));
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_AUDIT] 生成安全报告失败", e);
        }
    }
}