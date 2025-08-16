package cn.iocoder.yudao.module.system.enums.mockschool;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Mock School API 角色层级枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum SchoolRoleLevelEnum {

    SCHOOL_LEVEL(1, "校级权限", "校长、副校长等校级管理员"),
    COLLEGE_LEVEL(2, "学院级权限", "院长、党委书记等学院管理员"),  
    DEPT_LEVEL(3, "部门级权限", "系主任、处长等部门管理员"),
    CLASS_LEVEL(4, "班级权限", "辅导员、班主任等班级管理员"),
    STUDENT_LEVEL(5, "学生权限", "学生、班干部等");
    
    /**
     * 层级等级
     */
    private final Integer level;
    /**
     * 层级名称
     */
    private final String name;
    /**
     * 层级描述
     */
    private final String description;

}