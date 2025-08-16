package cn.iocoder.yudao.mock.school.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户认证请求DTO
 * 用于工号+姓名+密码登录认证 (根据业务逻辑文档要求)
 * 
 * @author Claude
 */
public class AuthenticateRequest {

    // 新方式：工号+姓名+密码登录
    private String employeeId;
    private String name;

    @NotBlank(message = "密码不能为空")
    private String password;

    // 保留username字段用于向后兼容
    private String username;

    // 构造函数
    public AuthenticateRequest() {}

    public AuthenticateRequest(String employeeId, String name, String password) {
        this.employeeId = employeeId;
        this.name = name;
        this.password = password;
    }

    // 向后兼容的构造函数
    public AuthenticateRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "AuthenticateRequest{" +
                "employeeId='" + employeeId + '\'' +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", password='***'" + // 不记录明文密码
                '}';
    }
}