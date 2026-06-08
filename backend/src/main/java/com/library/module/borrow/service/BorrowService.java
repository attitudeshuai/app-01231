package com.library.module.borrow.service;

import com.library.common.base.BaseService;
import com.library.common.response.PageResult;
import com.library.module.borrow.dto.BorrowQueryDTO;
import com.library.module.borrow.entity.BorrowRecord;

import java.util.List;
import java.util.Map;

/**
 * 借阅Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface BorrowService extends BaseService<BorrowRecord> {

    /**
     * 分页查询借阅记录
     */
    PageResult<BorrowRecord> pageBorrows(BorrowQueryDTO queryDTO);

    /**
     * 借阅图书
     */
    Long borrowBook(Long userId, Long bookId);

    /**
     * 归还图书
     */
    void returnBook(Long borrowId);

    /**
     * 续借图书
     */
    void renewBook(Long borrowId);

    /**
     * 获取用户借阅记录
     */
    PageResult<BorrowRecord> getMyBorrows(Long userId, Integer status, int pageNum, int pageSize);

    /**
     * 获取用户当前借阅列表
     */
    List<BorrowRecord> getCurrentBorrows(Long userId);

    /**
     * 获取逾期列表
     */
    PageResult<BorrowRecord> getOverdueList(int pageNum, int pageSize);

    /**
     * 更新逾期状态
     */
    void updateOverdueStatus();

    /**
     * 发送到期提醒
     */
    void sendDueReminder();

    /**
     * 获取借阅统计
     */
    Map<String, Object> getStatistics();

    /**
     * 检查用户是否可以借阅
     */
    boolean canBorrow(Long userId, Long bookId);
}
