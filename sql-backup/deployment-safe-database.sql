-- ===================================================================
-- 哈尔滨信息工程学院校园门户系统 - 安全部署数据库脚本
-- ===================================================================
--
-- 📋 **重要说明**:
-- 1. 本文件是从原始SQL文件中清理敏感信息后的安全版本
-- 2. 移除了所有API密钥、访问令牌等敏感配置信息
-- 3. 适用于新环境部署和团队协作
-- 4. 包含完整的数据库结构定义，无敏感数据
--
-- 🚨 **注意事项**:
-- - 请根据实际环境配置相应的API密钥
-- - 生产环境部署前请检查所有配置项
-- - 本文件仅包含数据库结构，不含业务数据
--
-- 📅 创建时间: 2025-09-17
-- 🔒 安全等级: 可公开分享
-- ===================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================================
-- 核心通知系统表
-- ===================================================================

-- 通知信息表
DROP TABLE IF EXISTS `notification_info`;
CREATE TABLE `notification_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text COMMENT '通知内容',
  `summary` varchar(500) DEFAULT NULL COMMENT '通知摘要',
  `level` tinyint NOT NULL DEFAULT '4' COMMENT '通知级别：1紧急 2重要 3常规 4提醒',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1草稿 2待审批 3已发布 4已撤回',
  `category_id` bigint DEFAULT NULL COMMENT '通知分类ID',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `publisher_name` varchar(100) NOT NULL COMMENT '发布者姓名',
  `publisher_role` varchar(50) NOT NULL COMMENT '发布者角色',
  `target_scope` varchar(50) NOT NULL DEFAULT 'SCHOOL_WIDE' COMMENT '目标范围',
  `push_channels` varchar(200) DEFAULT NULL COMMENT '推送渠道',
  `require_confirm` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要确认阅读',
  `pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶',
  `scheduled_time` datetime DEFAULT NULL COMMENT '定时发布时间',
  `expired_time` datetime DEFAULT NULL COMMENT '过期时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审批人ID',
  `approved_time` datetime DEFAULT NULL COMMENT '审批时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知信息表';

-- 待办通知表
DROP TABLE IF EXISTS `todo_notifications`;
CREATE TABLE `todo_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `title` varchar(200) NOT NULL COMMENT '待办标题',
  `content` text COMMENT '待办内容',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '优先级：1高 2中 3低',
  `due_date` datetime DEFAULT NULL COMMENT '截止时间',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `publisher_name` varchar(100) NOT NULL COMMENT '发布者姓名',
  `target_scope` varchar(50) NOT NULL DEFAULT 'SCHOOL_WIDE' COMMENT '目标范围',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1活跃 2完成 3取消',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办通知表';

-- 待办完成状态表
DROP TABLE IF EXISTS `todo_completion`;
CREATE TABLE `todo_completion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `todo_id` bigint NOT NULL COMMENT '待办ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `completed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否完成',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `hidden` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号(乐观锁)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_user` (`todo_id`,`user_id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_todo_id` (`todo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办完成状态表';

-- ===================================================================
-- 天气缓存系统表
-- ===================================================================

-- 天气缓存表
DROP TABLE IF EXISTS `weather_cache`;
CREATE TABLE `weather_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `city_code` varchar(20) NOT NULL COMMENT '城市代码',
  `city_name` varchar(50) NOT NULL COMMENT '城市名称',
  `temperature` int NOT NULL COMMENT '当前温度',
  `weather_text` varchar(50) NOT NULL COMMENT '天气描述',
  `humidity` int DEFAULT NULL COMMENT '湿度百分比',
  `wind_dir` varchar(20) DEFAULT NULL COMMENT '风向',
  `wind_scale` varchar(10) DEFAULT NULL COMMENT '风力等级',
  `update_time` datetime NOT NULL COMMENT '数据更新时间',
  `api_update_time` datetime NOT NULL COMMENT 'API数据时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_city_code` (`city_code`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='天气缓存表';

-- ===================================================================
-- 基础测试数据插入
-- ===================================================================

-- 插入基础通知数据（测试用途）
INSERT INTO `notification_info`
(`tenant_id`, `title`, `content`, `summary`, `level`, `status`, `category_id`, `publisher_id`, `publisher_name`, `publisher_role`, `target_scope`, `push_channels`, `require_confirm`, `pinned`, `creator`, `updater`)
VALUES
(1, '【系统通知】欢迎使用校园门户系统', '欢迎使用哈尔滨信息工程学院校园门户系统！本系统为全校师生提供便捷的信息服务。', '系统欢迎信息', 4, 3, 1, 999, '系统管理员', 'SYSTEM_ADMIN', 'SCHOOL_WIDE', '1,5', 0, 1, 'system', 'system'),
(1, '【重要提醒】请及时关注校园通知', '请全校师生及时关注校园通知系统，重要信息将通过本平台发布。', '关注通知提醒', 2, 3, 1, 999, '系统管理员', 'SYSTEM_ADMIN', 'SCHOOL_WIDE', '1,5', 1, 0, 'system', 'system');

-- 插入基础待办数据（测试用途）
INSERT INTO `todo_notifications`
(`tenant_id`, `title`, `content`, `priority`, `publisher_id`, `publisher_name`, `target_scope`, `status`, `creator`, `updater`)
VALUES
(1, '完善个人信息', '请登录系统完善您的个人信息，确保信息准确性。', 2, 999, '系统管理员', 'SCHOOL_WIDE', 1, 'system', 'system'),
(1, '阅读用户手册', '请仔细阅读系统用户手册，了解各项功能使用方法。', 3, 999, '系统管理员', 'SCHOOL_WIDE', 1, 'system', 'system');

-- 插入基础天气数据（哈尔滨）
INSERT INTO `weather_cache`
(`city_code`, `city_name`, `temperature`, `weather_text`, `humidity`, `wind_dir`, `wind_scale`, `update_time`, `api_update_time`)
VALUES
('101050101', '哈尔滨', 22, '晴', 65, '西南风', '2', NOW(), NOW());

-- ===================================================================
-- 结束设置
-- ===================================================================

SET FOREIGN_KEY_CHECKS = 1;

-- ===================================================================
-- 部署后操作提醒
-- ===================================================================
/*
🚀 **部署后必须执行的操作**:

1. **API密钥配置**:
   - 配置和风天气API密钥（src/main/resources/application.yml）
   - 配置学校API接口地址和认证信息
   - 配置JWT密钥和CSRF密钥

2. **数据库权限**:
   - 创建应用专用数据库用户
   - 设置适当的数据库权限
   - 检查字符集设置（推荐utf8mb4）

3. **测试验证**:
   - 运行健康检查: curl http://localhost:48081/admin-api/test/notification/api/ping
   - 验证认证服务: curl http://localhost:48082/mock-school-api/ping
   - 测试通知发布功能

4. **生产配置**:
   - 修改默认密码
   - 配置日志级别
   - 设置监控和告警

📚 **相关文档**:
- 技术手册: CLAUDE.md
- 部署指南: documentation/
- API文档: 项目根目录
*/