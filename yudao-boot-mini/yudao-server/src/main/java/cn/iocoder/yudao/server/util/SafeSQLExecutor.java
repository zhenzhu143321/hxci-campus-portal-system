package cn.iocoder.yudao.server.util;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
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
    
    // SQL注入风险字符模式 - 只检测真正的注入原语和注释符号
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "(?is)(/\\*.*?\\*/|(?m)(^|\\s)(?:--|#)|\\bunion\\s+(?:all\\s+)?select\\b|\\binto\\s+(?:outfile|dumpfile)\\b|\\bload_file\\b|\\bsleep\\s*\\(|\\bbenchmark\\s*\\(|\\bxp_\\w+\\b|\\bexec(?:ute)?\\b|\\binformation_schema\\b)"
    );

    // Markdown内容安全模式 - 移除#注释检测，保留真正的SQL注入检测，修复---误判问题
    private static final Pattern DANGEROUS_PATTERN_FOR_CONTENT = Pattern.compile(
        "(?is)(/\\*.*?\\*/|(?m)(^|\\s)--(?!-)|\\bunion\\s+(?:all\\s+)?select\\b|\\binto\\s+(?:outfile|dumpfile)\\b|\\bload_file\\b|\\bsleep\\s*\\(|\\bbenchmark\\s*\\(|\\bxp_\\w+\\b|\\bexec(?:ute)?\\b|\\binformation_schema\\b)"
    );

    // Markdown语法过滤模式 - 在安全检测前移除这些内容
    // Markdown代码块: ```...```
    private static final Pattern MD_FENCED_CODE_BLOCK = Pattern.compile("(?s)```.*?```\\n?");
    // Markdown行内代码: `...`
    private static final Pattern MD_INLINE_CODE_SPAN = Pattern.compile("`[^`]*`");
    // Markdown表格分隔线: | --- | :---: |
    private static final Pattern MD_TABLE_SEPARATOR_LINE = Pattern.compile("(?m)^\\s*\\|?(\\s*:?-{2,}:?\\s*\\|)+\\s*$");
    
    // 用于检测SQL子句边界的模式
    private static final Pattern CLAUSE_BOUNDARY = Pattern.compile(
        "(?i)\\b(GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT|OFFSET|FETCH|FOR\\s+UPDATE)\\b"
    );
    
    // 预编译的WHERE模式 - 性能优化
    private static final Pattern WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b");
    
    // 用于验证安全条件的模式 - 修复支持标准SQL条件如"deleted = 0"
    private static final Pattern SAFE_CONDITION_PATTERN = Pattern.compile(
        "^[a-zA-Z_][a-zA-Z0-9_\\.]*\\s*(=|<|>|<=|>=|<>|!=|\\s+IN\\s+|\\s+LIKE\\s+|\\s+BETWEEN\\s+|\\s+IS\\s+(?:NOT\\s+)?NULL)\\s*[a-zA-Z0-9_\\.'\"\\s,\\(\\)]+$"
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
            
            // 🔐 输入验证和清理 - Markdown友好模式用于title和content
            this.safeTitle = sanitizeMarkdownInput(title, MAX_STRING_LENGTH, "title");
            this.safeContent = sanitizeMarkdownInput(content, MAX_CONTENT_LENGTH, "content");
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
     * 🔐 Markdown内容安全清理 - 支持Markdown语法但防止SQL注入
     * 专用于title和content字段，允许#、*、**、_、-等Markdown字符
     */
    private static String sanitizeMarkdownInput(String input, int maxLength, String fieldName) {
        if (input == null) {
            log.warn("🔐 [MARKDOWN-SANITIZE] 字段 {} 为null，使用空字符串", fieldName);
            return "";
        }

        // 长度检查
        if (input.length() > maxLength) {
            log.warn("🔐 [MARKDOWN-SANITIZE] 字段 {} 长度超限: {} > {}, 截断处理", fieldName, input.length(), maxLength);
            input = input.substring(0, maxLength);
        }

        // 检测并拒绝二进制控制字符（除了CR/LF/TAB）
        if (Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]").matcher(input).find()) {
            log.error("🚨 [SECURITY] 检测到控制字符: 字段={}", fieldName);
            throw new SecurityException("Control characters detected in field: " + fieldName);
        }

        // 先移除Markdown语法元素，再进行SQL注入检测
        String scanText = MD_FENCED_CODE_BLOCK.matcher(input).replaceAll("");
        scanText = MD_INLINE_CODE_SPAN.matcher(scanText).replaceAll("");
        scanText = MD_TABLE_SEPARATOR_LINE.matcher(scanText).replaceAll("");

        // 使用Markdown友好的SQL注入检测（移除#注释检测，且避免匹配'---'为注释）
        if (DANGEROUS_PATTERN_FOR_CONTENT.matcher(scanText).find()) {
            log.error("🚨 [SECURITY] SQL注入风险检测(Markdown模式): 字段={}, 扫描文本={}", fieldName, scanText);
            throw new SecurityException("Potential SQL injection detected in field: " + fieldName);
        }

        // SQL转义 - 单引号处理
        String escaped = input.replace("'", "''");

        // 其他危险字符转义
        escaped = escaped.replace("\\", "\\\\");

        if (!input.equals(escaped)) {
            log.info("🔐 [MARKDOWN-SANITIZE] 字段 {} 已转义: {} -> {}", fieldName, input, escaped);
        }

        return escaped;
    }

    /**
     * 🔐 查找SQL中第一个子句边界的位置（已被findTopLevelBoundary替代）
     * @deprecated 使用 findTopLevelBoundary 以正确处理子查询
     */
    @Deprecated
    private static int findFirstBoundary(String upperSql) {
        Matcher m = CLAUSE_BOUNDARY.matcher(upperSql);
        return m.find() ? m.start() : -1;
    }
    
    /**
     * 🔐 检查SQL是否已包含WHERE子句 - 使用预编译模式
     */
    private static boolean hasWhere(String upperSql) {
        return WHERE_PATTERN.matcher(upperSql).find();
    }
    
    /**
     * 🔐 安全地追加条件：在正确的位置注入WHERE或AND
     * 将条件放置在ORDER BY/GROUP BY/HAVING/LIMIT/FOR UPDATE之前
     */
    public static String appendCondition(String sql, String condition) {
        if (sql == null || condition == null || condition.trim().isEmpty()) {
            return sql;
        }
        
        // 验证条件是否安全
        String trimmedCondition = condition.trim();
        if (!isValidCondition(trimmedCondition)) {
            log.error("🚨 [SECURITY] 检测到不安全的SQL条件: {}", trimmedCondition);
            throw new SecurityException("Invalid SQL condition format");
        }
        
        String trimmed = sql.trim();
        // 移除尾部分号
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        
        String upper = trimmed.toUpperCase(Locale.ROOT);
        int boundaryPos = findTopLevelBoundary(trimmed);
        boolean whereExists = hasWhere(upper);

        String injector = whereExists ? " AND " : " WHERE ";
        String toInsert = injector + "(" + trimmedCondition + ")";

        if (boundaryPos >= 0) {
            return trimmed.substring(0, boundaryPos) + toInsert + " " + trimmed.substring(boundaryPos);
        } else {
            return trimmed + toInsert;
        }
    }
    
    /**
     * 🔐 验证条件是否安全
     */
    private static boolean isValidCondition(String condition) {
        // 基础验证：不允许分号、注释、UNION等危险关键字（使用单词边界匹配避免误伤）
        String upperCondition = condition.toUpperCase();
        if (condition.contains(";") || 
            condition.contains("--") || 
            condition.contains("/*") || 
            condition.contains("*/") ||
            upperCondition.matches(".*\\bUNION\\b.*") ||
            upperCondition.matches(".*\\bSELECT\\b.*") ||
            upperCondition.matches(".*\\bINSERT\\b.*") ||
            upperCondition.matches(".*\\bUPDATE\\b.*") ||
            upperCondition.matches(".*\\bDELETE\\b.*") ||
            upperCondition.matches(".*\\bDROP\\b.*")) {
            return false;
        }
        
        // 验证基本格式：列名 操作符 值
        return SAFE_CONDITION_PATTERN.matcher(condition).matches();
    }
    
    /**
     * 🔐 查找顶层SQL子句边界（忽略子查询）
     */
    private static int findTopLevelBoundary(String sql) {
        int parenLevel = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        String upper = sql.toUpperCase(Locale.ROOT);
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            // 处理字符串
            if (c == '\'' && !inDoubleQuote && (i == 0 || sql.charAt(i-1) != '\\')) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '\"' && !inSingleQuote && (i == 0 || sql.charAt(i-1) != '\\')) {
                inDoubleQuote = !inDoubleQuote;
            }
            
            // 跳过字符串内容
            if (inSingleQuote || inDoubleQuote) continue;
            
            // 处理括号
            if (c == '(') parenLevel++;
            else if (c == ')') parenLevel--;
            
            // 只在顶层查找边界
            if (parenLevel == 0) {
                // 检查是否匹配边界关键字
                Matcher m = CLAUSE_BOUNDARY.matcher(upper.substring(i));
                if (m.find() && m.start() == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }

    /**
     * 🔐 确保软删除保护
     */
    public static String ensureNotDeleted(String sql) {
        return appendCondition(sql, "deleted = 0");
    }

    /**
     * 🔐 验证SQL语句安全性 - 改进版
     */
    public static boolean isSecureSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        // 以语法感知的方式分割语句：忽略字符串/注释中的分号
        List<String> statements = splitSqlStatementsRespectingLiterals(sql);

        // 只统计看起来是SQL语句的片段，避免Markdown文本中的分号被误算
        List<String> sqlStatements = new ArrayList<>();
        for (String st : statements) {
            String s = st.trim();
            if (s.isEmpty()) continue;
            if (isLikelySqlStatementStart(s)) {
                sqlStatements.add(s);
            }
        }

        if (sqlStatements.size() > 3) { // 允许DML + SELECT LAST_INSERT_ID()模式
            log.warn("🚨 [SECURITY] SQL包含过多语句: {}", sqlStatements.size());
            return false;
        }

        for (String s : sqlStatements) {
            String upper = s.toUpperCase(Locale.ROOT);

            // 阻止DDL和导出操作
            if (upper.startsWith("DROP ") || upper.startsWith("TRUNCATE ") || upper.contains(" INTO OUTFILE")) {
                log.error("🚨 [SECURITY] 检测到危险SQL操作: {}", s);
                return false;
            }

            // DELETE和UPDATE需要WHERE或LIMIT - 使用正则匹配
            if (upper.startsWith("DELETE ")) {
                boolean hasWhere = WHERE_PATTERN.matcher(upper).find();
                boolean hasLimit = Pattern.compile("\\bLIMIT\\b", Pattern.CASE_INSENSITIVE).matcher(upper).find();
                if (!hasWhere && !hasLimit) {
                    log.warn("🚨 [SECURITY] DELETE缺少WHERE或LIMIT: {}", s);
                    return false;
                }
            }
            if (upper.startsWith("UPDATE ")) {
                boolean hasWhere = WHERE_PATTERN.matcher(upper).find();
                if (!hasWhere) {
                    log.warn("🚨 [SECURITY] UPDATE缺少WHERE: {}", s);
                    return false;
                }
            }
        }
        // 若未检测到可疑SQL或违规，视为安全
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
    
    /**
     * 🔐 构建安全的ORDER BY子句
     * - 支持逗号分隔："col1 asc, col2 desc"
     * - 每个列必须在白名单中；方向必须是ASC/DESC
     * - 拒绝函数、子查询、表达式
     * 如果没有有效排序则返回空字符串
     */
    public static String buildSafeOrderBy(String orderByRaw, Collection<String> allowedColumns, String defaultOrder) {
        if (orderByRaw == null || orderByRaw.trim().isEmpty()) {
            return (defaultOrder != null && !defaultOrder.isEmpty()) ? " ORDER BY " + defaultOrder : "";
        }
        if (allowedColumns == null || allowedColumns.isEmpty()) {
            log.warn("🔐 [ORDER-BY] 未提供白名单，忽略排序");
            return (defaultOrder != null && !defaultOrder.isEmpty()) ? " ORDER BY " + defaultOrder : "";
        }

        Set<String> whitelist = new LinkedHashSet<>();
        for (String c : allowedColumns) {
            if (c != null) whitelist.add(c.trim());
        }

        String[] parts = orderByRaw.split(",");
        List<String> safeParts = new ArrayList<>();
        Pattern token = Pattern.compile("(?i)^[a-zA-Z0-9_\\.]+(?:\\s+(ASC|DESC))?$");

        for (String p : parts) {
            String item = p.trim();
            if (item.isEmpty()) continue;
            if (!token.matcher(item).matches()) {
                log.warn("🔐 [ORDER-BY] 非法排序片段，已忽略: {}", item);
                continue;
            }

            // 分离列名和方向
            String[] seg = item.split("\\s+");
            String col = seg[0];
            String dir = (seg.length > 1) ? seg[1].toUpperCase(Locale.ROOT) : "ASC";
            if (!"ASC".equals(dir) && !"DESC".equals(dir)) {
                dir = "ASC";
            }

            // 白名单检查：大小写不敏感比较
            String colLower = col.toLowerCase(Locale.ROOT);
            boolean found = false;
            String matchedCol = col;
            
            for (String whiteCol : whitelist) {
                if (whiteCol.toLowerCase(Locale.ROOT).equals(colLower)) {
                    found = true;
                    matchedCol = whiteCol; // 使用白名单中的形式
                    break;
                }
            }
            
            if (!found && col.contains(".")) {
                // 尝试非限定匹配
                String unqualified = col.substring(col.lastIndexOf('.') + 1);
                String unqualifiedLower = unqualified.toLowerCase(Locale.ROOT);
                for (String whiteCol : whitelist) {
                    if (whiteCol.toLowerCase(Locale.ROOT).equals(unqualifiedLower)) {
                        found = true;
                        matchedCol = whiteCol;
                        break;
                    }
                }
            }
            
            if (!found) {
                log.warn("🔐 [ORDER-BY] 不在白名单，已忽略: {}", col);
                continue;
            }
            
            col = matchedCol;

            safeParts.add(col + " " + dir);
        }

        if (safeParts.isEmpty()) {
            return (defaultOrder != null && !defaultOrder.isEmpty()) ? " ORDER BY " + defaultOrder : "";
        }
        return " ORDER BY " + String.join(", ", safeParts);
    }

    /**
     * 🔐 将SQL按顶层分号拆分，忽略字符串与注释内的分号
     */
    private static List<String> splitSqlStatementsRespectingLiterals(String sql) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingle = false;
        boolean inDouble = false;
        boolean inLineComment = false;   // -- ... 到行尾
        boolean inHashComment = false;   // # ... 到行尾（用于拆分，不作为危险判定）
        boolean inBlockComment = false;  // /* ... */

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';
            char next2 = (i + 2 < sql.length()) ? sql.charAt(i + 2) : '\0';

            // 行注释结束
            if (inLineComment || inHashComment) {
                current.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                    inHashComment = false;
                }
                continue;
            }

            // 块注释结束
            if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append('/');
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            // 进入注释（不在字符串中）
            if (!inSingle && !inDouble) {
                // -- 注释：要求后面跟空白/换行/结束，避免误命中 Markdown '---'
                if (c == '-' && next == '-' && (next2 == ' ' || next2 == '\t' || next2 == '\r' || next2 == '\n' || next2 == '\0')) {
                    current.append(c).append(next);
                    i++;
                    inLineComment = true;
                    continue;
                }
                // # 行注释
                if (c == '#') {
                    current.append(c);
                    inHashComment = true;
                    continue;
                }
                // 块注释
                if (c == '/' && next == '*') {
                    current.append(c).append(next);
                    i++;
                    inBlockComment = true;
                    continue;
                }
            }

            // 处理字符串与转义
            if (!inDouble && c == '\'') {
                current.append(c);
                if (inSingle) {
                    // '' 转义
                    if (next == '\'') {
                        current.append(next);
                        i++;
                    } else {
                        inSingle = false;
                    }
                } else {
                    inSingle = true;
                }
                continue;
            }
            if (!inSingle && c == '\"') {
                current.append(c);
                if (inDouble) {
                    // "" 转义
                    if (next == '\"') {
                        current.append(next);
                        i++;
                    } else {
                        inDouble = false;
                    }
                } else {
                    inDouble = true;
                }
                continue;
            }
            // 处理反斜杠转义
            if ((inSingle || inDouble) && c == '\\' && next != '\0') {
                current.append(c).append(next);
                i++;
                continue;
            }

            // 顶层分号 -> 分割
            if (!inSingle && !inDouble && c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    parts.add(stmt);
                }
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            parts.add(tail);
        }
        return parts;
    }

    /**
     * 🔐 判断片段是否像SQL语句开头，仅对这些片段计数和校验
     */
    private static boolean isLikelySqlStatementStart(String s) {
        String up = s.trim().toUpperCase(Locale.ROOT);
        return up.matches("^(WITH|SELECT|INSERT|UPDATE|DELETE|REPLACE|MERGE|CALL|DO|CREATE|ALTER|DROP|TRUNCATE)\\b.*");
    }
}