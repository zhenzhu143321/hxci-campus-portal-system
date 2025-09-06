package cn.iocoder.yudao.mock.school.config;

import cn.iocoder.yudao.mock.school.client.SchoolApiClient;
import cn.iocoder.yudao.mock.school.client.impl.MockSchoolApiClient;
import cn.iocoder.yudao.mock.school.client.impl.RealSchoolApiClient;
import cn.iocoder.yudao.mock.school.client.impl.FallbackSchoolApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 学校API客户端配置类
 * 基于配置文件驱动选择合适的SchoolApiClient实现
 * 
 * @author Claude
 * @since 2025-09-04
 */
@Configuration
@EnableConfigurationProperties(SchoolApiProperties.class)
public class SchoolApiClientConfig {
    
    private static final Logger log = LoggerFactory.getLogger(SchoolApiClientConfig.class);
    
    /**
     * 主要的SchoolApiClient Bean
     * 基于配置驱动选择Mock或Real实现，支持降级机制
     */
    @Bean
    @Primary
    public SchoolApiClient schoolApiClient(SchoolApiProperties properties,
                                         MockSchoolApiClient mockClient,
                                         RealSchoolApiClient realClient) {
        
        log.info("🏗️ [CONFIG] 初始化SchoolApiClient: mode={}, fallback={}", 
                properties.getMode(), properties.isFallbackEnabled());
        
        // 根据配置模式选择主客户端
        SchoolApiClient primaryClient;
        if (properties.getMode() == SchoolApiProperties.Mode.REAL) {
            primaryClient = realClient;
            log.info("🌐 [CONFIG] 选择Real模式作为主客户端");
        } else {
            primaryClient = mockClient;
            log.info("🔄 [CONFIG] 选择Mock模式作为主客户端");
        }
        
        // 如果启用降级机制，使用FallbackSchoolApiClient包装
        if (properties.isFallbackEnabled()) {
            log.info("🛡️ [CONFIG] 启用降级机制，Real模式失败时自动切换Mock模式");
            return new FallbackSchoolApiClient(primaryClient, mockClient, properties);
        } else {
            log.info("⚠️ [CONFIG] 降级机制已禁用，使用单一模式");
            return primaryClient;
        }
    }
    
    /**
     * Mock客户端Bean（独立注册，便于注入和测试）
     */
    @Bean("mockSchoolApiClient")
    public MockSchoolApiClient mockSchoolApiClient() {
        log.info("🔄 [CONFIG] 注册MockSchoolApiClient");
        return new MockSchoolApiClient();
    }
    
    /**
     * Real客户端Bean（独立注册，便于注入和测试）
     */
    @Bean("realSchoolApiClient")
    public RealSchoolApiClient realSchoolApiClient(SchoolApiProperties properties,
                                                  org.springframework.boot.web.client.RestTemplateBuilder restTemplateBuilder) {
        log.info("🌐 [CONFIG] 注册RealSchoolApiClient: baseUrl={}", 
                properties.getReal().getBaseUrl());
        return new RealSchoolApiClient(restTemplateBuilder, properties);
    }
}