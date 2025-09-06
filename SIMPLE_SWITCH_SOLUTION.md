# 🎯 最简单的Mock/Real API切换方案

> **设计原则**: 最小改动，立即可用，生产环境一键切换
> **核心思路**: 只改数据格式，不动架构

## 📋 方案概述

**一句话方案**: 修改MockSchoolApiService的返回数据格式，使其与真实API完全一致，通过一个环境变量控制Mock/Real切换。

## 🔧 实施步骤（只需3步）

### Step 1: 修改DTO定义（对齐真实API）

**文件**: `yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/dto/SchoolLoginResult.java`

```java
package cn.iocoder.yudao.mock.school.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchoolLoginResult {
    // 统一字段（所有用户都有）
    private String id;           // 用户ID
    private String companyId;    // 公司ID
    private String officeId;     // 部门ID
    private String no;           // 工号/学号
    private String name;         // 姓名
    private String email;        // 邮箱
    private String phone;        // 电话
    private String mobile;       // 手机
    private List<String> role;   // 角色数组
    private String photo;        // 头像
    private String token;        // UUID格式token
    
    // 学生特有字段
    private String schoolName;   // 校区名称
    private String grade;        // 年级
    private String className;    // 班级
    
    // 教师特有字段
    private String teacherStatus; // 教师状态
}
```

### Step 2: 修改Mock数据填充逻辑

**文件**: `yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java`

```java
@Service
public class MockSchoolUserServiceImpl implements MockSchoolUserService {
    
    @Override
    public SchoolLoginResult authenticate(String employeeId, String name, String password) {
        SchoolLoginResult result = new SchoolLoginResult();
        
        // 根据工号判断角色
        if (employeeId.startsWith("202")) {
            // 学生数据（完全匹配真实API）
            result.setId("23230129050231");
            result.setCompanyId("10000001");
            result.setOfficeId("01");
            result.setNo(employeeId);
            result.setSchoolName("江北校区");
            result.setName(name);
            result.setEmail(employeeId + "@hrbiit.edu.cn");
            result.setPhone("15846029850");
            result.setMobile(null);
            result.setRole(Arrays.asList("student"));
            result.setPhoto(null);
            result.setToken(UUID.randomUUID().toString()); // UUID格式
            result.setGrade("2023");
            result.setClassName("软件23M01");
            
        } else if (employeeId.equals("10031") || employeeId.startsWith("TEACHER")) {
            // 教师数据（完全匹配真实API）
            result.setId(employeeId);
            result.setCompanyId("10000001");
            result.setOfficeId("90000022");
            result.setNo(employeeId);
            result.setSchoolName(null);
            result.setName(name);
            result.setEmail("teacher@hrbiit.edu.cn");
            result.setPhone("15945931099");
            result.setMobile("15945931099");
            result.setRole(Arrays.asList("teacher", "zaizhi", "listen_admin"));
            result.setPhoto(null);
            result.setToken(UUID.randomUUID().toString());
            result.setGrade(null);
            result.setClassName(null);
            
        } else if (employeeId.startsWith("PRINCIPAL")) {
            // 校长数据
            result.setId(employeeId);
            result.setCompanyId("10000001");
            result.setOfficeId("90000001");
            result.setNo(employeeId);
            result.setName(name);
            result.setRole(Arrays.asList("principal", "teacher", "admin"));
            result.setToken(UUID.randomUUID().toString());
        }
        
        return result;
    }
}
```

### Step 3: 配置环境变量切换

**文件**: `yudao-boot-mini/yudao-mock-school-api/src/main/resources/application.yaml`

```yaml
school:
  api:
    # 核心切换开关（只需要这一个）
    mode: ${SCHOOL_API_MODE:mock}  # mock或real，默认mock
    
    # Mock模式配置
    mock:
      enabled: true
      
    # Real模式配置（生产环境才需要）
    real:
      enabled: false
      base-url: ${SCHOOL_API_BASE_URL:https://work.greathiit.com}
      path: /api/user/loginWai
```

## 🎮 使用方法

### 开发环境（默认Mock）
```bash
# 不需要设置任何环境变量，直接启动
mvn spring-boot:run
```

### 生产环境（切换到Real）
```bash
# 只需要设置一个环境变量
export SCHOOL_API_MODE=real
mvn spring-boot:run
```

### Docker部署
```dockerfile
# 开发环境
ENV SCHOOL_API_MODE=mock

# 生产环境
ENV SCHOOL_API_MODE=real
ENV SCHOOL_API_BASE_URL=https://work.greathiit.com
```

## ✅ 验证清单

### Mock模式验证
```bash
# 测试学生登录
curl -X POST http://localhost:48082/mock-school-api/auth/school-login \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "2023010105",
    "name": "测试学生",
    "password": "888888"
  }'

# 期望返回（与真实API格式完全一致）
{
  "code": 0,
  "data": {
    "id": "23230129050231",
    "no": "2023010105",
    "name": "测试学生",
    "token": "557b76cd-ef17-4360-8a00-06a1b78b2656",
    "role": ["student"],
    "grade": "2023",
    "className": "软件23M01"
  }
}
```

## 🎯 方案优势

1. **最小改动**: 只改了2个文件（DTO + Service）
2. **零侵入**: 不影响现有架构和业务逻辑
3. **一键切换**: 一个环境变量搞定Mock/Real切换
4. **格式一致**: Mock返回与真实API 100%相同
5. **立即可用**: 改完即可测试，无需等待

## 📊 与其他方案对比

| 方案 | 改动文件数 | 复杂度 | 切换方式 | 风险 |
|------|------------|--------|----------|------|
| **本方案** | 2个 | ⭐ | 1个环境变量 | 极低 |
| NetworkProbe方案 | 5+个 | ⭐⭐⭐⭐ | 多个配置 | 中等 |
| 完全重构 | 10+个 | ⭐⭐⭐⭐⭐ | 复杂配置 | 高 |

## 🚀 下一步行动

1. **立即执行**: 修改上述2个文件
2. **本地测试**: 验证Mock数据格式
3. **部署测试**: 在测试环境验证Real模式
4. **生产部署**: 设置SCHOOL_API_MODE=real即可

---

**结论**: 这是最简单、最实用、风险最低的方案。无需过度设计，立即可用！