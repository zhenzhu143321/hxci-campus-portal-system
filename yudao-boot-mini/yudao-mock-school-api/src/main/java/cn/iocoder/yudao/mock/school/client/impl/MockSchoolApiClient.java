package cn.iocoder.yudao.mock.school.client.impl;

import cn.iocoder.yudao.mock.school.client.SchoolApiClient;
import cn.iocoder.yudao.mock.school.model.SchoolUserInfo;
import cn.iocoder.yudao.mock.school.exception.SchoolApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock模式的学校API客户端实现
 * 使用内存数据模拟学校API响应，便于开发和测试
 * 
 * @author Claude
 * @since 2025-09-04
 */
@Component("mockSchoolApiClient")
public class MockSchoolApiClient implements SchoolApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(MockSchoolApiClient.class);
    private static final String MODE = "MOCK";
    
    // Mock用户数据
    private static final Map<String, MockUserData> MOCK_USERS = new HashMap<>();
    
    static {
        // 学生用户
        MOCK_USERS.put("2023010105", new MockUserData(
            "2023010105", "张小明", "计算机学院", "软件工程", "软件2301", 
            "13800138001", "男", "2023", new String[]{"学生"}, "student"
        ));
        
        // 教师用户
        MOCK_USERS.put("10031", new MockUserData(
            "10031", "李老师", "计算机学院", "软件工程", "教师", 
            "13900139001", "女", "", new String[]{"教师"}, "teacher"
        ));
        
        // 教务主任
        MOCK_USERS.put("10001", new MockUserData(
            "10001", "王主任", "教务处", "管理", "教务主任", 
            "13700137001", "男", "", new String[]{"教务主任"}, "teacher"
        ));
        
        // 校长
        MOCK_USERS.put("10000", new MockUserData(
            "10000", "校长张明", "校长办公室", "管理", "校长", 
            "13600136001", "男", "", new String[]{"校长"}, "teacher"
        ));
    }
    
    @Override
    public SchoolUserInfo login(String username, String password) throws SchoolApiException {
        log.info("🔄 [MOCK_MODE] 开始Mock模式登录验证: username={}", username);
        
        try {
            // 模拟网络延迟
            Thread.sleep(200);
            
            // 验证密码（Mock模式统一密码：888888）
            if (!"888888".equals(password)) {
                throw new SchoolApiException("密码错误", "AUTH_FAILED", MODE);
            }
            
            // 查找用户
            MockUserData userData = MOCK_USERS.get(username);
            if (userData == null) {
                throw new SchoolApiException("用户不存在", "USER_NOT_FOUND", MODE);
            }
            
            // 构建返回对象
            SchoolUserInfo userInfo = new SchoolUserInfo();
            userInfo.setStudentNo(userData.studentNo);
            userInfo.setName(userData.name);
            userInfo.setCollege(userData.college);
            userInfo.setMajor(userData.major);
            userInfo.setClassName(userData.className);
            userInfo.setPhone(userData.phone);
            userInfo.setGender(userData.gender);
            userInfo.setGrade(userData.grade);
            userInfo.setRoles(userData.roles);
            userInfo.setUserType(userData.userType);
            userInfo.setBasicToken(UUID.randomUUID().toString()); // Mock Basic Token
            
            // 添加Mock原始数据
            userInfo.addRawData("mode", MODE);
            userInfo.addRawData("mockUser", true);
            userInfo.addRawData("loginTime", System.currentTimeMillis());
            
            log.info("✅ [MOCK_MODE] Mock登录成功: user={}, type={}", 
                    userInfo.getName(), userInfo.getUserType());
            
            return userInfo;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchoolApiException("Mock登录被中断", e, "INTERRUPTED", MODE);
        } catch (SchoolApiException e) {
            log.warn("❌ [MOCK_MODE] Mock登录失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("💥 [MOCK_MODE] Mock登录异常", e);
            throw new SchoolApiException("Mock登录服务异常: " + e.getMessage(), e, "SYSTEM_ERROR", MODE);
        }
    }
    
    @Override
    public String getMode() {
        return MODE;
    }
    
    // 内部Mock用户数据类
    private static class MockUserData {
        public final String studentNo;
        public final String name;
        public final String college;
        public final String major;
        public final String className;
        public final String phone;
        public final String gender;
        public final String grade;
        public final String[] roles;
        public final String userType;
        
        public MockUserData(String studentNo, String name, String college, String major, 
                          String className, String phone, String gender, String grade, 
                          String[] roles, String userType) {
            this.studentNo = studentNo;
            this.name = name;
            this.college = college;
            this.major = major;
            this.className = className;
            this.phone = phone;
            this.gender = gender;
            this.grade = grade;
            this.roles = roles;
            this.userType = userType;
        }
    }
}