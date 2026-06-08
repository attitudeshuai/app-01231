package com.library.common.service;

import com.library.common.service.impl.MailServiceImpl;
import com.library.common.service.impl.MockMailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailServiceImpl(mailSender);
        ReflectionTestUtils.setField(mailService, "systemName", "图书管理系统");
    }

    @Test
    void sendSimpleMail_SendsWhenConfigured() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@example.com");

        mailService.sendSimpleMail("to@example.com", "subject", "content");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("noreply@example.com", captor.getValue().getFrom());
        assertEquals("subject", captor.getValue().getSubject());
    }

    @Test
    void sendSimpleMail_SkipsWhenNotConfigured() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "");

        mailService.sendSimpleMail("to@example.com", "subject", "content");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendHtmlMail_SendsWhenConfigured() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@example.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendHtmlMail("to@example.com", "subject", "<b>content</b>");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendBorrowReminderMail_SendsHtmlMail() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@example.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendBorrowReminderMail("to@example.com", "alice", "三体", "2026-03-10", 3);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOverdueNoticeMail_SendsHtmlMail() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@example.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendOverdueNoticeMail("to@example.com", "alice", "三体", "2026-03-01", 5, 2.5);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void mockMailService_DoesNotThrow() {
        MailService mockMailService = new MockMailServiceImpl();

        assertDoesNotThrow(() -> mockMailService.sendSimpleMail("to@example.com", "subject", "content"));
        assertDoesNotThrow(() -> mockMailService.sendHtmlMail("to@example.com", "subject", "<b>content</b>"));
        assertDoesNotThrow(() -> mockMailService.sendBorrowReminderMail("to@example.com", "alice", "三体", "2026-03-10", 3));
        assertDoesNotThrow(() -> mockMailService.sendOverdueNoticeMail("to@example.com", "alice", "三体", "2026-03-01", 5, 2.5));
    }
}
