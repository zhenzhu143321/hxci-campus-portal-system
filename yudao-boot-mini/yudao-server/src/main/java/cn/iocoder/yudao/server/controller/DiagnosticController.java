package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "诊断测试Controller")
@RestController
@RequestMapping("/admin-api/server/diagnostic")
@TenantIgnore
@Slf4j
public class DiagnosticController {
    
    @GetMapping("/ping")
    @Operation(summary = "诊断ping测试")
    @PermitAll
    public CommonResult<String> ping() {
        log.info("🔍 [DIAGNOSTIC] 诊断Controller ping成功");
        return success("诊断Controller工作正常 - server模块");
    }

    @GetMapping("/notification-path-test")
    @Operation(summary = "测试通知路径")
    @PermitAll
    public CommonResult<String> notificationPathTest() {
        log.info("🔍 [DIAGNOSTIC] 通知路径测试");
        return success("通知路径测试成功 - 模拟infra/notification路径");
    }
}