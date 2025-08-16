package cn.iocoder.yudao.module.system.dal.mysql.mockschool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.mockschool.SchoolUserRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * Mock School API 用户角色关联 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface SchoolUserRoleMapper extends BaseMapperX<SchoolUserRoleDO> {

    /**
     * 根据用户ID查询角色关联
     *
     * @param userId 用户ID
     * @return 用户角色关联列表
     */
    default List<SchoolUserRoleDO> selectListByUserId(Long userId) {
        return selectList(SchoolUserRoleDO::getUserId, userId);
    }

    /**
     * 根据用户ID列表查询角色关联
     *
     * @param userIds 用户ID列表
     * @return 用户角色关联列表
     */
    default List<SchoolUserRoleDO> selectListByUserIds(Collection<Long> userIds) {
        return selectList(SchoolUserRoleDO::getUserId, userIds);
    }

    /**
     * 根据角色ID查询用户关联
     *
     * @param roleId 角色ID
     * @return 用户角色关联列表
     */
    default List<SchoolUserRoleDO> selectListByRoleId(Long roleId) {
        return selectList(SchoolUserRoleDO::getRoleId, roleId);
    }

    /**
     * 根据用户ID删除关联
     *
     * @param userId 用户ID
     */
    default void deleteByUserId(Long userId) {
        delete(SchoolUserRoleDO::getUserId, userId);
    }

    /**
     * 根据角色ID删除关联
     *
     * @param roleId 角色ID
     */
    default void deleteByRoleId(Long roleId) {
        delete(SchoolUserRoleDO::getRoleId, roleId);
    }

}