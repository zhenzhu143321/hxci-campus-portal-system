package cn.iocoder.yudao.server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 访问控制列表(ACL)管理器
 * 防止访问控制缺失漏洞 - CVE-HXCI-2025-009
 * 
 * 功能:
 * 1. 基于角色的访问控制(RBAC)
 * 2. 基于属性的访问控制(ABAC) 
 * 3. 资源访问权限矩阵
 * 4. 动态权限计算与缓存
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Slf4j
@Component
public class AccessControlListManager {

    /**
     * UserInfo DTO - 用于安全验证
     */
    public static class UserInfo {
        private String username;
        private String roleCode;
        private String roleName;
        private String employeeId;
        private String studentId;
        private String gradeId;
        private String classId;
        private Long departmentId;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        
        public String getGradeId() { return gradeId; }
        public void setGradeId(String gradeId) { this.gradeId = gradeId; }
        
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        
        public String getUserId() { return employeeId; }
    }

    // 权限缓存 - 提高性能
    private final Map<String, Set<String>> rolePermissionsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 300_000; // 5分钟缓存

    /**
     * 角色权限矩阵定义
     */
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
        "SYSTEM_ADMIN", Set.of(
            // 系统管理员 - 最高权限
            "NOTIFICATION_CREATE_ALL", "NOTIFICATION_READ_ALL", "NOTIFICATION_UPDATE_ALL", "NOTIFICATION_DELETE_ALL",
            "TODO_CREATE_ALL", "TODO_READ_ALL", "TODO_UPDATE_ALL", "TODO_DELETE_ALL",
            "USER_CREATE", "USER_READ_ALL", "USER_UPDATE_ALL", "USER_DELETE",
            "PERMISSION_MANAGE", "CACHE_MANAGE", "SYSTEM_CONFIG", "AUDIT_LOG_READ"
        ),
        "PRINCIPAL", Set.of(
            // 校长 - 全校管理权限
            "NOTIFICATION_CREATE_SCHOOL", "NOTIFICATION_READ_ALL", "NOTIFICATION_UPDATE_SCHOOL", "NOTIFICATION_DELETE_SCHOOL",
            "TODO_CREATE_SCHOOL", "TODO_READ_ALL", "TODO_UPDATE_SCHOOL", "TODO_DELETE_SCHOOL",
            "USER_READ_ALL", "REPORT_READ_ALL", "AUDIT_LOG_READ"
        ),
        "ACADEMIC_ADMIN", Set.of(
            // 教务主任 - 教务管理权限
            "NOTIFICATION_CREATE_ACADEMIC", "NOTIFICATION_READ_ACADEMIC", "NOTIFICATION_UPDATE_ACADEMIC", "NOTIFICATION_DELETE_ACADEMIC",
            "TODO_CREATE_ACADEMIC", "TODO_READ_ACADEMIC", "TODO_UPDATE_ACADEMIC", "TODO_DELETE_ACADEMIC",
            "USER_READ_ACADEMIC", "STUDENT_MANAGE", "COURSE_MANAGE"
        ),
        "TEACHER", Set.of(
            // 教师 - 教学权限
            "NOTIFICATION_CREATE_DEPT", "NOTIFICATION_READ_DEPT", "NOTIFICATION_UPDATE_DEPT", "NOTIFICATION_DELETE_DEPT",
            "TODO_CREATE_CLASS", "TODO_READ_CLASS", "TODO_UPDATE_CLASS", "TODO_DELETE_CLASS",
            "STUDENT_READ_CLASS", "GRADE_MANAGE"
        ),
        "CLASS_TEACHER", Set.of(
            // 班主任 - 班级管理权限
            "NOTIFICATION_CREATE_CLASS", "NOTIFICATION_READ_CLASS", "NOTIFICATION_UPDATE_CLASS", "NOTIFICATION_DELETE_CLASS",
            "TODO_CREATE_CLASS", "TODO_READ_CLASS", "TODO_UPDATE_CLASS", "TODO_DELETE_CLASS",
            "STUDENT_READ_CLASS", "STUDENT_UPDATE_CLASS"
        ),
        "STUDENT", Set.of(
            // 学生 - 基础权限
            "NOTIFICATION_CREATE_PERSONAL", "NOTIFICATION_READ_PERSONAL", "TODO_READ_PERSONAL", "TODO_UPDATE_PERSONAL", "PROFILE_UPDATE_SELF"
        )
    );

    /**
     * 资源访问级别定义
     */
    public enum AccessLevel {
        ALL,           // 全部访问
        SCHOOL,        // 全校访问
        ACADEMIC,      // 教务范围
        DEPARTMENT,    // 部门范围
        CLASS,         // 班级范围
        PERSONAL       // 个人范围
    }

    /**
     * 检查用户是否具有指定权限
     * @param user 用户信息
     * @param permission 权限标识
     * @return 是否有权限
     */
    public boolean hasPermission(AccessControlListManager.UserInfo user, String permission) {
        if (user == null || permission == null) {
            log.warn("🚨 [ACL_CHECK] 用户或权限为空，拒绝访问: user={}, permission={}", user, permission);
            return false;
        }

        String roleCode = user.getRoleCode();
        Set<String> userPermissions = getUserPermissions(roleCode);

        boolean hasPermission = userPermissions.contains(permission);
        
        if (hasPermission) {
            log.info("✅ [ACL_CHECK] 权限验证通过: user={}, role={}, permission={}", 
                    user.getEmployeeId(), roleCode, permission);
        } else {
            log.warn("🚨 [ACL_DENIED] 权限验证失败: user={}, role={}, permission={}", 
                    user.getEmployeeId(), roleCode, permission);
        }

        return hasPermission;
    }

    /**
     * 检查用户对特定资源的操作权限
     * @param user 用户信息
     * @param operation 操作类型 (CREATE, READ, UPDATE, DELETE)
     * @param resourceType 资源类型 (NOTIFICATION, TODO, USER)
     * @param accessLevel 访问级别
     * @return 是否有权限
     */
    public boolean hasResourcePermission(AccessControlListManager.UserInfo user, String operation, String resourceType, AccessLevel accessLevel) {
        if (user == null || operation == null || resourceType == null || accessLevel == null) {
            log.warn("🚨 [ACL_RESOURCE_CHECK] 参数为空，拒绝访问");
            return false;
        }

        String permission = String.format("%s_%s_%s", resourceType, operation, accessLevel.name());
        return hasPermission(user, permission);
    }

    /**
     * 检查用户对通知的访问权限
     * @param user 用户信息
     * @param operation 操作类型
     * @param notificationLevel 通知级别 (1-4)
     * @param targetScope 目标范围
     * @return 是否有权限
     */
    public boolean hasNotificationPermission(AccessControlListManager.UserInfo user, String operation, int notificationLevel, String targetScope) {
        if (user == null) {
            return false;
        }

        String roleCode = user.getRoleCode();
        
        // 系统管理员拥有所有权限
        if ("SYSTEM_ADMIN".equals(roleCode)) {
            log.info("✅ [NOTIFICATION_ACL] 系统管理员权限通过: user={}, operation={}, level={}, scope={}", 
                    user.getEmployeeId(), operation, notificationLevel, targetScope);
            return true;
        }

        // 🔧 DEBUG-FIX: 添加详细权限检查日志，精确定位权限失败原因
        boolean levelPermission = checkNotificationLevelPermission(roleCode, operation, notificationLevel);
        boolean scopePermission = checkNotificationScopePermission(roleCode, operation, targetScope);
        
        log.info("🔍 [ACL_DEBUG] 权限检查详情: user={}, role={}, operation={}, level={}, scope={}", 
                user.getEmployeeId(), roleCode, operation, notificationLevel, targetScope);
        log.info("🔍 [ACL_DEBUG] Level权限: {}, Scope权限: {}", levelPermission, scopePermission);

        // 根据角色和通知级别进行权限检查
        boolean hasPermission = levelPermission && scopePermission;

        if (hasPermission) {
            log.info("✅ [NOTIFICATION_ACL] 通知权限验证通过: user={}, role={}, operation={}, level={}, scope={}", 
                    user.getEmployeeId(), roleCode, operation, notificationLevel, targetScope);
        } else {
            log.warn("🚨 [NOTIFICATION_ACL_DENIED] 通知权限验证失败: user={}, role={}, operation={}, level={}, scope={}, levelOK={}, scopeOK={}", 
                    user.getEmployeeId(), roleCode, operation, notificationLevel, targetScope, levelPermission, scopePermission);
        }

        return hasPermission;
    }

    /**
     * 🔧 GRADE-ARCH-FIX: 检查通知级别权限 - 修复学生权限矩阵
     * 根据CLAUDE.md权限矩阵：所有人都必须能看到紧急(L1)、重要(L2)、常规(L3)通知
     */
    private boolean checkNotificationLevelPermission(String roleCode, String operation, int level) {
        // 🚨 权限矩阵修复：所有角色都能READ所有级别通知（紧急通知必须人人可见）
        if ("READ".equals(operation)) {
            // READ权限：基于CLAUDE.md安全设计，所有角色都能看到各级别通知
            Map<String, Set<Integer>> readableLevels = Map.of(
                "PRINCIPAL", Set.of(1, 2, 3, 4),      // 校长：可查看1-4级全部
                "ACADEMIC_ADMIN", Set.of(1, 2, 3, 4), // 教务主任：可查看1-4级全部  
                "TEACHER", Set.of(1, 2, 3, 4),        // 教师：可查看1-4级全部
                "CLASS_TEACHER", Set.of(1, 2, 3, 4),  // 班主任：可查看1-4级全部
                "STUDENT", Set.of(1, 2, 3, 4)         // 🔧 学生：可查看1-4级全部（紧急通知必须可见）
            );
            
            Set<Integer> allowedLevels = readableLevels.get(roleCode);
            boolean hasReadPermission = allowedLevels != null && allowedLevels.contains(level);
            
            if (hasReadPermission) {
                log.debug("✅ [LEVEL_READ_FIX] 角色{}可以READ Level{}通知", roleCode, level);
            } else {
                log.warn("🚨 [LEVEL_READ_DENIED] 角色{}不能READ Level{}通知", roleCode, level);
            }
            
            return hasReadPermission;
        }
        
        // CREATE权限：保持原有发布权限限制
        Map<String, Integer> minCreateLevels = Map.of(
            "PRINCIPAL", 1,      // 校长可以发布1-4级通知
            "ACADEMIC_ADMIN", 2, // 教务主任可以发布2-4级通知
            "TEACHER", 3,        // 教师可以发布3-4级通知
            "CLASS_TEACHER", 3,  // 班主任可以发布3-4级通知
            "STUDENT", 4         // 学生只能发布4级通知
        );

        Integer minLevel = minCreateLevels.get(roleCode);
        if (minLevel == null) {
            return false;
        }

        // 🔧 修复逻辑：level >= minLevel 表示可以发布该级别及更低优先级的通知
        return level >= minLevel;
    }

    /**
     * 🔧 GRADE-ARCH-FIX: 检查通知范围权限 - 修复学生访问权限
     * 根据CLAUDE.md权限矩阵：学生必须能看到SCHOOL_WIDE紧急通知
     */
    private boolean checkNotificationScopePermission(String roleCode, String operation, String scope) {
        if ("READ".equals(operation)) {
            // READ权限：基于CLAUDE.md安全设计，学生必须能看到全校紧急通知
            Map<String, Set<String>> readableScopes = Map.of(
                "PRINCIPAL", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
                "ACADEMIC_ADMIN", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
                "TEACHER", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"), 
                "CLASS_TEACHER", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
                "STUDENT", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS")  // 🔧 学生：必须能看到全校通知
            );
            
            Set<String> userAllowedScopes = readableScopes.get(roleCode);
            boolean hasScopePermission = userAllowedScopes != null && userAllowedScopes.contains(scope);
            
            if (hasScopePermission) {
                log.debug("✅ [SCOPE_READ_FIX] 角色{}可以READ {}范围通知", roleCode, scope);
            } else {
                log.warn("🚨 [SCOPE_READ_DENIED] 角色{}不能READ {}范围通知", roleCode, scope);
            }
            
            return hasScopePermission;
        }
        
        // CREATE权限：保持原有发布范围限制
        Map<String, Set<String>> createScopes = Map.of(
            "PRINCIPAL", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "ACADEMIC_ADMIN", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE"),
            "TEACHER", Set.of("DEPARTMENT", "CLASS"),
            "CLASS_TEACHER", Set.of("CLASS", "GRADE"),
            "STUDENT", Set.of("CLASS")  // 学生只能发布班级范围通知
        );

        Set<String> userAllowedScopes = createScopes.get(roleCode);
        return userAllowedScopes != null && userAllowedScopes.contains(scope);
    }

    /**
     * 获取用户权限列表 (带缓存)
     */
    private Set<String> getUserPermissions(String roleCode) {
        String cacheKey = "permissions:" + roleCode;
        
        // 检查缓存
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_TTL) {
            Set<String> cachedPermissions = rolePermissionsCache.get(cacheKey);
            if (cachedPermissions != null) {
                log.debug("📋 [ACL_CACHE] 从缓存获取权限: role={}, permissions={}", roleCode, cachedPermissions.size());
                return cachedPermissions;
            }
        }

        // 获取权限并缓存
        Set<String> permissions = ROLE_PERMISSIONS.getOrDefault(roleCode, Collections.emptySet());
        rolePermissionsCache.put(cacheKey, permissions);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());

        log.info("📋 [ACL_LOAD] 加载角色权限: role={}, permissions={}", roleCode, permissions.size());
        return permissions;
    }

    /**
     * 获取用户可访问的资源范围
     * @param user 用户信息
     * @return 可访问的范围列表
     */
    public Set<String> getAccessibleScopes(AccessControlListManager.UserInfo user) {
        if (user == null) {
            return Collections.emptySet();
        }

        String roleCode = user.getRoleCode();
        Map<String, Set<String>> scopeMap = Map.of(
            "SYSTEM_ADMIN", Set.of("ALL"),
            "PRINCIPAL", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "ACADEMIC_ADMIN", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE"),
            "TEACHER", Set.of("DEPARTMENT", "CLASS"),
            "CLASS_TEACHER", Set.of("CLASS", "GRADE"),
            "STUDENT", Set.of("CLASS", "GRADE")
        );

        return scopeMap.getOrDefault(roleCode, Collections.emptySet());
    }

    /**
     * 检查用户是否可以访问指定用户的数据
     * @param currentUser 当前用户
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    public boolean canAccessUserData(UserInfo currentUser, String targetUserId) {
        if (currentUser == null || targetUserId == null) {
            return false;
        }

        // 用户可以访问自己的数据
        if (currentUser.getEmployeeId().equals(targetUserId)) {
            return true;
        }

        // 管理员可以访问其管辖范围内的用户数据
        String roleCode = currentUser.getRoleCode();
        return "SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode);
    }

    /**
     * 清除权限缓存
     */
    public void clearPermissionCache() {
        rolePermissionsCache.clear();
        cacheTimestamps.clear();
        log.info("🔄 [ACL_CACHE] 权限缓存已清空");
    }

    /**
     * 获取权限统计信息
     * @return 权限统计数据
     */
    public Map<String, Object> getPermissionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRoles", ROLE_PERMISSIONS.size());
        stats.put("cacheSize", rolePermissionsCache.size());
        stats.put("cacheHitRatio", calculateCacheHitRatio());
        
        Map<String, Integer> permissionCounts = new HashMap<>();
        ROLE_PERMISSIONS.forEach((role, permissions) -> 
            permissionCounts.put(role, permissions.size()));
        stats.put("rolePermissionCounts", permissionCounts);

        return stats;
    }

    /**
     * 计算缓存命中率
     */
    private double calculateCacheHitRatio() {
        if (cacheTimestamps.isEmpty()) {
            return 0.0;
        }
        
        long validCacheCount = cacheTimestamps.values().stream()
            .mapToLong(timestamp -> System.currentTimeMillis() - timestamp < CACHE_TTL ? 1 : 0)
            .sum();
        
        return (double) validCacheCount / cacheTimestamps.size();
    }
}