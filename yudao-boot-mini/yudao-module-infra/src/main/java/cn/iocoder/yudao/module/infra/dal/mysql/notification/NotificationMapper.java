package cn.iocoder.yudao.module.infra.dal.mysql.notification;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationListReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.notification.NotificationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapperX<NotificationDO> {

    default PageResult<NotificationDO> selectPage(NotificationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<NotificationDO>()
                .likeIfPresent(NotificationDO::getTitle, reqVO.getTitle())
                .eqIfPresent(NotificationDO::getLevel, reqVO.getLevel())
                .eqIfPresent(NotificationDO::getStatus, reqVO.getStatus())
                .eqIfPresent(NotificationDO::getCategoryId, reqVO.getCategoryId())
                .likeIfPresent(NotificationDO::getPublisherName, reqVO.getPublisherName())
                .eqIfPresent(NotificationDO::getPublisherRole, reqVO.getPublisherRole())
                .betweenIfPresent(NotificationDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .eqIfPresent(NotificationDO::getPinned, reqVO.getPinned())
                .eqIfPresent(NotificationDO::getRequireConfirm, reqVO.getRequireConfirm())
                .orderByDesc(NotificationDO::getId));
    }

    default List<NotificationDO> selectList(NotificationListReqVO reqVO) {
        LambdaQueryWrapperX<NotificationDO> queryWrapper = new LambdaQueryWrapperX<NotificationDO>()
                .likeIfPresent(NotificationDO::getTitle, reqVO.getTitle())
                .eqIfPresent(NotificationDO::getLevel, reqVO.getLevel())
                .eqIfPresent(NotificationDO::getStatus, reqVO.getStatus())
                .eqIfPresent(NotificationDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(NotificationDO::getPublisherRole, reqVO.getPublisherRole())
                .betweenIfPresent(NotificationDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .eqIfPresent(NotificationDO::getPinned, reqVO.getPinned())
                .orderByDesc(NotificationDO::getId);
        
        // 如果设置了限制数量，则应用限制
        if (reqVO.getLimit() != null && reqVO.getLimit() > 0) {
            queryWrapper.last("LIMIT " + reqVO.getLimit());
        }
        
        return selectList(queryWrapper);
    }

    /**
     * 根据角色和权限查询通知列表
     */
    default List<NotificationDO> selectListByRole(String userRole, String userDept, Integer status) {
        return selectList(new LambdaQueryWrapperX<NotificationDO>()
                .eqIfPresent(NotificationDO::getStatus, status)
                // 根据业务逻辑，这里可以添加更多的角色过滤条件
                .orderByAsc(NotificationDO::getLevel) // 按优先级排序
                .orderByDesc(NotificationDO::getPinned) // 置顶优先
                .orderByDesc(NotificationDO::getCreateTime)); // 时间倒序
    }

    /**
     * 统计各种状态的通知数量
     */
    default long countByStatus(Integer status) {
        return selectCount(new LambdaQueryWrapperX<NotificationDO>()
                .eq(NotificationDO::getStatus, status));
    }

    /**
     * 统计各种级别的通知数量
     */
    default long countByLevel(Integer level) {
        return selectCount(new LambdaQueryWrapperX<NotificationDO>()
                .eq(NotificationDO::getLevel, level));
    }

    /**
     * 查询即将过期的通知
     */
    default List<NotificationDO> selectExpiringNotifications(int hours) {
        return selectList(new LambdaQueryWrapperX<NotificationDO>()
                .eq(NotificationDO::getStatus, 3) // 已发布状态
                .isNotNull(NotificationDO::getExpiredTime)
                .apply("expired_time <= DATE_ADD(NOW(), INTERVAL " + hours + " HOUR)")
                .apply("expired_time > NOW()"));
    }
}