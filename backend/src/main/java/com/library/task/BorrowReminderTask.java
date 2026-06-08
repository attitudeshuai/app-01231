package com.library.task;

import com.library.common.service.MailService;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.mapper.BorrowRecordMapper;
import com.library.module.user.entity.User;
import com.library.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 借阅到期提醒定时任务
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowReminderTask {

    private final BorrowRecordMapper borrowRecordMapper;
    private final UserMapper userMapper;
    private final MailService mailService;

    @Value("${library.reminder.enabled:true}")
    private boolean reminderEnabled;

    @Value("${library.reminder.days-before:3}")
    private int daysBeforeReminder;

    @Value("${library.overdue-fine-per-day:0.5}")
    private double finePerDay;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每天早上8点执行借阅到期提醒
     */
    @Scheduled(cron = "${library.reminder.cron:0 0 8 * * ?}")
    public void sendBorrowReminders() {
        if (!reminderEnabled) {
            log.info("借阅提醒功能已禁用");
            return;
        }

        log.info("开始执行借阅到期提醒任务...");
        
        try {
            // 获取即将到期的借阅记录（未归还且在指定天数内到期）
            LocalDate today = LocalDate.now();
            LocalDate reminderDate = today.plusDays(daysBeforeReminder);
            
            List<BorrowRecord> dueSoonRecords = borrowRecordMapper.selectDueSoonRecords(today, reminderDate);
            
            int sentCount = 0;
            for (BorrowRecord record : dueSoonRecords) {
                try {
                    sendReminderEmail(record);
                    sentCount++;
                } catch (Exception e) {
                    log.error("发送提醒邮件失败: recordId={}", record.getId(), e);
                }
            }
            
            log.info("借阅到期提醒任务完成，共发送 {} 封提醒邮件", sentCount);
        } catch (Exception e) {
            log.error("借阅到期提醒任务执行失败", e);
        }
    }

    /**
     * 每天早上9点执行逾期通知
     */
    @Scheduled(cron = "${library.overdue.cron:0 0 9 * * ?}")
    public void sendOverdueNotices() {
        if (!reminderEnabled) {
            log.info("逾期通知功能已禁用");
            return;
        }

        log.info("开始执行逾期通知任务...");
        
        try {
            LocalDate today = LocalDate.now();
            List<BorrowRecord> overdueRecords = borrowRecordMapper.selectOverdueRecords(today);
            
            int sentCount = 0;
            for (BorrowRecord record : overdueRecords) {
                try {
                    sendOverdueEmail(record, today);
                    sentCount++;
                } catch (Exception e) {
                    log.error("发送逾期通知邮件失败: recordId={}", record.getId(), e);
                }
            }
            
            log.info("逾期通知任务完成，共发送 {} 封通知邮件", sentCount);
        } catch (Exception e) {
            log.error("逾期通知任务执行失败", e);
        }
    }

    private void sendReminderEmail(BorrowRecord record) {
        User user = userMapper.selectById(record.getUserId());
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("用户邮箱为空，跳过发送: userId={}", record.getUserId());
            return;
        }

        LocalDate dueDate = record.getDueDate().toLocalDate();
        int daysRemain = (int) ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        
        mailService.sendBorrowReminderMail(
            user.getEmail(),
            user.getUsername(),
            record.getBookTitle(),
            dueDate.format(DATE_FORMATTER),
            daysRemain
        );
        
        log.debug("已发送到期提醒邮件: user={}, book={}, dueDate={}", 
            user.getUsername(), record.getBookTitle(), dueDate);
    }

    private void sendOverdueEmail(BorrowRecord record, LocalDate today) {
        User user = userMapper.selectById(record.getUserId());
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("用户邮箱为空，跳过发送: userId={}", record.getUserId());
            return;
        }

        LocalDate dueDate = record.getDueDate().toLocalDate();
        int overdueDays = (int) ChronoUnit.DAYS.between(dueDate, today);
        double fineAmount = overdueDays * finePerDay;
        
        mailService.sendOverdueNoticeMail(
            user.getEmail(),
            user.getUsername(),
            record.getBookTitle(),
            dueDate.format(DATE_FORMATTER),
            overdueDays,
            fineAmount
        );
        
        log.debug("已发送逾期通知邮件: user={}, book={}, overdueDays={}, fine={}", 
            user.getUsername(), record.getBookTitle(), overdueDays, fineAmount);
    }
}
