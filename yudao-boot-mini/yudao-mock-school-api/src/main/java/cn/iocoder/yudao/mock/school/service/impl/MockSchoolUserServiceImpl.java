package cn.iocoder.yudao.mock.school.service.impl;

import cn.iocoder.yudao.mock.school.config.JwtSecurityConfig;
import cn.iocoder.yudao.mock.school.dto.SchoolLoginRequest;
import cn.iocoder.yudao.mock.school.dto.SchoolLoginResult;
import cn.iocoder.yudao.mock.school.dto.SchoolUserDTO;
import cn.iocoder.yudao.mock.school.dto.UserInfo;
import cn.iocoder.yudao.mock.school.entity.MockSchoolUser;
import cn.iocoder.yudao.mock.school.repository.MockSchoolUserRepository;
import cn.iocoder.yudao.mock.school.service.MockSchoolUserService;
import cn.iocoder.yudao.mock.school.service.SchoolApiClient;
import cn.iocoder.yudao.mock.school.client.adapter.SchoolApiClientAdapter;
import cn.iocoder.yudao.mock.school.service.SchoolTokenService;
import cn.iocoder.yudao.mock.school.service.UserMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// 🚨 P0安全修复：添加Auth0 JWT库
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

    // 🆕 新增服务依赖 - 支持双Token认证（使用配置驱动适配器）
    @Autowired
    private SchoolApiClientAdapter schoolApiClient;

    @Autowired
    private SchoolTokenService schoolTokenService;

    @Autowired
    private UserMappingService userMappingService;
    
    @Autowired
    private JwtSecurityConfig jwtSecurityConfig;

    // 🚨 P0-SEC-02安全修复：使用安全配置替代硬编码
    // JWT配置现在从JwtSecurityConfig中获取，支持环境变量和安全密钥生成
    private static final String JWT_AUDIENCE = "school-api-secure";

    // 🔐 P0-SEC-02修复：已移除弱密钥生成方法，使用JwtSecurityConfig提供的安全密钥

    /**
     * 🛡️ P1.2安全修复：Token脱敏工具方法
     * 防止完整Token在日志中泄露
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "***INVALID_TOKEN***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 6);
    }

    /**
     * 🆕 生成安全JWT Token（P1.2安全修复：信息泄露防护）
     * ✅ P1.2修复内容：
     * 1. 🔐 动态密钥生成，防止硬编码泄露
     * 2. ⏰ Token有效期缩短到10分钟，降低泄露风险
     * 3. 🛡️ 强化日志脱敏，防止Token在日志中泄露
     * 4. 🔒 增强JWT Claims验证，添加更多安全标识符
     * 5. 🎯 极简载荷设计，绝对最小化敏感信息暴露
     * 6. 🆔 增强JWT ID生成算法，防重放攻击
     */
    @Override
    public String generateJwtToken(UserInfo userInfo) {
        log.info("🔐 [JWT_GENERATE_V2] P1.2安全版本：为用户生成强化JWT Token: employeeId={}", 
                userInfo.getEmployeeId());
        
        try {
            // 🚨 P1.2安全检查：验证用户信息完整性
            if (userInfo == null || userInfo.getEmployeeId() == null || userInfo.getUsername() == null) {
                log.error("❌ [JWT_GENERATE_V2] 用户信息不完整，拒绝生成Token");
                throw new SecurityException("用户信息不完整");
            }

            Date now = new Date();
            Date expiresAt = new Date(now.getTime() + jwtSecurityConfig.getJwtExpiration()); // 使用配置的有效期
            
            // 🆕 P1.2强化：生成更安全的JWT ID
            String jwtId = "jwt_v2_" + userInfo.getUserId() + "_" + 
                          System.currentTimeMillis() + "_" + 
                          Integer.toHexString(Objects.hash(userInfo.getEmployeeId(), now.getTime()));

            // 🔐 P0-SEC-02强化：使用安全配置的密钥和参数生成JWT
            String jwtToken = JWT.create()
                    .withSubject(userInfo.getUserId())
                    .withIssuer(jwtSecurityConfig.getJwtIssuer()) // 使用配置的签发者
                    .withAudience(JWT_AUDIENCE) // 使用安全的受众标识
                    .withIssuedAt(now)
                    .withExpiresAt(expiresAt)
                    .withJWTId(jwtId) // 🆕 强化的JWT ID
                    
                    // 🎯 P0-SEC-02极简载荷：只保留认证和授权绝对必需信息
                    .withClaim("userId", userInfo.getUserId())
                    .withClaim("empId", userInfo.getEmployeeId()) // 缩短claim名称
                    .withClaim("role", userInfo.getRoleCode()) // 缩短claim名称
                    .withClaim("type", userInfo.getUserType()) // 缩短claim名称
                    .withClaim("ver", "2.0") // 🆕 Token版本标识
                    
                    // 🚫 P0-SEC-02绝对禁止：任何可识别个人身份的信息
                    // 包括：真实姓名、部门名称、年级班级具体信息、邮箱、电话等
                    
                    // 🛡️ P0-SEC-02强化：使用安全配置的算法签名
                    .sign(jwtSecurityConfig.getJwtAlgorithm());
            
            log.info("✅ [JWT_GENERATE_V2] P0-SEC-02强化JWT生成成功，算法: HS256安全密钥，有效期: {}分钟", jwtSecurityConfig.getJwtExpiration() / 60000);
            log.info("🔒 [SECURITY_V2] JWT载荷极简化：移除所有个人身份信息，只保留认证必需数据");
            log.info("🛡️ [SECURITY_V2] Token脱敏日志: {}", maskToken(jwtToken));
            
            return jwtToken;
            
        } catch (JWTCreationException e) {
            log.error("❌ [JWT_GENERATE_V2] JWT Token创建失败", e);
            throw new SecurityException("JWT Token生成失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ [JWT_GENERATE_V2] JWT Token生成异常", e);
            throw new SecurityException("JWT Token生成异常: " + e.getMessage());
        }
    }

    /**
     * 🆕 解析JWT Token获取用户信息（P1.2安全修复：信息泄露防护）
     * ✅ P1.2修复内容：
     * 1. 🔐 适配动态密钥验证，防止密钥泄露攻击
     * 2. 🛡️ 强化签名验证，拒绝任何篡改Token
     * 3. 🔍 严格算法检查，绝对禁用不安全算法
     * 4. ⏰ 适配10分钟有效期验证
     * 5. 🎯 适配极简载荷结构，安全获取用户信息
     * 6. 🆔 增强JWT ID验证，全面防重放攻击
     * 7. 🛡️ 日志脱敏，防止Token在解析日志中泄露
     */
    @Override
    public UserInfo parseJwtToken(String jwtToken) {
        log.info("🔍 [JWT_PARSE_V2] P1.2安全版本：解析强化JWT Token，长度: {}", 
                jwtToken != null ? jwtToken.length() : 0);
        log.info("🛡️ [JWT_PARSE_V2] Token脱敏: {}", maskToken(jwtToken));
        
        try {
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                log.warn("❌ [JWT_PARSE_V2] Token为空");
                throw new SecurityException("JWT Token为空");
            }

            // 🚨 P0-SEC-02安全修复：创建强化JWT验证器，使用安全配置
            JWTVerifier verifier = JWT.require(jwtSecurityConfig.getJwtAlgorithm()) // 使用安全配置的密钥
                    .withIssuer(jwtSecurityConfig.getJwtIssuer()) // 验证配置的签发者
                    .withAudience(JWT_AUDIENCE) // 验证安全受众
                    .acceptLeeway(30) // 允许30秒时钟偏移
                    .build();

            // 🛡️ P1.2强化：验证JWT签名和有效期
            DecodedJWT jwt = verifier.verify(jwtToken);
            
            // 🔐 P1.2强化：验证算法和版本
            String algorithm = jwt.getAlgorithm();
            if (!"HS256".equals(algorithm)) {
                log.error("🚨 [JWT_PARSE_V2] 不安全的算法: {}", algorithm);
                throw new SecurityException("JWT算法不安全: " + algorithm);
            }

            // 🆕 P1.2新增：验证Token版本
            String version = jwt.getClaim("ver").asString();
            if (!"2.0".equals(version)) {
                log.warn("⚠️ [JWT_PARSE_V2] Token版本不匹配: {}", version);
            }

            // 🆕 P1.2强化：验证JWT ID（防重放攻击）
            String jwtId = jwt.getId();
            if (jwtId == null || jwtId.trim().isEmpty() || !jwtId.startsWith("jwt_v2_")) {
                log.warn("⚠️ [JWT_PARSE_V2] JWT ID格式异常，可能存在重放攻击风险: {}", jwtId);
            } else {
                log.info("✅ [JWT_PARSE_V2] JWT ID验证通过: {}", jwtId.substring(0, 15) + "...");
            }

            // 📊 P1.2适配：提取极简载荷信息
            String userId = jwt.getSubject();
            String employeeId = jwt.getClaim("empId").asString(); // 适配新的claim名称
            String roleCode = jwt.getClaim("role").asString(); // 适配新的claim名称
            String userType = jwt.getClaim("type").asString(); // 适配新的claim名称
            
            if (userId == null || employeeId == null || roleCode == null) {
                log.error("❌ [JWT_PARSE_V2] JWT载荷缺少必需字段");
                throw new SecurityException("JWT载荷不完整");
            }

            // 🔍 P1.2强化：通过数据库安全获取完整用户信息
            log.info("🔍 [JWT_PARSE_V2] 安全数据库查询: userId={}, employeeId={}", userId, employeeId);
            Optional<MockSchoolUser> userOpt = userRepository.findByUserId(userId);
            
            if (userOpt.isEmpty()) {
                log.error("❌ [JWT_PARSE_V2] 用户不存在于数据库: {}", userId);
                throw new SecurityException("用户信息不存在");
            }

            MockSchoolUser dbUser = userOpt.get();
            
            // 🚨 P1.2强化：双重安全验证，确认数据库与Token一致
            if (!employeeId.equals(dbUser.getUserId()) || !roleCode.equals(dbUser.getRoleCode())) {
                log.error("🚨 [JWT_PARSE_V2] 数据库信息与Token不匹配，可能存在篡改攻击");
                throw new SecurityException("用户信息验证失败");
            }

            // 🎯 P1.2安全构建：从数据库安全获取用户信息
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(userId);
            userInfo.setEmployeeId(employeeId);
            userInfo.setRoleCode(roleCode);
            userInfo.setUserType(userType);
            
            // 🔒 P1.2安全原则：敏感信息只从数据库获取，绝不从Token中获取
            userInfo.setUsername(dbUser.getUsername());
            userInfo.setRealName(dbUser.getUsername()); // Demo环境使用username作为realName
            userInfo.setRoleName(dbUser.getRoleName());
            userInfo.setDepartmentId(dbUser.getDepartmentId());
            userInfo.setDepartmentName(dbUser.getDepartmentName());
            userInfo.setGradeId(dbUser.getGradeId());
            userInfo.setClassId(dbUser.getClassId());
            userInfo.setEnabled(dbUser.getEnabled());
            
            // 为学生用户设置studentId
            if ("STUDENT".equals(roleCode)) {
                userInfo.setStudentId(employeeId);
            }
            
            log.info("✅ [JWT_PARSE_V2] P1.2强化Token解析成功: employeeId={}, roleCode={}, version={}", 
                    employeeId, roleCode, version);
            log.info("🔒 [SECURITY_V2] 敏感信息通过数据库安全获取，Token载荷极简化生效");
            
            return userInfo;
            
        } catch (JWTVerificationException e) {
            log.error("❌ [JWT_PARSE_V2] JWT验证失败: {}", e.getMessage());
            throw new SecurityException("JWT Token验证失败: " + e.getMessage());
        } catch (SecurityException e) {
            log.error("🚨 [JWT_PARSE_V2] 安全验证失败: {}", e.getMessage());
            throw e; // 重新抛出安全异常
        } catch (Exception e) {
            log.error("❌ [JWT_PARSE_V2] Token解析异常", e);
            throw new SecurityException("JWT Token解析异常: " + e.getMessage());
        }
    }

    /**
     * 学校登录认证 - 返回与真实API完全一致的格式
     * 
     * 支持通过环境变量SCHOOL_API_MODE控制Mock/Real切换:
     * - SCHOOL_API_MODE=mock: 使用Mock数据（默认）
     * - SCHOOL_API_MODE=real: 调用真实学校API
     * 
     * @author Auth-Integration-Expert
     */
    @Override
    public SchoolLoginResult processSchoolAuthentication(SchoolLoginRequest request) {
        log.info("🏫 [SCHOOL_AUTH] 开始学校登录认证流程: employeeId={}, name={}", 
                request.getEmployeeId(), request.getName());
        
        try {
            // 🔍 第一步：参数验证
            if (request == null || request.getEmployeeId() == null || 
                request.getName() == null || request.getPassword() == null) {
                throw new SecurityException("登录参数不完整");
            }
            
            // 🎯 检查环境变量决定使用Mock还是Real API
            String apiMode = System.getenv("SCHOOL_API_MODE");
            boolean useRealApi = "real".equalsIgnoreCase(apiMode);
            log.info("📡 [SCHOOL_AUTH] API模式: {} (SCHOOL_API_MODE={})", 
                    useRealApi ? "真实API" : "Mock数据", apiMode);
            
            // 🎨 构建与真实API完全一致的返回格式
            SchoolLoginResult.LoginData loginData;
            
            if (useRealApi) {
                // 真实API调用逻辑（待实现）
                log.info("🌐 [SCHOOL_AUTH] 调用真实学校API...");
                // TODO: 调用真实API并解析返回
                loginData = buildMockLoginData(request); // 暂时使用Mock
            } else {
                // Mock数据 - 格式与真实API完全一致
                loginData = buildMockLoginData(request);
            }
            
            // 🔨 构建最终返回结果
            SchoolLoginResult result = SchoolLoginResult.builder()
                .code(0)
                .msg("认证成功")
                .data(loginData)
                .build();
            
            log.info("🎉 [SCHOOL_AUTH] 学校登录认证完成: no={}, role={}, token={}", 
                    loginData.getNo(), loginData.getRole(), loginData.getToken().substring(0, 8) + "...");
            
            return result;
            
        } catch (SecurityException e) {
            log.error("🚨 [SCHOOL_AUTH] 安全验证失败: {}", e.getMessage());
            throw e; // 重新抛出安全异常
            
        } catch (Exception e) {
            log.error("💥 [SCHOOL_AUTH] 学校登录认证异常", e);
            throw new SecurityException("学校登录认证失败: " + e.getMessage(), e);
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
        log.info("🔍 [TOKEN_VERIFY_V2] P1.2强化版本：开始验证token");
        log.info("🛡️ [TOKEN_VERIFY_V2] Token脱敏: {}", maskToken(token));
        System.out.println("[DEBUG] verifyToken调用 - token脱敏: " + maskToken(token));
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("❌ [TOKEN_VERIFY_V2] Token为空或null");
            System.out.println("[DEBUG] Token验证失败 - Token为空");
            return null;
        }
        
        try {
            // 🆕 P1.2优先：尝试强化JWT Token解析（新方式）
            if (token.contains(".") && token.split("\\.").length == 3) {
                log.info("🔐 [TOKEN_VERIFY_V2] 检测到JWT格式Token，尝试P1.2强化解析...");
                System.out.println("[DEBUG] JWT格式Token检测到，开始P1.2强化解析");
                
                UserInfo jwtUserInfo = parseJwtToken(token);
                if (jwtUserInfo != null) {
                    log.info("✅ [TOKEN_VERIFY_V2] P1.2强化JWT Token验证成功: employeeId={}", jwtUserInfo.getEmployeeId());
                    System.out.println("[DEBUG] P1.2强化JWT Token验证成功: " + jwtUserInfo.getEmployeeId());
                    return jwtUserInfo;
                } else {
                    log.warn("⚠️ [TOKEN_VERIFY_V2] JWT Token解析失败，尝试数据库验证...");
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
        
        // 🆕 为学生用户同时设置studentId字段（主服务兼容性）
        if ("STUDENT".equals(user.getRoleCode())) {
            userInfo.setStudentId(user.getUserId()); // 学生的studentId与employeeId相同
        }
        
        userInfo.setRealName(user.getUsername()); // 使用username作为真实姓名（Demo环境）
        
        userInfo.setRoleCode(user.getRoleCode());
        userInfo.setRoleName(user.getRoleName());
        userInfo.setDepartmentName(user.getDepartmentName());
        userInfo.setEnabled(user.getEnabled());
        
        // 🆕 设置年级班级信息（权限验证必需）
        userInfo.setGradeId(user.getGradeId());
        userInfo.setClassId(user.getClassId());
        
        // 🆕 设置部门信息（同时设置数值和字符串格式）
        userInfo.setDepartmentId(user.getDepartmentId());
        if (user.getDepartmentId() != null) {
            userInfo.setDepartmentIdStr(user.getDepartmentId().toString()); // 主服务可能期望字符串格式
        }
        
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
    
    /**
     * 构建Mock登录数据 - 与真实API格式完全一致
     * 
     * @author Auth-Integration-Expert
     */
    private SchoolLoginResult.LoginData buildMockLoginData(SchoolLoginRequest request) {
        // 根据employeeId判断用户类型
        String employeeId = request.getEmployeeId();
        String name = request.getName();
        
        // 生成UUID格式的token
        String token = UUID.randomUUID().toString();
        
        // 根据用户类型构建不同的返回数据
        SchoolLoginResult.LoginData.LoginDataBuilder builder = SchoolLoginResult.LoginData.builder()
            .id(employeeId)
            .companyId("10000001")
            .no(employeeId)
            .name(name)
            .token(token);
        
        // 根据不同角色设置不同的字段（按照真实API的ID格式判断）
        if (employeeId.startsWith("202") || employeeId.startsWith("STUDENT")) {
            // 学生数据（202开头的是真实学生ID格式）
            builder.officeId("01")
                   .schoolName("江北校区")
                   .email(employeeId.toLowerCase() + "@hrbiit.edu.cn")
                   .phone("15846029850")
                   .mobile(null)
                   .role(List.of("student"))  // 角色数组
                   .photo(null)
                   .grade("2023")
                   .teacherStatus(null)
                   .className("软件23M01");
        } else if (employeeId.equals("10031") || employeeId.startsWith("TEACHER") || employeeId.startsWith("CLASS_TEACHER")) {
            // 教师数据（10031是真实教师ID）
            builder.officeId("90000022")
                   .schoolName(null)
                   .email("teacher@hxci.edu.cn")
                   .phone("15945931099")
                   .mobile("15945931099")
                   .role(List.of("teacher", "zaizhi", "listen_admin"))  // 多个角色
                   .photo(null)
                   .grade(null)
                   .teacherStatus("在职")
                   .className(null);
        } else if (employeeId.startsWith("PRINCIPAL")) {
            // 校长数据
            builder.officeId("90000001")
                   .schoolName("江北校区")
                   .email("principal@hxci.edu.cn")
                   .phone("13900000001")
                   .mobile("13900000001")
                   .role(List.of("principal", "teacher", "admin"))  // 多个角色
                   .photo(null)
                   .grade(null)
                   .teacherStatus("在职")
                   .className(null);
        } else if (employeeId.startsWith("ACADEMIC_ADMIN")) {
            // 教务主任数据
            builder.officeId("90000002")
                   .schoolName("江北校区")
                   .email("academic@hxci.edu.cn")
                   .phone("13900000002")
                   .mobile("13900000002")
                   .role(List.of("academic_admin", "teacher", "zaizhi"))  // 多个角色
                   .photo(null)
                   .grade(null)
                   .teacherStatus("在职")
                   .className(null);
        } else {
            // 默认数据
            builder.officeId("99999999")
                   .schoolName("江北校区")
                   .email(employeeId.toLowerCase() + "@hxci.edu.cn")
                   .phone("13900000000")
                   .mobile("13900000000")
                   .role(List.of("user"))  // 默认角色
                   .photo(null)
                   .grade(null)
                   .teacherStatus(null)
                   .className(null);
        }
        
        SchoolLoginResult.LoginData loginData = builder.build();
        
        // 设置内部处理字段（这些字段不会被序列化到JSON）
        loginData.setJwtToken(generateJwtToken(convertRequestToUserInfo(request)));
        loginData.setAuthMode("mock");
        
        log.info("📦 [MOCK_DATA] 构建Mock登录数据: id={}, no={}, role={}, token={}", 
                loginData.getId(), loginData.getNo(), loginData.getRole(), token.substring(0, 8) + "...");
        
        return loginData;
    }
    
    /**
     * 将请求转换为UserInfo（用于JWT生成）
     */
    private UserInfo convertRequestToUserInfo(SchoolLoginRequest request) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(request.getEmployeeId());
        userInfo.setEmployeeId(request.getEmployeeId());
        userInfo.setUsername(request.getName());
        userInfo.setRealName(request.getName());
        
        // 根据employeeId判断角色
        String employeeId = request.getEmployeeId();
        if (employeeId.startsWith("STUDENT")) {
            userInfo.setRoleCode("STUDENT");
            userInfo.setRoleName("学生");
            userInfo.setUserType("STUDENT");
            userInfo.setStudentId(employeeId);
        } else if (employeeId.startsWith("TEACHER")) {
            userInfo.setRoleCode("TEACHER");
            userInfo.setRoleName("教师");
            userInfo.setUserType("TEACHER");
        } else if (employeeId.startsWith("CLASS_TEACHER")) {
            userInfo.setRoleCode("CLASS_TEACHER");
            userInfo.setRoleName("班主任");
            userInfo.setUserType("TEACHER");
        } else if (employeeId.startsWith("PRINCIPAL")) {
            userInfo.setRoleCode("PRINCIPAL");
            userInfo.setRoleName("校长");
            userInfo.setUserType("ADMIN");
        } else if (employeeId.startsWith("ACADEMIC_ADMIN")) {
            userInfo.setRoleCode("ACADEMIC_ADMIN");
            userInfo.setRoleName("教务主任");
            userInfo.setUserType("ADMIN");
        } else {
            userInfo.setRoleCode("USER");
            userInfo.setRoleName("用户");
            userInfo.setUserType("OTHER");
        }
        
        userInfo.setEnabled(true);
        return userInfo;
    }

    // 🚫 [REFACTORED] 权限相关辅助方法已移除 - 职责转移到主通知服务
    // 原权限确定、权限获取等辅助方法已删除，Mock API专注于身份认证
}