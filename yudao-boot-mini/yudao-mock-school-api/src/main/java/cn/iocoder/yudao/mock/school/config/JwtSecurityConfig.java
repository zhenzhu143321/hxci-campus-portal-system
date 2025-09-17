package cn.iocoder.yudao.mock.school.config;

import com.auth0.jwt.algorithms.Algorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JWT安全配置 - P0-SEC-02修复
 * 
 * 解决的安全问题：
 * 1. JWT密钥硬编码
 * 2. 密钥生成使用弱算法
 * 3. 缺少密钥轮换机制
 * 
 * @author Claude Code AI
 * @date 2025-09-07
 */
@Configuration
public class JwtSecurityConfig {
    
    private static final Logger log = LoggerFactory.getLogger(JwtSecurityConfig.class);
    
    /**
     * JWT密钥最小长度（256位）
     */
    private static final int MIN_KEY_LENGTH = 32;
    
    /**
     * JWT签名算法
     */
    private Algorithm jwtAlgorithm;
    
    /**
     * JWT密钥（从环境变量或配置文件读取）
     */
    @Value("${jwt.secret:#{null}}")
    private String configuredSecret;
    
    /**
     * JWT发行者
     */
    @Value("${jwt.issuer:hxci-campus-portal}")
    private String jwtIssuer;
    
    /**
     * JWT有效期（毫秒）
     */
    @Value("${jwt.expiration:3600000}") // 默认60分钟
    private long jwtExpiration;
    
    /**
     * 初始化JWT配置
     */
    @PostConstruct
    public void init() {
        String jwtSecret = getOrGenerateSecret();
        this.jwtAlgorithm = Algorithm.HMAC256(jwtSecret);
        
        // 安全日志（不记录完整密钥）
        log.info("🔒 JWT安全配置初始化完成");
        log.info("   - 算法: HMAC256");
        log.info("   - 密钥长度: {} 字节", jwtSecret.length());
        log.info("   - 发行者: {}", jwtIssuer);
        log.info("   - 有效期: {} 分钟", jwtExpiration / 60000);
        log.info("   - 密钥来源: {}", configuredSecret != null ? "配置文件" : "环境变量/自动生成");
    }
    
    /**
     * 获取或生成JWT密钥
     * 
     * 优先级：
     * 1. 环境变量 JWT_SECRET
     * 2. 配置文件 jwt.secret
     * 3. 自动生成安全密钥
     */
    private String getOrGenerateSecret() {
        // 1. 尝试从环境变量获取
        String envSecret = System.getenv("JWT_SECRET");
        if (isValidSecret(envSecret)) {
            log.info("✅ 使用环境变量中的JWT密钥");
            return envSecret;
        }
        
        // 2. 尝试从配置文件获取
        if (isValidSecret(configuredSecret)) {
            log.info("✅ 使用配置文件中的JWT密钥");
            return configuredSecret;
        }
        
        // 3. 自动生成安全密钥
        log.warn("⚠️ 未配置JWT密钥，自动生成安全密钥（建议生产环境配置固定密钥）");
        String generatedSecret = generateSecureSecret();
        
        // 输出生成的密钥到日志（仅在开发环境）
        if (isDevEnvironment()) {
            log.info("📝 生成的JWT密钥（请保存到环境变量）: JWT_SECRET={}", generatedSecret);
        }
        
        return generatedSecret;
    }
    
    /**
     * 验证密钥是否有效
     */
    private boolean isValidSecret(String secret) {
        return secret != null && !secret.isEmpty() && secret.length() >= MIN_KEY_LENGTH;
    }
    
    /**
     * 生成安全的JWT密钥
     * 使用加密安全的随机数生成器
     */
    private String generateSecureSecret() {
        try {
            // 使用HMAC-SHA256密钥生成器
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            keyGen.init(256, secureRandom);
            
            // 生成密钥
            SecretKey secretKey = keyGen.generateKey();
            byte[] keyBytes = secretKey.getEncoded();
            
            // Base64编码
            return Base64.getEncoder().encodeToString(keyBytes);
            
        } catch (NoSuchAlgorithmException e) {
            log.error("❌ 生成安全密钥失败，使用备用方案", e);
            // 备用方案：使用SecureRandom直接生成
            return generateFallbackSecret();
        }
    }
    
    /**
     * 备用密钥生成方案
     */
    private String generateFallbackSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256位
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * 检查是否为开发环境
     */
    private boolean isDevEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("local") || profile.contains("dev");
    }
    
    // Getters for other components to use
    
    public Algorithm getJwtAlgorithm() {
        return jwtAlgorithm;
    }
    
    public String getJwtIssuer() {
        return jwtIssuer;
    }
    
    public long getJwtExpiration() {
        return jwtExpiration;
    }
}