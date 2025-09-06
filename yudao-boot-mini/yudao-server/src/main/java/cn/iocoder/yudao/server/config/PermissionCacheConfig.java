package cn.iocoder.yudao.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 🔧 权限缓存配置类 - P0级性能优化配置管理
 * 
 * QA修复：增强配置验证，确保所有参数的有效性检查
 * 
 * @author Claude AI - P0级权限缓存系统优化
 * @since 2025-08-20
 */
@Configuration
@ConfigurationProperties(prefix = "yudao.permission.cache")
@Slf4j
public class PermissionCacheConfig {
    
    /**
     * 是否启用权限缓存（默认启用）
     */
    private boolean enabled = true;
    
    /**
     * 权限缓存TTL（秒），默认15分钟
     */
    private long ttl = 900;  // 15 * 60 = 900秒
    
    /**
     * Redis键前缀
     */
    private String keyPrefix = "permission:user:";
    
    /**
     * 最大缓存用户数量（防止内存溢出）
     */
    private int maxCachedUsers = 10000;
    
    /**
     * 是否启用性能监控
     */
    private boolean metricsEnabled = true;
    
    /**
     * 缓存预热用户列表（优先缓存的核心用户）
     */
    private String[] warmupUsers = {"SYSTEM_ADMIN_001", "PRINCIPAL_001"};
    
    /**
     * QA修复：配置参数验证和初始化
     */
    @PostConstruct
    public void validateAndInitialize() {
        log.info("🔧 [PERMISSION-CACHE-CONFIG] 开始验证权限缓存配置参数");
        
        List<String> validationErrors = new ArrayList<>();
        
        // 验证TTL设置
        if (ttl <= 0) {
            validationErrors.add("TTL必须大于0秒，当前值: " + ttl);
        } else if (ttl < 60) {
            log.warn("⚠️ [PERMISSION-CACHE-CONFIG] TTL设置过短可能影响缓存效果: {}秒", ttl);
        } else if (ttl > 3600) {
            log.warn("⚠️ [PERMISSION-CACHE-CONFIG] TTL设置过长可能导致权限更新延迟: {}秒", ttl);
        }
        
        // 验证键前缀
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            validationErrors.add("keyPrefix不能为空");
        } else if (!keyPrefix.endsWith(":")) {
            // 自动修正键前缀
            keyPrefix = keyPrefix + ":";
            log.info("🔧 [PERMISSION-CACHE-CONFIG] 自动修正keyPrefix: {}", keyPrefix);
        }
        
        // 验证最大缓存用户数
        if (maxCachedUsers <= 0) {
            validationErrors.add("maxCachedUsers必须大于0，当前值: " + maxCachedUsers);
        } else if (maxCachedUsers < 100) {
            log.warn("⚠️ [PERMISSION-CACHE-CONFIG] maxCachedUsers设置过小可能影响性能: {}", maxCachedUsers);
        } else if (maxCachedUsers > 100000) {
            log.warn("⚠️ [PERMISSION-CACHE-CONFIG] maxCachedUsers设置过大可能消耗过多内存: {} (约{}MB)", 
                    maxCachedUsers, maxCachedUsers * 5 / 1024);
        }
        
        // 验证预热用户列表
        if (warmupUsers != null) {
            List<String> validWarmupUsers = new ArrayList<>();
            for (String user : warmupUsers) {
                if (user != null && !user.trim().isEmpty()) {
                    validWarmupUsers.add(user.trim());
                }
            }
            warmupUsers = validWarmupUsers.toArray(new String[0]);
            
            if (warmupUsers.length > maxCachedUsers / 10) {
                log.warn("⚠️ [PERMISSION-CACHE-CONFIG] 预热用户数量过多: {} (建议不超过最大缓存用户数的10%: {})", 
                        warmupUsers.length, maxCachedUsers / 10);
            }
        }
        
        // 输出验证错误
        if (!validationErrors.isEmpty()) {
            String errorMessage = "权限缓存配置验证失败: " + String.join(", ", validationErrors);
            log.error("❌ [PERMISSION-CACHE-CONFIG] {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        // 输出配置摘要
        logConfigurationSummary();
        
        log.info("✅ [PERMISSION-CACHE-CONFIG] 权限缓存配置验证通过");
    }
    
    /**
     * 📊 输出配置摘要
     */
    private void logConfigurationSummary() {
        log.info("📊 [PERMISSION-CACHE-CONFIG] 权限缓存配置摘要:");
        log.info("   └── 启用状态: {}", enabled ? "✅ 已启用" : "❌ 已禁用");
        log.info("   └── 缓存TTL: {} 秒 ({}分钟)", ttl, ttl / 60);
        log.info("   └── Redis键前缀: '{}'", keyPrefix);
        log.info("   └── 最大缓存用户数: {} (预估内存: {}MB)", maxCachedUsers, maxCachedUsers * 5 / 1024);
        log.info("   └── 性能监控: {}", metricsEnabled ? "✅ 已启用" : "❌ 已禁用");
        
        if (warmupUsers != null && warmupUsers.length > 0) {
            log.info("   └── 预热用户数量: {} 个 {}", warmupUsers.length, Arrays.toString(warmupUsers));
        }
        
        // 性能建议
        if (enabled) {
            long estimatedMemoryMB = maxCachedUsers * 5 / 1024;
            if (estimatedMemoryMB > 512) {
                log.warn("💡 [PERMISSION-CACHE-CONFIG] 性能建议: 缓存预估内存使用{}MB，建议确保Redis有足够内存", estimatedMemoryMB);
            }
            
            if (ttl < 300) {
                log.warn("💡 [PERMISSION-CACHE-CONFIG] 性能建议: TTL过短可能导致缓存命中率低，建议至少5分钟");
            }
        }
    }
    
    /**
     * 🔍 验证配置是否有效
     * 
     * @return true表示配置有效
     */
    public boolean isConfigurationValid() {
        return enabled &&
               ttl > 0 &&
               keyPrefix != null && !keyPrefix.trim().isEmpty() &&
               maxCachedUsers > 0;
    }
    
    /**
     * 📏 获取估算内存使用量（MB）
     * 
     * @return 预估内存使用量
     */
    public long getEstimatedMemoryUsageMB() {
        return maxCachedUsers * 5 / 1024; // 每个用户权限约5KB
    }
    
    /**
     * ⏰ 获取TTL的毫秒值
     * 
     * @return TTL毫秒值
     */
    public long getTtlMillis() {
        return ttl * 1000;
    }
    
    /**
     * 🎯 检查是否应该预热指定用户
     * 
     * @param userId 用户ID
     * @return true表示应该预热
     */
    public boolean shouldWarmupUser(String userId) {
        if (warmupUsers == null || userId == null) {
            return false;
        }
        
        for (String warmupUser : warmupUsers) {
            if (userId.equals(warmupUser)) {
                return true;
            }
        }
        return false;
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public long getTtl() {
        return ttl;
    }
    
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
    
    public String getKeyPrefix() {
        return keyPrefix;
    }
    
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
    
    public int getMaxCachedUsers() {
        return maxCachedUsers;
    }
    
    public void setMaxCachedUsers(int maxCachedUsers) {
        this.maxCachedUsers = maxCachedUsers;
    }
    
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
    
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
    
    public String[] getWarmupUsers() {
        return warmupUsers;
    }
    
    public void setWarmupUsers(String[] warmupUsers) {
        this.warmupUsers = warmupUsers;
    }
}