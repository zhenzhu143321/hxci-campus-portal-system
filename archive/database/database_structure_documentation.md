# 哈尔滨信息工程学院校园门户系统 - 数据库结构文档

## 📋 文档概述

**生成时间**: 2025年8月14日  
**数据库名称**: ruoyi-vue-pro  
**系统版本**: Spring Boot 3.4.5 + Vue 3  
**文档作用**: 系统数据库表结构中文对照及功能说明  

---

## 📊 数据库概览

| 表名 | 中文名称 | 记录数 | 状态 | 核心功能 |
|------|----------|--------|------|----------|
| infra_api_access_log | API访问日志表 | 488条 | ✅活跃 | 系统监控 |
| infra_api_error_log | 系统异常日志表 | 43条 | ✅活跃 | 错误追踪 |
| mock_role_permissions | Mock角色权限表 | 40条 | ✅活跃 | 权限控制 |
| mock_school_users | Mock学校用户表 | 12条 | ✅活跃 | 用户认证 |
| notification_info | 智能通知信息表 | 15条 | ✅活跃 | 核心业务 |
| system_dept | 系统部门表 | 2条 | ✅使用 | 组织架构 |
| system_login_log | 系统登录日志表 | 29条 | ✅活跃 | 登录追踪 |
| system_oauth2_access_token | OAuth2访问令牌表 | 0条 | 🔄待用 | 认证管理 |
| system_oauth2_client | OAuth2客户端表 | 0条 | 🔄待用 | 认证配置 |
| system_oauth2_refresh_token | OAuth2刷新令牌表 | 0条 | 🔄待用 | 令牌刷新 |
| system_tenant | 系统租户表 | 0条 | 🔄待用 | 多租户 |
| system_users | 系统用户表 | 1条 | ✅使用 | 主系统用户 |
| weather_cache | 天气数据缓存表 | 0条 | 🚧开发中 | 天气服务 |

---

## 📋 详细表结构

### 1. infra_api_access_log (API访问日志表)

**表作用**: 记录系统所有API调用的详细信息，用于性能监控、安全审计和系统分析。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 日志编号 | 是 | 自增 | 主键，唯一标识 |
| trace_id | varchar(64) | 链路追踪编号 | 是 | - | 用于分布式追踪 |
| user_id | bigint | 用户编号 | 是 | 0 | 调用用户ID |
| user_type | tinyint | 用户类型 | 是 | 0 | 用户分类标识 |
| application_name | varchar(50) | 应用名称 | 是 | - | 调用方应用标识 |
| request_method | varchar(16) | 请求方法 | 是 | - | GET/POST/PUT/DELETE |
| request_url | varchar(255) | 请求地址 | 是 | - | API接口路径 |
| request_params | text | 请求参数 | 否 | - | 请求体内容 |
| user_ip | varchar(50) | 用户IP地址 | 是 | - | 客户端IP |
| user_agent | varchar(512) | 浏览器标识 | 是 | - | 客户端信息 |
| begin_time | datetime | 请求开始时间 | 是 | - | 接口调用开始 |
| end_time | datetime | 请求结束时间 | 是 | - | 接口调用结束 |
| duration | int | 执行耗时(毫秒) | 是 | - | 性能监控指标 |
| result_code | int | 响应状态码 | 是 | 0 | HTTP状态码 |
| result_msg | varchar(512) | 响应消息 | 否 | 空 | 接口返回信息 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 记录生成时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |

### 2. infra_api_error_log (系统异常日志表)

**表作用**: 记录系统运行过程中发生的所有异常和错误，便于快速定位和解决问题。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | int | 异常编号 | 是 | 自增 | 主键，唯一标识 |
| trace_id | varchar(64) | 链路追踪编号 | 是 | - | 分布式追踪ID |
| user_id | int | 用户编号 | 是 | 0 | 发生异常的用户 |
| user_type | tinyint | 用户类型 | 是 | 0 | 用户分类 |
| application_name | varchar(50) | 应用名称 | 是 | - | 异常发生的应用 |
| request_method | varchar(16) | 请求方法 | 是 | - | 异常接口的方法 |
| request_url | varchar(255) | 请求地址 | 是 | - | 异常接口路径 |
| request_params | text | 请求参数 | 否 | - | 导致异常的参数 |
| user_ip | varchar(50) | 用户IP地址 | 是 | - | 客户端IP |
| user_agent | varchar(512) | 浏览器标识 | 是 | - | 客户端信息 |
| exception_time | datetime | 异常发生时间 | 是 | - | 精确异常时间 |
| exception_name | varchar(128) | 异常名称 | 是 | 空 | Java异常类名 |
| exception_message | text | 异常信息 | 是 | - | 异常描述消息 |
| exception_root_cause_message | text | 根因异常信息 | 是 | - | 根本原因 |
| exception_stack_trace | text | 异常堆栈轨迹 | 是 | - | 完整堆栈信息 |
| exception_class_name | varchar(512) | 异常发生类名 | 是 | - | Java类全名 |
| exception_file_name | varchar(512) | 异常发生文件名 | 是 | - | 源代码文件 |
| exception_method_name | varchar(512) | 异常发生方法名 | 是 | - | 出错的方法 |
| exception_line_number | int | 异常发生行号 | 是 | - | 源码行数 |
| process_status | tinyint | 处理状态 | 是 | - | 0:待处理 1:已处理 |
| process_time | datetime | 处理时间 | 否 | - | 异常处理时间 |
| process_user_id | int | 处理人编号 | 否 | 0 | 处理异常的用户 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 记录生成时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |

### 3. mock_role_permissions (Mock角色权限表)

**表作用**: 定义Mock School系统中各种角色的通知发布权限，实现基于角色的访问控制(RBAC)。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 权限编号 | 是 | 自增 | 主键，唯一标识 |
| role_code | varchar(30) | 角色代码 | 是 | - | PRINCIPAL/TEACHER等 |
| permission_code | varchar(50) | 权限代码 | 是 | - | PUBLISH_EMERGENCY等 |
| permission_name | varchar(100) | 权限名称 | 是 | - | 权限中文说明 |
| description | varchar(200) | 权限描述 | 否 | - | 详细功能说明 |
| notification_levels | varchar(20) | 可发布通知级别 | 否 | - | 1,2,3,4级别组合 |
| target_scope | varchar(100) | 目标范围 | 否 | - | 发布范围限制 |
| approval_required | bit(1) | 是否需要审批 | 否 | - | 审批流程控制 |
| create_time | datetime(6) | 创建时间 | 是 | - | 记录创建时间 |

### 4. mock_school_users (Mock学校用户表)

**表作用**: 存储Mock School API的用户信息，实现双重认证系统的第一层用户验证。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 用户编号 | 是 | 自增 | 主键，唯一标识 |
| username | varchar(50) | 用户名 | 是 | - | 显示名称 |
| user_id | varchar(50) | 用户ID | 是 | - | 工号/学号，唯一 |
| role_code | varchar(30) | 角色代码 | 是 | - | 用户角色标识 |
| role_name | varchar(50) | 角色名称 | 是 | - | 角色中文名称 |
| department_id | bigint | 部门编号 | 否 | - | 所属部门ID |
| department_name | varchar(100) | 部门名称 | 否 | - | 部门中文名称 |
| grade | varchar(20) | 年级信息 | 否 | - | **新增**: 学生年级 |
| enabled | tinyint(1) | 是否启用 | 否 | 1 | 账户状态 |
| token | varchar(200) | 认证令牌 | 否 | - | JWT Token |
| token_expires_at | datetime | 令牌过期时间 | 是 | - | Token有效期 |
| create_time | datetime | 创建时间 | 否 | 当前时间 | 账户创建时间 |
| update_time | datetime | 更新时间 | 否 | 当前时间 | 自动更新时间 |
| token_expires_time | datetime(6) | 令牌过期时间2 | 否 | - | 备用过期字段 |

### 5. notification_info (智能通知信息表) ⭐核心表

**表作用**: 系统核心表，存储所有通知的完整信息，包括审批流程、发布状态、目标范围等。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 通知编号 | 是 | 自增 | 主键，唯一标识 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |
| title | varchar(200) | 通知标题 | 是 | - | 通知主题 |
| content | text | 通知内容 | 是 | - | 详细内容 |
| summary | varchar(500) | 通知摘要 | 否 | - | 内容简述 |
| level | tinyint | 通知级别 | 是 | 3 | 1紧急/2重要/3常规/4提醒 |
| status | tinyint | 通知状态 | 是 | 0 | 0草稿/2待审批/3已发布 |
| category_id | bigint | 分类编号 | 否 | - | 通知分类ID |
| publisher_id | bigint | 发布者编号 | 是 | - | 发布人用户ID |
| publisher_name | varchar(50) | 发布者姓名 | 是 | - | 发布人姓名 |
| publisher_role | varchar(30) | 发布者角色 | 否 | - | 发布人角色代码 |
| target_scope | varchar(50) | 目标范围 | 否 | SCHOOL_WIDE | **核心**: SCHOOL_WIDE/DEPARTMENT/CLASS/GRADE |
| scheduled_time | datetime | 定时发布时间 | 否 | - | 计划发布时间 |
| expired_time | datetime | 过期时间 | 否 | - | 通知失效时间 |
| push_channels | varchar(100) | 推送渠道 | 否 | - | 1APP/2短信/5系统 |
| require_confirm | tinyint | 是否需要确认 | 否 | 0 | 阅读确认功能 |
| pinned | tinyint | 是否置顶 | 否 | 0 | 置顶显示 |
| push_count | int | 推送次数 | 否 | 0 | 推送统计 |
| read_count | int | 阅读次数 | 否 | 0 | 阅读统计 |
| confirm_count | int | 确认次数 | 否 | 0 | 确认统计 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 通知创建时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| approver_id | bigint | 审批者编号 | 否 | - | 审批人用户ID |
| approver_name | varchar(50) | 审批者姓名 | 否 | - | 审批人姓名 |
| approval_time | datetime | 审批时间 | 否 | - | 审批完成时间 |
| approval_comment | text | 审批意见 | 否 | - | 审批备注 |
| approval_status | varchar(20) | 审批状态 | 否 | - | APPROVED/REJECTED |

### 6. system_dept (系统部门表)

**表作用**: 管理系统的组织架构，支持树形结构的部门层级关系。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 部门编号 | 是 | 自增 | 主键，唯一标识 |
| name | varchar(30) | 部门名称 | 是 | 空 | 部门中文名称 |
| parent_id | bigint | 父部门编号 | 是 | 0 | 上级部门ID |
| sort | int | 显示顺序 | 是 | 0 | 排序权重 |
| status | tinyint | 部门状态 | 是 | - | 0正常/1停用 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 部门创建时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |

### 7. system_login_log (系统登录日志表)

**表作用**: 记录用户登录系统的详细信息，用于安全审计和异常登录检测。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 日志编号 | 是 | 自增 | 主键，唯一标识 |
| log_type | bigint | 日志类型 | 是 | - | 登录类型分类 |
| trace_id | varchar(64) | 链路追踪编号 | 否 | 空 | 分布式追踪ID |
| user_id | bigint | 用户编号 | 是 | 0 | 登录用户ID |
| user_type | tinyint | 用户类型 | 是 | 0 | 用户分类 |
| username | varchar(50) | 用户名 | 否 | 空 | 登录用户名 |
| result | tinyint | 登录结果 | 是 | - | 0成功/1失败 |
| user_ip | varchar(50) | 用户IP地址 | 是 | - | 登录来源IP |
| user_agent | varchar(512) | 浏览器标识 | 是 | - | 客户端信息 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 登录时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |

### 8. system_oauth2_access_token (OAuth2访问令牌表)

**表作用**: 存储OAuth2认证体系的访问令牌信息，实现JWT Token管理。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 令牌编号 | 是 | 自增 | 主键，唯一标识 |
| user_id | bigint | 用户编号 | 是 | - | 令牌所属用户 |
| user_type | tinyint | 用户类型 | 是 | - | 用户分类 |
| user_info | varchar(512) | 用户信息 | 是 | - | 用户详细信息JSON |
| access_token | varchar(255) | 访问令牌 | 是 | - | JWT Access Token |
| refresh_token | varchar(255) | 刷新令牌 | 是 | - | JWT Refresh Token |
| client_id | varchar(255) | 客户端编号 | 是 | - | OAuth2客户端ID |
| scopes | varchar(255) | 授权范围 | 否 | - | 权限范围 |
| expires_time | datetime | 过期时间 | 是 | - | Token失效时间 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 令牌创建时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |

### 9. system_oauth2_client (OAuth2客户端表)

**表作用**: 定义OAuth2认证体系的客户端配置信息，管理应用接入凭证。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 客户端编号 | 是 | 自增 | 主键，唯一标识 |
| client_id | varchar(255) | 客户端ID | 是 | - | OAuth2客户端标识 |
| secret | varchar(255) | 客户端密钥 | 是 | - | 认证密钥 |
| name | varchar(255) | 应用名称 | 是 | - | 客户端应用名 |
| logo | varchar(255) | 应用图标 | 是 | - | 应用Logo URL |
| description | varchar(255) | 应用描述 | 否 | - | 应用说明 |
| status | tinyint | 客户端状态 | 是 | - | 0启用/1禁用 |
| access_token_validity_seconds | int | 访问令牌有效期 | 是 | - | Token存活时间(秒) |
| refresh_token_validity_seconds | int | 刷新令牌有效期 | 是 | - | Refresh Token存活时间 |
| redirect_uris | varchar(255) | 重定向地址 | 是 | - | OAuth2回调地址 |
| authorized_grant_types | varchar(255) | 授权类型 | 是 | - | 支持的OAuth2流程 |
| scopes | varchar(255) | 授权范围 | 否 | - | 权限范围定义 |
| auto_approve_scopes | varchar(255) | 自动授权范围 | 否 | - | 免确认的权限 |
| authorities | varchar(255) | 权限列表 | 否 | - | 客户端权限 |
| resource_ids | varchar(255) | 资源ID列表 | 否 | - | 可访问的资源 |
| additional_information | varchar(4096) | 附加信息 | 否 | - | 扩展配置JSON |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 客户端创建时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |

### 10. system_oauth2_refresh_token (OAuth2刷新令牌表)

**表作用**: 存储OAuth2体系的刷新令牌，支持访问令牌的自动续期功能。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 刷新令牌编号 | 是 | 自增 | 主键，唯一标识 |
| user_id | bigint | 用户编号 | 是 | - | 令牌所属用户 |
| user_type | tinyint | 用户类型 | 是 | - | 用户分类 |
| refresh_token | varchar(255) | 刷新令牌 | 是 | - | JWT Refresh Token |
| client_id | varchar(255) | 客户端编号 | 是 | - | OAuth2客户端ID |
| scopes | varchar(255) | 授权范围 | 否 | - | 权限范围 |
| expires_time | datetime | 过期时间 | 是 | - | Refresh Token失效时间 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 令牌创建时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |

### 11. system_tenant (系统租户表)

**表作用**: 支持多租户架构，管理不同租户的基本信息和配置。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 租户编号 | 是 | 自增 | 主键，唯一标识 |
| name | varchar(30) | 租户名称 | 是 | - | 租户中文名称 |
| contact_user_id | bigint | 联系人用户编号 | 否 | - | 联系人ID |
| contact_name | varchar(30) | 联系人姓名 | 是 | - | 联系人姓名 |
| contact_mobile | varchar(500) | 联系人手机 | 否 | - | 联系方式 |
| status | tinyint | 租户状态 | 是 | 0 | 0正常/1停用 |
| website | varchar(256) | 绑定域名 | 否 | 空 | 租户域名 |
| package_id | bigint | 租户套餐编号 | 是 | - | 套餐类型ID |
| expire_time | datetime | 过期时间 | 是 | - | 租户到期时间 |
| account_count | int | 账号数量 | 是 | - | 允许的用户数 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 租户创建时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |

### 12. system_users (系统用户表)

**表作用**: 存储主系统的用户基本信息，与mock_school_users配合实现双重认证体系。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 用户编号 | 是 | 自增 | 主键，唯一标识 |
| username | varchar(30) | 用户账号 | 是 | - | 登录用户名，唯一 |
| password | varchar(100) | 登录密码 | 否 | 空 | 加密存储密码 |
| nickname | varchar(30) | 用户昵称 | 是 | - | 显示名称 |
| remark | varchar(500) | 用户备注 | 否 | - | 用户说明信息 |
| dept_id | bigint | 部门编号 | 否 | - | 所属部门ID |
| post_ids | varchar(255) | 岗位编号数组 | 否 | - | 岗位信息JSON |
| email | varchar(50) | 用户邮箱 | 否 | 空 | 邮箱地址 |
| mobile | varchar(11) | 手机号码 | 否 | 空 | 联系电话 |
| sex | tinyint | 用户性别 | 否 | 0 | 0未知/1男/2女 |
| avatar | varchar(512) | 头像地址 | 否 | 空 | 头像图片URL |
| status | tinyint | 账号状态 | 是 | 0 | 0正常/1停用 |
| login_ip | varchar(50) | 最后登录IP | 否 | 空 | 最近登录IP |
| login_date | datetime | 最后登录时间 | 否 | - | 最近登录时间 |
| creator | varchar(64) | 创建者 | 否 | 空 | 记录创建人 |
| create_time | datetime | 创建时间 | 是 | 当前时间 | 用户创建时间 |
| updater | varchar(64) | 更新者 | 否 | 空 | 记录修改人 |
| update_time | datetime | 更新时间 | 是 | 当前时间 | 自动更新时间 |
| deleted | bit(1) | 是否删除 | 是 | 0 | 软删除标记 |
| tenant_id | bigint | 租户编号 | 是 | 0 | 多租户支持 |

### 13. weather_cache (天气数据缓存表) 🚧开发中

**表作用**: 缓存和风天气API的数据，减少外部API调用次数，提高系统响应速度。

| 字段名 | 数据类型 | 中文名称 | 是否必填 | 默认值 | 说明 |
|--------|----------|----------|----------|--------|------|
| id | bigint | 缓存编号 | 是 | 自增 | 主键，唯一标识 |
| city_code | varchar(20) | 城市代码 | 是 | - | 和风天气城市编码 |
| city_name | varchar(50) | 城市名称 | 是 | - | 城市中文名称 |
| temperature | int | 温度 | 是 | - | 当前温度(摄氏度) |
| weather_text | varchar(50) | 天气描述 | 是 | - | 天气状况文字 |
| humidity | int | 湿度 | 否 | - | 相对湿度百分比 |
| wind_dir | varchar(20) | 风向 | 否 | - | 风向描述 |
| wind_scale | varchar(10) | 风力等级 | 否 | - | 风力等级 |
| update_time | datetime | 更新时间 | 是 | - | 缓存更新时间 |
| api_update_time | datetime | API更新时间 | 是 | - | 原始数据时间 |
| create_time | datetime | 创建时间 | 否 | 当前时间 | 缓存创建时间 |

---

## 🔗 表关系说明

### 核心业务关系
- **notification_info** ↔ **mock_school_users**: 通过publisher_id关联，实现发布者信息查询
- **mock_school_users** ↔ **mock_role_permissions**: 通过role_code关联，实现权限验证
- **system_users** ↔ **system_dept**: 通过dept_id关联，实现组织架构

### 认证体系关系
- **Mock认证层**: mock_school_users + mock_role_permissions
- **OAuth2认证层**: system_oauth2_* 系列表
- **主系统层**: system_users + system_dept

### 日志审计关系
- **API监控**: infra_api_access_log + infra_api_error_log
- **登录审计**: system_login_log
- **业务审计**: notification_info (审批字段)

---

## 📈 数据统计分析

### 业务活跃度
- **高活跃表**: notification_info(15条), infra_api_access_log(488条)
- **中活跃表**: mock_school_users(12条), system_login_log(29条)
- **低活跃表**: system_users(1条), system_dept(2条)
- **待启用表**: OAuth2系列表(0条), weather_cache(0条)

### 存储空间评估
- **大数据量表**: infra_api_access_log, infra_api_error_log
- **中等数据量表**: notification_info, mock_role_permissions
- **小数据量表**: 其他系统配置表

---

## 🛡️ 安全特性

### 数据保护
- **软删除**: 所有业务表使用deleted字段实现软删除
- **多租户**: tenant_id字段支持数据隔离
- **审计追踪**: creator/create_time/updater/update_time完整审计

### 权限控制
- **角色权限**: mock_role_permissions定义细粒度权限
- **范围控制**: target_scope字段实现通知范围限制
- **审批流程**: notification_info包含完整审批字段

---

**📅 文档生成时间**: 2025年8月14日  
**🔄 最后更新**: T12.2任务 - 数据库表格结构中文文档生成  
**📊 统计数据**: 13个表, 180+个字段, 完整中文对照