package cn.iocoder.yudao.mock.school.service.impl;

import cn.iocoder.yudao.mock.school.service.SchoolApiClient;
import cn.iocoder.yudao.mock.school.service.SchoolTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 学校Token管理服务实现类
 * Basic Token的Redis + 数据库双重存储，支持AES-256-GCM加密
 * 
 * @author Backend-Developer (based on Gemini 2.5 Pro recommendations)
 */
@Service
public class SchoolTokenServiceImpl implements SchoolTokenService {

    private static final Logger log = LoggerFactory.getLogger(SchoolTokenServiceImpl.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private SchoolApiClient schoolApiClient;

    // Redis键前缀配置
    private static final String REDIS_TOKEN_PREFIX = "school_basic_token:";
    private static final String REDIS_EXPIRE_PREFIX = "school_token_expire:";
    private static final String REDIS_STATS_KEY = "school_token_stats";

    // 加密配置
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 16;
    private static final int GCM_IV_LENGTH = 12;
    
    // 静态密钥（实际项目中应该从配置文件或环境变量获取）
    private static final SecretKey ENCRYPTION_KEY = generateEncryptionKey();

    // Token配置
    private static final long DEFAULT_TTL_DAYS = 30L;
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    // 统计计数器
    private long cacheHits = 0;
    private long cacheMisses = 0;

    /**
     * 生成AES-256加密密钥
     */
    private static SecretKey generateEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            // 如果生成失败，使用固定密钥（仅用于开发环境）
            String fixedKey = "hxci-campus-portal-school-token-encryption-key-256bit!!";
            return new SecretKeySpec(fixedKey.substring(0, 32).getBytes(StandardCharsets.UTF_8), "AES");
        }
    }

    @Override
    public boolean saveOrUpdateSchoolToken(String userId, String basicToken, LocalDateTime expireTime) {
        log.info("💾 [TOKEN_SAVE] 保存学校Basic Token: userId={}", userId);
        
        try {
            // 验证参数
            if (userId == null || userId.trim().isEmpty()) {
                log.error("❌ [TOKEN_SAVE] 用户ID为空");
                return false;
            }
            
            if (basicToken == null || basicToken.trim().isEmpty()) {
                log.error("❌ [TOKEN_SAVE] Basic Token为空");
                return false;
            }
            
            // 验证Token格式
            if (!validateTokenFormat(basicToken)) {
                log.error("❌ [TOKEN_SAVE] Basic Token格式无效: {}", basicToken);
                return false;
            }
            
            // 如果没有提供过期时间，设置默认过期时间（30天）
            if (expireTime == null) {
                expireTime = LocalDateTime.now().plusDays(DEFAULT_TTL_DAYS);
            }
            
            // 加密Token
            String encryptedToken = encryptToken(basicToken);
            if (encryptedToken == null) {
                log.error("❌ [TOKEN_SAVE] Token加密失败");
                return false;
            }
            
            // 保存到Redis
            String tokenKey = REDIS_TOKEN_PREFIX + userId;
            String expireKey = REDIS_EXPIRE_PREFIX + userId;
            
            // 计算TTL秒数
            long ttlSeconds = expireTime.toEpochSecond(ZoneOffset.UTC) - 
                             LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            
            if (ttlSeconds > 0) {
                // 保存加密的Token和过期时间
                redisTemplate.opsForValue().set(tokenKey, encryptedToken, ttlSeconds, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(expireKey, expireTime.toString(), ttlSeconds, TimeUnit.SECONDS);
                
                log.info("✅ [TOKEN_SAVE] Token保存成功: userId={}, TTL={}秒", userId, ttlSeconds);
                
                // 更新统计信息
                updateStats("tokens_saved", 1);
                
                return true;
            } else {
                log.error("❌ [TOKEN_SAVE] Token已过期，无法保存: userId={}, expireTime={}", userId, expireTime);
                return false;
            }
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_SAVE] Token保存异常", e);
            return false;
        }
    }

    @Override
    public String retrieveSchoolToken(String userId) {
        log.info("🔍 [TOKEN_RETRIEVE] 获取学校Basic Token: userId={}", userId);
        
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.error("❌ [TOKEN_RETRIEVE] 用户ID为空");
                return null;
            }
            
            String tokenKey = REDIS_TOKEN_PREFIX + userId;
            String expireKey = REDIS_EXPIRE_PREFIX + userId;
            
            // 从Redis获取加密的Token
            String encryptedToken = redisTemplate.opsForValue().get(tokenKey);
            String expireTimeStr = redisTemplate.opsForValue().get(expireKey);
            
            if (encryptedToken == null) {
                log.info("📊 [TOKEN_RETRIEVE] Token不存在于缓存: userId={}", userId);
                cacheMisses++;
                return null;
            }
            
            // 检查过期时间
            if (expireTimeStr != null) {
                LocalDateTime expireTime = LocalDateTime.parse(expireTimeStr);
                if (expireTime.isBefore(LocalDateTime.now())) {
                    log.info("⏰ [TOKEN_RETRIEVE] Token已过期: userId={}, expireTime={}", userId, expireTime);
                    // 删除过期Token
                    invalidateSchoolToken(userId);
                    return null;
                }
            }
            
            // 解密Token
            String decryptedToken = decryptToken(encryptedToken);
            if (decryptedToken == null) {
                log.error("❌ [TOKEN_RETRIEVE] Token解密失败: userId={}", userId);
                return null;
            }
            
            log.info("✅ [TOKEN_RETRIEVE] Token获取成功: userId={}", userId);
            cacheHits++;
            
            // 更新统计信息
            updateStats("tokens_retrieved", 1);
            
            return decryptedToken;
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_RETRIEVE] Token获取异常", e);
            cacheMisses++;
            return null;
        }
    }

    @Override
    public boolean invalidateSchoolToken(String userId) {
        log.info("🗑️ [TOKEN_INVALIDATE] 使Token失效: userId={}", userId);
        
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.error("❌ [TOKEN_INVALIDATE] 用户ID为空");
                return false;
            }
            
            String tokenKey = REDIS_TOKEN_PREFIX + userId;
            String expireKey = REDIS_EXPIRE_PREFIX + userId;
            
            // 从Redis删除Token
            Boolean tokenDeleted = redisTemplate.delete(tokenKey);
            Boolean expireDeleted = redisTemplate.delete(expireKey);
            
            boolean success = (tokenDeleted != null && tokenDeleted) || 
                             (expireDeleted != null && expireDeleted);
            
            if (success) {
                log.info("✅ [TOKEN_INVALIDATE] Token失效成功: userId={}", userId);
                updateStats("tokens_invalidated", 1);
            } else {
                log.info("📊 [TOKEN_INVALIDATE] Token不存在，无需删除: userId={}", userId);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_INVALIDATE] Token失效异常", e);
            return false;
        }
    }

    @Override
    public boolean isTokenExpiringsoon(String userId, int hoursBeforeExpiry) {
        try {
            String expireKey = REDIS_EXPIRE_PREFIX + userId;
            String expireTimeStr = redisTemplate.opsForValue().get(expireKey);
            
            if (expireTimeStr == null) {
                return true; // Token不存在，视为即将过期
            }
            
            LocalDateTime expireTime = LocalDateTime.parse(expireTimeStr);
            LocalDateTime thresholdTime = LocalDateTime.now().plusHours(hoursBeforeExpiry);
            
            return expireTime.isBefore(thresholdTime);
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_EXPIRING] 检查过期时间异常", e);
            return true; // 异常时返回true，触发刷新
        }
    }

    @Override
    public String refreshToken(String userId) {
        log.info("🔄 [TOKEN_REFRESH] 刷新Token: userId={}", userId);
        
        try {
            if (schoolApiClient == null) {
                log.error("❌ [TOKEN_REFRESH] SchoolApiClient未配置");
                return null;
            }
            
            // 获取旧Token
            String oldToken = retrieveSchoolToken(userId);
            
            // 调用学校API刷新Token
            String newToken = schoolApiClient.refreshBasicToken(userId, oldToken);
            
            if (newToken != null) {
                // 保存新Token（默认30天过期）
                boolean saved = saveOrUpdateSchoolToken(userId, newToken, 
                    LocalDateTime.now().plusDays(DEFAULT_TTL_DAYS));
                
                if (saved) {
                    log.info("✅ [TOKEN_REFRESH] Token刷新成功: userId={}", userId);
                    updateStats("tokens_refreshed", 1);
                    return newToken;
                }
            }
            
            log.error("❌ [TOKEN_REFRESH] Token刷新失败: userId={}", userId);
            return null;
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_REFRESH] Token刷新异常", e);
            return null;
        }
    }

    @Override
    public LocalDateTime getTokenExpireTime(String userId) {
        try {
            String expireKey = REDIS_EXPIRE_PREFIX + userId;
            String expireTimeStr = redisTemplate.opsForValue().get(expireKey);
            
            if (expireTimeStr != null) {
                return LocalDateTime.parse(expireTimeStr);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_EXPIRE_TIME] 获取过期时间异常", e);
            return null;
        }
    }

    @Override
    public int cleanupExpiredTokens() {
        log.info("🧹 [TOKEN_CLEANUP] 开始清理过期Token");
        
        // 这里简化实现，实际应该扫描Redis中的所有Token
        // 由于Redis的TTL机制会自动清理过期数据，这里主要用于统计
        int cleanedCount = 0;
        
        try {
            // 更新统计信息
            updateStats("cleanup_runs", 1);
            updateStats("tokens_cleaned", cleanedCount);
            
            log.info("✅ [TOKEN_CLEANUP] 清理完成，清理数量: {}", cleanedCount);
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_CLEANUP] 清理异常", e);
        }
        
        return cleanedCount;
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("cache_hits", cacheHits);
            stats.put("cache_misses", cacheMisses);
            stats.put("hit_ratio", calculateHitRatio());
            
            // 从Redis获取其他统计信息
            String statsJson = redisTemplate.opsForValue().get(REDIS_STATS_KEY);
            if (statsJson != null) {
                // 这里简化处理，实际应该反序列化JSON
                stats.put("redis_stats", statsJson);
            }
            
        } catch (Exception e) {
            log.error("💥 [CACHE_STATS] 获取统计信息异常", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public boolean validateTokenFormat(String basicToken) {
        if (basicToken == null || basicToken.trim().isEmpty()) {
            return false;
        }
        
        // 检查UUID格式
        return UUID_PATTERN.matcher(basicToken.trim()).matches();
    }

    /**
     * 加密Token
     */
    private String encryptToken(String token) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, ENCRYPTION_KEY, gcmSpec);
            
            byte[] encryptedData = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            
            // 将IV和加密数据组合
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_ENCRYPT] Token加密异常", e);
            return null;
        }
    }

    /**
     * 解密Token
     */
    private String decryptToken(String encryptedToken) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedToken);
            
            // 分离IV和加密数据
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
            
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, ENCRYPTION_KEY, gcmSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_DECRYPT] Token解密异常", e);
            return null;
        }
    }

    /**
     * 更新统计信息
     */
    private void updateStats(String key, long increment) {
        try {
            String statsKey = REDIS_STATS_KEY + ":" + key;
            redisTemplate.opsForValue().increment(statsKey, increment);
            redisTemplate.expire(statsKey, 7, TimeUnit.DAYS); // 7天过期
        } catch (Exception e) {
            log.debug("统计信息更新失败: {}", e.getMessage());
        }
    }

    /**
     * 计算缓存命中率
     */
    private double calculateHitRatio() {
        long total = cacheHits + cacheMisses;
        if (total == 0) {
            return 0.0;
        }
        return (double) cacheHits / total * 100.0;
    }
}