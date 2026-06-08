package com.library.module.notification.service;

import com.library.common.response.PageResult;
import com.library.module.notification.entity.Notification;
import com.library.module.notification.mapper.NotificationMapper;
import com.library.module.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "baseMapper", notificationMapper);
    }

    @Test
    @DisplayName("getUserNotifications returns page result")
    void getUserNotifications_Success() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setTitle("系统通知");
        when(notificationMapper.selectByUserId(1L, 0, 10)).thenReturn(List.of(notification));
        when(notificationMapper.countByUserId(1L)).thenReturn(1L);

        PageResult<Notification> result = notificationService.getUserNotifications(1L, 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("系统通知", result.getList().get(0).getTitle());
    }

    @Test
    @DisplayName("getUnreadCount delegates to mapper")
    void getUnreadCount_Success() {
        when(notificationMapper.countUnreadByUserId(1L)).thenReturn(3);

        assertEquals(3, notificationService.getUnreadCount(1L));
    }

    @Test
    @DisplayName("markAsRead delegates to mapper")
    void markAsRead_Success() {
        notificationService.markAsRead(9L);

        verify(notificationMapper).markAsRead(9L);
    }

    @Test
    @DisplayName("markAllAsRead delegates to mapper")
    void markAllAsRead_Success() {
        notificationService.markAllAsRead(3L);

        verify(notificationMapper).markAllAsRead(3L);
    }

    @Test
    @DisplayName("sendSystemNotification saves notification")
    void sendSystemNotification_Success() {
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

        notificationService.sendSystemNotification(5L, "标题", "内容");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(5L, saved.getUserId());
        assertEquals("标题", saved.getTitle());
        assertEquals("内容", saved.getContent());
        assertEquals(1, saved.getType());
        assertEquals(0, saved.getIsRead());
    }

    @Test
    @DisplayName("sendDueReminder saves due reminder notification")
    void sendDueReminder_Success() {
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

        notificationService.sendDueReminder(7L, "三体", "2026-03-10");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(7L, saved.getUserId());
        assertEquals("借阅到期提醒", saved.getTitle());
        assertEquals(2, saved.getType());
        assertEquals(0, saved.getIsRead());
        assertTrue(saved.getContent().contains("三体"));
        assertTrue(saved.getContent().contains("2026-03-10"));
    }
}
