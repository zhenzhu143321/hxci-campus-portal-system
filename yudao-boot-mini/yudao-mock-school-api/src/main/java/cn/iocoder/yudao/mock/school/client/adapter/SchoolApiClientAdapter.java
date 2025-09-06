package cn.iocoder.yudao.mock.school.client.adapter;

import cn.iocoder.yudao.mock.school.model.SchoolUserInfo;
import cn.iocoder.yudao.mock.school.exception.SchoolApiException;
import cn.iocoder.yudao.mock.school.dto.SchoolUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 学校API客户端适配器
 * 将新的配置驱动SchoolApiClient适配到现有的Legacy接口
 * 保持向后兼容性，避免重大重构
 * 
 * @author Claude
 * @since 2025-09-04
 */
@Component
public class SchoolApiClientAdapter implements cn.iocoder.yudao.mock.school.service.SchoolApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(SchoolApiClientAdapter.class);
    
    @Autowired
    private cn.iocoder.yudao.mock.school.client.SchoolApiClient configDrivenClient; // 新的配置驱动客户端
    
    @Override
    public SchoolUserDTO authenticateUser(String userNumber, String password, 
                                        Boolean autoLogin, String provider) 
            throws cn.iocoder.yudao.mock.school.service.SchoolApiClient.SchoolApiException {
        
        log.info("🔄 [ADAPTER] 适配器处理登录请求: userNumber={}, mode={}", 
                userNumber, configDrivenClient.getMode());
        
        try {
            // 使用新的配置驱动客户端进行认证
            SchoolUserInfo schoolUserInfo = configDrivenClient.login(userNumber, password);
            
            // 将新的SchoolUserInfo转换为Legacy的SchoolUserDTO
            SchoolUserDTO legacyDto = convertToLegacyDto(schoolUserInfo);
            
            log.info("✅ [ADAPTER] 适配器认证成功: user={}, mode={}", 
                    legacyDto.getRealName(), configDrivenClient.getMode());
            
            return legacyDto;
            
        } catch (cn.iocoder.yudao.mock.school.exception.SchoolApiException e) {
            log.warn("❌ [ADAPTER] 新客户端认证失败: {}", e.getMessage());
            
            // 转换异常类型
            throw new cn.iocoder.yudao.mock.school.service.SchoolApiClient.SchoolApiException(
                e.getMessage(), 
                e.getErrorCode(),
                500  // 默认状态码
            );
        } catch (Exception e) {
            log.error("💥 [ADAPTER] 适配器出现未知异常", e);
            throw new cn.iocoder.yudao.mock.school.service.SchoolApiClient.SchoolApiException(
                "适配器异常: " + e.getMessage(), "ADAPTER_ERROR", 500);
        }
    }
    
    /**
     * 将新的SchoolUserInfo转换为Legacy的SchoolUserDTO
     */
    private SchoolUserDTO convertToLegacyDto(SchoolUserInfo userInfo) {
        SchoolUserDTO dto = new SchoolUserDTO();
        
        // 基本信息映射
        dto.setUserNumber(userInfo.getStudentNo());
        dto.setRealName(userInfo.getName()); // 使用realName字段
        dto.setCollege(userInfo.getCollege());
        dto.setClassName(userInfo.getClassName());
        dto.setGrade(userInfo.getGrade());
        dto.setBasicToken(userInfo.getBasicToken());
        
        // 角色处理 - 转换为List<String>
        if (userInfo.getRoles() != null && userInfo.getRoles().length > 0) {
            java.util.List<String> roleList = new java.util.ArrayList<>();
            for (String role : userInfo.getRoles()) {
                roleList.add(role);
            }
            dto.setRole(roleList);
        } else {
            java.util.List<String> defaultRole = new java.util.ArrayList<>();
            defaultRole.add("学生");
            dto.setRole(defaultRole);
        }
        
        // 用户类型映射
        if ("teacher".equals(userInfo.getUserType())) {
            dto.setUserType("teacher");
        } else {
            dto.setUserType("student");
        }
        
        // Token过期时间（假设30天有效期）
        long expireTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);
        dto.setTokenExpireTime(expireTime);
        
        log.debug("🔧 [ADAPTER] 数据转换完成: userNumber={}, realName={}, type={}", 
                 dto.getUserNumber(), dto.getRealName(), dto.getUserType());
        
        return dto;
    }
    
    @Override
    public String refreshBasicToken(String userNumber, String oldBasicToken) 
            throws cn.iocoder.yudao.mock.school.service.SchoolApiClient.SchoolApiException {
        log.warn("🚫 [ADAPTER] refreshBasicToken方法暂未实现: userNumber={}", userNumber);
        throw new cn.iocoder.yudao.mock.school.service.SchoolApiClient.SchoolApiException(
            "适配器暂不支持refreshBasicToken功能", "NOT_IMPLEMENTED", 501);
    }
    
    @Override
    public boolean validateBasicToken(String basicToken) 
            throws cn.iocoder.yudao.mock.school.service.SchoolApiClient.SchoolApiException {
        log.warn("🚫 [ADAPTER] validateBasicToken方法暂未实现");
        throw new cn.iocoder.yudao.mock.school.service.SchoolApiClient.SchoolApiException(
            "适配器暂不支持validateBasicToken功能", "NOT_IMPLEMENTED", 501);
    }
    
    @Override
    public boolean isServiceAvailable() {
        log.debug("✅ [ADAPTER] 配置驱动客户端可用性检查: mode={}", configDrivenClient.getMode());
        return true; // 配置驱动客户端总是可用的（有降级机制）
    }
}