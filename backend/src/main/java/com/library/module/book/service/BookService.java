package com.library.module.book.service;

import com.library.common.base.BaseService;
import com.library.common.response.PageResult;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.dto.BookQueryDTO;
import com.library.module.book.dto.StockDTO;
import com.library.module.book.entity.Book;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图书Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface BookService extends BaseService<Book> {

    /**
     * 分页查询图书
     */
    PageResult<Book> pageBooks(BookQueryDTO queryDTO);

    /**
     * 创建图书
     */
    Long createBook(BookDTO bookDTO);

    /**
     * 更新图书
     */
    void updateBook(Long id, BookDTO bookDTO);

    /**
     * 调整库存
     */
    void adjustStock(Long id, StockDTO stockDTO);

    /**
     * 减少可用库存
     */
    boolean decreaseStock(Long bookId, int count);

    /**
     * 增加可用库存
     */
    boolean increaseStock(Long bookId, int count);

    /**
     * 查询库存预警图书
     */
    PageResult<Book> getStockWarning(int pageNum, int pageSize);

    /**
     * 上传封面
     */
    String uploadCover(MultipartFile file);

    /**
     * 批量导入图书
     */
    void importBooks(List<BookDTO> books);

    /**
     * 导出图书
     */
    List<Book> exportBooks(BookQueryDTO queryDTO);

    /**
     * 根据ISBN查询
     */
    Book getByIsbn(String isbn);

    /**
     * 更新状态
     */
    void updateStatus(Long id, Integer status);
}
