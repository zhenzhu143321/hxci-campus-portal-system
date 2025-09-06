package cn.iocoder.yudao.mock.school.client.impl;

import cn.iocoder.yudao.mock.school.client.SchoolApiClient;
import cn.iocoder.yudao.mock.school.model.SchoolUserInfo;
import cn.iocoder.yudao.mock.school.exception.SchoolApiException;
import cn.iocoder.yudao.mock.school.config.SchoolApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 带降级机制的学校API客户端
 * Primary模式失败时自动切换到Mock模式
 * 确保系统稳定性和可用性
 * 
 * @author Claude
 * @since 2025-09-04
 */
public class FallbackSchoolApiClient implements SchoolApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(FallbackSchoolApiClient.class);
    
    private final SchoolApiClient primaryClient;
    private final SchoolApiClient fallbackClient; // 通常是Mock客户端
    private final SchoolApiProperties properties;
    
    public FallbackSchoolApiClient(SchoolApiClient primaryClient, 
                                 SchoolApiClient fallbackClient,
                                 SchoolApiProperties properties) {
        this.primaryClient = primaryClient;
        this.fallbackClient = fallbackClient;
        this.properties = properties;
        
        log.info("🛡️ [FALLBACK] 初始化降级客户端: primary={}, fallback={}", 
                primaryClient.getMode(), fallbackClient.getMode());
    }
    
    @Override
    public SchoolUserInfo login(String username, String password) throws SchoolApiException {
        
        // 第一次尝试：使用主客户端
        try {
            log.info("🎯 [FALLBACK] 尝试使用主客户端登录: mode={}, username={}", 
                    primaryClient.getMode(), username);
            
            SchoolUserInfo result = primaryClient.login(username, password);
            
            log.info("✅ [FALLBACK] 主客户端登录成功: mode={}, user={}", 
                    primaryClient.getMode(), result.getName());
            
            return result;
            
        } catch (SchoolApiException e) {
            log.warn("⚠️ [FALLBACK] 主客户端登录失败: mode={}, error={}", 
                    primaryClient.getMode(), e.getMessage());
            
            // 只有当启用降级机制时才进行降级
            if (!properties.isFallbackEnabled()) {
                log.error("🚨 [FALLBACK] 降级机制已禁用，直接抛出异常");
                throw e;
            }
            
            // 判断是否需要降级（某些错误不适合降级，如认证失败）
            if (shouldFallback(e)) {
                return performFallback(username, password, e);
            } else {
                log.warn("⚠️ [FALLBACK] 错误类型不适合降级，直接抛出: errorCode={}", e.getErrorCode());
                throw e;
            }
            
        } catch (Exception e) {
            log.error("💥 [FALLBACK] 主客户端出现未知异常", e);
            
            if (properties.isFallbackEnabled()) {
                return performFallback(username, password, 
                    new SchoolApiException("主客户端未知异常: " + e.getMessage(), e, "UNKNOWN", primaryClient.getMode()));
            } else {
                throw new SchoolApiException("主客户端异常且降级禁用: " + e.getMessage(), e, "UNKNOWN", primaryClient.getMode());
            }
        }
    }
    
    /**
     * 执行降级操作
     */
    private SchoolUserInfo performFallback(String username, String password, 
                                         SchoolApiException originalException) throws SchoolApiException {
        try {
            log.info("🔄 [FALLBACK] 开始降级到备用客户端: mode={}", fallbackClient.getMode());
            
            SchoolUserInfo result = fallbackClient.login(username, password);
            
            // 在结果中添加降级标识
            result.addRawData("fallbackUsed", true);
            result.addRawData("originalError", originalException.getMessage());
            result.addRawData("originalMode", originalException.getMode());
            result.addRawData("fallbackMode", fallbackClient.getMode());
            
            log.info("🛡️ [FALLBACK] 降级登录成功: fallbackMode={}, user={}", 
                    fallbackClient.getMode(), result.getName());
            
            return result;
            
        } catch (Exception fallbackException) {
            log.error("💥 [FALLBACK] 降级客户端也失败了", fallbackException);
            
            // 降级也失败，抛出综合异常信息
            String errorMsg = String.format("主客户端失败: %s; 降级客户端也失败: %s", 
                                          originalException.getMessage(), 
                                          fallbackException.getMessage());
            throw new SchoolApiException(errorMsg, originalException, "FALLBACK_FAILED", "FALLBACK");
        }
    }
    
    /**
     * 判断是否应该进行降级
     * 某些错误（如认证失败）不适合降级，应该直接返回给用户
     */
    private boolean shouldFallback(SchoolApiException e) {
        String errorCode = e.getErrorCode();
        
        // 不适合降级的错误类型
        switch (errorCode) {
            case "AUTH_FAILED":        // 认证失败 - 用户名密码错误
            case "USER_NOT_FOUND":     // 用户不存在
                log.debug("🚫 [FALLBACK] 认证类错误不适合降级: {}", errorCode);
                return false;
                
            case "NETWORK_ERROR":      // 网络错误 - 适合降级
            case "SERVER_ERROR":       // 服务器错误 - 适合降级  
            case "CLIENT_ERROR":       // 客户端错误 - 适合降级
            case "API_ERROR":          // API错误 - 适合降级
            case "PARSE_ERROR":        // 解析错误 - 适合降级
            case "SYSTEM_ERROR":       // 系统错误 - 适合降级
            default:
                log.debug("✅ [FALLBACK] 错误适合降级: {}", errorCode);
                return true;
        }
    }
    
    @Override
    public String getMode() {
        return String.format("FALLBACK[primary=%s, fallback=%s]", 
                           primaryClient.getMode(), fallbackClient.getMode());
    }
}