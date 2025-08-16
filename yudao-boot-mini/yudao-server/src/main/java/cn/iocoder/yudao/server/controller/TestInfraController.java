package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 测试Controller - 验证admin-api/infra路径是否可以在server模块中工作
 */
@RestController
@RequestMapping("/admin-api/infra/server-test")
@Slf4j
public class TestInfraController {

    @GetMapping("/ping")
    @PermitAll
    public CommonResult<String> ping() {
        log.info("[ping][server模块测试Controller调用成功]");
        return success("server模块中的admin-api/infra/server-test/ping工作正常");
    }

    @GetMapping("/info")
    @PermitAll
    public CommonResult<String> info() {
        log.info("[info][server模块info接口调用成功]");
        return success("位于yudao-server模块，路径：/admin-api/infra/server-test");
    }
}