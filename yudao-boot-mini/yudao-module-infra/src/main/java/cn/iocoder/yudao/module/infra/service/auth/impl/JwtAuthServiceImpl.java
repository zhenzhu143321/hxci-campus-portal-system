package cn.iocoder.yudao.module.infra.service.auth.impl;

import cn.iocoder.yudao.module.infra.service.auth.JwtAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证服务实现类
 * 支持两步认证架构：JWT token解析 + Mock API权限验证
 * 
 * @author Claude
 */
@Service
@Slf4j
public class JwtAuthServiceImpl implements JwtAuthService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${mock-school-api.base-url:http://localhost:48082}")
    private String mockApiBaseUrl;

    @Override
    public UserAuthInfo parseJwtToken(String jwtToken) {
        log.info("🔍 [JWT_AUTH] 解析JWT Token，长度: {}", jwtToken != null ? jwtToken.length() : 0);
        
        UserAuthInfo authInfo = new UserAuthInfo();
        authInfo.setValid(false);
        
        try {
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                log.warn("❌ [JWT_AUTH] Token为空");
                return authInfo;
            }
            
            // 移除Bearer前缀（如果存在）
            String actualToken = jwtToken;
            if (jwtToken.startsWith("Bearer ")) {
                actualToken = jwtToken.substring(7);
            }
            
            // 解析JWT格式: header.payload.signature
            String[] parts = actualToken.split("\\.");
            if (parts.length != 3) {
                log.warn("❌ [JWT_AUTH] Token格式错误，部分数量: {}", parts.length);
                return authInfo;
            }
            
            // 解码payload
            String payloadJson = new String(Base64.getDecoder().decode(parts[1]));
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            
            // 验证token是否过期
            Long exp = ((Number) payload.get("exp")).longValue();
            if (System.currentTimeMillis() / 1000 > exp) {
                log.warn("❌ [JWT_AUTH] Token已过期, exp: {}", exp);
                return authInfo;
            }
            
            // 提取用户信息
            authInfo.setEmployeeId((String) payload.get("employeeId"));
            authInfo.setUsername((String) payload.get("username"));
            authInfo.setRealName((String) payload.get("realName"));
            authInfo.setRoleCode((String) payload.get("roleCode"));
            authInfo.setRoleName((String) payload.get("roleName"));
            authInfo.setUserType((String) payload.get("userType"));
            
            if (payload.get("departmentId") != null) {
                authInfo.setDepartmentId(((Number) payload.get("departmentId")).longValue());
            }
            authInfo.setDepartmentName((String) payload.get("departmentName"));
            authInfo.setValid(true);
            
            log.info("✅ [JWT_AUTH] Token解析成功: employeeId={}, username={}, roleCode={}", 
                    authInfo.getEmployeeId(), authInfo.getUsername(), authInfo.getRoleCode());
            
            return authInfo;
            
        } catch (Exception e) {
            log.error("❌ [JWT_AUTH] Token解析失败", e);
            return authInfo;
        }
    }

    @Override
    public AuthResult verifyNotificationPermission(String jwtToken, Integer notificationLevel, String targetScope) {
        log.info("🔒 [PERMISSION_CHECK] 开始权限验证: level={}, scope={}", notificationLevel, targetScope);
        
        // 第一步：解析JWT Token
        UserAuthInfo userInfo = parseJwtToken(jwtToken);
        if (!userInfo.isValid()) {
            return createDeniedResult("Token无效或已过期", null, null, null, null);
        }
        
        // 第二步：通过Mock API验证权限
        try {
            return verifyPermissionViaApi(jwtToken, notificationLevel, targetScope);
        } catch (Exception e) {
            log.error("❌ [PERMISSION_CHECK] API权限验证失败", e);
            return createDeniedResult("权限验证服务异常", userInfo.getEmployeeId(), 
                    userInfo.getUsername(), userInfo.getRoleCode(), userInfo.getRoleName());
        }
    }

    @Override
    public AuthResult verifyPermissionViaApi(String token, Integer notificationLevel, String targetScope) {
        log.info("🌐 [API_VERIFY] 调用Mock API验证权限: level={}, scope={}", notificationLevel, targetScope);
        
        try {
            // 构建请求
            String url = mockApiBaseUrl + "/mock-school-api/auth/verify-permission";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("notificationLevel", notificationLevel);
            requestBody.put("targetScope", targetScope);
            requestBody.put("permissionType", "PUBLISH_NOTIFICATION");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.get("code").equals(200)) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                
                AuthResult result = new AuthResult();
                result.setPermissionGranted((Boolean) data.get("permissionGranted"));
                result.setEmployeeId((String) data.get("employeeId"));
                result.setUsername((String) data.get("username"));
                result.setRoleCode((String) data.get("roleCode"));
                result.setRoleName((String) data.get("roleName"));
                result.setMessage((String) data.get("message"));
                result.setRequiresApproval((Boolean) data.getOrDefault("requiresApproval", false));
                result.setApproverRole((String) data.get("approverRole"));
                
                log.info("✅ [API_VERIFY] 权限验证完成: granted={}, message={}", 
                        result.isPermissionGranted(), result.getMessage());
                
                return result;
            } else {
                String errorMessage = responseBody != null ? (String) responseBody.get("message") : "API响应错误";
                log.warn("❌ [API_VERIFY] 权限验证失败: {}", errorMessage);
                return createDeniedResult(errorMessage, null, null, null, null);
            }
            
        } catch (Exception e) {
            log.error("❌ [API_VERIFY] 权限验证API调用异常", e);
            return createDeniedResult("权限验证服务异常: " + e.getMessage(), null, null, null, null);
        }
    }

    /**
     * 创建拒绝权限的结果
     */
    private AuthResult createDeniedResult(String message, String employeeId, String username, String roleCode, String roleName) {
        AuthResult result = new AuthResult();
        result.setPermissionGranted(false);
        result.setEmployeeId(employeeId);
        result.setUsername(username);
        result.setRoleCode(roleCode);
        result.setRoleName(roleName);
        result.setMessage(message);
        result.setRequiresApproval(false);
        result.setApproverRole(null);
        return result;
    }
}