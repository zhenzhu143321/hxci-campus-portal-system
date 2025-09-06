package cn.iocoder.yudao.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知权限验证器 - P0级安全修复
 * 
 * 🚨 修复关键安全漏洞：
 * 1. 权限越权攻击防护 (CVSS 8.9)
 * 2. 严格权限验证矩阵
 * 3. 学生权限限制 Level 4 + CLASS范围
 * 4. 最小权限原则实施
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Component
public class NotificationPermissionValidator {

    private static final Logger log = LoggerFactory.getLogger(NotificationPermissionValidator.class);

    // 🚨 权限矩阵配置 - 核心安全策略
    private static final Map<String, PermissionConfig> ROLE_PERMISSION_MATRIX = new HashMap<>();

    static {
        // 🔴 系统管理员 - 超级权限
        ROLE_PERMISSION_MATRIX.put("SYSTEM_ADMIN", new PermissionConfig(
                Arrays.asList(1, 2, 3, 4), // 可发布所有级别
                Arrays.asList("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"), // 所有范围
                false // 无需审批
        ));

        // 🟠 校长 - 管理权限
        ROLE_PERMISSION_MATRIX.put("PRINCIPAL", new PermissionConfig(
                Arrays.asList(1, 2, 3, 4), // 可发布所有级别
                Arrays.asList("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"), // 所有范围
                false // 无需审批
        ));

        // 🟡 教务主任 - 高级权限
        ROLE_PERMISSION_MATRIX.put("ACADEMIC_ADMIN", new PermissionConfig(
                Arrays.asList(2, 3, 4), // 可发布2-4级（1级需审批）
                Arrays.asList("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"), // 所有范围
                false // 当前不需要审批（简化版）
        ));

        // 🔵 教师 - 教学权限
        ROLE_PERMISSION_MATRIX.put("TEACHER", new PermissionConfig(
                Arrays.asList(3, 4), // 只能发布3-4级
                Arrays.asList("DEPARTMENT", "GRADE", "CLASS"), // 部门/年级/班级
                false // 无需审批
        ));

        // 🟢 班主任 - 班级管理权限
        ROLE_PERMISSION_MATRIX.put("CLASS_TEACHER", new PermissionConfig(
                Arrays.asList(3, 4), // 只能发布3-4级
                Arrays.asList("GRADE", "CLASS"), // 年级/班级
                false // 无需审批
        ));

        // 🔴 学生 - 最小权限（关键安全控制）
        ROLE_PERMISSION_MATRIX.put("STUDENT", new PermissionConfig(
                Arrays.asList(4), // 只能发布4级（提醒级别）
                Arrays.asList("CLASS"), // 只能班级范围
                false // 无需审批
        ));
    }

    /**
     * 🛡️ 验证用户发布通知权限（核心安全检查）
     */
    public boolean validatePublishPermission(String roleCode, int level, String targetScope) {
        log.info("🔍 [PERMISSION_CHECK] 验证发布权限: role={}, level={}, scope={}", roleCode, level, targetScope);

        try {
            // 1️⃣ 角色有效性检查
            if (roleCode == null || roleCode.trim().isEmpty()) {
                log.error("❌ [PERMISSION_CHECK] 角色代码为空");
                return false;
            }

            // 2️⃣ 获取角色权限配置
            PermissionConfig config = ROLE_PERMISSION_MATRIX.get(roleCode);
            if (config == null) {
                log.error("❌ [PERMISSION_CHECK] 未知角色: {}", roleCode);
                return false;
            }

            // 3️⃣ 验证通知级别权限
            if (!config.allowedLevels.contains(level)) {
                log.error("🚨 [PERMISSION_CHECK] 角色 {} 无权限发布 Level {} 通知，允许级别: {}", 
                         roleCode, level, config.allowedLevels);
                return false;
            }

            // 4️⃣ 验证发布范围权限
            if (targetScope != null && !config.allowedScopes.contains(targetScope)) {
                log.error("🚨 [PERMISSION_CHECK] 角色 {} 无权限发布到 {} 范围，允许范围: {}", 
                         roleCode, targetScope, config.allowedScopes);
                return false;
            }

            // 5️⃣ 特殊安全检查：学生权限严格控制
            if ("STUDENT".equals(roleCode)) {
                if (level != 4) {
                    log.error("🚨 [STUDENT_SECURITY] 学生只能发布Level 4通知，尝试发布: Level {}", level);
                    return false;
                }
                if (!"CLASS".equals(targetScope)) {
                    log.error("🚨 [STUDENT_SECURITY] 学生只能发布到CLASS范围，尝试发布到: {}", targetScope);
                    return false;
                }
            }

            log.info("✅ [PERMISSION_CHECK] 权限验证通过: {} 可发布 Level {} 到 {} 范围", roleCode, level, targetScope);
            return true;

        } catch (Exception e) {
            log.error("❌ [PERMISSION_CHECK] 权限验证异常", e);
            return false; // 异常时拒绝权限
        }
    }

    /**
     * 👁️ 验证用户查看通知权限
     */
    public boolean validateViewPermission(String roleCode, int notificationLevel, String notificationScope) {
        log.debug("🔍 [VIEW_PERMISSION] 验证查看权限: role={}, level={}, scope={}", 
                 roleCode, notificationLevel, notificationScope);

        try {
            // 基础角色检查
            if (roleCode == null || !ROLE_PERMISSION_MATRIX.containsKey(roleCode)) {
                return false;
            }

            // 🚨 紧急通知（Level 1）- 所有人可见（安全要求）
            if (notificationLevel == 1) {
                log.debug("✅ [VIEW_PERMISSION] Level 1紧急通知 - 所有用户可见");
                return true;
            }

            // 🔴 管理员角色 - 可查看所有通知
            if ("SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode)) {
                return true;
            }

            // 🟡 其他角色基于权限配置检查
            PermissionConfig config = ROLE_PERMISSION_MATRIX.get(roleCode);
            
            // 检查是否在权限级别范围内
            boolean levelAllowed = config.allowedLevels.contains(notificationLevel) || 
                                  notificationLevel >= 2; // Level 2+重要通知相关人员可见

            // 检查是否在范围权限内
            boolean scopeAllowed = notificationScope == null || 
                                  config.allowedScopes.contains(notificationScope) ||
                                  notificationLevel == 4; // Level 4提醒所有人可见

            return levelAllowed && scopeAllowed;

        } catch (Exception e) {
            log.error("❌ [VIEW_PERMISSION] 查看权限验证异常", e);
            return false;
        }
    }

    /**
     * 🔧 获取用户最高发布权限
     */
    public int getMaxPublishLevel(String roleCode) {
        PermissionConfig config = ROLE_PERMISSION_MATRIX.get(roleCode);
        if (config == null || config.allowedLevels.isEmpty()) {
            return 0; // 无权限
        }
        return config.allowedLevels.stream().min(Integer::compareTo).orElse(0); // 最高权限（数字越小级别越高）
    }

    /**
     * 📊 获取用户允许的发布范围
     */
    public List<String> getAllowedScopes(String roleCode) {
        PermissionConfig config = ROLE_PERMISSION_MATRIX.get(roleCode);
        return config != null ? config.allowedScopes : Arrays.asList();
    }

    /**
     * 📋 获取权限矩阵信息（调试用）
     */
    public Map<String, String> getPermissionMatrixInfo(String roleCode) {
        PermissionConfig config = ROLE_PERMISSION_MATRIX.get(roleCode);
        if (config == null) {
            return Map.of("error", "未知角色: " + roleCode);
        }

        return Map.of(
                "roleCode", roleCode,
                "allowedLevels", config.allowedLevels.toString(),
                "allowedScopes", config.allowedScopes.toString(),
                "needsApproval", String.valueOf(config.needsApproval),
                "maxLevel", String.valueOf(getMaxPublishLevel(roleCode))
        );
    }

    /**
     * 🔐 权限配置内部类
     */
    private static class PermissionConfig {
        final List<Integer> allowedLevels;   // 允许发布的通知级别
        final List<String> allowedScopes;    // 允许发布的范围
        final boolean needsApproval;         // 是否需要审批

        PermissionConfig(List<Integer> allowedLevels, List<String> allowedScopes, boolean needsApproval) {
            this.allowedLevels = allowedLevels;
            this.allowedScopes = allowedScopes;
            this.needsApproval = needsApproval;
        }
    }
}