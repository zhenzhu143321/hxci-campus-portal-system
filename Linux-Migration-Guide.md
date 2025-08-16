# Linux系统开发迁移完整指南

## 🎯 项目迁移概述

**项目名称**: 哈尔滨信息工程学院校园门户系统  
**技术栈**: Spring Boot 3.4.5 + Vue 3 + MySQL 8 + Redis  
**GitHub仓库**: https://github.com/zhenzhu143321/hxci-campus-portal-system.git  
**Windows → Linux**: 完整开发环境迁移方案  

---

## 📋 迁移前准备清单

### ✅ 确认信息
- [ ] Linux发行版 (推荐: Ubuntu 22.04 LTS / CentOS 8+ / AlmaLinux 9)
- [ ] 网络连接 (需要访问GitHub和外网)
- [ ] 用户权限 (sudo权限或root权限)
- [ ] 磁盘空间 (至少5GB可用空间)

### 📦 需要安装的软件
- [ ] Git 2.34+
- [ ] OpenJDK 17
- [ ] Node.js 18+ (LTS)
- [ ] Maven 3.8+
- [ ] MySQL 8.0
- [ ] Redis 6.2+
- [ ] Python 3.8+ (天气API需要)

---

## 🚀 第一阶段: Linux系统环境准备

### 1.1 系统更新 (必需)
```bash
# Ubuntu/Debian系统
sudo apt update && sudo apt upgrade -y

# CentOS/RHEL/AlmaLinux系统
sudo dnf update -y
# 或者 (老版本)
sudo yum update -y
```

### 1.2 安装基础开发工具
```bash
# Ubuntu/Debian
sudo apt install -y curl wget git vim build-essential software-properties-common

# CentOS/RHEL/AlmaLinux
sudo dnf groupinstall -y "Development Tools"
sudo dnf install -y curl wget git vim epel-release
```

### 1.3 创建项目目录
```bash
# 创建统一的项目目录
sudo mkdir -p /opt/hxci-campus
sudo chown $USER:$USER /opt/hxci-campus
cd /opt/hxci-campus

# 或者使用用户目录 (推荐)
mkdir -p $HOME/Projects/hxci-campus
cd $HOME/Projects/hxci-campus
```

---

## ☕ 第二阶段: Java开发环境配置

### 2.1 安装OpenJDK 17
```bash
# Ubuntu/Debian
sudo apt install -y openjdk-17-jdk openjdk-17-jre

# CentOS/RHEL/AlmaLinux
sudo dnf install -y java-17-openjdk java-17-openjdk-devel

# 验证安装
java -version
javac -version
```

### 2.2 配置JAVA_HOME环境变量
```bash
# 查找Java安装路径
sudo find /usr -name "java" -type f 2>/dev/null | head -5

# 编辑环境变量配置文件
vim ~/.bashrc

# 在文件末尾添加:
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # Ubuntu路径
# export JAVA_HOME=/usr/lib/jvm/java-17-openjdk      # CentOS路径
export PATH=$JAVA_HOME/bin:$PATH

# 重新加载配置
source ~/.bashrc

# 验证配置
echo $JAVA_HOME
```

### 2.3 安装Maven 3.8+
```bash
# 方法1: 使用包管理器 (推荐)
# Ubuntu/Debian
sudo apt install -y maven

# CentOS/RHEL/AlmaLinux
sudo dnf install -y maven

# 方法2: 手动安装最新版本
cd /tmp
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
sudo tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/
sudo ln -s /opt/apache-maven-3.9.6 /opt/maven

# 添加到PATH (如果手动安装)
echo 'export MAVEN_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# 验证安装
mvn -version
```

---

## 🗄️ 第三阶段: 数据库环境配置

### 3.1 安装MySQL 8.0
```bash
# Ubuntu/Debian
sudo apt install -y mysql-server mysql-client

# CentOS/RHEL/AlmaLinux
sudo dnf install -y mysql-server mysql

# 启动MySQL服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

### 3.2 MySQL初始配置
```bash
# 登录MySQL (首次可能需要sudo)
sudo mysql -u root -p

# 在MySQL中执行:
```

```sql
-- 创建数据库
CREATE DATABASE `ruoyi-vue-pro` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户 (替换为您的密码)
CREATE USER 'hxci_user'@'localhost' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON `ruoyi-vue-pro`.* TO 'hxci_user'@'localhost';
FLUSH PRIVILEGES;

-- 验证创建
SHOW DATABASES;
SELECT user, host FROM mysql.user WHERE user = 'hxci_user';
```

### 3.3 安装Redis
```bash
# Ubuntu/Debian
sudo apt install -y redis-server

# CentOS/RHEL/AlmaLinux
sudo dnf install -y redis

# 启动Redis服务
sudo systemctl start redis
sudo systemctl enable redis

# 验证Redis
redis-cli ping
# 应该返回: PONG
```

---

## 🌐 第四阶段: Node.js前端环境

### 4.1 安装Node.js 18 LTS
```bash
# 方法1: 使用NodeSource仓库 (推荐)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs  # Ubuntu/Debian

# CentOS/RHEL/AlmaLinux
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo dnf install -y nodejs

# 方法2: 使用包管理器
# Ubuntu/Debian (可能版本较旧)
sudo apt install -y nodejs npm

# 验证安装
node --version  # 应该显示 v18.x.x
npm --version
```

### 4.2 配置npm和安装包管理器
```bash
# 设置npm镜像源 (国内用户推荐)
npm config set registry https://registry.npmmirror.com

# 安装pnpm (可选, 更快的包管理器)
npm install -g pnpm

# 验证配置
npm config get registry
```

---

## 🐍 第五阶段: Python环境 (天气API需要)

### 5.1 安装Python 3.8+
```bash
# Ubuntu/Debian
sudo apt install -y python3 python3-pip python3-venv

# CentOS/RHEL/AlmaLinux
sudo dnf install -y python3 python3-pip

# 验证安装
python3 --version
pip3 --version
```

### 5.2 安装Python依赖
```bash
# 安装JWT库 (天气API需要)
pip3 install PyJWT

# 验证安装
python3 -c "import jwt; print('PyJWT安装成功')"
```

---

## 📥 第六阶段: 项目代码获取

### 6.1 克隆GitHub仓库
```bash
# 进入项目目录
cd $HOME/Projects/hxci-campus

# 克隆代码
git clone https://github.com/zhenzhu143321/hxci-campus-portal-system.git
cd hxci-campus-portal-system

# 查看项目结构
ls -la
```

### 6.2 验证项目完整性
```bash
# 检查关键目录是否存在
ls -la yudao-boot-mini/
ls -la hxci-campus-portal/
ls -la scripts/weather/

# 检查天气API密钥文件
ls -la scripts/weather/ed25519-*.pem
ls -la scripts/weather/generate-weather-jwt.py
```

---

## 🔧 第七阶段: 后端服务配置

### 7.1 数据库初始化
```bash
# 进入项目根目录
cd $HOME/Projects/hxci-campus/hxci-campus-portal-system

# 导入数据库结构 (如果有SQL文件)
# 查找SQL初始化文件
find . -name "*.sql" -type f

# 导入SQL (根据实际文件路径调整)
mysql -u hxci_user -p ruoyi-vue-pro < yudao-boot-mini/sql/mysql/ruoyi-vue-pro.sql
# 输入之前设置的数据库密码
```

### 7.2 修改数据库配置
```bash
# 编辑主服务配置
vim yudao-boot-mini/yudao-server/src/main/resources/application-local.yaml

# 修改数据库连接信息:
```

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ruoyi-vue-pro?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: hxci_user
    password: your_strong_password  # 替换为您设置的密码
    driver-class-name: com.mysql.cj.jdbc.Driver

  redis:
    host: localhost
    port: 6379
    password:     # 如果Redis设置了密码则填写
    database: 0
```

### 7.3 修改Mock School API配置
```bash
# 编辑Mock API配置
vim yudao-boot-mini/yudao-mock-school-api/src/main/resources/application.yaml

# 确认端口和数据库配置正确
```

---

## 🚀 第八阶段: 服务启动脚本创建

### 8.1 创建后端启动脚本
```bash
# 创建脚本目录
mkdir -p scripts/linux

# 创建主通知服务启动脚本
cat > scripts/linux/start-main-service.sh << 'EOF'
#!/bin/bash
echo "🚀 启动主通知服务 (端口48081)..."
cd $(dirname $0)/../../yudao-boot-mini
export MAVEN_OPTS="-Xms256m -Xmx1024m -XX:MaxMetaspaceSize=512m"
mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local
EOF

# 创建Mock API启动脚本
cat > scripts/linux/start-mock-api.sh << 'EOF'
#!/bin/bash
echo "🚀 启动Mock School API (端口48082)..."
cd $(dirname $0)/../../yudao-boot-mini
export MAVEN_OPTS="-Xms256m -Xmx512m"
mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local
EOF

# 设置执行权限
chmod +x scripts/linux/start-*.sh
```

### 8.2 创建前端启动脚本
```bash
# 创建Vue前端启动脚本
cat > scripts/linux/start-vue-frontend.sh << 'EOF'
#!/bin/bash
echo "🌐 启动Vue前端开发服务器..."
cd $(dirname $0)/../../hxci-campus-portal

# 检查是否已安装依赖
if [ ! -d "node_modules" ]; then
    echo "📦 首次运行，安装依赖包..."
    npm install
fi

echo "🚀 启动开发服务器 (http://localhost:3000)..."
npm run dev
EOF

chmod +x scripts/linux/start-vue-frontend.sh
```

### 8.3 创建一键启动脚本
```bash
# 创建全服务启动脚本 (使用tmux或screen)
cat > scripts/linux/start-all-services.sh << 'EOF'
#!/bin/bash

echo "🚀 启动哈尔滨信息工程学院校园门户系统"
echo "=========================================="

# 检查tmux是否安装
if ! command -v tmux &> /dev/null; then
    echo "📦 安装tmux..."
    sudo apt install -y tmux 2>/dev/null || sudo dnf install -y tmux 2>/dev/null
fi

# 创建tmux会话
SESSION_NAME="hxci-campus"

# 杀死已存在的会话
tmux kill-session -t $SESSION_NAME 2>/dev/null

# 创建新会话
tmux new-session -d -s $SESSION_NAME

# 创建窗口并启动服务
tmux rename-window -t $SESSION_NAME:0 'main-service'
tmux send-keys -t $SESSION_NAME:0 'cd /opt/hxci-campus/hxci-campus-portal-system && ./scripts/linux/start-main-service.sh' C-m

tmux new-window -t $SESSION_NAME -n 'mock-api'
tmux send-keys -t $SESSION_NAME:1 './scripts/linux/start-mock-api.sh' C-m

tmux new-window -t $SESSION_NAME -n 'vue-frontend'
tmux send-keys -t $SESSION_NAME:2 './scripts/linux/start-vue-frontend.sh' C-m

echo "✅ 所有服务已在tmux会话中启动"
echo "📋 使用以下命令管理:"
echo "   tmux attach -t $SESSION_NAME     # 连接到会话"
echo "   tmux list-sessions               # 查看所有会话"
echo "   tmux kill-session -t $SESSION_NAME  # 关闭所有服务"
echo ""
echo "🌐 访问地址:"
echo "   前端门户: http://localhost:3000"
echo "   主服务API: http://localhost:48081"
echo "   Mock API: http://localhost:48082"
EOF

chmod +x scripts/linux/start-all-services.sh
```

---

## 🧪 第九阶段: 系统测试验证

### 9.1 数据库连接测试
```bash
# 测试MySQL连接
mysql -u hxci_user -p -e "SELECT 'MySQL连接成功' as status; SHOW DATABASES;"

# 测试Redis连接
redis-cli ping
```

### 9.2 天气API测试
```bash
# 测试Python JWT生成
cd scripts/weather
python3 generate-weather-jwt.py

# 如果成功应该看到生成的JWT Token
```

### 9.3 项目编译测试
```bash
# 测试后端编译
cd yudao-boot-mini
mvn clean compile -pl yudao-server
mvn clean compile -pl yudao-mock-school-api

# 测试前端编译
cd ../hxci-campus-portal
npm install
npm run build
```

---

## 🔥 第十阶段: 服务启动和验证

### 10.1 启动所有服务
```bash
# 进入项目根目录
cd $HOME/Projects/hxci-campus/hxci-campus-portal-system

# 启动所有服务
./scripts/linux/start-all-services.sh

# 等待30-60秒让服务完全启动
```

### 10.2 验证服务状态
```bash
# 检查端口占用
netstat -tlnp | grep -E ':(3000|48081|48082)'

# 或使用ss命令
ss -tlnp | grep -E ':(3000|48081|48082)'

# 测试API响应
curl -s http://localhost:48081/doc.html  # Swagger文档
curl -s http://localhost:48082/mock-school-api/auth/ping  # Mock API健康检查
curl -s http://localhost:3000  # 前端页面
```

### 10.3 查看服务日志
```bash
# 连接到tmux会话查看日志
tmux attach -t hxci-campus

# 在tmux中切换窗口
# Ctrl+B, 然后按数字键 0/1/2 切换到不同服务
# Ctrl+B, d 退出tmux但保持服务运行
```

---

## 🛡️ 第十一阶段: 安全和优化配置

### 11.1 防火墙配置
```bash
# Ubuntu/Debian (ufw)
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 3000/tcp    # Vue前端
sudo ufw allow 48081/tcp   # 主服务
sudo ufw allow 48082/tcp   # Mock API
sudo ufw --force enable

# CentOS/RHEL/AlmaLinux (firewalld)
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --permanent --add-port=48081/tcp
sudo firewall-cmd --permanent --add-port=48082/tcp
sudo firewall-cmd --reload
```

### 11.2 系统服务配置 (可选)
```bash
# 创建systemd服务文件
sudo tee /etc/systemd/system/hxci-campus.service > /dev/null << EOF
[Unit]
Description=HXCI Campus Portal System
After=network.target mysql.service redis.service

[Service]
Type=forking
User=$USER
WorkingDirectory=$HOME/Projects/hxci-campus/hxci-campus-portal-system
ExecStart=$HOME/Projects/hxci-campus/hxci-campus-portal-system/scripts/linux/start-all-services.sh
ExecStop=/usr/bin/tmux kill-session -t hxci-campus
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# 启用服务
sudo systemctl daemon-reload
sudo systemctl enable hxci-campus
```

### 11.3 日志配置
```bash
# 创建日志目录
mkdir -p logs

# 配置logrotate (可选)
sudo tee /etc/logrotate.d/hxci-campus > /dev/null << EOF
$HOME/Projects/hxci-campus/hxci-campus-portal-system/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    copytruncate
}
EOF
```

---

## 📚 第十二阶段: 开发工具配置

### 12.1 安装代码编辑器
```bash
# 安装VS Code (推荐)
wget -qO- https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > packages.microsoft.gpg
sudo install -o root -g root -m 644 packages.microsoft.gpg /etc/apt/trusted.gpg.d/
sudo sh -c 'echo "deb [arch=amd64,arm64,armhf signed-by=/etc/apt/trusted.gpg.d/packages.microsoft.gpg] https://packages.microsoft.com/repos/code stable main" > /etc/apt/sources.list.d/vscode.list'
sudo apt update
sudo apt install code

# 或安装vim增强配置
sudo apt install vim-gtk3  # 支持系统剪贴板
```

### 12.2 配置Git
```bash
# 配置Git用户信息
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 配置Git编辑器
git config --global core.editor vim

# 查看配置
git config --list
```

---

## 🔧 故障排除指南

### 常见问题解决方案

#### 1. 端口被占用
```bash
# 查找占用进程
sudo lsof -i :48081
sudo lsof -i :48082
sudo lsof -i :3000

# 杀死进程
sudo kill -9 <PID>
```

#### 2. 数据库连接失败
```bash
# 检查MySQL服务
sudo systemctl status mysql
sudo systemctl restart mysql

# 检查用户权限
mysql -u root -p -e "SHOW GRANTS FOR 'hxci_user'@'localhost';"
```

#### 3. 内存不足
```bash
# 增加交换空间
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 永久启用
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

#### 4. Maven下载缓慢
```bash
# 配置阿里云镜像
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>alimaven</id>
      <name>aliyun maven</name>
      <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
```

---

## ✅ 迁移完成验证清单

### 系统验证
- [ ] ✅ Linux系统更新完成
- [ ] ✅ Java 17环境配置成功 (`java -version`)
- [ ] ✅ Maven 3.8+安装成功 (`mvn -version`)
- [ ] ✅ Node.js 18安装成功 (`node --version`)
- [ ] ✅ MySQL 8.0服务运行 (`sudo systemctl status mysql`)
- [ ] ✅ Redis服务运行 (`redis-cli ping`)
- [ ] ✅ Python 3.8+和PyJWT安装 (`python3 -c "import jwt"`)

### 项目验证
- [ ] ✅ GitHub项目克隆成功
- [ ] ✅ 数据库初始化完成
- [ ] ✅ 天气API密钥文件存在
- [ ] ✅ 后端编译成功
- [ ] ✅ 前端依赖安装成功

### 服务验证
- [ ] ✅ 主通知服务启动 (端口48081)
- [ ] ✅ Mock School API启动 (端口48082)
- [ ] ✅ Vue前端服务启动 (端口3000)
- [ ] ✅ API接口响应正常
- [ ] ✅ 前端页面访问正常

### 功能验证
- [ ] ✅ 用户登录功能
- [ ] ✅ 通知发布功能
- [ ] ✅ 权限验证功能
- [ ] ✅ 天气数据显示

---

## 🎯 开发建议

### 推荐Linux发行版
1. **Ubuntu 22.04 LTS** - 最佳兼容性，丰富文档
2. **CentOS Stream 9** - 企业级稳定性
3. **AlmaLinux 9** - CentOS替代方案

### 性能优化建议
1. **内存**: 最少4GB，推荐8GB+
2. **CPU**: 2核心以上
3. **存储**: SSD硬盘，15GB+可用空间
4. **网络**: 稳定的互联网连接

### 开发工具推荐
1. **IDE**: IntelliJ IDEA / VS Code
2. **数据库管理**: DBeaver / phpMyAdmin
3. **API测试**: Postman / Insomnia
4. **版本控制**: Git + GitHub Desktop (可选)

---

## 📞 技术支持

如果在迁移过程中遇到问题，请检查：

1. **系统日志**: `journalctl -f`
2. **服务状态**: `tmux attach -t hxci-campus`
3. **端口占用**: `netstat -tlnp`
4. **磁盘空间**: `df -h`
5. **内存使用**: `free -h`

---

**🎉 恭喜！Linux开发环境迁移完成！**

现在您可以在Linux系统上继续开发哈尔滨信息工程学院校园门户系统了。

---
*文档生成时间: 2025年8月16日*  
*系统支持: Ubuntu 22.04+ / CentOS 8+ / AlmaLinux 9+*  
*项目版本: Spring Boot 3.4.5 + Vue 3*