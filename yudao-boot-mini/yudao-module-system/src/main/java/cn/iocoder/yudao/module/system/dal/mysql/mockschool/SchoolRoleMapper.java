package cn.iocoder.yudao.module.system.dal.mysql.mockschool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.mockschool.SchoolRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Mock School API 角色 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface SchoolRoleMapper extends BaseMapperX<SchoolRoleDO> {

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    default SchoolRoleDO selectByRoleCode(String roleCode) {
        return selectOne(SchoolRoleDO::getRoleCode, roleCode);
    }

    /**
     * 根据角色层级查询角色列表
     *
     * @param roleLevel 角色层级
     * @return 角色列表
     */
    default List<SchoolRoleDO> selectListByRoleLevel(Integer roleLevel) {
        return selectList(SchoolRoleDO::getRoleLevel, roleLevel);
    }

}