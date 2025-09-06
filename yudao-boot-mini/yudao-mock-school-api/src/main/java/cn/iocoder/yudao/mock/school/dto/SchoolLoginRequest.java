package cn.iocoder.yudao.mock.school.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 学校登录请求DTO
 * 用于真实学校API登录认证和双Token生成
 * 
 * 支持两种登录模式：
 * 1. 真实学校API模式：调用https://work.greathiit.com/api/user/loginWai
 * 2. Mock模式：使用现有Mock API认证机制
 * 
 * @author Backend-Developer (based on Gemini 2.5 Pro recommendations)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolLoginRequest {

    /**
     * 学号/工号（必需）
     * 对应学校API的userNumber字段
     */
    @NotBlank(message = "学号/工号不能为空")
    private String employeeId;

    /**
     * 用户姓名（必需）
     * 用于身份二次验证
     */
    @NotBlank(message = "用户姓名不能为空")
    private String name;

    /**
     * 登录密码（必需）
     * 对应学校API的password字段
     */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /**
     * 自动登录标志
     * 对应学校API的autoLogin字段，默认为true
     */
    private Boolean autoLogin = true;

    /**
     * 认证提供者
     * 对应学校API的provider字段，默认为"account"
     */
    private String provider = "account";

    /**
     * 学校API模式标志
     * true: 调用真实学校API
     * false: 使用Mock API (默认开发模式)
     */
    private Boolean useRealSchoolApi = false;

    /**
     * 构造函数：标准登录方式
     */
    public SchoolLoginRequest(String employeeId, String name, String password) {
        this.employeeId = employeeId;
        this.name = name;
        this.password = password;
        this.autoLogin = true;
        this.provider = "account";
        this.useRealSchoolApi = false;
    }

    /**
     * 构造函数：指定学校API模式
     */
    public SchoolLoginRequest(String employeeId, String name, String password, Boolean useRealSchoolApi) {
        this.employeeId = employeeId;
        this.name = name;
        this.password = password;
        this.autoLogin = true;
        this.provider = "account";
        this.useRealSchoolApi = useRealSchoolApi;
    }

    @Override
    public String toString() {
        return "SchoolLoginRequest{" +
                "employeeId='" + employeeId + '\'' +
                ", name='" + name + '\'' +
                ", password='***'" + // 安全：不记录明文密码
                ", autoLogin=" + autoLogin +
                ", provider='" + provider + '\'' +
                ", useRealSchoolApi=" + useRealSchoolApi +
                '}';
    }
}