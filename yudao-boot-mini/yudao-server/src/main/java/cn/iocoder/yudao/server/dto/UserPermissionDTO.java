package cn.iocoder.yudao.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户权限缓存DTO - 用户权限矩阵的完整缓存结构
 * 
 * 设计目标：15分钟TTL的用户权限完整缓存
 * 核心特性：用户信息 + 权限列表 + 缓存元数据
 * 
 * @author Claude AI - P0级权限缓存系统优化  
 * @since 2025-08-20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPermissionDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    @JsonProperty("user_id")
    private String userId;
    
    /**
     * 用户名
     */
    @JsonProperty("username")
    private String username;
    
    /**
     * 角色代码
     */
    @JsonProperty("role_code") 
    private String roleCode;
    
    /**
     * 角色名称
     */
    @JsonProperty("role_name")
    private String roleName;
    
    /**
     * 用户权限列表
     */
    @JsonProperty("permissions")
    private List<PermissionDTO> permissions;
    
    /**
     * 最高发布级别
     */
    @JsonProperty("max_publish_level")
    private Integer maxPublishLevel;
    
    /**
     * 可发布范围列表
     */
    @JsonProperty("allowed_scopes")
    private List<String> allowedScopes;
    
    /**
     * 缓存创建时间
     */
    @JsonProperty("cached_at")
    private LocalDateTime cachedAt;
    
    /**
     * 缓存版本（用于缓存失效）
     */
    @JsonProperty("cache_version")
    private String cacheVersion;
    
    /**
     * 权限数量（快速统计）
     */
    @JsonProperty("permission_count")
    private Integer permissionCount;
    
    // 默认构造函数
    public UserPermissionDTO() {
        this.cachedAt = LocalDateTime.now();
    }
    
    // 核心构造函数
    public UserPermissionDTO(String userId, String username, String roleCode, 
                           String roleName, List<PermissionDTO> permissions) {
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.permissions = permissions;
        this.cachedAt = LocalDateTime.now();
        this.permissionCount = permissions != null ? permissions.size() : 0;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRoleCode() {
        return roleCode;
    }
    
    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public List<PermissionDTO> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
        this.permissionCount = permissions != null ? permissions.size() : 0;
    }
    
    public Integer getMaxPublishLevel() {
        return maxPublishLevel;
    }
    
    public void setMaxPublishLevel(Integer maxPublishLevel) {
        this.maxPublishLevel = maxPublishLevel;
    }
    
    public List<String> getAllowedScopes() {
        return allowedScopes;
    }
    
    public void setAllowedScopes(List<String> allowedScopes) {
        this.allowedScopes = allowedScopes;
    }
    
    public LocalDateTime getCachedAt() {
        return cachedAt;
    }
    
    public void setCachedAt(LocalDateTime cachedAt) {
        this.cachedAt = cachedAt;
    }
    
    public String getCacheVersion() {
        return cacheVersion;
    }
    
    public void setCacheVersion(String cacheVersion) {
        this.cacheVersion = cacheVersion;
    }
    
    public Integer getPermissionCount() {
        return permissionCount;
    }
    
    public void setPermissionCount(Integer permissionCount) {
        this.permissionCount = permissionCount;
    }
    
    /**
     * 检查是否有特定权限
     */
    public boolean hasPermission(String permissionCode) {
        if (permissions == null) return false;
        return permissions.stream()
                .anyMatch(p -> permissionCode.equals(p.getPermissionCode()));
    }
    
    /**
     * 检查是否有特定级别的发布权限
     */
    public boolean canPublishLevel(int level) {
        return maxPublishLevel != null && maxPublishLevel >= level;
    }
    
    /**
     * 检查是否有特定范围的权限
     */
    public boolean canAccessScope(String scope) {
        return allowedScopes != null && allowedScopes.contains(scope);
    }
    
    @Override
    public String toString() {
        return "UserPermissionDTO{" +
               "userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", roleCode='" + roleCode + '\'' +
               ", permissionCount=" + permissionCount +
               ", maxPublishLevel=" + maxPublishLevel +
               ", cachedAt=" + cachedAt +
               '}';
    }
}