package cn.iocoder.yudao.mock.school.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * 学校登录结果DTO - 与真实学校API格式完全一致
 * 
 * 真实API返回格式:
 * {
 *   "code": 0,
 *   "data": {
 *     "id": "23230129050231",
 *     "no": "2023010105", 
 *     "name": "顾春琳",
 *     "role": ["student"],
 *     "token": "557b76cd-ef17-4360-8a00-06a1b78b2656",
 *     "grade": "2023",
 *     "className": "软件23M01"
 *     // 其他字段...
 *   }
 * }
 * 
 * @author Auth-Integration-Expert
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolLoginResult {

    /**
     * 响应码 - 0表示成功
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String msg;
    
    /**
     * 响应数据 - 包含用户信息
     */
    private LoginData data;
    
    /**
     * 登录数据内部类 - 与真实API完全一致
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginData {
        /**
         * 用户ID - 学生为学号，教师为工号
         */
        private String id;
        
        /**
         * 公司ID
         */
        private String companyId;
        
        /**
         * 办公室ID
         */
        private String officeId;
        
        /**
         * 工号/学号
         */
        private String no;
        
        /**
         * 校区名称
         */
        private String schoolName;
        
        /**
         * 姓名
         */
        private String name;
        
        /**
         * 邮箱
         */
        private String email;
        
        /**
         * 电话
         */
        private String phone;
        
        /**
         * 手机
         */
        private String mobile;
        
        /**
         * 角色数组 - 必须是数组格式
         * 学生: ["student"]
         * 教师: ["teacher", "zaizhi", "listen_admin"]
         */
        private List<String> role;
        
        /**
         * 照片URL
         */
        private String photo;
        
        /**
         * Token - UUID格式
         */
        private String token;
        
        /**
         * 年级 - 学生有值，教师为null
         */
        private String grade;
        
        /**
         * 教师状态 - 教师有值，学生为null
         */
        private String teacherStatus;
        
        /**
         * 班级名称 - 学生有值，教师为null
         */
        private String className;
        
        // 额外字段用于内部处理（不在API返回中）
        @Builder.Default
        private transient String jwtToken = null;
        @Builder.Default
        private transient String authMode = "mock";
    }
}