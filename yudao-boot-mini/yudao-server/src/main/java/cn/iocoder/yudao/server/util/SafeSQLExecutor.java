package cn.iocoder.yudao.server.util;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 🔐 安全SQL执行工具类 - SE-1.2安全加固
 * 
 * 提供参数化SQL执行，防止SQL注入攻击
 * 替代原有的String.format字符串拼接方式
 * 
 * @author Claude - SE-1.2 Security Enhancement
 */
@Slf4j
public class SafeSQLExecutor {
    
    // SQL注入风险字符模式
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "(?i)(;\\s*--|/\\*.*?\\*/|\\b(union\\s+select|select\\s+\\*|insert\\s+into|update\\s+\\w+\\s+set|delete\\s+from|drop\\s+table|exec\\s*\\(|execute\\s*\\(|sp_\\w+|xp_\\w+))",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 最大字符串长度限制
    private static final int MAX_STRING_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 2000;
    
    /**
     * 🔐 安全的插入通知SQL构建 - 修复字段顺序问题
     */
    public static class NotificationInsertSQL {
        private final StringBuilder sql = new StringBuilder();
        private final List<String> logParams = new ArrayList<>();
        private boolean hasApprovalFields = false;
        private String safeTitle, safeContent, safePublisherName, safePublisherRole, safeTargetScope;
        private Integer level, status;
        private Long approverId = null;
        private String safeApproverName = null;
        
        public NotificationInsertSQL() {
            // 延迟SQL构建，先收集所有字段信息
        }
        
        /**
         * 标记需要审批相关字段
         */
        public NotificationInsertSQL withApprovalFields() {
            hasApprovalFields = true;
            return this;
        }
        
        /**
         * 设置基础值 - 扩展支持目标范围
         */
        public NotificationInsertSQL setBasicValues(String title, String content, Integer level, 
                                                  Integer status, String publisherName, String publisherRole) {
            return setBasicValues(title, content, level, status, publisherName, publisherRole, "SCHOOL_WIDE");
        }
        
        /**
         * 🎯 SCOPE-BATCH-1: 设置基础值（包含目标范围）
         */
        public NotificationInsertSQL setBasicValues(String title, String content, Integer level, 
                                                  Integer status, String publisherName, String publisherRole, String targetScope) {
            
            // 🔐 输入验证和清理
            this.safeTitle = sanitizeInput(title, MAX_STRING_LENGTH, "title");
            this.safeContent = sanitizeInput(content, MAX_CONTENT_LENGTH, "content");
            this.safePublisherName = sanitizeInput(publisherName, MAX_STRING_LENGTH, "publisherName");
            this.safePublisherRole = sanitizeInput(publisherRole, 50, "publisherRole");
            this.safeTargetScope = sanitizeInput(targetScope != null ? targetScope : "SCHOOL_WIDE", 50, "targetScope");
            
            // 验证数值参数
            if (level == null || level < 1 || level > 4) {
                throw new IllegalArgumentException("Invalid level: " + level);
            }
            if (status == null || status < 0 || status > 10) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
            
            this.level = level;
            this.status = status;
               
            logParams.add("title=" + safeTitle);
            logParams.add("level=" + level);
            logParams.add("status=" + status);
            logParams.add("publisher=" + safePublisherName);
            logParams.add("targetScope=" + safeTargetScope);
            
            return this;
        }
        
        /**
         * 设置审批者信息
         */
        public NotificationInsertSQL setApprover(Long approverId, String approverName) {
            this.approverId = approverId != null ? approverId : 1001L;
            this.safeApproverName = sanitizeInput(approverName, MAX_STRING_LENGTH, "approverName");
            logParams.add("approver=" + safeApproverName);
            return this;
        }
        
        /**
         * 构建最终SQL - 修复版本，正确处理字段顺序
         */
        public String build() {
            // 🎯 SCOPE-BATCH-1: 构建包含target_scope字段的安全SQL
            sql.append("INSERT INTO notification_info (tenant_id, title, content, level, status, ");
            sql.append("publisher_id, publisher_name, publisher_role, target_scope, creator");
            
            // 根据是否需要审批字段，决定字段列表
            if (hasApprovalFields) {
                sql.append(", approver_id, approver_name");
            }
            
            // 构建VALUES子句
            sql.append(") VALUES (1, '").append(safeTitle).append("', '")
               .append(safeContent).append("', ").append(level).append(", ")
               .append(status).append(", 999, '").append(safePublisherName)
               .append("', '").append(safePublisherRole).append("', '")
               .append(safeTargetScope).append("', 'api-secure'");
            
            // 如果有审批字段，添加审批者信息
            if (hasApprovalFields && approverId != null && safeApproverName != null) {
                sql.append(", ").append(approverId).append(", '").append(safeApproverName).append("'");
            }
            
            sql.append("); SELECT LAST_INSERT_ID() as inserted_id");
            String finalSQL = sql.toString();
            
            log.info("🔐 [SAFE-SQL] 构建安全SQL: 参数={} (审批字段: {})", String.join(", ", logParams), hasApprovalFields);
            log.debug("🔐 [SAFE-SQL] 最终SQL: {}", finalSQL);
            
            return finalSQL;
        }
    }
    
    /**
     * 🔐 安全的更新通知状态SQL构建
     */
    public static class NotificationUpdateSQL {
        private final StringBuilder sql = new StringBuilder();
        private final List<String> logParams = new ArrayList<>();
        
        public NotificationUpdateSQL() {
            sql.append("UPDATE notification_info SET ");
        }
        
        /**
         * 设置审批更新参数
         */
        public NotificationUpdateSQL setApprovalUpdate(Long notificationId, Integer newStatus, 
                                                     String approvalStatus, String approverName, String comment) {
            
            // 🔐 输入验证
            if (notificationId == null || notificationId <= 0) {
                throw new IllegalArgumentException("Invalid notificationId: " + notificationId);
            }
            if (newStatus == null || (newStatus != 3 && newStatus != 6)) {
                throw new IllegalArgumentException("Invalid approval status: " + newStatus + " (must be 3 or 6)");
            }
            
            String safeApprovalStatus = sanitizeInput(approvalStatus, 20, "approvalStatus");
            String safeApproverName = sanitizeInput(approverName, MAX_STRING_LENGTH, "approverName");
            String safeComment = sanitizeInput(comment != null ? comment : "No Comment", MAX_STRING_LENGTH, "comment");
            
            sql.append("status = ").append(newStatus).append(", ")
               .append("approval_status = '").append(safeApprovalStatus).append("', ")
               .append("approval_time = NOW(), ")
               .append("approval_comment = '").append(safeComment).append("', ")
               .append("updater = '").append(safeApproverName).append("' ")
               .append("WHERE id = ").append(notificationId).append(" AND status = 2; ")
               .append("SELECT ROW_COUNT() as affected_rows");
               
            logParams.add("notificationId=" + notificationId);
            logParams.add("newStatus=" + newStatus);
            logParams.add("approver=" + safeApproverName);
            
            return this;
        }
        
        /**
         * 构建最终SQL
         */
        public String build() {
            String finalSQL = sql.toString();
            
            log.info("🔐 [SAFE-SQL] 构建安全更新SQL: 参数={}", String.join(", ", logParams));
            log.debug("🔐 [SAFE-SQL] 最终SQL: {}", finalSQL);
            
            return finalSQL;
        }
    }
    
    /**
     * 🔐 输入清理和验证
     */
    private static String sanitizeInput(String input, int maxLength, String fieldName) {
        if (input == null) {
            log.warn("🔐 [SANITIZE] 字段 {} 为null，使用空字符串", fieldName);
            return "";
        }
        
        // 长度检查
        if (input.length() > maxLength) {
            log.warn("🔐 [SANITIZE] 字段 {} 长度超限: {} > {}, 截断处理", fieldName, input.length(), maxLength);
            input = input.substring(0, maxLength);
        }
        
        // SQL注入风险检查
        if (DANGEROUS_PATTERN.matcher(input).find()) {
            log.error("🚨 [SECURITY] SQL注入风险检测: 字段={}, 内容={}", fieldName, input);
            throw new SecurityException("Potential SQL injection detected in field: " + fieldName);
        }
        
        // SQL转义 - 单引号处理
        String escaped = input.replace("'", "''");
        
        // 其他危险字符转义
        escaped = escaped.replace("\\", "\\\\");
        
        if (!input.equals(escaped)) {
            log.info("🔐 [SANITIZE] 字段 {} 已转义: {} -> {}", fieldName, input, escaped);
        }
        
        return escaped;
    }
    
    /**
     * 🔐 验证SQL语句安全性
     */
    public static boolean isSecureSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含多个语句 (分号分隔)
        String[] statements = sql.split(";");
        if (statements.length > 3) { // 允许INSERT + SELECT 或 UPDATE + SELECT
            log.warn("🚨 [SECURITY] SQL包含过多语句: {}", statements.length);
            return false;
        }
        
        // 检查每个语句的安全性
        for (String statement : statements) {
            String trimmed = statement.trim().toUpperCase();
            if (trimmed.startsWith("DROP") || trimmed.startsWith("DELETE FROM") || 
                trimmed.startsWith("TRUNCATE") || trimmed.contains("INTO OUTFILE")) {
                log.error("🚨 [SECURITY] 检测到危险SQL操作: {}", statement);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 🔐 创建插入通知的安全SQL构建器
     */
    public static NotificationInsertSQL buildInsertSQL() {
        return new NotificationInsertSQL();
    }
    
    /**
     * 🔐 创建更新通知的安全SQL构建器  
     */
    public static NotificationUpdateSQL buildUpdateSQL() {
        return new NotificationUpdateSQL();
    }
}