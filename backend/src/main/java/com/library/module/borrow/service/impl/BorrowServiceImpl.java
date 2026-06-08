package com.library.module.borrow.service.impl;

import com.library.common.base.BaseServiceImpl;
import com.library.common.exception.BusinessException;
import com.library.common.response.PageResult;
import com.library.common.response.ResultCode;
import com.library.module.book.entity.Book;
import com.library.module.book.service.BookService;
import com.library.module.borrow.dto.BorrowQueryDTO;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.mapper.BorrowRecordMapper;
import com.library.module.borrow.service.BorrowService;
import com.library.module.notification.entity.Notification;
import com.library.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 借阅Service实现
 *
 * <p>实现图书借阅核心业务：借阅、归还、续借、逾期处理、定时提醒。</p>
 * <ul>
 *   <li>每人最多同时借阅 {@value #MAX_BORROW_COUNT} 本</li>
 *   <li>逾期自动计算罚款，定时任务检查逾期并发送邮件/站内通知</li>
 *   <li>每本书最多续借1次，续借后到期日期顺延</li>
 * </ul>
 *
 * @author Library System
 * @since 1.0.0
 * @see BorrowService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl extends BaseServiceImpl<BorrowRecordMapper, BorrowRecord> 
        implements BorrowService {

    private final BookService bookService;
    private final NotificationService notificationService;

    @Value("${library.default-borrow-days:30}")
    private int defaultBorrowDays;

    @Value("${library.max-renew-times:1}")
    private int maxRenewTimes;

    @Value("${library.renew-days:30}")
    private int renewDays;

    @Value("${library.overdue-fine-per-day:0.5}")
    private double overdueFinePerDay;

    @Value("${library.reminder-days-before:3}")
    private int reminderDaysBefore;

    private static final int MAX_BORROW_COUNT = 5;

    @Override
    public PageResult<BorrowRecord> pageBorrows(BorrowQueryDTO queryDTO) {
        Map<String, Object> params = new HashMap<>();
        if (queryDTO.getUserId() != null) {
            params.put("userId", queryDTO.getUserId());
        }
        if (queryDTO.getUsername() != null) {
            params.put("username", queryDTO.getUsername());
        }
        if (queryDTO.getBookTitle() != null) {
            params.put("bookTitle", queryDTO.getBookTitle());
        }
        if (queryDTO.getStatus() != null) {
            params.put("status", queryDTO.getStatus());
        }
        if (queryDTO.getStartDate() != null) {
            params.put("startDate", queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            params.put("endDate", queryDTO.getEndDate());
        }

        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        List<BorrowRecord> list = baseMapper.selectPageWithDetails(params, offset, queryDTO.getPageSize());
        long total = baseMapper.countWithDetails(params);

        return PageResult.of(list, total, queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long borrowBook(Long userId, Long bookId) {
        // 检查图书是否存在
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }

        // 检查图书是否上架
        if (book.getStatus() != 1) {
            throw new BusinessException(ResultCode.DATA_ERROR, "该图书已下架");
        }

        // 检查库存
        if (book.getAvailableStock() <= 0) {
            throw new BusinessException(ResultCode.BOOK_STOCK_NOT_ENOUGH);
        }

        // 检查是否已借阅该图书
        BorrowRecord existRecord = baseMapper.selectBorrowingByUserIdAndBookId(userId, bookId);
        if (existRecord != null) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_BORROWED);
        }

        // 检查借阅数量是否达到上限
        int borrowingCount = baseMapper.countBorrowingByUserId(userId);
        if (borrowingCount >= MAX_BORROW_COUNT) {
            throw new BusinessException(ResultCode.BORROW_LIMIT_EXCEEDED);
        }

        // 减少库存
        if (!bookService.decreaseStock(bookId, 1)) {
            throw new BusinessException(ResultCode.BOOK_STOCK_NOT_ENOUGH);
        }

        // 创建借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(defaultBorrowDays));
        record.setRenewCount(0);
        record.setFineAmount(BigDecimal.ZERO);
        record.setStatus(0);

        save(record);
        log.info("借阅图书成功: userId={}, bookId={}, recordId={}", userId, bookId, record.getId());

        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBook(Long borrowId) {
        BorrowRecord record = getById(borrowId);
        if (record == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_EXIST);
        }

        if (record.getStatus() == 1) {
            throw new BusinessException(ResultCode.DATA_ERROR, "该图书已归还");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal fineAmount = BigDecimal.ZERO;
        int status = 1;

        // 计算逾期罚款
        if (now.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), now);
            fineAmount = BigDecimal.valueOf(overdueDays * overdueFinePerDay);
            log.info("逾期归还: borrowId={}, overdueDays={}, fine={}", borrowId, overdueDays, fineAmount);
        }

        // 更新借阅记录
        baseMapper.updateReturn(borrowId, now, fineAmount, status);

        // 增加库存
        bookService.increaseStock(record.getBookId(), 1);

        log.info("归还图书成功: borrowId={}", borrowId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renewBook(Long borrowId) {
        BorrowRecord record = getById(borrowId);
        if (record == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_EXIST);
        }

        if (record.getStatus() == 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_RETURNED, "该图书已归还，无需续借");
        }

        // 检查续借次数
        if (record.getRenewCount() >= maxRenewTimes) {
            throw new BusinessException(ResultCode.RENEW_LIMIT_EXCEEDED);
        }

        // 检查是否逾期
        if (record.getStatus() == 2 || LocalDateTime.now().isAfter(record.getDueDate())) {
            throw new BusinessException(ResultCode.BORROW_OVERDUE);
        }

        // 更新续借信息
        LocalDateTime newDueDate = record.getDueDate().plusDays(renewDays);
        int newRenewCount = record.getRenewCount() + 1;
        baseMapper.updateRenew(borrowId, newDueDate, newRenewCount);

        log.info("续借成功: borrowId={}, newDueDate={}", borrowId, newDueDate);
    }

    @Override
    public PageResult<BorrowRecord> getMyBorrows(Long userId, Integer status, int pageNum, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        if (status != null) {
            params.put("status", status);
        }

        int offset = (pageNum - 1) * pageSize;
        List<BorrowRecord> list = baseMapper.selectPageWithDetails(params, offset, pageSize);
        long total = baseMapper.countWithDetails(params);

        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public List<BorrowRecord> getCurrentBorrows(Long userId) {
        return baseMapper.selectBorrowingByUserId(userId);
    }

    @Override
    public PageResult<BorrowRecord> getOverdueList(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<BorrowRecord> list = baseMapper.selectOverdue(LocalDateTime.now(), offset, pageSize);
        long total = baseMapper.countOverdue(LocalDateTime.now());
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateOverdueStatus() {
        int count = baseMapper.updateOverdueStatus(LocalDateTime.now());
        log.info("更新逾期状态完成，共{}条", count);
    }

    @Override
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDueReminder() {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(reminderDaysBefore);

        List<BorrowRecord> records = baseMapper.selectDueSoon(startDate, endDate);

        for (BorrowRecord record : records) {
            Notification notification = new Notification();
            notification.setUserId(record.getUserId());
            notification.setTitle("借阅到期提醒");
            notification.setContent(String.format("您借阅的《%s》将于%s到期，请及时归还或续借。",
                    record.getBookTitle(),
                    record.getDueDate().toLocalDate().toString()));
            notification.setType(2);
            notification.setIsRead(0);

            notificationService.save(notification);
        }

        log.info("发送到期提醒完成，共{}条", records.size());
    }

    @Override
    public Map<String, Object> getStatistics() {
        // 先更新逾期状态
        baseMapper.updateOverdueStatus(LocalDateTime.now());
        
        // 统计所有借阅记录
        Map<String, Object> result = new HashMap<>();
        
        // 查询各状态的数量
        Map<String, Object> params = new HashMap<>();
        
        // 借阅中（状态0，未逾期）
        params.put("status", 0);
        long borrowingCount = baseMapper.countByCondition(params);
        
        // 已归还（状态1）
        params.put("status", 1);
        long returnedCount = baseMapper.countByCondition(params);
        
        // 逾期（状态2）
        params.put("status", 2);
        long overdueCount = baseMapper.countByCondition(params);
        
        // 总数
        params.clear();
        long totalBorrows = baseMapper.countByCondition(params);
        
        result.put("totalBorrows", totalBorrows);
        result.put("borrowingCount", borrowingCount);
        result.put("returnedCount", returnedCount);
        result.put("overdueCount", overdueCount);
        
        return result;
    }

    @Override
    public boolean canBorrow(Long userId, Long bookId) {
        // 检查是否已借阅
        BorrowRecord existRecord = baseMapper.selectBorrowingByUserIdAndBookId(userId, bookId);
        if (existRecord != null) {
            return false;
        }

        // 检查借阅数量
        int borrowingCount = baseMapper.countBorrowingByUserId(userId);
        return borrowingCount < MAX_BORROW_COUNT;
    }
}
