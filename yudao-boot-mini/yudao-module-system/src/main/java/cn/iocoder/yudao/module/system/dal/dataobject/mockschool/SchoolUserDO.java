package cn.iocoder.yudao.module.system.dal.dataobject.mockschool;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Mock School API 用户数据对象
 * 
 * @author 芋道源码
 */
@TableName("system_mock_school_user")
@KeySequence("system_mock_school_user_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolUserDO extends TenantBaseDO {

    /**
     * 用户ID
     */
    @TableId
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 真实姓名
     */
    private String realName;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 密码哈希
     */
    private String passwordHash;
    /**
     * 学校编码
     */
    private String schoolCode;
    /**
     * 学校名称
     */
    private String schoolName;
    /**
     * 部门ID
     */
    private String departmentId;
    /**
     * 部门名称
     */
    private String departmentName;
    /**
     * 学号(学生用户专用)
     */
    private String studentId;
    /**
     * 工号(教职工用户专用)
     */
    private String teacherId;
    /**
     * 用户类型：1-校级管理员 2-学院管理员 3-部门管理员 4-教师 5-学生
     */
    private Integer userType;
    /**
     * 用户状态：1-正常 2-禁用 3-锁定
     */
    private Integer status;
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    /**
     * 最后登录IP
     */
    private String lastLoginIp;

}