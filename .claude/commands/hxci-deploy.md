---
allowed-tools: Bash(mvn:*), Bash(npm:*), Bash(git:*), Bash(curl:*), Bash(netstat:*), Bash(pkill:*), Bash(ps:*), Bash(echo:*), Bash(cd:*), Bash(ls:*), Bash(wc:*), Bash(grep:*)
argument-hint: 部署指定功能模块 [backend|frontend|security|api|all]
description: HXCI校园门户系统安全部署工作流
model: claude-4-sonnet
---

# HXCI校园门户系统安全部署流程

**部署目标**: $ARGUMENTS

## 部署前状态检查

### Git仓库状态
- 当前分支和提交: !`git log --oneline -1`
- 工作区状态: !`git status --porcelain`
- 远程同步: !`git fetch && git status`

### 当前系统状态
- 运行服务: !`netstat -tlnp | grep -E ":(48081|48082|3000)"`
- Java进程: !`ps aux | grep java | grep -v grep | wc -l`

## 编译和构建阶段

使用 **deployment-engineer** subagent 执行构建流程：

**构建任务**: "执行HXCI校园门户系统的安全编译流程，部署目标: $ARGUMENTS

### 后端编译 (如果backend相关)
1. 清理缓存: `mvn clean -f yudao-boot-mini/pom.xml`
2. 编译验证: `mvn compile -f yudao-boot-mini/pom.xml -pl yudao-server`
3. 测试运行: `mvn test -f yudao-boot-mini/pom.xml -pl yudao-server`

### 前端构建 (如果frontend相关)  
1. 依赖检查: `cd hxci-campus-portal && npm ls --depth=0`
2. 构建验证: `cd hxci-campus-portal && npm run build --if-present`
3. 开发环境: `cd hxci-campus-portal && npm run dev --if-present`

**安全检查要求**:
- 确保没有编译错误和警告
- 验证关键依赖版本无漏洞
- 检查构建产物完整性"

## 服务重启阶段

**重启流程**:
1. 优雅停止服务: !`pkill -f "spring-boot:run" 2>/dev/null || echo "没有运行中的服务"`
2. 验证进程清理: !`ps aux | grep java | grep -v grep || echo "清理完成"`
3. 等待用户手动启动服务 (遵循CLAUDE.md铁律)

**⚠️ 用户手动启动提醒**: 
```bash
# 用户需要打开两个终端窗口执行:
cd /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini
mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local          # 48081
mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local # 48082
```

## 部署验证阶段

使用 **test-automator** subagent 执行部署验证：

**验证任务**: "对HXCI校园门户系统进行部署后功能验证，验证目标: $ARGUMENTS

### API连通性验证
1. 主服务健康检查: `curl http://localhost:48081/admin-api/test/notification/api/ping`
2. Mock API健康检查: `curl http://localhost:48082/mock-school-api/ping`  
3. 前端访问验证: `curl -I http://localhost:3000` (如果前端已启动)

### 核心功能验证
1. 认证流程: 测试JWT Token获取
2. 权限系统: 验证权限缓存正常工作
3. 数据库连接: 验证通知数据读写
4. Redis缓存: 验证缓存服务正常

**验证标准**:
- 所有API返回200状态码
- 核心业务流程无错误
- 性能指标在正常范围内"

## 部署完成和状态记录

### Git状态更新 (如需要)
- 提交更改: 根据部署情况决定是否需要Git提交
- 推送更新: 同步到远程仓库

### 部署日志记录
- 记录部署时间、版本、验证结果
- 更新 @CURRENT_WORK_STATUS.md 项目状态

**执行方式**: `/hxci-deploy backend` 或 `/hxci-deploy all`