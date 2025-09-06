package cn.iocoder.yudao.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JWT黑名单服务 - P1.2安全修复：防重放攻击
 * 
 * 功能：
 * 1. JWT Token黑名单管理
 * 2. 防重放攻击（jti追踪）
 * 3. Token主动撤销机制
 * 4. 内存缓存 + 自动过期清理
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Service
public class JwtBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistService.class);

    // 🗃️ 黑名单存储：jti -> 过期时间
    private final ConcurrentMap<String, LocalDateTime> jwtBlacklist = new ConcurrentHashMap<>();
    
    // 📊 使用计数统计
    private final ConcurrentMap<String, Integer> jtiUsageCount = new ConcurrentHashMap<>();
    
    /**
     * 🚫 将JWT Token加入黑名单
     * 
     * @param jwtId JWT ID (jti声明)
     * @param expirationTime Token过期时间
     */
    public void addToBlacklist(String jwtId, LocalDateTime expirationTime) {
        if (jwtId == null || jwtId.trim().isEmpty()) {
            log.warn("❌ [JWT_BLACKLIST] jwtId为空，无法加入黑名单");
            return;
        }
        
        jwtBlacklist.put(jwtId, expirationTime);
        log.info("🚫 [JWT_BLACKLIST] JWT已加入黑名单: jwtId={}, 过期时间={}", jwtId, expirationTime);
        
        // 触发清理过期条目
        cleanExpiredTokens();
    }
    
    /**
     * 🔍 检查JWT是否在黑名单中
     * 
     * @param jwtId JWT ID
     * @return true表示在黑名单中（应该拒绝）
     */
    public boolean isBlacklisted(String jwtId) {
        if (jwtId == null || jwtId.trim().isEmpty()) {
            log.warn("❌ [JWT_BLACKLIST] jwtId为空，默认不在黑名单中");
            return false;
        }
        
        boolean isBlacklisted = jwtBlacklist.containsKey(jwtId);
        
        if (isBlacklisted) {
            log.warn("🚫 [JWT_BLACKLIST] Token在黑名单中，拒绝访问: jwtId={}", jwtId);
        }
        
        return isBlacklisted;
    }
    
    /**
     * 🔄 检测和防护JWT重放攻击
     * 
     * @param jwtId JWT ID
     * @return true表示安全（首次使用或允许的重复使用），false表示疑似重放攻击
     */
    public boolean validateJwtReplayProtection(String jwtId) {
        if (jwtId == null || jwtId.trim().isEmpty()) {
            log.warn("⚠️ [REPLAY_PROTECTION] jwtId为空，跳过重放检测");
            return true; // 允许通过，但记录警告
        }
        
        // 检查是否在黑名单中
        if (isBlacklisted(jwtId)) {
            log.error("🚨 [REPLAY_PROTECTION] 黑名单Token尝试重用: jwtId={}", jwtId);
            return false;
        }
        
        // 计数追踪（检测异常频繁使用）
        int usageCount = jtiUsageCount.compute(jwtId, (k, v) -> v == null ? 1 : v + 1);
        
        // 🚨 重放攻击检测：同一Token在短时间内大量使用
        if (usageCount > 100) { // 阈值：单个Token最多100次使用
            log.error("🚨 [REPLAY_PROTECTION] 疑似重放攻击，Token使用次数过多: jwtId={}, 使用次数={}", 
                    jwtId, usageCount);
            
            // 自动加入黑名单
            addToBlacklist(jwtId, LocalDateTime.now().plusHours(1));
            return false;
        }
        
        if (usageCount % 10 == 0) { // 每10次使用记录一次
            log.info("📊 [REPLAY_PROTECTION] Token使用计数: jwtId={}, 使用次数={}", jwtId, usageCount);
        }
        
        return true;
    }
    
    /**
     * 🧹 清理过期的黑名单条目
     */
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int initialSize = jwtBlacklist.size();
        
        jwtBlacklist.entrySet().removeIf(entry -> {
            boolean isExpired = entry.getValue().isBefore(now);
            if (isExpired) {
                String jwtId = entry.getKey();
                log.info("🧹 [JWT_BLACKLIST] 清理过期Token: jwtId={}, 过期时间={}", jwtId, entry.getValue());
                
                // 同时清理使用计数
                jtiUsageCount.remove(jwtId);
            }
            return isExpired;
        });
        
        int cleanedCount = initialSize - jwtBlacklist.size();
        if (cleanedCount > 0) {
            log.info("🧹 [JWT_BLACKLIST] 清理完成，移除{}个过期Token，当前黑名单大小: {}", 
                    cleanedCount, jwtBlacklist.size());
        }
    }
    
    /**
     * 📊 获取黑名单统计信息
     */
    public BlacklistStats getBlacklistStats() {
        cleanExpiredTokens(); // 先清理过期条目
        
        return new BlacklistStats(
                jwtBlacklist.size(),
                jtiUsageCount.size(),
                LocalDateTime.now()
        );
    }
    
    /**
     * 🚨 强制撤销特定用户的所有Token
     * 
     * @param userId 用户ID
     */
    public void revokeUserTokens(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("❌ [TOKEN_REVOKE] 用户ID为空");
            return;
        }
        
        LocalDateTime revocationTime = LocalDateTime.now().plusHours(1);
        
        // 添加用户级别的撤销标记（简化实现：基于用户ID前缀匹配）
        String userPrefix = "jwt_" + userId + "_";
        
        jtiUsageCount.keySet().stream()
                .filter(jwtId -> jwtId.startsWith(userPrefix))
                .forEach(jwtId -> {
                    addToBlacklist(jwtId, revocationTime);
                    log.info("🚨 [TOKEN_REVOKE] 用户Token已撤销: userId={}, jwtId={}", userId, jwtId);
                });
        
        log.info("🚨 [TOKEN_REVOKE] 用户所有Token撤销完成: userId={}", userId);
    }
    
    /**
     * 📊 黑名单统计信息
     */
    public static class BlacklistStats {
        public final int blacklistedTokens;
        public final int trackedTokens;
        public final LocalDateTime lastUpdated;
        
        public BlacklistStats(int blacklistedTokens, int trackedTokens, LocalDateTime lastUpdated) {
            this.blacklistedTokens = blacklistedTokens;
            this.trackedTokens = trackedTokens;
            this.lastUpdated = lastUpdated;
        }
        
        @Override
        public String toString() {
            return String.format("JWT黑名单统计 - 黑名单Token: %d, 跟踪Token: %d, 更新时间: %s", 
                    blacklistedTokens, trackedTokens, lastUpdated);
        }
    }
}