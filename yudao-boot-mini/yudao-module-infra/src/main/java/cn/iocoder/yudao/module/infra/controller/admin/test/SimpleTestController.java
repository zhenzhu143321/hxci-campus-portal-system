package cn.iocoder.yudao.module.infra.controller.admin.test;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 简单测试接口")
@RestController
@RequestMapping("/infra/test")
@Slf4j
public class SimpleTestController {

    @GetMapping("/ping")
    @Operation(summary = "简单ping测试")
    @PermitAll
    public CommonResult<String> ping() {
        log.info("[ping][简单测试调用成功]");
        return success("pong");
    }

    @GetMapping("/hello")
    @Operation(summary = "Hello测试")
    @PermitAll
    public String hello() {
        log.info("[hello][Hello测试调用成功]");
        return "Hello, World!";
    }

    @GetMapping("/status")
    @Operation(summary = "状态检查")
    @PermitAll
    public CommonResult<Object> status() {
        log.info("[status][状态检查调用成功]");
        return success(java.util.Map.of(
            "status", "ok",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "message", "系统运行正常"
        ));
    }
}