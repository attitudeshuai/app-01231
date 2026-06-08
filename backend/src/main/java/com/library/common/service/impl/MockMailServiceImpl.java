package com.library.common.service.impl;

import com.library.common.service.MailService;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮件服务Mock实现
 *
 * <p>当未配置SMTP邮件服务时作为降级方案自动启用。
 * 所有邮件发送操作仅记录到日志，不会实际发送邮件。
 * 适用于开发环境和未配置邮箱的部署场景。</p>
 *
 * @author Library System
 * @since 1.0.0
 * @see com.library.common.config.MailConfig
 * @see MailServiceImpl
 */
@Slf4j
public class MockMailServiceImpl implements MailService {

    /**
     * 模拟发送简单文本邮件，将邮件内容输出到日志
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件正文内容
     */
    @Override
    public void sendSimpleMail(String to, String subject, String content) {
        log.info("[MockMail] to={}, subject={}, content={}", to, subject, content);
    }

    /**
     * 模拟发送HTML邮件，将邮件信息输出到日志
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param content HTML格式的邮件内容
     */
    @Override
    public void sendHtmlMail(String to, String subject, String content) {
        log.info("[MockMail] to={}, subject={}, htmlLength={}", to, subject, content == null ? 0 : content.length());
    }

    /**
     * 模拟发送借阅到期提醒邮件
     *
     * @param to         收件人邮箱地址
     * @param username   借阅用户名
     * @param bookTitle  图书名称
     * @param dueDate    到期日期
     * @param daysRemain 距离到期剩余天数
     */
    @Override
    public void sendBorrowReminderMail(String to, String username, String bookTitle, String dueDate, int daysRemain) {
        log.info("[MockMail] borrow reminder to={}, username={}, bookTitle={}, dueDate={}, daysRemain={}",
                to, username, bookTitle, dueDate, daysRemain);
    }

    /**
     * 模拟发送逾期通知邮件
     *
     * @param to          收件人邮箱地址
     * @param username    借阅用户名
     * @param bookTitle   图书名称
     * @param dueDate     到期日期
     * @param overdueDays 逾期天数
     * @param fineAmount  罚款金额
     */
    @Override
    public void sendOverdueNoticeMail(String to, String username, String bookTitle, String dueDate, int overdueDays, double fineAmount) {
        log.info("[MockMail] overdue notice to={}, username={}, bookTitle={}, dueDate={}, overdueDays={}, fineAmount={}",
                to, username, bookTitle, dueDate, overdueDays, fineAmount);
    }
}
