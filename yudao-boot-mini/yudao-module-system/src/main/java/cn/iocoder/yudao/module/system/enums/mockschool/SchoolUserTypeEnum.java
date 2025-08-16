package cn.iocoder.yudao.module.system.enums.mockschool;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Mock School API 用户类型枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum SchoolUserTypeEnum {

    SCHOOL_ADMIN(1, "校级管理员", "校长、副校长等校级管理人员"),
    COLLEGE_ADMIN(2, "学院管理员", "院长、党委书记等学院管理人员"),
    DEPT_ADMIN(3, "部门管理员", "系主任、处长等部门管理人员"),
    TEACHER(4, "教师", "普通教师、讲师、教授等教学人员"),
    STUDENT(5, "学生", "在读学生");
    
    /**
     * 用户类型
     */
    private final Integer type;
    /**
     * 类型名称
     */
    private final String name;
    /**
     * 类型描述
     */
    private final String description;

}