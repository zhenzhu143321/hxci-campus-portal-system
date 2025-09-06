package cn.iocoder.yudao.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * 敏感信息保护服务 - P1.2安全修复：信息泄露防护
 * 
 * 职责：
 * 1. 将敏感用户信息从JWT中移除
 * 2. 通过安全API获取完整用户信息
 * 3. 实施信息访问控制和审计
 * 4. 缓存优化（减少API调用）
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Service
public class SensitiveInfoProtectionService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveInfoProtectionService.class);
    
    private static final String MOCK_API_BASE = "http://localhost:48082";
    
    @Autowired
    private RestTemplate restTemplate;
    
    // 🗄️ 用户信息缓存（5分钟过期）
    private final Map<String, CachedUserInfo> userInfoCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5分钟

    /**
     * 🔒 安全获取完整用户信息
     * 将JWT中的最小载荷扩展为完整用户信息，避免敏感信息在Token中传输
     * 
     * @param minimalUserInfo 从JWT中解析的最小用户信息
     * @param authToken JWT Token（用于API调用认证）
     * @return 完整的用户信息
     */
    public UserInfo getCompleteUserInfo(MinimalUserInfo minimalUserInfo, String authToken) {
        if (minimalUserInfo == null) {
            log.warn("❌ [SENSITIVE_INFO] 最小用户信息为空");
            return null;
        }
        
        String userId = minimalUserInfo.getUserId();
        log.info("🔒 [SENSITIVE_INFO] 安全获取完整用户信息: userId={}, employeeId={}", 
                userId, minimalUserInfo.getEmployeeId());
        
        try {
            // 1️⃣ 检查缓存
            CachedUserInfo cached = userInfoCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                log.info("📋 [SENSITIVE_INFO] 缓存命中: userId={}", userId);
                return cached.userInfo;
            }
            
            // 2️⃣ 通过Mock API安全获取用户信息
            UserInfo completeUserInfo = fetchUserInfoFromMockApi(authToken, minimalUserInfo);
            
            if (completeUserInfo == null) {
                log.error("❌ [SENSITIVE_INFO] 无法获取完整用户信息: userId={}", userId);
                return null;
            }
            
            // 3️⃣ 更新缓存
            userInfoCache.put(userId, new CachedUserInfo(completeUserInfo));
            
            // 4️⃣ 记录敏感信息访问审计
            auditSensitiveInfoAccess(userId, completeUserInfo.getEmployeeId(), "SUCCESS");
            
            log.info("✅ [SENSITIVE_INFO] 完整用户信息获取成功: userId={}, realName={}", 
                    userId, completeUserInfo.getRealName());
            
            return completeUserInfo;
            
        } catch (Exception e) {
            log.error("❌ [SENSITIVE_INFO] 获取完整用户信息异常: userId={}", userId, e);
            auditSensitiveInfoAccess(userId, minimalUserInfo.getEmployeeId(), "ERROR: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 🔍 从Mock API获取用户信息
     */
    private UserInfo fetchUserInfoFromMockApi(String authToken, MinimalUserInfo minimalUserInfo) {
        String url = MOCK_API_BASE + "/mock-school-api/auth/user-info";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            
            // 检查响应码
            Object codeObj = body.get("code");
            boolean isSuccess = (codeObj instanceof Integer && (Integer) codeObj == 200) || 
                              (codeObj instanceof String && "200".equals(codeObj));
            
            if (isSuccess && body.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) body.get("data");
                return mapToUserInfo(userData, minimalUserInfo);
            } else {
                log.warn("⚠️ [SENSITIVE_INFO] Mock API返回错误: {}", body.get("msg"));
                return null;
            }
        } else {
            log.error("❌ [SENSITIVE_INFO] Mock API调用失败: {}", response.getStatusCode());
            return null;
        }
    }
    
    /**
     * 🎯 将API响应映射为UserInfo对象
     */
    private UserInfo mapToUserInfo(Map<String, Object> userData, MinimalUserInfo minimalUserInfo) {
        UserInfo userInfo = new UserInfo();
        
        // 基础信息（来自JWT最小载荷）
        userInfo.setUserId(minimalUserInfo.getUserId());
        userInfo.setEmployeeId(minimalUserInfo.getEmployeeId());
        userInfo.setRoleCode(minimalUserInfo.getRoleCode());
        userInfo.setUserType(minimalUserInfo.getUserType());
        
        // 🔒 敏感信息（通过安全API获取）
        userInfo.setUsername(getStringValue(userData, "username"));
        userInfo.setRealName(getStringValue(userData, "realName"));
        userInfo.setRoleName(getStringValue(userData, "roleName"));
        userInfo.setDepartmentName(getStringValue(userData, "departmentName"));
        userInfo.setGradeId(getStringValue(userData, "gradeId"));
        userInfo.setClassId(getStringValue(userData, "classId"));
        
        // 部门ID处理
        Object deptIdObj = userData.get("departmentId");
        if (deptIdObj instanceof Number) {
            userInfo.setDepartmentId(((Number) deptIdObj).longValue());
            userInfo.setDepartmentIdStr(deptIdObj.toString());
        }
        
        // 学生特殊字段
        if ("STUDENT".equals(minimalUserInfo.getRoleCode())) {
            userInfo.setStudentId(minimalUserInfo.getEmployeeId());
        }
        
        userInfo.setEnabled(true);
        
        return userInfo;
    }
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 🚨 敏感信息访问审计
     */
    private void auditSensitiveInfoAccess(String userId, String employeeId, String result) {
        log.info("📋 [AUDIT] 敏感信息访问: userId={}, employeeId={}, result={}, timestamp={}", 
                userId, employeeId, result, LocalDateTime.now());
        
        // TODO: 将审计记录写入专用的审计日志或数据库
        // 生产环境应该实现完整的审计追踪
    }
    
    /**
     * 🧹 清理过期缓存
     */
    public void cleanExpiredCache() {
        int initialSize = userInfoCache.size();
        
        userInfoCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        int cleanedCount = initialSize - userInfoCache.size();
        if (cleanedCount > 0) {
            log.info("🧹 [CACHE] 清理过期用户信息缓存: 清理{}个条目，当前大小: {}", 
                    cleanedCount, userInfoCache.size());
        }
    }
    
    /**
     * 📊 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        cleanExpiredCache();
        return new CacheStats(userInfoCache.size(), CACHE_EXPIRY_MS / 1000);
    }
    
    /**
     * 🚨 清空特定用户的缓存（用户信息更新时）
     */
    public void invalidateUserCache(String userId) {
        if (userId != null) {
            userInfoCache.remove(userId);
            log.info("🗑️ [CACHE] 清空用户缓存: userId={}", userId);
        }
    }
    
    /**
     * 最小用户信息（从JWT解析）
     */
    public static class MinimalUserInfo {
        private final String userId;
        private final String employeeId;
        private final String roleCode;
        private final String userType;
        
        public MinimalUserInfo(String userId, String employeeId, String roleCode, String userType) {
            this.userId = userId;
            this.employeeId = employeeId;
            this.roleCode = roleCode;
            this.userType = userType;
        }
        
        public String getUserId() { return userId; }
        public String getEmployeeId() { return employeeId; }
        public String getRoleCode() { return roleCode; }
        public String getUserType() { return userType; }
    }
    
    /**
     * 完整用户信息（包含敏感信息）
     */
    public static class UserInfo {
        private String userId;
        private String username;
        private String employeeId;
        private String studentId;
        private String realName;
        private String roleCode;
        private String roleName;
        private Long departmentId;
        private String departmentName;
        private String departmentIdStr;
        private String gradeId;
        private String classId;
        private String userType;
        private Boolean enabled;
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        
        public String getDepartmentIdStr() { return departmentIdStr; }
        public void setDepartmentIdStr(String departmentIdStr) { this.departmentIdStr = departmentIdStr; }
        
        public String getGradeId() { return gradeId; }
        public void setGradeId(String gradeId) { this.gradeId = gradeId; }
        
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
    
    /**
     * 缓存用户信息
     */
    private static class CachedUserInfo {
        private final UserInfo userInfo;
        private final long timestamp;
        
        public CachedUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final int cachedUsers;
        public final long cacheExpirySeconds;
        
        public CacheStats(int cachedUsers, long cacheExpirySeconds) {
            this.cachedUsers = cachedUsers;
            this.cacheExpirySeconds = cacheExpirySeconds;
        }
        
        @Override
        public String toString() {
            return String.format("用户信息缓存统计 - 缓存用户: %d, 过期时间: %d秒", cachedUsers, cacheExpirySeconds);
        }
    }
}