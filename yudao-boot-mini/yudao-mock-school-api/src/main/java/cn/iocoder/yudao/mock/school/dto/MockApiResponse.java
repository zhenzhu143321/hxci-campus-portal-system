package cn.iocoder.yudao.mock.school.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mock API统一响应格式
 * 
 * @author Claude
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockApiResponse<T> {

    /**
     * 响应码：200=成功, 400=请求错误, 401=未认证, 403=无权限, 500=服务器错误
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }

    /**
     * 创建成功响应
     */
    public static <T> MockApiResponse<T> success(T data) {
        return new MockApiResponse<>(200, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 创建成功响应（自定义消息）
     */
    public static <T> MockApiResponse<T> success(T data, String message) {
        return new MockApiResponse<>(200, message, data, LocalDateTime.now());
    }

    /**
     * 创建错误响应
     */
    public static <T> MockApiResponse<T> error(Integer code, String message) {
        return new MockApiResponse<>(code, message, null, LocalDateTime.now());
    }

    /**
     * 创建未认证响应
     */
    public static <T> MockApiResponse<T> unauthorized(String message) {
        return error(401, message);
    }

    /**
     * 创建无权限响应
     */
    public static <T> MockApiResponse<T> forbidden(String message) {
        return error(403, message);
    }

    /**
     * 创建请求错误响应
     */
    public static <T> MockApiResponse<T> badRequest(String message) {
        return error(400, message);
    }

    /**
     * 创建服务器错误响应
     */
    public static <T> MockApiResponse<T> serverError(String message) {
        return error(500, message);
    }
}