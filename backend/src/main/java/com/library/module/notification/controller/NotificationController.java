package com.library.module.notification.controller;

import com.library.common.response.PageResult;
import com.library.common.response.Result;
import com.library.common.util.SecurityUtil;
import com.library.module.notification.entity.Notification;
import com.library.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知Controller
 *
 * <p>提供用户站内通知相关的RESTful API，包括通知列表查询、
 * 未读数量统计、标记已读等功能。通知由借阅到期提醒、
 * 逾期通知等业务场景自动触发生成。</p>
 *
 * @author Library System
 * @since 1.0.0
 * @see NotificationService
 */
@Tag(name = "通知管理")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前用户的通知列表（分页）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页通知列表
     */
    @Operation(summary = "获取通知列表")
    @GetMapping
    public Result<PageResult<Notification>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        PageResult<Notification> page = notificationService.getUserNotifications(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取当前用户的未读通知数量
     *
     * @return 未读通知数
     */
    @Operation(summary = "获取未读数量")
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        int count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记指定通知为已读
     *
     * @param id 通知ID
     */
    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    /**
     * 将当前用户的所有未读通知标记为已读
     */
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
}
