# 学校API集成 - AI协作模板

## 项目背景
- **项目**: 哈尔滨信息工程学院校园门户系统
- **技术栈**: Spring Boot 3.4.5 + Vue 3
- **当前任务**: 学校API集成 - Basic Token到JWT Token适配

## 核心代码文件

### 1. MockAuthController.java (完整代码)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/controller/MockAuthController.java

```java
[请使用 ai-context-collector.sh 自动收集此文件的完整内容]
```

### 2. SecurityTokenService.java (完整代码)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/SecurityTokenService.java

```java
[请使用 ai-context-collector.sh 自动收集此文件的完整内容]
```

### 3. UserInfo.java (DTO完整代码)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/dto/UserInfo.java

```java
[请使用 ai-context-collector.sh 自动收集此文件的完整内容]
```

### 4. application-local.yaml (配置片段)
文件路径: /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/resources/application-local.yaml

```yaml
# 学校API相关配置
school:
  api:
    mode: ${SCHOOL_API_MODE:mock}  # mock 或 real
    real-endpoint: https://work.greathiit.com/api/user/loginWai
    mock-endpoint: http://localhost:48082/mock-school-api
    timeout: 5000
    retry-times: 3
```

## 真实学校API信息

### API端点
- URL: `https://work.greathiit.com/api/user/loginWai`
- Method: POST
- Content-Type: application/json

### 请求格式
```json
{
  "userNumber": "工号",
  "password": "密码",
  "autoLogin": true,
  "provider": "account"
}
```

### 响应格式
```json
{
  "success": true,
  "data": {
    "token": "uuid-format-basic-token",
    "userInfo": {
      "name": "用户姓名",
      "userNumber": "工号",
      "role": ["student", "teacher"],
      "grade": "2023",
      "className": "计科1班",
      "department": "信息工程系"
    }
  }
}
```

## 需要实现的功能

### 1. Basic Token认证接口
```java
@PostMapping("/auth/basic-authenticate")
public MockApiResponse<Map<String, Object>> basicAuthenticate(@RequestBody Map<String, String> request) {
    // 1. 调用真实学校API获取Basic Token
    // 2. 缓存Basic Token与用户信息映射
    // 3. 生成系统JWT Token
    // 4. 返回JWT Token给前端
}
```

### 2. Token转换接口
```java
@PostMapping("/auth/convert-token")
public MockApiResponse<String> convertToken(@RequestBody Map<String, String> request) {
    // 1. 验证Basic Token有效性
    // 2. 从缓存获取用户信息
    // 3. 生成对应的JWT Token
    // 4. 返回JWT Token
}
```

### 3. 配置驱动切换
```java
@Value("${school.api.mode}")
private String apiMode;

private boolean isRealMode() {
    return "real".equalsIgnoreCase(apiMode);
}
```

## 具体问题

1. **Token兼容性问题**: Basic Token (UUID格式) vs JWT Token (JSON格式)
2. **用户信息映射**: 学校API返回的role数组需要映射到系统的单一角色
3. **缓存策略**: Basic Token缓存时间和更新机制
4. **降级策略**: 学校API不可用时的处理方案

## 期望输出

请提供：
1. 完整的Basic Token认证实现代码
2. Token缓存管理方案（Redis或内存）
3. 配置驱动的模式切换实现
4. 错误处理和降级策略
5. 前端调用示例代码

## 架构约束

- 必须保持现有Mock API完全可用（向后兼容）
- 不能修改现有JWT Token验证逻辑
- 必须支持配置文件切换（不需要改代码）
- 要考虑学校API的稳定性问题
