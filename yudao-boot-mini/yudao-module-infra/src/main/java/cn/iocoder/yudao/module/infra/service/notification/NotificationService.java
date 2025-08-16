package cn.iocoder.yudao.module.infra.service.notification;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationListReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.notification.NotificationDO;

import jakarta.validation.Valid;
import java.util.List;

public interface NotificationService {

    Long createNotification(@Valid NotificationSaveReqVO createReqVO);

    void updateNotification(@Valid NotificationSaveReqVO updateReqVO);

    void deleteNotification(Long id);

    NotificationDO getNotification(Long id);

    PageResult<NotificationDO> getNotificationPage(NotificationPageReqVO pageReqVO);

    List<NotificationDO> getNotificationList(NotificationListReqVO listReqVO);

    // ==================== 业务功能 ====================

    void submitForApproval(Long id);

    void approve(Long id);

    void reject(Long id, String reason);

    void publish(Long id);

    void pushImmediately(Long id);

    // ==================== 角色过滤功能 ====================

    PageResult<NotificationDO> getNotificationsByRole(NotificationPageReqVO pageReqVO, String userRole, String userDept);
}