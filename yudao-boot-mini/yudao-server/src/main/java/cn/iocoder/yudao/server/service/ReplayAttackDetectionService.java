package cn.iocoder.yudao.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 增强重放攻击检测服务
 * P1.3安全升级：IP+UserAgent异常检测和频率限制
 * 
 * 核心功能：
 * 1. IP地址异常检测
 * 2. UserAgent变化监控  
 * 3. Token使用频率限制
 * 4. 地理位置异常告警
 * 5. 设备指纹识别
 * 
 * @author Claude Code AI
 */
@Service
public class ReplayAttackDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ReplayAttackDetectionService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis键前缀
    private static final String USER_IP_HISTORY_PREFIX = "security:user:ip:";
    private static final String USER_AGENT_HISTORY_PREFIX = "security:user:agent:";
    private static final String TOKEN_USAGE_FREQ_PREFIX = "security:token:freq:";
    private static final String SUSPICIOUS_ACTIVITY_PREFIX = "security:suspicious:";
    
    // 安全阈值配置
    private static final long TOKEN_USAGE_LIMIT = 60; // 每分钟最多60次Token使用
    private static final long IP_CHANGE_THRESHOLD = 3; // IP变更次数阈值
    private static final long AGENT_CHANGE_THRESHOLD = 2; // UserAgent变更次数阈值
    private static final long HISTORY_TTL_HOURS = 24; // 历史记录保存24小时

    /**
     * 检测Token使用是否存在异常行为
     * 
     * @param userId 用户ID
     * @param jti JWT ID
     * @param request HTTP请求
     * @return 检测结果
     */
    public SecurityCheckResult checkTokenUsage(String userId, String jti, HttpServletRequest request) {
        log.info("🔍 [SECURITY_CHECK] 开始异常检测: userId={}, JTI={}", userId, maskJti(jti));
        
        SecurityCheckResult result = new SecurityCheckResult();
        
        try {
            // 1. Token使用频率检测
            boolean frequencyAnomalyDetected = checkTokenUsageFrequency(userId, jti);
            if (frequencyAnomalyDetected) {
                result.addWarning("Token使用频率异常");
            }

            // 2. IP地址异常检测
            String clientIp = getClientIpAddress(request);
            boolean ipAnomalyDetected = checkIpAddressAnomaly(userId, clientIp);
            if (ipAnomalyDetected) {
                result.addWarning("IP地址异常变更");
            }

            // 3. UserAgent异常检测
            String userAgent = request.getHeader("User-Agent");
            boolean agentAnomalyDetected = checkUserAgentAnomaly(userId, userAgent);
            if (agentAnomalyDetected) {
                result.addWarning("设备指纹异常变更");
            }

            // 4. 地理位置异常检测（简化版）
            boolean geoAnomalyDetected = checkGeographicalAnomaly(userId, clientIp);
            if (geoAnomalyDetected) {
                result.addWarning("地理位置异常");
            }

            // 5. 综合风险评估
            int riskScore = calculateRiskScore(result.getWarnings().size(), userId);
            result.setRiskScore(riskScore);
            result.setRiskLevel(determineRiskLevel(riskScore));

            // 6. 记录安全事件
            if (result.getWarnings().size() > 0) {
                recordSecurityEvent(userId, jti, clientIp, userAgent, result);
            }

            log.info("✅ [SECURITY_CHECK] 异常检测完成: userId={}, 风险等级={}, 警告数={}", 
                    userId, result.getRiskLevel(), result.getWarnings().size());
            
            return result;

        } catch (Exception e) {
            log.error("❌ [SECURITY_CHECK] 异常检测失败", e);
            result.addWarning("安全检测服务异常");
            result.setRiskScore(50); // 中等风险
            result.setRiskLevel("MEDIUM");
            return result;
        }
    }

    /**
     * 检测Token使用频率是否异常
     */
    private boolean checkTokenUsageFrequency(String userId, String jti) {
        String freqKey = TOKEN_USAGE_FREQ_PREFIX + userId;
        
        try {
            // 使用滑动窗口计数器
            Long count = redisTemplate.opsForValue().increment(freqKey, 1);
            
            if (count == 1) {
                // 第一次访问，设置1分钟过期时间
                redisTemplate.expire(freqKey, 1, TimeUnit.MINUTES);
            }

            if (count > TOKEN_USAGE_LIMIT) {
                log.warn("⚠️ [FREQ_ANOMALY] Token使用频率异常: userId={}, count={}/min", userId, count);
                return true;
            }

            log.info("✅ [FREQ_CHECK] Token使用频率正常: userId={}, count={}/min", userId, count);
            return false;

        } catch (Exception e) {
            log.error("❌ [FREQ_CHECK] 频率检测失败", e);
            return false; // 检测失败不应该阻止正常业务
        }
    }

    /**
     * 检测IP地址变更异常
     */
    private boolean checkIpAddressAnomaly(String userId, String currentIp) {
        String ipHistoryKey = USER_IP_HISTORY_PREFIX + userId;
        
        try {
            // 获取历史IP记录
            String lastIp = (String) redisTemplate.opsForValue().get(ipHistoryKey);
            
            if (lastIp == null) {
                // 首次访问，记录IP
                redisTemplate.opsForValue().set(ipHistoryKey, currentIp, HISTORY_TTL_HOURS, TimeUnit.HOURS);
                log.info("📍 [IP_TRACK] 首次IP记录: userId={}, ip={}", userId, currentIp);
                return false;
            }

            if (!currentIp.equals(lastIp)) {
                // IP发生变更
                log.warn("⚠️ [IP_ANOMALY] IP地址变更: userId={}, 前次={}, 当前={}", userId, lastIp, currentIp);
                
                // 更新IP记录
                redisTemplate.opsForValue().set(ipHistoryKey, currentIp, HISTORY_TTL_HOURS, TimeUnit.HOURS);
                
                // 记录IP变更次数
                String ipChangeCountKey = ipHistoryKey + ":changes";
                Long changeCount = redisTemplate.opsForValue().increment(ipChangeCountKey, 1);
                redisTemplate.expire(ipChangeCountKey, HISTORY_TTL_HOURS, TimeUnit.HOURS);
                
                if (changeCount > IP_CHANGE_THRESHOLD) {
                    log.error("🚨 [IP_ANOMALY_ALERT] IP频繁变更: userId={}, 变更次数={}", userId, changeCount);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("❌ [IP_CHECK] IP异常检测失败", e);
            return false;
        }
    }

    /**
     * 检测UserAgent变更异常
     */
    private boolean checkUserAgentAnomaly(String userId, String currentAgent) {
        if (currentAgent == null || currentAgent.trim().isEmpty()) {
            log.warn("⚠️ [AGENT_ANOMALY] UserAgent为空: userId={}", userId);
            return true;
        }

        String agentHistoryKey = USER_AGENT_HISTORY_PREFIX + userId;
        
        try {
            String lastAgent = (String) redisTemplate.opsForValue().get(agentHistoryKey);
            
            if (lastAgent == null) {
                // 首次访问，记录UserAgent
                redisTemplate.opsForValue().set(agentHistoryKey, currentAgent, HISTORY_TTL_HOURS, TimeUnit.HOURS);
                log.info("🖥️ [AGENT_TRACK] 首次UserAgent记录: userId={}", userId);
                return false;
            }

            if (!currentAgent.equals(lastAgent)) {
                // UserAgent发生变更
                log.warn("⚠️ [AGENT_ANOMALY] UserAgent变更: userId={}", userId);
                
                // 更新UserAgent记录
                redisTemplate.opsForValue().set(agentHistoryKey, currentAgent, HISTORY_TTL_HOURS, TimeUnit.HOURS);
                
                // 记录变更次数
                String agentChangeCountKey = agentHistoryKey + ":changes";
                Long changeCount = redisTemplate.opsForValue().increment(agentChangeCountKey, 1);
                redisTemplate.expire(agentChangeCountKey, HISTORY_TTL_HOURS, TimeUnit.HOURS);
                
                if (changeCount > AGENT_CHANGE_THRESHOLD) {
                    log.error("🚨 [AGENT_ANOMALY_ALERT] UserAgent频繁变更: userId={}, 变更次数={}", userId, changeCount);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("❌ [AGENT_CHECK] UserAgent异常检测失败", e);
            return false;
        }
    }

    /**
     * 检测地理位置异常（简化版）
     */
    private boolean checkGeographicalAnomaly(String userId, String ip) {
        // 简化实现：检测明显的地理位置跳跃
        // 实际应用中应该使用IP地理位置数据库
        
        try {
            // 这里可以集成第三方地理位置服务
            // 暂时使用简单的IP段检测
            
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("127.")) {
                // 内网IP，跳过地理检测
                return false;
            }

            // 实际实现中，可以检测IP地理位置的快速变化
            // 例如：从北京跳跃到美国，时间间隔很短
            
            return false; // 简化实现暂不检测

        } catch (Exception e) {
            log.error("❌ [GEO_CHECK] 地理位置检测失败", e);
            return false;
        }
    }

    /**
     * 计算风险评分
     */
    private int calculateRiskScore(int warningCount, String userId) {
        int baseScore = warningCount * 25; // 每个警告25分
        
        // 可以根据用户历史行为调整评分
        // 例如：新用户风险评分较高，老用户风险评分较低
        
        return Math.min(baseScore, 100); // 最高100分
    }

    /**
     * 确定风险等级
     */
    private String determineRiskLevel(int riskScore) {
        if (riskScore >= 75) {
            return "HIGH";
        } else if (riskScore >= 50) {
            return "MEDIUM";
        } else if (riskScore >= 25) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }

    /**
     * 记录安全事件
     */
    private void recordSecurityEvent(String userId, String jti, String ip, String userAgent, SecurityCheckResult result) {
        String eventKey = SUSPICIOUS_ACTIVITY_PREFIX + userId + ":" + System.currentTimeMillis();
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("jti", maskJti(jti));
            event.put("ip", ip);
            event.put("userAgent", maskUserAgent(userAgent));
            event.put("warnings", result.getWarnings());
            event.put("riskScore", result.getRiskScore());
            event.put("riskLevel", result.getRiskLevel());
            event.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            redisTemplate.opsForValue().set(eventKey, event, 7, TimeUnit.DAYS); // 保存7天
            
            log.info("📝 [SECURITY_EVENT] 安全事件已记录: userId={}, riskLevel={}", userId, result.getRiskLevel());

        } catch (Exception e) {
            log.error("❌ [SECURITY_EVENT] 安全事件记录失败", e);
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }

    /**
     * JTI脱敏显示
     */
    private String maskJti(String jti) {
        if (jti == null || jti.length() < 15) {
            return "***INVALID_JTI***";
        }
        return jti.substring(0, 15) + "..." + jti.substring(jti.length() - 8);
    }

    /**
     * UserAgent脱敏显示
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() < 20) {
            return "***MASKED_USER_AGENT***";
        }
        return userAgent.substring(0, 20) + "...";
    }

    /**
     * 安全检查结果
     */
    public static class SecurityCheckResult {
        private java.util.List<String> warnings = new java.util.ArrayList<>();
        private int riskScore = 0;
        private String riskLevel = "MINIMAL";

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public java.util.List<String> getWarnings() {
            return warnings;
        }

        public int getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(int riskScore) {
            this.riskScore = riskScore;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public boolean isHighRisk() {
            return "HIGH".equals(riskLevel);
        }

        public boolean hasSuspiciousActivity() {
            return !warnings.isEmpty();
        }
    }
}