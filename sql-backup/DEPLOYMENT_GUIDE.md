# 🚀 校园门户系统部署指南

## 📋 **敏感文件处理说明**

### ⚠️ **重要说明**
原始SQL文件(`ruoyi-vue-pro.sql`)包含敏感信息（阿里云/腾讯云API密钥），已从Git仓库中移除以确保安全性。

### 🔒 **安全措施**
- ✅ 敏感文件已添加到`.gitignore`
- ✅ Git历史记录已清理
- ✅ 创建了安全的部署版本

## 📁 **部署文件说明**

| 文件 | 用途 | 安全等级 |
|------|------|----------|
| `deployment-safe-database.sql` | 🟢 **新环境部署使用** | ✅ 安全，可分享 |
| `database-schema-only.sql` | 🟢 **数据库结构备份** | ✅ 安全，仅结构 |
| ~~`ruoyi-vue-pro.sql`~~ | ❌ **已移除** | 🚨 包含敏感信息 |

## 🔧 **新环境部署步骤**

### 1️⃣ **克隆项目**
```bash
git clone https://github.com/zhenzhu143321/hxci-campus-portal-system.git
cd hxci-campus-portal-system
```

### 2️⃣ **数据库初始化**
```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE ruoyi_vue_pro DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入安全SQL文件
mysql -u root -p ruoyi_vue_pro < sql-backup/deployment-safe-database.sql
```

### 3️⃣ **配置文件设置**
编辑以下配置文件，补充必要的API密钥：

#### **后端配置** (`yudao-boot-mini/yudao-server/src/main/resources/application.yml`)
```yaml
# 配置数据库连接
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ruoyi_vue_pro?useSSL=false&serverTimezone=Asia/Shanghai
    username: your_db_username
    password: your_db_password

# 配置和风天气API（如需要）
weather:
  api:
    key: your_weather_api_key
    base-url: your_weather_api_endpoint
```

#### **学校API集成** (`yudao-boot-mini/yudao-mock-school-api/src/main/resources/application.yml`)
```yaml
# 配置学校API接口
school:
  api:
    endpoint: your_school_api_endpoint
    credentials: your_school_api_credentials
```

### 4️⃣ **启动服务**
```bash
# 启动后端服务
cd yudao-boot-mini
mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local &
mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local &

# 启动前端服务
cd ../hxci-campus-portal
npm install
npm run dev
```

### 5️⃣ **验证部署**
```bash
# 检查服务状态
curl http://localhost:48081/admin-api/test/notification/api/ping
curl http://localhost:48082/mock-school-api/ping

# 检查前端访问
curl http://localhost:3000
```

## 🔐 **安全配置清单**

### ✅ **必须配置项**
- [ ] 修改默认数据库密码
- [ ] 配置JWT密钥（生产环境使用强密钥）
- [ ] 设置CSRF密钥
- [ ] 配置和风天气API密钥（如需要）
- [ ] 配置学校API认证信息

### ✅ **推荐配置项**
- [ ] 启用HTTPS
- [ ] 配置防火墙规则
- [ ] 设置日志轮转
- [ ] 配置监控告警
- [ ] 设置数据库备份

## 🚨 **注意事项**

### 🔒 **敏感信息管理**
- 🚫 **绝对不要**将包含真实API密钥的配置文件提交到Git
- ✅ 使用环境变量或配置文件管理敏感信息
- ✅ 定期轮换API密钥和数据库密码

### 📊 **数据恢复**
如果需要完整的生产数据：
1. 联系系统管理员获取完整备份
2. 或从现有环境导出数据
3. 确保敏感信息在传输过程中的安全性

### 🔧 **故障排除**
- 服务启动失败：检查端口占用和配置文件
- 数据库连接失败：验证连接字符串和权限
- API调用失败：检查网络连接和认证配置

## 📞 **技术支持**

如在部署过程中遇到问题，请参考：
- 📖 技术手册：`CLAUDE.md`
- 🔧 API文档：项目根目录相关文档
- 🐛 问题反馈：GitHub Issues

---

**📅 更新时间**: 2025-09-17
**🔒 文档版本**: v1.0
**✅ 验证状态**: 已测试可用