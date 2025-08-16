package cn.iocoder.yudao.module.infra.service.auth;

/**
 * JWT认证服务接口
 * 支持两步认证架构：解析JWT token并从Mock API获取权限信息
 * 
 * @author Claude
 */
public interface JwtAuthService {

    /**
     * 🎯 解析JWT Token获取用户信息（核心方法）
     * 从Mock API生成的JWT token中提取学号/工号等信息
     */
    UserAuthInfo parseJwtToken(String jwtToken);

    /**
     * 验证用户是否有发布通知的权限
     * 
     * @param jwtToken JWT令牌
     * @param notificationLevel 通知级别（1-4）
     * @param targetScope 目标范围
     * @return 权限验证结果
     */
    AuthResult verifyNotificationPermission(String jwtToken, Integer notificationLevel, String targetScope);

    /**
     * 通过Mock API验证用户权限
     * 调用Mock School API获取详细权限信息
     */
    AuthResult verifyPermissionViaApi(String token, Integer notificationLevel, String targetScope);

    /**
     * 用户认证信息类
     */
    class UserAuthInfo {
        private String employeeId;
        private String username;
        private String realName;
        private String roleCode;
        private String roleName;
        private String userType;
        private Long departmentId;
        private String departmentName;
        private boolean valid;

        // Getters and Setters
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
    }

    /**
     * 权限验证结果类
     */
    class AuthResult {
        private boolean permissionGranted;
        private String employeeId;
        private String username;
        private String roleCode;
        private String roleName;
        private String message;
        private boolean requiresApproval;
        private String approverRole;

        // Getters and Setters
        public boolean isPermissionGranted() { return permissionGranted; }
        public void setPermissionGranted(boolean permissionGranted) { this.permissionGranted = permissionGranted; }
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isRequiresApproval() { return requiresApproval; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
        public String getApproverRole() { return approverRole; }
        public void setApproverRole(String approverRole) { this.approverRole = approverRole; }
    }
}