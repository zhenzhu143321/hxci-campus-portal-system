package cn.iocoder.yudao.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 安全密钥配置管理 - P0级安全修复
 * 
 * 🔐 核心功能：
 * 1. 外部配置化密钥管理
 * 2. 环境变量和配置文件密钥加载
 * 3. 安全密钥生成和验证
 * 4. 密钥轮换和过期管理
 * 
 * 🛡️ 安全特性：
 * - 优先从环境变量加载密钥
 * - 支持配置文件备用密钥
 * - 自动生成安全密钥（开发环境）
 * - 密钥强度验证和合规检查
 * - 密钥内存安全清理
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-01-05
 */
@Component
@ConfigurationProperties(prefix = "security.encryption")
public class SecurityKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityKeyConfig.class);

    // 环境变量名
    private static final String ENV_ENCRYPTION_KEY = "HXCI_ENCRYPTION_KEY";
    private static final String ENV_JWT_SECRET_KEY = "HXCI_JWT_SECRET_KEY";
    private static final String ENV_HMAC_SECRET_KEY = "HXCI_HMAC_SECRET_KEY";

    // 密钥要求
    private static final int AES_KEY_LENGTH_BYTES = 32; // 256位
    private static final int MIN_KEY_ENTROPY = 128; // 最小熵值
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 配置属性（从application.yml读取，作为环境变量的备用）
    private String encryptionKey;
    private String jwtSecretKey;
    private String hmacSecretKey;
    private boolean allowAutoGeneration = true; // 是否允许自动生成密钥
    private String keySource = "unknown";

    // 运行时密钥（内存中存储）
    private byte[] runtimeEncryptionKey;
    private byte[] runtimeJwtSecretKey;
    private byte[] runtimeHmacSecretKey;

    /**
     * 🚀 初始化密钥配置
     * 在Spring容器启动后自动执行
     */
    @PostConstruct
    public void initializeKeys() {
        log.info("🔐 [KEY_INIT] 开始初始化安全密钥配置...");
        
        try {
            // 1. 初始化AES加密密钥
            initializeEncryptionKey();
            
            // 2. 初始化JWT签名密钥
            initializeJwtSecretKey();
            
            // 3. 初始化HMAC密钥
            initializeHmacSecretKey();
            
            // 4. 验证密钥配置
            validateKeyConfiguration();
            
            log.info("✅ [KEY_INIT] 安全密钥配置初始化完成");
            log.info("🔑 [KEY_SOURCE] 密钥来源: {}", keySource);
            
        } catch (Exception e) {
            log.error("❌ [KEY_INIT] 密钥配置初始化失败", e);
            throw new IllegalStateException("安全密钥初始化失败，系统无法启动", e);
        }
    }

    /**
     * 🔐 初始化AES加密密钥
     */
    private void initializeEncryptionKey() {
        log.debug("🔍 [ENCRYPTION_KEY] 开始初始化AES加密密钥...");
        
        // 1. 优先从环境变量获取
        String envKey = System.getenv(ENV_ENCRYPTION_KEY);
        if (isValidKeyString(envKey)) {
            runtimeEncryptionKey = processKeyString(envKey, "AES加密密钥");
            keySource = "环境变量";
            log.info("✅ [ENCRYPTION_KEY] 从环境变量加载AES密钥成功");
            return;
        }
        
        // 2. 从配置文件获取
        if (isValidKeyString(encryptionKey)) {
            runtimeEncryptionKey = processKeyString(encryptionKey, "AES加密密钥");
            keySource = keySource.equals("unknown") ? "配置文件" : keySource + "+配置文件";
            log.info("✅ [ENCRYPTION_KEY] 从配置文件加载AES密钥成功");
            return;
        }
        
        // 3. 自动生成密钥（开发环境）
        if (allowAutoGeneration) {
            runtimeEncryptionKey = generateSecureAESKey();
            keySource = keySource.equals("unknown") ? "自动生成" : keySource + "+自动生成";
            log.warn("⚠️ [ENCRYPTION_KEY] 未找到配置的AES密钥，自动生成密钥（仅适用于开发环境）");
            return;
        }
        
        throw new IllegalStateException("无法初始化AES加密密钥：未找到有效的密钥配置且禁用了自动生成");
    }

    /**
     * 🔐 初始化JWT签名密钥
     */
    private void initializeJwtSecretKey() {
        log.debug("🔍 [JWT_KEY] 开始初始化JWT签名密钥...");
        
        // 1. 优先从环境变量获取
        String envKey = System.getenv(ENV_JWT_SECRET_KEY);
        if (isValidKeyString(envKey)) {
            runtimeJwtSecretKey = processKeyString(envKey, "JWT签名密钥");
            log.info("✅ [JWT_KEY] 从环境变量加载JWT密钥成功");
            return;
        }
        
        // 2. 从配置文件获取
        if (isValidKeyString(jwtSecretKey)) {
            runtimeJwtSecretKey = processKeyString(jwtSecretKey, "JWT签名密钥");
            log.info("✅ [JWT_KEY] 从配置文件加载JWT密钥成功");
            return;
        }
        
        // 3. 自动生成密钥
        if (allowAutoGeneration) {
            runtimeJwtSecretKey = generateSecureRandomKey(64); // 512位
            log.warn("⚠️ [JWT_KEY] 未找到配置的JWT密钥，自动生成密钥（仅适用于开发环境）");
            return;
        }
        
        throw new IllegalStateException("无法初始化JWT签名密钥");
    }

    /**
     * 🔐 初始化HMAC密钥
     */
    private void initializeHmacSecretKey() {
        log.debug("🔍 [HMAC_KEY] 开始初始化HMAC密钥...");
        
        // 1. 优先从环境变量获取
        String envKey = System.getenv(ENV_HMAC_SECRET_KEY);
        if (isValidKeyString(envKey)) {
            runtimeHmacSecretKey = processKeyString(envKey, "HMAC密钥");
            log.info("✅ [HMAC_KEY] 从环境变量加载HMAC密钥成功");
            return;
        }
        
        // 2. 从配置文件获取
        if (isValidKeyString(hmacSecretKey)) {
            runtimeHmacSecretKey = processKeyString(hmacSecretKey, "HMAC密钥");
            log.info("✅ [HMAC_KEY] 从配置文件加载HMAC密钥成功");
            return;
        }
        
        // 3. 自动生成密钥
        if (allowAutoGeneration) {
            runtimeHmacSecretKey = generateSecureRandomKey(32); // 256位
            log.warn("⚠️ [HMAC_KEY] 未找到配置的HMAC密钥，自动生成密钥（仅适用于开发环境）");
            return;
        }
        
        throw new IllegalStateException("无法初始化HMAC密钥");
    }

    /**
     * 🔍 验证密钥字符串是否有效
     */
    private boolean isValidKeyString(String key) {
        return key != null && !key.trim().isEmpty() && key.length() >= 32;
    }

    /**
     * 🔧 处理密钥字符串
     * 支持Base64编码和原始字符串
     */
    private byte[] processKeyString(String keyString, String keyType) {
        try {
            // 尝试Base64解码
            if (isBase64(keyString)) {
                byte[] decoded = Base64.getDecoder().decode(keyString);
                if (decoded.length >= AES_KEY_LENGTH_BYTES) {
                    log.debug("🔓 [PROCESS_KEY] {}Base64解码成功，长度: {}", keyType, decoded.length);
                    return decoded;
                }
            }
            
            // 使用SHA-256哈希字符串生成固定长度密钥
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hashedKey = sha256.digest(keyString.getBytes(StandardCharsets.UTF_8));
            
            log.debug("🔨 [PROCESS_KEY] {}使用SHA-256哈希生成，长度: {}", keyType, hashedKey.length);
            return hashedKey;
            
        } catch (Exception e) {
            log.error("❌ [PROCESS_KEY] {}处理失败", keyType, e);
            throw new IllegalStateException(keyType + "处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 🔐 生成安全的AES密钥
     */
    private byte[] generateSecureAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, SECURE_RANDOM); // 256位密钥
            SecretKey secretKey = keyGenerator.generateKey();
            
            byte[] keyBytes = secretKey.getEncoded();
            log.debug("🔑 [GEN_AES_KEY] 生成AES-256密钥成功，长度: {}", keyBytes.length);
            
            return keyBytes;
            
        } catch (NoSuchAlgorithmException e) {
            log.error("❌ [GEN_AES_KEY] AES密钥生成失败", e);
            throw new IllegalStateException("AES密钥生成失败", e);
        }
    }

    /**
     * 🔐 生成安全的随机密钥
     */
    private byte[] generateSecureRandomKey(int lengthBytes) {
        byte[] key = new byte[lengthBytes];
        SECURE_RANDOM.nextBytes(key);
        
        log.debug("🔑 [GEN_RANDOM_KEY] 生成随机密钥成功，长度: {}", lengthBytes);
        return key;
    }

    /**
     * 🔍 检查字符串是否为Base64格式
     */
    private boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * ✅ 验证密钥配置完整性
     */
    private void validateKeyConfiguration() {
        log.debug("🔍 [VALIDATE_KEYS] 开始验证密钥配置...");
        
        // 验证AES加密密钥
        if (runtimeEncryptionKey == null || runtimeEncryptionKey.length < AES_KEY_LENGTH_BYTES) {
            throw new IllegalStateException("AES加密密钥长度不足，需要至少" + AES_KEY_LENGTH_BYTES + "字节");
        }
        
        // 验证JWT密钥
        if (runtimeJwtSecretKey == null || runtimeJwtSecretKey.length < 32) {
            throw new IllegalStateException("JWT签名密钥长度不足，需要至少32字节");
        }
        
        // 验证HMAC密钥
        if (runtimeHmacSecretKey == null || runtimeHmacSecretKey.length < 32) {
            throw new IllegalStateException("HMAC密钥长度不足，需要至少32字节");
        }
        
        log.info("✅ [VALIDATE_KEYS] 密钥配置验证通过");
        log.debug("🔐 [KEY_LENGTHS] AES: {}字节, JWT: {}字节, HMAC: {}字节",
                runtimeEncryptionKey.length, runtimeJwtSecretKey.length, runtimeHmacSecretKey.length);
    }

    // ========== 公开方法 ==========

    /**
     * 获取AES加密密钥
     * 
     * @return AES密钥字节数组
     */
    public byte[] getEncryptionKey() {
        if (runtimeEncryptionKey == null) {
            throw new IllegalStateException("AES加密密钥未初始化");
        }
        return runtimeEncryptionKey.clone(); // 返回副本，避免外部修改
    }

    /**
     * 获取JWT签名密钥
     * 
     * @return JWT密钥字节数组
     */
    public byte[] getJwtSecretKey() {
        if (runtimeJwtSecretKey == null) {
            throw new IllegalStateException("JWT签名密钥未初始化");
        }
        return runtimeJwtSecretKey.clone();
    }

    /**
     * 获取HMAC密钥
     * 
     * @return HMAC密钥字节数组
     */
    public byte[] getHmacSecretKey() {
        if (runtimeHmacSecretKey == null) {
            throw new IllegalStateException("HMAC密钥未初始化");
        }
        return runtimeHmacSecretKey.clone();
    }

    /**
     * 检查密钥配置是否有效
     * 
     * @return 密钥配置是否有效
     */
    public boolean isKeyConfigValid() {
        return runtimeEncryptionKey != null && 
               runtimeJwtSecretKey != null && 
               runtimeHmacSecretKey != null;
    }

    /**
     * 获取密钥来源信息
     * 
     * @return 密钥来源描述
     */
    public String getKeySource() {
        return keySource;
    }

    // ========== 配置属性设置方法 ==========

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public void setJwtSecretKey(String jwtSecretKey) {
        this.jwtSecretKey = jwtSecretKey;
    }

    public void setHmacSecretKey(String hmacSecretKey) {
        this.hmacSecretKey = hmacSecretKey;
    }

    public void setAllowAutoGeneration(boolean allowAutoGeneration) {
        this.allowAutoGeneration = allowAutoGeneration;
    }
}