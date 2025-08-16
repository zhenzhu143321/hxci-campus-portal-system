# 智能通知系统数据库设计方案

## 📋 项目概述

基于yudao-boot-mini框架的多租户智能通知系统，支持25+角色权限体系、4级通知分类、多渠道推送、工作流审批、实时统计等复杂业务场景。

### 核心技术栈
- **数据库**: MySQL 8.0+
- **ORM**: MyBatis Plus
- **架构**: 多租户SaaS架构
- **缓存**: Redis集群
- **消息队列**: Kafka + Redis Streams

### 业务特点
- **高并发**: 支持10万+用户同时推送
- **复杂权限**: 6层级组织架构权限控制
- **多租户**: 数据完全隔离
- **实时性**: 推送延迟 < 5秒

## 🏗️ 数据库架构设计

### 分库分表策略

```sql
-- 分库策略：按租户ID分库
Database Sharding:
├── notification_tenant_0  (tenant_id % 4 = 0)
├── notification_tenant_1  (tenant_id % 4 = 1)
├── notification_tenant_2  (tenant_id % 4 = 2)
└── notification_tenant_3  (tenant_id % 4 = 3)

-- 分表策略：按业务特点分表
Table Sharding:
├── 通知表：按月分表 (notification_YYYYMM)
├── 推送记录表：按日分表 (push_record_YYYYMMDD)  
├── 统计表：按周分表 (statistics_YYYY_WW)
└── 用户行为表：按月分表 (user_behavior_YYYYMM)
```

### 读写分离策略

```yaml
读写分离配置:
  主库 (Master):
    - 所有写操作
    - 实时读操作
    - 事务操作
  
  从库 (Slave):
    - 一般查询操作
    - 统计分析查询  
    - 报表生成查询
    - 只读副本: 3个节点
```

## 📊 核心业务表结构

### 1. 系统管理模块

#### 1.1 租户管理表

```sql
-- 租户信息表
CREATE TABLE `system_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户编号',
  `name` varchar(30) NOT NULL COMMENT '租户名称',
  `contact_user_id` bigint DEFAULT NULL COMMENT '联系人用户编号',
  `contact_name` varchar(30) NOT NULL COMMENT '联系人',
  `contact_mobile` varchar(500) DEFAULT NULL COMMENT '联系手机',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '租户状态：0正常 1停用',
  `domain` varchar(256) DEFAULT NULL COMMENT '绑定域名',
  `package_id` bigint NOT NULL COMMENT '租户套餐编号',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `account_count` int NOT NULL COMMENT '账号数量',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB COMMENT='租户表';

-- 租户套餐表
CREATE TABLE `system_tenant_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐编号',
  `name` varchar(30) NOT NULL COMMENT '套餐名称',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `menu_ids` varchar(4096) NOT NULL COMMENT '关联的菜单编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='租户套餐表';
```

#### 1.2 用户权限表

```sql
-- 用户信息表
CREATE TABLE `system_users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `username` varchar(30) NOT NULL COMMENT '用户账号',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `nickname` varchar(30) NOT NULL COMMENT '用户昵称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `post_ids` varchar(255) DEFAULT NULL COMMENT '岗位编号数组',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `mobile` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` tinyint DEFAULT '0' COMMENT '用户性别：0男 1女 2未知',
  `avatar` varchar(512) DEFAULT '' COMMENT '头像地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '帐号状态：0停用 1正常',
  `login_ip` varchar(50) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`,`username`,`update_time`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='用户信息表';

-- 角色信息表  
CREATE TABLE `system_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` varchar(30) NOT NULL COMMENT '角色名称',
  `code` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据范围：1全部 2自定义 3本部门 4本部门及以下 5仅本人',
  `data_scope_dept_ids` varchar(500) DEFAULT '' COMMENT '数据范围(指定部门数组)',
  `status` tinyint NOT NULL COMMENT '角色状态：0正常 1停用',
  `type` tinyint NOT NULL COMMENT '角色类型：1内置角色 2自定义角色',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`,`code`,`deleted`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='角色信息表';

-- 用户角色关联表
CREATE TABLE `system_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增编号',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user_id` (`tenant_id`,`user_id`),
  KEY `idx_tenant_role_id` (`tenant_id`,`role_id`)
) ENGINE=InnoDB COMMENT='用户和角色关联表';

-- 菜单权限表
CREATE TABLE `system_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `name` varchar(50) NOT NULL COMMENT '菜单名称',
  `permission` varchar(100) NOT NULL DEFAULT '' COMMENT '权限标识',
  `type` tinyint NOT NULL COMMENT '菜单类型：1目录 2菜单 3按钮',
  `sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `component_name` varchar(255) DEFAULT NULL COMMENT '组件名',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '菜单状态：0正常 1停用',
  `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否可见',
  `keep_alive` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否缓存',
  `always_show` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否总是显示',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB COMMENT='菜单权限表';

-- 角色菜单关联表
CREATE TABLE `system_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增编号',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB COMMENT='角色和菜单关联表';

-- 部门表
CREATE TABLE `system_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` varchar(30) NOT NULL DEFAULT '' COMMENT '部门名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门id',
  `sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `leader_user_id` bigint DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NOT NULL COMMENT '部门状态：0正常 1停用',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
  KEY `idx_leader` (`leader_user_id`)
) ENGINE=InnoDB COMMENT='部门表';
```

### 2. 通知核心模块

#### 2.1 通知管理表

```sql
-- 通知分类表
CREATE TABLE `notification_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `code` varchar(50) NOT NULL COMMENT '分类编码',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类ID',
  `icon` varchar(100) DEFAULT NULL COMMENT '分类图标',
  `color` varchar(20) DEFAULT NULL COMMENT '分类颜色',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`, `deleted`),
  KEY `idx_tenant_parent` (`tenant_id`, `parent_id`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB COMMENT='通知分类表';

-- 通知模板表
CREATE TABLE `notification_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `code` varchar(100) NOT NULL COMMENT '模板编码',
  `title_template` varchar(200) NOT NULL COMMENT '标题模板',
  `content_template` text NOT NULL COMMENT '内容模板',
  `type` tinyint NOT NULL DEFAULT '3' COMMENT '通知类型：1紧急 2重要 3常规 4提醒',
  `priority` tinyint NOT NULL DEFAULT '5' COMMENT '优先级：1-10',
  `push_channels` varchar(100) NOT NULL DEFAULT 'app,site' COMMENT '推送渠道：app,sms,email,site',
  `confirm_required` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要确认：0否 1是',
  `variables` json DEFAULT NULL COMMENT '模板变量配置',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`, `deleted`),
  KEY `idx_tenant_category` (`tenant_id`, `category_id`),
  KEY `idx_type_priority` (`type`, `priority`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='通知模板表';

-- 通知信息表（按月分表）
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `template_id` bigint DEFAULT NULL COMMENT '模板ID',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` longtext NOT NULL COMMENT '通知内容',
  `content_html` longtext DEFAULT NULL COMMENT '富文本内容',
  `type` tinyint NOT NULL DEFAULT '3' COMMENT '通知类型：1紧急 2重要 3常规 4提醒',
  `priority` tinyint NOT NULL DEFAULT '5' COMMENT '优先级：1-10',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `publisher_name` varchar(50) NOT NULL COMMENT '发布者姓名',
  `publisher_dept_id` bigint DEFAULT NULL COMMENT '发布者部门ID',
  `target_type` tinyint NOT NULL DEFAULT '1' COMMENT '目标类型：1全部 2角色 3部门 4用户组 5指定用户',
  `target_config` json NOT NULL COMMENT '目标配置',
  `target_count` int NOT NULL DEFAULT '0' COMMENT '目标用户数量',
  `push_channels` varchar(100) NOT NULL DEFAULT 'app,site' COMMENT '推送渠道',
  `confirm_required` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要确认',
  `auto_confirm_time` int DEFAULT NULL COMMENT '自动确认时间(分钟)',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `scheduled_time` datetime DEFAULT NULL COMMENT '定时发送时间',
  `attachments` json DEFAULT NULL COMMENT '附件信息',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1待审批 2审批通过 3已发布 4已暂停 5已过期 6已撤回',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `push_count` int NOT NULL DEFAULT '0' COMMENT '推送数量',
  `read_count` int NOT NULL DEFAULT '0' COMMENT '已读数量',
  `confirm_count` int NOT NULL DEFAULT '0' COMMENT '确认数量',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type_time` (`tenant_id`, `type`, `create_time`),
  KEY `idx_tenant_publisher` (`tenant_id`, `publisher_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_category` (`tenant_id`, `category_id`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_scheduled_time` (`scheduled_time`)
) ENGINE=InnoDB COMMENT='通知信息表'
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202407 VALUES LESS THAN (TO_DAYS('2024-08-01')),
    PARTITION p202408 VALUES LESS THAN (TO_DAYS('2024-09-01')),
    PARTITION p202409 VALUES LESS THAN (TO_DAYS('2024-10-01')),
    PARTITION p202410 VALUES LESS THAN (TO_DAYS('2024-11-01')),
    PARTITION p202411 VALUES LESS THAN (TO_DAYS('2024-12-01')),
    PARTITION p202412 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### 2.2 推送记录表

```sql
-- 推送记录表（按日分表）
CREATE TABLE `push_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `notification_id` bigint NOT NULL COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) NOT NULL COMMENT '用户姓名',
  `user_mobile` varchar(20) DEFAULT NULL COMMENT '用户手机',
  `user_email` varchar(100) DEFAULT NULL COMMENT '用户邮箱',
  `channel` varchar(20) NOT NULL COMMENT '推送渠道：app,sms,email,site',
  `channel_config` json DEFAULT NULL COMMENT '渠道配置',
  `push_time` datetime NOT NULL COMMENT '推送时间',
  `push_content` text NOT NULL COMMENT '推送内容',
  `push_result` json DEFAULT NULL COMMENT '推送结果',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `read_ip` varchar(50) DEFAULT NULL COMMENT '阅读IP',
  `read_device` varchar(100) DEFAULT NULL COMMENT '阅读设备',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `confirm_ip` varchar(50) DEFAULT NULL COMMENT '确认IP',
  `confirm_remark` varchar(200) DEFAULT NULL COMMENT '确认备注',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0推送中 1推送成功 2推送失败 3已读 4已确认 5已过期',
  `retry_count` tinyint NOT NULL DEFAULT '0' COMMENT '重试次数',
  `retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `error_code` varchar(50) DEFAULT NULL COMMENT '错误码',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `cost_time` int DEFAULT NULL COMMENT '耗时(毫秒)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_notification_user` (`tenant_id`, `notification_id`, `user_id`),
  KEY `idx_tenant_user_time` (`tenant_id`, `user_id`, `push_time`),
  KEY `idx_tenant_channel_status` (`tenant_id`, `channel`, `status`),
  KEY `idx_status_retry` (`status`, `retry_count`, `retry_time`),
  KEY `idx_push_time` (`push_time`),
  KEY `idx_read_time` (`read_time`),
  KEY `idx_confirm_time` (`confirm_time`)
) ENGINE=InnoDB COMMENT='推送记录表'
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p20240725 VALUES LESS THAN (TO_DAYS('2024-07-26')),
    PARTITION p20240726 VALUES LESS THAN (TO_DAYS('2024-07-27')),
    PARTITION p20240727 VALUES LESS THAN (TO_DAYS('2024-07-28')),
    PARTITION p20240728 VALUES LESS THAN (TO_DAYS('2024-07-29')),
    PARTITION p20240729 VALUES LESS THAN (TO_DAYS('2024-07-30')),
    PARTITION p20240730 VALUES LESS THAN (TO_DAYS('2024-07-31')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 推送任务表
CREATE TABLE `push_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `notification_id` bigint NOT NULL COMMENT '通知ID',
  `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型：1立即推送 2定时推送 3重试推送',
  `target_users` longtext NOT NULL COMMENT '目标用户列表JSON',
  `target_count` int NOT NULL DEFAULT '0' COMMENT '目标用户数量',
  `push_channels` varchar(100) NOT NULL COMMENT '推送渠道',
  `priority` tinyint NOT NULL DEFAULT '5' COMMENT '任务优先级：1-10',
  `scheduled_time` datetime DEFAULT NULL COMMENT '计划执行时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始执行时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束执行时间',
  `progress` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '执行进度(%)',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功数量',
  `failed_count` int NOT NULL DEFAULT '0' COMMENT '失败数量',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0待执行 1执行中 2执行成功 3执行失败 4已取消',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `retry_count` tinyint NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retry_count` tinyint NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  `executor` varchar(50) DEFAULT NULL COMMENT '执行器',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_notification` (`tenant_id`, `notification_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_scheduled_time` (`scheduled_time`),
  KEY `idx_priority_status` (`priority`, `status`),
  KEY `idx_executor` (`executor`)
) ENGINE=InnoDB COMMENT='推送任务表';
```

### 3. 工作流审批模块

#### 3.1 工作流定义表

```sql
-- 审批流程定义表
CREATE TABLE `workflow_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `key` varchar(64) NOT NULL COMMENT '流程标识',
  `name` varchar(100) NOT NULL COMMENT '流程名称',
  `category` varchar(50) DEFAULT NULL COMMENT '流程分类',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `form_type` tinyint NOT NULL DEFAULT '1' COMMENT '表单类型：1自定义表单 2业务表单',
  `form_id` bigint DEFAULT NULL COMMENT '表单ID',
  `form_conf` longtext DEFAULT NULL COMMENT '表单配置',
  `form_fields` longtext DEFAULT NULL COMMENT '表单字段',
  `bpmn_xml` longtext NOT NULL COMMENT '流程XML',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认流程：0否 1是',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key_version` (`tenant_id`, `key`, `version`, `deleted`),
  KEY `idx_tenant_category` (`tenant_id`, `category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='工作流程定义表';

-- 审批流程实例表
CREATE TABLE `workflow_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `definition_id` bigint NOT NULL COMMENT '流程定义ID',
  `definition_key` varchar(64) NOT NULL COMMENT '流程标识',
  `definition_version` int NOT NULL COMMENT '流程版本',
  `business_key` varchar(64) DEFAULT NULL COMMENT '业务标识',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '业务ID',
  `title` varchar(200) NOT NULL COMMENT '流程标题',
  `start_user_id` bigint NOT NULL COMMENT '发起人ID',
  `start_user_name` varchar(50) NOT NULL COMMENT '发起人姓名',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '流程耗时(毫秒)',
  `current_activity_ids` varchar(500) DEFAULT NULL COMMENT '当前节点ID',
  `current_activity_names` varchar(500) DEFAULT NULL COMMENT '当前节点名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1运行中 2已完成 3已取消 4已终止',
  `result` tinyint DEFAULT NULL COMMENT '审批结果：1通过 2拒绝',
  `form_variables` longtext DEFAULT NULL COMMENT '表单变量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_definition` (`tenant_id`, `definition_id`),
  KEY `idx_tenant_business` (`tenant_id`, `business_type`, `business_id`),
  KEY `idx_tenant_start_user` (`tenant_id`, `start_user_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_business_key` (`business_key`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB COMMENT='工作流程实例表';

-- 审批任务表
CREATE TABLE `workflow_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `definition_id` bigint NOT NULL COMMENT '流程定义ID',
  `task_def_key` varchar(64) NOT NULL COMMENT '任务定义Key',
  `name` varchar(200) NOT NULL COMMENT '任务名称',
  `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `category` varchar(50) DEFAULT NULL COMMENT '任务分类',
  `parent_task_id` bigint DEFAULT NULL COMMENT '父任务ID',
  `assignee` bigint DEFAULT NULL COMMENT '负责人',
  `assignee_name` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
  `owner` bigint DEFAULT NULL COMMENT '委托人',
  `candidate_users` varchar(1000) DEFAULT NULL COMMENT '候选人',
  `candidate_roles` varchar(200) DEFAULT NULL COMMENT '候选角色',
  `candidate_groups` varchar(200) DEFAULT NULL COMMENT '候选组',
  `form_key` varchar(200) DEFAULT NULL COMMENT '表单Key',
  `form_variables` longtext DEFAULT NULL COMMENT '表单变量',
  `priority` int DEFAULT '50' COMMENT '优先级',
  `due_date` datetime DEFAULT NULL COMMENT '到期时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '耗时(毫秒)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1待处理 2处理中 3已完成 4已取消 5已终止',
  `result` tinyint DEFAULT NULL COMMENT '处理结果：1通过 2拒绝 3转办 4委托',
  `reason` varchar(500) DEFAULT NULL COMMENT '处理意见',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_instance` (`tenant_id`, `instance_id`),
  KEY `idx_tenant_assignee` (`tenant_id`, `assignee`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB COMMENT='工作流任务表';

-- 审批历史表
CREATE TABLE `workflow_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `task_id` bigint DEFAULT NULL COMMENT '任务ID',
  `activity_id` varchar(64) NOT NULL COMMENT '节点ID',
  `activity_name` varchar(200) NOT NULL COMMENT '节点名称',
  `activity_type` varchar(50) NOT NULL COMMENT '节点类型',
  `assignee` bigint DEFAULT NULL COMMENT '处理人',
  `assignee_name` varchar(50) DEFAULT NULL COMMENT '处理人姓名',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '耗时(毫秒)',
  `result` tinyint DEFAULT NULL COMMENT '处理结果：1通过 2拒绝 3转办 4委托',
  `reason` varchar(500) DEFAULT NULL COMMENT '处理意见',
  `form_variables` longtext DEFAULT NULL COMMENT '表单变量',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_instance` (`tenant_id`, `instance_id`),
  KEY `idx_tenant_assignee` (`tenant_id`, `assignee`),
  KEY `idx_activity` (`activity_id`, `activity_type`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB COMMENT='工作流历史表';
```

### 4. 统计监控模块

#### 4.1 统计报表表

```sql
-- 通知统计表（按周分表）
CREATE TABLE `notification_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `stat_type` tinyint NOT NULL COMMENT '统计类型：1日统计 2周统计 3月统计',
  `dimension` varchar(50) NOT NULL COMMENT '统计维度：notification,user,dept,type',
  `dimension_id` bigint NOT NULL COMMENT '维度ID',
  `dimension_name` varchar(100) NOT NULL COMMENT '维度名称',
  `notification_count` int NOT NULL DEFAULT '0' COMMENT '通知数量',
  `push_count` int NOT NULL DEFAULT '0' COMMENT '推送数量',
  `read_count` int NOT NULL DEFAULT '0' COMMENT '阅读数量',
  `confirm_count` int NOT NULL DEFAULT '0' COMMENT '确认数量',
  `push_success_count` int NOT NULL DEFAULT '0' COMMENT '推送成功数量',
  `push_failed_count` int NOT NULL DEFAULT '0' COMMENT '推送失败数量',
  `read_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '阅读率(%)',
  `confirm_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '确认率(%)',
  `push_success_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '推送成功率(%)',
  `avg_read_time` bigint DEFAULT NULL COMMENT '平均阅读时长(分钟)',
  `avg_confirm_time` bigint DEFAULT NULL COMMENT '平均确认时长(分钟)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_stat` (`tenant_id`, `stat_date`, `stat_type`, `dimension`, `dimension_id`),
  KEY `idx_tenant_date_type` (`tenant_id`, `stat_date`, `stat_type`),
  KEY `idx_dimension` (`dimension`, `dimension_id`)
) ENGINE=InnoDB COMMENT='通知统计表'
PARTITION BY RANGE (YEAR(stat_date)*100 + WEEK(stat_date)) (
    PARTITION p202430 VALUES LESS THAN (202431),
    PARTITION p202431 VALUES LESS THAN (202432),
    PARTITION p202432 VALUES LESS THAN (202433),
    PARTITION p202433 VALUES LESS THAN (202434),
    PARTITION p202434 VALUES LESS THAN (202435),
    PARTITION p202435 VALUES LESS THAN (202436),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 实时统计缓存表
CREATE TABLE `notification_realtime_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `stat_key` varchar(100) NOT NULL COMMENT '统计键',
  `stat_value` longtext NOT NULL COMMENT '统计值JSON',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key` (`tenant_id`, `stat_key`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB COMMENT='实时统计缓存表';

-- 用户行为日志表（按月分表）
CREATE TABLE `user_behavior_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) NOT NULL COMMENT '用户姓名',
  `notification_id` bigint DEFAULT NULL COMMENT '通知ID',
  `action` varchar(50) NOT NULL COMMENT '操作类型：view,read,confirm,share,download',
  `action_target` varchar(100) DEFAULT NULL COMMENT '操作对象',
  `action_result` varchar(20) DEFAULT NULL COMMENT '操作结果：success,failed',
  `action_detail` json DEFAULT NULL COMMENT '操作详情',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `device_type` varchar(20) DEFAULT NULL COMMENT '设备类型：pc,mobile,tablet',
  `device_info` varchar(200) DEFAULT NULL COMMENT '设备信息',
  `location` varchar(100) DEFAULT NULL COMMENT '地理位置',
  `referer` varchar(500) DEFAULT NULL COMMENT '来源页面',
  `duration` bigint DEFAULT NULL COMMENT '操作时长(毫秒)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user_time` (`tenant_id`, `user_id`, `create_time`),
  KEY `idx_tenant_notification` (`tenant_id`, `notification_id`),
  KEY `idx_tenant_action` (`tenant_id`, `action`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='用户行为日志表'
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202407 VALUES LESS THAN (TO_DAYS('2024-08-01')),
    PARTITION p202408 VALUES LESS THAN (TO_DAYS('2024-09-01')),
    PARTITION p202409 VALUES LESS THAN (TO_DAYS('2024-10-01')),
    PARTITION p202410 VALUES LESS THAN (TO_DAYS('2024-11-01')),
    PARTITION p202411 VALUES LESS THAN (TO_DAYS('2024-12-01')),
    PARTITION p202412 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### 4.2 监控告警表

```sql
-- 系统监控配置表
CREATE TABLE `system_monitor_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` varchar(100) NOT NULL COMMENT '监控名称',
  `type` varchar(50) NOT NULL COMMENT '监控类型：push_rate,read_rate,confirm_rate,error_rate',
  `metric` varchar(100) NOT NULL COMMENT '监控指标',
  `threshold_warn` decimal(10,2) NOT NULL COMMENT '警告阈值',
  `threshold_critical` decimal(10,2) NOT NULL COMMENT '严重阈值',
  `check_duration` int NOT NULL DEFAULT '300' COMMENT '检查时长(秒)',
  `check_frequency` int NOT NULL DEFAULT '60' COMMENT '检查频率(秒)',
  `notification_channels` varchar(200) NOT NULL COMMENT '通知渠道：email,sms,webhook',
  `notification_users` varchar(500) NOT NULL COMMENT '通知用户ID列表',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`, `type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='系统监控配置表';

-- 监控告警记录表
CREATE TABLE `system_alert_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '告警ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `config_id` bigint NOT NULL COMMENT '配置ID',
  `alert_level` tinyint NOT NULL COMMENT '告警级别：1信息 2警告 3严重 4紧急',
  `alert_type` varchar(50) NOT NULL COMMENT '告警类型',
  `alert_title` varchar(200) NOT NULL COMMENT '告警标题',
  `alert_content` text NOT NULL COMMENT '告警内容',
  `alert_data` json DEFAULT NULL COMMENT '告警数据',
  `trigger_time` datetime NOT NULL COMMENT '触发时间',
  `recover_time` datetime DEFAULT NULL COMMENT '恢复时间',
  `duration` bigint DEFAULT NULL COMMENT '持续时间(秒)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1触发 2恢复 3忽略',
  `handle_user` bigint DEFAULT NULL COMMENT '处理人',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `notification_sent` tinyint NOT NULL DEFAULT '0' COMMENT '是否已通知：0否 1是',
  `notification_time` datetime DEFAULT NULL COMMENT '通知时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_config` (`tenant_id`, `config_id`),
  KEY `idx_tenant_level` (`tenant_id`, `alert_level`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_trigger_time` (`trigger_time`)
) ENGINE=InnoDB COMMENT='监控告警记录表';
```

### 5. 系统配置模块

#### 5.1 参数配置表

```sql
-- 系统参数配置表
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `category` varchar(50) NOT NULL DEFAULT 'system' COMMENT '配置分类',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '参数名称',
  `key` varchar(100) NOT NULL DEFAULT '' COMMENT '参数键名',
  `value` text NOT NULL DEFAULT '' COMMENT '参数键值',
  `default_value` text DEFAULT NULL COMMENT '默认值',
  `value_type` varchar(20) NOT NULL DEFAULT 'string' COMMENT '参数类型：string,number,boolean,json',
  `description` varchar(500) DEFAULT NULL COMMENT '参数描述',
  `editable` tinyint NOT NULL DEFAULT '1' COMMENT '是否可编辑：0否 1是',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否可见：0否 1是',
  `sensitive` tinyint NOT NULL DEFAULT '0' COMMENT '是否敏感：0否 1是',
  `sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key` (`tenant_id`, `key`, `deleted`),
  KEY `idx_tenant_category` (`tenant_id`, `category`),
  KEY `idx_visible_sort` (`visible`, `sort`)
) ENGINE=InnoDB COMMENT='参数配置表';

-- 字典类型表
CREATE TABLE `system_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '字典名称',
  `type` varchar(100) NOT NULL DEFAULT '' COMMENT '字典类型',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_type` (`tenant_id`, `type`, `deleted`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='字典类型表';

-- 字典数据表
CREATE TABLE `system_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `sort` int NOT NULL DEFAULT '0' COMMENT '字典排序',
  `label` varchar(100) NOT NULL DEFAULT '' COMMENT '字典标签',
  `value` varchar(100) NOT NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) NOT NULL DEFAULT '' COMMENT '字典类型',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
  `color_type` varchar(100) DEFAULT '' COMMENT '颜色类型',
  `css_class` varchar(100) DEFAULT '' COMMENT 'css样式',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_dict_type` (`tenant_id`, `dict_type`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB COMMENT='字典数据表';
```

### 6. 日志审计模块

#### 6.1 操作日志表

```sql
-- 操作日志表
CREATE TABLE `system_operate_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `trace_id` varchar(64) NOT NULL DEFAULT '' COMMENT '链路追踪编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `user_type` tinyint NOT NULL DEFAULT '0' COMMENT '用户类型：1会员 2管理员',
  `module` varchar(50) NOT NULL COMMENT '模块标题',
  `name` varchar(50) NOT NULL COMMENT '操作名',
  `type` bigint NOT NULL DEFAULT '0' COMMENT '操作分类',
  `content` varchar(2000) NOT NULL DEFAULT '' COMMENT '操作内容',
  `exts` varchar(512) NOT NULL DEFAULT '' COMMENT '拓展字段',
  `request_method` varchar(16) DEFAULT '' COMMENT '请求方法名',
  `request_url` varchar(500) DEFAULT '' COMMENT '请求地址',
  `user_ip` varchar(50) DEFAULT NULL COMMENT '用户IP',
  `user_agent` varchar(1000) DEFAULT NULL COMMENT '浏览器UA',
  `java_method` varchar(512) NOT NULL DEFAULT '' COMMENT 'Java方法名',
  `java_method_args` varchar(8000) DEFAULT '' COMMENT 'Java方法的参数',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `duration` int NOT NULL COMMENT '执行时长',
  `result_code` int NOT NULL DEFAULT '0' COMMENT '结果码',
  `result_msg` varchar(512) DEFAULT '' COMMENT '结果提示',
  `result_data` varchar(4000) DEFAULT '' COMMENT '结果数据',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user_type` (`tenant_id`, `user_id`, `user_type`),
  KEY `idx_tenant_module` (`tenant_id`, `module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='操作日志记录表';

-- 登录日志表
CREATE TABLE `system_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
  `log_type` bigint NOT NULL COMMENT '日志类型',
  `trace_id` varchar(64) NOT NULL DEFAULT '' COMMENT '链路追踪编号',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户编号',
  `user_type` tinyint NOT NULL DEFAULT '0' COMMENT '用户类型：1会员 2管理员',
  `username` varchar(50) NOT NULL DEFAULT '' COMMENT '用户账号',
  `result` tinyint NOT NULL COMMENT '登录结果：0成功 1失败',
  `user_ip` varchar(50) NOT NULL COMMENT '登录IP地址',
  `user_agent` varchar(1000) NOT NULL DEFAULT '' COMMENT '浏览器UA',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user_id` (`tenant_id`, `user_id`),
  KEY `idx_tenant_username` (`tenant_id`, `username`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='系统访问记录表';
```

## 🔧 索引优化设计

### 核心查询索引

```sql
-- 通知表核心索引
ALTER TABLE notification ADD INDEX `idx_tenant_type_priority_time` (`tenant_id`, `type`, `priority`, `create_time` DESC);
ALTER TABLE notification ADD INDEX `idx_tenant_publisher_status` (`tenant_id`, `publisher_id`, `status`);
ALTER TABLE notification ADD INDEX `idx_tenant_target_type` (`tenant_id`, `target_type`);
ALTER TABLE notification ADD INDEX `idx_status_scheduled_time` (`status`, `scheduled_time`);
ALTER TABLE notification ADD INDEX `idx_status_expire_time` (`status`, `expire_time`);

-- 推送记录表核心索引
ALTER TABLE push_record ADD INDEX `idx_tenant_user_channel_time` (`tenant_id`, `user_id`, `channel`, `push_time`);
ALTER TABLE push_record ADD INDEX `idx_tenant_notification_status` (`tenant_id`, `notification_id`, `status`);
ALTER TABLE push_record ADD INDEX `idx_status_retry_time` (`status`, `retry_count`, `retry_time`);
ALTER TABLE push_record ADD INDEX `idx_read_status_time` (`status`, `read_time`);
ALTER TABLE push_record ADD INDEX `idx_confirm_status_time` (`status`, `confirm_time`);

-- 用户权限相关索引
ALTER TABLE system_users ADD INDEX `idx_tenant_dept_status` (`tenant_id`, `dept_id`, `status`);
ALTER TABLE system_user_role ADD INDEX `idx_tenant_role_user` (`tenant_id`, `role_id`, `user_id`);
ALTER TABLE system_role_menu ADD INDEX `idx_role_menu` (`role_id`, `menu_id`);

-- 工作流索引
ALTER TABLE workflow_instance ADD INDEX `idx_tenant_business_status` (`tenant_id`, `business_type`, `business_id`, `status`);
ALTER TABLE workflow_task ADD INDEX `idx_tenant_assignee_status` (`tenant_id`, `assignee`, `status`);
ALTER TABLE workflow_task ADD INDEX `idx_tenant_due_date` (`tenant_id`, `due_date`);

-- 统计表索引
ALTER TABLE notification_statistics ADD INDEX `idx_tenant_dimension_date` (`tenant_id`, `dimension`, `dimension_id`, `stat_date`);
ALTER TABLE user_behavior_log ADD INDEX `idx_tenant_user_action_time` (`tenant_id`, `user_id`, `action`, `create_time`);
```

### 复合索引策略

```sql
-- 多条件查询优化索引
CREATE INDEX `idx_notification_complex` ON notification 
(`tenant_id`, `status`, `type`, `publisher_id`, `create_time` DESC);

CREATE INDEX `idx_push_record_complex` ON push_record 
(`tenant_id`, `notification_id`, `user_id`, `status`, `push_time` DESC);

-- 统计查询优化索引
CREATE INDEX `idx_statistics_complex` ON notification_statistics 
(`tenant_id`, `stat_date`, `dimension`, `dimension_id`, `stat_type`);

-- 覆盖索引 - 减少回表查询
CREATE INDEX `idx_notification_cover` ON notification 
(`tenant_id`, `status`, `type`) 
INCLUDE (`title`, `publisher_name`, `create_time`, `target_count`, `read_count`);

CREATE INDEX `idx_push_record_cover` ON push_record 
(`tenant_id`, `user_id`, `status`) 
INCLUDE (`notification_id`, `channel`, `push_time`, `read_time`);
```

## 📊 数据字典

### 通知类型枚举

```sql
-- 通知类型字典
INSERT INTO system_dict_type (tenant_id, name, type, status, remark) 
VALUES (1, '通知类型', 'notification_type', 0, '系统通知分类');

INSERT INTO system_dict_data (tenant_id, sort, label, value, dict_type, color_type, status) VALUES
(1, 1, '紧急通知', '1', 'notification_type', 'danger', 0),
(1, 2, '重要通知', '2', 'notification_type', 'warning', 0),
(1, 3, '常规通知', '3', 'notification_type', 'primary', 0),
(1, 4, '提醒通知', '4', 'notification_type', 'info', 0);

-- 通知状态字典
INSERT INTO system_dict_type (tenant_id, name, type, status, remark) 
VALUES (1, '通知状态', 'notification_status', 0, '通知当前状态');

INSERT INTO system_dict_data (tenant_id, sort, label, value, dict_type, color_type, status) VALUES
(1, 1, '草稿', '0', 'notification_status', 'info', 0),
(1, 2, '待审批', '1', 'notification_status', 'warning', 0),
(1, 3, '审批通过', '2', 'notification_status', 'success', 0),
(1, 4, '已发布', '3', 'notification_status', 'primary', 0),
(1, 5, '已暂停', '4', 'notification_status', 'warning', 0),
(1, 6, '已过期', '5', 'notification_status', 'danger', 0),
(1, 7, '已撤回', '6', 'notification_status', 'info', 0);

-- 推送渠道字典
INSERT INTO system_dict_type (tenant_id, name, type, status, remark) 
VALUES (1, '推送渠道', 'push_channel', 0, '消息推送渠道');

INSERT INTO system_dict_data (tenant_id, sort, label, value, dict_type, color_type, status) VALUES
(1, 1, 'APP推送', 'app', 'push_channel', 'primary', 0),
(1, 2, '站内信', 'site', 'push_channel', 'success', 0),
(1, 3, '短信推送', 'sms', 'push_channel', 'warning', 0),
(1, 4, '邮件推送', 'email', 'push_channel', 'info', 0),
(1, 5, '微信推送', 'wechat', 'push_channel', 'success', 0),
(1, 6, '钉钉推送', 'dingtalk', 'push_channel', 'primary', 0);

-- 推送状态字典
INSERT INTO system_dict_type (tenant_id, name, type, status, remark) 
VALUES (1, '推送状态', 'push_status', 0, '推送记录状态');

INSERT INTO system_dict_data (tenant_id, sort, label, value, dict_type, color_type, status) VALUES
(1, 1, '推送中', '0', 'push_status', 'info', 0),
(1, 2, '推送成功', '1', 'push_status', 'success', 0),
(1, 3, '推送失败', '2', 'push_status', 'danger', 0),
(1, 4, '已读', '3', 'push_status', 'primary', 0),
(1, 5, '已确认', '4', 'push_status', 'success', 0),
(1, 6, '已过期', '5', 'push_status', 'warning', 0);

-- 目标类型字典
INSERT INTO system_dict_type (tenant_id, name, type, status, remark) 
VALUES (1, '目标类型', 'target_type', 0, '通知推送目标类型');

INSERT INTO system_dict_data (tenant_id, sort, label, value, dict_type, color_type, status) VALUES
(1, 1, '全部用户', '1', 'target_type', 'primary', 0),
(1, 2, '指定角色', '2', 'target_type', 'success', 0),
(1, 3, '指定部门', '3', 'target_type', 'warning', 0),
(1, 4, '用户组', '4', 'target_type', 'info', 0),
(1, 5, '指定用户', '5', 'target_type', 'danger', 0);
```

### 角色权限数据

```sql
-- 教育系统角色初始化
INSERT INTO system_role (tenant_id, name, code, sort, data_scope, status, type, remark) VALUES
-- 校级管理层
(1, '校长', 'school_principal', 1, 1, 0, 1, '校长，拥有全校最高权限'),
(1, '副校长', 'school_vice_principal', 2, 2, 0, 1, '副校长，分管具体领域'),
(1, '教务处长', 'academic_dean', 3, 2, 0, 1, '教务处长，负责教学管理'),
(1, '学生处长', 'student_dean', 4, 2, 0, 1, '学生处长，负责学生工作'),

-- 学院管理层
(1, '院长', 'college_dean', 10, 3, 0, 1, '学院院长'),
(1, '副院长', 'college_vice_dean', 11, 3, 0, 1, '学院副院长'),
(1, '党委书记', 'party_secretary', 12, 3, 0, 1, '学院党委书记'),
(1, '党委副书记', 'party_vice_secretary', 13, 3, 0, 1, '学院党委副书记'),

-- 行政执行层
(1, '教学秘书', 'teaching_secretary', 20, 4, 0, 1, '教学秘书'),
(1, '科研秘书', 'research_secretary', 21, 4, 0, 1, '科研秘书'),
(1, '行政秘书', 'admin_secretary', 22, 4, 0, 1, '行政秘书'),
(1, '团委书记', 'youth_secretary', 23, 4, 0, 1, '团委书记'),

-- 教学执行层
(1, '系主任', 'department_head', 30, 4, 0, 1, '系主任'),
(1, '教研室主任', 'teaching_group_head', 31, 4, 0, 1, '教研室主任'),
(1, '任课教师', 'teacher', 32, 5, 0, 1, '任课教师'),
(1, '实验教师', 'lab_teacher', 33, 5, 0, 1, '实验教师'),

-- 学生管理层
(1, '年级主任', 'grade_head', 40, 4, 0, 1, '年级主任'),
(1, '年级组长', 'grade_leader', 41, 4, 0, 1, '年级组长'),
(1, '辅导员', 'counselor', 42, 5, 0, 1, '辅导员'),
(1, '班主任', 'class_teacher', 43, 5, 0, 1, '班主任'),

-- 学生组织层
(1, '学生会主席', 'student_union_president', 50, 4, 0, 1, '学生会主席'),
(1, '学生会部长', 'student_union_minister', 51, 5, 0, 1, '学生会部长'),
(1, '班长', 'class_monitor', 52, 5, 0, 1, '班长'),
(1, '团支书', 'youth_league_secretary', 53, 5, 0, 1, '团支书'),

-- 学生
(1, '学生', 'student', 60, 5, 0, 1, '学生');
```

## 🚀 性能优化建议

### 1. 查询优化

```sql
-- 分页查询优化 - 使用覆盖索引避免回表
SELECT n.id, n.title, n.type, n.publisher_name, n.create_time, n.read_count
FROM notification n
WHERE n.tenant_id = 1 AND n.status = 3
ORDER BY n.create_time DESC
LIMIT 20 OFFSET 0;

-- 统计查询优化 - 使用预计算表
SELECT 
  ns.dimension_name,
  ns.notification_count,
  ns.push_count,
  ns.read_count,
  ns.read_rate
FROM notification_statistics ns
WHERE ns.tenant_id = 1 
  AND ns.stat_date >= '2024-07-01'
  AND ns.stat_type = 1
  AND ns.dimension = 'dept';

-- 复杂条件查询优化 - 合理使用联合索引
SELECT n.*, u.nickname as publisher_nickname
FROM notification n
INNER JOIN system_users u ON n.publisher_id = u.id
WHERE n.tenant_id = 1
  AND n.status IN (3, 4)
  AND n.type IN (1, 2)
  AND n.create_time >= '2024-07-01'
ORDER BY n.priority DESC, n.create_time DESC
LIMIT 50;
```

### 2. 写入优化

```sql
-- 批量插入推送记录
INSERT INTO push_record (
  tenant_id, notification_id, user_id, user_name, 
  channel, push_time, push_content, status
) VALUES
(1, 1001, 2001, '张三', 'app', NOW(), '通知内容1', 1),
(1, 1001, 2002, '李四', 'app', NOW(), '通知内容1', 1),
(1, 1001, 2003, '王五', 'app', NOW(), '通知内容1', 1);

-- 使用ON DUPLICATE KEY UPDATE优化更新
INSERT INTO notification_statistics (
  tenant_id, stat_date, stat_type, dimension, dimension_id,
  notification_count, push_count, read_count
) VALUES (1, CURDATE(), 1, 'user', 2001, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  notification_count = notification_count + VALUES(notification_count),
  push_count = push_count + VALUES(push_count),
  read_count = read_count + VALUES(read_count);
```

### 3. 缓存策略

```yaml
缓存配置建议:
  Redis配置:
    # 用户权限缓存 - 1小时
    user:permissions:{userId}: 3600s
    
    # 通知列表缓存 - 5分钟
    notification:list:{tenantId}:{type}:{page}: 300s
    
    # 实时统计缓存 - 1分钟
    stats:realtime:{tenantId}: 60s
    
    # 推送队列缓存 - 实时
    push:queue:{tenantId}: 永久(直到消费)
    
  本地缓存配置:
    # 系统配置缓存
    system:config: 24小时
    
    # 字典数据缓存  
    dict:data:{type}: 30分钟
    
    # 用户基本信息缓存
    user:info:{userId}: 1小时
```

## 💾 数据迁移与初始化

### 1. 初始化脚本

```sql
-- 创建数据库初始化脚本
CREATE DATABASE IF NOT EXISTS `yudao_notification` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `yudao_notification`;

-- 设置数据库参数
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建所有表结构（上述表结构）
-- ... 所有CREATE TABLE语句 ...

-- 初始化基础数据
SOURCE ./data/init_dict_data.sql;
SOURCE ./data/init_role_data.sql;  
SOURCE ./data/init_config_data.sql;
SOURCE ./data/init_template_data.sql;

-- 创建默认管理员账号
INSERT INTO system_users (tenant_id, username, password, nickname, status) 
VALUES (1, 'admin', '$2a$10$7JB720yubVSOfvVWbXkTa.kPMv8YyLyV9z2WfmjZqWJrQABAZWJZL', '系统管理员', 1);

-- 分配管理员角色
INSERT INTO system_user_role (tenant_id, user_id, role_id) 
VALUES (1, 1, 1);

SET FOREIGN_KEY_CHECKS = 1;
```

### 2. 数据库升级脚本

```sql
-- V1.1.0 升级脚本
ALTER TABLE notification ADD COLUMN `content_html` longtext DEFAULT NULL COMMENT '富文本内容' AFTER content;
ALTER TABLE notification ADD COLUMN `attachments` json DEFAULT NULL COMMENT '附件信息' AFTER expire_time;

-- V1.2.0 升级脚本  
CREATE TABLE `notification_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `notification_id` bigint NOT NULL COMMENT '通知ID',
  `file_name` varchar(200) NOT NULL COMMENT '文件名称',
  `file_size` bigint NOT NULL COMMENT '文件大小',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '下载次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB COMMENT='通知附件表';
```

### 3. 性能监控脚本

```sql
-- 创建性能监控视图
CREATE VIEW `v_notification_performance` AS
SELECT 
  DATE(create_time) as stat_date,
  tenant_id,
  COUNT(*) as total_count,
  SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as published_count,
  SUM(target_count) as total_target_users,
  SUM(push_count) as total_push_count,
  SUM(read_count) as total_read_count,
  SUM(confirm_count) as total_confirm_count,
  ROUND(SUM(read_count) * 100.0 / SUM(push_count), 2) as read_rate,
  ROUND(SUM(confirm_count) * 100.0 / SUM(push_count), 2) as confirm_rate
FROM notification 
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(create_time), tenant_id
ORDER BY stat_date DESC;

-- 创建系统健康检查存储过程
DELIMITER //
CREATE PROCEDURE `sp_system_health_check`()
BEGIN
    DECLARE v_notification_count INT;
    DECLARE v_push_pending_count INT;
    DECLARE v_error_rate DECIMAL(5,2);
    
    -- 检查通知数量
    SELECT COUNT(*) INTO v_notification_count 
    FROM notification 
    WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY);
    
    -- 检查待推送数量
    SELECT COUNT(*) INTO v_push_pending_count 
    FROM push_record 
    WHERE status = 0 AND create_time <= DATE_SUB(NOW(), INTERVAL 10 MINUTE);
    
    -- 检查错误率
    SELECT ROUND(
        SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2
    ) INTO v_error_rate
    FROM push_record 
    WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR);
    
    -- 返回健康检查结果
    SELECT 
        v_notification_count as notification_count,
        v_push_pending_count as pending_count,
        v_error_rate as error_rate,
        CASE 
            WHEN v_push_pending_count > 1000 THEN '异常'
            WHEN v_error_rate > 5.0 THEN '警告'
            ELSE '正常'
        END as health_status;
END //
DELIMITER ;
```

## 📈 分库分表实施方案

### 1. ShardingSphere配置

```yaml
# sharding-jdbc配置
spring:
  shardingsphere:
    datasource:
      names: ds0,ds1,ds2,ds3
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/notification_tenant_0?useUnicode=true&characterEncoding=utf-8&useSSL=false
        username: root
        password: 123456
      ds1:
        type: com.zaxxer.hikari.HikariDataSource  
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/notification_tenant_1?useUnicode=true&characterEncoding=utf-8&useSSL=false
        username: root
        password: 123456
      ds2:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/notification_tenant_2?useUnicode=true&characterEncoding=utf-8&useSSL=false
        username: root
        password: 123456
      ds3:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/notification_tenant_3?useUnicode=true&characterEncoding=utf-8&useSSL=false
        username: root
        password: 123456
    
    rules:
      sharding:
        tables:
          notification:
            actual-data-nodes: ds$->{0..3}.notification_$->{2024..2025}$->{01..12}
            database-strategy:
              standard:
                sharding-column: tenant_id
                sharding-algorithm-name: tenant-mod
            table-strategy:
              standard:
                sharding-column: create_time
                sharding-algorithm-name: notification-month
          push_record:
            actual-data-nodes: ds$->{0..3}.push_record_$->{20240701..20241231}
            database-strategy:
              standard:
                sharding-column: tenant_id
                sharding-algorithm-name: tenant-mod
            table-strategy:
              standard:
                sharding-column: create_time
                sharding-algorithm-name: push-record-day
        
        sharding-algorithms:
          tenant-mod:
            type: MOD
            props:
              sharding-count: 4
          notification-month:
            type: INTERVAL
            props:
              datetime-pattern: "yyyy-MM-dd HH:mm:ss"
              datetime-lower: "2024-01-01 00:00:00"
              datetime-upper: "2025-12-31 23:59:59"
              sharding-suffix-pattern: "yyyyMM"
              datetime-interval-amount: 1
              datetime-interval-unit: "MONTHS"
          push-record-day:
            type: INTERVAL  
            props:
              datetime-pattern: "yyyy-MM-dd HH:mm:ss"
              datetime-lower: "2024-07-01 00:00:00"
              datetime-upper: "2024-12-31 23:59:59"
              sharding-suffix-pattern: "yyyyMMdd"
              datetime-interval-amount: 1
              datetime-interval-unit: "DAYS"

    props:
      sql-show: true
      sql-comment-parse-enabled: true
      sql-federation-enabled: true
```

### 2. 读写分离配置

```yaml
# 读写分离配置
spring:
  shardingsphere:
    rules:
      readwrite-splitting:
        data-sources:
          ds0:
            static-strategy:
              write-data-source-name: ds0-write
              read-data-source-names: 
                - ds0-read1
                - ds0-read2
          ds1:
            static-strategy:
              write-data-source-name: ds1-write
              read-data-source-names:
                - ds1-read1
                - ds1-read2
          ds2:
            static-strategy:
              write-data-source-name: ds2-write
              read-data-source-names:
                - ds2-read1
                - ds2-read2
          ds3:
            static-strategy:
              write-data-source-name: ds3-write
              read-data-source-names:
                - ds3-read1
                - ds3-read2
        
        load-balancers:
          round-robin:
            type: ROUND_ROBIN
```

## 📋 总结

### 设计特色

1. **多租户架构**: 完全的数据隔离，支持教育机构独立部署
2. **复杂权限模型**: 6层级25+角色的细粒度权限控制
3. **高性能设计**: 分库分表 + 读写分离 + 多级缓存
4. **业务完整性**: 覆盖通知全生命周期管理
5. **扩展性强**: 支持水平扩展和业务扩展

### 技术亮点

- **分片策略**: 按租户分库，按时间分表，提升查询性能
- **索引优化**: 覆盖索引减少回表，复合索引优化多条件查询
- **缓存策略**: 三级缓存架构，从JVM到Redis到数据库
- **监控完善**: 实时监控 + 历史统计 + 告警机制
- **数据安全**: 敏感数据加密 + 权限数据隔离

### 部署建议

- **开发环境**: 单机MySQL + 单节点Redis
- **测试环境**: 主从MySQL + Redis集群
- **生产环境**: 分库分表 + 读写分离 + 高可用集群

该数据库设计方案充分考虑了教育行业通知系统的复杂业务需求，采用了现代化的数据库设计理念和技术架构，能够满足高并发、大数据量、复杂业务逻辑的需求，为系统的长期稳定运行提供了坚实的数据基础。