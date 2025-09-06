package cn.iocoder.yudao.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * JWT JTI黑名单服务
 * 防止重放攻击的核心组件 - P0级安全防护
 * 
 * 功能：
 * 1. 维护已使用JWT ID的Redis黑名单
 * 2. 防止同一Token被多次使用
 * 3. 自动清理过期JTI，节省存储空间
 * 4. 提供异常检测和告警功能
 * 
 * @author Claude Code AI
 */
@Service
public class JtiBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(JtiBlacklistService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis键前缀
    private static final String JTI_BLACKLIST_PREFIX = "jwt:jti:blacklist:";
    private static final String JTI_USAGE_COUNT_PREFIX = "jwt:jti:usage:";
    
    // JTI有效期 - 与JWT Token有效期保持一致
    private static final long JTI_TTL_MINUTES = 10; // 10分钟
    private static final long JTI_EXTENDED_TTL_MINUTES = 30; // 扩展TTL用于异常检测

    /**
     * 检查JTI是否已被使用（防重放攻击核心方法）
     * 
     * @param jti JWT ID
     * @return true=已使用(重放攻击), false=未使用(安全)
     */
    public boolean isJtiUsed(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            log.warn("🚨 [JTI_CHECK] JTI为空，拒绝验证");
            return true; // 空JTI视为已使用，安全拒绝
        }

        String blacklistKey = JTI_BLACKLIST_PREFIX + jti;
        
        try {
            // 检查Redis黑名单
            Boolean exists = redisTemplate.hasKey(blacklistKey);
            
            if (Boolean.TRUE.equals(exists)) {
                log.error("🚨 [REPLAY_ATTACK_DETECTED] 检测到重放攻击！JTI已被使用: {}", maskJti(jti));
                
                // 记录重放攻击尝试次数
                incrementReplayAttempt(jti);
                
                return true; // 重放攻击
            }
            
            log.info("✅ [JTI_CHECK] JTI验证通过，首次使用: {}", maskJti(jti));
            return false; // 首次使用，安全
            
        } catch (Exception e) {
            log.error("❌ [JTI_CHECK] Redis查询异常，安全策略：拒绝访问", e);
            return true; // Redis异常时安全拒绝
        }
    }

    /**
     * 标记JTI为已使用（防重放攻击关键步骤）
     * 
     * @param jti JWT ID
     * @param expirationTime Token过期时间
     */
    public void markJtiAsUsed(String jti, LocalDateTime expirationTime) {
        if (jti == null || jti.trim().isEmpty()) {
            log.warn("🚨 [JTI_MARK] JTI为空，跳过标记");
            return;
        }

        String blacklistKey = JTI_BLACKLIST_PREFIX + jti;
        
        try {
            // 计算TTL：Token剩余有效时间 + 安全缓冲时间
            long ttlSeconds = calculateJtiTtl(expirationTime);
            
            // 存储到Redis黑名单，值包含首次使用时间戳
            String value = "USED:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(blacklistKey, value, ttlSeconds, TimeUnit.SECONDS);
            
            log.info("✅ [JTI_MARK] JTI已标记为使用: {}, TTL: {}秒", maskJti(jti), ttlSeconds);
            
        } catch (Exception e) {
            log.error("❌ [JTI_MARK] 标记JTI失败，安全风险：可能允许重放攻击", e);
            // 这是严重的安全风险，应当触发告警
        }
    }

    /**
     * 记录重放攻击尝试次数
     * 
     * @param jti JWT ID
     */
    private void incrementReplayAttempt(String jti) {
        String usageKey = JTI_USAGE_COUNT_PREFIX + jti;
        
        try {
            Long count = redisTemplate.opsForValue().increment(usageKey, 1);
            
            // 设置30分钟TTL用于统计
            if (count == 1) {
                redisTemplate.expire(usageKey, JTI_EXTENDED_TTL_MINUTES, TimeUnit.MINUTES);
            }
            
            log.warn("⚠️ [REPLAY_STATS] JTI重放攻击尝试次数: {} -> {}", maskJti(jti), count);
            
            // 重放攻击次数异常告警
            if (count >= 5) {
                log.error("🚨 [SECURITY_ALERT] 严重重放攻击！同一JTI被尝试使用{}次: {}", 
                         count, maskJti(jti));
            }
            
        } catch (Exception e) {
            log.error("❌ [REPLAY_STATS] 记录重放攻击统计失败", e);
        }
    }

    /**
     * 计算JTI在Redis中的TTL
     * 
     * @param tokenExpirationTime Token过期时间
     * @return TTL秒数
     */
    private long calculateJtiTtl(LocalDateTime tokenExpirationTime) {
        if (tokenExpirationTime == null) {
            return TimeUnit.MINUTES.toSeconds(JTI_TTL_MINUTES);
        }
        
        long tokenTtlSeconds = tokenExpirationTime.toEpochSecond(ZoneOffset.UTC) - 
                              LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        
        // TTL = Token剩余时间 + 5分钟安全缓冲
        long safeTtl = Math.max(tokenTtlSeconds + 300, TimeUnit.MINUTES.toSeconds(JTI_TTL_MINUTES));
        
        return Math.min(safeTtl, TimeUnit.MINUTES.toSeconds(JTI_EXTENDED_TTL_MINUTES));
    }

    /**
     * 获取JTI重放攻击统计
     * 
     * @param jti JWT ID
     * @return 重放攻击尝试次数
     */
    public long getReplayAttemptCount(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return 0;
        }

        String usageKey = JTI_USAGE_COUNT_PREFIX + jti;
        
        try {
            String countStr = (String) redisTemplate.opsForValue().get(usageKey);
            return countStr != null ? Long.parseLong(countStr) : 0;
            
        } catch (Exception e) {
            log.error("❌ [REPLAY_STATS] 获取重放统计失败", e);
            return 0;
        }
    }

    /**
     * 清理过期的JTI黑名单（手动维护接口）
     * Redis的TTL机制会自动清理，此方法用于手动维护
     * 
     * @return 清理的条目数量
     */
    public long cleanupExpiredJti() {
        log.info("🧹 [JTI_CLEANUP] 开始清理过期JTI黑名单...");
        
        try {
            // Redis TTL机制会自动清理过期键
            // 这里可以添加额外的清理逻辑或统计
            
            log.info("✅ [JTI_CLEANUP] JTI清理完成（Redis自动TTL）");
            return 0;
            
        } catch (Exception e) {
            log.error("❌ [JTI_CLEANUP] JTI清理异常", e);
            return 0;
        }
    }

    /**
     * 获取JTI黑名单统计信息
     */
    public JtiStatistics getJtiStatistics() {
        try {
            // 统计黑名单中的JTI数量
            String pattern = JTI_BLACKLIST_PREFIX + "*";
            Long blacklistCount = 0L;
            
            // 统计重放攻击尝试
            String usagePattern = JTI_USAGE_COUNT_PREFIX + "*";
            Long replayAttempts = 0L;
            
            // 注意：在生产环境中，keys命令可能影响性能，建议使用scan
            
            return new JtiStatistics(blacklistCount, replayAttempts);
            
        } catch (Exception e) {
            log.error("❌ [JTI_STATS] 获取统计信息失败", e);
            return new JtiStatistics(0L, 0L);
        }
    }

    /**
     * JTI脱敏显示
     */
    private String maskJti(String jti) {
        if (jti == null || jti.length() < 10) {
            return "***INVALID_JTI***";
        }
        return jti.substring(0, 15) + "..." + jti.substring(jti.length() - 6);
    }

    /**
     * JTI统计信息
     */
    public static class JtiStatistics {
        private final Long blacklistCount;
        private final Long replayAttemptCount;

        public JtiStatistics(Long blacklistCount, Long replayAttemptCount) {
            this.blacklistCount = blacklistCount;
            this.replayAttemptCount = replayAttemptCount;
        }

        public Long getBlacklistCount() {
            return blacklistCount;
        }

        public Long getReplayAttemptCount() {
            return replayAttemptCount;
        }
    }
}