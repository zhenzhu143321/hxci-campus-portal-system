package cn.iocoder.yudao.mock.school.service.impl;

import cn.iocoder.yudao.mock.school.dto.SchoolUserDTO;
import cn.iocoder.yudao.mock.school.dto.UserInfo;
import cn.iocoder.yudao.mock.school.service.UserMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户信息映射服务实现类
 * 将学校API用户信息映射到系统UserInfo，保持权限矩阵不变
 * 
 * @author Backend-Developer (based on Gemini 2.5 Pro recommendations)
 */
@Service
public class UserMappingServiceImpl implements UserMappingService {

    private static final Logger log = LoggerFactory.getLogger(UserMappingServiceImpl.class);

    // 学校角色到系统角色的映射配置
    private static final String[][] ROLE_MAPPING = {
        {"校长", "PRINCIPAL"},
        {"principal", "PRINCIPAL"},
        {"教务主任", "ACADEMIC_ADMIN"},
        {"academic_admin", "ACADEMIC_ADMIN"},
        {"教师", "TEACHER"},
        {"teacher", "TEACHER"},
        {"班主任", "CLASS_TEACHER"},
        {"class_teacher", "CLASS_TEACHER"},
        {"学生", "STUDENT"},
        {"student", "STUDENT"},
        {"系统管理员", "SYSTEM_ADMIN"},
        {"system_admin", "SYSTEM_ADMIN"}
    };

    @Override
    public UserInfo mapToLocalUserInfo(SchoolUserDTO schoolUser) throws MappingException {
        log.info("🔄 [USER_MAPPING] 开始映射学校用户信息: userNumber={}", schoolUser.getUserNumber());
        
        try {
            // 验证输入参数
            if (schoolUser == null) {
                throw new MappingException("学校用户信息为空");
            }
            
            if (schoolUser.getUserNumber() == null || schoolUser.getUserNumber().trim().isEmpty()) {
                throw new MappingException("用户工号/学号为空", "userNumber", "employeeId");
            }
            
            // 创建系统用户信息对象
            UserInfo userInfo = new UserInfo();
            
            // 🎯 核心字段映射
            userInfo.setUserId(schoolUser.getUserNumber());
            userInfo.setEmployeeId(schoolUser.getUserNumber());
            userInfo.setUsername(schoolUser.getRealName() != null ? schoolUser.getRealName() : schoolUser.getUserNumber());
            userInfo.setRealName(schoolUser.getRealName());
            
            // 🔄 角色映射
            String systemRole = mapSchoolRoleToSystemRole(schoolUser.getRole());
            userInfo.setRoleCode(systemRole);
            userInfo.setRoleName(getSystemRoleName(systemRole));
            
            // 🎯 年级班级信息映射
            userInfo.setGradeId(extractGradeId(schoolUser.getGrade()));
            userInfo.setClassId(extractClassId(schoolUser.getClassName()));
            
            // 👥 用户类型确定
            userInfo.setUserType(determineUserType(systemRole));
            
            // 🏢 部门信息映射
            userInfo.setDepartmentName(schoolUser.getDepartment());
            userInfo.setDepartmentId(mapDepartmentNameToId(schoolUser.getDepartment()));
            if (userInfo.getDepartmentId() != null) {
                userInfo.setDepartmentIdStr(userInfo.getDepartmentId().toString());
            }
            
            // 📧 联系方式映射
            // email和phone字段暂时不设置，避免敏感信息泄露
            
            // ✅ 学生特殊处理
            if ("STUDENT".equals(systemRole)) {
                userInfo.setStudentId(schoolUser.getUserNumber());
            }
            
            // 🔧 系统默认配置
            userInfo.setEnabled(true); // 从学校API验证通过的用户默认为启用状态
            
            log.info("✅ [USER_MAPPING] 用户信息映射成功: employeeId={}, role={}, grade={}, class={}", 
                    userInfo.getEmployeeId(), userInfo.getRoleCode(), userInfo.getGradeId(), userInfo.getClassId());
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("❌ [USER_MAPPING] 用户信息映射异常", e);
            throw new MappingException("用户信息映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String mapSchoolRoleToSystemRole(List<String> schoolRoles) {
        log.info("🔄 [ROLE_MAPPING] 映射学校角色: {}", schoolRoles);
        
        if (schoolRoles == null || schoolRoles.isEmpty()) {
            log.warn("⚠️ [ROLE_MAPPING] 学校角色为空，返回默认角色STUDENT");
            return "STUDENT";
        }
        
        // 遍历学校角色，找到第一个匹配的系统角色
        for (String schoolRole : schoolRoles) {
            if (schoolRole == null) continue;
            
            String normalizedSchoolRole = schoolRole.toLowerCase().trim();
            
            // 查找角色映射
            for (String[] mapping : ROLE_MAPPING) {
                String schoolRolePattern = mapping[0].toLowerCase();
                String systemRole = mapping[1];
                
                if (normalizedSchoolRole.contains(schoolRolePattern) || 
                    schoolRolePattern.contains(normalizedSchoolRole)) {
                    log.info("✅ [ROLE_MAPPING] 角色映射成功: {} -> {}", schoolRole, systemRole);
                    return systemRole;
                }
            }
        }
        
        // 如果没有找到匹配的角色，根据学号特征判断
        String firstRole = schoolRoles.get(0);
        if (firstRole != null && firstRole.matches("\\d{10}")) {
            // 10位数字通常是学号
            log.info("🎯 [ROLE_MAPPING] 根据学号特征推断为学生: {}", firstRole);
            return "STUDENT";
        }
        
        // 默认返回学生角色
        log.warn("⚠️ [ROLE_MAPPING] 未找到匹配角色，返回默认角色STUDENT: {}", schoolRoles);
        return "STUDENT";
    }

    @Override
    public String extractGradeId(String schoolGrade) {
        if (schoolGrade == null || schoolGrade.trim().isEmpty()) {
            return null;
        }
        
        // 提取年份信息（如"2023级"、"2023年"、"2023"）
        Pattern gradePattern = Pattern.compile("(20\\d{2})");
        Matcher matcher = gradePattern.matcher(schoolGrade);
        
        if (matcher.find()) {
            String gradeId = matcher.group(1);
            log.info("✅ [GRADE_EXTRACTION] 提取年级ID: {} -> {}", schoolGrade, gradeId);
            return gradeId;
        }
        
        log.warn("⚠️ [GRADE_EXTRACTION] 无法提取年级ID: {}", schoolGrade);
        return schoolGrade; // 如果无法提取，返回原始值
    }

    @Override
    public String extractClassId(String schoolClassName) {
        if (schoolClassName == null || schoolClassName.trim().isEmpty()) {
            return null;
        }
        
        // 提取班级信息（如"计算机科学与技术1班" -> "1"）
        Pattern classPattern = Pattern.compile("(\\d+)班");
        Matcher matcher = classPattern.matcher(schoolClassName);
        
        if (matcher.find()) {
            String classId = matcher.group(1);
            log.info("✅ [CLASS_EXTRACTION] 提取班级ID: {} -> {}", schoolClassName, classId);
            return classId;
        }
        
        // 如果包含"班"字但无法提取数字，尝试其他模式
        if (schoolClassName.contains("班")) {
            // 简化处理，返回班级名称的hash值的后两位作为ID
            String classId = String.valueOf(Math.abs(schoolClassName.hashCode() % 100));
            log.info("🔧 [CLASS_EXTRACTION] 使用hash提取班级ID: {} -> {}", schoolClassName, classId);
            return classId;
        }
        
        log.warn("⚠️ [CLASS_EXTRACTION] 无法提取班级ID: {}", schoolClassName);
        return schoolClassName; // 如果无法提取，返回原始值
    }

    @Override
    public String determineUserType(String roleCode) {
        if (roleCode == null) {
            return "OTHER";
        }
        
        switch (roleCode) {
            case "STUDENT":
                return "STUDENT";
            case "TEACHER":
            case "CLASS_TEACHER":
                return "TEACHER";
            case "PRINCIPAL":
            case "ACADEMIC_ADMIN":
            case "SYSTEM_ADMIN":
                return "ADMIN";
            default:
                return "OTHER";
        }
    }

    @Override
    public boolean validateMappedUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            log.error("❌ [VALIDATION] 用户信息为空");
            return false;
        }
        
        // 检查必需字段
        if (userInfo.getEmployeeId() == null || userInfo.getEmployeeId().trim().isEmpty()) {
            log.error("❌ [VALIDATION] 工号/学号为空");
            return false;
        }
        
        if (userInfo.getRoleCode() == null || userInfo.getRoleCode().trim().isEmpty()) {
            log.error("❌ [VALIDATION] 角色代码为空");
            return false;
        }
        
        // 检查角色代码是否有效
        List<String> validRoles = Arrays.asList("STUDENT", "TEACHER", "CLASS_TEACHER", "PRINCIPAL", "ACADEMIC_ADMIN", "SYSTEM_ADMIN");
        if (!validRoles.contains(userInfo.getRoleCode())) {
            log.error("❌ [VALIDATION] 无效的角色代码: {}", userInfo.getRoleCode());
            return false;
        }
        
        // 学生用户特殊验证
        if ("STUDENT".equals(userInfo.getRoleCode())) {
            if (userInfo.getStudentId() == null || userInfo.getStudentId().trim().isEmpty()) {
                log.error("❌ [VALIDATION] 学生用户缺少学号");
                return false;
            }
        }
        
        log.info("✅ [VALIDATION] 用户信息验证通过: employeeId={}, role={}", 
                userInfo.getEmployeeId(), userInfo.getRoleCode());
        return true;
    }

    @Override
    public List<String> getDefaultPermissionsByRole(String roleCode) {
        // 返回基础权限列表，具体权限由主服务管理
        switch (roleCode) {
            case "SYSTEM_ADMIN":
                return Arrays.asList("NOTIFICATION_READ", "NOTIFICATION_WRITE", "NOTIFICATION_DELETE", "ADMIN_MANAGE");
            case "PRINCIPAL":
                return Arrays.asList("NOTIFICATION_READ", "NOTIFICATION_WRITE", "NOTIFICATION_DELETE");
            case "ACADEMIC_ADMIN":
                return Arrays.asList("NOTIFICATION_READ", "NOTIFICATION_WRITE");
            case "TEACHER":
            case "CLASS_TEACHER":
                return Arrays.asList("NOTIFICATION_READ", "NOTIFICATION_WRITE");
            case "STUDENT":
                return Arrays.asList("NOTIFICATION_READ");
            default:
                return Arrays.asList("NOTIFICATION_READ");
        }
    }

    /**
     * 获取系统角色名称
     */
    private String getSystemRoleName(String roleCode) {
        switch (roleCode) {
            case "SYSTEM_ADMIN":
                return "系统管理员";
            case "PRINCIPAL":
                return "校长";
            case "ACADEMIC_ADMIN":
                return "教务主任";
            case "TEACHER":
                return "教师";
            case "CLASS_TEACHER":
                return "班主任";
            case "STUDENT":
                return "学生";
            default:
                return "其他";
        }
    }

    /**
     * 部门名称到部门ID的映射
     * 这里使用简单的hash映射，实际项目中应该查询部门表
     */
    private Long mapDepartmentNameToId(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            return 1L; // 默认部门ID
        }
        
        // 简单的hash映射，确保相同部门名称总是映射到相同ID
        long hash = Math.abs(departmentName.hashCode()) % 10000;
        return hash + 1000L; // 确保ID在1000以上
    }
}