package cn.iocoder.yudao.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 🛡️ SECURITY-BATCH-1: 安全增强工具类
 * 
 * 集成所有安全相关功能：
 * - SE-1.3: 输入参数验证
 * - SE-2.1: HTML转义
 * - INPUT-1.1: 超长字符串处理 
 * - SECURITY-ENHANCE: 危险操作检测
 * 
 * @author Claude
 */
@Slf4j
public class SecurityEnhancementUtil {

    // 🔐 SE-1.3: 输入验证配置
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final int MAX_COMMENT_LENGTH = 500;
    
    // 🛡️ SE-2.1: HTML转义映射 (修复：移除日期斜杠转义)
    private static final Map<String, String> HTML_ESCAPE_MAP = Map.of(
        "&", "&amp;",
        "<", "&lt;",
        ">", "&gt;",
        "\"", "&quot;",
        "'", "&#x27;"
        // 移除 "/" 转义，避免破坏日期格式如 2025/8/12
    );
    
    // 🚨 SECURITY-ENHANCE: 危险操作检测模式
    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)(<script[^>]*>.*?</script>)", Pattern.DOTALL),
        Pattern.compile("(?i)(javascript:)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(on\\w+\\s*=)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(eval\\s*\\()", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(expression\\s*\\()", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 🔐 SE-1.3: 验证通知发布请求参数
     */
    public static ValidationResult validateNotificationRequest(String title, String content, Integer level) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        
        log.info("🔐 [VALIDATE] 开始验证通知发布参数");
        
        // 验证标题
        if (!StringUtils.hasText(title)) {
            errors.add("通知标题不能为空");
        } else if (title.trim().length() > MAX_TITLE_LENGTH) {
            errors.add(String.format("通知标题长度不能超过%d个字符，当前长度：%d", MAX_TITLE_LENGTH, title.length()));
        } else if (containsDangerousContent(title)) {
            errors.add("通知标题包含不安全内容");
        }
        
        // 验证内容
        if (!StringUtils.hasText(content)) {
            errors.add("通知内容不能为空");
        } else if (content.trim().length() > MAX_CONTENT_LENGTH) {
            errors.add(String.format("通知内容长度不能超过%d个字符，当前长度：%d", MAX_CONTENT_LENGTH, content.length()));
        } else if (containsDangerousContent(content)) {
            errors.add("通知内容包含不安全内容");
        }
        
        // 验证级别
        if (level == null) {
            errors.add("通知级别不能为空");
        } else if (level < 1 || level > 4) {
            errors.add("通知级别必须在1-4之间");
        }
        
        result.isValid = errors.isEmpty();
        result.errors = errors;
        result.sanitizedTitle = title != null ? sanitizeHtml(title.trim()) : null;
        result.sanitizedContent = content != null ? sanitizeHtml(content.trim()) : null;
        
        log.info("🔐 [VALIDATE] 参数验证完成: {}, 错误数量: {}", result.isValid ? "通过" : "失败", errors.size());
        return result;
    }
    
    /**
     * 🔐 SE-1.3: 验证审批请求参数
     */
    public static ValidationResult validateApprovalRequest(Long notificationId, String comment) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        
        log.info("🔐 [VALIDATE] 开始验证审批请求参数");
        
        // 验证通知ID
        if (notificationId == null || notificationId <= 0) {
            errors.add("通知ID无效");
        }
        
        // 验证审批意见（可选）
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            errors.add(String.format("审批意见长度不能超过%d个字符，当前长度：%d", MAX_COMMENT_LENGTH, comment.length()));
        } else if (comment != null && containsDangerousContent(comment)) {
            errors.add("审批意见包含不安全内容");
        }
        
        result.isValid = errors.isEmpty();
        result.errors = errors;
        result.sanitizedComment = comment != null ? sanitizeHtml(comment.trim()) : "";
        
        log.info("🔐 [VALIDATE] 审批参数验证完成: {}, 错误数量: {}", result.isValid ? "通过" : "失败", errors.size());
        return result;
    }
    
    /**
     * 🛡️ SE-2.1: HTML转义处理
     */
    public static String sanitizeHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        log.debug("🛡️ [HTML-ESCAPE] 开始HTML转义处理");
        
        String result = input;
        for (Map.Entry<String, String> entry : HTML_ESCAPE_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        log.debug("🛡️ [HTML-ESCAPE] HTML转义完成");
        return result;
    }
    
    /**
     * 🚨 SECURITY-ENHANCE: 检测危险内容
     */
    private static boolean containsDangerousContent(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("🚨 [DANGEROUS] 检测到危险内容: {}", pattern.pattern());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🔧 INPUT-1.1: 安全截断超长字符串
     */
    public static String safeTruncate(String input, int maxLength, String suffix) {
        if (!StringUtils.hasText(input) || input.length() <= maxLength) {
            return input;
        }
        
        String truncated = input.substring(0, maxLength - suffix.length()) + suffix;
        log.info("🔧 [TRUNCATE] 字符串已截断: {} -> {} 字符", input.length(), truncated.length());
        return truncated;
    }
    
    /**
     * 📊 INPUT-1.2: 生成详细的错误报告
     */
    public static String generateDetailedErrorReport(List<String> errors, String operation) {
        if (errors.isEmpty()) {
            return "操作成功";
        }
        
        StringBuilder report = new StringBuilder();
        report.append(String.format("操作 '%s' 失败，发现 %d 个错误：\n", operation, errors.size()));
        
        for (int i = 0; i < errors.size(); i++) {
            report.append(String.format("%d. %s\n", i + 1, errors.get(i)));
        }
        
        report.append("\n请修正上述错误后重试。");
        
        log.info("📊 [ERROR-REPORT] 生成详细错误报告: {} 个错误", errors.size());
        return report.toString();
    }
    
    /**
     * 🔍 SECURITY-ENHANCE: 安全审计日志
     */
    public static void auditSecurityEvent(String event, String userInfo, Map<String, Object> details) {
        log.warn("🔍 [SECURITY-AUDIT] 事件: {} | 用户: {} | 详情: {}", event, userInfo, details);
        
        // 在生产环境中，这里应该写入专门的安全审计日志文件
        // 或发送到安全监控系统
    }
    
    /**
     * 📋 T13专用: 验证待办通知发布请求参数
     */
    public static List<String> validateTodoPublishRequest(Map<String, Object> request) {
        List<String> errors = new ArrayList<>();
        
        // 验证标题
        String title = (String) request.get("title");
        if (!StringUtils.hasText(title)) {
            errors.add("待办标题不能为空");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.add(String.format("待办标题长度不能超过%d个字符，当前长度：%d", MAX_TITLE_LENGTH, title.length()));
        } else if (containsDangerousContent(title)) {
            errors.add("待办标题包含不安全内容");
        }
        
        // 验证内容
        String content = (String) request.get("content");
        if (!StringUtils.hasText(content)) {
            errors.add("待办内容不能为空");
        } else if (content.length() > MAX_CONTENT_LENGTH) {
            errors.add(String.format("待办内容长度不能超过%d个字符，当前长度：%d", MAX_CONTENT_LENGTH, content.length()));
        } else if (containsDangerousContent(content)) {
            errors.add("待办内容包含不安全内容");
        }
        
        // 验证优先级
        String priority = (String) request.get("priority");
        if (!StringUtils.hasText(priority)) {
            errors.add("待办优先级不能为空");
        } else if (!Arrays.asList("low", "medium", "high").contains(priority)) {
            errors.add("待办优先级必须是 low、medium 或 high");
        }
        
        // 验证截止日期 - 支持deadline和dueDate两种字段名
        String deadline = (String) request.get("deadline");
        if (deadline == null) {
            deadline = (String) request.get("dueDate"); // 向后兼容
        }
        if (!StringUtils.hasText(deadline)) {
            errors.add("待办截止日期不能为空");
        } else if (!deadline.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            errors.add("待办截止日期格式不正确，应为 YYYY-MM-DD 或 YYYY-MM-DDTHH:mm:ss");
        }
        
        // 验证目标范围
        String targetScope = (String) request.get("targetScope");
        if (StringUtils.hasText(targetScope)) {
            if (!Arrays.asList("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS").contains(targetScope)) {
                errors.add("目标范围必须是 SCHOOL_WIDE、DEPARTMENT、GRADE 或 CLASS");
            }
        }
        
        log.info("📋 [TODO-VALIDATE] 待办发布参数验证完成: {}, 错误数量: {}", 
                errors.isEmpty() ? "通过" : "失败", errors.size());
        return errors;
    }
    
    /**
     * 🛡️ T13专用: HTML转义处理（简化版，用于待办通知）
     */
    public static String escapeHTML(String input) {
        return sanitizeHtml(input);
    }
    
    /**
     * 🔧 T13专用: SQL转义处理
     */
    public static String escapeSQL(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        // 基本SQL注入防护
        String result = input
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
            
        log.debug("🔧 [SQL-ESCAPE] SQL转义完成");
        return result;
    }
    
    /**
     * 🔧 T13专用: 安全截断字符串
     */
    public static String truncateString(String input, int maxLength) {
        if (!StringUtils.hasText(input) || input.length() <= maxLength) {
            return input;
        }
        
        String truncated = input.substring(0, maxLength - 3) + "...";
        log.debug("🔧 [TRUNCATE] 字符串已截断: {} -> {} 字符", input.length(), truncated.length());
        return truncated;
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        public boolean isValid = false;
        public List<String> errors = new ArrayList<>();
        public String sanitizedTitle;
        public String sanitizedContent;  
        public String sanitizedComment;
        
        public String getErrorSummary() {
            return errors.stream().collect(Collectors.joining("; "));
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("isValid", isValid);
            map.put("errors", errors);
            map.put("errorCount", errors.size());
            map.put("errorSummary", getErrorSummary());
            return map;
        }
    }
}