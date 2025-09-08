---
allowed-tools: Bash(netstat:*), Bash(curl:*), Bash(mysql:*), Bash(redis-cli:*), Bash(ps:*)
argument-hint: 检查指定组件 [backend|frontend|database|redis|all]
description: HXCI校园门户系统全栈健康检查
model: claude-4-sonnet
---

# HXCI校园门户系统全栈健康状态检查

**目标组件**: $ARGUMENTS

## 系统状态收集

### 服务进程状态
- 主通知服务(48081): !`netstat -tlnp | grep :48081`
- Mock School API(48082): !`netstat -tlnp | grep :48082` 
- Vue前端服务(3000): !`netstat -tlnp | grep :3000`
- Java进程详情: !`ps aux | grep java | grep -v grep`

### 基础设施状态
- 数据库连接: !`mysql -u root ruoyi-vue-pro -e "SELECT 'DATABASE_OK' as status, NOW() as check_time;"`
- Redis缓存: !`redis-cli ping`
- 磁盘空间: !`df -h | head -5`
- 内存使用: !`free -m`

### 关键API健康检查
- 主服务Ping: !`curl -s -o /dev/null -w "%{http_code}" http://localhost:48081/admin-api/test/notification/api/ping || echo "FAILED"`
- Mock API Ping: !`curl -s -o /dev/null -w "%{http_code}" http://localhost:48082/mock-school-api/ping || echo "FAILED"`

## 专业诊断分析

使用 **devops-troubleshooter** subagent 进行系统健康分析：

**分析任务**: "基于上述收集的系统状态信息，分析HXCI校园门户系统的健康状态。重点关注：
1. 三大核心服务(48081主服务/48082 Mock API/3000前端)的运行状态
2. 数据库和Redis缓存的连接状态
3. 系统资源使用情况(内存/磁盘)
4. API接口的响应状态
5. 发现的问题和修复建议

目标组件: $ARGUMENTS

请提供结构化的健康报告，包含：
- ✅ 正常运行的组件
- ⚠️  存在问题的组件  
- 🔧 具体修复建议
- 📊 关键性能指标"

## 自动化修复建议

如果发现问题，提供一键修复命令：
- 重启服务: `sudo pkill -f java && 用户手动启动服务`
- 清理缓存: `redis-cli flushdb`  
- 检查日志: `tail -50 /home/ecs-assist-user/logs/yudao-server.log`

**执行方式**: `/hxci-health all` 或 `/hxci-health backend`