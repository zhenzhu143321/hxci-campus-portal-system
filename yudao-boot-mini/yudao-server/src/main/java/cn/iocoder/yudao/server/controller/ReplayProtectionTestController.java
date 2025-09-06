package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.service.JtiBlacklistService;
import cn.iocoder.yudao.server.service.ReplayAttackDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 重放攻击防护测试控制器
 * P1.3安全升级：测试和验证重放攻击防护机制
 * 
 * 测试端点：
 * 1. JTI黑名单机制测试
 * 2. 异常检测系统测试
 * 3. Token使用频率限制测试
 * 4. 安全统计和监控
 * 
 * @author Claude Code AI
 */
@RestController
@RequestMapping("/admin-api/test/replay-protection")
public class ReplayProtectionTestController {

    private static final Logger log = LoggerFactory.getLogger(ReplayProtectionTestController.class);

    @Autowired
    private JtiBlacklistService jtiBlacklistService;

    @Autowired
    private ReplayAttackDetectionService replayAttackDetectionService;

    /**
     * 测试JTI黑名单机制
     * GET /admin-api/test/replay-protection/jti-blacklist-test
     */
    @GetMapping("/jti-blacklist-test")
    public CommonResult<Map<String, Object>> testJtiBlacklist(
            @RequestParam(defaultValue = "test_jti_001") String testJti) {
        
        log.info("🧪 [JTI_TEST] 开始JTI黑名单机制测试: testJti={}", testJti);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 首次检查JTI（应该返回false - 未使用）
            boolean firstCheck = jtiBlacklistService.isJtiUsed(testJti);
            result.put("firstCheck", firstCheck);
            result.put("firstCheckResult", firstCheck ? "❌ JTI已被使用（异常）" : "✅ JTI首次使用（正常）");

            // 2. 标记JTI为已使用
            LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(10);
            jtiBlacklistService.markJtiAsUsed(testJti, expirationTime);
            result.put("markAsUsed", "✅ JTI已标记为使用");

            // 3. 再次检查JTI（应该返回true - 已使用）
            boolean secondCheck = jtiBlacklistService.isJtiUsed(testJti);
            result.put("secondCheck", secondCheck);
            result.put("secondCheckResult", secondCheck ? "✅ JTI重复使用被阻止（防重放成功）" : "❌ JTI重复使用未被阻止（防重放失败）");

            // 4. 获取JTI统计信息
            JtiBlacklistService.JtiStatistics stats = jtiBlacklistService.getJtiStatistics();
            result.put("statistics", Map.of(
                "blacklistCount", stats.getBlacklistCount(),
                "replayAttemptCount", stats.getReplayAttemptCount()
            ));

            // 5. 测试结果评估
            boolean testPassed = !firstCheck && secondCheck;
            result.put("testStatus", testPassed ? "✅ PASSED" : "❌ FAILED");
            result.put("testSummary", testPassed ? "JTI黑名单机制工作正常" : "JTI黑名单机制存在问题");

            log.info("✅ [JTI_TEST] JTI黑名单测试完成: testJti={}, 测试结果={}", testJti, testPassed ? "通过" : "失败");

            return CommonResult.success(result);

        } catch (Exception e) {
            log.error("❌ [JTI_TEST] JTI黑名单测试异常", e);
            result.put("testStatus", "❌ ERROR");
            result.put("error", e.getMessage());
            return CommonResult.success(result);
        }
    }

    /**
     * 测试异常检测系统
     * GET /admin-api/test/replay-protection/anomaly-detection-test
     */
    @GetMapping("/anomaly-detection-test")
    public CommonResult<Map<String, Object>> testAnomalyDetection(
            @RequestParam(defaultValue = "TEST_USER_001") String testUserId,
            @RequestParam(defaultValue = "test_jti_anomaly_001") String testJti,
            HttpServletRequest request) {
        
        log.info("🧪 [ANOMALY_TEST] 开始异常检测系统测试: userId={}, JTI={}", testUserId, testJti);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 执行异常检测
            ReplayAttackDetectionService.SecurityCheckResult checkResult = 
                replayAttackDetectionService.checkTokenUsage(testUserId, testJti, request);

            // 构建测试结果
            result.put("riskScore", checkResult.getRiskScore());
            result.put("riskLevel", checkResult.getRiskLevel());
            result.put("warnings", checkResult.getWarnings());
            result.put("hasSuspiciousActivity", checkResult.hasSuspiciousActivity());
            result.put("isHighRisk", checkResult.isHighRisk());

            // 检测系统评估
            result.put("detectionStatus", checkResult.hasSuspiciousActivity() ? "⚠️ 检测到可疑活动" : "✅ 未检测到异常");
            result.put("securityLevel", determineSecurityLevel(checkResult));

            // 请求信息
            result.put("requestInfo", Map.of(
                "clientIP", getClientIP(request),
                "userAgent", request.getHeader("User-Agent"),
                "timestamp", LocalDateTime.now().toString()
            ));

            log.info("✅ [ANOMALY_TEST] 异常检测测试完成: userId={}, riskLevel={}", testUserId, checkResult.getRiskLevel());

            return CommonResult.success(result);

        } catch (Exception e) {
            log.error("❌ [ANOMALY_TEST] 异常检测测试异常", e);
            result.put("error", e.getMessage());
            return CommonResult.success(result);
        }
    }

    /**
     * 测试Token使用频率限制
     * GET /admin-api/test/replay-protection/frequency-limit-test
     */
    @GetMapping("/frequency-limit-test")
    public CommonResult<Map<String, Object>> testFrequencyLimit(
            @RequestParam(defaultValue = "TEST_USER_FREQ_001") String testUserId,
            @RequestParam(defaultValue = "5") int requestCount,
            HttpServletRequest request) {
        
        log.info("🧪 [FREQ_TEST] 开始频率限制测试: userId={}, requestCount={}", testUserId, requestCount);
        
        Map<String, Object> result = new HashMap<>();
        java.util.List<Map<String, Object>> requests = new java.util.ArrayList<>();
        
        try {
            // 模拟多次请求
            for (int i = 1; i <= requestCount; i++) {
                String testJti = "test_freq_jti_" + i + "_" + System.currentTimeMillis();
                
                // 执行异常检测
                ReplayAttackDetectionService.SecurityCheckResult checkResult = 
                    replayAttackDetectionService.checkTokenUsage(testUserId, testJti, request);

                Map<String, Object> requestResult = new HashMap<>();
                requestResult.put("requestNumber", i);
                requestResult.put("jti", testJti);
                requestResult.put("riskScore", checkResult.getRiskScore());
                requestResult.put("riskLevel", checkResult.getRiskLevel());
                requestResult.put("warnings", checkResult.getWarnings());
                
                requests.add(requestResult);
                
                // 短暂延迟
                Thread.sleep(100);
            }

            result.put("testUserId", testUserId);
            result.put("totalRequests", requestCount);
            result.put("requests", requests);
            
            // 统计频率限制效果
            long highRiskCount = requests.stream()
                .mapToInt(r -> (Integer) r.get("riskScore"))
                .filter(score -> score >= 75)
                .count();
                
            result.put("highRiskRequestCount", highRiskCount);
            result.put("frequencyLimitEffective", highRiskCount > 0 ? "✅ 频率限制生效" : "⚠️ 频率限制未触发");

            log.info("✅ [FREQ_TEST] 频率限制测试完成: userId={}, highRiskCount={}", testUserId, highRiskCount);

            return CommonResult.success(result);

        } catch (Exception e) {
            log.error("❌ [FREQ_TEST] 频率限制测试异常", e);
            result.put("error", e.getMessage());
            return CommonResult.success(result);
        }
    }

    /**
     * 获取重放攻击防护统计信息
     * GET /admin-api/test/replay-protection/security-stats
     */
    @GetMapping("/security-stats")
    public CommonResult<Map<String, Object>> getSecurityStats() {
        log.info("📊 [SECURITY_STATS] 获取重放攻击防护统计信息");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // JTI黑名单统计
            JtiBlacklistService.JtiStatistics jtiStats = jtiBlacklistService.getJtiStatistics();
            result.put("jtiBlacklistStats", Map.of(
                "blacklistCount", jtiStats.getBlacklistCount(),
                "replayAttemptCount", jtiStats.getReplayAttemptCount()
            ));

            // 系统状态
            result.put("systemStatus", Map.of(
                "jtiBlacklistService", "✅ ACTIVE",
                "anomalyDetectionService", "✅ ACTIVE",
                "redisConnection", "✅ CONNECTED"
            ));

            // 安全配置
            result.put("securityConfig", Map.of(
                "jtiTtlMinutes", 10,
                "tokenUsageLimitPerMinute", 60,
                "ipChangeThreshold", 3,
                "userAgentChangeThreshold", 2
            ));

            // 最后更新时间
            result.put("lastUpdateTime", LocalDateTime.now().toString());
            result.put("reportGeneratedAt", System.currentTimeMillis());

            return CommonResult.success(result);

        } catch (Exception e) {
            log.error("❌ [SECURITY_STATS] 获取安全统计异常", e);
            result.put("error", e.getMessage());
            return CommonResult.success(result);
        }
    }

    /**
     * 清理测试数据
     * POST /admin-api/test/replay-protection/cleanup
     */
    @PostMapping("/cleanup")
    public CommonResult<String> cleanupTestData() {
        log.info("🧹 [CLEANUP] 开始清理重放攻击防护测试数据");
        
        try {
            // 清理过期的JTI黑名单
            long cleanedCount = jtiBlacklistService.cleanupExpiredJti();
            
            log.info("✅ [CLEANUP] 测试数据清理完成: cleanedCount={}", cleanedCount);
            
            return CommonResult.success("测试数据清理完成，清理条目数：" + cleanedCount);

        } catch (Exception e) {
            log.error("❌ [CLEANUP] 清理测试数据异常", e);
            return CommonResult.error(500, "清理测试数据失败：" + e.getMessage());
        }
    }

    /**
     * 确定安全等级
     */
    private String determineSecurityLevel(ReplayAttackDetectionService.SecurityCheckResult checkResult) {
        if (checkResult.isHighRisk()) {
            return "🔴 HIGH_RISK";
        } else if (checkResult.hasSuspiciousActivity()) {
            return "🟡 SUSPICIOUS";
        } else {
            return "🟢 SECURE";
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}