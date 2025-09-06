package cn.iocoder.yudao.mock.school.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 学校API配置属性
 * 支持Mock和Real两种模式的配置驱动切换
 * 
 * @author Claude
 * @since 2025-09-04
 */
@ConfigurationProperties(prefix = "school.api")
public class SchoolApiProperties {
    
    /**
     * API模式：MOCK(开发测试) 或 REAL(生产环境)
     */
    private Mode mode = Mode.MOCK;
    
    /**
     * 是否启用降级机制：Real模式失败时自动切换到Mock模式
     */
    private boolean fallbackEnabled = true;
    
    /**
     * Mock模式配置
     */
    private MockConfig mock = new MockConfig();
    
    /**
     * Real模式配置
     */
    private RealConfig real = new RealConfig();
    
    // Getters and Setters
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }
    
    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }
    
    public MockConfig getMock() {
        return mock;
    }
    
    public void setMock(MockConfig mock) {
        this.mock = mock;
    }
    
    public RealConfig getReal() {
        return real;
    }
    
    public void setReal(RealConfig real) {
        this.real = real;
    }
    
    /**
     * API模式枚举
     */
    public enum Mode {
        MOCK,  // Mock模式：使用内存数据
        REAL   // Real模式：调用真实学校API
    }
    
    /**
     * Mock模式配置
     */
    public static class MockConfig {
        private boolean enabled = true;
        private long delayMs = 200; // 模拟网络延迟
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public long getDelayMs() {
            return delayMs;
        }
        
        public void setDelayMs(long delayMs) {
            this.delayMs = delayMs;
        }
    }
    
    /**
     * Real模式配置
     */
    public static class RealConfig {
        private String baseUrl = "https://work.greathiit.com";
        private String path = "/api/user/loginWai";
        private String method = "POST";
        private String usernameField = "userNumber";
        private String passwordField = "password";
        private long connectTimeoutMs = 5000;
        private long readTimeoutMs = 10000;
        private int maxRetries = 2;
        private long retryDelayMs = 1000;
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public String getUsernameField() {
            return usernameField;
        }
        
        public void setUsernameField(String usernameField) {
            this.usernameField = usernameField;
        }
        
        public String getPasswordField() {
            return passwordField;
        }
        
        public void setPasswordField(String passwordField) {
            this.passwordField = passwordField;
        }
        
        public long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }
        
        public void setConnectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }
        
        public long getReadTimeoutMs() {
            return readTimeoutMs;
        }
        
        public void setReadTimeoutMs(long readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        public long getRetryDelayMs() {
            return retryDelayMs;
        }
        
        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }
    }
    
    @Override
    public String toString() {
        return String.format("SchoolApiProperties{mode=%s, fallbackEnabled=%s, " +
                           "real.baseUrl=%s, real.path=%s}", 
                           mode, fallbackEnabled, real.baseUrl, real.path);
    }
}