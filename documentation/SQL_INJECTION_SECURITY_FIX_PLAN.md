# 哈尔滨信息工程学院校园门户系统SQL注入修复方案

## 🎯 修复目标

基于发现的SQL注入风险点，设计企业级安全修复方案：
- **零容忍原则**：消除所有SQL注入风险
- **分层防护**：输入验证+参数化查询+白名单控制
- **向后兼容**：保持现有API接口不变
- **性能保证**：修复不影响系统性能

## 📊 风险评估与分类

### 🔴 P0 - 关键风险 (立即修复)

#### 1. PermissionMapper.xml - 动态表名注入
```xml
<!-- 当前风险代码 -->
<select id="selectByPermission" resultType="java.lang.Long">
    SELECT COUNT(*)
    FROM ${permission}  <!-- 🚨 直接拼接，极高风险 -->
    WHERE deleted = 0
</select>
```
**风险程度**: ⭐⭐⭐⭐⭐ (5/5)  
**影响范围**: 权限系统核心  
**攻击向量**: 恶意表名 → 数据泄露/系统破坏

### 🟠 P1 - 重要风险 (优先修复)

#### 2. SysRoleMenuMapper.java - 字符串拼接
```java
// 当前风险代码
@Select("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE update_time > #{maxUpdateTime}")
Long selectCountByUpdateTimeGt(@Param("maxUpdateTime") LocalDateTime maxUpdateTime);
```
**风险程度**: ⭐⭐⭐⭐ (4/5)  
**影响范围**: 角色菜单权限  
**攻击向量**: 常量注入风险较低，但违反安全规范

#### 3. BaseMapperX - 动态SQL Provider
```java
// 当前风险代码
@SelectProvider(type = DynamicSqlProvider.class, method = "buildSelectSql")
List<T> selectByDynamicCondition(Map<String, Object> params);
```
**风险程度**: ⭐⭐⭐ (3/5)  
**影响范围**: 通用查询框架  
**攻击向量**: 参数注入可能性

## 🛡️ 分层防护修复架构

### Layer 1: 输入验证层
- **白名单验证**：严格限制动态参数
- **类型检查**：强制参数类型验证
- **长度限制**：防止缓冲区溢出

### Layer 2: SQL构建层  
- **参数化查询**：完全消除字符串拼接
- **预编译语句**：使用PreparedStatement
- **安全Builder**：封装SQL构建逻辑

### Layer 3: 执行层
- **权限检查**：SQL执行前权限验证
- **审计日志**：记录所有SQL执行
- **异常处理**：安全的错误信息

## 🔧 具体修复方案

### 方案一：PermissionMapper.xml 安全重构

#### **修复前 (风险代码)**
```xml
<select id="selectByPermission" resultType="java.lang.Long">
    SELECT COUNT(*)
    FROM ${permission}
    WHERE deleted = 0
</select>
```

#### **修复后 (安全代码)**
```xml
<!-- 方案1: 条件分支 + 预定义表名 -->
<select id="selectByPermission" resultType="java.lang.Long">
    SELECT COUNT(*) FROM
    <choose>
        <when test="permission == 'sys_user_permission'">sys_user_permission</when>
        <when test="permission == 'sys_role_permission'">sys_role_permission</when>
        <when test="permission == 'sys_dept_permission'">sys_dept_permission</when>
        <when test="permission == 'sys_menu_permission'">sys_menu_permission</when>
        <otherwise>
            <!-- 默认安全表，防止注入 -->
            sys_permission_default
        </otherwise>
    </choose>
    WHERE deleted = 0
</select>
```

#### **配套Java接口安全改造**
```java
public interface PermissionMapper {
    
    /**
     * 安全的权限查询方法
     * @param permission 权限表名（仅接受预定义值）
     * @return 权限数量
     */
    Long selectByPermission(@Param("permission") String permission);
    
    /**
     * 权限表白名单验证
     */
    default boolean isValidPermissionTable(String tableName) {
        Set<String> allowedTables = Set.of(
            "sys_user_permission",
            "sys_role_permission", 
            "sys_dept_permission",
            "sys_menu_permission"
        );
        return allowedTables.contains(tableName);
    }
}
```

#### **Service层安全封装**
```java
@Service
public class PermissionSecurityService {
    
    @Autowired
    private PermissionMapper permissionMapper;
    
    /**
     * 安全的权限统计方法
     */
    public Long countPermissionsSafely(String permissionType) {
        // 1. 输入验证
        if (StringUtils.isBlank(permissionType)) {
            throw new IllegalArgumentException("权限类型不能为空");
        }
        
        // 2. 白名单验证  
        String tableName = mapPermissionTypeToTable(permissionType);
        if (!permissionMapper.isValidPermissionTable(tableName)) {
            throw new SecurityException("非法的权限表名: " + permissionType);
        }
        
        // 3. 参数化查询
        return permissionMapper.selectByPermission(tableName);
    }
    
    /**
     * 权限类型到表名映射（安全转换）
     */
    private String mapPermissionTypeToTable(String permissionType) {
        Map<String, String> typeToTable = Map.of(
            "USER", "sys_user_permission",
            "ROLE", "sys_role_permission",
            "DEPT", "sys_dept_permission", 
            "MENU", "sys_menu_permission"
        );
        return typeToTable.getOrDefault(permissionType.toUpperCase(), "sys_permission_default");
    }
}
```

### 方案二：SysRoleMenuMapper.java 参数化重构

#### **修复前 (风险代码)**
```java
@Select("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE update_time > #{maxUpdateTime}")
Long selectCountByUpdateTimeGt(@Param("maxUpdateTime") LocalDateTime maxUpdateTime);
```

#### **修复后 (安全代码)**
```java
/**
 * 安全的角色菜单统计查询
 * 使用XML配置替代字符串拼接
 */
Long selectCountByUpdateTimeGt(@Param("maxUpdateTime") LocalDateTime maxUpdateTime);
```

#### **对应XML配置**
```xml
<!-- SysRoleMenuMapper.xml -->
<mapper namespace="cn.iocoder.yudao.module.system.dal.mysql.permission.SysRoleMenuMapper">
    
    <select id="selectCountByUpdateTimeGt" resultType="java.lang.Long">
        SELECT COUNT(*) 
        FROM sys_role_menu 
        WHERE update_time > #{maxUpdateTime}
          AND deleted = 0
    </select>
    
</mapper>
```

### 方案三：BaseMapperX 动态SQL安全化

#### **修复前 (风险代码)**
```java
@SelectProvider(type = DynamicSqlProvider.class, method = "buildSelectSql")
List<T> selectByDynamicCondition(Map<String, Object> params);
```

#### **修复后 (安全代码)**
```java
/**
 * 安全的动态SQL构建器
 */
public class SecureDynamicSqlProvider {
    
    // 允许的字段白名单
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "id", "name", "status", "create_time", "update_time", "deleted"
    );
    
    // 允许的操作符白名单
    private static final Set<String> ALLOWED_OPERATORS = Set.of(
        "=", ">", "<", ">=", "<=", "!=", "LIKE", "IN"
    );
    
    /**
     * 安全的动态SELECT构建
     */
    public String buildSecureSelectSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        
        // 1. 表名验证（必须从白名单获取）
        String tableName = validateAndGetTableName(params);
        sql.append(tableName);
        
        // 2. WHERE条件安全构建
        String whereClause = buildSecureWhereClause(params);
        if (StringUtils.isNotBlank(whereClause)) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        // 3. 添加默认删除标记过滤
        sql.append(" AND deleted = 0");
        
        return sql.toString();
    }
    
    /**
     * 安全的WHERE子句构建
     */
    private String buildSecureWhereClause(Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 跳过非查询参数
            if ("tableName".equals(key) || value == null) {
                continue;
            }
            
            // 验证字段名
            if (!isValidFieldName(key)) {
                throw new SecurityException("非法字段名: " + key);
            }
            
            // 构建安全条件 (使用参数化查询)
            conditions.add(key + " = #{" + key + "}");
        }
        
        return String.join(" AND ", conditions);
    }
    
    /**
     * 表名白名单验证
     */
    private String validateAndGetTableName(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
        
        // 表名白名单检查
        Set<String> allowedTables = Set.of(
            "sys_user", "sys_role", "sys_menu", "sys_dept",
            "notification_info", "weather_cache", "todo_info"
        );
        
        if (!allowedTables.contains(tableName)) {
            throw new SecurityException("非法表名: " + tableName);
        }
        
        return tableName;
    }
    
    /**
     * 字段名安全验证
     */
    private boolean isValidFieldName(String fieldName) {
        // 1. 基础格式检查
        if (!fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return false;
        }
        
        // 2. 白名单检查
        return ALLOWED_FIELDS.contains(fieldName);
    }
}
```

## 🧪 测试用例设计

### 单元测试 - SQL注入攻击模拟

```java
@SpringBootTest
public class SqlInjectionSecurityTest {
    
    @Autowired
    private PermissionSecurityService permissionSecurityService;
    
    @Test
    @DisplayName("测试表名注入攻击防护")
    public void testTableNameInjectionPrevention() {
        // 模拟SQL注入攻击
        String[] maliciousInputs = {
            "sys_user; DROP TABLE sys_user; --",
            "sys_user' UNION SELECT password FROM admin_user --",
            "sys_user WHERE 1=1 OR '1'='1",
            "../../../etc/passwd",
            "<script>alert('xss')</script>"
        };
        
        for (String maliciousInput : maliciousInputs) {
            assertThatThrownBy(() -> {
                permissionSecurityService.countPermissionsSafely(maliciousInput);
            })
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("非法");
        }
    }
    
    @Test
    @DisplayName("测试正常权限查询功能")
    public void testLegitimatePermissionQuery() {
        // 正常查询应该正常工作
        Long userPermCount = permissionSecurityService.countPermissionsSafely("USER");
        Long rolePermCount = permissionSecurityService.countPermissionsSafely("ROLE");
        
        assertThat(userPermCount).isGreaterThanOrEqualTo(0);
        assertThat(rolePermCount).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    @DisplayName("测试动态SQL安全构建")
    public void testSecureDynamicSqlBuilder() {
        SecureDynamicSqlProvider provider = new SecureDynamicSqlProvider();
        
        // 正常参数
        Map<String, Object> validParams = Map.of(
            "tableName", "sys_user",
            "status", 1,
            "name", "admin"
        );
        
        String sql = provider.buildSecureSelectSql(validParams);
        
        assertThat(sql).contains("sys_user");
        assertThat(sql).contains("status = #{status}");
        assertThat(sql).contains("deleted = 0");
        assertThat(sql).doesNotContain("DROP");
        assertThat(sql).doesNotContain("UNION");
    }
}
```

### 集成测试 - 端到端安全验证

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SqlInjectionIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("API层SQL注入攻击防护测试")
    public void testApiSqlInjectionPrevention() {
        // 获取认证Token
        String jwt = getValidJwtToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.set("tenant-id", "1");
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 测试权限查询API的注入攻击防护
        String[] maliciousQueries = {
            "USER'; DROP TABLE sys_user; --",
            "ROLE' UNION SELECT password FROM admin --",
            "MENU' OR '1'='1' --"
        };
        
        for (String maliciousQuery : maliciousQueries) {
            ResponseEntity<String> response = restTemplate.exchange(
                "/admin-api/test/permission/count?type=" + maliciousQuery,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );
            
            // 应该返回错误而不是执行恶意SQL
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.BAD_REQUEST, 
                HttpStatus.FORBIDDEN,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    private String getValidJwtToken() {
        // 使用测试账号获取JWT Token
        Map<String, String> loginRequest = Map.of(
            "employeeId", "SYSTEM_ADMIN_001",
            "name", "系统管理员",
            "password", "admin123"
        );
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://localhost:48082/mock-school-api/auth/authenticate",
            loginRequest,
            Map.class
        );
        
        return (String) response.getBody().get("data");
    }
}
```

## 📋 实施计划

### Phase 1: 紧急修复 (1-2天)
- [ ] **P0风险**: PermissionMapper.xml 动态表名注入修复
- [ ] **安全测试**: 注入攻击模拟测试
- [ ] **回滚预案**: 准备紧急回滚脚本

### Phase 2: 系统加固 (3-5天)  
- [ ] **P1风险**: SysRoleMenuMapper.java 参数化改造
- [ ] **框架升级**: BaseMapperX 安全化重构
- [ ] **集成测试**: 端到端安全验证

### Phase 3: 完善防护 (1-2天)
- [ ] **安全审计**: SQL执行日志记录
- [ ] **监控告警**: 注入攻击检测告警
- [ ] **文档更新**: 安全开发规范

## 🔙 回滚应急预案

### 紧急回滚策略
1. **代码回滚**: Git快速回滚到修复前版本
2. **数据库备份**: 确保修复前数据完整性
3. **服务降级**: 临时禁用高风险功能

### 回滚执行脚本
```bash
#!/bin/bash
# SQL注入修复紧急回滚脚本

echo "🚨 开始SQL注入修复回滚..."

# 1. 停止服务
sudo pkill -f java

# 2. Git代码回滚
git stash
git checkout HEAD~1  # 回滚到修复前版本

# 3. 数据库恢复 (如需要)
# mysql -u root ruoyi-vue-pro < backup_before_sql_fix.sql

# 4. 重启服务
cd /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini
mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local &
mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local &

echo "✅ 回滚完成，服务已恢复"
```

## 📊 风险评估矩阵

| 风险点 | 修复前风险 | 修复后风险 | 修复成本 | 业务影响 |
|--------|------------|------------|----------|----------|
| PermissionMapper.xml | ⭐⭐⭐⭐⭐ | ⭐ | 中等 | 低 |
| SysRoleMenuMapper | ⭐⭐⭐⭐ | ⭐ | 低 | 无 |
| BaseMapperX | ⭐⭐⭐ | ⭐ | 高 | 低 |

## 🎯 成功标准

### 安全指标
- [ ] **零SQL注入**: 所有注入攻击被成功阻止
- [ ] **性能保持**: 修复后API响应时间不增加20%以上  
- [ ] **功能完整**: 所有现有功能正常工作
- [ ] **测试覆盖**: SQL注入测试覆盖率100%

### 验收标准
- [ ] **渗透测试**: 第三方安全测试通过
- [ ] **代码审计**: 静态代码分析0高风险
- [ ] **监控告警**: 注入攻击实时检测就位
- [ ] **文档完整**: 安全修复文档100%完成

---

**📋 项目**: 哈尔滨信息工程学院校园门户系统  
**📅 创建时间**: 2025年1月5日  
**👨‍💻 负责人**: Backend System Architect  
**🔒 安全级别**: P0 - 最高优先级  
**📈 预期收益**: 消除SQL注入风险，提升系统安全等级