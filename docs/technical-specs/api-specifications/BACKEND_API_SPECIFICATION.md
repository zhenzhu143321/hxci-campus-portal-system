# AI智能通知推送系统 - 后端API规格说明书

## 📋 项目概述

### 系统架构基础
- **核心框架**: yudao-boot-mini (Spring Boot 3.4.5 + Java 17)
- **数据库**: MySQL 8.0 + MyBatis Plus
- **缓存**: Redis集群 + 本地缓存
- **消息队列**: Kafka + Redis Streams
- **工作流**: Flowable引擎
- **安全认证**: Spring Security + JWT
- **多租户**: 完全数据隔离架构

### 核心业务复杂度
- **角色权限**: 25+角色的6层级权限体系
- **通知分类**: 紧急/重要/常规/提醒四级分类
- **推送渠道**: APP/短信/邮件/站内信多渠道
- **高并发**: 支持10万+用户同时推送
- **实时性**: 推送延迟 < 5秒

---

## 🏗️ 模块化开发规划

### Phase 1: 基础模块开发 (2-3周)

#### 1.1 系统管理模块 (yudao-module-system)
**开发优先级**: 最高
**核心业务**: 用户权限管理、租户管理、基础配置
**关键技术挑战**: 25+角色权限的高效查询和缓存

```java
// 核心权限查询优化
@Service
public class PermissionService {
    
    @Cacheable(value = "user:permissions", key = "#userId")
    public List<Permission> getUserPermissions(Long userId) {
        // 多级缓存: L1本地 -> L2Redis -> L3数据库
        return permissionRepository.getUserPermissionsWithCache(userId);
    }
    
    // 权限树形结构构建
    public PermissionTree buildPermissionTree(Long userId) {
        List<Permission> permissions = getUserPermissions(userId);
        return PermissionTreeBuilder.build(permissions);
    }
}
```

#### 1.2 通知核心模块 (yudao-module-notification)
**开发优先级**: 最高
**核心业务**: 通知管理、模板管理、分类管理、生命周期管理
**关键技术挑战**: 四级通知分类的业务规则引擎

```java
// 通知分类业务规则引擎
@Component
public class NotificationRuleEngine {
    
    public NotificationRule getPublishRule(UserRole role, NotificationType type) {
        return ruleMatrix.get(role.getCode() + "_" + type.getCode());
    }
    
    // 权限验证
    public boolean canPublish(Long userId, NotificationCreateRequest request) {
        UserRole role = userService.getUserRole(userId);
        NotificationRule rule = getPublishRule(role, request.getType());
        return rule.isAllowed() && 
               rule.getTargetScope().contains(request.getTargetType());
    }
}
```

### Phase 2: 核心功能开发 (4-5周)

#### 2.1 推送引擎模块 (yudao-module-push)
**开发优先级**: 高
**核心业务**: 多渠道推送、批量处理、状态管理
**关键技术挑战**: 10万+用户并发推送的性能优化

```java
// 高并发推送引擎设计
@Service
public class BatchPushEngine {
    
    @Async("pushTaskExecutor")
    public CompletableFuture<PushResult> batchPush(PushTask task) {
        // 1. 用户分片处理 (每组1000用户)
        List<List<Long>> userGroups = partitionUsers(task.getTargetUsers(), 1000);
        
        // 2. 并行推送
        List<CompletableFuture<Void>> futures = userGroups.stream()
            .map(group -> CompletableFuture.runAsync(() -> 
                pushToUserGroup(task, group), pushExecutor))
            .collect(Collectors.toList());
            
        // 3. 结果汇聚
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> aggregateResults(task));
    }
}
```

#### 2.2 工作流审批模块 (yudao-module-workflow)
**开发优先级**: 高
**核心业务**: 流程定义、审批管理、任务处理
**关键技术挑战**: 复杂审批流程的性能优化

### Phase 3: 高级功能开发 (3-4周)

#### 3.1 统计监控模块 (yudao-module-statistics)
**开发优先级**: 中高
**核心业务**: 实时统计、报表生成、预警机制

#### 3.2 集成服务模块 (yudao-module-integration)
**开发优先级**: 中
**核心业务**: 第三方推送服务、短信邮件集成

---

## 🔧 API接口设计规范

### 1. REST API设计标准

#### 1.1 URL设计规范
```
基础路径: /admin-api/v1
租户隔离: Header中的tenant-id参数

资源命名规范:
- 通知管理: /admin-api/v1/notifications
- 用户管理: /admin-api/v1/users  
- 角色权限: /admin-api/v1/roles
- 推送任务: /admin-api/v1/push-tasks
- 统计报表: /admin-api/v1/statistics
```

#### 1.2 HTTP方法标准
```http
GET    /notifications        - 查询通知列表
POST   /notifications        - 创建新通知
GET    /notifications/{id}   - 获取通知详情
PUT    /notifications/{id}   - 更新通知信息
DELETE /notifications/{id}   - 删除通知(软删除)
PATCH  /notifications/{id}/status - 更新通知状态
```

#### 1.3 统一响应格式
```json
{
  "code": 200,
  "message": "success", 
  "data": {},
  "timestamp": "2024-07-25T10:30:00Z",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. 核心API接口详细设计

#### 2.1 用户权限管理API

##### GET /admin-api/v1/users/permissions/{userId}
**功能**: 获取用户完整权限信息
**权限**: 管理员权限或本人权限
**性能要求**: 响应时间 < 500ms

```json
{
  "code": 200,
  "data": {
    "userId": 1001,
    "username": "counselor001", 
    "roles": [
      {
        "id": 42,
        "code": "counselor",
        "name": "辅导员",
        "level": 5,
        "dataScope": "class",
        "permissions": [
          {
            "resource": "notification",
            "actions": ["create", "read", "update"],
            "constraints": {
              "notificationTypes": ["normal", "reminder"],
              "targetTypes": ["class"]
            }
          }
        ]
      }
    ],
    "dataPermissions": {
      "departments": [101, 102],
      "classes": [201, 202, 203]
    },
    "cacheTime": "2024-07-25T10:30:00Z",
    "expireTime": "2024-07-25T11:30:00Z"
  }
}
```

##### POST /admin-api/v1/auth/permission-check
**功能**: 批量权限验证接口
**应用场景**: 前端菜单显示、按钮权限控制

```json
// Request
{
  "userId": 1001,
  "permissions": [
    {
      "resource": "notification", 
      "action": "create",
      "context": {
        "notificationType": "urgent",
        "targetType": "all_users"
      }
    },
    {
      "resource": "statistics",
      "action": "read", 
      "context": {
        "scope": "college"
      }
    }
  ]
}

// Response
{
  "code": 200,
  "data": {
    "results": [
      {
        "resource": "notification",
        "action": "create", 
        "allowed": false,
        "reason": "insufficient_role_level"
      },
      {
        "resource": "statistics",
        "action": "read",
        "allowed": true
      }
    ]
  }
}
```

#### 2.2 通知管理API

##### POST /admin-api/v1/notifications
**功能**: 创建新通知
**业务规则**: 根据用户角色自动判断是否需要审批

```json
// Request
{
  "title": "期末考试安排通知",
  "content": "详细考试安排内容...",
  "contentHtml": "<h1>期末考试安排通知</h1><p>详细内容...</p>",
  "type": "important",
  "priority": 8,
  "categoryId": 1001,
  "templateId": 2001,
  "targetType": "class",
  "targetConfig": {
    "classIds": [201, 202],
    "userIds": [],
    "roleIds": [],
    "deptIds": []
  },
  "pushChannels": ["app", "site", "email"],
  "confirmRequired": true,
  "autoConfirmTime": 24,
  "scheduledTime": "2024-07-26T09:00:00Z",
  "expireTime": "2024-08-01T23:59:59Z", 
  "attachments": [
    {
      "name": "考试安排表.xlsx",
      "url": "https://storage.example.com/files/exam_schedule.xlsx",
      "size": 102400
    }
  ],
  "tags": ["期末考试", "教务"]
}

// Response  
{
  "code": 200,
  "message": "通知创建成功",
  "data": {
    "id": 10001,
    "status": "pending_approval", // 或 "published" 如果无需审批
    "workflowInstanceId": 20001, // 如果需要审批
    "estimatedTargetCount": 156,
    "createTime": "2024-07-25T10:30:00Z"
  }
}
```

##### GET /admin-api/v1/notifications
**功能**: 分页查询通知列表
**性能优化**: 索引优化、分页优化、缓存策略

```http
GET /admin-api/v1/notifications?page=1&size=20&type=important&status=published&publisherId=1001&startTime=2024-07-01&endTime=2024-07-31&keyword=考试

Response:
```
```json
{
  "code": 200,
  "data": {
    "total": 156,
    "pages": 8, 
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 10001,
        "title": "期末考试安排通知",
        "type": "important",
        "typeLabel": "重要通知",
        "priority": 8,
        "status": "published",
        "statusLabel": "已发布",
        "publisherId": 1001,
        "publisherName": "张教务员",
        "publisherDept": "教务处",
        "targetCount": 156,
        "pushCount": 156,
        "readCount": 142,
        "confirmCount": 89,
        "readRate": 91.03,
        "confirmRate": 57.05,
        "publishTime": "2024-07-25T09:00:00Z",
        "createTime": "2024-07-25T08:45:00Z",
        "expireTime": "2024-08-01T23:59:59Z"
      }
      // ... 更多记录
    ]
  }
}
```

##### GET /admin-api/v1/notifications/{id}/statistics
**功能**: 获取通知详细统计信息
**实时性**: 数据实时更新，缓存5分钟

```json
{
  "code": 200,
  "data": {
    "notificationId": 10001,
    "overview": {
      "targetCount": 156,
      "pushCount": 156, 
      "successCount": 154,
      "failedCount": 2,
      "readCount": 142,
      "confirmCount": 89,
      "pushSuccessRate": 98.72,
      "readRate": 91.03,
      "confirmRate": 57.05
    },
    "channelStats": [
      {
        "channel": "app",
        "pushCount": 156,
        "successCount": 155,
        "readCount": 128,
        "successRate": 99.36,
        "readRate": 82.58
      },
      {
        "channel": "email", 
        "pushCount": 156,
        "successCount": 142,
        "readCount": 67,
        "successRate": 91.03,
        "readRate": 47.18
      }
    ],
    "timelineStats": {
      "pushCompleteTime": "2024-07-25T09:05:00Z",
      "first50PercentReadTime": "2024-07-25T09:30:00Z",
      "first80PercentReadTime": "2024-07-25T12:15:00Z",
      "avgReadDelay": 1800, // 秒
      "avgConfirmDelay": 3600
    },
    "demographicStats": {
      "byClass": [
        {
          "classId": 201,
          "className": "软件工程2021-1班",
          "targetCount": 78,
          "readCount": 74,
          "confirmCount": 45,
          "readRate": 94.87
        }
      ],
      "byGrade": [
        {
          "grade": "2021级",
          "targetCount": 156,
          "readCount": 142,
          "readRate": 91.03
        }
      ]
    },
    "unreadUsers": [
      {
        "userId": 2001,
        "username": "student001", 
        "realName": "张小明",
        "className": "软件工程2021-1班",
        "mobile": "138****1234",
        "lastActiveTime": "2024-07-24T18:30:00Z"
      }
      // ... 未读用户列表
    ]
  }
}
```

#### 2.3 推送任务API

##### POST /admin-api/v1/push-tasks
**功能**: 创建推送任务
**应用场景**: 大批量推送、定时推送、重试推送

```json
// Request
{
  "notificationId": 10001,
  "taskType": "immediate", // immediate, scheduled, retry
  "targetUsers": [2001, 2002, 2003, ...], // 最大100000个用户ID
  "pushChannels": ["app", "sms"],
  "priority": 8,
  "scheduledTime": null, // 立即推送为null
  "maxRetryCount": 3,
  "batchSize": 1000 // 批次大小
}

// Response
{
  "code": 200,
  "data": {
    "taskId": 30001,
    "estimatedDuration": 300, // 预估完成时间(秒)
    "batchCount": 100, // 分批数量
    "status": "running"
  }
}
```

##### GET /admin-api/v1/push-tasks/{id}/progress
**功能**: 查询推送任务进度
**实时性**: WebSocket或Server-Sent Events推送进度

```json
{
  "code": 200,
  "data": {
    "taskId": 30001,
    "status": "running", // pending, running, completed, failed, cancelled
    "progress": 68.5, // 完成百分比
    "processedCount": 68500,
    "totalCount": 100000,
    "successCount": 67892,
    "failedCount": 608,
    "startTime": "2024-07-25T10:00:00Z",
    "estimatedEndTime": "2024-07-25T10:04:30Z",
    "currentBatch": 69,
    "totalBatches": 100,
    "throughput": 1250, // 每秒处理数量
    "errors": [
      {
        "userId": 2001,
        "channel": "sms", 
        "errorCode": "INSUFFICIENT_BALANCE",
        "errorMessage": "短信余额不足",
        "retryCount": 2
      }
    ]
  }
}
```

#### 2.4 工作流审批API

##### GET /admin-api/v1/workflow/tasks/my-pending
**功能**: 获取待我审批的任务列表

```json
{
  "code": 200,
  "data": {
    "total": 5,
    "records": [
      {
        "taskId": 40001,
        "instanceId": 20001,
        "notificationId": 10001,
        "title": "期末考试安排通知",
        "applicantName": "张辅导员",
        "applicantDept": "软件学院",
        "taskName": "年级主任审批",
        "priority": 8,
        "dueDate": "2024-07-26T18:00:00Z",
        "createTime": "2024-07-25T10:30:00Z",
        "formVariables": {
          "notificationType": "important",
          "targetCount": 156,
          "reason": "期末考试时间安排"
        }
      }
    ]
  }
}
```

##### POST /admin-api/v1/workflow/tasks/{id}/approve
**功能**: 审批任务

```json
// Request
{
  "result": "approve", // approve, reject, transfer
  "reason": "同意发布，内容准确",
  "transferTo": null, // 转办时需要指定用户ID
  "variables": {
    "approvalLevel": "grade_director",
    "nextApprover": null
  }
}

// Response
{
  "code": 200,
  "data": {
    "taskId": 40001,
    "result": "approved",
    "nextTask": null, // 如果有下一步审批节点
    "processCompleted": true,
    "notificationStatus": "approved" // 审批完成后通知状态
  }
}
```

---

## 🚀 核心算法与业务逻辑实现

### 1. 复杂权限模型的高效查询算法

#### 1.1 权限缓存策略
```java
@Service
public class AdvancedPermissionService {
    
    // 三级缓存架构
    @Autowired
    private CacheManager localCacheManager;   // L1: 本地缓存
    @Autowired
    private RedisTemplate redisTemplate;      // L2: Redis缓存  
    @Autowired
    private PermissionRepository repository;  // L3: 数据库
    
    public UserPermissionContext getUserPermissions(Long userId) {
        String cacheKey = "user:permissions:" + userId;
        
        // L1缓存查询
        UserPermissionContext cached = localCacheManager.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        
        // L2缓存查询
        cached = (UserPermissionContext) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            localCacheManager.put(cacheKey, cached, 30, TimeUnit.MINUTES);
            return cached;
        }
        
        // L3数据库查询
        UserPermissionContext permissions = buildPermissionContext(userId);
        
        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, permissions, 1, TimeUnit.HOURS);
        localCacheManager.put(cacheKey, permissions, 30, TimeUnit.MINUTES);
        
        return permissions;
    }
    
    private UserPermissionContext buildPermissionContext(Long userId) {
        // 1. 获取用户角色
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        
        // 2. 构建权限树
        Set<Permission> allPermissions = new HashSet<>();
        Map<String, DataScope> dataScopes = new HashMap<>();
        
        for (UserRole role : roles) {
            // 获取角色权限
            List<Permission> rolePermissions = rolePermissionRepository
                .findByRoleId(role.getRoleId());
            allPermissions.addAll(rolePermissions);
            
            // 构建数据权限范围
            DataScope dataScope = buildDataScope(role);
            dataScopes.put(role.getRoleCode(), dataScope);
        }
        
        // 3. 权限去重和继承处理
        PermissionTree permissionTree = PermissionTreeBuilder
            .buildWithInheritance(allPermissions);
            
        return UserPermissionContext.builder()
            .userId(userId)
            .roles(roles)
            .permissions(permissionTree)
            .dataScopes(dataScopes)
            .buildTime(LocalDateTime.now())
            .expireTime(LocalDateTime.now().plusHours(1))
            .build();
    }
}
```

#### 1.2 权限验证算法优化
```java
@Component
public class PermissionChecker {
    
    // 权限验证矩阵缓存
    private final LoadingCache<String, Boolean> permissionCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build(this::checkPermissionFromDatabase);
    
    public boolean hasPermission(Long userId, String resource, String action, 
                                Map<String, Object> context) {
        String cacheKey = buildPermissionKey(userId, resource, action, context);
        
        try {
            return permissionCache.get(cacheKey);
        } catch (Exception e) {
            log.error("权限检查异常: userId={}, resource={}, action={}", 
                     userId, resource, action, e);
            return false; // 安全策略：异常时拒绝访问
        }
    }
    
    private Boolean checkPermissionFromDatabase(String cacheKey) {
        PermissionCheckRequest request = parsePermissionKey(cacheKey);
        
        UserPermissionContext userContext = permissionService
            .getUserPermissions(request.getUserId());
            
        // 1. 基础权限检查
        if (!hasBasicPermission(userContext, request.getResource(), 
                               request.getAction())) {
            return false;
        }
        
        // 2. 上下文权限检查
        return hasContextPermission(userContext, request);
    }
    
    private boolean hasContextPermission(UserPermissionContext context, 
                                       PermissionCheckRequest request) {
        // 通知发布权限检查示例
        if ("notification".equals(request.getResource()) && 
            "create".equals(request.getAction())) {
            
            String notificationType = (String) request.getContext().get("notificationType");
            String targetType = (String) request.getContext().get("targetType");
            
            // 检查角色是否允许发布此类型通知
            for (UserRole role : context.getRoles()) {
                NotificationPublishRule rule = getPublishRule(role.getRoleCode());
                if (rule.canPublish(notificationType) && 
                    rule.canTarget(targetType)) {
                    return true;
                }
            }
            
            return false;
        }
        
        return true; // 其他情况默认通过上下文检查
    }
}
```

### 2. 10万+用户并发推送处理架构

#### 2.1 批量推送核心引擎
```java
@Service
public class HighConcurrencyPushEngine {
    
    @Autowired
    @Qualifier("pushTaskExecutor")
    private ThreadPoolTaskExecutor pushExecutor;
    
    @Autowired
    private KafkaTemplate<String, PushMessage> kafkaTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Async("pushTaskExecutor")
    public CompletableFuture<PushTaskResult> executeBatchPush(BatchPushTask task) {
        log.info("开始执行批量推送任务: taskId={}, targetCount={}", 
                task.getId(), task.getTargetUsers().size());
        
        // 1. 任务状态初始化
        updateTaskStatus(task.getId(), TaskStatus.RUNNING);
        
        try {
            // 2. 用户分片处理
            List<UserBatch> userBatches = partitionUsers(
                task.getTargetUsers(), task.getBatchSize());
            
            // 3. 并发推送处理
            List<CompletableFuture<BatchResult>> batchFutures = userBatches.stream()
                .map(batch -> processBatchAsync(task, batch))
                .collect(Collectors.toList());
            
            // 4. 等待所有批次完成
            CompletableFuture<Void> allBatches = CompletableFuture.allOf(
                batchFutures.toArray(new CompletableFuture[0]));
                
            return allBatches.thenApply(v -> {
                List<BatchResult> results = batchFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                    
                return aggregateResults(task, results);
            });
            
        } catch (Exception e) {
            log.error("批量推送任务执行失败: taskId={}", task.getId(), e);
            updateTaskStatus(task.getId(), TaskStatus.FAILED);
            throw new PushTaskException("推送任务执行失败", e);
        }
    }
    
    private CompletableFuture<BatchResult> processBatchAsync(
            BatchPushTask task, UserBatch batch) {
        
        return CompletableFuture.supplyAsync(() -> {
            BatchResult result = new BatchResult(batch.getBatchId());
            
            for (Long userId : batch.getUserIds()) {
                try {
                    // 构建推送消息
                    PushMessage message = buildPushMessage(task, userId);
                    
                    // 发送到消息队列
                    sendToMessageQueue(message);
                    
                    // 记录推送记录
                    savePushRecord(message, PushStatus.PENDING);
                    
                    result.incrementSuccess();
                    
                } catch (Exception e) {
                    log.error("用户推送失败: userId={}, taskId={}", 
                             userId, task.getId(), e);
                    result.incrementFailed();
                    result.addError(userId, e.getMessage());
                }
            }
            
            // 更新任务进度
            updateTaskProgress(task.getId(), batch);
            
            return result;
            
        }, pushExecutor);
    }
    
    private List<UserBatch> partitionUsers(List<Long> userIds, int batchSize) {
        List<UserBatch> batches = new ArrayList<>();
        
        for (int i = 0; i < userIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, userIds.size());
            List<Long> batchUsers = userIds.subList(i, endIndex);
            
            batches.add(UserBatch.builder()
                .batchId(i / batchSize + 1)
                .userIds(batchUsers)
                .build());
        }
        
        return batches;
    }
    
    private void sendToMessageQueue(PushMessage message) {
        String topic = "notification-push-" + message.getChannel();
        String key = message.getUserId().toString();
        
        kafkaTemplate.send(topic, key, message)
            .addCallback(
                result -> log.debug("消息发送成功: userId={}", message.getUserId()),
                failure -> log.error("消息发送失败: userId={}", 
                                   message.getUserId(), failure)
            );
    }
}
```

#### 2.2 消息队列消费者优化
```java
@Component
@KafkaListener(topics = {"notification-push-app", "notification-push-sms", 
                        "notification-push-email"})
public class PushMessageConsumer {
    
    @Autowired
    private List<PushChannelHandler> channelHandlers;
    
    @Autowired
    private PushRecordService pushRecordService;
    
    @KafkaHandler
    public void handlePushMessage(PushMessage message) {
        String channel = message.getChannel();
        
        try {
            // 1. 获取对应渠道处理器
            PushChannelHandler handler = getChannelHandler(channel);
            if (handler == null) {
                throw new UnsupportedChannelException("不支持的推送渠道: " + channel);
            }
            
            // 2. 执行推送
            long startTime = System.currentTimeMillis();
            PushResult result = handler.push(message);
            long costTime = System.currentTimeMillis() - startTime;
            
            // 3. 更新推送记录
            pushRecordService.updatePushResult(
                message.getRecordId(), result, costTime);
                
            // 4. 推送成功后的后续处理
            if (result.isSuccess()) {
                handlePushSuccess(message, result);
            } else {
                handlePushFailure(message, result);
            }
            
        } catch (Exception e) {
            log.error("推送消息处理失败: messageId={}, channel={}", 
                     message.getId(), channel, e);
            
            // 处理推送失败
            handlePushError(message, e);
        }
    }
    
    private void handlePushFailure(PushMessage message, PushResult result) {
        // 如果推送失败且重试次数未达到上限，则重新入队
        if (message.getRetryCount() < message.getMaxRetryCount()) {
            PushMessage retryMessage = message.toBuilder()
                .retryCount(message.getRetryCount() + 1)
                .nextRetryTime(calculateNextRetryTime(message.getRetryCount()))
                .build();
                
            // 延时重试 - 使用Redis延时队列
            scheduleRetry(retryMessage);
        }
    }
    
    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // 指数退避策略: 1分钟, 5分钟, 15分钟
        int delayMinutes = (int) Math.pow(5, retryCount);
        return LocalDateTime.now().plusMinutes(Math.min(delayMinutes, 60));
    }
}
```

### 3. 多租户数据隔离实现

#### 3.1 数据权限拦截器
```java
@Component
public class TenantDataPermissionHandler {
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, 
                           Object parameter, RowBounds rowBounds, 
                           ResultHandler resultHandler, BoundSql boundSql) {
        
        // 获取当前租户ID
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            throw new TenantContextException("租户上下文缺失");
        }
        
        // 解析SQL并添加租户过滤条件
        String originalSql = boundSql.getSql();
        String tenantFilteredSql = addTenantFilter(originalSql, currentTenantId);
        
        // 重新设置SQL
        setFieldValue(boundSql, "sql", tenantFilteredSql);
    }
    
    private String addTenantFilter(String originalSql, Long tenantId) {
        // 使用JSqlParser解析SQL
        Statement stmt = CCJSqlParserUtil.parse(originalSql);
        
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            
            // 添加租户ID过滤条件
            Expression tenantCondition = new EqualsTo(
                new Column("tenant_id"),
                new LongValue(tenantId)
            );
            
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, tenantCondition));
            } else {
                plainSelect.setWhere(tenantCondition);
            }
        }
        
        return stmt.toString();
    }
}
```

#### 3.2 租户上下文管理
```java
public class TenantContextHolder {
    
    private static final ThreadLocal<Long> TENANT_CONTEXT = new ThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        TENANT_CONTEXT.set(tenantId);
    }
    
    public static Long getTenantId() {
        return TENANT_CONTEXT.get();
    }
    
    public static void clear() {
        TENANT_CONTEXT.remove();
    }
}

@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, Object handler) {
        
        // 从Header中获取租户ID
        String tenantIdHeader = request.getHeader("Tenant-Id");
        
        if (StringUtils.hasText(tenantIdHeader)) {
            try {
                Long tenantId = Long.parseLong(tenantIdHeader);
                TenantContextHolder.setTenantId(tenantId);
                return true;
            } catch (NumberFormatException e) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return false;
            }
        }
        
        // 如果是登录接口，允许通过
        if (isLoginRequest(request)) {
            return true;
        }
        
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return false;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                              HttpServletResponse response, 
                              Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}
```

---

## ⚡ 性能优化具体措施

### 1. 数据库性能优化

#### 1.1 索引优化策略
```sql
-- 通知表核心查询索引
CREATE INDEX idx_notification_complex ON notification 
(tenant_id, status, type, publisher_id, create_time DESC);

-- 推送记录表分区索引
CREATE INDEX idx_push_record_query ON push_record 
(tenant_id, notification_id, user_id, status, push_time DESC);

-- 用户权限查询优化索引
CREATE INDEX idx_user_role_complex ON system_user_role 
(tenant_id, user_id, role_id);

-- 覆盖索引减少回表
CREATE INDEX idx_notification_cover ON notification 
(tenant_id, status, type) 
INCLUDE (title, publisher_name, create_time, target_count, read_count);
```

#### 1.2 分库分表实施
```yaml
# ShardingSphere配置
spring:
  shardingsphere:
    rules:
      sharding:
        tables:
          notification:
            actual-data-nodes: ds$->{0..3}.notification_$->{2024..2025}$->{01..12}
            database-strategy:
              standard:
                sharding-column: tenant_id
                sharding-algorithm-name: tenant-hash-mod
            table-strategy:
              standard:
                sharding-column: create_time
                sharding-algorithm-name: notification-month-range
                
          push_record:
            actual-data-nodes: ds$->{0..3}.push_record_$->{20240701..20241231}
            database-strategy:
              standard:
                sharding-column: tenant_id  
                sharding-algorithm-name: tenant-hash-mod
            table-strategy:
              standard:
                sharding-column: create_time
                sharding-algorithm-name: push-record-daily-range
```

### 2. 缓存优化策略

#### 2.1 多级缓存实现
```java
@Configuration
public class CacheConfiguration {
    
    // L1缓存: 本地Caffeine缓存
    @Bean
    public CacheManager localCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats()
        );
        return manager;
    }
    
    // L2缓存: Redis缓存
    @Bean  
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
                
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}

@Service
public class HybridCacheService {
    
    public <T> T getFromCache(String key, Class<T> type, 
                             Supplier<T> valueLoader) {
        // L1缓存查询
        T value = localCache.get(key, type);
        if (value != null) {
            return value;
        }
        
        // L2缓存查询  
        value = redisCache.get(key, type);
        if (value != null) {
            localCache.put(key, value);
            return value;
        }
        
        // 数据源查询
        value = valueLoader.get();
        if (value != null) {
            redisCache.put(key, value);
            localCache.put(key, value);
        }
        
        return value;
    }
}
```

#### 2.2 缓存预热和更新策略
```java
@Component
public class CacheWarmupService {
    
    @Scheduled(fixedRate = 300000) // 每5分钟执行
    public void warmupHotData() {
        // 1. 预热热点通知数据
        List<Long> hotNotificationIds = statisticsService.getHotNotifications(100);
        hotNotificationIds.parallelStream().forEach(id -> {
            notificationService.getNotificationDetail(id); // 触发缓存加载
        });
        
        // 2. 预热活跃用户权限
        List<Long> activeUserIds = userService.getActiveUsers(1000);
        activeUserIds.parallelStream().forEach(userId -> {
            permissionService.getUserPermissions(userId); // 触发权限缓存
        });
        
        // 3. 预热统计数据
        statisticsService.getTodayStatistics(); // 今日统计
        statisticsService.getWeeklyStatistics(); // 本周统计
    }
    
    @EventListener
    public void handleNotificationUpdated(NotificationUpdatedEvent event) {
        // 通知更新时清除相关缓存
        String notificationKey = "notification:" + event.getNotificationId();
        String statisticsKey = "statistics:notification:" + event.getNotificationId();
        
        cacheManager.evict("notifications", notificationKey);
        cacheManager.evict("statistics", statisticsKey);
        
        // 异步重建缓存
        CompletableFuture.runAsync(() -> {
            notificationService.getNotificationDetail(event.getNotificationId());
            statisticsService.getNotificationStatistics(event.getNotificationId());
        });
    }
}
```

### 3. 异步处理优化

#### 3.1 线程池配置优化
```java
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {
    
    @Bean("pushTaskExecutor")
    public ThreadPoolTaskExecutor pushTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(20);
        // 最大线程数  
        executor.setMaxPoolSize(100);
        // 队列容量
        executor.setQueueCapacity(500);
        // 线程名前缀
        executor.setThreadNamePrefix("push-task-");
        // 空闲线程存活时间
        executor.setKeepAliveSeconds(60);
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
    
    @Bean("notificationTaskExecutor")  
    public ThreadPoolTaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-task-");
        executor.initialize();
        return executor;
    }
}
```

---

## 📊 开发阶段性里程碑

### Week 1-2: 基础架构搭建
**里程碑目标**: 完成系统基础架构和核心配置

#### 具体交付物:
- [x] 项目结构创建和依赖配置
- [x] 数据库表结构创建和初始化
- [x] 基础框架集成 (Spring Security + JWT)
- [x] 多租户架构实现
- [x] 基础工具类和通用组件
- [x] 日志和监控配置

#### 验收标准:
- 项目可以正常启动和运行
- 数据库连接和基础CRUD操作正常
- JWT认证机制工作正常
- 多租户数据隔离验证通过

### Week 3-4: 用户权限系统
**里程碑目标**: 完成25+角色权限体系

#### 具体交付物:
- [x] 用户管理API (CRUD + 状态管理)
- [x] 角色权限管理API
- [x] 权限验证拦截器
- [x] 数据权限过滤器
- [x] 权限缓存机制
- [x] 权限验证单元测试 (覆盖率 > 80%)

#### 验收标准:
- 25个角色权限配置完成
- 6层级数据权限验证正确
- 权限查询性能 < 500ms
- 所有权限API测试通过

### Week 5-7: 通知核心功能
**里程碑目标**: 完成通知管理和分类系统

#### 具体交付物:
- [x] 通知CRUD API
- [x] 通知分类管理
- [x] 通知模板系统
- [x] 四级通知类型业务规则
- [x] 通知生命周期管理
- [x] 通知搜索和筛选API

#### 验收标准:
- 四级通知分类规则正确执行
- 通知发布权限验证准确
- 通知列表查询性能 < 1秒
- 通知模板渲染功能完整

### Week 8-10: 推送引擎系统
**里程碑目标**: 完成多渠道推送和高并发处理

#### 具体交付物:
- [x] 批量推送引擎
- [x] 多渠道推送适配器
- [x] 推送任务管理API
- [x] 推送状态跟踪
- [x] 推送性能优化
- [x] 推送重试机制

#### 验收标准:
- 支持10万+用户并发推送
- 推送成功率 > 99%
- 推送延迟 < 5秒
- 推送任务进度实时更新

### Week 11-12: 工作流审批系统
**里程碑目标**: 完成审批流程集成

#### 具体交付物:
- [x] Flowable工作流集成
- [x] 审批流程定义API
- [x] 审批任务处理API
- [x] 审批历史记录
- [x] 工作流监控

#### 验收标准:
- 审批流程配置正确
- 审批性能满足要求
- 审批历史记录完整
- 工作流监控数据准确

### Week 13-14: 统计监控系统  
**里程碑目标**: 完成数据统计和系统监控

#### 具体交付物:
- [x] 实时统计API
- [x] 报表生成API
- [x] 监控指标API
- [x] 预警机制
- [x] 数据可视化接口

#### 验收标准:
- 统计数据实时准确
- 报表生成性能良好
- 监控指标完整
- 预警机制有效

### Week 15-16: 集成测试和优化
**里程碑目标**: 系统集成测试和性能优化

#### 具体交付物:
- [x] 完整集成测试
- [x] 性能压力测试
- [x] 安全渗透测试
- [x] 问题修复和优化
- [x] 部署文档

#### 验收标准:
- 所有功能测试通过
- 性能指标达标
- 安全测试通过
- 系统稳定运行

---

## 📋 总结

### 核心技术特色

#### 1. 企业级架构设计
- **分层架构**: 清晰的业务分层和模块划分
- **微服务支持**: 为后续微服务拆分预留接口
- **云原生**: 支持容器化部署和水平扩展
- **高可用**: 多层容错和故障恢复机制

#### 2. 性能优化方案
- **多级缓存**: 本地缓存 + Redis + 数据库三级缓存
- **分库分表**: 按租户分库、按时间分表的分片策略
- **异步处理**: 大规模推送任务的异步批处理
- **连接池优化**: 数据库连接池和线程池的精细化配置

#### 3. 安全保障措施
- **权限控制**: 基于RBAC的细粒度权限管理
- **数据隔离**: 多租户数据完全隔离
- **安全防护**: SQL注入、XSS攻击等安全防护
- **审计日志**: 完整的操作审计和安全日志

#### 4. 业务特色功能
- **智能分类**: 四级通知分类的智能业务规则
- **工作流集成**: 基于Flowable的审批工作流
- **多渠道推送**: 统一推送网关支持多渠道
- **实时统计**: 基于事件驱动的实时数据统计

### 开发建议

#### 1. 立即行动项
1. **环境搭建**: 优先搭建开发、测试、性能测试环境
2. **团队组建**: 确保有经验的架构师和高级开发人员参与
3. **技术预研**: 对Flowable工作流和高并发推送进行技术预研
4. **监控体系**: 尽早建设完整的监控和告警体系

#### 2. 风险控制措施
1. **分阶段交付**: 按照里程碑计划分阶段交付和验收
2. **性能测试**: 在开发过程中持续进行性能测试
3. **代码质量**: 建立代码审查和自动化测试机制
4. **文档管理**: 保持设计文档和API文档的实时更新

通过本规格说明书的指导，开发团队可以有序、高效地完成这个复杂的企业级通知推送系统，确保系统在教育机构的复杂环境中稳定、安全、高效运行。