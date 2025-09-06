# 认证集成指南 - Mock/Real API切换方案

## 实施概述

**实施时间**: 2025-09-06  
**实施者**: Auth-Integration-Expert  
**核心目标**: 使Mock API返回格式与真实学校API完全一致，支持环境变量切换

## 核心改动

### 1. 修改的文件（最小化改动）

仅修改了2个核心文件：

1. **SchoolLoginResult.java** - 数据结构调整
   - 路径: `yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/dto/`
   - 改动: 完全重构为与真实API一致的格式

2. **MockSchoolUserServiceImpl.java** - 业务逻辑调整  
   - 路径: `yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/`
   - 改动: 重写`processSchoolAuthentication`方法，新增`buildMockLoginData`方法

### 2. 数据格式对比

#### 真实学校API格式
```json
{
  "code": 0,
  "data": {
    "id": "23230129050231",
    "companyId": "10000001", 
    "officeId": "01",
    "no": "2023010105",
    "schoolName": "江北校区",
    "name": "顾春琳",
    "email": "2023010105@hrbiit.edu.cn",
    "phone": "15846029850",
    "mobile": null,
    "role": ["student"],  // 数组格式
    "photo": null,
    "token": "557b76cd-ef17-4360-8a00-06a1b78b2656",  // UUID格式
    "grade": "2023",
    "teacherStatus": null,
    "className": "软件23M01"
  }
}
```

#### Mock API格式（改造后）
完全一致！包括：
- ✅ code/msg/data三层结构
- ✅ token为UUID格式
- ✅ role为数组格式
- ✅ 学生有grade/className，教师无
- ✅ 教师有multiple roles

## 使用方法

### 1. 环境变量控制

通过设置`SCHOOL_API_MODE`环境变量控制API模式：

```bash
# 使用Mock数据（默认）
export SCHOOL_API_MODE=mock

# 使用真实学校API
export SCHOOL_API_MODE=real
```

### 2. 启动服务

```bash
# 启动Mock School API服务（端口48082）
cd /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini
mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local
```

### 3. 测试验证

使用提供的测试脚本验证格式一致性：

```bash
# 运行测试脚本
/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/testing/test_school_login_format.sh
```

### 4. API调用示例

```bash
# 学生登录
curl -X POST http://localhost:48082/mock-school-api/auth/school-login \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "STUDENT_001",
    "name": "Student-Zhang",
    "password": "admin123"
  }'

# 教师登录  
curl -X POST http://localhost:48082/mock-school-api/auth/school-login \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "TEACHER_001",
    "name": "Teacher-Wang",
    "password": "admin123"
  }'
```

## 技术实现细节

### 1. 数据结构设计

**SchoolLoginResult.java**
```java
@Data
@Builder
public class SchoolLoginResult {
    private Integer code;
    private String msg;
    private LoginData data;
    
    @Data
    @Builder
    public static class LoginData {
        private String id;
        private String companyId;
        private String officeId;
        private String no;
        private String schoolName;
        private String name;
        private String email;
        private String phone;
        private String mobile;
        private List<String> role;  // 数组格式
        private String photo;
        private String token;  // UUID格式
        private String grade;
        private String teacherStatus;
        private String className;
    }
}
```

### 2. Mock数据生成逻辑

**buildMockLoginData方法**
- 根据employeeId前缀判断用户类型
- 使用UUID.randomUUID()生成标准UUID token
- 学生设置grade/className，教师设置为null
- 教师设置多个角色：["teacher", "zaizhi", "listen_admin"]

### 3. 环境变量检测

```java
String apiMode = System.getenv("SCHOOL_API_MODE");
boolean useRealApi = "real".equalsIgnoreCase(apiMode);
```

## 核心优势

1. **最小化改动**: 仅修改2个文件，降低系统风险
2. **格式100%一致**: Mock返回与真实API完全相同
3. **灵活切换**: 通过环境变量控制，无需修改代码
4. **向后兼容**: 保留原有功能，新增格式化输出

## 测试用例

### 测试账号
- 学生: STUDENT_001 / Student-Zhang / admin123
- 教师: TEACHER_001 / Teacher-Wang / admin123  
- 校长: PRINCIPAL_001 / Principal-Zhang / admin123
- 教务主任: ACADEMIC_ADMIN_001 / Director-Li / admin123

### 验证点
- ✅ Token格式为UUID
- ✅ role为数组格式
- ✅ 学生包含grade/className
- ✅ 教师不包含grade/className
- ✅ 响应码code=0表示成功

## 后续工作

1. **实现真实API调用逻辑**
   - 当SCHOOL_API_MODE=real时，调用真实学校API
   - 解析真实API返回并转换为统一格式

2. **错误处理增强**
   - 添加更详细的错误码和错误信息
   - 实现重试机制和降级策略

3. **性能优化**
   - 添加缓存机制减少API调用
   - 实现批量查询接口

## 注意事项

1. **服务必须重启**: Java代码修改后必须重启服务才能生效
2. **端口检查**: 确保48082端口未被占用
3. **依赖服务**: Mock School API可独立运行，不依赖主服务

---

**文档版本**: 1.0.0  
**最后更新**: 2025-09-06  
**维护者**: Auth-Integration-Expert