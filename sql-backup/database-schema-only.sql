-- MySQL dump 10.13  Distrib 8.0.43, for Linux (x86_64)
--
-- Host: localhost    Database: ruoyi-vue-pro
-- ------------------------------------------------------
-- Server version	8.0.43-0ubuntu0.22.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `infra_api_access_log`
--

DROP TABLE IF EXISTS `infra_api_access_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `infra_api_access_log` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '编号',
  `trace_id` varchar(64) NOT NULL COMMENT '链路追踪编号',
  `user_id` int NOT NULL DEFAULT '0' COMMENT '用户编号',
  `user_type` tinyint NOT NULL DEFAULT '0' COMMENT '用户类型',
  `application_name` varchar(50) NOT NULL COMMENT '应用名',
  `request_method` varchar(16) NOT NULL COMMENT '请求方法名',
  `request_url` varchar(255) NOT NULL COMMENT '请求地址',
  `request_params` text COMMENT '请求参数',
  `user_ip` varchar(50) NOT NULL COMMENT '用户 IP',
  `user_agent` varchar(512) NOT NULL COMMENT '浏览器 UA',
  `begin_time` datetime NOT NULL COMMENT '开始请求时间',
  `end_time` datetime NOT NULL COMMENT '结束请求时间',
  `duration` int NOT NULL COMMENT '执行时长',
  `result_code` int NOT NULL DEFAULT '0' COMMENT '结果码',
  `result_msg` varchar(512) DEFAULT '' COMMENT '结果提示',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API访问日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `infra_api_error_log`
--

DROP TABLE IF EXISTS `infra_api_error_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `infra_api_error_log` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '编号',
  `trace_id` varchar(64) NOT NULL COMMENT '链路追踪编号',
  `user_id` int NOT NULL DEFAULT '0' COMMENT '用户编号',
  `user_type` tinyint NOT NULL DEFAULT '0' COMMENT '用户类型',
  `application_name` varchar(50) NOT NULL COMMENT '应用名',
  `request_method` varchar(16) NOT NULL COMMENT '请求方法名',
  `request_url` varchar(255) NOT NULL COMMENT '请求地址',
  `request_params` text COMMENT '请求参数',
  `user_ip` varchar(50) NOT NULL COMMENT '用户 IP',
  `user_agent` varchar(512) NOT NULL COMMENT '浏览器 UA',
  `exception_time` datetime NOT NULL COMMENT '异常发生时间',
  `exception_name` varchar(128) NOT NULL DEFAULT '' COMMENT '异常名',
  `exception_message` text NOT NULL COMMENT '异常导致的消息',
  `exception_root_cause_message` text NOT NULL COMMENT '异常导致的根消息',
  `exception_stack_trace` text NOT NULL COMMENT '异常的栈轨迹',
  `exception_class_name` varchar(512) NOT NULL COMMENT '异常发生的类全名',
  `exception_file_name` varchar(512) NOT NULL COMMENT '异常发生的类文件',
  `exception_method_name` varchar(512) NOT NULL COMMENT '异常发生的方法名',
  `exception_line_number` int NOT NULL COMMENT '异常发生的方法所在行',
  `process_status` tinyint NOT NULL COMMENT '处理状态',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `process_user_id` int DEFAULT '0' COMMENT '处理用户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统异常日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mock_role_permissions`
--

DROP TABLE IF EXISTS `mock_role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mock_role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approval_required` bit(1) DEFAULT NULL,
  `create_time` datetime(6) NOT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notification_levels` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `permission_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `permission_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_scope` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5q7q0b1orjo9hqr7hsihjcpqj` (`role_code`,`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mock_school_users`
--

DROP TABLE IF EXISTS `mock_school_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mock_school_users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `user_id` varchar(50) NOT NULL COMMENT 'School系统用户ID',
  `role_code` varchar(30) NOT NULL COMMENT '角色编码',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `token` varchar(200) DEFAULT NULL,
  `token_expires_at` datetime NOT NULL COMMENT 'Token过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `department_id` bigint DEFAULT NULL,
  `department_name` varchar(100) DEFAULT NULL,
  `grade_id` varchar(20) DEFAULT NULL,
  `class_id` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mock School 用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_info`
--

DROP TABLE IF EXISTS `notification_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `summary` varchar(500) DEFAULT NULL COMMENT '通知摘要',
  `level` tinyint NOT NULL DEFAULT '3' COMMENT '通知级别：1紧急,2重要,3常规,4提醒',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '通知状态',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `publisher_name` varchar(50) NOT NULL COMMENT '发布者姓名',
  `publisher_role` varchar(30) DEFAULT NULL COMMENT '发布者角色',
  `target_scope` varchar(50) DEFAULT 'SCHOOL_WIDE',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `todo_priority` tinyint DEFAULT NULL COMMENT '待办优先级：1=low,2=medium,3=high',
  `todo_deadline` datetime DEFAULT NULL COMMENT '待办截止时间',
  `todo_status` tinyint DEFAULT NULL COMMENT '待办状态：0=pending,1=in_progress,2=completed,3=overdue',
  `target_grade` varchar(10) DEFAULT NULL COMMENT '目标年级(如2023,2024)',
  `target_class` varchar(20) DEFAULT NULL COMMENT '目标班级(如2023-1,2024-2)',
  `target_department` varchar(50) DEFAULT NULL COMMENT '目标部门(如计算机系,数学系)',
  `approver_id` bigint DEFAULT NULL COMMENT '审批者ID',
  `approver_name` varchar(50) DEFAULT NULL COMMENT '审批者姓名',
  `approval_status` varchar(20) DEFAULT NULL COMMENT '审批状态',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_comment` text COMMENT '审批意见',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_target_grade` (`target_grade`),
  KEY `idx_target_class` (`target_class`),
  KEY `idx_target_department` (`target_department`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_updater` (`updater`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能通知信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_oauth2_access_token`
--

DROP TABLE IF EXISTS `system_oauth2_access_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_oauth2_access_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `user_type` tinyint NOT NULL COMMENT '用户类型',
  `user_info` varchar(512) NOT NULL COMMENT '用户信息',
  `access_token` varchar(255) NOT NULL COMMENT '访问令牌',
  `refresh_token` varchar(255) NOT NULL COMMENT '刷新令牌',
  `client_id` varchar(255) NOT NULL COMMENT '客户端编号',
  `scopes` varchar(255) DEFAULT NULL COMMENT '授权范围',
  `expires_time` datetime NOT NULL COMMENT '过期时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_access_token` (`access_token`),
  KEY `idx_refresh_token` (`refresh_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OAuth2 访问令牌';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_oauth2_refresh_token`
--

DROP TABLE IF EXISTS `system_oauth2_refresh_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_oauth2_refresh_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `user_type` tinyint NOT NULL COMMENT '用户类型',
  `refresh_token` varchar(255) NOT NULL COMMENT '刷新令牌',
  `client_id` varchar(255) NOT NULL COMMENT '客户端编号',
  `scopes` varchar(255) DEFAULT NULL COMMENT '授权范围',
  `expires_time` datetime NOT NULL COMMENT '过期时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OAuth2 刷新令牌';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_tenant`
--

DROP TABLE IF EXISTS `system_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户编号',
  `name` varchar(30) NOT NULL COMMENT '租户名',
  `contact_user_id` bigint DEFAULT NULL COMMENT '联系人的用户编号',
  `contact_name` varchar(30) NOT NULL COMMENT '联系人',
  `contact_mobile` varchar(500) DEFAULT NULL COMMENT '联系手机',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '租户状态（0正常 1停用）',
  `website` varchar(256) DEFAULT '' COMMENT '绑定域名',
  `package_id` bigint NOT NULL COMMENT '租户套餐编号',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `account_count` int NOT NULL COMMENT '账号数量',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_users`
--

DROP TABLE IF EXISTS `system_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(30) NOT NULL COMMENT '用户账号',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `nickname` varchar(30) NOT NULL COMMENT '用户昵称',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `todo_completions`
--

DROP TABLE IF EXISTS `todo_completions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `todo_completions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '完成记录ID',
  `todo_id` bigint NOT NULL COMMENT '待办通知ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) NOT NULL COMMENT '用户姓名',
  `user_role` varchar(30) NOT NULL COMMENT '用户角色',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0未读，1已读，2已完成，3已隐藏',
  `read_at` datetime DEFAULT NULL COMMENT '已读时间',
  `completed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
  `hidden_at` datetime DEFAULT NULL COMMENT '隐藏时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_user` (`todo_id`,`user_id`),
  UNIQUE KEY `uniq_user_todo` (`tenant_id`,`user_id`,`todo_id`),
  KEY `idx_todo_id` (`todo_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_completed_time` (`completed_time`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_todo_status` (`todo_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='待办完成记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `todo_notifications`
--

DROP TABLE IF EXISTS `todo_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `todo_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '待办通知ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `title` varchar(200) NOT NULL COMMENT '待办标题',
  `content` text NOT NULL COMMENT '待办内容',
  `summary` varchar(500) DEFAULT NULL COMMENT '待办摘要',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '优先级：1=low,2=medium,3=high',
  `deadline` datetime NOT NULL COMMENT '截止时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=pending,1=in_progress,2=completed,3=overdue',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `publisher_name` varchar(50) NOT NULL COMMENT '发布者姓名',
  `publisher_role` varchar(30) NOT NULL COMMENT '发布者角色',
  `target_scope` varchar(50) NOT NULL DEFAULT 'CLASS' COMMENT '目标范围',
  `target_student_ids` text COMMENT '目标学生ID列表(JSON格式,如: ["2023010105","2023010106"])',
  `target_grade_ids` text COMMENT '目标年级ID列表(JSON格式)',
  `target_class_ids` text COMMENT '目标班级ID列表(JSON格式)',
  `target_department_ids` text,
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `push_channels` varchar(100) DEFAULT NULL COMMENT '推送渠道',
  `require_confirm` tinyint DEFAULT '1' COMMENT '是否需要确认',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_priority` (`priority`),
  KEY `idx_status` (`status`),
  KEY `idx_deadline` (`deadline`),
  KEY `idx_publisher_role` (`publisher_role`),
  KEY `idx_target_scope` (`target_scope`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立的待办通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `weather_cache`
--

DROP TABLE IF EXISTS `weather_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `weather_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city_code` varchar(20) NOT NULL,
  `city_name` varchar(50) NOT NULL,
  `temperature` int NOT NULL,
  `weather_text` varchar(50) NOT NULL,
  `humidity` int DEFAULT NULL,
  `wind_dir` varchar(20) DEFAULT NULL,
  `wind_scale` varchar(10) DEFAULT NULL,
  `update_time` datetime NOT NULL,
  `api_update_time` datetime NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_city_code` (`city_code`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=953 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-17 10:25:09
