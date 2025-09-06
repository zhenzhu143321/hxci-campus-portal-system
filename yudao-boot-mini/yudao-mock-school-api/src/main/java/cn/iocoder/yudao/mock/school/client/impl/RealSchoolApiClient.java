package cn.iocoder.yudao.mock.school.client.impl;

import cn.iocoder.yudao.mock.school.client.SchoolApiClient;
import cn.iocoder.yudao.mock.school.model.SchoolUserInfo;
import cn.iocoder.yudao.mock.school.exception.SchoolApiException;
import cn.iocoder.yudao.mock.school.config.SchoolApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Real模式的学校API客户端实现
 * 调用真实学校API获取用户信息
 * API地址：https://work.greathiit.com/api/user/loginWai
 * 
 * @author Claude
 * @since 2025-09-04
 */
@Component("realSchoolApiClient")
public class RealSchoolApiClient implements SchoolApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(RealSchoolApiClient.class);
    private static final String MODE = "REAL";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SchoolApiProperties properties;
    
    @Autowired
    public RealSchoolApiClient(RestTemplateBuilder restTemplateBuilder, 
                              SchoolApiProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        
        // 配置RestTemplate with timeout
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getReal().getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReal().getReadTimeoutMs()))
                .build();
        
        log.info("🌐 [REAL_MODE] Real School API Client initialized: baseUrl={}", 
                properties.getReal().getBaseUrl());
    }
    
    @Override
    public SchoolUserInfo login(String username, String password) throws SchoolApiException {
        log.info("🌐 [REAL_MODE] 开始Real模式学校API调用: username={}", username);
        
        try {
            // 构建请求URL
            String url = properties.getReal().getBaseUrl() + properties.getReal().getPath();
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put(properties.getReal().getUsernameField(), username);
            requestBody.put(properties.getReal().getPasswordField(), password);
            requestBody.put("autoLogin", true);
            requestBody.put("provider", "account");
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("User-Agent", "HXCI-Campus-Portal/1.0");
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.debug("🔗 [REAL_MODE] 发送请求到学校API: url={}, body={}", url, requestBody);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            log.info("📡 [REAL_MODE] 学校API响应: status={}, body={}", 
                    response.getStatusCode(), response.getBody());
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseSchoolApiResponse(response.getBody(), username);
            } else {
                throw new SchoolApiException("学校API响应异常: " + response.getStatusCode(), 
                                           "API_RESPONSE_ERROR", MODE);
            }
            
        } catch (HttpClientErrorException e) {
            log.warn("🚨 [REAL_MODE] 学校API客户端错误: status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new SchoolApiException("用户名或密码错误", e, "AUTH_FAILED", MODE);
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new SchoolApiException("用户不存在", e, "USER_NOT_FOUND", MODE);
            } else {
                throw new SchoolApiException("学校API客户端错误: " + e.getStatusCode(), 
                                           e, "CLIENT_ERROR", MODE);
            }
            
        } catch (HttpServerErrorException e) {
            log.error("💥 [REAL_MODE] 学校API服务器错误: status={}, body={}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            throw new SchoolApiException("学校API服务器错误: " + e.getStatusCode(), 
                                       e, "SERVER_ERROR", MODE);
            
        } catch (RestClientException e) {
            log.error("🌐 [REAL_MODE] 学校API网络异常", e);
            throw new SchoolApiException("学校API网络异常: " + e.getMessage(), 
                                       e, "NETWORK_ERROR", MODE);
            
        } catch (Exception e) {
            log.error("💥 [REAL_MODE] 学校API调用异常", e);
            throw new SchoolApiException("学校API调用异常: " + e.getMessage(), 
                                       e, "SYSTEM_ERROR", MODE);
        }
    }
    
    /**
     * 解析学校API响应数据
     */
    private SchoolUserInfo parseSchoolApiResponse(String responseBody, String username) 
            throws SchoolApiException {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // 检查响应状态
            if (rootNode.has("error") || (rootNode.has("code") && rootNode.get("code").asInt() != 0)) {
                String errorMsg = rootNode.has("message") ? rootNode.get("message").asText() : "未知错误";
                throw new SchoolApiException("学校API返回错误: " + errorMsg, "API_ERROR", MODE);
            }
            
            // 提取用户数据（假设在data字段中）
            JsonNode dataNode = rootNode.has("data") ? rootNode.get("data") : rootNode;
            
            SchoolUserInfo userInfo = new SchoolUserInfo();
            userInfo.setStudentNo(username); // 使用登录用户名作为学号
            userInfo.setName(extractString(dataNode, "name", "姓名"));
            userInfo.setCollege(extractString(dataNode, "college", "学院"));
            userInfo.setMajor(extractString(dataNode, "major", "专业"));
            userInfo.setClassName(extractString(dataNode, "className", "班级"));
            userInfo.setGrade(extractString(dataNode, "grade", "年级"));
            userInfo.setPhone(extractString(dataNode, "phone", ""));
            userInfo.setGender(extractString(dataNode, "gender", ""));
            
            // 处理角色数组
            if (dataNode.has("role") && dataNode.get("role").isArray()) {
                String[] roles = objectMapper.convertValue(dataNode.get("role"), String[].class);
                userInfo.setRoles(roles);
            } else {
                userInfo.setRoles(new String[]{"学生"}); // 默认角色
            }
            
            // 判断用户类型
            String userType = username.startsWith("202") ? "student" : "teacher";
            userInfo.setUserType(userType);
            
            // 生成Basic Token（Real模式下由学校API返回，这里模拟）
            String basicToken = extractString(dataNode, "token", UUID.randomUUID().toString());
            userInfo.setBasicToken(basicToken);
            
            // 保存原始数据
            Map<String, Object> rawData = objectMapper.convertValue(dataNode, Map.class);
            rawData.put("mode", MODE);
            rawData.put("apiUrl", properties.getReal().getBaseUrl() + properties.getReal().getPath());
            rawData.put("loginTime", System.currentTimeMillis());
            userInfo.setRawData(rawData);
            
            log.info("✅ [REAL_MODE] 学校API登录成功: user={}, type={}", 
                    userInfo.getName(), userInfo.getUserType());
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("💥 [REAL_MODE] 解析学校API响应失败", e);
            throw new SchoolApiException("解析学校API响应失败: " + e.getMessage(), 
                                       e, "PARSE_ERROR", MODE);
        }
    }
    
    /**
     * 安全提取字符串字段
     */
    private String extractString(JsonNode node, String fieldName, String defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return defaultValue;
    }
    
    @Override
    public String getMode() {
        return MODE;
    }
}