package cn.iocoder.yudao.server.controller;

import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简单测试 Controller，用于验证 Spring Boot Controller 扫描是否正常工作
 * 不依赖任何外部 Service 或复杂组件
 * 
 * @author Claude
 */
@RestController
@RequestMapping("/admin-api/server/test")
public class SimpleTestController {

    /**
     * 简单的 Hello World 测试接口
     * 路径: /admin-api/server/test/hello
     * 
     * @return 简单的字符串响应
     */
    @GetMapping("/hello")
    @PermitAll
    public String hello() {
        return "Hello World from SimpleTestController!";
    }

    /**
     * 返回服务器状态信息的测试接口
     * 路径: /admin-api/server/test/status
     * 
     * @return 状态信息字符串
     */
    @GetMapping("/status")
    @PermitAll
    public String status() {
        return "Server is running! Controller scan is working properly.";
    }
}