package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.server.config.JwtSecurityConfig;
import cn.iocoder.yudao.server.security.JwtBlacklistService;
import cn.iocoder.yudao.server.security.SensitiveInfoProtectionService;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * JWT安全测试控制器 - P1.2安全修复验证
 * 
 * 用于测试和验证JWT信息泄露修复效果
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Tag(name = "JWT安全测试API")
@RestController
@RequestMapping("/admin-api/test/jwt-security")
@Validated
@TenantIgnore
@Slf4j
public class JwtSecurityTestController {

    @Autowired
    private JwtSecurityConfig.JwtSecurityValidator jwtValidator;
    
    @Autowired
    private JWTVerifier jwtVerifier;
    
    @Autowired
    private JwtBlacklistService jwtBlacklistService;
    
    @Autowired
    private SensitiveInfoProtectionService sensitiveInfoService;

    @GetMapping("/api/ping")
    @Operation(summary = "JWT安全测试 - Ping")
    @PermitAll
    @TenantIgnore
    public CommonResult<String> ping() {
        log.info("🔐 [JWT_SECURITY_TEST] JWT安全测试控制器 ping");
        return success("JWT Security Test Controller - Ready");
    }

    /**
     * 🔍 JWT Token载荷分析
     * 验证Token中是否还包含敏感信息
     */
    @PostMapping("/api/analyze-token-payload")
    @Operation(summary = "分析JWT Token载荷")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> analyzeTokenPayload(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("🔍 [JWT_ANALYSIS] 开始分析JWT Token载荷");
        
        try {
            String token = extractToken(authHeader);
            if (token == null) {
                return CommonResult.error(400, "无效的Authorization头");
            }
            
            Map<String, Object> result = new HashMap<>();
            
            // 1️⃣ 解析Token结构
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return CommonResult.error(400, "无效的JWT Token格式");
            }
            
            // 2️⃣ 解码Header
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            result.put("header", headerJson);
            
            // 3️⃣ 解码Payload（载荷分析）
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            result.put("payload", payloadJson);
            result.put("payloadSize", payloadJson.length());
            
            // 4️⃣ 敏感信息检测
            Map<String, Boolean> sensitiveInfoCheck = new HashMap<>();
            sensitiveInfoCheck.put("containsRealName", payloadJson.contains("realName"));
            sensitiveInfoCheck.put("containsUsername", payloadJson.contains("username"));
            sensitiveInfoCheck.put("containsDepartmentName", payloadJson.contains("departmentName"));
            sensitiveInfoCheck.put("containsGradeId", payloadJson.contains("gradeId"));
            sensitiveInfoCheck.put("containsClassId", payloadJson.contains("classId"));
            sensitiveInfoCheck.put("containsStudentId", payloadJson.contains("studentId"));
            
            boolean hasSensitiveInfo = sensitiveInfoCheck.values().stream().anyMatch(Boolean::booleanValue);
            
            result.put("sensitiveInfoCheck", sensitiveInfoCheck);
            result.put("hasSensitiveInfo", hasSensitiveInfo);
            result.put("securityLevel", hasSensitiveInfo ? "❌ 高风险 - 包含敏感信息" : "✅ 安全 - 最小载荷原则");
            
            // 5️⃣ JWT安全特征检查
            Map<String, Object> securityFeatures = new HashMap<>();
            securityFeatures.put("hasJwtId", payloadJson.contains("jti"));
            securityFeatures.put("hasIssuer", payloadJson.contains("iss"));
            securityFeatures.put("hasAudience", payloadJson.contains("aud"));
            securityFeatures.put("hasExpiration", payloadJson.contains("exp"));
            
            result.put("securityFeatures", securityFeatures);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("✅ [JWT_ANALYSIS] Token载荷分析完成 - 敏感信息: {}, 载荷大小: {}", 
                    hasSensitiveInfo, payloadJson.length());
            
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [JWT_ANALYSIS] Token载荷分析异常", e);
            return CommonResult.error(500, "Token载荷分析失败: " + e.getMessage());
        }
    }

    /**
     * 🛡️ JWT签名验证测试
     * 测试算法安全性和签名完整性
     */
    @PostMapping("/api/verify-token-security")
    @Operation(summary = "验证JWT Token安全性")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> verifyTokenSecurity(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("🛡️ [JWT_SECURITY_VERIFY] 开始JWT安全验证");
        
        try {
            String token = extractToken(authHeader);
            if (token == null) {
                return CommonResult.error(400, "无效的Authorization头");
            }
            
            Map<String, Object> result = new HashMap<>();
            
            // 1️⃣ 算法安全性验证
            boolean algorithmValid = jwtValidator.validateJwtAlgorithm(token);
            result.put("algorithmValid", algorithmValid);
            
            // 2️⃣ 完整安全验证
            JwtSecurityConfig.ValidationResult validationResult = 
                jwtValidator.validateJwtToken(token, jwtVerifier, jwtBlacklistService);
            
            result.put("signatureValid", validationResult.isValid);
            result.put("validationMessage", validationResult.message);
            
            if (validationResult.isValid && validationResult.jwt != null) {
                DecodedJWT jwt = validationResult.jwt;
                
                // 3️⃣ Token详细信息
                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("algorithm", jwt.getAlgorithm());
                tokenInfo.put("jwtId", jwt.getId());
                tokenInfo.put("subject", jwt.getSubject());
                tokenInfo.put("issuer", jwt.getIssuer());
                tokenInfo.put("audience", jwt.getAudience());
                tokenInfo.put("issuedAt", jwt.getIssuedAt());
                tokenInfo.put("expiresAt", jwt.getExpiresAt());
                
                result.put("tokenInfo", tokenInfo);
                
                // 4️⃣ 黑名单检查
                String jwtId = jwt.getId();
                boolean isBlacklisted = jwtBlacklistService.isBlacklisted(jwtId);
                result.put("isBlacklisted", isBlacklisted);
                
                // 5️⃣ 重放攻击检测
                boolean replayProtectionPassed = jwtBlacklistService.validateJwtReplayProtection(jwtId);
                result.put("replayProtectionPassed", replayProtectionPassed);
            }
            
            // 6️⃣ 整体安全评分
            int securityScore = calculateSecurityScore(result);
            result.put("securityScore", securityScore);
            result.put("securityLevel", getSecurityLevel(securityScore));
            
            result.put("timestamp", LocalDateTime.now());
            
            log.info("✅ [JWT_SECURITY_VERIFY] JWT安全验证完成 - 得分: {}", securityScore);
            
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [JWT_SECURITY_VERIFY] JWT安全验证异常", e);
            return CommonResult.error(500, "JWT安全验证失败: " + e.getMessage());
        }
    }

    /**
     * 📊 JWT黑名单统计信息
     */
    @GetMapping("/api/blacklist-stats")
    @Operation(summary = "获取JWT黑名单统计")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> getBlacklistStats() {
        
        log.info("📊 [JWT_BLACKLIST_STATS] 获取JWT黑名单统计");
        
        try {
            JwtBlacklistService.BlacklistStats stats = jwtBlacklistService.getBlacklistStats();
            
            Map<String, Object> result = new HashMap<>();
            result.put("blacklistedTokens", stats.blacklistedTokens);
            result.put("trackedTokens", stats.trackedTokens);
            result.put("lastUpdated", stats.lastUpdated);
            result.put("timestamp", LocalDateTime.now());
            
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [JWT_BLACKLIST_STATS] 获取黑名单统计异常", e);
            return CommonResult.error(500, "获取黑名单统计失败: " + e.getMessage());
        }
    }

    /**
     * 🧹 清理过期Token
     */
    @PostMapping("/api/cleanup-expired-tokens")
    @Operation(summary = "清理过期Token")
    @PermitAll
    @TenantIgnore
    public CommonResult<String> cleanupExpiredTokens() {
        
        log.info("🧹 [JWT_CLEANUP] 手动清理过期Token");
        
        try {
            jwtBlacklistService.cleanExpiredTokens();
            sensitiveInfoService.cleanExpiredCache();
            
            return success("过期Token清理完成");
            
        } catch (Exception e) {
            log.error("❌ [JWT_CLEANUP] 清理过期Token异常", e);
            return CommonResult.error(500, "清理过期Token失败: " + e.getMessage());
        }
    }

    /**
     * 🔒 敏感信息保护服务统计
     */
    @GetMapping("/api/sensitive-info-stats")
    @Operation(summary = "获取敏感信息保护统计")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> getSensitiveInfoStats() {
        
        log.info("🔒 [SENSITIVE_INFO_STATS] 获取敏感信息保护统计");
        
        try {
            SensitiveInfoProtectionService.CacheStats cacheStats = sensitiveInfoService.getCacheStats();
            
            Map<String, Object> result = new HashMap<>();
            result.put("cachedUsers", cacheStats.cachedUsers);
            result.put("cacheExpirySeconds", cacheStats.cacheExpirySeconds);
            result.put("cacheStatsString", cacheStats.toString());
            result.put("timestamp", LocalDateTime.now());
            
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [SENSITIVE_INFO_STATS] 获取敏感信息保护统计异常", e);
            return CommonResult.error(500, "获取敏感信息保护统计失败: " + e.getMessage());
        }
    }

    /**
     * 提取Bearer Token
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /**
     * 计算安全得分
     */
    private int calculateSecurityScore(Map<String, Object> result) {
        int score = 0;
        
        if (Boolean.TRUE.equals(result.get("algorithmValid"))) score += 30;
        if (Boolean.TRUE.equals(result.get("signatureValid"))) score += 30;
        if (Boolean.FALSE.equals(result.get("isBlacklisted"))) score += 20;
        if (Boolean.TRUE.equals(result.get("replayProtectionPassed"))) score += 20;
        
        return score;
    }

    /**
     * 获取安全等级
     */
    private String getSecurityLevel(int score) {
        if (score >= 90) return "🟢 极高安全";
        if (score >= 80) return "🟡 高安全";
        if (score >= 60) return "🟠 中等安全";
        return "🔴 低安全";
    }
}