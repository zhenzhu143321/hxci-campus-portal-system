package cn.iocoder.yudao.module.infra.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock School API集成服务 - Infra模块版本
 * 负责与Mock School API进行通信，验证用户认证和权限
 * 🔄 从yudao-server模块迁移到yudao-module-infra，供NotificationController使用
 * 
 * @author Claude
 */
@Service
public class MockSchoolApiIntegration {

    private static final Logger log = LoggerFactory.getLogger(MockSchoolApiIntegration.class);

    @Value("${mock.school-api.base-url:http://localhost:48082}")
    private String mockApiBaseUrl;

    @Value("${mock.school-api.enabled:true}")
    private boolean mockApiEnabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MockSchoolApiIntegration(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 用户认证接口（用户名密码登录）
     */
    public UserInfo authenticateUser(String username, String password) {
        if (!mockApiEnabled) {
            log.warn("Mock School API已禁用，使用默认用户认证");
            return createDefaultUserInfo(username);
        }

        log.info("调用Mock School API认证用户: {}", username);
        
        try {
            String url = mockApiBaseUrl + "/mock-school-api/auth/authenticate";
            
            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("password", password);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
                
            if (response.getStatusCode().is2xxSuccessful()) {
                MockApiResponse<UserInfo> apiResponse = objectMapper.readValue(
                    response.getBody(), 
                    new TypeReference<MockApiResponse<UserInfo>>() {}
                );
                
                if (apiResponse.isSuccess()) {
                    log.info("用户认证成功: 用户={}, 角色={}", username, apiResponse.getData().getRoleName());
                    return apiResponse.getData();
                } else {
                    log.warn("用户认证失败: {}", apiResponse.getMessage());
                    return null;
                }
            } else {
                log.error("Mock School API调用失败，状态码: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("调用Mock School API认证异常", e);
            return null;
        }
    }

    /**
     * 验证用户token并获取权限信息
     */
    public UserInfo verifyUserToken(String token) {
        if (!mockApiEnabled) {
            log.warn("Mock School API已禁用");
            return null;
        }

        // 处理Bearer token前缀
        String actualToken = token;
        if (token != null && token.startsWith("Bearer ")) {
            actualToken = token.substring(7); // 移除"Bearer "前缀
            log.info("提取Bearer token: {} -> {}", token, actualToken);
        }

        log.info("调用Mock School API验证token: {}", actualToken);
        
        try {
            String url = mockApiBaseUrl + "/mock-school-api/auth/verify";
            
            Map<String, String> request = new HashMap<>();
            request.put("token", actualToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
                
            if (response.getStatusCode().is2xxSuccessful()) {
                MockApiResponse<UserInfo> apiResponse = objectMapper.readValue(
                    response.getBody(), 
                    new TypeReference<MockApiResponse<UserInfo>>() {}
                );
                
                if (apiResponse.isSuccess()) {
                    log.info("Token验证成功: 用户={}", apiResponse.getData().getUsername());
                    return apiResponse.getData();
                } else {
                    log.warn("Token验证失败: {}", apiResponse.getMessage());
                    return null;
                }
            } else {
                log.error("Mock School API调用失败，状态码: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("调用Mock School API异常", e);
            return null;
        }
    }

    /**
     * 🆕 获取用户信息（从Mock API - 新接口）
     * 替代原来的权限验证接口，现在只获取用户基本信息
     */
    public UserInfo getUserInfoFromMockApi(String token) {
        if (!mockApiEnabled) {
            log.warn("Mock School API已禁用");
            return null;
        }

        // 处理Bearer token前缀
        String actualToken = token;
        if (token != null && token.startsWith("Bearer ")) {
            actualToken = token.substring(7); // 移除"Bearer "前缀
            log.info("提取Bearer token: {} -> {}", token, actualToken);
        }

        log.info("🆕 调用Mock API获取用户信息: {}", actualToken);
        
        try {
            String url = mockApiBaseUrl + "/mock-school-api/auth/user-info";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + actualToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
                
            if (response.getStatusCode().is2xxSuccessful()) {
                MockApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                    response.getBody(), 
                    new TypeReference<MockApiResponse<Map<String, Object>>>() {}
                );
                
                if (apiResponse.isSuccess()) {
                    Map<String, Object> userData = apiResponse.getData();
                    
                    // 转换为UserInfo对象
                    UserInfo userInfo = new UserInfo();
                    userInfo.setUserId((String) userData.get("employeeId")); // 使用employeeId作为userId
                    userInfo.setUsername((String) userData.get("username"));
                    userInfo.setRoleCode((String) userData.get("roleCode"));
                    userInfo.setRoleName((String) userData.get("roleName"));
                    userInfo.setDepartmentName((String) userData.get("departmentName"));
                    userInfo.setEnabled(true);
                    
                    // 🆕 设置employeeId字段（关键字段）
                    userInfo.setEmployeeId((String) userData.get("employeeId"));
                    userInfo.setUserType((String) userData.get("userType"));
                    
                    if (userData.get("departmentId") != null) {
                        userInfo.setDepartmentId(((Number) userData.get("departmentId")).longValue());
                    }
                    
                    log.info("✅ 用户信息获取成功: employeeId={}, role={}", userInfo.getEmployeeId(), userInfo.getRoleCode());
                    return userInfo;
                } else {
                    log.warn("获取用户信息失败: {}", apiResponse.getMessage());
                    return null;
                }
            } else {
                log.error("Mock API调用失败，状态码: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("调用Mock API获取用户信息异常", e);
            return null;
        }
    }

    /**
     * 🆕 主服务内置权限验证逻辑
     * 基于用户角色进行权限验证，不再依赖Mock API
     */
    public PermissionResult verifyNotificationPermissionLocally(String token, int notificationLevel, String targetScope) {
        log.info("🔐 主服务权限验证: 级别={}, 范围={}", notificationLevel, targetScope);
        
        // 1. 从Mock API获取用户信息
        UserInfo userInfo = getUserInfoFromMockApi(token);
        if (userInfo == null) {
            log.warn("❌ 无法获取用户信息，Token可能无效");
            return createDeniedPermissionResult("用户认证失败，Token无效");
        }
        
        // 2. 基于角色和通知级别进行权限验证
        return verifyPermissionByRole(userInfo, notificationLevel, targetScope);
    }
    
    /**
     * 🆕 基于角色的权限验证逻辑
     */
    private PermissionResult verifyPermissionByRole(UserInfo userInfo, int notificationLevel, String targetScope) {
        String roleCode = userInfo.getRoleCode();
        String username = userInfo.getUsername();
        
        log.info("🔍 权限验证: 用户={}, 角色={}, 通知级别={}, 目标范围={}", 
                username, roleCode, notificationLevel, targetScope);
        
        // 权限验证矩阵
        switch (roleCode) {
            case "PRINCIPAL": // 校长 - 最高权限
                log.info("✅ 校长权限: 允许发布所有级别通知 (1-4级)");
                return createGrantedPermissionResult(
                    String.format("校长 %s 有权限发布 %d 级通知", username, notificationLevel),
                    false, null);
                
            case "ACADEMIC_ADMIN": // 教务主任
                if (notificationLevel == 1) { // 紧急通知需要校长审批
                    log.info("📝 教务主任发布紧急通知: 需要校长审批");
                    return createApprovalRequiredPermissionResult(
                        String.format("教务主任 %s 发布紧急通知需要校长审批", username),
                        "PRINCIPAL");
                } else if (notificationLevel <= 4) { // 2-4级可直接发布
                    log.info("✅ 教务主任权限: 允许发布 {} 级通知", notificationLevel);
                    return createGrantedPermissionResult(
                        String.format("教务主任 %s 有权限发布 %d 级通知", username, notificationLevel),
                        false, null);
                }
                break;
                
            case "TEACHER": // 任课教师
            case "CLASS_TEACHER": // 班主任
                if (notificationLevel >= 3 && notificationLevel <= 4) { // 只能发布3-4级通知
                    log.info("✅ 教师权限: 允许发布 {} 级通知", notificationLevel);
                    return createGrantedPermissionResult(
                        String.format("教师 %s 有权限发布 %d 级通知", username, notificationLevel),
                        false, null);
                }
                break;
                
            case "STUDENT": // 学生
                if (notificationLevel == 4) { // 只能发布提醒通知(4级)
                    log.info("✅ 学生权限: 允许发布提醒通知");
                    return createGrantedPermissionResult(
                        String.format("学生 %s 有权限发布提醒通知", username),
                        false, null);
                }
                break;
                
            default:
                log.warn("⚠️ 未知角色: {}", roleCode);
                break;
        }
        
        // 默认拒绝
        log.warn("❌ 权限验证失败: 用户 {} (角色: {}) 无权限发布 {} 级通知", username, roleCode, notificationLevel);
        return createDeniedPermissionResult(
            String.format("用户 %s (角色: %s) 没有发布 %d 级通知的权限", username, roleCode, notificationLevel));
    }
    
    /**
     * 创建授权通过的权限结果
     */
    private PermissionResult createGrantedPermissionResult(String message, Boolean requiresApproval, String approverRole) {
        PermissionResult result = new PermissionResult();
        result.setPermissionGranted(true);
        result.setMessage(message);
        result.setApprovalRequired(requiresApproval);
        result.setApproverRole(approverRole);
        return result;
    }
    
    /**
     * 创建需要审批的权限结果
     */
    private PermissionResult createApprovalRequiredPermissionResult(String message, String approverRole) {
        PermissionResult result = new PermissionResult();
        result.setPermissionGranted(true);
        result.setMessage(message);
        result.setApprovalRequired(true);
        result.setApproverRole(approverRole);
        return result;
    }

    /**
     * 验证通知发布权限（向后兼容方法）
     * 现在内部调用新的本地权限验证逻辑
     */
    public PermissionResult verifyNotificationPermission(String token, int notificationLevel, String targetScope) {
        log.info("🔄 [兼容接口] 调用权限验证，转发到本地验证逻辑");
        return verifyNotificationPermissionLocally(token, notificationLevel, targetScope);
    }
    
    /**
     * 获取用户权限列表（向后兼容方法）
     * 现在基于角色返回权限列表
     */
    public List<String> getUserPermissions(String userId) {
        log.info("🔄 [兼容接口] 获取用户权限列表: {}", userId);
        
        if (!mockApiEnabled) {
            return List.of("VIEW_NOTIFY"); // 默认权限
        }

        // 🚫 不再调用已删除的Mock API接口，基于默认逻辑返回权限
        // 这里可以根据userId推断角色，但为了简化，返回基础权限
        return List.of("VIEW_NOTIFY", "PUBLISH_REMINDER"); // 基础权限
    }
    
    /**
     * 检查Mock School API服务是否可用
     */
    public boolean isServiceAvailable() {
        if (!mockApiEnabled) {
            return false;
        }

        try {
            String url = mockApiBaseUrl + "/mock-school-api/auth/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.warn("Mock School API服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建默认权限结果（当Mock API不可用时）
     */
    private PermissionResult createDefaultPermissionResult() {
        PermissionResult result = new PermissionResult();
        result.setPermissionGranted(true);
        result.setPermissionLevel("DEFAULT");
        result.setApprovalRequired(false);
        result.setMessage("Mock API不可用，使用默认权限");
        return result;
    }

    /**
     * 创建默认用户信息（当Mock API不可用时）
     */
    private UserInfo createDefaultUserInfo(String username) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("default_user_001");
        userInfo.setUsername(username);
        userInfo.setRoleCode("ADMIN");
        userInfo.setRoleName("系统管理员");
        userInfo.setDepartmentId(1L);
        userInfo.setDepartmentName("系统管理部门");
        userInfo.setEnabled(true);
        userInfo.setPermissions(List.of("ADMIN_ALL"));
        return userInfo;
    }

    /**
     * 创建拒绝权限结果
     */
    private PermissionResult createDeniedPermissionResult(String message) {
        PermissionResult result = new PermissionResult();
        result.setPermissionGranted(false);
        result.setMessage(message);
        return result;
    }

    // 内部DTO类
    public static class UserInfo {
        private String userId;
        private String username;
        private String employeeId;  // 🆕 学号/工号字段
        private String userType;    // 🆕 用户类型字段
        private String roleCode;
        private String roleName;
        private Long departmentId;
        private String departmentName;
        private List<String> permissions;
        private Boolean enabled;

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
        
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static class PermissionResult {
        private Boolean permissionGranted;
        private String permissionLevel;
        private Boolean approvalRequired;
        private String approverRole;
        private String message;
        private String allowedScope;

        // Getters and Setters
        public Boolean getPermissionGranted() { return permissionGranted; }
        public void setPermissionGranted(Boolean permissionGranted) { this.permissionGranted = permissionGranted; }
        
        public String getPermissionLevel() { return permissionLevel; }
        public void setPermissionLevel(String permissionLevel) { this.permissionLevel = permissionLevel; }
        
        public Boolean getApprovalRequired() { return approvalRequired; }
        public void setApprovalRequired(Boolean approvalRequired) { this.approvalRequired = approvalRequired; }
        
        public String getApproverRole() { return approverRole; }
        public void setApproverRole(String approverRole) { this.approverRole = approverRole; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getAllowedScope() { return allowedScope; }
        public void setAllowedScope(String allowedScope) { this.allowedScope = allowedScope; }
    }

    public static class MockApiResponse<T> {
        private Integer code;
        private String message;
        private T data;
        private LocalDateTime timestamp;

        public boolean isSuccess() {
            return code != null && code == 200;
        }

        // Getters and Setters
        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}