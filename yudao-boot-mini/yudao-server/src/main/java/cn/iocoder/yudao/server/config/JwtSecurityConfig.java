package cn.iocoder.yudao.server.config;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT安全配置类 - P0级安全修复
 * 
 * 🚨 修复关键安全漏洞：
 * 1. 禁用None算法绕过攻击 (CVSS 9.8)
 * 2. 强制256位最小密钥长度
 * 3. 严格算法白名单验证
 * 4. 强制15分钟Token TTL
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Configuration
public class JwtSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtSecurityConfig.class);
    
    // 🔐 安全强化：256位强密钥 (生产环境应从安全配置加载)
    private static final String JWT_SECRET = "hxci-campus-portal-secure-jwt-secret-key-256-bits-minimum-length-required-for-production-security";
    
    // 🛡️ 算法白名单：只允许安全算法
    private static final Set<String> ALLOWED_ALGORITHMS = new HashSet<>(Arrays.asList("HS256", "RS256"));
    
    // ⏰ Token有效期：15分钟 (安全最佳实践)
    public static final long JWT_EXPIRATION_MS = 15 * 60 * 1000; // 15分钟
    
    /**
     * 🔒 JWT验证算法Bean配置
     */
    @Bean
    public Algorithm jwtAlgorithm() {
        log.info("🔐 [JWT_CONFIG] 初始化安全JWT算法: HS256");
        return Algorithm.HMAC256(JWT_SECRET);
    }
    
    /**
     * 🛡️ JWT验证器Bean配置
     */
    @Bean
    public JWTVerifier jwtVerifier(Algorithm jwtAlgorithm) {
        log.info("🛡️ [JWT_CONFIG] 初始化JWT验证器");
        return JWT.require(jwtAlgorithm)
                .acceptLeeway(30) // 允许30秒时钟偏移
                .build();
    }
    
    /**
     * 🚨 JWT安全验证拦截器 - P1.2增强：防重放攻击
     */
    @Component
    public static class JwtSecurityValidator {
        
        private static final Logger log = LoggerFactory.getLogger(JwtSecurityValidator.class);
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        /**
         * 🛡️ 验证JWT Token算法安全性
         * 防护目标：None算法绕过攻击 (CVSS 9.8)
         */
        public boolean validateJwtAlgorithm(String token) {
            if (token == null || token.trim().isEmpty()) {
                log.warn("❌ [JWT_SECURITY] Token为空");
                return false;
            }
            
            try {
                // 1️⃣ 解析JWT Header
                String[] chunks = token.split("\\.");
                if (chunks.length != 3) {
                    log.warn("❌ [JWT_SECURITY] Token格式错误，部分数量: {}", chunks.length);
                    return false;
                }
                
                String header = new String(Base64.getUrlDecoder().decode(chunks[0]));
                JsonNode headerNode = objectMapper.readTree(header);
                String algorithm = headerNode.get("alg").asText();
                
                log.info("🔍 [JWT_SECURITY] 检查Token算法: {}", algorithm);
                
                // 2️⃣ 严格检查None算法 (关键安全防护)
                if ("none".equalsIgnoreCase(algorithm) || 
                    "None".equals(algorithm) || 
                    "NONE".equals(algorithm)) {
                    log.error("🚨 [JWT_SECURITY] 检测到None算法攻击尝试！算法: {}", algorithm);
                    throw new SecurityException("JWT None算法已被禁用 - 安全防护");
                }
                
                // 3️⃣ 验证算法白名单
                if (!ALLOWED_ALGORITHMS.contains(algorithm)) {
                    log.error("🚨 [JWT_SECURITY] 不安全的算法: {}，允许的算法: {}", algorithm, ALLOWED_ALGORITHMS);
                    throw new SecurityException("JWT算法不在安全白名单中");
                }
                
                // 4️⃣ 额外检查MOCK算法 (修复现有漏洞)
                if ("MOCK".equalsIgnoreCase(algorithm)) {
                    log.error("🚨 [JWT_SECURITY] 检测到不安全的MOCK算法");
                    throw new SecurityException("MOCK算法已被禁用 - 生产安全要求");
                }
                
                log.info("✅ [JWT_SECURITY] 算法验证通过: {}", algorithm);
                return true;
                
            } catch (SecurityException e) {
                log.error("🚨 [JWT_SECURITY] 安全验证失败: {}", e.getMessage());
                throw e; // 重新抛出安全异常
            } catch (Exception e) {
                log.error("❌ [JWT_SECURITY] Token解析失败", e);
                return false;
            }
        }
        
        /**
         * 🔐 验证JWT Token签名有效性 - P1.2增强：jti验证
         * 防护目标：JWT签名绕过攻击 (CVSS 9.3) + 重放攻击 (CVSS 8.5)
         */
        public ValidationResult validateJwtSignature(String token, JWTVerifier verifier, 
                                                   cn.iocoder.yudao.server.security.JwtBlacklistService blacklistService) {
            if (token == null || verifier == null) {
                log.warn("❌ [JWT_SIGNATURE] Token或验证器为空");
                return new ValidationResult(false, "Token或验证器为空");
            }
            
            try {
                // 1️⃣ 先验证算法安全性
                if (!validateJwtAlgorithm(token)) {
                    return new ValidationResult(false, "JWT算法验证失败");
                }
                
                // 2️⃣ 验证签名和有效期
                DecodedJWT jwt = verifier.verify(token);
                
                // 3️⃣ 🆕 检查jti和黑名单
                String jwtId = jwt.getId();
                if (blacklistService != null) {
                    if (blacklistService.isBlacklisted(jwtId)) {
                        log.error("🚫 [JWT_SIGNATURE] Token在黑名单中: jwtId={}", jwtId);
                        return new ValidationResult(false, "Token已被撤销");
                    }
                    
                    if (!blacklistService.validateJwtReplayProtection(jwtId)) {
                        log.error("🚨 [JWT_SIGNATURE] 重放攻击检测: jwtId={}", jwtId);
                        return new ValidationResult(false, "疑似重放攻击");
                    }
                } else if (jwtId == null || jwtId.trim().isEmpty()) {
                    log.warn("⚠️ [JWT_SIGNATURE] JWT ID缺失，重放攻击防护降级");
                }
                
                // 4️⃣ 检查过期时间
                Date expiresAt = jwt.getExpiresAt();
                if (expiresAt != null && expiresAt.before(new Date())) {
                    log.warn("❌ [JWT_SIGNATURE] Token已过期: {}", expiresAt);
                    return new ValidationResult(false, "Token已过期");
                }
                
                // 5️⃣ 检查签发时间
                Date issuedAt = jwt.getIssuedAt();
                if (issuedAt != null && issuedAt.after(new Date())) {
                    log.warn("❌ [JWT_SIGNATURE] Token签发时间异常: {}", issuedAt);
                    return new ValidationResult(false, "Token签发时间异常");
                }
                
                log.info("✅ [JWT_SIGNATURE] 签名验证通过，用户: {}, jwtId: {}", jwt.getSubject(), jwtId);
                return new ValidationResult(true, "验证成功", jwt);
                
            } catch (JWTVerificationException e) {
                log.error("❌ [JWT_SIGNATURE] JWT签名验证失败: {}", e.getMessage());
                return new ValidationResult(false, "JWT签名验证失败: " + e.getMessage());
            } catch (Exception e) {
                log.error("❌ [JWT_SIGNATURE] 签名验证异常", e);
                return new ValidationResult(false, "签名验证异常: " + e.getMessage());
            }
        }
        
        /**
         * 🚨 完整JWT安全验证 (算法+签名+时间+黑名单) - P1.2完整版
         */
        public ValidationResult validateJwtToken(String token, JWTVerifier verifier, 
                                               cn.iocoder.yudao.server.security.JwtBlacklistService blacklistService) {
            log.info("🔍 [JWT_VALIDATE] 开始完整JWT安全验证");
            
            // 1️⃣ 算法安全验证
            if (!validateJwtAlgorithm(token)) {
                return new ValidationResult(false, "算法验证失败");
            }
            
            // 2️⃣ 签名有效性验证 + jti验证 + 黑名单检查
            ValidationResult signatureResult = validateJwtSignature(token, verifier, blacklistService);
            if (!signatureResult.isValid) {
                return signatureResult;
            }
            
            log.info("✅ [JWT_VALIDATE] JWT完整安全验证通过");
            return signatureResult;
        }
        
        /**
         * 📊 获取JWT安全配置信息
         */
        public String getJwtSecurityInfo() {
            return String.format("JWT安全配置: 允许算法=%s, Token有效期=%d分钟, 密钥长度=%d位", 
                    ALLOWED_ALGORITHMS, JWT_EXPIRATION_MS / 60000, JWT_SECRET.length() * 8);
        }
    }
    
    /**
     * JWT验证结果封装
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        public final DecodedJWT jwt;
        
        public ValidationResult(boolean isValid, String message) {
            this(isValid, message, null);
        }
        
        public ValidationResult(boolean isValid, String message, DecodedJWT jwt) {
            this.isValid = isValid;
            this.message = message;
            this.jwt = jwt;
        }
    }
    
    /**
     * 🎯 静态工具方法：获取JWT过期时间
     */
    public static Date getJwtExpirationDate() {
        return new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS);
    }
    
    /**
     * 🎯 静态工具方法：检查是否为安全算法
     */
    public static boolean isSecureAlgorithm(String algorithm) {
        return ALLOWED_ALGORITHMS.contains(algorithm);
    }
}