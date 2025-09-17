package cn.iocoder.yudao.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🔐 校园认证上下文持有者
 *
 * 核心职责：
 * 1. 使用ThreadLocal存储当前请求的用户信息
 * 2. 提供线程安全的用户信息存取
 * 3. 确保请求结束后清理ThreadLocal防止内存泄漏
 * 4. 避免重复调用Mock API提升性能
 *
 * 🚨 安全关键点：
 * - ThreadLocal必须在请求结束后清理（afterCompletion）
 * - 异常情况也必须清理防止用户信息串扰
 * - 不能跨线程访问，每个请求独立隔离
 *
 * @author Security Team
 * @since 2025-01-05
 */
public class CampusAuthContextHolder {

    private static final Logger log = LoggerFactory.getLogger(CampusAuthContextHolder.class);

    /**
     * ThreadLocal存储当前线程的用户信息
     * 使用普通ThreadLocal避免线程池环境下的上下文意外传播
     */
    private static final ThreadLocal<UserInfo> USER_CONTEXT = new ThreadLocal<>();

    /**
     * ThreadLocal存储当前线程的JWT Token
     * 使用普通ThreadLocal确保每个请求独立隔离
     */
    private static final ThreadLocal<String> TOKEN_CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前用户信息
     *
     * @param userInfo 用户信息对象
     */
    public static void setCurrentUser(UserInfo userInfo) {
        if (userInfo == null) {
            log.warn("⚠️ [AUTH_CONTEXT] 尝试设置null用户信息");
            return;
        }

        USER_CONTEXT.set(userInfo);
        log.debug("🔐 [AUTH_CONTEXT] 设置用户信息: {} ({})",
                userInfo.getUsername(), userInfo.getRoleCode());
    }

    /**
     * 获取当前用户信息
     *
     * @return 当前线程的用户信息，如果未设置返回null
     */
    public static UserInfo getCurrentUser() {
        UserInfo userInfo = USER_CONTEXT.get();
        if (userInfo == null) {
            log.debug("📝 [AUTH_CONTEXT] 当前线程无用户信息");
        }
        return userInfo;
    }

    /**
     * 设置当前JWT Token
     *
     * @param token JWT Token字符串
     */
    public static void setCurrentToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("⚠️ [AUTH_CONTEXT] 尝试设置空Token");
            return;
        }

        TOKEN_CONTEXT.set(token);
        log.debug("🔐 [AUTH_CONTEXT] 设置JWT Token: {}...",
                token.length() > 20 ? token.substring(0, 20) : token);
    }

    /**
     * 获取当前JWT Token
     *
     * @return 当前线程的JWT Token，如果未设置返回null
     */
    public static String getCurrentToken() {
        return TOKEN_CONTEXT.get();
    }

    /**
     * 🚨 清理当前线程的所有认证信息
     *
     * 重要：必须在请求结束后调用，防止内存泄漏和用户信息串扰
     * 调用时机：
     * 1. HandlerInterceptor.afterCompletion
     * 2. Filter.doFilter的finally块
     * 3. 异常处理的finally块
     */
    public static void clear() {
        UserInfo userInfo = USER_CONTEXT.get();
        if (userInfo != null) {
            log.debug("🧹 [AUTH_CONTEXT] 清理用户信息: {} ({})",
                    userInfo.getUsername(), userInfo.getRoleCode());
        }

        USER_CONTEXT.remove();
        TOKEN_CONTEXT.remove();

        log.debug("✅ [AUTH_CONTEXT] ThreadLocal已清理");
    }

    /**
     * 检查是否已认证
     *
     * @return true如果当前线程有用户信息
     */
    public static boolean isAuthenticated() {
        return USER_CONTEXT.get() != null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未认证返回null
     */
    public static String getCurrentUsername() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo != null ? userInfo.getUsername() : null;
    }

    /**
     * 获取当前用户角色
     *
     * @return 角色代码，未认证返回null
     */
    public static String getCurrentRoleCode() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo != null ? userInfo.getRoleCode() : null;
    }

    /**
     * 用户信息内部类
     * 与AccessControlListManager.UserInfo保持一致
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
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getGradeId() {
            return gradeId;
        }

        public void setGradeId(String gradeId) {
            this.gradeId = gradeId;
        }

        public String getClassId() {
            return classId;
        }

        public void setClassId(String classId) {
            this.classId = classId;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }

        /**
         * 从AccessControlListManager.UserInfo转换
         */
        public static UserInfo fromAclUserInfo(AccessControlListManager.UserInfo aclUserInfo) {
            if (aclUserInfo == null) {
                return null;
            }

            UserInfo userInfo = new UserInfo();
            userInfo.setUsername(aclUserInfo.getUsername());
            userInfo.setRoleCode(aclUserInfo.getRoleCode());
            userInfo.setRoleName(aclUserInfo.getRoleName());
            userInfo.setEmployeeId(aclUserInfo.getEmployeeId());
            userInfo.setStudentId(aclUserInfo.getStudentId());
            userInfo.setGradeId(aclUserInfo.getGradeId());
            userInfo.setClassId(aclUserInfo.getClassId());
            userInfo.setDepartmentId(aclUserInfo.getDepartmentId());

            return userInfo;
        }

        /**
         * 转换为AccessControlListManager.UserInfo
         */
        public AccessControlListManager.UserInfo toAclUserInfo() {
            AccessControlListManager.UserInfo aclUserInfo = new AccessControlListManager.UserInfo();
            aclUserInfo.setUsername(this.username);
            aclUserInfo.setRoleCode(this.roleCode);
            aclUserInfo.setRoleName(this.roleName);
            aclUserInfo.setEmployeeId(this.employeeId);
            aclUserInfo.setStudentId(this.studentId);
            aclUserInfo.setGradeId(this.gradeId);
            aclUserInfo.setClassId(this.classId);
            aclUserInfo.setDepartmentId(this.departmentId);

            return aclUserInfo;
        }

        @Override
        public String toString() {
            return String.format("UserInfo{username='%s', roleCode='%s', studentId='%s', gradeId='%s', classId='%s'}",
                    username, roleCode, studentId, gradeId, classId);
        }
    }

    /**
     * 🔍 调试方法：打印当前线程的认证状态
     */
    public static void debugPrintContext() {
        UserInfo userInfo = USER_CONTEXT.get();
        String token = TOKEN_CONTEXT.get();

        log.info("🔍 [AUTH_CONTEXT_DEBUG] ========== 认证上下文状态 ==========");
        log.info("🔍 [AUTH_CONTEXT_DEBUG] 线程ID: {}", Thread.currentThread().getId());
        log.info("🔍 [AUTH_CONTEXT_DEBUG] 线程名: {}", Thread.currentThread().getName());

        if (userInfo != null) {
            log.info("🔍 [AUTH_CONTEXT_DEBUG] 用户信息: {}", userInfo);
        } else {
            log.info("🔍 [AUTH_CONTEXT_DEBUG] 用户信息: 未设置");
        }

        if (token != null) {
            log.info("🔍 [AUTH_CONTEXT_DEBUG] JWT Token: {}...",
                    token.length() > 30 ? token.substring(0, 30) : token);
        } else {
            log.info("🔍 [AUTH_CONTEXT_DEBUG] JWT Token: 未设置");
        }

        log.info("🔍 [AUTH_CONTEXT_DEBUG] =====================================");
    }
}