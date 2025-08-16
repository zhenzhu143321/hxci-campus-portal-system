package cn.iocoder.yudao.mock.school.config;

import cn.iocoder.yudao.mock.school.entity.MockSchoolUser;
import cn.iocoder.yudao.mock.school.entity.MockRolePermission;
import cn.iocoder.yudao.mock.school.repository.MockSchoolUserRepository;
import cn.iocoder.yudao.mock.school.repository.MockRolePermissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Mock数据初始化配置
 * 在应用启动时自动创建测试用户和权限数据
 * 
 * @author Claude
 */
@Component
public class MockDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);

    @Autowired
    private MockSchoolUserRepository userRepository;

    @Autowired
    private MockRolePermissionRepository permissionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${mock.school-api.auto-init-data:true}")
    private boolean autoInitData;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoInitData) {
            log.info("自动初始化数据已禁用");
            return;
        }

        log.info("开始初始化Mock School API测试数据...");
        
        try {
            // 初始化角色权限
            initRolePermissions();
            
            // 初始化测试用户
            initTestUsers();
            
            log.info("Mock School API测试数据初始化完成！");
            
        } catch (Exception e) {
            log.error("初始化测试数据失败", e);
        }
    }

    /**
     * 初始化角色权限数据
     */
    private void initRolePermissions() {
        log.info("初始化角色权限数据...");
        
        // 检查是否已经存在数据
        if (permissionRepository.count() > 0) {
            log.info("角色权限数据已存在，跳过初始化");
            return;
        }

        // 校长权限
        permissionRepository.save(new MockRolePermission(null, "PRINCIPAL", "NOTIFY_ALL", "全校通知权限", "1,2,3,4", "ALL_SCHOOL", false, "校长拥有全校范围的所有级别通知权限", LocalDateTime.now()));
        
        // 教务处长权限
        permissionRepository.save(new MockRolePermission(null, "ACADEMIC_ADMIN", "NOTIFY_ACADEMIC", "教务通知权限", "1,2,3,4", "ACADEMIC_RELATED", false, "教务处长拥有教务相关的所有级别通知权限", LocalDateTime.now()));
        
        // 学院院长权限
        permissionRepository.save(new MockRolePermission(null, "DEAN", "NOTIFY_COLLEGE", "学院通知权限", "1,2,3,4", "COLLEGE_SCOPE", false, "院长拥有学院范围的所有级别通知权限", LocalDateTime.now()));
        
        // 辅导员权限
        permissionRepository.save(new MockRolePermission(null, "COUNSELOR", "NOTIFY_STUDENTS", "学生通知权限", "3,4", "STUDENT_SCOPE", true, "辅导员可发布常规和提醒通知，需要上级审批", LocalDateTime.now()));
        
        // 任课教师权限
        permissionRepository.save(new MockRolePermission(null, "TEACHER", "NOTIFY_COURSE", "课程通知权限", "3,4", "COURSE_SCOPE", false, "教师可向选课学生发布常规和提醒通知", LocalDateTime.now()));
        
        // 学生权限
        permissionRepository.save(new MockRolePermission(null, "STUDENT", "VIEW_NOTIFY", "查看通知权限", "", "SELF_ONLY", false, "学生只能查看通知", LocalDateTime.now()));

        log.info("角色权限数据初始化完成，共创建 {} 条记录", permissionRepository.count());
    }

    /**
     * 初始化测试用户数据
     */
    private void initTestUsers() {
        log.info("初始化测试用户数据...");
        
        // 检查是否已经存在数据
        if (userRepository.count() > 0) {
            log.info("测试用户数据已存在，跳过初始化");
            return;
        }

        try {
            // 校长
            createTestUser("校长张三", "principal_001", "PRINCIPAL", "校长", 1L, "校长办公室", 
                Arrays.asList("EMERGENCY_NOTIFY", "IMPORTANT_NOTIFY", "REGULAR_NOTIFY", "REMINDER_NOTIFY"));

            // 教务处长
            createTestUser("教务处长李四", "academic_admin_001", "ACADEMIC_ADMIN", "教务处长", 2L, "教务处",
                Arrays.asList("EMERGENCY_NOTIFY", "IMPORTANT_NOTIFY", "REGULAR_NOTIFY", "REMINDER_NOTIFY"));

            // 软件学院院长
            createTestUser("软件学院院长王五", "dean_001", "DEAN", "学院院长", 3L, "软件学院",
                Arrays.asList("EMERGENCY_NOTIFY", "IMPORTANT_NOTIFY", "REGULAR_NOTIFY", "REMINDER_NOTIFY"));

            // 辅导员
            createTestUser("辅导员赵六", "counselor_001", "COUNSELOR", "辅导员", 4L, "软件学院学生工作办",
                Arrays.asList("REGULAR_NOTIFY", "REMINDER_NOTIFY"));

            // 任课教师
            createTestUser("任课教师孙七", "teacher_001", "TEACHER", "任课教师", 5L, "软件工程系",
                Arrays.asList("REGULAR_NOTIFY", "REMINDER_NOTIFY"));

            // 学生
            createTestUser("学生张同学", "student_001", "STUDENT", "学生", 6L, "软件工程2021级1班",
                Arrays.asList("VIEW_NOTIFY"));

            log.info("测试用户数据初始化完成，共创建 {} 个用户", userRepository.count());
            
        } catch (Exception e) {
            log.error("创建测试用户失败", e);
        }
    }

    /**
     * 创建测试用户
     */
    private void createTestUser(String username, String userId, String roleCode, String roleName,
                               Long departmentId, String departmentName, java.util.List<String> permissions) {
        try {
            MockSchoolUser user = new MockSchoolUser();
            user.setUsername(username);
            user.setUserId(userId);
            user.setRoleCode(roleCode);
            user.setRoleName(roleName);
            user.setDepartmentId(departmentId);
            user.setDepartmentName(departmentName);
            user.setToken("mock_token_" + userId);
            user.setTokenExpiresTime(LocalDateTime.now().plusHours(24));
            user.setEnabled(true);
            // 由于permissions字段暂时不存在，跳过权限设置
            // user.setPermissions(objectMapper.writeValueAsString(permissions));

            userRepository.save(user);
            log.info("创建测试用户: {} ({})", username, roleCode);
            
        } catch (Exception e) {
            log.error("创建测试用户失败: {}", username, e);
        }
    }
}