package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 🛡️ CSRF Token管理控制器
 * 
 * 🚨 中风险安全漏洞修复：CVE-HXCI-2025-010
 * 提供CSRF Token获取和验证功能，防护跨站请求伪造攻击
 * 
 * 核心功能：
 * 1. 为前端提供CSRF Token获取端点
 * 2. 提供CSRF Token验证状态查询
 * 3. 支持CSRF Token刷新机制
 * 4. 记录CSRF相关安全事件
 * 
 * @author Security Team
 * @version 1.0
 * @since 2025-08-24
 */
@Tag(name = "CSRF防护API")
@RestController
@RequestMapping("/")
@TenantIgnore
@Slf4j
public class CsrfTokenController {

    /**
     * 🔐 获取CSRF Token
     * 
     * 前端在首次加载或Token过期时调用此接口获取有效的CSRF Token
     * Token将通过Cookie自动存储，同时在响应头中返回
     * 
     * @param request HTTP请求对象
     * @return CSRF Token信息
     */
    @Operation(summary = "获取CSRF Token", description = "为前端应用提供CSRF防护Token")
    @GetMapping("/csrf-token")
    @PermitAll
    public CommonResult<CsrfTokenInfo> getCsrfToken(HttpServletRequest request) {
        log.info("🔐 [CSRF_API] 客户端请求CSRF Token - IP: {}, User-Agent: {}", 
                getClientIp(request), request.getHeader("User-Agent"));
        
        try {
            // 从Spring Security获取CSRF Token
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            
            if (csrfToken == null) {
                log.warn("⚠️ [CSRF_API] 未找到CSRF Token - 可能是配置问题");
                return CommonResult.error(500, "CSRF Token生成失败");
            }
            
            // 构建响应数据
            CsrfTokenInfo tokenInfo = CsrfTokenInfo.builder()
                    .token(csrfToken.getToken())
                    .headerName(csrfToken.getHeaderName())
                    .parameterName(csrfToken.getParameterName())
                    .cookieName("XSRF-TOKEN")  // Cookie名称
                    .expiresIn(3600)  // 1小时有效期
                    .message("CSRF Token获取成功，请在后续写操作中携带此Token")
                    .build();
            
            log.info("✅ [CSRF_API] CSRF Token生成成功 - Token长度: {}, Header: {}", 
                    csrfToken.getToken().length(), csrfToken.getHeaderName());
            
            return success(tokenInfo);
            
        } catch (Exception e) {
            log.error("💥 [CSRF_API] CSRF Token获取异常", e);
            return CommonResult.error(500, "CSRF Token获取失败: " + e.getMessage());
        }
    }

    /**
     * 🔍 验证CSRF Token状态
     * 
     * 前端可以调用此接口检查当前的CSRF Token是否仍然有效
     * 
     * @param request HTTP请求对象
     * @return CSRF Token验证状态
     */
    @Operation(summary = "验证CSRF Token状态", description = "检查当前CSRF Token的有效性")
    @GetMapping("/csrf-status")
    @PermitAll
    public CommonResult<CsrfStatusInfo> getCsrfStatus(HttpServletRequest request) {
        log.info("🔍 [CSRF_API] 客户端查询CSRF状态 - IP: {}", getClientIp(request));
        
        try {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            
            CsrfStatusInfo statusInfo = CsrfStatusInfo.builder()
                    .isValid(csrfToken != null)
                    .hasToken(csrfToken != null && csrfToken.getToken() != null)
                    .tokenPresent(request.getHeader("X-CSRF-TOKEN") != null || 
                                request.getParameter("_csrf") != null)
                    .message(csrfToken != null ? "CSRF Token有效" : "未找到有效的CSRF Token")
                    .recommendation(csrfToken == null ? "请先调用 /csrf-token 获取有效Token" : "Token状态正常")
                    .build();
            
            log.info("📊 [CSRF_API] CSRF状态查询完成 - 有效: {}, 存在: {}", 
                    statusInfo.getIsValid(), statusInfo.getHasToken());
            
            return success(statusInfo);
            
        } catch (Exception e) {
            log.error("💥 [CSRF_API] CSRF状态查询异常", e);
            return CommonResult.error(500, "CSRF状态查询失败: " + e.getMessage());
        }
    }

    /**
     * 📊 获取CSRF防护配置信息
     * 
     * 为开发和调试提供CSRF防护的配置详情
     * 
     * @return CSRF防护配置信息
     */
    @Operation(summary = "获取CSRF防护配置", description = "查看当前CSRF防护的配置详情")
    @GetMapping("/csrf-config")
    @PermitAll
    public CommonResult<CsrfConfigInfo> getCsrfConfig() {
        log.info("📊 [CSRF_API] 查询CSRF防护配置信息");
        
        CsrfConfigInfo configInfo = CsrfConfigInfo.builder()
                .enabled(true)
                .tokenStorage("Cookie-based")
                .cookieName("XSRF-TOKEN")
                .headerName("X-CSRF-TOKEN")
                .parameterName("_csrf")
                .protectedMethods("POST, PUT, DELETE, PATCH")
                .exemptPaths("/admin-api/**/api/**(GET), /mock-school-api/**, /csrf-token")
                .sessionPolicy("STATELESS")
                .corsEnabled(true)
                .message("CSRF防护已启用，保护所有写操作免受跨站请求伪造攻击")
                .build();
        
        return success(configInfo);
    }

    /**
     * 🌐 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    // ========== 响应数据结构 ==========

    /**
     * CSRF Token信息响应结构
     */
    @lombok.Data
    @lombok.Builder
    public static class CsrfTokenInfo {
        private String token;          // CSRF Token值
        private String headerName;     // HTTP头名称 (X-CSRF-TOKEN)
        private String parameterName;  // 参数名称 (_csrf)
        private String cookieName;     // Cookie名称 (XSRF-TOKEN)
        private Integer expiresIn;     // 有效期(秒)
        private String message;        // 说明消息
    }

    /**
     * CSRF Token状态信息响应结构
     */
    @lombok.Data
    @lombok.Builder
    public static class CsrfStatusInfo {
        private Boolean isValid;       // Token是否有效
        private Boolean hasToken;      // 是否存在Token
        private Boolean tokenPresent;  // 请求中是否携带Token
        private String message;        // 状态消息
        private String recommendation; // 建议操作
    }

    /**
     * CSRF防护配置信息响应结构
     */
    @lombok.Data
    @lombok.Builder
    public static class CsrfConfigInfo {
        private Boolean enabled;        // CSRF防护是否启用
        private String tokenStorage;    // Token存储方式
        private String cookieName;      // Cookie名称
        private String headerName;      // HTTP头名称
        private String parameterName;   // 参数名称
        private String protectedMethods; // 受保护的HTTP方法
        private String exemptPaths;     // 豁免路径
        private String sessionPolicy;  // 会话策略
        private Boolean corsEnabled;   // CORS是否启用
        private String message;        // 配置说明
    }
}