package cn.iocoder.yudao.mock.school.model;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * 统一的学校用户信息模型
 * 兼容Mock和Real两种实现模式
 * 
 * @author Claude
 * @since 2025-09-04
 */
public class SchoolUserInfo implements Serializable {
    
    private String studentNo;     // 学号或工号
    private String name;          // 姓名
    private String college;       // 学院
    private String major;         // 专业
    private String className;     // 班级
    private String phone;         // 电话
    private String gender;        // 性别
    private String grade;         // 年级
    private String[] roles;       // 角色数组
    private String userType;      // 用户类型：student/teacher
    private String basicToken;    // Basic Token (Real模式)
    private Map<String, Object> rawData; // 原始数据，便于调试
    
    public SchoolUserInfo() {
        this.rawData = new HashMap<>();
    }
    
    public SchoolUserInfo(String studentNo, String name) {
        this();
        this.studentNo = studentNo;
        this.name = name;
    }
    
    // Getter and Setter methods
    public String getStudentNo() {
        return studentNo;
    }
    
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCollege() {
        return college;
    }
    
    public void setCollege(String college) {
        this.college = college;
    }
    
    public String getMajor() {
        return major;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getGrade() {
        return grade;
    }
    
    public void setGrade(String grade) {
        this.grade = grade;
    }
    
    public String[] getRoles() {
        return roles;
    }
    
    public void setRoles(String[] roles) {
        this.roles = roles;
    }
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
    
    public String getBasicToken() {
        return basicToken;
    }
    
    public void setBasicToken(String basicToken) {
        this.basicToken = basicToken;
    }
    
    public Map<String, Object> getRawData() {
        return rawData;
    }
    
    public void setRawData(Map<String, Object> rawData) {
        this.rawData = rawData;
    }
    
    // 便利方法
    public void addRawData(String key, Object value) {
        if (this.rawData == null) {
            this.rawData = new HashMap<>();
        }
        this.rawData.put(key, value);
    }
    
    @Override
    public String toString() {
        return String.format("SchoolUserInfo{studentNo='%s', name='%s', college='%s', " +
                           "major='%s', className='%s', grade='%s', userType='%s'}", 
                           studentNo, name, college, major, className, grade, userType);
    }
}