package com.library.module.borrow.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.borrow.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 借阅记录Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 分页查询（带用户和图书信息）
     */
    List<BorrowRecord> selectPageWithDetails(@Param("params") Map<String, Object> params,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 统计查询条件下的总数
     */
    long countWithDetails(@Param("params") Map<String, Object> params);

    /**
     * 查询用户的借阅记录
     */
    List<BorrowRecord> selectByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 统计用户借阅记录数
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * 查询用户正在借阅的图书
     */
    List<BorrowRecord> selectBorrowingByUserId(@Param("userId") Long userId);

    /**
     * 查询用户某本图书的借阅中记录
     */
    BorrowRecord selectBorrowingByUserIdAndBookId(@Param("userId") Long userId,
                                                   @Param("bookId") Long bookId);

    /**
     * 统计用户当前借阅数量
     */
    int countBorrowingByUserId(@Param("userId") Long userId);

    /**
     * 查询逾期记录
     */
    List<BorrowRecord> selectOverdue(@Param("now") LocalDateTime now,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /**
     * 统计逾期记录数
     */
    long countOverdue(@Param("now") LocalDateTime now);

    /**
     * 查询即将到期的记录（用于提醒）
     */
    List<BorrowRecord> selectDueSoon(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 归还图书
     */
    int updateReturn(@Param("id") Long id,
                     @Param("returnDate") LocalDateTime returnDate,
                     @Param("fineAmount") java.math.BigDecimal fineAmount,
                     @Param("status") Integer status);

    /**
     * 续借
     */
    int updateRenew(@Param("id") Long id,
                    @Param("dueDate") LocalDateTime dueDate,
                    @Param("renewCount") Integer renewCount);

    /**
     * 更新逾期状态
     */
    int updateOverdueStatus(@Param("now") LocalDateTime now);

    /**
     * 统计借阅数量（按时间范围）
     */
    Map<String, Object> selectStatistics(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 查询即将到期的借阅记录（用于邮件提醒）
     */
    List<BorrowRecord> selectDueSoonRecords(@Param("today") java.time.LocalDate today,
                                             @Param("reminderDate") java.time.LocalDate reminderDate);

    /**
     * 查询逾期的借阅记录（用于邮件通知）
     */
    List<BorrowRecord> selectOverdueRecords(@Param("today") java.time.LocalDate today);
}
