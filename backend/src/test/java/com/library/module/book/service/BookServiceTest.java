package com.library.module.book.service;

import com.library.common.exception.BusinessException;
import com.library.common.response.PageResult;
import com.library.common.response.ResultCode;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.dto.BookQueryDTO;
import com.library.module.book.dto.StockDTO;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.book.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookService 单元测试
 *
 * @author Library System
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("图书服务单元测试")
class BookServiceTest {

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private BookDTO testBookDTO;

    @BeforeEach
    void setUp() {
        // 设置私有字段
        ReflectionTestUtils.setField(bookService, "uploadPath", "/tmp/uploads");
        ReflectionTestUtils.setField(bookService, "stockWarningThreshold", 5);
        ReflectionTestUtils.setField(bookService, "baseMapper", bookMapper);

        // 初始化测试数据
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("978-7-111-11111-1");
        testBook.setTitle("测试图书");
        testBook.setAuthor("测试作者");
        testBook.setPublisher("测试出版社");
        testBook.setPublishDate(LocalDate.of(2024, 1, 1));
        testBook.setPrice(new BigDecimal("99.00"));
        testBook.setCategoryId(1L);
        testBook.setTotalStock(100);
        testBook.setAvailableStock(80);
        testBook.setStatus(1);

        testBookDTO = new BookDTO();
        testBookDTO.setIsbn("978-7-111-22222-2");
        testBookDTO.setTitle("新增测试图书");
        testBookDTO.setAuthor("新作者");
        testBookDTO.setPublisher("新出版社");
        testBookDTO.setPublishDate(LocalDate.of(2024, 6, 1));
        testBookDTO.setPrice(new BigDecimal("59.00"));
        testBookDTO.setCategoryId(2L);
        testBookDTO.setTotalStock(50);
        testBookDTO.setStatus(1);
    }

    @Test
    @DisplayName("分页查询图书 - 成功")
    void pageBooks_Success() {
        // 准备数据
        BookQueryDTO queryDTO = new BookQueryDTO();
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10);
        queryDTO.setKeyword("测试");

        List<Book> books = Arrays.asList(testBook);

        when(bookMapper.selectPageWithCategory(anyMap(), eq(0), eq(10))).thenReturn(books);
        when(bookMapper.countByCondition(anyMap())).thenReturn(1L);

        // 执行测试
        PageResult<Book> result = bookService.pageBooks(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("测试图书", result.getList().get(0).getTitle());

        verify(bookMapper).selectPageWithCategory(anyMap(), eq(0), eq(10));
        verify(bookMapper).countByCondition(anyMap());
    }

    @Test
    @DisplayName("创建图书 - 成功")
    void createBook_Success() {
        // 准备数据
        when(bookMapper.selectByIsbn(testBookDTO.getIsbn())).thenReturn(null);
        when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(2L);
            return 1;
        });

        // 执行测试
        Long bookId = bookService.createBook(testBookDTO);

        // 验证结果
        assertNotNull(bookId);
        assertEquals(2L, bookId);

        verify(bookMapper).selectByIsbn(testBookDTO.getIsbn());
        verify(bookMapper).insert(any(Book.class));
    }

    @Test
    @DisplayName("创建图书 - ISBN已存在")
    void createBook_IsbnExists() {
        // 准备数据
        when(bookMapper.selectByIsbn(testBookDTO.getIsbn())).thenReturn(testBook);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.createBook(testBookDTO);
        });

        assertEquals(ResultCode.BOOK_ISBN_EXIST.getCode(), exception.getCode());
        verify(bookMapper).selectByIsbn(testBookDTO.getIsbn());
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    @DisplayName("更新图书 - 成功")
    void updateBook_Success() {
        // 准备数据
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle("更新后的标题");
        updateDTO.setAuthor("更新后的作者");

        // 执行测试
        assertDoesNotThrow(() -> bookService.updateBook(1L, updateDTO));

        // 验证结果
        verify(bookMapper).selectById(1L);
        verify(bookMapper).updateById(any(Book.class));
    }

    @Test
    @DisplayName("更新图书 - 图书不存在")
    void updateBook_NotFound() {
        // 准备数据
        when(bookMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.updateBook(999L, testBookDTO);
        });

        assertEquals(ResultCode.BOOK_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("更新图书 - ISBN被其他图书使用")
    void updateBook_IsbnConflict() {
        // 准备数据
        Book anotherBook = new Book();
        anotherBook.setId(2L);
        anotherBook.setIsbn("978-7-111-33333-3");

        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.selectByIsbn("978-7-111-33333-3")).thenReturn(anotherBook);

        BookDTO updateDTO = new BookDTO();
        updateDTO.setIsbn("978-7-111-33333-3");

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.updateBook(1L, updateDTO);
        });

        assertEquals(ResultCode.BOOK_ISBN_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("调整库存 - 入库成功")
    void adjustStock_IncrementSuccess() {
        // 准备数据
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateStock(eq(1L), anyInt(), anyInt())).thenReturn(1);

        StockDTO stockDTO = new StockDTO();
        stockDTO.setType(1); // 入库
        stockDTO.setQuantity(20);

        // 执行测试
        assertDoesNotThrow(() -> bookService.adjustStock(1L, stockDTO));

        // 验证调用 - 入库后总库存120，可用库存100
        verify(bookMapper).updateStock(eq(1L), eq(120), eq(100));
    }

    @Test
    @DisplayName("调整库存 - 出库成功")
    void adjustStock_DecrementSuccess() {
        // 准备数据
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateStock(eq(1L), anyInt(), anyInt())).thenReturn(1);

        StockDTO stockDTO = new StockDTO();
        stockDTO.setType(2); // 出库
        stockDTO.setQuantity(10);

        // 执行测试
        assertDoesNotThrow(() -> bookService.adjustStock(1L, stockDTO));

        // 验证调用 - 出库后总库存90，可用库存70
        verify(bookMapper).updateStock(eq(1L), eq(90), eq(70));
    }

    @Test
    @DisplayName("调整库存 - 库存不足")
    void adjustStock_InsufficientStock() {
        // 准备数据
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        StockDTO stockDTO = new StockDTO();
        stockDTO.setType(2); // 出库
        stockDTO.setQuantity(100); // 超过可用库存80

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.adjustStock(1L, stockDTO);
        });

        assertEquals(ResultCode.BOOK_STOCK_NOT_ENOUGH.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("减少可用库存 - 成功")
    void decreaseStock_Success() {
        // 准备数据
        when(bookMapper.decreaseAvailableStock(1L, 5)).thenReturn(1);

        // 执行测试
        boolean result = bookService.decreaseStock(1L, 5);

        // 验证结果
        assertTrue(result);
        verify(bookMapper).decreaseAvailableStock(1L, 5);
    }

    @Test
    @DisplayName("减少可用库存 - 失败")
    void decreaseStock_Failed() {
        // 准备数据
        when(bookMapper.decreaseAvailableStock(1L, 100)).thenReturn(0);

        // 执行测试
        boolean result = bookService.decreaseStock(1L, 100);

        // 验证结果
        assertFalse(result);
    }

    @Test
    @DisplayName("增加可用库存 - 成功")
    void increaseStock_Success() {
        // 准备数据
        when(bookMapper.increaseAvailableStock(1L, 5)).thenReturn(1);

        // 执行测试
        boolean result = bookService.increaseStock(1L, 5);

        // 验证结果
        assertTrue(result);
        verify(bookMapper).increaseAvailableStock(1L, 5);
    }

    @Test
    @DisplayName("查询库存预警图书 - 成功")
    void getStockWarning_Success() {
        // 准备数据
        Book lowStockBook = new Book();
        lowStockBook.setId(1L);
        lowStockBook.setTitle("低库存图书");
        lowStockBook.setAvailableStock(3);

        when(bookMapper.selectStockWarning(eq(5), eq(0), eq(10)))
                .thenReturn(Arrays.asList(lowStockBook));
        when(bookMapper.countStockWarning(5)).thenReturn(1L);

        // 执行测试
        PageResult<Book> result = bookService.getStockWarning(1, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(3, result.getList().get(0).getAvailableStock());
    }

    @Test
    @DisplayName("根据ISBN查询 - 成功")
    void getByIsbn_Success() {
        // 准备数据
        when(bookMapper.selectByIsbn("978-7-111-11111-1")).thenReturn(testBook);

        // 执行测试
        Book result = bookService.getByIsbn("978-7-111-11111-1");

        // 验证结果
        assertNotNull(result);
        assertEquals("测试图书", result.getTitle());
    }

    @Test
    @DisplayName("更新图书状态 - 成功")
    void updateStatus_Success() {
        // 准备数据
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateStatus(1L, 0)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> bookService.updateStatus(1L, 0));

        verify(bookMapper).updateStatus(1L, 0);
    }

    @Test
    @DisplayName("更新图书状态 - 图书不存在")
    void updateStatus_NotFound() {
        // 准备数据
        when(bookMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.updateStatus(999L, 0);
        });

        assertEquals(ResultCode.BOOK_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("批量导入图书 - 成功")
    void importBooks_Success() {
        // 准备数据
        BookDTO book1 = new BookDTO();
        book1.setIsbn("978-7-111-44444-4");
        book1.setTitle("导入图书1");
        book1.setAuthor("作者1");
        book1.setTotalStock(10);

        BookDTO book2 = new BookDTO();
        book2.setIsbn("978-7-111-55555-5");
        book2.setTitle("导入图书2");
        book2.setAuthor("作者2");
        book2.setTotalStock(20);

        when(bookMapper.selectByIsbn(anyString())).thenReturn(null);
        when(bookMapper.insert(any(Book.class))).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> bookService.importBooks(Arrays.asList(book1, book2)));

        verify(bookMapper, times(2)).insert(any(Book.class));
    }

    @Test
    @DisplayName("导出图书 - 成功")
    void exportBooks_Success() {
        // 准备数据
        BookQueryDTO queryDTO = new BookQueryDTO();
        queryDTO.setKeyword("测试");

        when(bookMapper.selectByCondition(anyMap())).thenReturn(Arrays.asList(testBook));

        // 执行测试
        List<Book> result = bookService.exportBooks(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
