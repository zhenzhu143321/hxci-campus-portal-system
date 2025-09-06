package cn.iocoder.yudao.mock.school.service;

import cn.iocoder.yudao.mock.school.dto.SchoolUserDTO;
import cn.iocoder.yudao.mock.school.dto.UserInfo;

/**
 * 用户信息映射服务接口
 * 负责数据结构转换，将学校API用户信息映射到系统UserInfo
 * 
 * 职责：
 * 1. 学校用户信息到系统用户信息的映射
 * 2. 角色转换（学校角色 → 系统角色）
 * 3. 权限矩阵保持（确保现有96种权限组合不变）
 * 4. 数据清洗和验证
 * 
 * 映射规则：
 * - userNumber → employeeId
 * - grade → gradeId
 * - className → classId  
 * - role → roleCode (需要映射转换)
 * 
 * 技术实现：
 * - 可使用MapStruct或ModelMapper库进行映射
 * - 手动映射确保数据准确性
 * - 完整的异常处理和数据验证
 * 
 * @author Backend-Developer (based on Gemini 2.5 Pro recommendations)
 */
public interface UserMappingService {

    /**
     * 将学校用户信息映射到系统用户信息
     * 核心映射方法，确保所有字段正确转换
     * 
     * @param schoolUser 学校API返回的用户信息
     * @return 系统标准的用户信息对象
     * @throws MappingException 映射过程中的异常
     */
    UserInfo mapToLocalUserInfo(SchoolUserDTO schoolUser) throws MappingException;

    /**
     * 学校角色到系统角色的映射
     * 将学校API返回的角色转换为系统识别的角色代码
     * 
     * @param schoolRoles 学校API返回的角色数组
     * @return 系统角色代码 (STUDENT/TEACHER/PRINCIPAL等)
     */
    String mapSchoolRoleToSystemRole(java.util.List<String> schoolRoles);

    /**
     * 提取年级信息
     * 从学校API的grade字段提取标准化年级ID
     * 
     * @param schoolGrade 学校API返回的年级信息
     * @return 标准化的年级ID (如"2023"、"2022")
     */
    String extractGradeId(String schoolGrade);

    /**
     * 提取班级信息
     * 从学校API的className字段提取标准化班级ID
     * 
     * @param schoolClassName 学校API返回的班级信息
     * @return 标准化的班级ID
     */
    String extractClassId(String schoolClassName);

    /**
     * 确定用户类型
     * 基于角色信息确定用户类型 (STUDENT/TEACHER/ADMIN)
     * 
     * @param roleCode 系统角色代码
     * @return 用户类型
     */
    String determineUserType(String roleCode);

    /**
     * 验证映射结果
     * 确保映射后的用户信息完整且符合系统要求
     * 
     * @param userInfo 映射后的用户信息
     * @return 验证结果，true表示通过验证
     */
    boolean validateMappedUserInfo(UserInfo userInfo);

    /**
     * 获取默认权限配置
     * 基于角色代码返回默认权限配置
     * 
     * @param roleCode 角色代码
     * @return 默认权限列表
     */
    java.util.List<String> getDefaultPermissionsByRole(String roleCode);

    /**
     * 映射异常类
     * 用于处理用户信息映射过程中的异常
     */
    class MappingException extends Exception {
        private final String sourceField;
        private final String targetField;

        public MappingException(String message) {
            super(message);
            this.sourceField = null;
            this.targetField = null;
        }

        public MappingException(String message, String sourceField, String targetField) {
            super(message);
            this.sourceField = sourceField;
            this.targetField = targetField;
        }

        public MappingException(String message, Throwable cause) {
            super(message, cause);
            this.sourceField = null;
            this.targetField = null;
        }

        public String getSourceField() {
            return sourceField;
        }

        public String getTargetField() {
            return targetField;
        }
    }
}