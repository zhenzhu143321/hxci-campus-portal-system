# 🐧 Linux系统环境搭建完整指南

## 📋 项目概述（新Claude必读）

**项目名称**: 哈尔滨信息工程学院校园门户系统  
**当前状态**: Windows开发环境 → Linux生产环境迁移  
**技术栈**: Spring Boot 3.4.5 + Vue 3 + MySQL 8.x + JWT双重认证  
**GitHub仓库**: https://github.com/zhenzhu143321/hxci-campus-portal-system.git  
**完成度**: 35-40% (后端基本完成，前端门户完成，后台管理0%)

## 🎯 任务目标

你需要在Linux系统上重建整个开发环境，包括：
1. 克隆GitHub项目代码
2. 安装和配置所有依赖环境
3. 启动双服务架构（主通知服务48081 + Mock认证服务48082）
4. 配置Vue前端开发环境
5. 验证整个系统运行正常

## 🔧 第一步：系统基础环境安装

### 1.1 更新系统包管理器
```bash
# Ubuntu/Debian系统
sudo apt update && sudo apt upgrade -y

# CentOS/RHEL系统  
sudo yum update -y
# 或者（CentOS 8+）
sudo dnf update -y
```

### 1.2 安装Git和基础工具
```bash
# Ubuntu/Debian
sudo apt install -y git curl wget vim unzip tree htop

# CentOS/RHEL
sudo yum install -y git curl wget vim unzip tree htop
```

## ☕ 第二步：Java开发环境

### 2.1 安装JDK 17
```bash
# Ubuntu/Debian - 安装OpenJDK 17
sudo apt install -y openjdk-17-jdk openjdk-17-jre

# CentOS/RHEL - 安装OpenJDK 17
sudo yum install -y java-17-openjdk java-17-openjdk-devel

# 验证Java版本
java -version
javac -version
```

### 2.2 配置Java环境变量
```bash
# 找到Java安装路径
sudo find /usr -name "java" -type f 2>/dev/null | grep bin

# 编辑环境变量文件
sudo vim /etc/environment

# 添加以下内容（根据实际路径调整）
JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
PATH="$PATH:$JAVA_HOME/bin"

# 重新加载环境变量
source /etc/environment

# 验证JAVA_HOME
echo $JAVA_HOME
```

### 2.3 安装Maven
```bash
# Ubuntu/Debian
sudo apt install -y maven

# CentOS/RHEL
sudo yum install -y maven

# 验证Maven版本
mvn -version
```

## 🗄️ 第三步：MySQL数据库环境

### 3.1 安装MySQL 8.x
```bash
# Ubuntu/Debian
sudo apt install -y mysql-server mysql-client

# CentOS/RHEL
sudo yum install -y mysql-server mysql

# 启动MySQL服务
sudo systemctl start mysql
sudo systemctl enable mysql
```

### 3.2 配置MySQL安全设置
```bash
# 运行安全配置向导
sudo mysql_secure_installation

# 设置root密码（建议使用强密码）
# 移除匿名用户: Y
# 禁止root远程登录: N（开发环境可以选N）
# 移除test数据库: Y
# 重新加载权限表: Y
```

### 3.3 创建项目数据库
```bash
# 登录MySQL
sudo mysql -u root -p

# 在MySQL命令行中执行
CREATE DATABASE `ruoyi-vue-pro` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW DATABASES;
EXIT;
```

### 3.4 配置MySQL字符集（重要！）
```bash
# 编辑MySQL配置文件
sudo vim /etc/mysql/mysql.conf.d/mysqld.cnf

# 在[mysqld]部分添加
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='+8:00'

# 重启MySQL服务
sudo systemctl restart mysql

# 验证字符集配置
mysql -u root -p -e "SHOW VARIABLES LIKE 'character%';"
```

## 🌐 第四步：Node.js和前端环境

### 4.1 安装Node.js和npm
```bash
# 使用NodeSource仓库安装最新LTS版本
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt install -y nodejs

# 或者使用官方包管理器
# Ubuntu/Debian
sudo apt install -y nodejs npm

# CentOS/RHEL
sudo yum install -y nodejs npm

# 验证版本
node --version
npm --version
```

### 4.2 配置npm镜像源（可选，提升下载速度）
```bash
# 配置淘宝镜像源
npm config set registry https://registry.npmmirror.com/

# 验证配置
npm config get registry
```

## 🐍 第五步：Python环境（天气API需要）

### 5.1 安装Python和pip
```bash
# Ubuntu/Debian
sudo apt install -y python3 python3-pip python3-venv

# CentOS/RHEL
sudo yum install -y python3 python3-pip

# 验证Python版本
python3 --version
pip3 --version
```

### 5.2 安装JWT生成依赖
```bash
# 安装PyJWT库（天气API JWT生成需要）
pip3 install PyJWT

# 验证安装
python3 -c "import jwt; print('PyJWT installed successfully')"
```

## 📥 第六步：克隆项目代码

### 6.1 选择项目目录
```bash
# 创建项目目录
mkdir -p ~/projects
cd ~/projects

# 或者使用 /opt 目录（需要sudo权限）
sudo mkdir -p /opt/hxci-campus
sudo chown $USER:$USER /opt/hxci-campus
cd /opt/hxci-campus
```

### 6.2 克隆GitHub仓库
```bash
# 克隆项目（替换为实际的GitHub用户名）
git clone https://github.com/zhenzhu143321/hxci-campus-portal-system.git

# 进入项目目录
cd hxci-campus-portal-system

# 检查项目结构
tree -L 2 -a

# 查看项目状态
git status
git log --oneline -5
```

## 🛠️ 第七步：项目环境配置

### 7.1 修改配置文件路径
```bash
# 进入Java后端项目
cd yudao-boot-mini

# 查找配置文件
find . -name "application*.yaml" -type f

# 检查并修改数据库连接配置
vim yudao-server/src/main/resources/application-local.yaml

# 确认MySQL连接配置正确：
# spring:
#   datasource:
#     url: jdbc:mysql://localhost:3306/ruoyi-vue-pro?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
#     username: root
#     password: YOUR_MYSQL_PASSWORD
```

### 7.2 修改天气JWT生成脚本路径
```bash
# 查找天气控制器文件
find . -name "*Weather*Controller.java" -type f

# 编辑天气控制器，修改Python脚本路径
vim yudao-server/src/main/java/cn/iocoder/yudao/server/controller/TempWeatherController.java

# 查找并修改以下行（约第XXX行）：
# 从: String scriptPath = "D:/ClaudeCode/AI_Web/scripts/weather/generate-weather-jwt.py";
# 改为: String scriptPath = "/path/to/your/project/scripts/weather/generate-weather-jwt.py";

# 获取当前项目绝对路径
pwd
# 使用实际路径替换上面的 /path/to/your/project
```

### 7.3 检查和修改Python脚本权限
```bash
# 给Python脚本执行权限
chmod +x scripts/weather/generate-weather-jwt.py

# 测试Python脚本是否能正常运行
cd scripts/weather
python3 generate-weather-jwt.py

# 检查是否成功生成JWT Token
```

## 💾 第八步：初始化数据库

### 8.1 导入基础数据库结构
```bash
# 进入项目根目录
cd ~/projects/hxci-campus-portal-system  # 或你的实际路径

# 查找SQL初始化脚本
find . -name "*.sql" -type f | grep -E "(init|mysql|ruoyi)"

# 如果有现成的SQL文件，导入到数据库
mysql -u root -p ruoyi-vue-pro < yudao-boot-mini/sql/mysql/ruoyi-vue-pro.sql

# 或者手动执行一些基础表创建（参考archive/database/目录下的SQL文件）
```

### 8.2 创建必要的系统表
```bash
# 登录MySQL创建基础表结构
mysql -u root -p ruoyi-vue-pro

# 在MySQL中执行以下基础表创建（示例）
```

```sql
-- 创建系统用户表（简化版）
CREATE TABLE system_users (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username varchar(30) NOT NULL COMMENT '用户账号',
    password varchar(100) DEFAULT '' COMMENT '密码',
    nickname varchar(30) NOT NULL COMMENT '用户昵称',
    status tinyint NOT NULL DEFAULT 0 COMMENT '帐号状态（0正常 1停用）',
    creator varchar(64) DEFAULT '' COMMENT '创建者',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id bigint NOT NULL DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 插入测试管理员用户
INSERT INTO system_users (id, username, password, nickname, status, creator, tenant_id) VALUES 
(1, 'admin', '$2a$04$KljJDa/LK7QfDm0lF5OhuePhlPfjRH3tB2Wu351Uidz.oQGJXevPi', 'Administrator', 0, 'admin', 1);

-- 创建通知信息表
CREATE TABLE notification_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知编号',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    summary VARCHAR(500) COMMENT '通知摘要',
    level TINYINT NOT NULL DEFAULT 3 COMMENT '通知级别：1紧急,2重要,3常规,4提醒',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '通知状态',
    publisher_id BIGINT NOT NULL COMMENT '发布者ID',
    publisher_name VARCHAR(50) NOT NULL COMMENT '发布者姓名',
    publisher_role VARCHAR(30) COMMENT '发布者角色',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能通知信息表';

-- 创建Mock学校用户表
CREATE TABLE mock_school_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    user_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'School系统用户ID',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    token VARCHAR(100) NOT NULL COMMENT '认证Token',
    token_expires_at DATETIME NOT NULL COMMENT 'Token过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mock School 用户表';

-- 插入测试用户数据
INSERT INTO mock_school_users 
(username, user_id, role_code, role_name, enabled, token, token_expires_at) 
VALUES 
('Principal-Zhang', 'PRINCIPAL_001', 'PRINCIPAL', 'Principal', TRUE, 'YD_SCHOOL_PRINCIPAL_001', DATE_ADD(NOW(), INTERVAL 1 YEAR)),
('Director-Li', 'ACADEMIC_ADMIN_001', 'ACADEMIC_ADMIN', 'Academic Director', TRUE, 'YD_SCHOOL_ACADEMIC_ADMIN_001', DATE_ADD(NOW(), INTERVAL 1 YEAR)),
('Teacher-Wang', 'TEACHER_001', 'TEACHER', 'Teacher', TRUE, 'YD_SCHOOL_TEACHER_001', DATE_ADD(NOW(), INTERVAL 1 YEAR)),
('Student-Zhang', 'STUDENT_001', 'STUDENT', 'Student', TRUE, 'YD_SCHOOL_STUDENT_001', DATE_ADD(NOW(), INTERVAL 1 YEAR));

EXIT;
```

## 🚀 第九步：启动后端服务

### 9.1 编译Java项目
```bash
# 进入Java项目目录
cd ~/projects/hxci-campus-portal-system/yudao-boot-mini

# 清理并编译项目
mvn clean compile

# 如果编译有问题，尝试跳过测试
mvn clean compile -DskipTests

# 检查编译结果
echo "编译完成，准备启动服务..."
```

### 9.2 启动主通知服务（端口48081）
```bash
# 在第一个终端窗口启动主服务
cd ~/projects/hxci-campus-portal-system/yudao-boot-mini

# 启动主通知服务（spring.profiles.active=local）
mvn spring-boot:run -pl yudao-server -Dspring-boot.run.profiles=local

# 等待服务启动完成，看到类似以下日志：
# Started YudaoServerApplication in X.X seconds
# 服务将运行在 http://localhost:48081
```

### 9.3 启动Mock认证服务（端口48082）
```bash
# 打开第二个终端窗口
cd ~/projects/hxci-campus-portal-system/yudao-boot-mini

# 启动Mock School API服务
mvn spring-boot:run -pl yudao-mock-school-api -Dspring-boot.run.profiles=local

# 等待服务启动完成，看到类似以下日志：
# Started MockSchoolApiApplication in X.X seconds  
# 服务将运行在 http://localhost:48082
```

## 🌐 第十步：启动前端Vue服务

### 10.1 安装前端依赖
```bash
# 打开第三个终端窗口，进入Vue项目目录
cd ~/projects/hxci-campus-portal-system/hxci-campus-portal

# 安装依赖包（可能需要几分钟）
npm install

# 如果npm install失败，尝试清除缓存
npm cache clean --force
npm install
```

### 10.2 启动Vue开发服务器
```bash
# 在Vue项目目录中启动开发服务器
npm run dev

# Vue服务将启动在 http://localhost:3000 或 http://localhost:3001
# 浏览器会自动打开，或手动访问显示的地址
```

## ✅ 第十一步：系统验证测试

### 11.1 验证后端API服务
```bash
# 测试主通知服务健康检查
curl http://localhost:48081/admin-api/test/notification/api/ping

# 测试Mock认证服务健康检查  
curl http://localhost:48082/mock-school-api/auth/ping

# 预期响应都应该返回成功状态
```

### 11.2 验证前端登录功能
```bash
# 在浏览器中访问 http://localhost:3000（或实际端口）
# 使用测试账号登录：
# 工号: PRINCIPAL_001
# 姓名: Principal-Zhang  
# 密码: admin123

# 登录成功后应该能看到校园门户首页
```

### 11.3 验证通知发布功能
```bash
# 登录后在前端界面测试发布通知功能
# 或者使用curl测试API（需要先登录获取Token）

# 测试认证API
curl -X POST http://localhost:48082/mock-school-api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "PRINCIPAL_001",
    "name": "Principal-Zhang",
    "password": "admin123"
  }'

# 使用返回的Token测试通知列表API
curl -X GET http://localhost:48081/admin-api/test/notification/api/list \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "tenant-id: 1"
```

## 🐛 第十二步：常见问题排查

### 12.1 Java服务启动失败
```bash
# 检查Java版本
java -version  # 确保是JDK 17

# 检查端口占用
netstat -tulpn | grep :48081
netstat -tulpn | grep :48082

# 杀死占用端口的进程
sudo kill -9 $(lsof -t -i:48081)
sudo kill -9 $(lsof -t -i:48082)

# 检查MySQL连接
mysql -u root -p -e "SELECT 1"
```

### 12.2 MySQL连接问题
```bash
# 检查MySQL服务状态
sudo systemctl status mysql

# 重启MySQL服务
sudo systemctl restart mysql

# 检查MySQL端口
netstat -tulpn | grep :3306

# 检查MySQL用户权限
mysql -u root -p -e "SELECT user,host FROM mysql.user;"
```

### 12.3 前端启动问题
```bash
# 检查Node.js版本
node --version  # 建议 >= 16.x

# 清除npm缓存和node_modules
rm -rf node_modules package-lock.json
npm cache clean --force
npm install

# 检查Vue服务端口
netstat -tulpn | grep :3000
```

### 12.4 Python脚本问题
```bash
# 检查Python和PyJWT安装
python3 --version
python3 -c "import jwt; print('PyJWT OK')"

# 手动测试JWT生成
cd scripts/weather
python3 generate-weather-jwt.py

# 检查脚本权限
ls -la generate-weather-jwt.py
chmod +x generate-weather-jwt.py
```

## 📚 第十三步：开发环境工具推荐

### 13.1 安装常用开发工具
```bash
# 安装VS Code（如果需要图形界面开发）
# Ubuntu/Debian
sudo snap install code --classic

# 或者下载deb包安装
wget -qO- https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > packages.microsoft.gpg
sudo install -o root -g root -m 644 packages.microsoft.gpg /etc/apt/trusted.gpg.d/
sudo sh -c 'echo "deb [arch=amd64,arm64,armhf signed-by=/etc/apt/trusted.gpg.d/packages.microsoft.gpg] https://packages.microsoft.com/repos/code stable main" > /etc/apt/sources.list.d/vscode.list'
sudo apt update
sudo apt install code
```

### 13.2 配置开发环境别名
```bash
# 编辑bash配置文件
vim ~/.bashrc

# 添加有用的别名
alias ll='ls -alF'
alias la='ls -A'  
alias l='ls -CF'
alias ..='cd ..'
alias ...='cd ../..'
alias grep='grep --color=auto'
alias fgrep='fgrep --color=auto'
alias egrep='egrep --color=auto'

# 项目相关别名
alias cdproj='cd ~/projects/hxci-campus-portal-system'
alias startweb='cd ~/projects/hxci-campus-portal-system/hxci-campus-portal && npm run dev'
alias startapi='cd ~/projects/hxci-campus-portal-system/yudao-boot-mini'

# 重新加载配置
source ~/.bashrc
```

## 🎯 第十四步：项目核心信息速查

### 14.1 关键API端点
```
# 主通知服务 (48081)
POST /admin-api/test/notification/api/publish-database  # 发布通知
GET  /admin-api/test/notification/api/list              # 通知列表
GET  /admin-api/test/weather/api/current                # 天气数据

# Mock认证服务 (48082)  
POST /mock-school-api/auth/authenticate                 # 用户登录
POST /mock-school-api/auth/user-info                    # 用户信息

# 必需请求头
Authorization: Bearer {jwt_token}
Content-Type: application/json
tenant-id: 1
```

### 14.2 测试账号信息
```
校长: PRINCIPAL_001 + Principal-Zhang + admin123
教务主任: ACADEMIC_ADMIN_001 + Director-Li + admin123  
教师: TEACHER_001 + Teacher-Wang + admin123
学生: STUDENT_001 + Student-Zhang + admin123
```

### 14.3 项目目录结构
```
hxci-campus-portal-system/
├── CLAUDE.md                    # 项目技术手册
├── todos.md                     # 任务管理文档
├── yudao-boot-mini/            # 后端Java项目
│   ├── yudao-server/           # 主通知服务(48081)
│   └── yudao-mock-school-api/  # Mock认证服务(48082)
├── hxci-campus-portal/         # Vue3前端项目
├── scripts/weather/            # 天气JWT生成脚本
└── archive/                    # 归档文档
```

## 🚨 重要提醒

1. **⚠️ 服务启动顺序**: MySQL → 后端两个服务 → 前端服务
2. **⚠️ 端口确认**: 48081(主服务) + 48082(认证) + 3000(前端)
3. **⚠️ 数据库字符集**: 必须使用utf8mb4支持中文
4. **⚠️ JWT脚本路径**: 天气控制器中的Python脚本路径需要修改为Linux路径
5. **⚠️ 文件权限**: 给Python脚本和shell脚本执行权限

## 🎉 环境搭建完成检查清单

- [ ] ✅ JDK 17安装并配置环境变量
- [ ] ✅ Maven安装并能正常编译
- [ ] ✅ MySQL 8.x安装并创建ruoyi-vue-pro数据库
- [ ] ✅ Node.js和npm安装并配置
- [ ] ✅ Python3和PyJWT安装
- [ ] ✅ 项目代码克隆并检查目录结构
- [ ] ✅ 配置文件路径修改（数据库连接、Python脚本路径）
- [ ] ✅ 基础数据表创建和测试数据插入
- [ ] ✅ 主通知服务(48081)启动成功
- [ ] ✅ Mock认证服务(48082)启动成功  
- [ ] ✅ Vue前端服务启动成功
- [ ] ✅ 前端登录功能测试通过
- [ ] ✅ API接口基础功能验证通过

## 📞 后续开发指导

环境搭建完成后，你可以：

1. **查看项目文档**: 详细阅读 `CLAUDE.md` 了解项目技术架构
2. **查看任务状态**: 检查 `todos.md` 了解当前开发进度
3. **继续开发**: 根据todos.md中的任务优先级继续开发
4. **代码提交**: 开发完成后提交代码到GitHub仓库

**🤖 Good luck with the development! 新的Claude Code AI，加油！**

---
**文档创建时间**: 2025年8月16日  
**适用环境**: Ubuntu 20.04+, CentOS 8+, 其他主流Linux发行版  
**维护人**: Claude Code AI (Windows版)  
**目标用户**: Claude Code AI (Linux版)