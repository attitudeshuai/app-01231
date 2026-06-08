package com.library.module.notification.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 查询用户的通知列表
     */
    List<Notification> selectByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 统计用户通知数量
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * 统计用户未读通知数量
     */
    int countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 标记已读
     */
    int markAsRead(@Param("id") Long id);

    /**
     * 标记用户所有通知已读
     */
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 批量插入通知
     */
    int batchInsertNotifications(@Param("list") List<Notification> notifications);
}
