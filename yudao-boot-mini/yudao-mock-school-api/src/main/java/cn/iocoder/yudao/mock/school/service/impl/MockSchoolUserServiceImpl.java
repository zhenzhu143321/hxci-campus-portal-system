package cn.iocoder.yudao.mock.school.service.impl;

import cn.iocoder.yudao.mock.school.dto.UserInfo;
import cn.iocoder.yudao.mock.school.entity.MockSchoolUser;
import cn.iocoder.yudao.mock.school.repository.MockSchoolUserRepository;
import cn.iocoder.yudao.mock.school.service.MockSchoolUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mock School User Service 实现类 - 精简版
 * 专注身份认证：Mock API认证 + JWT token生成
 * 权限验证职责已转移到主通知服务
 * 
 * @author Claude
 */
@Service
public class MockSchoolUserServiceImpl implements MockSchoolUserService {

    private static final Logger log = LoggerFactory.getLogger(MockSchoolUserServiceImpl.class);

    @Autowired
    private MockSchoolUserRepository userRepository;

    // 🚫 [REFACTORED] 权限Repository已移除 - 权限验证转移到主服务
    // @Autowired private MockRolePermissionRepository permissionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 🆕 生成简单的JWT Token（包含学号/工号信息）
     * 注意：这是Mock实现，生产环境应该使用标准的JWT库
     */
    @Override
    public String generateJwtToken(UserInfo userInfo) {
        log.info("🔐 [JWT_GENERATE] 为用户生成JWT Token: employeeId={}, username={}", 
                userInfo.getEmployeeId(), userInfo.getUsername());
        
        try {
            // 构建JWT payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userInfo.getUserId());
            payload.put("username", userInfo.getUsername());
            payload.put("employeeId", userInfo.getEmployeeId()); // 关键：学号/工号
            payload.put("realName", userInfo.getRealName());
            payload.put("roleCode", userInfo.getRoleCode());
            payload.put("roleName", userInfo.getRoleName());
            payload.put("userType", userInfo.getUserType());
            payload.put("departmentId", userInfo.getDepartmentId());
            payload.put("departmentName", userInfo.getDepartmentName());
            payload.put("iat", System.currentTimeMillis() / 1000); // 签发时间
            payload.put("exp", (System.currentTimeMillis() / 1000) + 24 * 60 * 60); // 24小时后过期
            
            // 简单Base64编码（Mock实现，生产环境应该用HMAC签名）
            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getEncoder().encodeToString(payloadJson.getBytes());
            
            // 构造简单的JWT格式: header.payload.signature
            String header = Base64.getEncoder().encodeToString("{\"typ\":\"JWT\",\"alg\":\"MOCK\"}".getBytes());
            String signature = Base64.getEncoder().encodeToString(("MOCK_SIGNATURE_" + userInfo.getEmployeeId()).getBytes());
            
            String jwtToken = header + "." + encodedPayload + "." + signature;
            
            log.info("✅ [JWT_GENERATE] JWT Token生成成功，长度: {}", jwtToken.length());
            return jwtToken;
            
        } catch (Exception e) {
            log.error("❌ [JWT_GENERATE] JWT Token生成失败", e);
            return null;
        }
    }

    /**
     * 🆕 解析JWT Token获取用户信息
     */
    @Override
    public UserInfo parseJwtToken(String jwtToken) {
        log.info("🔍 [JWT_PARSE] 解析JWT Token，长度: {}", jwtToken != null ? jwtToken.length() : 0);
        
        try {
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                log.warn("❌ [JWT_PARSE] Token为空");
                return null;
            }
            
            // 解析JWT格式: header.payload.signature
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                log.warn("❌ [JWT_PARSE] Token格式错误，部分数量: {}", parts.length);
                return null;
            }
            
            // 解码payload
            String payloadJson = new String(Base64.getDecoder().decode(parts[1]));
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            
            // 验证token是否过期
            Long exp = ((Number) payload.get("exp")).longValue();
            if (System.currentTimeMillis() / 1000 > exp) {
                log.warn("❌ [JWT_PARSE] Token已过期, exp: {}", exp);
                return null;
            }
            
            // 构建UserInfo对象
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId((String) payload.get("userId"));
            userInfo.setUsername((String) payload.get("username"));
            userInfo.setEmployeeId((String) payload.get("employeeId")); // 关键：学号/工号
            userInfo.setRealName((String) payload.get("realName"));
            userInfo.setRoleCode((String) payload.get("roleCode"));
            userInfo.setRoleName((String) payload.get("roleName"));
            userInfo.setUserType((String) payload.get("userType"));
            
            if (payload.get("departmentId") != null) {
                userInfo.setDepartmentId(((Number) payload.get("departmentId")).longValue());
            }
            userInfo.setDepartmentName((String) payload.get("departmentName"));
            userInfo.setEnabled(true);
            
            log.info("✅ [JWT_PARSE] Token解析成功: employeeId={}, username={}", 
                    userInfo.getEmployeeId(), userInfo.getUsername());
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("❌ [JWT_PARSE] Token解析失败", e);
            return null;
        }
    }

    @Override
    public UserInfo authenticateUser(String username, String password) {
        log.info("🔍 [USER_AUTH] 开始用户认证: username={}", username);
        
        if (username == null || username.trim().isEmpty()) {
            log.warn("用户名为空");
            return null;
        }
        
        if (password == null || password.trim().isEmpty()) {
            log.warn("密码为空");
            return null;
        }
        
        try {
            // 查找用户（通过用户名）
            Optional<MockSchoolUser> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                log.warn("用户不存在: {}", username);
                return null;
            }
            
            MockSchoolUser user = userOpt.get();
            
            // 检查用户状态
            if (!user.getEnabled()) {
                log.warn("用户已禁用: {}", username);
                return null;
            }
            
            // 简单密码验证（Mock环境，实际应该用BCrypt等）
            // 这里我们假设admin123是正确密码
            if (!"admin123".equals(password)) {
                log.warn("密码错误: username={}", username);
                return null;
            }
            
            // 检查token是否过期
            if (user.getTokenExpiresTime() != null && user.getTokenExpiresTime().isBefore(LocalDateTime.now())) {
                log.info("Token已过期，刷新Token: {}", username);
                // 刷新token
                String newToken = "YD_SCHOOL_" + user.getRoleCode() + "_" + System.currentTimeMillis();
                user.setToken(newToken);
                user.setTokenExpiresTime(LocalDateTime.now().plusHours(24));
                userRepository.save(user);
            }
            
            log.info("✅ [USER_AUTH] 用户认证成功: username={}, role={}", username, user.getRoleName());
            
            // 转换为UserInfo DTO
            return convertToUserInfo(user);
            
        } catch (Exception e) {
            log.error("用户认证异常", e);
            return null;
        }
    }

    @Override
    public UserInfo authenticateUserByEmployeeId(String employeeId, String name, String password) {
        log.info("🔍 [EMPLOYEE_AUTH] 开始工号+姓名认证: employeeId={}, name={}", employeeId, name);
        
        if (employeeId == null || employeeId.trim().isEmpty()) {
            log.warn("工号为空");
            return null;
        }
        
        if (name == null || name.trim().isEmpty()) {
            log.warn("姓名为空");
            return null;
        }
        
        if (password == null || password.trim().isEmpty()) {
            log.warn("密码为空");
            return null;
        }
        
        try {
            // 查找用户（通过工号和姓名）
            Optional<MockSchoolUser> userOpt = userRepository.findByUserIdAndUsername(employeeId, name);
            
            if (userOpt.isEmpty()) {
                log.warn("用户不存在: employeeId={}, name={}", employeeId, name);
                return null;
            }
            
            MockSchoolUser user = userOpt.get();
            
            // 检查用户状态
            if (!user.getEnabled()) {
                log.warn("用户已禁用: employeeId={}, name={}", employeeId, name);
                return null;
            }
            
            // 简单密码验证（Mock环境，实际应该用BCrypt等）
            if (!"admin123".equals(password)) {
                log.warn("密码错误: employeeId={}, name={}", employeeId, name);
                return null;
            }
            
            // 检查token是否过期，如需要则刷新
            if (user.getTokenExpiresTime() != null && user.getTokenExpiresTime().isBefore(LocalDateTime.now())) {
                log.info("Token已过期，刷新Token: employeeId={}", employeeId);
                // 刷新token
                String newToken = "YD_SCHOOL_" + user.getRoleCode() + "_" + employeeId + "_" + System.currentTimeMillis();
                user.setToken(newToken);
                user.setTokenExpiresTime(LocalDateTime.now().plusHours(24));
                userRepository.save(user);
            }
            
            log.info("✅ [EMPLOYEE_AUTH] 工号+姓名认证成功: employeeId={}, name={}, role={}", 
                    employeeId, name, user.getRoleName());
            
            // 转换为UserInfo DTO
            return convertToUserInfo(user);
            
        } catch (Exception e) {
            log.error("工号+姓名认证异常", e);
            return null;
        }
    }

    @Override
    public UserInfo verifyToken(String token) {
        log.info("🔍 [TOKEN_VERIFY] 开始验证token: {}", token);
        System.out.println("[DEBUG] verifyToken调用 - token: " + token);
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("❌ [TOKEN_VERIFY] Token为空或null");
            System.out.println("[DEBUG] Token验证失败 - Token为空");
            return null;
        }
        
        try {
            // 🆕 优先尝试JWT Token解析（新方式）
            if (token.contains(".") && token.split("\\.").length == 3) {
                log.info("🔐 [TOKEN_VERIFY] 检测到JWT格式Token，尝试解析...");
                System.out.println("[DEBUG] JWT格式Token检测到，开始解析");
                
                UserInfo jwtUserInfo = parseJwtToken(token);
                if (jwtUserInfo != null) {
                    log.info("✅ [TOKEN_VERIFY] JWT Token验证成功: employeeId={}", jwtUserInfo.getEmployeeId());
                    System.out.println("[DEBUG] JWT Token验证成功: " + jwtUserInfo.getEmployeeId());
                    return jwtUserInfo;
                } else {
                    log.warn("⚠️ [TOKEN_VERIFY] JWT Token解析失败，尝试数据库验证...");
                    System.out.println("[DEBUG] JWT解析失败，尝试数据库验证");
                }
            }
            
            // 🔄 Fallback: 数据库Token验证（兼容旧方式）
            log.info("📊 [TOKEN_VERIFY] 使用数据库Token验证...");
            System.out.println("[DEBUG] 开始数据库查询...");
            
            // 先检查数据库连接状态
            long totalUsers = userRepository.count();
            log.info("📊 [TOKEN_VERIFY] 数据库中总用户数: {}", totalUsers);
            System.out.println("[DEBUG] 数据库连接正常，总用户数: " + totalUsers);
            
            // 查找用户
            Optional<MockSchoolUser> userOpt = userRepository.findValidTokenUser(token);
            log.info("📊 [TOKEN_VERIFY] 数据库查询结果 - 用户存在: {}", userOpt.isPresent());
            System.out.println("[DEBUG] 查询结果: " + (userOpt.isPresent() ? "找到用户" : "未找到用户"));
            
            if (!userOpt.isPresent()) {
                // 尝试直接查询所有token进行对比
                log.info("🔍 [TOKEN_VERIFY] 尝试查询所有用户token进行对比...");
                List<MockSchoolUser> allUsers = userRepository.findAll();
                log.info("📊 [TOKEN_VERIFY] 所有用户列表大小: {}", allUsers.size());
                System.out.println("[DEBUG] 所有用户数量: " + allUsers.size());
                
                for (MockSchoolUser user : allUsers) {
                    log.info("👤 [TOKEN_VERIFY] 用户: {}, token: {}, 过期时间: {}", 
                        user.getUsername(), user.getToken(), user.getTokenExpiresTime());
                    System.out.println(String.format("[DEBUG] 用户: %s, token: %s, 启用: %s", 
                        user.getUsername(), user.getToken(), user.getEnabled()));
                        
                    if (token.equals(user.getToken())) {
                        log.info("✅ [TOKEN_VERIFY] 找到匹配token的用户: {}", user.getUsername());
                        System.out.println("[DEBUG] 找到匹配用户: " + user.getUsername());
                        
                        // 检查token是否过期
                        if (user.getTokenExpiresTime().isAfter(LocalDateTime.now())) {
                            log.info("✅ [TOKEN_VERIFY] Token未过期，验证成功");
                            System.out.println("[DEBUG] Token验证成功 - 未过期");
                            userOpt = Optional.of(user);
                            break;
                        } else {
                            log.warn("⏰ [TOKEN_VERIFY] Token已过期: {}", user.getTokenExpiresTime());
                            System.out.println("[DEBUG] Token已过期: " + user.getTokenExpiresTime());
                        }
                    }
                }
                
                if (!userOpt.isPresent()) {
                    log.warn("❌ [TOKEN_VERIFY] 无效token或token已过期: {}", token);
                    System.out.println("[DEBUG] Token验证失败 - 无效或过期");
                    return null;
                }
            }

            MockSchoolUser user = userOpt.get();
            log.info("✅ [TOKEN_VERIFY] Token验证成功，用户: {}, 角色: {}, 部门: {}", 
                user.getUsername(), user.getRoleName(), user.getDepartmentName());
            System.out.println(String.format("[DEBUG] Token验证成功 - 用户: %s, 角色: %s", 
                user.getUsername(), user.getRoleName()));
            
            UserInfo userInfo = convertToUserInfo(user);
            log.info("🎯 [TOKEN_VERIFY] 返回用户信息: userId={}, roleCode={}", 
                userInfo.getUserId(), userInfo.getRoleCode());
            System.out.println("[DEBUG] 返回用户信息成功");
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("💥 [TOKEN_VERIFY] Token验证异常", e);
            System.out.println("[DEBUG] Token验证异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        log.info("获取用户信息: {}", userId);
        
        Optional<MockSchoolUser> userOpt = userRepository.findByUserId(userId);
        
        if (userOpt.isPresent()) {
            return convertToUserInfo(userOpt.get());
        }
        
        log.warn("未找到用户: {}", userId);
        return null;
    }

    @Override
    public List<UserInfo> getUsersByRole(String roleCode) {
        log.info("根据角色获取用户列表: {}", roleCode);
        
        List<MockSchoolUser> users = userRepository.findByRoleCodeAndEnabled(roleCode, true);
        return users.stream()
                .map(this::convertToUserInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserInfo> getUsersByDepartment(Long departmentId) {
        log.info("根据部门获取用户列表: {}", departmentId);
        
        List<MockSchoolUser> users = userRepository.findByDepartmentId(departmentId);
        return users.stream()
                .filter(MockSchoolUser::getEnabled)
                .map(this::convertToUserInfo)
                .collect(Collectors.toList());
    }

    // 🚫 [REFACTORED] 权限验证方法已移除 - 职责转移到主通知服务
    // 原权限验证、权限查询等方法已删除，Mock API专注于身份认证

    @Override
    public MockSchoolUser createTestUser(String username, String userId, String roleCode, String roleName,
                                        Long departmentId, String departmentName) {
        log.info("创建测试用户: {}", username);
        
        MockSchoolUser user = new MockSchoolUser();
        user.setUsername(username);
        user.setUserId(userId);
        user.setRoleCode(roleCode);
        user.setRoleName(roleName);
        user.setDepartmentId(departmentId);
        user.setDepartmentName(departmentName);
        user.setToken("mock_token_" + userId);
        user.setTokenExpiresTime(LocalDateTime.now().plusHours(24));
        user.setEnabled(true);
        
        // 由于permissions字段暂时不存在，跳过权限设置
        // List<String> permissions = getDefaultPermissionsByRole(roleCode);
        // try {
        //     user.setPermissions(objectMapper.writeValueAsString(permissions));
        // } catch (Exception e) {
        //     log.error("设置用户权限JSON失败", e);
        // }
        
        return userRepository.save(user);
    }

    @Override
    public String refreshUserToken(String userId) {
        log.info("刷新用户token: {}", userId);
        
        Optional<MockSchoolUser> userOpt = userRepository.findByUserId(userId);
        if (!userOpt.isPresent()) {
            return null;
        }

        MockSchoolUser user = userOpt.get();
        String newToken = "mock_token_" + userId + "_" + System.currentTimeMillis();
        user.setToken(newToken);
        user.setTokenExpiresTime(LocalDateTime.now().plusHours(24));
        
        userRepository.save(user);
        return newToken;
    }

    @Override
    public List<String> getAllRoles() {
        // 🚫 [REFACTORED] 硬编码角色列表，不再依赖权限Repository
        return List.of("PRINCIPAL", "ACADEMIC_ADMIN", "TEACHER", "CLASS_TEACHER", "STUDENT");
    }

    @Override
    public long countUsersByRole(String roleCode) {
        return userRepository.countByRoleCode(roleCode);
    }

    /**
     * 转换为UserInfo并生成JWT Token - 精简版
     * 专注身份认证，权限验证已转移到主通知服务
     */
    private UserInfo convertToUserInfo(MockSchoolUser user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        
        // 🆕 设置学号/工号（关键字段）
        userInfo.setEmployeeId(user.getUserId()); // 使用userId作为学号/工号
        userInfo.setRealName(user.getUsername()); // 使用username作为真实姓名（Demo环境）
        
        userInfo.setRoleCode(user.getRoleCode());
        userInfo.setRoleName(user.getRoleName());
        userInfo.setDepartmentId(user.getDepartmentId());
        userInfo.setDepartmentName(user.getDepartmentName());
        userInfo.setEnabled(user.getEnabled());
        
        // 🆕 设置用户类型
        userInfo.setUserType(determineUserType(user.getRoleCode()));
        
        // 🚫 [REFACTORED] 权限列表已移除 - 由主服务负责权限查询
        // userInfo.setPermissions(getUserPermissions(user.getUserId()));
        
        // 🆕 生成JWT Token（包含学号/工号）
        String jwtToken = generateJwtToken(userInfo);
        userInfo.setAccessToken(jwtToken);
        userInfo.setTokenExpireTime(LocalDateTime.now().plusHours(24));
        
        log.info("🎯 [CONVERT_USER] UserInfo转换完成: employeeId={}, token长度={}", 
                userInfo.getEmployeeId(), jwtToken != null ? jwtToken.length() : 0);
        
        return userInfo;
    }

    /**
     * 🆕 根据角色确定用户类型
     */
    private String determineUserType(String roleCode) {
        switch (roleCode) {
            case "STUDENT":
                return "STUDENT";
            case "TEACHER":
            case "CLASS_TEACHER":
                return "TEACHER";
            case "PRINCIPAL":
            case "ACADEMIC_ADMIN":
                return "ADMIN";
            default:
                return "OTHER";
        }
    }

    // 🚫 [REFACTORED] 权限相关辅助方法已移除 - 职责转移到主通知服务
    // 原权限确定、权限获取等辅助方法已删除，Mock API专注于身份认证
}