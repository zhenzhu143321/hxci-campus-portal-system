package cn.iocoder.yudao.server.annotation;

import java.lang.annotation.*;

/**
 * 🔐 权限验证注解 - 声明式权限校验
 * 
 * 设计目标：替代手动权限验证，提供AOP自动权限校验
 * 核心特性：级别控制 + 范围限制 + 自动缓存 + 性能优化
 * 
 * 使用示例：
 * @RequiresPermission(value = "NOTIFICATION_PUBLISH", level = 2, scope = "DEPARTMENT")
 * public void publishDepartmentNotification() { ... }
 * 
 * @author Claude AI - P0级权限缓存系统优化
 * @since 2025-08-20
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    
    /**
     * 权限代码（必需）
     * 例：NOTIFICATION_PUBLISH, TODO_MANAGE, SYSTEM_ADMIN
     */
    String value();
    
    /**
     * 通知级别要求（1-4，默认4）
     * 1=紧急，2=重要，3=常规，4=提醒
     */
    int level() default 4;
    
    /**
     * 权限范围要求（默认CLASS）
     * SCHOOL_WIDE, DEPARTMENT, GRADE, CLASS
     */
    String scope() default "CLASS";
    
    /**
     * 操作描述（用于日志和错误信息）
     */
    String description() default "";
    
    /**
     * 权限分类（notification, todo, system）
     */
    String category() default "notification";
    
    /**
     * 是否允许缓存权限结果（默认true）
     */
    boolean cacheable() default true;
    
    /**
     * 权限验证失败时的错误码（默认403）
     */
    int errorCode() default 403;
    
    /**
     * 权限验证失败时的错误信息
     */
    String errorMessage() default "权限不足，无法执行此操作";
}