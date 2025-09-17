package cn.iocoder.yudao.server.dal.mysql.todo;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.server.dal.dataobject.todo.TodoCompletionDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户待办状态 Mapper 接口
 *
 * 核心设计原则：
 * 1. 所有查询和更新必须强制包含 tenant_id 和 user_id 条件
 * 2. 防止用户越权访问其他用户的状态数据
 * 3. 使用乐观锁（version）防止并发更新冲突
 * 4. 提供批量操作接口提升性能
 *
 * @author Claude AI
 * @since 2025-09-15
 */
@Mapper
public interface TodoCompletionMapper extends BaseMapperX<TodoCompletionDO> {

    // ==================== 查询方法 ====================

    /**
     * 查询用户的待办状态列表（强制隔离）
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param status 状态过滤（可选）
     * @param limit 查询数量限制
     * @param offset 查询偏移量
     * @return 用户的待办状态列表
     */
    @Select({
        "<script>",
        "SELECT * FROM todo_completions",
        "WHERE tenant_id = #{tenantId}",
        "  AND user_id = #{userId}",
        "  AND deleted = 0",
        "<if test='status != null'>",
        "  AND status = #{status}",
        "</if>",
        "ORDER BY create_time DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<TodoCompletionDO> selectUserTodoList(@Param("tenantId") Long tenantId,
                                              @Param("userId") String userId,
                                              @Param("status") Integer status,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    /**
     * 查询用户的特定待办状态（强制隔离）
     *
     * @param todoId 待办ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 待办状态记录
     */
    @Select("""
        SELECT * FROM todo_completions
        WHERE todo_id = #{todoId}
          AND tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND deleted = 0
        LIMIT 1
    """)
    TodoCompletionDO selectByTodoAndUser(@Param("todoId") Long todoId,
                                         @Param("tenantId") Long tenantId,
                                         @Param("userId") String userId);

    /**
     * 批量查询用户的待办状态
     *
     * @param todoIds 待办ID列表
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 待办状态列表
     */
    @Select({
        "<script>",
        "SELECT * FROM todo_completions",
        "WHERE tenant_id = #{tenantId}",
        "  AND user_id = #{userId}",
        "  AND todo_id IN",
        "<foreach collection='todoIds' item='id' open='(' separator=',' close=')'>",
        "  #{id}",
        "</foreach>",
        "  AND deleted = 0",
        "</script>"
    })
    List<TodoCompletionDO> selectBatchByTodoIds(@Param("todoIds") List<Long> todoIds,
                                                @Param("tenantId") Long tenantId,
                                                @Param("userId") String userId);

    /**
     * 统计用户各状态的待办数量
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 各状态数量统计
     */
    @Select("""
        SELECT
            status,
            COUNT(*) as count
        FROM todo_completions
        WHERE tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND deleted = 0
        GROUP BY status
    """)
    @Results({
        @Result(property = "status", column = "status"),
        @Result(property = "count", column = "count")
    })
    List<StatusCountVO> selectStatusCount(@Param("tenantId") Long tenantId,
                                          @Param("userId") String userId);

    // ==================== 更新方法 ====================

    /**
     * 标记为已读（强制隔离 + 乐观锁）
     *
     * @param id 主键ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param version 版本号（乐观锁）
     * @return 更新的行数
     */
    @Update("""
        UPDATE todo_completions
        SET status = 1,
            read_at = NOW(),
            update_time = NOW(),
            version = version + 1
        WHERE id = #{id}
          AND tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND version = #{version}
          AND deleted = 0
          AND status = 0
    """)
    int markAsRead(@Param("id") Long id,
                   @Param("tenantId") Long tenantId,
                   @Param("userId") String userId,
                   @Param("version") Integer version);

    /**
     * 标记为已完成（强制隔离 + 乐观锁）
     *
     * @param id 主键ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param version 版本号（乐观锁）
     * @return 更新的行数
     */
    @Update("""
        UPDATE todo_completions
        SET status = 2,
            completed_time = NOW(),
            read_at = CASE WHEN read_at IS NULL THEN NOW() ELSE read_at END,
            update_time = NOW(),
            version = version + 1
        WHERE id = #{id}
          AND tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND version = #{version}
          AND deleted = 0
    """)
    int markAsCompleted(@Param("id") Long id,
                        @Param("tenantId") Long tenantId,
                        @Param("userId") String userId,
                        @Param("version") Integer version);

    /**
     * 标记为隐藏（强制隔离 + 乐观锁）
     *
     * @param id 主键ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param version 版本号（乐观锁）
     * @return 更新的行数
     */
    @Update("""
        UPDATE todo_completions
        SET status = 3,
            hidden_at = NOW(),
            update_time = NOW(),
            version = version + 1
        WHERE id = #{id}
          AND tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND version = #{version}
          AND deleted = 0
    """)
    int markAsHidden(@Param("id") Long id,
                     @Param("tenantId") Long tenantId,
                     @Param("userId") String userId,
                     @Param("version") Integer version);

    /**
     * 取消完成状态（恢复为未读）
     *
     * @param id 主键ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param version 版本号（乐观锁）
     * @return 更新的行数
     */
    @Update("""
        UPDATE todo_completions
        SET status = 0,
            completed_time = NULL,
            read_at = NULL,
            hidden_at = NULL,
            update_time = NOW(),
            version = version + 1
        WHERE id = #{id}
          AND tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND version = #{version}
          AND deleted = 0
    """)
    int resetStatus(@Param("id") Long id,
                   @Param("tenantId") Long tenantId,
                   @Param("userId") String userId,
                   @Param("version") Integer version);

    /**
     * 批量标记为已读
     *
     * @param todoIds 待办ID列表
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 更新的行数
     */
    @Update({
        "<script>",
        "UPDATE todo_completions",
        "SET status = 1,",
        "    read_at = NOW(),",
        "    update_time = NOW(),",
        "    version = version + 1",
        "WHERE tenant_id = #{tenantId}",
        "  AND user_id = #{userId}",
        "  AND todo_id IN",
        "<foreach collection='todoIds' item='id' open='(' separator=',' close=')'>",
        "  #{id}",
        "</foreach>",
        "  AND deleted = 0",
        "  AND status = 0",
        "</script>"
    })
    int batchMarkAsRead(@Param("todoIds") List<Long> todoIds,
                       @Param("tenantId") Long tenantId,
                       @Param("userId") String userId);

    // ==================== 插入方法 ====================

    /**
     * 插入或更新用户待办状态（用于初始化记录）
     *
     * @param todoId 待办ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param userRole 用户角色
     * @return 影响的行数
     */
    @Insert("""
        INSERT INTO todo_completions
        (todo_id, user_id, user_name, user_role, status, tenant_id, version, create_time)
        VALUES
        (#{todoId}, #{userId}, #{userName}, #{userRole}, 0, #{tenantId}, 0, NOW())
        ON DUPLICATE KEY UPDATE
        update_time = NOW()
    """)
    int insertOrUpdateStatus(@Param("todoId") Long todoId,
                            @Param("tenantId") Long tenantId,
                            @Param("userId") String userId,
                            @Param("userName") String userName,
                            @Param("userRole") String userRole);

    // ==================== 辅助VO类 ====================

    /**
     * 状态统计VO
     */
    class StatusCountVO {
        private Integer status;
        private Long count;

        // Getters and Setters
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}