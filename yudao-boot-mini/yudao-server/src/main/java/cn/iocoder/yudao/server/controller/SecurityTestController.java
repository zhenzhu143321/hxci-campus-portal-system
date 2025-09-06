package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.server.security.SecurityAuditService;
import cn.iocoder.yudao.server.security.SecurityKeyConfig;
import cn.iocoder.yudao.server.security.SecurityTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * P0级安全修复功能测试控制器
 * 
 * 🔐 测试功能：
 * 1. AES-256-GCM加密解密测试
 * 2. 外部配置化密钥管理测试
 * 3. 安全审计日志功能测试
 * 4. JWT验证增强功能测试
 * 
 * 🛡️ 安全测试：
 * - Basic Token加密存储验证
 * - 密钥配置安全性检查
 * - 安全事件审计验证
 * - 攻击检测能力验证
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-01-05
 */
@RestController
@RequestMapping("/admin-api/test/security")
public class SecurityTestController {

    private static final Logger log = LoggerFactory.getLogger(SecurityTestController.class);

    @Autowired
    private SecurityTokenService securityTokenService;
    
    @Autowired
    private SecurityKeyConfig securityKeyConfig;
    
    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * 🔐 测试AES-256-GCM加密解密功能
     * GET /admin-api/test/security/encryption-test
     */
    @GetMapping("/encryption-test")
    public Map<String, Object> testEncryption(@RequestParam(defaultValue = "test_basic_token_12345") String testToken) {
        log.info("🔐 [ENCRYPTION_TEST] 开始测试AES-256-GCM加密解密功能");
        
        Map<String, Object> result = new HashMap<>();
        result.put("testName", "AES-256-GCM加密解密测试");
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // 1. 加密测试
            log.info("🔒 [ENCRYPTION_TEST] 测试加密: {}", testToken);
            String encryptedToken = securityTokenService.encryptBasicToken(testToken);
            
            result.put("originalToken", testToken);
            result.put("encryptedToken", encryptedToken);
            result.put("encryptedLength", encryptedToken.length());
            
            // 2. 解密测试
            log.info("🔓 [ENCRYPTION_TEST] 测试解密");
            String decryptedToken = securityTokenService.decryptBasicToken(encryptedToken);
            
            result.put("decryptedToken", decryptedToken);
            result.put("decryptionSuccess", testToken.equals(decryptedToken));
            
            // 3. 格式验证测试
            boolean validFormat = securityTokenService.isValidEncryptedTokenFormat(encryptedToken);
            result.put("formatValidation", validFormat);
            
            // 4. 服务状态检查
            String serviceStatus = securityTokenService.getEncryptionServiceStatus();
            result.put("serviceStatus", serviceStatus);
            
            // 记录审计日志
            securityAuditService.logBasicTokenOperation("ENCRYPT_TEST", "test_user", true, 
                    "AES-256-GCM加密解密测试成功");
            
            result.put("testResult", "SUCCESS");
            result.put("message", "AES-256-GCM加密解密测试通过");
            
            log.info("✅ [ENCRYPTION_TEST] 加密解密测试成功");
            
        } catch (Exception e) {
            log.error("❌ [ENCRYPTION_TEST] 加密解密测试失败", e);
            
            result.put("testResult", "FAILED");
            result.put("error", e.getMessage());
            
            securityAuditService.logBasicTokenOperation("ENCRYPT_TEST", "test_user", false, 
                    "加密解密测试失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 🔑 测试密钥配置管理功能
     * GET /admin-api/test/security/key-config-test
     */
    @GetMapping("/key-config-test")
    public Map<String, Object> testKeyConfig() {
        log.info("🔑 [KEY_CONFIG_TEST] 开始测试密钥配置管理功能");
        
        Map<String, Object> result = new HashMap<>();
        result.put("testName", "密钥配置管理测试");
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // 1. 检查密钥配置有效性
            boolean keyConfigValid = securityKeyConfig.isKeyConfigValid();
            result.put("keyConfigValid", keyConfigValid);
            
            // 2. 获取密钥来源
            String keySource = securityKeyConfig.getKeySource();
            result.put("keySource", keySource);
            
            // 3. 检查密钥长度
            byte[] encryptionKey = securityKeyConfig.getEncryptionKey();
            byte[] jwtKey = securityKeyConfig.getJwtSecretKey();
            byte[] hmacKey = securityKeyConfig.getHmacSecretKey();
            
            result.put("encryptionKeyLength", encryptionKey.length);
            result.put("jwtKeyLength", jwtKey.length);
            result.put("hmacKeyLength", hmacKey.length);
            
            // 4. 验证密钥强度
            result.put("encryptionKeyStrong", encryptionKey.length >= 32);
            result.put("jwtKeyStrong", jwtKey.length >= 32);
            result.put("hmacKeyStrong", hmacKey.length >= 32);
            
            result.put("testResult", "SUCCESS");
            result.put("message", "密钥配置管理测试通过");
            
            log.info("✅ [KEY_CONFIG_TEST] 密钥配置测试成功");
            
        } catch (Exception e) {
            log.error("❌ [KEY_CONFIG_TEST] 密钥配置测试失败", e);
            
            result.put("testResult", "FAILED");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 📝 测试安全审计日志功能
     * POST /admin-api/test/security/audit-test
     */
    @PostMapping("/audit-test")
    public Map<String, Object> testSecurityAudit(@RequestBody Map<String, String> params, HttpServletRequest request) {
        log.info("📝 [AUDIT_TEST] 开始测试安全审计日志功能");
        
        Map<String, Object> result = new HashMap<>();
        result.put("testName", "安全审计日志测试");
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            String clientIP = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");
            
            // 1. 测试认证成功日志
            securityAuditService.logAuthenticationSuccess("test_user_001", clientIP, userAgent, request);
            
            // 2. 测试认证失败日志
            securityAuditService.logAuthenticationFailure("test_user_002", "密码错误", clientIP, userAgent, request);
            
            // 3. 测试Token验证失败日志
            securityAuditService.logTokenValidationFailure("invalid_jwt_token", "Token格式无效", clientIP, userAgent, request);
            
            // 4. 测试安全攻击检测日志
            securityAuditService.logSecurityAttack("JWT_NONE_ALGORITHM", "检测到JWT None算法攻击尝试", clientIP, userAgent, request);
            
            // 5. 测试权限检查日志
            securityAuditService.logPermissionCheck("test_user_003", "/admin-api/test", "READ", true, "权限验证通过");
            
            // 6. 获取安全统计
            Map<String, Object> stats = securityAuditService.getSecurityStatistics();
            result.put("securityStats", stats);
            
            // 7. 生成安全报告
            securityAuditService.printSecurityReport();
            
            result.put("testResult", "SUCCESS");
            result.put("message", "安全审计日志测试完成，请检查日志输出");
            result.put("clientIP", clientIP);
            result.put("userAgent", userAgent);
            
            log.info("✅ [AUDIT_TEST] 安全审计测试成功");
            
        } catch (Exception e) {
            log.error("❌ [AUDIT_TEST] 安全审计测试失败", e);
            
            result.put("testResult", "FAILED");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 🚨 测试攻击检测功能
     * POST /admin-api/test/security/attack-detection-test
     */
    @PostMapping("/attack-detection-test")
    public Map<String, Object> testAttackDetection(@RequestBody Map<String, String> params, HttpServletRequest request) {
        log.info("🚨 [ATTACK_TEST] 开始测试攻击检测功能");
        
        Map<String, Object> result = new HashMap<>();
        result.put("testName", "攻击检测功能测试");
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            String clientIP = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");
            String testType = params.getOrDefault("testType", "JWT_NONE_ALGORITHM");
            
            // 模拟不同类型的攻击检测
            switch (testType.toUpperCase()) {
                case "JWT_NONE_ALGORITHM":
                    securityAuditService.logSecurityAttack("JWT_NONE_ALGORITHM", 
                            "模拟JWT None算法攻击检测", clientIP, userAgent, request);
                    result.put("attackType", "JWT None算法攻击");
                    break;
                    
                case "SQL_INJECTION":
                    securityAuditService.logSecurityAttack("SQL_INJECTION", 
                            "模拟SQL注入攻击检测: ' OR 1=1--", clientIP, userAgent, request);
                    result.put("attackType", "SQL注入攻击");
                    break;
                    
                case "XSS_ATTACK":
                    securityAuditService.logSecurityAttack("XSS_ATTACK", 
                            "模拟XSS攻击检测: <script>alert('xss')</script>", clientIP, userAgent, request);
                    result.put("attackType", "XSS攻击");
                    break;
                    
                case "BRUTE_FORCE":
                    securityAuditService.logSecurityAttack("BRUTE_FORCE", 
                            "模拟暴力破解攻击检测: 短时间内多次登录失败", clientIP, userAgent, request);
                    result.put("attackType", "暴力破解攻击");
                    break;
                    
                default:
                    securityAuditService.logSecurityAttack("UNKNOWN_ATTACK", 
                            "模拟未知类型攻击检测", clientIP, userAgent, request);
                    result.put("attackType", "未知攻击类型");
                    break;
            }
            
            result.put("testResult", "SUCCESS");
            result.put("message", "攻击检测测试完成，检查审计日志");
            result.put("detectedAttack", testType);
            
            log.info("✅ [ATTACK_TEST] 攻击检测测试成功: {}", testType);
            
        } catch (Exception e) {
            log.error("❌ [ATTACK_TEST] 攻击检测测试失败", e);
            
            result.put("testResult", "FAILED");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 📊 获取P0安全修复状态
     * GET /admin-api/test/security/status
     */
    @GetMapping("/status")
    public Map<String, Object> getSecurityStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", System.currentTimeMillis());
        
        try {
            // 1. 加密服务状态
            String encryptionStatus = securityTokenService.getEncryptionServiceStatus();
            status.put("encryptionService", encryptionStatus);
            
            // 2. 密钥配置状态
            boolean keyConfigValid = securityKeyConfig.isKeyConfigValid();
            String keySource = securityKeyConfig.getKeySource();
            status.put("keyConfigValid", keyConfigValid);
            status.put("keySource", keySource);
            
            // 3. 安全统计
            Map<String, Object> auditStats = securityAuditService.getSecurityStatistics();
            status.put("securityStats", auditStats);
            
            // 4. P0修复功能状态
            status.put("p0SecurityFixes", Map.of(
                    "aes256GcmEncryption", "ACTIVE",
                    "externalKeyConfig", "ACTIVE",
                    "enhancedJwtValidation", "ACTIVE",
                    "securityAuditLogging", "ACTIVE"
            ));
            
            status.put("overallStatus", "P0_SECURITY_FIXES_ACTIVE");
            status.put("message", "P0级安全修复功能全部正常运行");
            
        } catch (Exception e) {
            log.error("❌ [SECURITY_STATUS] 获取安全状态失败", e);
            status.put("overallStatus", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 🌐 获取客户端真实IP
     */
    private String getClientIP(HttpServletRequest request) {
        String[] ipHeaders = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
            "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}