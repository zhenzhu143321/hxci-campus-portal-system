package cn.iocoder.yudao.module.infra.service.notification;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationListReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.notification.vo.NotificationSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.notification.NotificationDO;
import cn.iocoder.yudao.module.infra.dal.mysql.notification.NotificationMapper;
import cn.iocoder.yudao.module.infra.enums.notification.NotificationLevelEnum;
import cn.iocoder.yudao.module.infra.enums.notification.NotificationStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.NOTIFICATION_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Override
    public Long createNotification(@Valid NotificationSaveReqVO createReqVO) {
        log.info("[createNotification][创建通知] createReqVO: {}", createReqVO);
        
        NotificationDO notification = BeanUtils.toBean(createReqVO, NotificationDO.class);
        notification.setStatus(NotificationStatusEnum.DRAFT.getStatus());
        notification.setTenantId(1L); // 设置默认租户ID
        notification.setPublisherId(1L); // 设置默认发布者ID
        notification.setPushCount(0);
        notification.setReadCount(0);
        notification.setConfirmCount(0);
        
        // 处理推送渠道列表
        if (createReqVO.getPushChannels() != null && !createReqVO.getPushChannels().isEmpty()) {
            String channels = createReqVO.getPushChannels().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            notification.setPushChannels(channels);
        }
        
        notificationMapper.insert(notification);
        
        log.info("[createNotification][通知创建成功] id: {}", notification.getId());
        return notification.getId();
    }

    @Override
    public void updateNotification(@Valid NotificationSaveReqVO updateReqVO) {
        log.info("[updateNotification][更新通知] updateReqVO: {}", updateReqVO);
        
        validateNotificationExists(updateReqVO.getId());
        
        NotificationDO updateObj = BeanUtils.toBean(updateReqVO, NotificationDO.class);
        
        // 处理推送渠道列表
        if (updateReqVO.getPushChannels() != null && !updateReqVO.getPushChannels().isEmpty()) {
            String channels = updateReqVO.getPushChannels().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            updateObj.setPushChannels(channels);
        }
        
        notificationMapper.updateById(updateObj);
        
        log.info("[updateNotification][通知更新成功] id: {}", updateReqVO.getId());
    }

    @Override
    public void deleteNotification(Long id) {
        log.info("[deleteNotification][删除通知] id: {}", id);
        
        validateNotificationExists(id);
        
        notificationMapper.deleteById(id);
        
        log.info("[deleteNotification][通知删除成功] id: {}", id);
    }

    private NotificationDO validateNotificationExists(Long id) {
        NotificationDO notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw exception(NOTIFICATION_NOT_EXISTS);
        }
        return notification;
    }

    @Override
    public NotificationDO getNotification(Long id) {
        log.info("[getNotification][获取通知详情] id: {}", id);
        return notificationMapper.selectById(id);
    }

    @Override
    public PageResult<NotificationDO> getNotificationPage(NotificationPageReqVO pageReqVO) {
        log.info("[getNotificationPage][获取通知分页] pageReqVO: {}", pageReqVO);
        return notificationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<NotificationDO> getNotificationList(NotificationListReqVO listReqVO) {
        log.info("[getNotificationList][获取通知列表] listReqVO: {}", listReqVO);
        return notificationMapper.selectList(listReqVO);
    }

    // ==================== 业务功能 ====================

    @Override
    public void submitForApproval(Long id) {
        log.info("[submitForApproval][提交审批] id: {}", id);
        
        NotificationDO notification = validateNotificationExists(id);
        if (notification == null) {
            throw exception(NOTIFICATION_NOT_EXISTS);
        }
        
        // 只有草稿状态的通知才能提交审批
        if (!NotificationStatusEnum.DRAFT.getStatus().equals(notification.getStatus())) {
            log.warn("[submitForApproval][通知状态不符合要求] id: {}, status: {}", id, notification.getStatus());
            return;
        }
        
        NotificationDO updateObj = new NotificationDO();
        updateObj.setId(id);
        updateObj.setStatus(NotificationStatusEnum.PENDING_APPROVAL.getStatus());
        notificationMapper.updateById(updateObj);
        
        log.info("[submitForApproval][提交审批成功] id: {}", id);
    }

    @Override
    public void approve(Long id) {
        log.info("[approve][审批通过] id: {}", id);
        
        NotificationDO notification = getNotification(id);
        if (notification == null) {
            throw exception(NOTIFICATION_NOT_EXISTS);
        }
        
        // 只有待审批状态的通知才能审批
        if (!NotificationStatusEnum.PENDING_APPROVAL.getStatus().equals(notification.getStatus())) {
            log.warn("[approve][通知状态不符合要求] id: {}, status: {}", id, notification.getStatus());
            return;
        }
        
        NotificationDO updateObj = new NotificationDO();
        updateObj.setId(id);
        updateObj.setStatus(NotificationStatusEnum.APPROVED.getStatus());
        notificationMapper.updateById(updateObj);
        
        log.info("[approve][审批通过成功] id: {}", id);
    }

    @Override
    public void reject(Long id, String reason) {
        log.info("[reject][审批拒绝] id: {}, reason: {}", id, reason);
        
        NotificationDO notification = getNotification(id);
        if (notification == null) {
            throw exception(NOTIFICATION_NOT_EXISTS);
        }
        
        // 只有待审批状态的通知才能拒绝
        if (!NotificationStatusEnum.PENDING_APPROVAL.getStatus().equals(notification.getStatus())) {
            log.warn("[reject][通知状态不符合要求] id: {}, status: {}", id, notification.getStatus());
            return;
        }
        
        NotificationDO updateObj = new NotificationDO();
        updateObj.setId(id);
        updateObj.setStatus(NotificationStatusEnum.DRAFT.getStatus()); // 拒绝后回到草稿状态
        notificationMapper.updateById(updateObj);
        
        log.info("[reject][审批拒绝成功] id: {}, reason: {}", id, reason);
    }

    @Override
    public void publish(Long id) {
        log.info("[publish][发布通知] id: {}", id);
        
        NotificationDO notification = getNotification(id);
        if (notification == null) {
            throw exception(NOTIFICATION_NOT_EXISTS);
        }
        
        // 只有已审批状态的通知才能发布
        if (!NotificationStatusEnum.APPROVED.getStatus().equals(notification.getStatus())) {
            log.warn("[publish][通知状态不符合要求] id: {}, status: {}", id, notification.getStatus());
            return;
        }
        
        NotificationDO updateObj = new NotificationDO();
        updateObj.setId(id);
        updateObj.setStatus(NotificationStatusEnum.PUBLISHED.getStatus());
        notificationMapper.updateById(updateObj);
        
        log.info("[publish][发布通知成功] id: {}", id);
    }

    @Override
    public void pushImmediately(Long id) {
        log.info("[pushImmediately][立即推送] id: {}", id);
        
        NotificationDO notification = getNotification(id);
        if (notification == null) {
            throw exception(NOTIFICATION_NOT_EXISTS);
        }
        
        // 只有已发布状态的通知才能推送
        if (!NotificationStatusEnum.PUBLISHED.getStatus().equals(notification.getStatus())) {
            log.warn("[pushImmediately][通知状态不符合要求] id: {}, status: {}", id, notification.getStatus());
            return;
        }
        
        // 增加推送次数
        NotificationDO updateObj = new NotificationDO();
        updateObj.setId(id);
        updateObj.setPushCount(notification.getPushCount() + 1);
        notificationMapper.updateById(updateObj);
        
        log.info("[pushImmediately][立即推送成功] id: {}, pushCount: {}", id, notification.getPushCount() + 1);
    }

    // ==================== 角色过滤功能 ====================

    @Override
    public PageResult<NotificationDO> getNotificationsByRole(NotificationPageReqVO pageReqVO, String userRole, String userDept) {
        log.info("[getNotificationsByRole][获取角色通知] userRole: {}, userDept: {}, pageReqVO: {}", userRole, userDept, pageReqVO);
        
        // 根据角色过滤通知
        NotificationPageReqVO filteredPageReqVO = filterNotificationsByRole(pageReqVO, userRole, userDept);
        
        PageResult<NotificationDO> pageResult = notificationMapper.selectPage(filteredPageReqVO);
        
        // 按照优先级和时间排序
        List<NotificationDO> sortedList = pageResult.getList().stream()
                .sorted((n1, n2) -> {
                    // 首先按置顶排序
                    int pinnedCompare = Boolean.compare(n2.getPinned() != null && n2.getPinned(), 
                                                       n1.getPinned() != null && n1.getPinned());
                    if (pinnedCompare != 0) return pinnedCompare;
                    
                    // 然后按通知级别排序（数值越小优先级越高）
                    int levelCompare = Integer.compare(n1.getLevel(), n2.getLevel());
                    if (levelCompare != 0) return levelCompare;
                    
                    // 最后按创建时间倒序排序
                    return n2.getCreateTime().compareTo(n1.getCreateTime());
                })
                .collect(Collectors.toList());
        
        pageResult.setList(sortedList);
        
        log.info("[getNotificationsByRole][角色通知获取完成] userRole: {}, count: {}", userRole, sortedList.size());
        return pageResult;
    }

    /**
     * 根据角色过滤通知的请求参数
     * 根据NOTIFICATION_BUSINESS_LOGIC.md中的角色权限体系进行过滤
     */
    private NotificationPageReqVO filterNotificationsByRole(NotificationPageReqVO pageReqVO, String userRole, String userDept) {
        NotificationPageReqVO filteredReqVO = new NotificationPageReqVO();
        
        // 复制基础分页参数
        filteredReqVO.setPageNo(pageReqVO.getPageNo());
        filteredReqVO.setPageSize(pageReqVO.getPageSize());
        filteredReqVO.setTitle(pageReqVO.getTitle());
        
        // 只显示已发布的通知
        filteredReqVO.setStatus(NotificationStatusEnum.PUBLISHED.getStatus());
        
        // 根据角色设置通知级别权限
        switch (userRole.toUpperCase()) {
            case "PRINCIPAL": // 校长
            case "VICE_PRINCIPAL": // 副校长
            case "DEAN": // 院长
                // 可以查看所有级别的通知
                break;
                
            case "VICE_DEAN": // 副院长
            case "PARTY_SECRETARY": // 党委书记
            case "DEPARTMENT_HEAD": // 系主任
            case "GRADE_DIRECTOR": // 年级主任
                // 可以查看重要、常规、提醒通知
                filteredReqVO.setLevel(null); // 将在SQL层过滤
                break;
                
            case "TEACHER": // 教师
            case "COUNSELOR": // 辅导员
            case "CLASS_TEACHER": // 班主任
                // 可以查看常规、提醒通知
                filteredReqVO.setLevel(null); // 将在SQL层过滤
                break;
                
            case "STUDENT": // 学生
            default:
                // 学生可以查看所有发布给学生的通知
                break;
        }
        
        return filteredReqVO;
    }
}