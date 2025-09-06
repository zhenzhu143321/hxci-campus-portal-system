package cn.iocoder.yudao.server.security;

import cn.iocoder.yudao.server.dto.UserPermissionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 资源所有权验证器
 * 防止水平越权漏洞 - CVE-HXCI-2025-007
 * 
 * 功能:
 * 1. 验证用户只能访问自己创建的资源
 * 2. 防止跨角色资源访问
 * 3. 确保资源边界控制
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Slf4j
@Component
public class ResourceOwnershipValidator {

    /**
     * 验证通知资源所有权
     * @param currentUser 当前用户
     * @param resourceCreatorId 资源创建者ID
     * @param resourceId 资源ID
     * @return 是否有权限访问
     */
    public boolean validateNotificationOwnership(AccessControlListManager.UserInfo currentUser, Long resourceCreatorId, Long resourceId) {
        if (currentUser == null) {
            log.warn("🚨 [OWNERSHIP_CHECK] 当前用户为空，拒绝资源访问: resourceId={}", resourceId);
            return false;
        }

        if (resourceCreatorId == null) {
            log.warn("🚨 [OWNERSHIP_CHECK] 资源创建者ID为空，拒绝访问: resourceId={}", resourceId);
            return false;
        }

        // 系统管理员和校长有查看所有资源的权限
        String roleCode = currentUser.getRoleCode();
        if ("SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode)) {
            log.info("✅ [OWNERSHIP_CHECK] 管理员权限通过: user={}, role={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, resourceId);
            return true;
        }

        // 检查资源所有权 - 用户只能访问自己创建的资源
        Long currentUserId = Long.valueOf(currentUser.getEmployeeId().replace("_", "").replaceAll("[A-Z]", ""));
        boolean isOwner = Objects.equals(currentUserId, resourceCreatorId);
        
        if (!isOwner) {
            log.warn("🚨 [HORIZONTAL_PRIVILEGE_ESCALATION_BLOCKED] 水平越权尝试被阻止: " +
                    "user={}, role={}, currentUserId={}, resourceCreatorId={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, currentUserId, resourceCreatorId, resourceId);
            return false;
        }

        log.info("✅ [OWNERSHIP_CHECK] 资源所有权验证通过: user={}, resourceId={}", 
                currentUser.getEmployeeId(), resourceId);
        return true;
    }

    /**
     * 验证待办资源所有权
     * @param currentUser 当前用户
     * @param todoUserId 待办指定用户ID
     * @param todoId 待办ID
     * @return 是否有权限访问
     */
    public boolean validateTodoOwnership(AccessControlListManager.UserInfo currentUser, String todoUserId, Long todoId) {
        if (currentUser == null || todoUserId == null) {
            log.warn("🚨 [TODO_OWNERSHIP_CHECK] 参数为空，拒绝访问: todoId={}", todoId);
            return false;
        }

        // 系统管理员和校长可以查看所有待办
        String roleCode = currentUser.getRoleCode();
        if ("SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode)) {
            log.info("✅ [TODO_OWNERSHIP_CHECK] 管理员权限通过: user={}, role={}, todoId={}", 
                    currentUser.getEmployeeId(), roleCode, todoId);
            return true;
        }

        // 用户只能访问分配给自己的待办
        boolean isAssignedUser = Objects.equals(currentUser.getEmployeeId(), todoUserId);
        
        if (!isAssignedUser) {
            log.warn("🚨 [TODO_HORIZONTAL_PRIVILEGE_ESCALATION_BLOCKED] 待办水平越权尝试被阻止: " +
                    "user={}, role={}, todoUserId={}, todoId={}", 
                    currentUser.getEmployeeId(), roleCode, todoUserId, todoId);
            return false;
        }

        log.info("✅ [TODO_OWNERSHIP_CHECK] 待办所有权验证通过: user={}, todoId={}", 
                currentUser.getEmployeeId(), todoId);
        return true;
    }

    /**
     * 验证部门资源访问权限
     * @param currentUser 当前用户
     * @param targetDepartmentId 目标部门ID
     * @param resourceId 资源ID
     * @return 是否有权限访问
     */
    public boolean validateDepartmentAccess(AccessControlListManager.UserInfo currentUser, Long targetDepartmentId, Long resourceId) {
        if (currentUser == null || targetDepartmentId == null) {
            log.warn("🚨 [DEPT_ACCESS_CHECK] 参数为空，拒绝访问: resourceId={}", resourceId);
            return false;
        }

        // 系统管理员和校长可以访问所有部门资源
        String roleCode = currentUser.getRoleCode();
        if ("SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode)) {
            log.info("✅ [DEPT_ACCESS_CHECK] 管理员权限通过: user={}, role={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, resourceId);
            return true;
        }

        // 检查部门权限 - 用户只能访问所属部门的资源
        Long userDepartmentId = currentUser.getDepartmentId();
        boolean hasDepartmentAccess = Objects.equals(userDepartmentId, targetDepartmentId);
        
        if (!hasDepartmentAccess) {
            log.warn("🚨 [DEPT_HORIZONTAL_PRIVILEGE_ESCALATION_BLOCKED] 跨部门访问尝试被阻止: " +
                    "user={}, role={}, userDeptId={}, targetDeptId={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, userDepartmentId, targetDepartmentId, resourceId);
            return false;
        }

        log.info("✅ [DEPT_ACCESS_CHECK] 部门访问权限验证通过: user={}, deptId={}, resourceId={}", 
                currentUser.getEmployeeId(), targetDepartmentId, resourceId);
        return true;
    }

    /**
     * 验证班级资源访问权限
     * @param currentUser 当前用户
     * @param targetClassId 目标班级ID
     * @param resourceId 资源ID
     * @return 是否有权限访问
     */
    public boolean validateClassAccess(AccessControlListManager.UserInfo currentUser, String targetClassId, Long resourceId) {
        if (currentUser == null || targetClassId == null) {
            log.warn("🚨 [CLASS_ACCESS_CHECK] 参数为空，拒绝访问: resourceId={}", resourceId);
            return false;
        }

        // 系统管理员、校长和教务主任可以访问所有班级资源
        String roleCode = currentUser.getRoleCode();
        if ("SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode) || "ACADEMIC_ADMIN".equals(roleCode)) {
            log.info("✅ [CLASS_ACCESS_CHECK] 管理员权限通过: user={}, role={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, resourceId);
            return true;
        }

        // 检查班级权限 - 用户只能访问所属班级的资源
        String userClassId = currentUser.getClassId();
        boolean hasClassAccess = Objects.equals(userClassId, targetClassId);
        
        if (!hasClassAccess) {
            log.warn("🚨 [CLASS_HORIZONTAL_PRIVILEGE_ESCALATION_BLOCKED] 跨班级访问尝试被阻止: " +
                    "user={}, role={}, userClassId={}, targetClassId={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, userClassId, targetClassId, resourceId);
            return false;
        }

        log.info("✅ [CLASS_ACCESS_CHECK] 班级访问权限验证通过: user={}, classId={}, resourceId={}", 
                currentUser.getEmployeeId(), targetClassId, resourceId);
        return true;
    }

    /**
     * 验证年级资源访问权限
     * @param currentUser 当前用户
     * @param targetGradeId 目标年级ID
     * @param resourceId 资源ID
     * @return 是否有权限访问
     */
    public boolean validateGradeAccess(AccessControlListManager.UserInfo currentUser, String targetGradeId, Long resourceId) {
        if (currentUser == null || targetGradeId == null) {
            log.warn("🚨 [GRADE_ACCESS_CHECK] 参数为空，拒绝访问: resourceId={}", resourceId);
            return false;
        }

        // 系统管理员、校长和教务主任可以访问所有年级资源
        String roleCode = currentUser.getRoleCode();
        if ("SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode) || "ACADEMIC_ADMIN".equals(roleCode)) {
            log.info("✅ [GRADE_ACCESS_CHECK] 管理员权限通过: user={}, role={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, resourceId);
            return true;
        }

        // 检查年级权限 - 用户只能访问所属年级的资源
        String userGradeId = currentUser.getGradeId();
        boolean hasGradeAccess = Objects.equals(userGradeId, targetGradeId);
        
        if (!hasGradeAccess) {
            log.warn("🚨 [GRADE_HORIZONTAL_PRIVILEGE_ESCALATION_BLOCKED] 跨年级访问尝试被阻止: " +
                    "user={}, role={}, userGradeId={}, targetGradeId={}, resourceId={}", 
                    currentUser.getEmployeeId(), roleCode, userGradeId, targetGradeId, resourceId);
            return false;
        }

        log.info("✅ [GRADE_ACCESS_CHECK] 年级访问权限验证通过: user={}, gradeId={}, resourceId={}", 
                currentUser.getEmployeeId(), targetGradeId, resourceId);
        return true;
    }
}