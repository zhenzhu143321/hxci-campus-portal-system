# CLAUDE.md - 哈尔滨信息工程学院校园门户系统开发指南

## 🎯 项目本质 (30秒理解项目)

**哈尔滨信息工程学院校园门户系统** - Spring Boot 3.4.5 + Vue 3 + JWT双重认证
- **定位**: 学生入学第一接触点，全校信息化统一入口
- **架构**: 主通知服务(48081) + Mock School API(48082) + Vue3门户前端  
- **认证**: 工号+姓名+密码登录 + JWT Token双重认证
- **核心**: 通知发布/审批/权限控制 + 智能门户界面
- **用户**: 学生85% + 教师12% + 管理3%
- **当前进度**: 35-40%完成 (门户前端基本完成，后台管理系统0%完成)
- **核心缺失**: 后台管理系统完全未开发，影响项目实用性

**📊 项目状态获取**: 详见 todos.md (进度/任务/里程碑)

**✅ 完整的双重认证流程**

  1. 用户前台登录 (工号+姓名+密码)
     ↓
  2. Mock School API 验证用户身份 (学生/老师/校长等)
     ↓
  3. 验证通过后，生成包含用户信息的JWT Token
     ↓
  4. 前端携带Bearer Token访问主通知服务
     ↓
  5. 主通知服务验证Token并解析用户角色信息
     ↓
  6. 基于角色执行权限验证矩阵，确认操作权限
     ↓
  7. 权限通过后执行通知发布并写入数据库

## 🚨 开发流程铁律 (永远不能忘记!)

### ⚠️ **文档修改铁律** - 新增重要规则!
```cmd
修改CLAUDE.md或todos.md → 🛑 必须用户审核 → 用户确认 → 执行修改

🚨 绝对禁止: 擅自修改项目重要文档
🚨 绝对禁止: 直接更新进度评估或任务优先级  
✅ 正确做法: 提出修改建议，详细说明修改内容和原因，等待用户审核批准
```

### ⚠️ **文档修改原则** (⚠️ 严格遵守)
- **CLAUDE.md**: 技术手册和项目状态，修改需用户审核
- **todos.md**: 项目管理和任务状态，修改需用户审核
- **代码文件**: 功能实现相关，可以直接修改
- **审核流程**: 先说明 → 用户确认 → 再执行

### ⚠️ **Java代码修改后必须重启服务** - 这是最容易犯的错误!
```cmd
修改Java代码 → 编译 → 🛑 等待用户手动重启 → 用户确认 → 开始测试

🚨 绝对禁止: 编译成功后立即测试API  
🚨 绝对禁止: 自动启动服务
✅ 正确做法: 明确告知用户需要重启，等待用户确认
```

### ⚠️ **Vue前端代码修改后必须通知用户启动** - 新增铁律!
```cmd
修改Vue代码 → 编译/构建 → 🛑 通知用户手动启动前端服务 → 用户确认 → 开始测试

🚨 绝对禁止: 编译后私自运行Vue前端服务
🚨 绝对禁止: 自动启动npm run dev
✅ 正确做法: 明确通知用户需要手动启动Vue服务，等待用户确认
```

### 🔄 **用户手动重启原因** (⚠️ 除非用户特殊授权，否则禁止自动启动)
- **start命令不可靠**: 经常启动失败，无法查看错误信息
- **错误信息可见**: 手动启动可以立即看到启动问题
- **进程控制**: 用户可以随时用Ctrl+C停止服务

### 🔄 **服务重启操作** (用户手动执行)
```cmd
REM 1. 停止所有Java服务
wmic process where "name='java.exe'" delete

REM 2. 用户手动启动服务 (打开两个独立CMD窗口)
cd /d D:\ClaudeCode\AI_Web\yudao-boot-mini
mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local          # 48081
mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local # 48082
```

### 🔧 **完整开发工作流程** (🚨 严格遵守)
```
1️⃣ 代码修改前检查 → 2️⃣ 代码修改执行 → 3️⃣ 编译验证 → 
4️⃣ 等待用户重启服务 → 5️⃣ 测试验证 → 6️⃣ 文档同步更新
```

## 🚀 快速启动

### 后端服务启动 (Java)
```bash
# 一键启动脚本 (仅在用户授权时使用)
D:\ClaudeCode\AI_Web\scripts\deployment\start_all_services_complete.bat
```

### Vue前端服务启动 (Port 3000)
```bash
# 详细版启动脚本 (推荐)
D:\ClaudeCode\AI_Web\scripts\deployment\start_vue_dev_server.bat

# 快速启动版本 (简洁)
D:\ClaudeCode\AI_Web\scripts\deployment\vue_dev_quick.bat
```

**📋 分工模式**：
- **Claude负责**: 代码修改、编译、文档更新
- **用户负责**: 服务启动、重启、关闭 (Ctrl+C)
- **优势**: 用户可完全控制服务状态，出现问题能立即看到错误信息

⚠️ **重要**: 除非用户明确授权，否则必须手动启动服务！

## 🔑 关键API路径

### 主通知服务 (48081) - 生产就绪
```bash
# 核心通知API
✅ POST /admin-api/test/notification/api/publish-database  # 发布通知
✅ GET  /admin-api/test/notification/api/list              # 通知列表  
✅ POST /admin-api/test/notification/api/approve           # 批准通知
✅ POST /admin-api/test/notification/api/reject            # 拒绝通知
✅ GET  /admin-api/test/notification/api/pending-approvals # 待审批列表
✅ DELETE /admin-api/test/notification/api/delete/{id}     # 删除通知
✅ GET  /admin-api/test/notification/api/available-scopes  # 可用范围

# 天气缓存API (T12完成 - 2025-08-14)
✅ GET  /admin-api/test/weather/api/current                # 获取当前天气
✅ POST /admin-api/test/weather/api/refresh                # 手动刷新天气
✅ GET  /admin-api/test/weather/api/ping                   # 服务状态测试

# 待办通知API (T13完成 - 2025-08-15)
✅ GET  /admin-api/test/todo/api/my-list                   # 获取我的待办列表
✅ POST /admin-api/test/todo/api/{id}/complete             # 标记待办完成
✅ POST /admin-api/test/todo/api/publish                   # 发布待办通知
✅ GET  /admin-api/test/todo/api/{id}/stats                # 获取待办统计
✅ GET  /admin-api/test/todo/api/ping                      # 测试接口

# 🚨 必需请求头:
# Authorization: Bearer {jwt_token}
# Content-Type: application/json  
# tenant-id: 1                    # ⚠️ yudao框架必需!
```

### Mock School API (48082) - 生产就绪  
```bash  
# 认证API
✅ POST /mock-school-api/auth/authenticate  # 用户登录认证
✅ POST /mock-school-api/auth/user-info     # 获取用户信息
✅ POST /mock-school-api/auth/verify        # Token验证
```

## 🔐 双重认证系统

### 🔄 认证流程
1. **第一步**: Mock School API身份验证 → 获取JWT Token
2. **第二步**: 主通知服务权限验证 → 确认操作权限

### 📋 登录方式
```javascript
// 工号+姓名+密码 (新方式)
{
  "employeeId": "PRINCIPAL_001",
  "name": "校长张明", 
  "password": "admin123"
}

// 用户名+密码 (向后兼容)
{
  "username": "校长张明",
  "password": "admin123"  
}
```

### 🔑 测试账号 (永久有效)
```
系统管理员: SYSTEM_ADMIN_001 + 系统管理员 + admin123 → 1-4级发布权限(超级权限)
校长: PRINCIPAL_001 + Principal-Zhang + admin123 → 1-4级发布权限
教务主任: ACADEMIC_ADMIN_001 + Director-Li + admin123 → 2-4级发布权限(1级需审批)
教师: TEACHER_001 + Teacher-Wang + admin123 → 3-4级发布权限
班主任: CLASS_TEACHER_001 + ClassTeacher-Liu + admin123 → 3-4级发布权限
学生: STUDENT_001 + Student-Zhang + admin123 → 4级发布权限
```

### 🤖 系统角色 (非登录用户)
```
SYSTEM: 系统自动通知 → 所有用户可见 (如图书馆系统、教务系统自动生成的通知)
```

### 🏫 **学校正式API** (生产环境)
- **API地址**: `https://work.greathiit.com/api/user/loginWai`
- **请求方式**: `POST` + `Content-Type: application/json`
- **请求格式**: `{"userNumber":"工号","password":"密码","autoLogin":true,"provider":"account"}`
- **关键返回**: `grade`(年级) + `className`(班级) + `role`(角色数组)
- **令牌格式**: Basic Token (UUID) - 与系统JWT不兼容，需适配器
- **验证账号**: `2023010105/888888` (学生), `10031/888888` (教师)
- **升级状态**: ✅ 2025-08-16确认 - 返回完整班级年级信息

**集成方案**: 详见todos.md中T12.5 Mock API适配器设计

## 🔐 权限验证绝对原则 (安全第一!)

### 📋 **通知级别分类**
```
Level 1 (紧急) - 🔴 红色：校园安全警报、突发事件
Level 2 (重要) - 🟠 橙色：考试安排变更、重要政策通知  
Level 3 (常规) - 🟡 黄色：课程调整、日常管理通知
Level 4 (提醒) - 🟢 绿色：温馨提示、一般信息
```

### 🎯 **权限矩阵** (🚨 严格执行，不可降级安全标准!)

#### **📝 发布权限矩阵** - 谁可以发布什么级别的通知
| 角色 | 最高发布级别 | 可发布级别 | 审批要求 | 发布范围 |
|------|-------------|------------|----------|----------|
| **系统管理员(SYSTEM_ADMIN)** | Level 1 | 1-4级全部 | 无需审批 | 全校所有范围 + 系统管理 |
| **校长(PRINCIPAL)** | Level 1 | 1-4级全部 | 无需审批 | 全校所有范围 |
| **教务主任(ACADEMIC_ADMIN)** | Level 2 | 2-4级 | 1级需审批 | 全校/部门/年级 |
| **教师(TEACHER)** | Level 3 | 3-4级 | 无需审批 | 部门/班级 |
| **班主任(CLASS_TEACHER)** | Level 3 | 3-4级 | 无需审批 | 班级/年级 |
| **学生(STUDENT)** | Level 4 | 4级 | 无需审批 | 班级 |

#### **👁️ 查看权限矩阵** - 谁可以查看什么级别的通知

🚨 **核心原则**: 紧急通知和重要教务通知，所有相关人员(包括学生)都必须看到！

| 通知级别 | 系统管理员 | 校长 | 教务主任 | 教师 | 班主任 | 学生 | 设计理由 |
|---------|------------|------|----------|------|--------|------|----------|
| **Level 1 (紧急)** | ✅**全部** | ✅全部 | ✅全部 | ✅全部 | ✅全部 | ✅**全部** | 🚨校园安全警报学生必须知道 |
| **Level 2 (重要)** | ✅**全部** | ✅全部 | ✅全部 | ✅相关范围 | ✅相关范围 | ✅**相关范围** | 📚考试安排学生必须知道 |
| **Level 3 (常规)** | ✅**全部** | ✅全部 | ✅全部 | ✅相关范围 | ✅相关范围 | ✅**相关范围** | 📋课程调整学生必须知道 |
| **Level 4 (提醒)** | ✅**全部** | ✅全部 | ✅全部 | ✅全部 | ✅全部 | ✅**全部** | 💡温馨提示大家都能看 |

⚠️ **重大安全设计修复**: 
- 🚨 学生必须能看到紧急通知 (校园安全必需)
- 🚨 学生必须能看到考试安排 (教务必需)
- ❌ 错误设计: "学生只能看Level 4通知"

### 🌐 **范围权限控制**

#### **发布范围控制** - 谁可以向什么范围发布
| 范围 | 覆盖面 | 可发布角色 |
|------|--------|----------|
| **SCHOOL_WIDE** (全校范围) | 整个学校 | 系统管理员、校长、教务主任 |
| **DEPARTMENT** (部门范围) | 学科部门 | 系统管理员、校长、教务主任、教师 |
| **GRADE** (年级范围) | 特定年级 | 系统管理员、校长、教务主任、班主任 |
| **CLASS** (班级范围) | 具体班级 | 所有角色 |

## 📋 Windows系统开发要点

⚠️ **本系统运行在Windows环境，必须使用Windows CMD/PowerShell精确指令！**

### Windows服务管理
```cmd
# 检查端口占用
netstat -ano | findstr :48081
netstat -ano | findstr :48082

# 清理Java进程
wmic process where "name='java.exe'" delete

# 设置JVM内存参数
set "MAVEN_OPTS=-Xms256m -Xmx1024m -XX:MaxMetaspaceSize=512m"
```

### 中文支持配置
- HTML页面: `<meta charset="UTF-8">` + UTF-8编码
- 数据库: utf8字符集 + utf8_general_ci排序规则
- API调用: `'Content-Type': 'application/json; charset=utf-8'`

### 🔧 中文通知数据库插入方法 (重要!)
⚠️ **当前限制**: API传输中文存在编码问题，推荐使用直接数据库插入
```sql
# 使用utf8mb4字符集插入中文通知
"C:\tools\mysql\current\bin\mysql.exe" -u root ruoyi-vue-pro --default-character-set=utf8mb4 -e "
INSERT INTO notification_info 
(tenant_id, title, content, summary, level, status, category_id, publisher_id, publisher_name, publisher_role, target_scope, push_channels, require_confirm, pinned, creator, updater) 
VALUES 
(1, '【通知标题】', '通知内容...', '通知摘要', 4, 3, 1, 999, '系统管理员', 'SYSTEM_ADMIN', 'SCHOOL_WIDE', '1,5', 0, 0, 'system', 'system');
"

# 参数说明:
# level: 1紧急/2重要/3常规/4提醒
# status: 3已发布
# publisher_role: SYSTEM_ADMIN/PRINCIPAL/TEACHER等
# target_scope: SCHOOL_WIDE/DEPARTMENT/CLASS/GRADE
```

### 💾 **Windows文件操作关键经验** (重要!)
⚠️ **中文文件名移动操作必须使用PowerShell，CMD批处理会失败**

#### 🚨 **CMD vs PowerShell文件操作对比**
| 工具 | 中文文件名支持 | 编码支持 | 成功率 | 推荐度 |
|------|---------------|----------|--------|--------|
| **CMD批处理** | ❌ GBK编码限制 | 传统DOS编码 | 低 | 不推荐 |
| **PowerShell** | ✅ Unicode原生支持 | UTF-16 | 高 | 强烈推荐 |

#### 📋 **实战命令对比**
```powershell
# ✅ 推荐: PowerShell (成功率100%)
Move-Item '中文文件名.md' 'archive\target-dir\'
Get-ChildItem | Move-Item -Destination 'archive\target-dir\'

# ❌ 避免: CMD批处理 (中文文件名失败)
move "中文文件名.md" "archive\target-dir\"
```

#### 🎯 **核心原因分析**
1. **编码差异**: CMD使用GBK编码，PowerShell使用Unicode
2. **路径处理**: PowerShell有更智能的路径解析和.NET Framework支持
3. **错误处理**: PowerShell提供详细错误反馈，CMD可能静默失败
4. **特殊字符**: PowerShell对特殊字符和长文件名支持更好

#### ⚡ **最佳实践**
- **文件操作**: 优先使用PowerShell，特别是涉及中文文件名
- **批量处理**: 使用PowerShell脚本而非CMD批处理
- **编码环境**: 确保PowerShell环境配置UTF-8支持

## ⚡ 故障排除核心 (最常见问题)

| 问题 | 现象 | 解决方案 |
|------|------|----------|
| **🚨 代码修改不生效** | **Java代码修改后API行为无变化** | **🛑 重启服务! (最常见错误)** |
| **🚨 401错误** | 账号未登录 | 检查 `Authorization: Bearer {token}` + `tenant-id: 1` |
| **🚨 404错误** | API路径不存在 | 检查路径 `/admin-api/test/notification/` |
| **🚨 编译错误** | Maven启动失败 | 检查Maven配置和Java版本 |
| **🚨 500错误** | 服务内部错误 | 检查JVM内存参数和数据库连接 |
| **🚨 天气数据不更新** | 前端显示默认数据 | 检查API路径：`/admin-api/test/weather/api/current` |
| **🚨 前端编译失败** | npm run dev报错 | 检查Node.js版本和依赖安装 |
| **🚨 数据库连接失败** | MySQL连接异常 | 检查MySQL服务状态和连接字符串 |

### 🔄 快速重启服务
```cmd
wmic process where "name='java.exe'" delete
然后用户手动启动两个服务
```

### 🌤️ T12.6前端天气API修复详细步骤 (🔥 最高优先级)

**问题**: 前端调用错误的API路径，导致天气数据无法正确显示

**修复步骤**:
1. **文件位置**: `D:\ClaudeCode\AI_Web\hxci-campus-portal\src\api\weather.ts`
2. **修改第39行**:
   ```typescript
   // 修改前
   const response = await api.get('/admin-api/weather/current')
   
   // 修改后  
   const response = await api.get('/admin-api/test/weather/api/current')
   ```
3. **修改数据提取逻辑**:
   ```typescript
   // 修改前
   data: response.data.data,
   
   // 修改后
   data: response.data.data.weather,  // 提取weather子对象
   ```
4. **验证修复**: 重启Vue服务后检查天气显示是否为真实数据(哈尔滨 22°C)

**预期效果**: 从默认数据(-5°C) → 真实天气数据(22°C)

### 📁 **项目核心文件位置**

**项目根目录**: `D:\ClaudeCode\AI_Web\`

**🔥 T12.6修复目标文件**: `hxci-campus-portal/src/api/weather.ts`  
**🏠 首页组件**: `hxci-campus-portal/src/views/Home.vue` (2400+行)  
**🌤️ 天气组件**: `hxci-campus-portal/src/components/WeatherWidget.vue`  
**📢 通知API**: `hxci-campus-portal/src/api/notification.ts`  
**📄 已读状态**: `hxci-campus-portal/src/composables/useNotificationReadStatus.ts`

**🔧 后端API控制器**:  
- `yudao-server/src/main/java/cn/iocoder/yudao/server/controller/TempWeatherController.java` (完成)  
- `yudao-server/src/main/java/cn/iocoder/yudao/server/controller/TempNotificationController.java`

**🚀 启动脚本**:  
- `scripts/deployment/start_all_services_complete.bat` (一键启动)  
- `scripts/deployment/start_vue_dev_server.bat` (Vue启动)  
- `scripts/weather/generate-weather-jwt.py` (天气JWT生成器)

## ⚙️ 关键配置

### 📍 服务端口
- 主通知服务: 48081
- Mock School API: 48082  
- Vue前端: 3001-3002 (自动检测)

### 📊 测试数据  
- 通知数据: 6条真实数据
- 用户角色: 6种 (系统管理员/校长/教务主任/教师/班主任/学生)
- 系统角色: SYSTEM (自动通知，非登录用户)

### 📁 关键文件位置
- **Vue门户项目**: `D:\ClaudeCode\AI_Web\hxci-campus-portal`
- **后端服务**: `D:\ClaudeCode\AI_Web\yudao-boot-mini`
- **测试页面**: `D:\ClaudeCode\AI_Web\demo\phases\`

## 🌤️ 和风天气API系统 (生产就绪)

### 🔑 **和风天气JWT Token生成** (已验证)
- **生成脚本**: `D:\ClaudeCode\AI_Web\generate-jwt.py`
- **私钥文件**: `D:\ClaudeCode\AI_Web\ed25519-private.pem`
- **公钥文件**: `D:\ClaudeCode\AI_Web\ed25519-public.pem`
- **Token有效期**: 15分钟 (自动生成)
- **使用方式**: `python generate-jwt.py` 生成最新Token

#### 📋 **API配置信息**
- **专属域名**: https://kc62b63hjr.re.qweatherapi.com
- **凭据ID (kid)**: C7B7YU7RJA  
- **项目ID (sub)**: 3AE3TBK36X
- **哈尔滨城市代码**: 101050101
- **算法**: EdDSA (Ed25519)

#### 🧪 **API测试验证** (2025-08-14 20:28 成功)
```bash
# JWT Token生成
python generate-jwt.py

# API测试命令
curl -H "Authorization: Bearer {token}" --compressed \
"https://kc62b63hjr.re.qweatherapi.com/v7/weather/now?location=101050101"

# 返回数据示例 (哈尔滨实时天气)
{
  "code": "200",
  "updateTime": "2025-08-14T20:28+08:00", 
  "now": {
    "temp": "21",        # 温度21°C
    "feelsLike": "21",   # 体感温度
    "text": "晴",        # 天气状况
    "wind360": "225",    # 风向角度
    "windDir": "西南风", # 风向
    "windScale": "2",    # 风力等级
    "humidity": "75",    # 湿度75%
    "pressure": "997",   # 气压997hPa
    "vis": "30"          # 能见度30km
  }
}
```

### 🗄️ **天气数据库表** (已创建)
**表名**: `weather_cache`
```sql
-- 天气缓存表结构 (已存在)
CREATE TABLE weather_cache (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  city_code varchar(20) NOT NULL,           -- 城市代码 (101050101)
  city_name varchar(50) NOT NULL,           -- 城市名称 (哈尔滨)
  temperature int NOT NULL,                 -- 当前温度
  weather_text varchar(50) NOT NULL,        -- 天气描述 (晴/多云/雨)
  humidity int,                            -- 湿度百分比
  wind_dir varchar(20),                    -- 风向 (西南风)
  wind_scale varchar(10),                  -- 风力等级
  update_time datetime NOT NULL,           -- 数据更新时间
  api_update_time datetime NOT NULL,       -- API数据时间
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  
  UNIQUE KEY uk_city_code (city_code),      -- 每个城市一条记录
  INDEX idx_update_time (update_time)      -- 快速查询最新数据
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### ⚡ **天气缓存系统方案** (✅ T12已实现 - 2025-08-14)
**核心思路**: 后台定时任务 + 数据库缓存 + 前端API调用 + 降级机制
- **定时任务**: 每30分钟调用和风天气API更新数据（@Scheduled注解）
- **缓存API**: `/admin-api/test/weather/api/current` 返回缓存中的天气数据
- **效益**: 节省97%API调用量，50,000次/月额度可支持多城市
- **前端接口**: `src/api/weather.ts` 已完成企业级显示配置
- **降级机制**: API故障时自动返回默认天气数据，用户无感知

#### 📋 **实现细节** (TempWeatherController.java)
- **位置**: `yudao-server/src/main/java/cn/iocoder/yudao/server/controller/`
- **认证模式**: @PermitAll + @TenantIgnore + getUserInfoFromMockApi()双重认证
- **数据源**: weather_cache表 + 和风天气API + 默认数据降级
- **JWT生成**: Python脚本 `scripts/weather/generate-weather-jwt.py`
- **测试验证**: 2025-08-14 23:40成功返回哈尔滨实时天气数据

---

**📋 与todos.md协作**: CLAUDE.md = 技术手册，todos.md = 项目管理  
**📅 最后更新**: 2025年8月14日 20:30 | **维护**: Claude Code AI
