-- Mock School API 相关表结构
-- 用于开发阶段模拟学校认证系统

-- 1. Mock School 用户表
CREATE TABLE `system_mock_school_user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
    `mobile` varchar(11) DEFAULT NULL COMMENT '手机号',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `password_hash` varchar(100) NOT NULL COMMENT '密码哈希',
    `school_code` varchar(50) NOT NULL COMMENT '学校编码',
    `school_name` varchar(100) NOT NULL COMMENT '学校名称',
    `department_id` varchar(50) DEFAULT NULL COMMENT '部门ID',
    `department_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
    `student_id` varchar(50) DEFAULT NULL COMMENT '学号',
    `teacher_id` varchar(50) DEFAULT NULL COMMENT '工号',
    `user_type` tinyint NOT NULL DEFAULT 5 COMMENT '用户类型：1-校级管理员 2-学院管理员 3-部门管理员 4-教师 5-学生',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '用户状态：1-正常 2-禁用 3-锁定',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username_school` (`username`, `school_code`, `deleted`, `tenant_id`),
    KEY `idx_school_code` (`school_code`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Mock School API 用户表';

-- 2. Mock School 角色表
CREATE TABLE `system_mock_school_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code` varchar(50) NOT NULL COMMENT '角色编码',
    `role_name` varchar(50) NOT NULL COMMENT '角色名称',
    `role_level` tinyint NOT NULL DEFAULT 5 COMMENT '角色层级：1-校级 2-学院级 3-部门级 4-班级 5-学生',
    `description` varchar(500) DEFAULT NULL COMMENT '角色描述',
    `scope_data` varchar(100) DEFAULT NULL COMMENT '数据权限范围',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-禁用',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `deleted`, `tenant_id`),
    KEY `idx_role_level` (`role_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Mock School API 角色表';

-- 3. Mock School 用户角色关联表
CREATE TABLE `system_mock_school_user_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`, `deleted`, `tenant_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Mock School API 用户角色关联表';

-- 插入初始化数据的存储过程（可选，主要通过代码初始化）
DELIMITER //
CREATE PROCEDURE InitMockSchoolData()
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION 
    BEGIN
        -- 如果出现异常，回滚事务
        ROLLBACK;
    END;

    START TRANSACTION;
    
    -- 检查是否已经有数据
    IF (SELECT COUNT(*) FROM system_mock_school_user) = 0 THEN
        
        -- 插入角色数据
        INSERT INTO system_mock_school_role (role_code, role_name, role_level, description, scope_data, sort, status) VALUES
        ('PRINCIPAL', '校长', 1, '学校最高管理者', '*', 1, 1),
        ('ACADEMIC_ADMIN', '教务管理员', 3, '教务部门管理员', 'DEPT_ACADEMIC', 2, 1),
        ('TEACHER', '教师', 4, '普通教师', 'CLASS', 3, 1),
        ('STUDENT', '学生', 5, '在读学生', 'SELF', 4, 1);
        
        -- 插入用户数据（密码：$2a$04$mRMIYLDtRHlf6.9kMhV.YOgxMii9tCn6pGtb00WsGkC8RkIAec7Nu 对应 123456）
        INSERT INTO system_mock_school_user (username, real_name, mobile, email, password_hash, school_code, school_name, department_name, user_type, status) VALUES
        ('principal001', '张校长', '13800000001', 'principal@univ001.edu.cn', '$2a$04$mRMIYLDtRHlf6.9kMhV.YOgxMii9tCn6pGtb00WsGkC8RkIAec7Nu', 'UNIV001', '示例大学', NULL, 1, 1),
        ('admin001', '李管理员', '13800000002', 'admin@univ001.edu.cn', '$2a$04$mRMIYLDtRHlf6.9kMhV.YOgxMii9tCn6pGtb00WsGkC8RkIAec7Nu', 'UNIV001', '示例大学', '教务处', 3, 1),
        ('teacher001', '王老师', '13800000003', 'teacher001@univ001.edu.cn', '$2a$04$mRMIYLDtRHlf6.9kMhV.YOgxMii9tCn6pGtb00WsGkC8RkIAec7Nu', 'UNIV001', '示例大学', '计算机科学与技术学院', 4, 1),
        ('student001', '小明', '13800000004', 'student001@student.univ001.edu.cn', '$2a$04$mRMIYLDtRHlf6.9kMhV.YOgxMii9tCn6pGtb00WsGkC8RkIAec7Nu', 'UNIV001', '示例大学', '计算机科学与技术学院', 5, 1);
        
        -- 更新用户的学号和工号
        UPDATE system_mock_school_user SET teacher_id = 'T202401001', department_id = 'DEPT_CS' WHERE username = 'teacher001';
        UPDATE system_mock_school_user SET student_id = 'S202401001', department_id = 'DEPT_CS' WHERE username = 'student001';
        UPDATE system_mock_school_user SET department_id = 'DEPT_ACADEMIC' WHERE username = 'admin001';
        
        -- 分配用户角色
        INSERT INTO system_mock_school_user_role (user_id, role_id)
        SELECT u.id, r.id 
        FROM system_mock_school_user u, system_mock_school_role r
        WHERE (u.username = 'principal001' AND r.role_code = 'PRINCIPAL')
           OR (u.username = 'admin001' AND r.role_code = 'ACADEMIC_ADMIN') 
           OR (u.username = 'teacher001' AND r.role_code = 'TEACHER')
           OR (u.username = 'student001' AND r.role_code = 'STUDENT');
           
    END IF;
    
    COMMIT;
END //
DELIMITER ;

-- 执行初始化（可选，主要通过代码初始化）
-- CALL InitMockSchoolData();

-- 删除存储过程
-- DROP PROCEDURE IF EXISTS InitMockSchoolData;