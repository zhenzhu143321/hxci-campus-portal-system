package cn.iocoder.yudao.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 SCOPE-BATCH-1: 通知范围控制管理器
 * 
 * 功能包括：
 * - 通知范围分级管理 (SCHOOL_WIDE/DEPARTMENT/CLASS/GRADE)
 * - 目标受众精确控制
 * - 基于角色的范围权限验证
 * - 范围查询和过滤逻辑
 * 
 * @author Claude
 */
@Slf4j
public class NotificationScopeManager {

    /**
     * 通知范围枚举
     */
    public enum NotificationScope {
        SCHOOL_WIDE("SCHOOL_WIDE", "全校范围", 1, "面向全校师生"),
        DEPARTMENT("DEPARTMENT", "部门范围", 2, "面向特定部门"),
        CLASS("CLASS", "班级范围", 3, "面向特定班级"),
        GRADE("GRADE", "年级范围", 4, "面向特定年级");
        
        private final String code;
        private final String name;
        private final int level;
        private final String description;
        
        NotificationScope(String code, String name, int level, String description) {
            this.code = code;
            this.name = name;
            this.level = level;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public String getDescription() { return description; }
        
        public static NotificationScope fromCode(String code) {
            if (!StringUtils.hasText(code)) return SCHOOL_WIDE;
            
            for (NotificationScope scope : values()) {
                if (scope.code.equals(code)) {
                    return scope;
                }
            }
            return SCHOOL_WIDE; // 默认全校范围
        }
    }

    /**
     * 角色范围权限映射表
     */
    private static final Map<String, Set<NotificationScope>> ROLE_SCOPE_PERMISSIONS = Map.of(
        "PRINCIPAL", Set.of(NotificationScope.SCHOOL_WIDE, NotificationScope.DEPARTMENT, 
                           NotificationScope.CLASS, NotificationScope.GRADE), // 校长：所有范围
        "ACADEMIC_ADMIN", Set.of(NotificationScope.SCHOOL_WIDE, NotificationScope.DEPARTMENT, 
                                NotificationScope.GRADE), // 教务主任：除班级外所有范围
        "TEACHER", Set.of(NotificationScope.DEPARTMENT, NotificationScope.CLASS), // 教师：部门和班级
        "CLASS_TEACHER", Set.of(NotificationScope.CLASS, NotificationScope.GRADE), // 班主任：班级和年级
        "STUDENT", Set.of(NotificationScope.CLASS) // 学生：仅班级范围
    );

    /**
     * 验证发布范围权限
     */
    public static ScopePermissionResult validateScopePermission(String roleCode, String targetScope, 
                                                              Integer notificationLevel) {
        ScopePermissionResult result = new ScopePermissionResult();
        
        log.info("🎯 [SCOPE-CHECK] 开始范围权限验证 - 角色: {}, 目标范围: {}, 通知级别: {}", 
                roleCode, targetScope, notificationLevel);
        
        try {
            // 解析目标范围
            NotificationScope scope = NotificationScope.fromCode(targetScope);
            result.parsedScope = scope;
            
            // 获取角色权限
            Set<NotificationScope> allowedScopes = ROLE_SCOPE_PERMISSIONS.get(roleCode);
            if (allowedScopes == null) {
                result.hasPermission = false;
                result.reason = "未知角色: " + roleCode;
                log.warn("⛔ [SCOPE-CHECK] 未知角色: {}", roleCode);
                return result;
            }
            
            // 检查范围权限
            if (!allowedScopes.contains(scope)) {
                result.hasPermission = false;
                result.reason = String.format("角色 %s 无权限发布到范围 %s", roleCode, scope.getName());
                log.warn("⛔ [SCOPE-CHECK] 范围权限不足: 角色={}, 尝试范围={}", roleCode, scope.getName());
                return result;
            }
            
            // 特殊规则：紧急通知(级别1)需要特殊权限验证
            if (notificationLevel != null && notificationLevel == 1) {
                if (scope == NotificationScope.SCHOOL_WIDE && !"PRINCIPAL".equals(roleCode)) {
                    // 🔧 FIX: 教务主任可以发布1级紧急通知，但需要审批
                    if ("ACADEMIC_ADMIN".equals(roleCode)) {
                        result.hasPermission = true;  // ✅ 允许发布
                        result.reason = "1级紧急全校通知需要校长审批";
                        result.requiresApproval = true;  // ✅ 需要审批
                        result.approver = "校长";
                        log.info("📋 [SCOPE-CHECK] 教务主任1级紧急通知需要审批: 角色={}", roleCode);
                    } else {
                        // 其他角色不能发布1级紧急全校通知
                        result.hasPermission = false;
                        result.reason = "1级紧急全校通知只有校长和教务主任可以发布";
                        log.warn("⛔ [SCOPE-CHECK] 1级紧急全校通知权限不足: 角色={}", roleCode);
                        return result;
                    }
                }
            }
            
            // 权限验证通过
            result.hasPermission = true;
            result.reason = "范围权限验证通过";
            result.effectiveScope = scope;
            
            log.info("✅ [SCOPE-CHECK] 范围权限验证通过 - 角色: {}, 范围: {}", roleCode, scope.getName());
            
        } catch (Exception e) {
            log.error("💥 [SCOPE-CHECK] 范围权限验证异常", e);
            result.hasPermission = false;
            result.reason = "范围权限验证异常: " + e.getMessage();
        }
        
        return result;
    }

    /**
     * 获取角色可用的发布范围列表
     */
    public static List<ScopeOption> getAvailableScopes(String roleCode) {
        log.info("📋 [SCOPE-LIST] 获取可用范围列表 - 角色: {}", roleCode);
        
        Set<NotificationScope> allowedScopes = ROLE_SCOPE_PERMISSIONS.getOrDefault(roleCode, 
                                                                                  Set.of(NotificationScope.CLASS));
        
        return allowedScopes.stream()
                .map(scope -> new ScopeOption(scope.getCode(), scope.getName(), 
                                            scope.getDescription(), scope.getLevel()))
                .sorted(Comparator.comparing(ScopeOption::getLevel))
                .collect(Collectors.toList());
    }

    /**
     * 根据查看者角色过滤通知列表（基于范围）
     */
    public static List<Map<String, Object>> filterNotificationsByScope(
            List<Map<String, Object>> notifications, String viewerRole, String viewerDepartment) {
        
        log.info("🔒 [SCOPE-FILTER] 开始基于范围的通知过滤 - 角色: {}, 部门: {}", viewerRole, viewerDepartment);
        
        List<Map<String, Object>> filteredNotifications = new ArrayList<>();
        int originalCount = notifications.size();
        
        for (Map<String, Object> notification : notifications) {
            String publisherRole = (String) notification.get("publisherRole");
            String targetScope = (String) notification.get("targetScope");
            
            // 解析通知范围
            NotificationScope scope = NotificationScope.fromCode(targetScope);
            
            // 基于范围的查看权限判断
            if (canViewNotificationInScope(viewerRole, viewerDepartment, scope, publisherRole)) {
                filteredNotifications.add(notification);
                log.debug("🔒 [SCOPE-FILTER] 通知保留: ID={}, 范围={}", 
                         notification.get("id"), scope.getName());
            } else {
                log.debug("🔒 [SCOPE-FILTER] 通知过滤: ID={}, 范围={}, 查看者无权限", 
                         notification.get("id"), scope.getName());
            }
        }
        
        log.info("🔒 [SCOPE-FILTER] 范围过滤完成 - 原{}条 -> 过滤后{}条", originalCount, filteredNotifications.size());
        return filteredNotifications;
    }

    /**
     * 判断用户是否可以查看特定范围的通知
     */
    private static boolean canViewNotificationInScope(String viewerRole, String viewerDepartment, 
                                                    NotificationScope scope, String publisherRole) {
        
        switch (scope) {
            case SCHOOL_WIDE:
                // 全校通知：所有人都可以查看
                return true;
                
            case DEPARTMENT:
                // 部门通知：校长、教务主任、同部门人员可查看
                return "PRINCIPAL".equals(viewerRole) || 
                       "ACADEMIC_ADMIN".equals(viewerRole) ||
                       isSameDepartment(viewerRole, viewerDepartment, publisherRole);
                
            case CLASS:
                // 班级通知：校长、教务主任、教师、班主任、学生可查看（具体班级匹配由业务层处理）
                return !"STUDENT".equals(viewerRole) || 
                       "PRINCIPAL".equals(viewerRole) ||
                       "ACADEMIC_ADMIN".equals(viewerRole);
                
            case GRADE:
                // 年级通知：校长、教务主任、班主任、对应年级学生可查看
                return "PRINCIPAL".equals(viewerRole) || 
                       "ACADEMIC_ADMIN".equals(viewerRole) ||
                       "CLASS_TEACHER".equals(viewerRole) ||
                       ("STUDENT".equals(viewerRole) && isMatchingGrade(viewerDepartment, publisherRole));
                
            default:
                return false;
        }
    }

    /**
     * 判断是否同一部门（简化实现）
     */
    private static boolean isSameDepartment(String viewerRole, String viewerDepartment, String publisherRole) {
        // 简化实现：教师角色默认认为是同部门
        return "TEACHER".equals(viewerRole) || "CLASS_TEACHER".equals(viewerRole);
    }

    /**
     * 判断是否匹配年级（简化实现）
     */
    private static boolean isMatchingGrade(String viewerDepartment, String publisherRole) {
        // 简化实现：学生默认可以看到年级通知
        return true;
    }

    /**
     * 生成范围SQL条件（用于数据库查询优化）
     */
    public static String generateScopeFilterSQL(String viewerRole, String viewerDepartment) {
        StringBuilder sqlCondition = new StringBuilder();
        
        switch (viewerRole) {
            case "PRINCIPAL":
                // 校长可以看到所有范围
                sqlCondition.append("1=1");
                break;
                
            case "ACADEMIC_ADMIN":
                // 教务主任可以看到除个别班级通知外的所有通知
                sqlCondition.append("(target_scope IN ('SCHOOL_WIDE', 'DEPARTMENT', 'GRADE'))");
                break;
                
            case "TEACHER":
            case "CLASS_TEACHER":
                // 教师可以看到全校、部门、班级通知
                sqlCondition.append("(target_scope IN ('SCHOOL_WIDE', 'DEPARTMENT', 'CLASS'))");
                break;
                
            case "STUDENT":
                // 学生可以看到全校、班级、年级通知
                sqlCondition.append("(target_scope IN ('SCHOOL_WIDE', 'CLASS', 'GRADE'))");
                break;
                
            default:
                // 默认只能看全校通知
                sqlCondition.append("(target_scope = 'SCHOOL_WIDE')");
                break;
        }
        
        log.debug("🔍 [SCOPE-SQL] 生成范围过滤SQL - 角色: {}, 条件: {}", viewerRole, sqlCondition.toString());
        return sqlCondition.toString();
    }

    /**
     * 范围权限验证结果类
     */
    public static class ScopePermissionResult {
        public boolean hasPermission = false;
        public String reason = "";
        public NotificationScope parsedScope;
        public NotificationScope effectiveScope;
        public boolean requiresApproval = false;
        public String approver;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("hasPermission", hasPermission);
            map.put("reason", reason);
            map.put("parsedScope", parsedScope != null ? parsedScope.getName() : null);
            map.put("effectiveScope", effectiveScope != null ? effectiveScope.getName() : null);
            map.put("requiresApproval", requiresApproval);
            map.put("approver", approver);
            return map;
        }
    }

    /**
     * 范围选项类
     */
    public static class ScopeOption {
        public String code;
        public String name;
        public String description;
        public int level;
        
        public ScopeOption(String code, String name, String description, int level) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.level = level;
        }
        
        public int getLevel() { return level; }
    }
}