package cn.iocoder.yudao.mock.school.service;

import cn.iocoder.yudao.mock.school.dto.SchoolUserDTO;

/**
 * 学校API客户端接口
 * 专门负责与学校统一身份认证API进行HTTP通信
 * 
 * 职责：
 * 1. 调用真实学校API进行用户身份验证
 * 2. 处理HTTP请求和响应
 * 3. 异常处理和重试机制
 * 4. 将外部依赖完全隔离
 * 
 * 实现方式：
 * - 使用RestTemplate或WebClient进行HTTP调用
 * - 支持超时和重试配置
 * - 完整的异常处理
 * 
 * @author Backend-Developer (based on Gemini 2.5 Pro recommendations)
 */
public interface SchoolApiClient {

    /**
     * 调用学校API进行用户身份认证
     * 
     * @param userNumber 工号/学号
     * @param password 登录密码
     * @param autoLogin 自动登录标志（默认true）
     * @param provider 认证提供者（默认"account"）
     * @return 学校API返回的用户信息，包含Basic Token
     * @throws SchoolApiException 学校API调用异常
     */
    SchoolUserDTO authenticateUser(String userNumber, String password, Boolean autoLogin, String provider) 
            throws SchoolApiException;

    /**
     * 刷新Basic Token
     * 当Basic Token过期时调用学校API刷新
     * 
     * @param userNumber 工号/学号
     * @param oldBasicToken 旧的Basic Token
     * @return 新的Basic Token
     * @throws SchoolApiException 学校API调用异常
     */
    String refreshBasicToken(String userNumber, String oldBasicToken) throws SchoolApiException;

    /**
     * 验证Basic Token有效性
     * 调用学校API验证Token是否仍然有效
     * 
     * @param basicToken Basic Token
     * @return true如果Token有效，false如果无效
     * @throws SchoolApiException 学校API调用异常
     */
    boolean validateBasicToken(String basicToken) throws SchoolApiException;

    /**
     * 获取学校API服务状态
     * 用于健康检查和服务可用性检测
     * 
     * @return true如果服务可用，false如果不可用
     */
    boolean isServiceAvailable();

    /**
     * 学校API异常类
     * 用于包装学校API调用过程中的各种异常
     */
    class SchoolApiException extends Exception {
        private final int statusCode;
        private final String errorCode;

        public SchoolApiException(String message) {
            super(message);
            this.statusCode = 500;
            this.errorCode = "UNKNOWN_ERROR";
        }

        public SchoolApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
            this.errorCode = "HTTP_ERROR_" + statusCode;
        }

        public SchoolApiException(String message, String errorCode, int statusCode) {
            super(message);
            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }

        public SchoolApiException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = 500;
            this.errorCode = "UNKNOWN_ERROR";
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}