package cn.iocoder.yudao.module.system.dal.mysql.mockschool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.mockschool.SchoolUserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mock School API 用户 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface SchoolUserMapper extends BaseMapperX<SchoolUserDO> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    default SchoolUserDO selectByUsername(String username) {
        return selectOne(SchoolUserDO::getUsername, username);
    }

    /**
     * 根据用户名和学校编码查询用户
     *
     * @param username 用户名
     * @param schoolCode 学校编码
     * @return 用户信息
     */
    default SchoolUserDO selectByUsernameAndSchoolCode(String username, String schoolCode) {
        return selectOne("username", username, "school_code", schoolCode);
    }

    /**
     * 根据学号查询学生用户
     *
     * @param studentId 学号
     * @return 用户信息
     */
    default SchoolUserDO selectByStudentId(String studentId) {
        return selectOne(SchoolUserDO::getStudentId, studentId);
    }

    /**
     * 根据工号查询教职工用户
     *
     * @param teacherId 工号
     * @return 用户信息
     */
    default SchoolUserDO selectByTeacherId(String teacherId) {
        return selectOne(SchoolUserDO::getTeacherId, teacherId);
    }

}