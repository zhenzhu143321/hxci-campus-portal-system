package cn.iocoder.yudao.module.system.dal.dataobject.mockschool;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * Mock School API 角色数据对象
 * 
 * @author 芋道源码
 */
@TableName("system_mock_school_role")
@KeySequence("system_mock_school_role_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolRoleDO extends TenantBaseDO {

    /**
     * 角色ID
     */
    @TableId
    private Long id;
    /**
     * 角色编码
     */
    private String roleCode;
    /**
     * 角色名称
     */
    private String roleName;
    /**
     * 角色层级：1-校级 2-学院级 3-部门级 4-班级 5-学生
     */
    private Integer roleLevel;
    /**
     * 角色描述
     */
    private String description;
    /**
     * 数据权限范围
     */
    private String scopeData;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态：1-正常 2-禁用
     */
    private Integer status;

}