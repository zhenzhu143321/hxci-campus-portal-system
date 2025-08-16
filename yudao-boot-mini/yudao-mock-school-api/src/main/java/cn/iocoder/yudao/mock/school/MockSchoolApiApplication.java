package cn.iocoder.yudao.mock.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Mock School API 应用主类
 * 模拟教育机构的用户认证和权限验证服务
 * 
 * @author Claude
 */
@SpringBootApplication
@EntityScan(basePackages = "cn.iocoder.yudao.mock.school.entity")
@EnableJpaRepositories(basePackages = "cn.iocoder.yudao.mock.school.repository")
public class MockSchoolApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockSchoolApiApplication.class, args);
        System.out.println("========================================");
        System.out.println("🎓 Mock School API 服务启动成功!");
        System.out.println("📋 API文档地址: http://localhost:48082/swagger-ui.html");
        System.out.println("🔍 健康检查: http://localhost:48082/mock-school-api/auth/health");
        System.out.println("========================================");
    }
    
    /**
     * 配置CORS跨域支持，允许前端页面调用API
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}