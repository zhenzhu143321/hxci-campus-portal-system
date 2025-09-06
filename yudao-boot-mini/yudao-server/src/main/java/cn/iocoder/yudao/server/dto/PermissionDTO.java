package cn.iocoder.yudao.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限DTO - 替代低效的Map<String, Object>
 * 
 * 设计目标：提升权限数据序列化和反序列化性能
 * 核心优势：类型安全 + JSON优化 + 内存效率
 * 
 * @author Claude AI - P0级权限缓存系统优化
 * @since 2025-08-20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 权限代码（如：NOTIFICATION_PUBLISH_LEVEL_1）
     */
    @JsonProperty("permission_code")
    private String permissionCode;
    
    /**
     * 权限名称
     */
    @JsonProperty("permission_name")  
    private String permissionName;
    
    /**
     * 权限级别（1-4）
     */
    @JsonProperty("level")
    private Integer level;
    
    /**
     * 权限范围（SCHOOL_WIDE/DEPARTMENT/GRADE/CLASS）
     */
    @JsonProperty("scope")
    private String scope;
    
    /**
     * 是否需要审批
     */
    @JsonProperty("requires_approval")
    private Boolean requiresApproval;
    
    /**
     * 权限描述
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * 权限分类（notification/todo/system）
     */
    @JsonProperty("category")
    private String category;
    
    // 默认构造函数
    public PermissionDTO() {}
    
    // 简化构造函数（用于基础权限创建）
    public PermissionDTO(String permissionCode, String permissionName, String category) {
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.category = category;
        this.requiresApproval = false; // 默认不需要审批
    }
    
    // 全参构造函数
    public PermissionDTO(String permissionCode, String permissionName, Integer level, 
                        String scope, Boolean requiresApproval, String description, String category) {
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.level = level;
        this.scope = scope;
        this.requiresApproval = requiresApproval;
        this.description = description;
        this.category = category;
    }
    
    // Getters and Setters
    public String getPermissionCode() {
        return permissionCode;
    }
    
    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
    
    public String getPermissionName() {
        return permissionName;
    }
    
    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public Boolean getRequiresApproval() {
        return requiresApproval;
    }
    
    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    @Override
    public String toString() {
        return "PermissionDTO{" +
               "permissionCode='" + permissionCode + '\'' +
               ", level=" + level +
               ", scope='" + scope + '\'' +
               ", category='" + category + '\'' +
               '}';
    }
}