package cn.iocoder.yudao.mock.school.exception;

/**
 * 学校API调用异常
 * 封装Mock和Real模式下的所有异常情况
 * 
 * @author Claude
 * @since 2025-09-04
 */
public class SchoolApiException extends Exception {
    
    private final String errorCode;
    private final String mode;
    
    public SchoolApiException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.mode = "UNKNOWN";
    }
    
    public SchoolApiException(String message, String errorCode, String mode) {
        super(message);
        this.errorCode = errorCode;
        this.mode = mode;
    }
    
    public SchoolApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.mode = "UNKNOWN";
    }
    
    public SchoolApiException(String message, Throwable cause, String errorCode, String mode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.mode = mode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getMode() {
        return mode;
    }
    
    @Override
    public String toString() {
        return String.format("SchoolApiException[mode=%s, code=%s, message=%s]", 
                           mode, errorCode, getMessage());
    }
}