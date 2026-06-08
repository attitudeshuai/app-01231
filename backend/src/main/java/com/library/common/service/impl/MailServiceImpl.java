package com.library.common.service.impl;

import com.library.common.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

/**
 * 邮件服务实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.application.name:图书管理系统}")
    private String systemName;

    @Override
    @Async
    public void sendSimpleMail(String to, String subject, String content) {
        if (!isMailConfigured()) {
            log.warn("邮件服务未配置，跳过发送邮件到: {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("简单邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("简单邮件发送失败: to={}, subject={}", to, subject, e);
        }
    }

    @Override
    @Async
    public void sendHtmlMail(String to, String subject, String content) {
        if (!isMailConfigured()) {
            log.warn("邮件服务未配置，跳过发送邮件到: {}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: to={}, subject={}", to, subject, e);
        }
    }

    @Override
    public void sendBorrowReminderMail(String to, String username, String bookTitle, String dueDate, int daysRemain) {
        try {
            String subject = "【" + systemName + "】借阅到期提醒";
            String content = buildBorrowReminderHtml(username, bookTitle, dueDate, daysRemain);
            sendHtmlMail(to, subject, content);
        } catch (Exception e) {
            log.error("发送借阅到期提醒邮件失败，降级处理: to={}, book={}", to, bookTitle, e);
        }
    }

    @Override
    public void sendOverdueNoticeMail(String to, String username, String bookTitle, String dueDate, int overdueDays, double fineAmount) {
        try {
            String subject = "【" + systemName + "】图书逾期通知";
            String content = buildOverdueNoticeHtml(username, bookTitle, dueDate, overdueDays, fineAmount);
            sendHtmlMail(to, subject, content);
        } catch (Exception e) {
            log.error("发送逾期通知邮件失败，降级处理: to={}, book={}", to, bookTitle, e);
        }
    }

    private boolean isMailConfigured() {
        return fromEmail != null && !fromEmail.isEmpty() && !fromEmail.equals("${MAIL_USER:}");
    }

    private String buildBorrowReminderHtml(String username, String bookTitle, String dueDate, int daysRemain) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f5f5f5; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 40px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .header h1 { color: #5B5FC7; margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.8; }
                    .highlight { background: #FFF7ED; border-left: 4px solid #F59E0B; padding: 15px 20px; margin: 20px 0; border-radius: 0 8px 8px 0; }
                    .book-title { color: #5B5FC7; font-weight: bold; }
                    .days-remain { color: #F59E0B; font-size: 24px; font-weight: bold; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #999; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📚 借阅到期提醒</h1>
                    </div>
                    <div class="content">
                        <p>尊敬的 <strong>%s</strong>，您好！</p>
                        <div class="highlight">
                            <p>您借阅的图书 <span class="book-title">《%s》</span> 将于 <strong>%s</strong> 到期。</p>
                            <p>距离到期还有 <span class="days-remain">%d</span> 天，请及时归还或续借。</p>
                        </div>
                        <p>如已归还，请忽略此邮件。</p>
                        <p>感谢您使用图书管理系统！</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, bookTitle, dueDate, daysRemain);
    }

    private String buildOverdueNoticeHtml(String username, String bookTitle, String dueDate, int overdueDays, double fineAmount) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f5f5f5; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 40px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .header h1 { color: #EF4444; margin: 0; font-size: 24px; }
                    .content { color: #333; line-height: 1.8; }
                    .warning { background: #FEF2F2; border-left: 4px solid #EF4444; padding: 15px 20px; margin: 20px 0; border-radius: 0 8px 8px 0; }
                    .book-title { color: #5B5FC7; font-weight: bold; }
                    .overdue-days { color: #EF4444; font-size: 24px; font-weight: bold; }
                    .fine { color: #EF4444; font-size: 20px; font-weight: bold; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #999; font-size: 12px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⚠️ 图书逾期通知</h1>
                    </div>
                    <div class="content">
                        <p>尊敬的 <strong>%s</strong>，您好！</p>
                        <div class="warning">
                            <p>您借阅的图书 <span class="book-title">《%s》</span> 应于 <strong>%s</strong> 归还。</p>
                            <p>目前已逾期 <span class="overdue-days">%d</span> 天，产生罚款 <span class="fine">¥%.2f</span>。</p>
                        </div>
                        <p>请尽快归还图书并缴纳罚款，以免影响您的借阅权限。</p>
                        <p>感谢您的配合！</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, bookTitle, dueDate, overdueDays, fineAmount);
    }
}
