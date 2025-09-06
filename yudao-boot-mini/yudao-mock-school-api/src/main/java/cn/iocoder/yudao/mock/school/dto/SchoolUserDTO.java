package cn.iocoder.yudao.mock.school.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 学校用户信息DTO
 * 用于接收真实学校API返回的用户信息
 * 
 * 对应学校API返回字段：
 * - userNumber: 工号/学号
 * - grade: 年级信息
 * - className: 班级信息  
 * - role: 角色数组
 * - Basic Token: UUID格式
 * 
 * @author Backend-Developer (based on Gemini 2.5 Pro recommendations)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolUserDTO {

    /**
     * 工号/学号
     * 对应学校API的userNumber字段
     */
    private String userNumber;

    /**
     * 用户真实姓名
     * 从学校API获取的真实姓名
     */
    private String realName;

    /**
     * 年级信息
     * 对应学校API的grade字段
     * 示例: "2023"、"2022"等
     */
    private String grade;

    /**
     * 班级信息
     * 对应学校API的className字段
     * 示例: "计算机科学与技术1班"
     */
    private String className;

    /**
     * 角色数组
     * 对应学校API的role字段
     * 学校可能返回多个角色
     */
    private List<String> role;

    /**
     * Basic Token (UUID格式)
     * 从学校API获取，需要保存到后端
     */
    private String basicToken;

    /**
     * Token过期时间戳
     * 学校API返回的过期时间
     */
    private Long tokenExpireTime;

    /**
     * 部门信息
     * 学校API可能返回的部门信息
     */
    private String department;

    /**
     * 学院信息
     * 学校API可能返回的学院信息
     */
    private String college;

    /**
     * 学生/教师类型标识
     * 从角色信息推断得出
     */
    private String userType;

    /**
     * 邮箱地址
     * 学校API可能返回的邮箱信息
     */
    private String email;

    /**
     * 手机号码
     * 学校API可能返回的手机号信息
     */
    private String phone;

    /**
     * 学校API原始响应数据
     * 用于调试和扩展，生产环境可移除
     */
    private Object rawResponse;

    /**
     * 构造函数：基础信息
     */
    public SchoolUserDTO(String userNumber, String realName, String grade, String className, List<String> role, String basicToken) {
        this.userNumber = userNumber;
        this.realName = realName;
        this.grade = grade;
        this.className = className;
        this.role = role;
        this.basicToken = basicToken;
    }

    /**
     * 获取主要角色
     * 从角色数组中获取第一个角色作为主要角色
     */
    public String getPrimaryRole() {
        if (role != null && !role.isEmpty()) {
            return role.get(0);
        }
        return "UNKNOWN";
    }

    /**
     * 判断是否为学生
     */
    public boolean isStudent() {
        return role != null && role.stream()
                .anyMatch(r -> r.toLowerCase().contains("student") || r.toLowerCase().contains("学生"));
    }

    /**
     * 判断是否为教师
     */
    public boolean isTeacher() {
        return role != null && role.stream()
                .anyMatch(r -> r.toLowerCase().contains("teacher") || r.toLowerCase().contains("教师"));
    }

    @Override
    public String toString() {
        return "SchoolUserDTO{" +
                "userNumber='" + userNumber + '\'' +
                ", realName='" + realName + '\'' +
                ", grade='" + grade + '\'' +
                ", className='" + className + '\'' +
                ", role=" + role +
                ", basicToken='" + (basicToken != null ? basicToken.substring(0, Math.min(basicToken.length(), 8)) + "..." : "null") + '\'' +
                ", tokenExpireTime=" + tokenExpireTime +
                ", department='" + department + '\'' +
                ", college='" + college + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }
}