package cn.iocoder.yudao.mock.school.service;

import java.time.LocalDateTime;

/**
 * 学校Token管理服务接口
 * 管理School Basic Token生命周期
 * 
 * 职责：
 * 1. Basic Token的Redis + 数据库双重存储
 * 2. Token生命周期管理（保存、获取、刷新、失效）
 * 3. 安全存储（AES-256-GCM加密）
 * 4. 自动过期检查和刷新
 * 
 * 存储策略：
 * - Redis主存储：快速访问，TTL 30天
 * - MySQL持久化：数据持久化，防止Redis故障
 * - 加密存储：使用AES-256-GCM加密Basic Token
 * 
 * @author Backend-Developer (based on Gemini 2.5 Pro recommendations)
 */
public interface SchoolTokenService {

    /**
     * 保存或更新学校Basic Token
     * 
     * @param userId 用户ID (employeeId)
     * @param basicToken 学校API返回的Basic Token (UUID格式)
     * @param expireTime Token过期时间
     * @return 操作是否成功
     */
    boolean saveOrUpdateSchoolToken(String userId, String basicToken, LocalDateTime expireTime);

    /**
     * 获取用户的Basic Token
     * 优先从Redis获取，如果不存在则从数据库获取并缓存到Redis
     * 
     * @param userId 用户ID (employeeId)
     * @return Basic Token，如果不存在或已过期返回null
     */
    String retrieveSchoolToken(String userId);

    /**
     * 使Basic Token失效
     * 同时从Redis和数据库中删除Token
     * 
     * @param userId 用户ID (employeeId)
     * @return 操作是否成功
     */
    boolean invalidateSchoolToken(String userId);

    /**
     * 检查Token是否即将过期
     * 如果在指定时间内（如24小时）即将过期，返回true
     * 
     * @param userId 用户ID (employeeId)
     * @param hoursBeforeExpiry 过期前多少小时算作即将过期
     * @return true如果即将过期，false如果还有足够有效时间
     */
    boolean isTokenExpiringsoon(String userId, int hoursBeforeExpiry);

    /**
     * 刷新Basic Token
     * 调用学校API获取新Token，并更新存储
     * 
     * @param userId 用户ID (employeeId)
     * @return 新的Basic Token，如果刷新失败返回null
     */
    String refreshToken(String userId);

    /**
     * 获取Token过期时间
     * 
     * @param userId 用户ID (employeeId)
     * @return Token过期时间，如果Token不存在返回null
     */
    LocalDateTime getTokenExpireTime(String userId);

    /**
     * 批量清理过期Token
     * 定时任务调用，清理Redis和数据库中的过期Token
     * 
     * @return 清理的Token数量
     */
    int cleanupExpiredTokens();

    /**
     * 获取缓存统计信息
     * 用于监控和调试
     * 
     * @return 包含缓存命中率、Token数量等统计信息的Map
     */
    java.util.Map<String, Object> getCacheStats();

    /**
     * 验证Token格式
     * 检查Token是否符合预期格式（UUID）
     * 
     * @param basicToken Basic Token
     * @return true如果格式正确，false如果格式错误
     */
    boolean validateTokenFormat(String basicToken);
}