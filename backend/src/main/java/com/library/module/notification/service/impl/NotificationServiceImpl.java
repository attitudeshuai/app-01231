package com.library.module.notification.service.impl;

import com.library.common.base.BaseServiceImpl;
import com.library.common.response.PageResult;
import com.library.module.notification.entity.Notification;
import com.library.module.notification.mapper.NotificationMapper;
import com.library.module.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知Service实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
public class NotificationServiceImpl extends BaseServiceImpl<NotificationMapper, Notification> 
        implements NotificationService {

    @Override
    public PageResult<Notification> getUserNotifications(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notification> list = baseMapper.selectByUserId(userId, offset, pageSize);
        long total = baseMapper.countByUserId(userId);
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return baseMapper.countUnreadByUserId(userId);
    }

    @Override
    public void markAsRead(Long id) {
        baseMapper.markAsRead(id);
        log.debug("标记通知已读: {}", id);
    }

    @Override
    public void markAllAsRead(Long userId) {
        baseMapper.markAllAsRead(userId);
        log.info("标记用户所有通知已读: userId={}", userId);
    }

    @Override
    public void sendSystemNotification(Long userId, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(1);
        notification.setIsRead(0);
        save(notification);
        log.info("发送系统通知: userId={}, title={}", userId, title);
    }

    @Override
    public void sendDueReminder(Long userId, String bookTitle, String dueDate) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("借阅到期提醒");
        notification.setContent(String.format("您借阅的《%s》将于%s到期，请及时归还或续借。", bookTitle, dueDate));
        notification.setType(2);
        notification.setIsRead(0);
        save(notification);
        log.info("发送到期提醒: userId={}, book={}", userId, bookTitle);
    }
}
