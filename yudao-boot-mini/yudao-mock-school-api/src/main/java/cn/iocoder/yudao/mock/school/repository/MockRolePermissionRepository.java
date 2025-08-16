package cn.iocoder.yudao.mock.school.repository;

import cn.iocoder.yudao.mock.school.entity.MockRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mock Role Permission Repository
 * 
 * @author Claude
 */
@Repository
public interface MockRolePermissionRepository extends JpaRepository<MockRolePermission, Long> {

    /**
     * 根据角色编码查找权限
     */
    List<MockRolePermission> findByRoleCode(String roleCode);

    /**
     * 根据权限编码查找角色
     */
    List<MockRolePermission> findByPermissionCode(String permissionCode);

    /**
     * 根据角色和权限编码查找
     */
    MockRolePermission findByRoleCodeAndPermissionCode(String roleCode, String permissionCode);

    /**
     * 查找支持指定通知级别的角色权限
     */
    @Query("SELECT rp FROM MockRolePermission rp WHERE rp.roleCode = :roleCode AND rp.notificationLevels LIKE %:level%")
    List<MockRolePermission> findByRoleCodeAndNotificationLevel(@Param("roleCode") String roleCode, @Param("level") String level);

    /**
     * 查找指定目标范围的角色权限
     */
    @Query("SELECT rp FROM MockRolePermission rp WHERE rp.roleCode = :roleCode AND (rp.targetScope = 'ALL_SCHOOL' OR rp.targetScope = :scope)")
    List<MockRolePermission> findByRoleCodeAndTargetScope(@Param("roleCode") String roleCode, @Param("scope") String scope);

    /**
     * 查找需要审批的权限
     */
    List<MockRolePermission> findByRoleCodeAndApprovalRequired(String roleCode, Boolean approvalRequired);

    /**
     * 查找所有不同的角色编码
     */
    @Query("SELECT DISTINCT rp.roleCode FROM MockRolePermission rp")
    List<String> findDistinctRoleCodes();

    /**
     * 查找所有不同的权限编码
     */
    @Query("SELECT DISTINCT rp.permissionCode FROM MockRolePermission rp")
    List<String> findDistinctPermissionCodes();
}