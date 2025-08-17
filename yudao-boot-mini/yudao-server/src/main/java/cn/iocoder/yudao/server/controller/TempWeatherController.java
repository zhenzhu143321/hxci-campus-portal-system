package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.validation.annotation.Validated;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import java.time.LocalDateTime;
import java.util.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 🌤️ 天气缓存系统 Controller - 仿照TempNotificationController
 * 位于yudao-server模块，使用双重认证模式
 * 
 * @author Claude AI
 * @since 2025-08-14
 */
@Tag(name = "天气缓存系统API")
@RestController
@RequestMapping("/admin-api/test/weather")
@Validated
@TenantIgnore
@Component
@Slf4j
public class TempWeatherController {

    private static final String MOCK_API_BASE = "http://localhost:48082";
    private final RestTemplate restTemplate;
    
    // 构造函数：配置支持HTTPS和gzip的RestTemplate
    public TempWeatherController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10秒连接超时
        factory.setReadTimeout(10000);    // 10秒读取超时
        
        this.restTemplate = new RestTemplate(factory);
        log.info("🔧 [WEATHER-INIT] RestTemplate配置完成，支持HTTPS和超时设置");
    }

    /**
     * 🌤️ 获取当前天气数据 - 双重认证版本
     */
    @GetMapping("/api/current")
    @Operation(summary = "获取当前天气数据(双重认证)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> getCurrentWeather(HttpServletRequest httpRequest) {
        log.info("🌤️ [WEATHER] 收到天气数据请求 - 使用双重认证模式");
        
        try {
            // 🔐 Step 1: 从请求头获取认证Token
            String authToken = httpRequest.getHeader("Authorization");
            log.info("🔐 [WEATHER] 获取到Authorization头: {}", 
                    authToken != null ? authToken.substring(0, Math.min(20, authToken.length())) + "..." : "null");
            
            if (authToken == null) {
                log.warn("❌ [WEATHER] 未提供认证Token");
                return CommonResult.error(401, "未提供认证Token");
            }

            // 🔍 Step 2: 验证Token并获取用户信息
            log.info("🔍 [WEATHER] 验证Token并获取用户信息...");
            UserInfo userInfo = getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                log.warn("❌ [WEATHER] Token验证失败或用户信息获取失败");
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [WEATHER] 用户认证成功: {} (角色: {})", userInfo.username, userInfo.roleCode);

            // ✅ Step 3: 认证通过，获取天气数据
            Map<String, Object> weatherData = getCurrentWeatherFromDatabase();
            
            if (weatherData != null) {
                log.info("✅ [WEATHER] 成功返回天气数据 (用户: {})", userInfo.username);
                Map<String, Object> result = new HashMap<>();
                result.put("weather", weatherData);
                result.put("user", Map.of(
                    "username", userInfo.username,
                    "roleCode", userInfo.roleCode,
                    "roleName", userInfo.roleName
                ));
                result.put("timestamp", System.currentTimeMillis());
                return success(result);
            } else {
                log.warn("⚠️ [WEATHER] 缓存中无天气数据，返回默认数据 (用户: {})", userInfo.username);
                Map<String, Object> defaultWeather = getDefaultWeatherData();
                Map<String, Object> result = new HashMap<>();
                result.put("weather", defaultWeather);
                result.put("user", Map.of(
                    "username", userInfo.username,
                    "roleCode", userInfo.roleCode,
                    "roleName", userInfo.roleName
                ));
                result.put("timestamp", System.currentTimeMillis());
                return success(result);
            }
        } catch (Exception e) {
            log.error("❌ [WEATHER] 获取天气数据异常", e);
            // 降级方案：返回默认天气数据
            Map<String, Object> defaultWeather = getDefaultWeatherData();
            Map<String, Object> result = new HashMap<>();
            result.put("weather", defaultWeather);
            result.put("error", "服务异常，返回默认数据");
            result.put("timestamp", System.currentTimeMillis());
            return success(result);
        }
    }

    /**
     * 🔄 手动刷新天气数据
     */
    @PostMapping("/api/refresh")
    @Operation(summary = "手动刷新天气数据(双重认证)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> refreshWeatherData(HttpServletRequest httpRequest) {
        log.info("🔄 [WEATHER-REFRESH] 收到手动刷新请求");
        
        try {
            // 🔐 认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [WEATHER-REFRESH] 用户认证成功: {} (角色: {})", userInfo.username, userInfo.roleCode);

            // ✅ 刷新天气数据
            Map<String, Object> refreshedData = refreshWeatherFromAPI();
            
            Map<String, Object> result = new HashMap<>();
            result.put("weather", refreshedData);
            result.put("refreshedBy", userInfo.username);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [WEATHER-REFRESH] 天气数据刷新成功 (操作者: {})", userInfo.username);
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [WEATHER-REFRESH] 天气数据刷新异常", e);
            return CommonResult.error(500, "天气数据刷新异常: " + e.getMessage());
        }
    }

    /**
     * 🧪 测试接口
     */
    @GetMapping("/api/ping")
    @Operation(summary = "天气服务Ping测试")
    @PermitAll
    @TenantIgnore
    public CommonResult<String> ping() {
        log.info("🏓 [WEATHER-PING] 天气服务ping测试");
        return success("pong from TempWeatherController - server module");
    }

    /**
     * 从Mock API获取用户信息 (仿照通知API)
     */
    private UserInfo getUserInfoFromMockApi(String authToken) {
        try {
            String url = MOCK_API_BASE + "/mock-school-api/auth/user-info";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                log.info("🔍 [WEATHER-API] Mock API响应: {}", body);
                
                Object codeObj = body.get("code");
                boolean isSuccess = (codeObj instanceof Integer && (Integer) codeObj == 200) || 
                                  (codeObj instanceof String && "200".equals(codeObj));
                
                if (isSuccess && body.get("data") != null) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    UserInfo userInfo = new UserInfo();
                    userInfo.username = (String) data.get("username");
                    userInfo.roleCode = (String) data.get("roleCode");
                    userInfo.roleName = (String) data.get("roleName");
                    
                    log.info("✅ [WEATHER-API] 用户信息解析成功: user={}, role={}", userInfo.username, userInfo.roleCode);
                    return userInfo;
                } else {
                    log.warn("❌ [WEATHER-API] Mock API响应失败: code={}, success={}", 
                            body.get("code"), body.get("success"));
                }
            }
        } catch (Exception e) {
            log.error("🔗 [WEATHER-API] Mock API调用异常: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 从数据库获取当前天气数据
     */
    private Map<String, Object> getCurrentWeatherFromDatabase() {
        try {
            log.info("💾 [WEATHER-DB] 开始查询weather_cache表");
            
            String querySql = "SELECT city_code, city_name, temperature, weather_text, humidity, wind_dir, wind_scale, " +
                "DATE_FORMAT(update_time, '%Y-%m-%dT%H:%i:%s') as update_time, " +
                "DATE_FORMAT(api_update_time, '%Y-%m-%dT%H:%i:%s') as api_update_time " +
                "FROM weather_cache WHERE city_code='101050101' ORDER BY update_time DESC LIMIT 1";
            
            String mysqlCommand = String.format(
                "mysql -u root ruoyi-vue-pro --default-character-set=utf8 -e \"%s\"",
                querySql.replace("\"", "\\\"")
            );
            
            Process process = Runtime.getRuntime().exec(mysqlCommand);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
            
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && lines.size() > 1) {
                String dataLine = lines.get(lines.size() - 1);
                String[] fields = dataLine.split("\t", -1);
                
                if (fields.length >= 7) {
                    Map<String, Object> weather = new HashMap<>();
                    weather.put("cityCode", fields[0]);
                    weather.put("cityName", fields[1]);
                    weather.put("temperature", parseIntSafely(fields[2]));
                    weather.put("weatherText", fields[3]);
                    weather.put("humidity", parseIntSafely(fields[4]));
                    weather.put("windDir", fields[5]);
                    weather.put("windScale", fields[6]);
                    weather.put("updateTime", fields.length > 7 && !"NULL".equals(fields[7]) ? fields[7] : null);
                    weather.put("apiUpdateTime", fields.length > 8 && !"NULL".equals(fields[8]) ? fields[8] : null);
                    
                    log.info("✅ [WEATHER-DB] 成功获取天气数据: {} {} {}°C", fields[1], fields[3], fields[2]);
                    return weather;
                }
            }
            
            log.warn("⚠️ [WEATHER-DB] 数据库中无天气数据");
            return null;
            
        } catch (Exception e) {
            log.error("💥 [WEATHER-DB] 数据库查询异常", e);
            return null;
        }
    }

    /**
     * 从和风天气API刷新数据
     */
    private Map<String, Object> refreshWeatherFromAPI() {
        try {
            log.info("🌐 [QWEATHER-API] 开始调用和风天气API");
            
            // 1. 生成JWT Token
            String jwtToken = generateQWeatherJWT();
            if (jwtToken == null) {
                log.error("❌ [QWEATHER-API] JWT Token生成失败");
                return getDefaultWeatherData();
            }

            // 2. 调用和风天气API
            String apiUrl = "https://kc62b63hjr.re.qweatherapi.com/v7/weather/now?location=101050101";
            log.info("📤 [QWEATHER-API] 请求API: {}", apiUrl);
            log.info("🔑 [QWEATHER-API] 使用JWT Token: {}...", jwtToken.substring(0, Math.min(20, jwtToken.length())));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");
            
            HttpEntity<String> entity = new HttpEntity<>("", headers);
            
            // 使用自定义ResponseExtractor处理gzip响应
            String responseBody = restTemplate.execute(apiUrl, HttpMethod.GET, 
                requestCallback -> {
                    requestCallback.getHeaders().putAll(headers);
                },
                new ResponseExtractor<String>() {
                    @Override
                    public String extractData(ClientHttpResponse response) throws IOException {
                        InputStream inputStream = response.getBody();
                        
                        // 检查是否是gzip压缩
                        String contentEncoding = response.getHeaders().getFirst("Content-Encoding");
                        if ("gzip".equals(contentEncoding)) {
                            inputStream = new GZIPInputStream(inputStream);
                        }
                        
                        // 读取响应内容
                        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                });
            
            log.info("📥 [QWEATHER-API] HTTP状态码: {}", responseBody != null ? "200" : "ERROR");
            log.info("📋 [QWEATHER-API] 响应内容: {}", responseBody);
            
            if (responseBody != null) {
                // 解析JSON响应
                ObjectMapper objectMapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                if ("200".equals(responseMap.get("code"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nowData = (Map<String, Object>) responseMap.get("now");
                    String updateTime = (String) responseMap.get("updateTime");
                    
                    Map<String, Object> weatherData = parseQWeatherResponse(nowData, updateTime);
                    
                    // 3. 更新数据库缓存
                    updateWeatherCache(weatherData);
                    
                    log.info("✅ [QWEATHER-API] 天气数据刷新成功: {} {} {}°C", 
                        weatherData.get("cityName"), weatherData.get("weatherText"), weatherData.get("temperature"));
                    return weatherData;
                } else {
                    log.error("❌ [QWEATHER-API] 和风天气API返回错误: {}", responseMap);
                }
            } else {
                log.error("❌ [QWEATHER-API] HTTP请求失败或响应为空");
            }
            
        } catch (Exception e) {
            log.error("❌ [QWEATHER-API] 调用和风天气API异常", e);
        }
        
        return getDefaultWeatherData();
    }

    /**
     * 生成和风天气JWT Token (Java版本)
     */
    private String generateQWeatherJWT() {
        try {
            // 调用Python脚本生成JWT Token
            String scriptPath = "/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/weather/generate-weather-jwt.py";
            ProcessBuilder processBuilder = new ProcessBuilder("python3", scriptPath);
            // 设置工作目录为脚本所在目录，确保能找到私钥文件
            processBuilder.directory(new java.io.File("/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/weather"));
            Process process = processBuilder.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line;
            String token = null;
            
            // 查找Token行（包含很长的字符串）
            while ((line = reader.readLine()) != null) {
                if (line.length() > 100 && !line.contains("=") && !line.contains("curl")) {
                    token = line.trim();
                    break;
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0 && token != null) {
                log.info("✅ [JWT] 和风天气JWT Token生成成功: {}...", token.substring(0, Math.min(20, token.length())));
                return token;
            } else {
                log.error("❌ [JWT] JWT生成脚本执行失败，退出码: {}, token: {}", exitCode, token);
                return null;
            }
        } catch (Exception e) {
            log.error("❌ [JWT] JWT Token生成异常", e);
            return null;
        }
    }

    /**
     * 解析和风天气API响应数据
     */
    private Map<String, Object> parseQWeatherResponse(Map<String, Object> nowData, String updateTime) {
        Map<String, Object> weather = new HashMap<>();
        weather.put("cityCode", "101050101");
        weather.put("cityName", "哈尔滨");
        weather.put("temperature", Integer.parseInt(nowData.get("temp").toString()));
        weather.put("weatherText", nowData.get("text").toString());
        weather.put("humidity", nowData.containsKey("humidity") ? 
            Integer.parseInt(nowData.get("humidity").toString()) : null);
        weather.put("windDir", nowData.containsKey("windDir") ? nowData.get("windDir").toString() : null);
        weather.put("windScale", nowData.containsKey("windScale") ? 
            nowData.get("windScale").toString() + "级" : null);
        weather.put("updateTime", LocalDateTime.now().toString());
        weather.put("apiUpdateTime", updateTime != null ? updateTime.substring(0, 19) : null);
        
        return weather;
    }

    /**
     * 更新数据库缓存
     */
    private void updateWeatherCache(Map<String, Object> weatherData) {
        try {
            String insertSql = String.format(
                "INSERT INTO weather_cache (city_code, city_name, temperature, weather_text, humidity, wind_dir, wind_scale, update_time, api_update_time) " +
                "VALUES ('%s', '%s', %d, '%s', %s, '%s', '%s', NOW(), '%s') " +
                "ON DUPLICATE KEY UPDATE " +
                "temperature = VALUES(temperature), weather_text = VALUES(weather_text), " +
                "humidity = VALUES(humidity), wind_dir = VALUES(wind_dir), wind_scale = VALUES(wind_scale), " +
                "update_time = VALUES(update_time), api_update_time = VALUES(api_update_time)",
                weatherData.get("cityCode"),
                weatherData.get("cityName"),
                weatherData.get("temperature"),
                weatherData.get("weatherText"),
                weatherData.get("humidity"),
                weatherData.get("windDir"),
                weatherData.get("windScale"),
                weatherData.get("apiUpdateTime")
            );

            String mysqlCommand = String.format(
                "mysql -u root ruoyi-vue-pro --default-character-set=utf8 -e \"%s\"",
                insertSql.replace("\"", "\\\"")
            );

            Process process = Runtime.getRuntime().exec(mysqlCommand);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.debug("✅ [WEATHER-DB] 数据库缓存更新完成");
            } else {
                log.error("❌ [WEATHER-DB] 数据库缓存更新失败");
            }
        } catch (Exception e) {
            log.error("❌ [WEATHER-DB] 更新数据库缓存异常", e);
        }
    }

    /**
     * 获取默认天气数据（降级方案）
     */
    private Map<String, Object> getDefaultWeatherData() {
        Map<String, Object> defaultWeather = new HashMap<>();
        defaultWeather.put("cityCode", "101050101");
        defaultWeather.put("cityName", "哈尔滨");
        defaultWeather.put("temperature", 20);
        defaultWeather.put("weatherText", "晴");
        defaultWeather.put("humidity", 60);
        defaultWeather.put("windDir", "西南风");
        defaultWeather.put("windScale", "2级");
        defaultWeather.put("updateTime", LocalDateTime.now().toString());
        defaultWeather.put("apiUpdateTime", LocalDateTime.now().toString());
        defaultWeather.put("isDefault", true);
        
        return defaultWeather;
    }

    /**
     * 安全的整数解析
     */
    private Integer parseIntSafely(String str) {
        try {
            return "NULL".equals(str) ? null : Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========================= 定时任务功能 =========================

    /**
     * 🕒 定时任务：自动刷新天气数据
     * 每30分钟执行一次，从和风天气API获取最新数据并缓存到数据库
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟 = 30 * 60 * 1000毫秒
    public void autoRefreshWeatherData() {
        log.info("🕒 [WEATHER-SCHEDULER] 定时任务启动 - 自动刷新天气数据");
        
        try {
            // 执行天气数据刷新
            Map<String, Object> refreshedData = refreshWeatherFromAPI();
            
            if (refreshedData != null && !refreshedData.containsKey("isDefault")) {
                log.info("✅ [WEATHER-SCHEDULER] 定时刷新成功: {} {} {}°C", 
                    refreshedData.get("cityName"), 
                    refreshedData.get("weatherText"), 
                    refreshedData.get("temperature"));
            } else {
                log.warn("⚠️ [WEATHER-SCHEDULER] 定时刷新失败，API可能异常");
            }
            
        } catch (Exception e) {
            log.error("❌ [WEATHER-SCHEDULER] 定时任务执行异常", e);
        }
    }

    /**
     * 🌅 应用启动时立即刷新一次天气数据
     * 确保系统启动后有最新的天气信息
     */
    @Scheduled(initialDelay = 10000, fixedRate = Long.MAX_VALUE) // 启动10秒后执行一次
    public void initializeWeatherData() {
        log.info("🌅 [WEATHER-INIT] 应用启动初始化 - 获取天气数据");
        
        try {
            // 检查数据库是否有数据
            Map<String, Object> existingData = getCurrentWeatherFromDatabase();
            
            if (existingData == null) {
                log.info("💾 [WEATHER-INIT] 数据库无缓存数据，立即从API获取");
                autoRefreshWeatherData();
            } else {
                log.info("✅ [WEATHER-INIT] 数据库已有缓存数据: {} {} {}°C", 
                    existingData.get("cityName"), 
                    existingData.get("weatherText"), 
                    existingData.get("temperature"));
            }
            
        } catch (Exception e) {
            log.error("❌ [WEATHER-INIT] 初始化天气数据异常", e);
        }
    }

    /**
     * 用户信息类
     */
    public static class UserInfo {
        public String username;
        public String roleCode;
        public String roleName;
    }
}