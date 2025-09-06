package cn.iocoder.yudao.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 安全Token加密服务 - P0级安全修复
 * 
 * 🔐 核心功能：
 * 1. AES-256-GCM加密/解密Basic Token
 * 2. 安全随机IV生成
 * 3. 密钥管理和验证
 * 4. 加密数据完整性验证
 * 
 * 🛡️ 安全特性：
 * - AES-256-GCM认证加密，防篡改
 * - 每次加密使用随机IV，防重放攻击
 * - 密钥外部配置化，避免硬编码
 * - 完整的异常处理和日志记录
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-01-05
 */
@Service
public class SecurityTokenService {

    private static final Logger log = LoggerFactory.getLogger(SecurityTokenService.class);

    @Autowired
    private SecurityKeyConfig securityKeyConfig;

    // AES-GCM加密参数
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96位IV
    private static final int GCM_AUTH_TAG_LENGTH = 128; // 128位认证标签
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 🔐 加密Basic Token
     * 
     * 加密流程：
     * 1. 生成随机IV
     * 2. 使用AES-256-GCM加密
     * 3. 组合IV+加密数据+认证标签
     * 4. Base64编码返回
     * 
     * @param basicToken 原始Basic Token
     * @return 加密后的Token字符串 (Base64编码)
     * @throws SecurityException 加密失败时抛出
     */
    public String encryptBasicToken(String basicToken) throws SecurityException {
        if (basicToken == null || basicToken.trim().isEmpty()) {
            throw new SecurityException("Basic Token不能为空");
        }

        log.info("🔐 [ENCRYPT_TOKEN] 开始加密Basic Token，长度: {}", basicToken.length());
        
        try {
            // 1. 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            
            // 2. 创建密钥规范
            byte[] keyBytes = securityKeyConfig.getEncryptionKey();
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            
            // 3. 初始化密码器
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            
            // 4. 执行加密
            byte[] tokenBytes = basicToken.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedData = cipher.doFinal(tokenBytes);
            
            // 5. 组合IV + 加密数据
            byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);
            
            // 6. Base64编码
            String encryptedToken = Base64.getEncoder().encodeToString(encryptedWithIv);
            
            log.info("✅ [ENCRYPT_TOKEN] Basic Token加密成功，加密后长度: {}", encryptedToken.length());
            log.debug("🔒 [ENCRYPT_TOKEN] 加密Token前缀: {}", 
                    encryptedToken.substring(0, Math.min(20, encryptedToken.length())) + "...");
            
            return encryptedToken;
            
        } catch (Exception e) {
            log.error("❌ [ENCRYPT_TOKEN] Basic Token加密失败", e);
            throw new SecurityException("Token加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 🔓 解密Basic Token
     * 
     * 解密流程：
     * 1. Base64解码
     * 2. 分离IV和加密数据
     * 3. 使用AES-256-GCM解密
     * 4. 验证认证标签完整性
     * 5. 返回原始Token
     * 
     * @param encryptedToken 加密的Token字符串 (Base64编码)
     * @return 解密后的Basic Token
     * @throws SecurityException 解密失败或数据被篡改时抛出
     */
    public String decryptBasicToken(String encryptedToken) throws SecurityException {
        if (encryptedToken == null || encryptedToken.trim().isEmpty()) {
            throw new SecurityException("加密Token不能为空");
        }

        log.info("🔓 [DECRYPT_TOKEN] 开始解密Basic Token，长度: {}", encryptedToken.length());
        
        try {
            // 1. Base64解码
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedToken);
            
            if (encryptedWithIv.length < GCM_IV_LENGTH + 16) { // 至少需要IV+最小加密数据
                throw new SecurityException("加密Token格式无效：数据长度不足");
            }
            
            // 2. 分离IV和加密数据
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            // 3. 创建密钥规范
            byte[] keyBytes = securityKeyConfig.getEncryptionKey();
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            
            // 4. 初始化密码器
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            
            // 5. 执行解密（同时验证认证标签）
            byte[] decryptedBytes = cipher.doFinal(encryptedData);
            String decryptedToken = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            log.info("✅ [DECRYPT_TOKEN] Basic Token解密成功，解密后长度: {}", decryptedToken.length());
            log.debug("🔓 [DECRYPT_TOKEN] Token前缀: {}", 
                    decryptedToken.substring(0, Math.min(10, decryptedToken.length())) + "...");
            
            return decryptedToken;
            
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("🚨 [DECRYPT_TOKEN] 认证标签验证失败 - 数据可能被篡改！", e);
            throw new SecurityException("Token数据完整性验证失败，可能被篡改", e);
            
        } catch (Exception e) {
            log.error("❌ [DECRYPT_TOKEN] Basic Token解密失败", e);
            throw new SecurityException("Token解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 🔍 验证加密Token格式
     * 
     * @param encryptedToken 加密Token
     * @return 是否为有效格式
     */
    public boolean isValidEncryptedTokenFormat(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 尝试Base64解码
            byte[] decoded = Base64.getDecoder().decode(encryptedToken);
            
            // 检查长度（至少包含IV + 最小加密数据 + 认证标签）
            return decoded.length >= GCM_IV_LENGTH + 16;
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [TOKEN_FORMAT] 无效的Base64格式: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 🎯 生成用于测试的模拟Basic Token
     * 仅用于开发和测试环境
     * 
     * @param userId 用户ID
     * @return 模拟的Basic Token
     */
    public String generateMockBasicToken(String userId) {
        String mockToken = "BASIC_" + userId + "_" + System.currentTimeMillis() + "_" + 
                           Integer.toHexString(SECURE_RANDOM.nextInt());
        
        log.info("🎭 [MOCK_TOKEN] 生成模拟Basic Token: userId={}, token长度={}", 
                userId, mockToken.length());
        
        return mockToken;
    }

    /**
     * 🛡️ 安全清理敏感数据
     * 
     * @param sensitiveData 需要清理的敏感字节数组
     */
    public void secureClearSensitiveData(byte[] sensitiveData) {
        if (sensitiveData != null) {
            // 用随机数据覆盖敏感信息
            SECURE_RANDOM.nextBytes(sensitiveData);
            // 再用零覆盖
            java.util.Arrays.fill(sensitiveData, (byte) 0);
        }
    }

    /**
     * 📊 获取加密服务状态信息
     * 
     * @return 服务状态信息
     */
    public String getEncryptionServiceStatus() {
        try {
            boolean keyConfigValid = securityKeyConfig != null && securityKeyConfig.isKeyConfigValid();
            String algorithm = ENCRYPTION_ALGORITHM;
            
            return String.format("SecurityTokenService状态: 密钥配置=%s, 算法=%s, IV长度=%d位, 认证标签=%d位",
                    keyConfigValid ? "有效" : "无效", algorithm, GCM_IV_LENGTH * 8, GCM_AUTH_TAG_LENGTH);
                    
        } catch (Exception e) {
            log.warn("⚠️ [SERVICE_STATUS] 获取服务状态异常", e);
            return "SecurityTokenService状态: 异常 - " + e.getMessage();
        }
    }
}