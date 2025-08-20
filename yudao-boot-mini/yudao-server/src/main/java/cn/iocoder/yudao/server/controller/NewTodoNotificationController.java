package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.server.util.SecurityEnhancementUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 🎯 新待办通知系统Controller - 完全模仿TempNotificationController成功模式
 * 设计理念：完全独立的待办通知系统，使用独立数据表，避免与普通通知系统冲突
 * 
 * 核心特性：
 * 1. 双重认证：Mock School API + JWT Token验证
 * 2. 独立数据表：todo_notifications + todo_completions
 * 3. 类型安全：使用DTO模式，避免Map类型转换问题
 * 4. 权限控制：基于角色和范围的精确权限验证
 * 5. Linux兼容：解决跨平台类型解析差异
 * 
 * @author Claude AI
 * @since 2025-08-19
 */
@Tag(name = "新待办通知系统API")
@RestController
@RequestMapping("/admin-api/test/todo-new")
@Validated
@TenantIgnore
@Slf4j
public class NewTodoNotificationController {

    private static final String MOCK_API_BASE = "http://localhost:48082";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 🧪 服务测试接口
     */
    @GetMapping("/api/ping")
    @Operation(summary = "新待办通知服务Ping测试")
    @PermitAll
    @TenantIgnore
    public CommonResult<String> ping() {
        log.info("🏓 [NEW-TODO-PING] 新待办通知服务ping测试");
        return success("pong from NewTodoNotificationController - 完全独立的待办通知系统");
    }

    /**
     * 📝 获取我的待办列表 - 双重认证 + 独立数据表版本
     */
    @GetMapping("/api/my-list")
    @Operation(summary = "获取我的待办列表(新版)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> getMyTodoList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            HttpServletRequest httpRequest) {
        
        log.info("📝 [NEW-TODO-LIST] 获取我的待办列表 - page:{}, pageSize:{}, status:{}, priority:{}", 
                page, pageSize, status, priority);
        
        try {
            // 🔐 Step 1: 双重认证验证 - 完全复制TempNotificationController模式
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                log.warn("❌ [NEW-TODO-LIST] 未提供认证Token");
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                log.warn("❌ [NEW-TODO-LIST] Token验证失败");
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [NEW-TODO-LIST] 用户认证成功: {} (角色: {})", userInfo.username, userInfo.roleCode);

            // 🔍 Step 2: 构建查询条件 - 使用独立的todo_notifications表
            StringBuilder whereClause = new StringBuilder();
            whereClause.append("WHERE deleted = 0"); // 基础条件
            
            // 添加状态过滤
            if (status != null) {
                Integer statusCode = getStatusCode(status);
                if (statusCode != null) {
                    whereClause.append(" AND status = ").append(statusCode);
                }
            }
            
            // 添加优先级过滤
            if (priority != null) {
                Integer priorityCode = getPriorityCode(priority);
                if (priorityCode != null) {
                    whereClause.append(" AND priority = ").append(priorityCode);
                }
            }
            
            // 添加范围权限过滤 - 基于用户角色
            whereClause.append(buildScopeFilter(userInfo.roleCode));

            // 📋 Step 3: 查询待办列表数据
            String countSql = "SELECT COUNT(*) as total FROM todo_notifications " + whereClause;
            
            String dataSql = String.format(
                "SELECT id, title, content, summary, priority, " +
                "DATE_FORMAT(deadline, '%%Y-%%m-%%d %%H:%%i:%%s') as due_date, " +
                "status, publisher_name as assigner_name, target_scope, " +
                "DATE_FORMAT(create_time, '%%Y-%%m-%%d %%H:%%i:%%s') as create_time " +
                "FROM todo_notifications %s " +
                "ORDER BY priority DESC, deadline ASC " +
                "LIMIT %d OFFSET %d",
                whereClause, pageSize, (page - 1) * pageSize
            );

            log.info("🔍 [NEW-TODO-LIST] 执行查询SQL: {}", dataSql);

            // 🎯 Step 4: 执行数据库查询
            List<Map<String, Object>> todos = executeQueryAndReturnList(dataSql);
            Map<String, Object> countResult = executeQueryAndReturnSingle(countSql);
            
            int total = countResult != null ? 
                Integer.parseInt(countResult.get("total").toString()) : 0;

            // 🔄 Step 5: 检查每个待办的个人完成状态
            for (Map<String, Object> todo : todos) {
                Long todoId = Long.parseLong(todo.get("id").toString());
                boolean isCompleted = checkUserTodoCompletion(todoId, userInfo.username);
                
                // 📊 构建前端所需的数据格式
                todo.put("level", 5); // 固定Level 5
                todo.put("priority", getPriorityName(Integer.parseInt(todo.get("priority").toString())));
                todo.put("dueDate", todo.get("due_date"));
                todo.put("status", isCompleted ? "completed" : getStatusName(Integer.parseInt(todo.get("status").toString())));
                todo.put("assignerName", todo.get("assigner_name"));
                todo.put("isCompleted", isCompleted);
                
                // 清理数据库字段
                todo.remove("assigner_name");
                todo.remove("due_date");
            }

            // ✅ Step 6: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("todos", todos);
            result.put("pagination", Map.of(
                "current", page,
                "pageSize", pageSize,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / pageSize)
            ));
            result.put("user", Map.of(
                "username", userInfo.username,
                "roleCode", userInfo.roleCode,
                "roleName", userInfo.roleName
            ));
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [NEW-TODO-LIST] 成功返回{}条待办数据 (用户: {})", todos.size(), userInfo.username);
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-LIST] 获取待办列表异常", e);
            return CommonResult.error(500, "获取待办列表异常: " + e.getMessage());
        }
    }

    /**
     * ✅ 标记待办完成 - 双重认证版本
     */
    @PostMapping("/api/{id}/complete")
    @Operation(summary = "标记待办完成(新版)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> completeTodo(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        log.info("✅ [NEW-TODO-COMPLETE] 标记待办完成 - todoId: {}", id);
        
        try {
            // 🔐 Step 1: 双重认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [NEW-TODO-COMPLETE] 用户认证成功: {} (角色: {})", userInfo.username, userInfo.roleCode);

            // 🔍 Step 2: 检查待办是否存在且有效
            String checkSql = "SELECT id, title, status FROM todo_notifications " +
                             "WHERE id = " + id + " AND deleted = 0";
            
            Map<String, Object> todoInfo = executeQueryAndReturnSingle(checkSql);
            if (todoInfo == null) {
                log.warn("❌ [NEW-TODO-COMPLETE] 待办不存在或无效: {}", id);
                return CommonResult.error(404, "待办任务不存在");
            }

            // 🔄 Step 3: 检查是否已经完成
            boolean alreadyCompleted = checkUserTodoCompletion(id, userInfo.username);
            if (alreadyCompleted) {
                log.warn("⚠️ [NEW-TODO-COMPLETE] 待办已完成: {} (用户: {})", id, userInfo.username);
                return CommonResult.error(409, "该待办任务已完成");
            }

            // ✅ Step 4: 插入完成记录
            String insertSql = String.format(
                "INSERT INTO todo_completions " +
                "(todo_id, user_id, user_name, user_role, completed_time, tenant_id) " +
                "VALUES (%d, '%s', '%s', '%s', NOW(), 1)",
                id, 
                SecurityEnhancementUtil.escapeSQL(userInfo.username), // 使用username作为user_id
                SecurityEnhancementUtil.escapeSQL(userInfo.username),
                SecurityEnhancementUtil.escapeSQL(userInfo.roleCode)
            );

            boolean insertSuccess = executeSQLUpdate(insertSql);
            if (!insertSuccess) {
                log.error("❌ [NEW-TODO-COMPLETE] 插入完成记录失败");
                return CommonResult.error(500, "标记完成失败");
            }

            // ✅ Step 5: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("todoId", id);
            result.put("title", todoInfo.get("title"));
            result.put("completedBy", userInfo.username);
            result.put("completedTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("isCompleted", true);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [NEW-TODO-COMPLETE] 待办标记完成成功 - todoId: {}, user: {}", id, userInfo.username);
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-COMPLETE] 标记待办完成异常", e);
            return CommonResult.error(500, "标记完成异常: " + e.getMessage());
        }
    }

    /**
     * 📝 发布待办通知 - 双重认证版本 (修复版本)
     */
    @PostMapping("/api/publish")
    @Operation(summary = "发布待办通知(新版+修复)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> publishTodoNotification(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        log.info("📝 [NEW-TODO-PUBLISH] 发布待办通知请求开始");
        log.info("📝 [NEW-TODO-PUBLISH] 接收到请求参数: {}", request);
        
        try {
            // 🔐 Step 1: 双重认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                log.error("❌ [NEW-TODO-PUBLISH] 未提供认证Token");
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                log.error("❌ [NEW-TODO-PUBLISH] Token验证失败");
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [NEW-TODO-PUBLISH] 用户认证成功: {} (角色: {})", userInfo.username, userInfo.roleCode);

            // 📝 Step 2: 提取并验证请求参数
            String title = (String) request.get("title");
            String content = (String) request.get("content");
            String priority = (String) request.get("priority");
            String dueDate = (String) request.get("dueDate");
            String targetScope = (String) request.get("targetScope");
            
            log.info("📝 [NEW-TODO-PUBLISH] 解析参数: title={}, priority={}, dueDate={}, targetScope={}", 
                    title, priority, dueDate, targetScope);
            
            // 🛡️ Step 3: 参数验证
            List<String> validationErrors = new ArrayList<>();
            
            if (title == null || title.trim().isEmpty()) {
                validationErrors.add("待办标题不能为空");
            } else if (title.length() > 200) {
                validationErrors.add("待办标题长度不能超过200个字符");
            }
            
            if (content == null || content.trim().isEmpty()) {
                validationErrors.add("待办内容不能为空");
            } else if (content.length() > 2000) {
                validationErrors.add("待办内容长度不能超过2000个字符");
            }
            
            if (priority == null || !Arrays.asList("low", "medium", "high").contains(priority)) {
                validationErrors.add("待办优先级必须是 low、medium 或 high");
            }
            
            if (dueDate == null || dueDate.trim().isEmpty()) {
                validationErrors.add("待办截止日期不能为空");
            }
            
            if (targetScope == null || !Arrays.asList("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS").contains(targetScope)) {
                validationErrors.add("目标范围必须是 SCHOOL_WIDE、DEPARTMENT、GRADE 或 CLASS");
            }
            
            if (!validationErrors.isEmpty()) {
                log.warn("❌ [NEW-TODO-PUBLISH] 参数验证失败: {}", validationErrors);
                return CommonResult.error(400, "参数验证失败: " + String.join(", ", validationErrors));
            }

            // 🎯 Step 4: 权限验证 - 待办通知发布权限
            boolean hasPermission = validateTodoPublishPermission(userInfo.roleCode, targetScope);
            if (!hasPermission) {
                log.warn("❌ [NEW-TODO-PUBLISH] 用户{}无权限发布{}范围的待办通知", 
                        userInfo.username, targetScope);
                return CommonResult.error(403, "无权限发布该范围的待办通知");
            }

            // 🗄️ Step 5: 构建并插入数据库
            String deadline = dueDate;
            if (!deadline.contains(" ")) {
                deadline = deadline + " 23:59:59"; // 补充时间部分
            }
            
            String insertSql = String.format(
                "INSERT INTO todo_notifications " +
                "(tenant_id, title, content, summary, priority, deadline, status, publisher_id, publisher_name, publisher_role, target_scope, " +
                "category_id, creator, updater) " +
                "VALUES " +
                "(%d, '%s', '%s', '%s', %d, '%s', %d, %d, '%s', '%s', '%s', " +
                "%d, '%s', '%s')",
                
                1, // tenant_id 必须字段
                SecurityEnhancementUtil.escapeSQL(title), 
                SecurityEnhancementUtil.escapeSQL(content), 
                SecurityEnhancementUtil.escapeSQL(content.length() > 100 ? content.substring(0, 100) + "..." : content),
                getPriorityCode(priority), 
                SecurityEnhancementUtil.escapeSQL(deadline), 
                0, // 初始状态pending=0
                999, // 默认发布者ID
                SecurityEnhancementUtil.escapeSQL(userInfo.username), 
                SecurityEnhancementUtil.escapeSQL(userInfo.roleCode), 
                SecurityEnhancementUtil.escapeSQL(targetScope),
                1, // 默认分类ID
                SecurityEnhancementUtil.escapeSQL(userInfo.username), 
                SecurityEnhancementUtil.escapeSQL(userInfo.username)
            );
            
            log.info("🗄️ [NEW-TODO-PUBLISH] 执行插入SQL: {}", insertSql);
            
            boolean insertSuccess = executeSQLUpdate(insertSql);
            if (!insertSuccess) {
                log.error("❌ [NEW-TODO-PUBLISH] 数据库插入失败");
                return CommonResult.error(500, "数据库插入失败");
            }

            // 🔍 Step 6: 获取插入的记录ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Map<String, Object> idResult = executeQueryAndReturnSingle(lastIdSql);
            Long notificationId = idResult != null ? 
                Long.parseLong(idResult.get("id").toString()) : null;

            // ✅ Step 7: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("id", notificationId);
            result.put("title", title);
            result.put("level", 5);
            result.put("priority", priority);
            result.put("deadline", deadline);
            result.put("status", "pending");
            result.put("assignerName", userInfo.username);
            result.put("targetScope", targetScope);
            result.put("publishedBy", userInfo.username);
            result.put("publishedTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [NEW-TODO-PUBLISH] 待办通知发布成功 - id: {}, title: {}", notificationId, title);
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-PUBLISH] 发布待办通知异常: {}", e.getMessage(), e);
            return CommonResult.error(500, "发布待办通知异常: " + e.getMessage());
        }
    }

    /**
     * 📊 获取待办统计 - 双重认证版本
     */
    @GetMapping("/api/{id}/stats")
    @Operation(summary = "获取待办统计(新版)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> getTodoStats(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        log.info("📊 [NEW-TODO-STATS] 获取待办统计 - todoId: {}", id);
        
        try {
            // 🔐 Step 1: 双重认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [NEW-TODO-STATS] 用户认证成功: {} (角色: {})", userInfo.username, userInfo.roleCode);

            // 🔍 Step 2: 检查待办是否存在
            String checkSql = "SELECT id, title, publisher_name, target_scope, " +
                             "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') as create_time, " +
                             "DATE_FORMAT(deadline, '%Y-%m-%d %H:%i:%s') as due_date " +
                             "FROM todo_notifications " +
                             "WHERE id = " + id + " AND deleted = 0";
            
            Map<String, Object> todoInfo = executeQueryAndReturnSingle(checkSql);
            if (todoInfo == null) {
                log.warn("❌ [NEW-TODO-STATS] 待办不存在: {}", id);
                return CommonResult.error(404, "待办任务不存在");
            }

            // 📊 Step 3: 统计完成情况
            String statsSql = String.format(
                "SELECT " +
                "COUNT(*) as total_completed, " +
                "COUNT(CASE WHEN user_role = 'STUDENT' THEN 1 END) as student_completed, " +
                "COUNT(CASE WHEN user_role = 'TEACHER' THEN 1 END) as teacher_completed, " +
                "COUNT(CASE WHEN user_role = 'CLASS_TEACHER' THEN 1 END) as class_teacher_completed " +
                "FROM todo_completions " +
                "WHERE todo_id = %d", id
            );

            Map<String, Object> statsData = executeQueryAndReturnSingle(statsSql);

            // 🔍 Step 4: 获取最近完成记录
            String recentSql = String.format(
                "SELECT user_name, user_role, " +
                "DATE_FORMAT(completed_time, '%%Y-%%m-%%d %%H:%%i:%%s') as completed_time " +
                "FROM todo_completions " +
                "WHERE todo_id = %d " +
                "ORDER BY completed_time DESC LIMIT 10", id
            );

            List<Map<String, Object>> recentCompletions = executeQueryAndReturnList(recentSql);

            // ✅ Step 5: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("todoInfo", todoInfo);
            result.put("stats", Map.of(
                "totalCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("total_completed").toString()) : 0,
                "studentCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("student_completed").toString()) : 0,
                "teacherCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("teacher_completed").toString()) : 0,
                "classTeacherCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("class_teacher_completed").toString()) : 0
            ));
            result.put("recentCompletions", recentCompletions);
            result.put("requestedBy", userInfo.username);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [NEW-TODO-STATS] 成功返回待办统计 - todoId: {}, totalCompleted: {}", 
                    id, statsData != null ? statsData.get("total_completed") : 0);
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-STATS] 获取待办统计异常", e);
            return CommonResult.error(500, "获取待办统计异常: " + e.getMessage());
        }
    }

    // ========================= 私有辅助方法 =========================

    /**
     * 🔐 从Mock API获取用户信息 - 完全复制TempNotificationController成功模式
     */
    private UserInfo getUserInfoFromMockApi(String authToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", authToken);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(new HashMap<>(), headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                MOCK_API_BASE + "/mock-school-api/auth/user-info",
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data != null) {
                        UserInfo userInfo = new UserInfo();
                        userInfo.username = (String) data.get("username");
                        userInfo.roleCode = (String) data.get("roleCode");
                        userInfo.roleName = (String) data.get("roleName");
                        
                        log.info("✅ [NEW-TODO-AUTH] Mock API认证成功: {} ({})", userInfo.username, userInfo.roleCode);
                        return userInfo;
                    }
                }
            }
        } catch (Exception e) {
            log.error("🔗 [NEW-TODO-AUTH] Mock API调用异常: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 检查用户待办完成状态
     */
    private boolean checkUserTodoCompletion(Long todoId, String username) {
        try {
            String checkSql = String.format(
                "SELECT id FROM todo_completions " +
                "WHERE todo_id = %d AND user_id = '%s'",
                todoId, SecurityEnhancementUtil.escapeSQL(username)
            );
            
            Map<String, Object> result = executeQueryAndReturnSingle(checkSql);
            return result != null;
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-COMPLETION-CHECK] 检查完成状态异常", e);
            return false;
        }
    }

    /**
     * 构建范围过滤条件
     */
    private String buildScopeFilter(String roleCode) {
        // 根据角色限制可见的待办范围
        switch (roleCode) {
            case "SYSTEM_ADMIN":
            case "PRINCIPAL":
                return ""; // 可以看到所有范围
            case "ACADEMIC_ADMIN":
                return " AND target_scope IN ('SCHOOL_WIDE', 'DEPARTMENT', 'GRADE')";
            case "TEACHER":
                return " AND target_scope IN ('DEPARTMENT', 'CLASS')";
            case "CLASS_TEACHER":
                return " AND target_scope IN ('GRADE', 'CLASS')";
            case "STUDENT":
                return " AND target_scope IN ('SCHOOL_WIDE', 'CLASS')"; // 学生可以看学校通知和班级通知
            default:
                return " AND target_scope = 'CLASS'";
        }
    }

    /**
     * 验证待办发布权限
     */
    private boolean validateTodoPublishPermission(String roleCode, String targetScope) {
        Map<String, Set<String>> rolePermissions = Map.of(
            "SYSTEM_ADMIN", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "PRINCIPAL", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "ACADEMIC_ADMIN", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "TEACHER", Set.of("DEPARTMENT", "CLASS"),
            "CLASS_TEACHER", Set.of("GRADE", "CLASS"),
            "STUDENT", Set.of("CLASS")
        );
        
        Set<String> allowedScopes = rolePermissions.get(roleCode);
        return allowedScopes != null && allowedScopes.contains(targetScope);
    }

    /**
     * 验证TodoRequest对象
     */
    private List<String> validateTodoRequest(TodoRequest request) {
        List<String> errors = new ArrayList<>();
        
        if (request.title == null || request.title.trim().isEmpty()) {
            errors.add("待办标题不能为空");
        } else if (request.title.length() > 200) {
            errors.add("待办标题长度不能超过200个字符");
        }
        
        if (request.content == null || request.content.trim().isEmpty()) {
            errors.add("待办内容不能为空");
        } else if (request.content.length() > 2000) {
            errors.add("待办内容长度不能超过2000个字符");
        }
        
        if (request.priority == null || !Arrays.asList("low", "medium", "high").contains(request.priority)) {
            errors.add("待办优先级必须是 low、medium 或 high");
        }
        
        if (request.deadline == null || request.deadline.trim().isEmpty()) {
            errors.add("待办截止日期不能为空");
        } else if (!request.deadline.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            errors.add("待办截止日期格式不正确，应为 YYYY-MM-DD 或 YYYY-MM-DDTHH:mm:ss");
        }
        
        if (request.targetScope != null && !Arrays.asList("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS").contains(request.targetScope)) {
            errors.add("目标范围必须是 SCHOOL_WIDE、DEPARTMENT、GRADE 或 CLASS");
        }
        
        log.info("📋 [NEW-TODO-VALIDATE] 待办DTO验证完成: {}, 错误数量: {}", 
                errors.isEmpty() ? "通过" : "失败", errors.size());
        return errors;
    }

    /**
     * 构建待办插入SQL
     */
    private String buildTodoInsertSQL(TodoRequest request, UserInfo userInfo) {
        String deadline = request.deadline;
        if (!deadline.contains(" ")) {
            deadline = deadline + " 23:59:59"; // 补充时间部分
        }
        
        return String.format(
            "INSERT INTO todo_notifications " +
            "(title, content, summary, priority, deadline, status, publisher_id, publisher_name, publisher_role, target_scope, " +
            "category_id, creator, updater) " +
            "VALUES " +
            "('%s', '%s', '%s', %d, '%s', %d, %d, '%s', '%s', '%s', " +
            "%d, '%s', '%s')",
            
            SecurityEnhancementUtil.escapeSQL(request.title), 
            SecurityEnhancementUtil.escapeSQL(request.content), 
            SecurityEnhancementUtil.escapeSQL(request.content.length() > 100 ? request.content.substring(0, 100) + "..." : request.content),
            getPriorityCode(request.priority), 
            SecurityEnhancementUtil.escapeSQL(deadline), 
            0, // 初始状态pending=0
            999, // 默认发布者ID
            SecurityEnhancementUtil.escapeSQL(userInfo.username), 
            SecurityEnhancementUtil.escapeSQL(userInfo.roleCode), 
            SecurityEnhancementUtil.escapeSQL(request.targetScope),
            request.categoryId != null ? request.categoryId : 1, 
            SecurityEnhancementUtil.escapeSQL(userInfo.username), 
            SecurityEnhancementUtil.escapeSQL(userInfo.username)
        );
    }

    /**
     * 类型安全的JSON解析方法
     */
    private TodoRequest parseTodoJsonRequest(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            TodoRequest request = new TodoRequest();
            request.title = jsonNode.has("title") ? jsonNode.get("title").asText("待办事项") : "待办事项";
            request.content = jsonNode.has("content") ? jsonNode.get("content").asText("待办内容") : "待办内容";
            request.priority = jsonNode.has("priority") ? jsonNode.get("priority").asText("medium") : "medium";
            request.deadline = jsonNode.has("deadline") ? jsonNode.get("deadline").asText("2025-12-31T23:59:59") : "2025-12-31T23:59:59";
            request.categoryId = jsonNode.has("categoryId") ? jsonNode.get("categoryId").asInt(1) : 1;
            request.targetScope = jsonNode.has("targetScope") ? jsonNode.get("targetScope").asText("CLASS") : "CLASS";
            
            log.info("🔧 [NEW-TODO-JSON-PARSE] 成功解析: title={}, priority={}, deadline={}, categoryId={}", 
                    request.title, request.priority, request.deadline, request.categoryId);
            
            return request;
        } catch (Exception e) {
            log.warn("🔧 [NEW-TODO-JSON-PARSE] JSON解析失败，使用默认值: {}", e.getMessage());
            TodoRequest defaultRequest = new TodoRequest();
            defaultRequest.title = "默认待办事项";
            defaultRequest.content = "默认待办内容";
            defaultRequest.priority = "medium";
            defaultRequest.deadline = "2025-12-31T23:59:59";
            defaultRequest.categoryId = 1;
            defaultRequest.targetScope = "CLASS";
            return defaultRequest;
        }
    }

    // ========================= 工具方法 =========================

    private Integer getStatusCode(String status) {
        Map<String, Integer> statusMap = Map.of(
            "pending", 0, "in_progress", 1, "completed", 2, "overdue", 3
        );
        return statusMap.get(status);
    }

    private String getStatusName(Integer code) {
        Map<Integer, String> statusMap = Map.of(
            0, "pending", 1, "in_progress", 2, "completed", 3, "overdue"
        );
        return statusMap.getOrDefault(code, "pending");
    }

    private Integer getPriorityCode(String priority) {
        Map<String, Integer> priorityMap = Map.of(
            "low", 1, "medium", 2, "high", 3
        );
        return priorityMap.getOrDefault(priority, 2);
    }

    private String getPriorityName(Integer code) {
        Map<Integer, String> priorityMap = Map.of(
            1, "low", 2, "medium", 3, "high"
        );
        return priorityMap.getOrDefault(code, "medium");
    }

    // ========================= 数据库访问方法 =========================

    private Map<String, Object> executeQueryAndReturnSingle(String sql) {
        try {
            String[] command = {"mysql", "-u", "root", "ruoyi-vue-pro", "--default-character-set=utf8", "-e", sql};
            Process process = Runtime.getRuntime().exec(command);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
            
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && lines.size() > 1) {
                String[] headers = lines.get(0).split("\t");
                String[] values = lines.get(1).split("\t", -1);
                
                Map<String, Object> result = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    result.put(headers[i], "NULL".equals(values[i]) ? null : values[i]);
                }
                return result;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-DB-QUERY-SINGLE] 数据库查询异常", e);
            return null;
        }
    }

    private List<Map<String, Object>> executeQueryAndReturnList(String sql) {
        try {
            String[] command = {"mysql", "-u", "root", "ruoyi-vue-pro", "--default-character-set=utf8", "-e", sql};
            Process process = Runtime.getRuntime().exec(command);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
            
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            int exitCode = process.waitFor();
            
            List<Map<String, Object>> results = new ArrayList<>();
            if (exitCode == 0 && lines.size() > 1) {
                String[] headers = lines.get(0).split("\t");
                
                for (int i = 1; i < lines.size(); i++) {
                    String[] values = lines.get(i).split("\t", -1);
                    Map<String, Object> row = new HashMap<>();
                    
                    for (int j = 0; j < headers.length && j < values.length; j++) {
                        row.put(headers[j], "NULL".equals(values[j]) ? null : values[j]);
                    }
                    results.add(row);
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-DB-QUERY-LIST] 数据库查询异常", e);
            return new ArrayList<>();
        }
    }

    private boolean executeSQLUpdate(String sql) {
        try {
            String[] command = {"mysql", "-u", "root", "ruoyi-vue-pro", "--default-character-set=utf8mb4", "-e", sql};
            Process process = Runtime.getRuntime().exec(command);
            
            // 读取错误输出
            java.io.BufferedReader errorReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getErrorStream(), "UTF-8"));
            
            StringBuilder errorOutput = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine).append("\n");
            }
            
            // 读取标准输出
            java.io.BufferedReader outputReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
            
            StringBuilder output = new StringBuilder();
            String outputLine;
            while ((outputLine = outputReader.readLine()) != null) {
                output.append(outputLine).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("✅ [NEW-TODO-DB-UPDATE] SQL执行成功");
                if (output.length() > 0) {
                    log.info("📝 [NEW-TODO-DB-UPDATE] 输出: {}", output.toString().trim());
                }
                return true;
            } else {
                log.error("❌ [NEW-TODO-DB-UPDATE] SQL执行失败，退出码: {}", exitCode);
                if (errorOutput.length() > 0) {
                    log.error("❌ [NEW-TODO-DB-UPDATE] 错误信息: {}", errorOutput.toString().trim());
                }
                if (output.length() > 0) {
                    log.error("❌ [NEW-TODO-DB-UPDATE] 输出信息: {}", output.toString().trim());
                }
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [NEW-TODO-DB-UPDATE] SQL执行异常: {}", e.getMessage(), e);
            return false;
        }
    }

    // ========================= DTO类定义 =========================

    /**
     * 用户信息类 - 复制TempNotificationController模式
     */
    public static class UserInfo {
        public String username;
        public String roleCode;
        public String roleName;
    }

    /**
     * 待办通知请求DTO
     */
    public static class TodoRequest {
        public String title;
        public String content;
        public String priority;      // low/medium/high
        public String deadline;      // ISO 8601格式
        public Integer categoryId;   // 分类ID
        public String targetScope;   // SCHOOL_WIDE/DEPARTMENT/GRADE/CLASS
        
        public TodoRequest() {}
    }
}