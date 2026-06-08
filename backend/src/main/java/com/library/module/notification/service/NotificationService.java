package com.library.module.notification.service;

import com.library.common.base.BaseService;
import com.library.common.response.PageResult;
import com.library.module.notification.entity.Notification;

/**
 * 通知Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface NotificationService extends BaseService<Notification> {

    /**
     * 获取用户通知列表
     */
    PageResult<Notification> getUserNotifications(Long userId, int pageNum, int pageSize);

    /**
     * 获取用户未读通知数量
     */
    int getUnreadCount(Long userId);

    /**
     * 标记已读
     */
    void markAsRead(Long id);

    /**
     * 标记全部已读
     */
    void markAllAsRead(Long userId);

    /**
     * 发送系统通知
     */
    void sendSystemNotification(Long userId, String title, String content);

    /**
     * 发送到期提醒
     */
    void sendDueReminder(Long userId, String bookTitle, String dueDate);
}
