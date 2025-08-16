package cn.iocoder.yudao.mock.school.repository;

import cn.iocoder.yudao.mock.school.entity.MockSchoolUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mock School User Repository
 * 
 * @author Claude
 */
@Repository
public interface MockSchoolUserRepository extends JpaRepository<MockSchoolUser, Long> {

    /**
     * 根据token查找用户
     */
    Optional<MockSchoolUser> findByToken(String token);

    /**
     * 根据用户ID查找用户
     */
    Optional<MockSchoolUser> findByUserId(String userId);

    /**
     * 根据用户名查找用户
     */
    Optional<MockSchoolUser> findByUsername(String username);

    /**
     * 根据工号/学号和姓名查找用户（支持工号+姓名登录）
     */
    Optional<MockSchoolUser> findByUserIdAndUsername(String userId, String username);

    /**
     * 根据角色编码查找用户
     */
    List<MockSchoolUser> findByRoleCode(String roleCode);

    /**
     * 根据部门ID查找用户
     */
    List<MockSchoolUser> findByDepartmentId(Long departmentId);

    /**
     * 根据角色编码和部门ID查找用户
     */
    List<MockSchoolUser> findByRoleCodeAndDepartmentId(String roleCode, Long departmentId);

    /**
     * 查找启用的用户
     */
    List<MockSchoolUser> findByEnabled(Boolean enabled);

    /**
     * 根据角色编码查找启用的用户
     */
    List<MockSchoolUser> findByRoleCodeAndEnabled(String roleCode, Boolean enabled);

    /**
     * 检查token是否存在且未过期
     */
    @Query("SELECT u FROM MockSchoolUser u WHERE u.token = :token AND u.tokenExpiresTime > CURRENT_TIMESTAMP AND u.enabled = true")
    Optional<MockSchoolUser> findValidTokenUser(@Param("token") String token);

    /**
     * 统计指定角色的用户数量
     */
    long countByRoleCode(String roleCode);

    /**
     * 统计指定部门的用户数量
     */
    long countByDepartmentId(Long departmentId);
}