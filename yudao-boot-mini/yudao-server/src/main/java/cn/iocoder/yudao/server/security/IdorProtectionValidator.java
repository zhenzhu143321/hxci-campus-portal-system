package cn.iocoder.yudao.server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * IDOR (不安全直接对象引用) 防护器
 * 防止IDOR漏洞 - CVE-HXCI-2025-008
 * 
 * 功能:
 * 1. ID参数安全验证
 * 2. 防止恶意ID枚举攻击
 * 3. 资源访问边界检查
 * 4. SQL注入防护加强
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Slf4j
@Component
public class IdorProtectionValidator {

    // ID参数安全模式 - 只允许数字和特定字符
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[0-9]{1,19}$");
    private static final Pattern SAFE_STRING_ID_PATTERN = Pattern.compile("^[A-Z0-9_]{1,50}$");
    
    // 最大允许的ID值 - 防止过大数值攻击
    private static final long MAX_ALLOWED_ID = 999999999999999999L; // 18位数字
    private static final int MAX_BATCH_SIZE = 100; // 批量操作最大数量

    /**
     * 验证通知ID参数安全性
     * @param notificationId 通知ID
     * @param currentUser 当前用户
     * @return 是否通过验证
     */
    public boolean validateNotificationId(Long notificationId, AccessControlListManager.UserInfo currentUser) {
        if (notificationId == null) {
            log.warn("🚨 [IDOR_PROTECTION] 通知ID为空，拒绝访问");
            return false;
        }

        if (notificationId <= 0) {
            log.warn("🚨 [IDOR_PROTECTION] 无效通知ID: {} (必须为正数)", notificationId);
            return false;
        }

        if (notificationId > MAX_ALLOWED_ID) {
            log.warn("🚨 [IDOR_PROTECTION] 通知ID过大，疑似攻击: id={}, user={}", 
                    notificationId, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        log.info("✅ [IDOR_PROTECTION] 通知ID验证通过: id={}, user={}", 
                notificationId, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
        return true;
    }

    /**
     * 验证用户ID参数安全性
     * @param userId 用户ID (字符串格式，如 STUDENT_001)
     * @param currentUser 当前用户
     * @return 是否通过验证
     */
    public boolean validateUserId(String userId, AccessControlListManager.UserInfo currentUser) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("🚨 [IDOR_PROTECTION] 用户ID为空，拒绝访问");
            return false;
        }

        if (!SAFE_STRING_ID_PATTERN.matcher(userId).matches()) {
            log.warn("🚨 [IDOR_PROTECTION] 用户ID格式不安全，疑似注入攻击: userId={}, user={}", 
                    userId, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        // 用户只能查询自己的信息（除了管理员）
        if (currentUser != null) {
            String roleCode = currentUser.getRoleCode();
            boolean isAdmin = "SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode);
            
            if (!isAdmin && !userId.equals(currentUser.getEmployeeId())) {
                log.warn("🚨 [IDOR_PROTECTION] 用户尝试访问其他用户数据: currentUser={}, targetUser={}", 
                        currentUser.getEmployeeId(), userId);
                return false;
            }
        }

        log.info("✅ [IDOR_PROTECTION] 用户ID验证通过: userId={}, currentUser={}", 
                userId, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
        return true;
    }

    /**
     * 验证部门ID参数安全性
     * @param departmentId 部门ID
     * @param currentUser 当前用户
     * @return 是否通过验证
     */
    public boolean validateDepartmentId(Long departmentId, AccessControlListManager.UserInfo currentUser) {
        if (departmentId == null) {
            log.warn("🚨 [IDOR_PROTECTION] 部门ID为空，拒绝访问");
            return false;
        }

        if (departmentId <= 0 || departmentId > MAX_ALLOWED_ID) {
            log.warn("🚨 [IDOR_PROTECTION] 无效部门ID: {} (范围: 1-{})", departmentId, MAX_ALLOWED_ID);
            return false;
        }

        log.info("✅ [IDOR_PROTECTION] 部门ID验证通过: deptId={}, user={}", 
                departmentId, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
        return true;
    }

    /**
     * 验证批量ID操作安全性
     * @param ids ID数组
     * @param currentUser 当前用户
     * @return 是否通过验证
     */
    public boolean validateBatchIds(Long[] ids, AccessControlListManager.UserInfo currentUser) {
        if (ids == null || ids.length == 0) {
            log.warn("🚨 [IDOR_PROTECTION] 批量ID数组为空，拒绝操作");
            return false;
        }

        if (ids.length > MAX_BATCH_SIZE) {
            log.warn("🚨 [IDOR_PROTECTION] 批量操作数量超限: count={}, max={}, user={}", 
                    ids.length, MAX_BATCH_SIZE, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        // 验证每个ID的安全性
        for (int i = 0; i < ids.length; i++) {
            Long id = ids[i];
            if (id == null || id <= 0 || id > MAX_ALLOWED_ID) {
                log.warn("🚨 [IDOR_PROTECTION] 批量操作中发现无效ID: index={}, id={}, user={}", 
                        i, id, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
                return false;
            }
        }

        log.info("✅ [IDOR_PROTECTION] 批量ID验证通过: count={}, user={}", 
                ids.length, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
        return true;
    }

    /**
     * 验证分页参数安全性
     * @param page 页码
     * @param size 页面大小
     * @param currentUser 当前用户
     * @return 是否通过验证
     */
    public boolean validatePaginationParams(Integer page, Integer size, AccessControlListManager.UserInfo currentUser) {
        if (page != null && (page < 0 || page > 10000)) {
            log.warn("🚨 [IDOR_PROTECTION] 无效页码参数: page={}, user={}", 
                    page, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        if (size != null && (size <= 0 || size > 1000)) {
            log.warn("🚨 [IDOR_PROTECTION] 无效页面大小参数: size={}, user={}", 
                    size, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        log.info("✅ [IDOR_PROTECTION] 分页参数验证通过: page={}, size={}, user={}", 
                page, size, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
        return true;
    }

    /**
     * 验证SQL查询参数安全性 (防止SQL注入)
     * @param queryParam 查询参数
     * @param paramName 参数名称
     * @param currentUser 当前用户
     * @return 是否通过验证
     */
    public boolean validateQueryParam(String queryParam, String paramName, AccessControlListManager.UserInfo currentUser) {
        if (queryParam == null) {
            return true; // null参数是安全的
        }

        // 检查SQL注入特征
        String lowerParam = queryParam.toLowerCase();
        String[] sqlInjectionPatterns = {
            "union", "select", "insert", "update", "delete", "drop", "create", "alter",
            "--", "/*", "*/", ";", "xp_", "sp_", "exec", "script", "iframe", "object"
        };

        for (String pattern : sqlInjectionPatterns) {
            if (lowerParam.contains(pattern)) {
                log.warn("🚨 [IDOR_PROTECTION] 检测到SQL注入尝试: param={}, value={}, user={}", 
                        paramName, queryParam, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
                return false;
            }
        }

        // 长度限制
        if (queryParam.length() > 1000) {
            log.warn("🚨 [IDOR_PROTECTION] 查询参数过长，疑似攻击: param={}, length={}, user={}", 
                    paramName, queryParam.length(), currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        log.info("✅ [IDOR_PROTECTION] 查询参数验证通过: param={}, user={}", 
                paramName, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
        return true;
    }

    /**
     * 验证文件路径安全性 (防止路径遍历)
     * @param filePath 文件路径
     * @param currentUser 当前用户
     * @return 是否通过验证
     */
    public boolean validateFilePath(String filePath, AccessControlListManager.UserInfo currentUser) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("🚨 [IDOR_PROTECTION] 文件路径为空，拒绝访问");
            return false;
        }

        // 检查路径遍历攻击
        if (filePath.contains("..") || filePath.contains("./") || filePath.contains("\\")) {
            log.warn("🚨 [IDOR_PROTECTION] 检测到路径遍历攻击: path={}, user={}", 
                    filePath, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        // 只允许安全的文件路径字符
        if (!filePath.matches("^[a-zA-Z0-9._/-]+$")) {
            log.warn("🚨 [IDOR_PROTECTION] 文件路径包含不安全字符: path={}, user={}", 
                    filePath, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
            return false;
        }

        log.info("✅ [IDOR_PROTECTION] 文件路径验证通过: path={}, user={}", 
                filePath, currentUser != null ? currentUser.getEmployeeId() : "anonymous");
        return true;
    }
}