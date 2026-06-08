package com.library.common.service;

/**
 * 邮件服务接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface MailService {

    /**
     * 发送简单文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     */
    void sendSimpleMail(String to, String subject, String content);

    /**
     * 发送HTML邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content HTML内容
     */
    void sendHtmlMail(String to, String subject, String content);

    /**
     * 发送借阅到期提醒邮件
     *
     * @param to         收件人
     * @param username   用户名
     * @param bookTitle  书名
     * @param dueDate    到期日期
     * @param daysRemain 剩余天数
     */
    void sendBorrowReminderMail(String to, String username, String bookTitle, String dueDate, int daysRemain);

    /**
     * 发送逾期通知邮件
     *
     * @param to           收件人
     * @param username     用户名
     * @param bookTitle    书名
     * @param dueDate      到期日期
     * @param overdueDays  逾期天数
     * @param fineAmount   罚款金额
     */
    void sendOverdueNoticeMail(String to, String username, String bookTitle, String dueDate, int overdueDays, double fineAmount);
}
