# 🚀 智能通知系统本地服务器启动指南

## 📋 准备工作

### ✅ 已配置环境
- ✅ Java 17 (OpenJDK) - 已安装
- ✅ Maven 3.9.11 - 已安装  
- ✅ H2内存数据库 - 已配置
- ✅ 演示数据 - 已准备

### 🎯 演示配置说明
- **数据库**: H2内存数据库 (无需安装MySQL)
- **端口**: 48080 
- **认证**: 已简化，方便测试
- **示例数据**: 6条通知演示数据

---

## 🚀 启动方式 (选择一种)

### 方式一：使用启动脚本 (推荐)

**步骤 1**: 双击运行启动脚本
```
D:\ClaudeCode\AI_Web\start_server.bat
```

**步骤 2**: 等待服务启动完成（约30-60秒）

### 方式二：命令行启动

**步骤 1**: 打开命令提示符 (Win+R → cmd)

**步骤 2**: 设置环境变量
```bat
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot"
set "MAVEN_HOME=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.11"  
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
```

**步骤 3**: 切换到项目目录
```bat
cd /d "D:\ClaudeCode\AI_Web\yudao-boot-mini"
```

**步骤 4**: 启动应用
```bat
mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=demo
```

---

## 🌐 访问地址

### 🏠 主要访问地址

| 服务 | 地址 | 说明 |
|-----|------|------|
| **API文档 (推荐)** | http://localhost:48080/doc.html | Knife4j API文档界面 |
| **Swagger UI** | http://localhost:48080/swagger-ui/index.html | 标准Swagger界面 |
| **数据库控制台** | http://localhost:48080/h2-console | H2数据库管理界面 |
| **数据监控** | http://localhost:48080/druid | Druid数据源监控 |

### 🔧 数据库连接信息 (H2控制台)
- **JDBC URL**: `jdbc:h2:mem:notification_demo`
- **用户名**: `sa`
- **密码**: (留空)
- **驱动**: `org.h2.Driver`

### 📊 监控账号信息
- **Druid监控** - 用户名: `admin` / 密码: `admin123`

---

## 🧪 API测试指南

### 1. 📖 查看通知列表

**接口**: `GET /admin-api/notification/page`

**测试步骤**:
1. 打开API文档: http://localhost:48080/doc.html
2. 找到 "通知管理" → "分页查询通知"
3. 点击 "Try it out"
4. 设置参数 `pageNo=1, pageSize=10`
5. 点击 "Execute"

**预期结果**: 返回6条演示通知数据

### 2. ➕ 创建新通知

**接口**: `POST /admin-api/notification/create`

**测试数据**:
```json
{
  "title": "API测试通知",
  "content": "这是通过API创建的测试通知",
  "level": 3,
  "categoryId": 1,
  "publisherId": 1,
  "publisherName": "API测试者",
  "requireConfirm": false,
  "pinned": false
}
```

### 3. 🔍 查询单个通知

**接口**: `GET /admin-api/notification/get?id=1`

**说明**: 查询ID为1的通知详情

### 4. 📈 查看统计信息

**接口**: `GET /admin-api/notification/statistics/1`

**说明**: 查看通知的推送统计数据

---

## 📊 演示数据说明

系统已预置6条不同类型的通知：

| ID | 标题 | 级别 | 状态 | 分类 |
|----|------|------|------|------|
| 1 | 智能通知系统正式上线 | 重要 | 已发布 | 系统通知 |
| 2 | 系统维护通知 | 紧急 | 已发布 | 系统通知 |  
| 3 | 期末考试安排通知 | 重要 | 已发布 | 教务通知 |
| 4 | 学费缴费提醒 | 重要 | 已发布 | 财务通知 |
| 5 | 校园文化节活动预告 | 常规 | 已发布 | 活动通知 |
| 6 | 上课提醒：高等数学 | 提醒 | 已发布 | 教务通知 |

---

## 🛠 常见问题解决

### ❓ 启动失败怎么办？

1. **检查Java版本**:
   ```bat
   java -version
   ```
   确认是Java 17版本

2. **检查端口占用**:
   ```bat
   netstat -ano | findstr :48080
   ```
   如被占用，结束占用进程或修改端口

3. **查看日志信息**: 启动时注意控制台输出的错误信息

### ❓ 访问页面显示404？

1. 确认服务已完全启动 (看到 "Started YudaoServerApplication" 日志)
2. 检查访问地址是否正确
3. 尝试访问基础接口: http://localhost:48080/admin-api/notification/page

### ❓ API测试时报错？

1. 检查请求格式是否正确
2. 查看控制台错误日志
3. 确认H2数据库已正确初始化

---

## 🎯 测试重点功能

### 1. 四级通知分类
- 测试创建不同级别的通知
- 观察不同级别的颜色和确认要求

### 2. 业务流程
- 创建草稿 → 提交审批 → 发布通知
- 测试状态流转的正确性

### 3. 分页查询
- 测试按条件过滤
- 测试排序功能

### 4. 统计功能
- 查看推送量、阅读量统计
- 测试数据更新功能

---

## 📝 启动成功标志

看到以下日志表示启动成功：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.5)

Started YudaoServerApplication in 15.234 seconds (process running for 16.789)

智能通知系统启动成功！
访问地址：http://localhost:48080/doc.html
```

---

## 🎉 开始测试

1. 🚀 **启动服务**: 运行 `start_server.bat`
2. 🌐 **打开浏览器**: http://localhost:48080/doc.html  
3. 🧪 **开始测试**: 从查询通知列表开始
4. 📝 **创建通知**: 尝试创建自己的通知
5. 🔄 **测试流程**: 体验完整的业务流程

**祝您测试愉快！** 🎊