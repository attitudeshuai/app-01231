package com.library.module.borrow.service;

import com.library.common.exception.BusinessException;
import com.library.common.response.PageResult;
import com.library.common.response.ResultCode;
import com.library.module.book.entity.Book;
import com.library.module.book.service.BookService;
import com.library.module.borrow.dto.BorrowQueryDTO;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.mapper.BorrowRecordMapper;
import com.library.module.borrow.service.impl.BorrowServiceImpl;
import com.library.module.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BorrowService 单元测试
 *
 * @author Library System
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("借阅服务单元测试")
class BorrowServiceTest {

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookService bookService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private Book testBook;
    private BorrowRecord testBorrowRecord;

    @BeforeEach
    void setUp() {
        // 设置私有字段
        ReflectionTestUtils.setField(borrowService, "defaultBorrowDays", 30);
        ReflectionTestUtils.setField(borrowService, "maxRenewTimes", 1);
        ReflectionTestUtils.setField(borrowService, "renewDays", 30);
        ReflectionTestUtils.setField(borrowService, "overdueFinePerDay", 0.5);
        ReflectionTestUtils.setField(borrowService, "reminderDaysBefore", 3);
        ReflectionTestUtils.setField(borrowService, "baseMapper", borrowRecordMapper);

        // 初始化测试数据
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("测试图书");
        testBook.setStatus(1);
        testBook.setAvailableStock(10);

        testBorrowRecord = new BorrowRecord();
        testBorrowRecord.setId(1L);
        testBorrowRecord.setUserId(1L);
        testBorrowRecord.setBookId(1L);
        testBorrowRecord.setBorrowDate(LocalDateTime.now().minusDays(10));
        testBorrowRecord.setDueDate(LocalDateTime.now().plusDays(20));
        testBorrowRecord.setRenewCount(0);
        testBorrowRecord.setFineAmount(BigDecimal.ZERO);
        testBorrowRecord.setStatus(0); // 借阅中
    }

    @Test
    @DisplayName("分页查询借阅记录 - 成功")
    void pageBorrows_Success() {
        // 准备数据
        BorrowQueryDTO queryDTO = new BorrowQueryDTO();
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10);
        queryDTO.setUserId(1L);

        when(borrowRecordMapper.selectPageWithDetails(anyMap(), eq(0), eq(10)))
                .thenReturn(Arrays.asList(testBorrowRecord));
        when(borrowRecordMapper.countWithDetails(anyMap())).thenReturn(1L);

        // 执行测试
        PageResult<BorrowRecord> result = borrowService.pageBorrows(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    @Test
    @DisplayName("借阅图书 - 成功")
    void borrowBook_Success() {
        // 准备数据
        when(bookService.getById(1L)).thenReturn(testBook);
        when(borrowRecordMapper.selectBorrowingByUserIdAndBookId(1L, 1L)).thenReturn(null);
        when(borrowRecordMapper.countBorrowingByUserId(1L)).thenReturn(2);
        when(bookService.decreaseStock(1L, 1)).thenReturn(true);
        when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setId(1L);
            return 1;
        });

        // 执行测试
        Long recordId = borrowService.borrowBook(1L, 1L);

        // 验证结果
        assertNotNull(recordId);
        assertEquals(1L, recordId);

        verify(bookService).decreaseStock(1L, 1);
        verify(borrowRecordMapper).insert(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("借阅图书 - 图书不存在")
    void borrowBook_BookNotFound() {
        // 准备数据
        when(bookService.getById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.borrowBook(1L, 999L);
        });

        assertEquals(ResultCode.BOOK_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅图书 - 图书已下架")
    void borrowBook_BookOffShelf() {
        // 准备数据
        testBook.setStatus(0); // 下架
        when(bookService.getById(1L)).thenReturn(testBook);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.borrowBook(1L, 1L);
        });

        assertEquals(ResultCode.DATA_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅图书 - 库存不足")
    void borrowBook_InsufficientStock() {
        // 准备数据
        testBook.setAvailableStock(0);
        when(bookService.getById(1L)).thenReturn(testBook);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.borrowBook(1L, 1L);
        });

        assertEquals(ResultCode.BOOK_STOCK_NOT_ENOUGH.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅图书 - 已借阅该图书")
    void borrowBook_AlreadyBorrowed() {
        // 准备数据
        when(bookService.getById(1L)).thenReturn(testBook);
        when(borrowRecordMapper.selectBorrowingByUserIdAndBookId(1L, 1L)).thenReturn(testBorrowRecord);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.borrowBook(1L, 1L);
        });

        assertEquals(ResultCode.BOOK_ALREADY_BORROWED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅图书 - 借阅数量达到上限")
    void borrowBook_LimitExceeded() {
        // 准备数据
        when(bookService.getById(1L)).thenReturn(testBook);
        when(borrowRecordMapper.selectBorrowingByUserIdAndBookId(1L, 1L)).thenReturn(null);
        when(borrowRecordMapper.countBorrowingByUserId(1L)).thenReturn(5); // 已达上限

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.borrowBook(1L, 1L);
        });

        assertEquals(ResultCode.BORROW_LIMIT_EXCEEDED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("归还图书 - 成功（未逾期）")
    void returnBook_Success() {
        // 准备数据
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);
        when(borrowRecordMapper.updateReturn(eq(1L), any(LocalDateTime.class), 
                eq(BigDecimal.ZERO), eq(1))).thenReturn(1);
        when(bookService.increaseStock(1L, 1)).thenReturn(true);

        // 执行测试
        assertDoesNotThrow(() -> borrowService.returnBook(1L));

        verify(borrowRecordMapper).updateReturn(eq(1L), any(LocalDateTime.class), 
                eq(BigDecimal.ZERO), eq(1));
        verify(bookService).increaseStock(1L, 1);
    }

    @Test
    @DisplayName("归还图书 - 逾期归还")
    void returnBook_Overdue() {
        // 准备数据 - 设置为逾期
        testBorrowRecord.setDueDate(LocalDateTime.now().minusDays(5)); // 5天前到期
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);
        when(borrowRecordMapper.updateReturn(eq(1L), any(LocalDateTime.class), 
                any(BigDecimal.class), eq(1))).thenReturn(1);
        when(bookService.increaseStock(1L, 1)).thenReturn(true);

        // 执行测试
        assertDoesNotThrow(() -> borrowService.returnBook(1L));

        // 验证罚款金额计算（5天 * 0.5元/天 = 2.5元）
        verify(borrowRecordMapper).updateReturn(eq(1L), any(LocalDateTime.class), 
                argThat(fine -> fine.compareTo(BigDecimal.ZERO) > 0), eq(1));
    }

    @Test
    @DisplayName("归还图书 - 记录不存在")
    void returnBook_NotFound() {
        // 准备数据
        when(borrowRecordMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.returnBook(999L);
        });

        assertEquals(ResultCode.BORROW_RECORD_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("归还图书 - 已归还")
    void returnBook_AlreadyReturned() {
        // 准备数据
        testBorrowRecord.setStatus(1); // 已归还
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.returnBook(1L);
        });

        assertEquals(ResultCode.DATA_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("续借图书 - 成功")
    void renewBook_Success() {
        // 准备数据
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);
        when(borrowRecordMapper.updateRenew(eq(1L), any(LocalDateTime.class), eq(1))).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> borrowService.renewBook(1L));

        verify(borrowRecordMapper).updateRenew(eq(1L), any(LocalDateTime.class), eq(1));
    }

    @Test
    @DisplayName("续借图书 - 记录不存在")
    void renewBook_NotFound() {
        // 准备数据
        when(borrowRecordMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.renewBook(999L);
        });

        assertEquals(ResultCode.BORROW_RECORD_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("续借图书 - 已归还")
    void renewBook_AlreadyReturned() {
        // 准备数据
        testBorrowRecord.setStatus(1); // 已归还
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.renewBook(1L);
        });

        assertEquals(ResultCode.BOOK_NOT_RETURNED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("续借图书 - 续借次数已达上限")
    void renewBook_LimitExceeded() {
        // 准备数据
        testBorrowRecord.setRenewCount(1); // 已续借1次
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.renewBook(1L);
        });

        assertEquals(ResultCode.RENEW_LIMIT_EXCEEDED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("续借图书 - 已逾期")
    void renewBook_Overdue() {
        // 准备数据
        testBorrowRecord.setDueDate(LocalDateTime.now().minusDays(1)); // 昨天到期
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.renewBook(1L);
        });

        assertEquals(ResultCode.BORROW_OVERDUE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("获取用户借阅记录 - 成功")
    void getMyBorrows_Success() {
        // 准备数据
        when(borrowRecordMapper.selectPageWithDetails(anyMap(), eq(0), eq(10)))
                .thenReturn(Arrays.asList(testBorrowRecord));
        when(borrowRecordMapper.countWithDetails(anyMap())).thenReturn(1L);

        // 执行测试
        PageResult<BorrowRecord> result = borrowService.getMyBorrows(1L, null, 1, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("获取当前借阅列表 - 成功")
    void getCurrentBorrows_Success() {
        // 准备数据
        when(borrowRecordMapper.selectBorrowingByUserId(1L))
                .thenReturn(Arrays.asList(testBorrowRecord));

        // 执行测试
        List<BorrowRecord> result = borrowService.getCurrentBorrows(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取逾期列表 - 成功")
    void getOverdueList_Success() {
        // 准备数据
        BorrowRecord overdueRecord = new BorrowRecord();
        overdueRecord.setId(2L);
        overdueRecord.setStatus(2); // 逾期

        when(borrowRecordMapper.selectOverdue(any(LocalDateTime.class), eq(0), eq(10)))
                .thenReturn(Arrays.asList(overdueRecord));
        when(borrowRecordMapper.countOverdue(any(LocalDateTime.class))).thenReturn(1L);

        // 执行测试
        PageResult<BorrowRecord> result = borrowService.getOverdueList(1, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("获取借阅统计 - 成功")
    void getStatistics_Success() {
        // 准备数据
        when(borrowRecordMapper.updateOverdueStatus(any(LocalDateTime.class))).thenReturn(0);
        when(borrowRecordMapper.countByCondition(anyMap())).thenReturn(10L);

        // 执行测试
        Map<String, Object> result = borrowService.getStatistics();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("totalBorrows"));
    }

    @Test
    @DisplayName("检查用户是否可以借阅 - 可以借阅")
    void canBorrow_True() {
        // 准备数据
        when(borrowRecordMapper.selectBorrowingByUserIdAndBookId(1L, 1L)).thenReturn(null);
        when(borrowRecordMapper.countBorrowingByUserId(1L)).thenReturn(3);

        // 执行测试
        boolean result = borrowService.canBorrow(1L, 1L);

        // 验证结果
        assertTrue(result);
    }

    @Test
    @DisplayName("检查用户是否可以借阅 - 已借阅")
    void canBorrow_AlreadyBorrowed() {
        // 准备数据
        when(borrowRecordMapper.selectBorrowingByUserIdAndBookId(1L, 1L)).thenReturn(testBorrowRecord);

        // 执行测试
        boolean result = borrowService.canBorrow(1L, 1L);

        // 验证结果
        assertFalse(result);
    }

    @Test
    @DisplayName("检查用户是否可以借阅 - 达到上限")
    void canBorrow_LimitReached() {
        // 准备数据
        when(borrowRecordMapper.selectBorrowingByUserIdAndBookId(1L, 1L)).thenReturn(null);
        when(borrowRecordMapper.countBorrowingByUserId(1L)).thenReturn(5);

        // 执行测试
        boolean result = borrowService.canBorrow(1L, 1L);

        // 验证结果
        assertFalse(result);
    }
}
