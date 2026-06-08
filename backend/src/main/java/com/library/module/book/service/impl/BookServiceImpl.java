package com.library.module.book.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.library.common.base.BaseServiceImpl;
import com.library.common.exception.BusinessException;
import com.library.common.response.PageResult;
import com.library.common.response.ResultCode;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.dto.BookQueryDTO;
import com.library.module.book.dto.StockDTO;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.book.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书Service实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends BaseServiceImpl<BookMapper, Book> implements BookService {

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    @Value("${library.stock-warning-threshold:5}")
    private int stockWarningThreshold;

    @Override
    public PageResult<Book> pageBooks(BookQueryDTO queryDTO) {
        Map<String, Object> params = new HashMap<>();
        if (queryDTO.getKeyword() != null) {
            params.put("keyword", queryDTO.getKeyword());
        }
        if (queryDTO.getTitle() != null) {
            params.put("title", queryDTO.getTitle());
        }
        if (queryDTO.getAuthor() != null) {
            params.put("author", queryDTO.getAuthor());
        }
        if (queryDTO.getIsbn() != null) {
            params.put("isbn", queryDTO.getIsbn());
        }
        if (queryDTO.getCategoryId() != null) {
            params.put("categoryId", queryDTO.getCategoryId());
        }
        if (queryDTO.getStatus() != null) {
            params.put("status", queryDTO.getStatus());
        }

        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        List<Book> list = baseMapper.selectPageWithCategory(params, offset, queryDTO.getPageSize());
        long total = baseMapper.countByCondition(params);

        return PageResult.of(list, total, queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBook(BookDTO bookDTO) {
        // 检查ISBN是否已存在
        if (baseMapper.selectByIsbn(bookDTO.getIsbn()) != null) {
            throw new BusinessException(ResultCode.BOOK_ISBN_EXIST);
        }

        Book book = new Book();
        book.setIsbn(bookDTO.getIsbn());
        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setPublisher(bookDTO.getPublisher());
        book.setPublishDate(bookDTO.getPublishDate());
        book.setPrice(bookDTO.getPrice());
        book.setCategoryId(bookDTO.getCategoryId());
        book.setCoverUrl(bookDTO.getCoverUrl());
        book.setDescription(bookDTO.getDescription());
        book.setTotalStock(bookDTO.getTotalStock() != null ? bookDTO.getTotalStock() : 0);
        book.setAvailableStock(book.getTotalStock());
        book.setLocation(bookDTO.getLocation());
        book.setStatus(bookDTO.getStatus() != null ? bookDTO.getStatus() : 1);

        save(book);
        log.info("创建图书成功: {}", book.getTitle());

        return book.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBook(Long id, BookDTO bookDTO) {
        Book existBook = getById(id);
        if (existBook == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }

        // 检查ISBN是否被其他图书使用
        if (bookDTO.getIsbn() != null && !bookDTO.getIsbn().equals(existBook.getIsbn())) {
            Book byIsbn = baseMapper.selectByIsbn(bookDTO.getIsbn());
            if (byIsbn != null) {
                throw new BusinessException(ResultCode.BOOK_ISBN_EXIST);
            }
            existBook.setIsbn(bookDTO.getIsbn());
        }

        if (bookDTO.getTitle() != null) {
            existBook.setTitle(bookDTO.getTitle());
        }
        if (bookDTO.getAuthor() != null) {
            existBook.setAuthor(bookDTO.getAuthor());
        }
        if (bookDTO.getPublisher() != null) {
            existBook.setPublisher(bookDTO.getPublisher());
        }
        if (bookDTO.getPublishDate() != null) {
            existBook.setPublishDate(bookDTO.getPublishDate());
        }
        if (bookDTO.getPrice() != null) {
            existBook.setPrice(bookDTO.getPrice());
        }
        if (bookDTO.getCategoryId() != null) {
            existBook.setCategoryId(bookDTO.getCategoryId());
        }
        if (bookDTO.getCoverUrl() != null) {
            existBook.setCoverUrl(bookDTO.getCoverUrl());
        }
        if (bookDTO.getDescription() != null) {
            existBook.setDescription(bookDTO.getDescription());
        }
        if (bookDTO.getLocation() != null) {
            existBook.setLocation(bookDTO.getLocation());
        }
        if (bookDTO.getStatus() != null) {
            existBook.setStatus(bookDTO.getStatus());
        }

        updateById(existBook);
        log.info("更新图书成功: {}", existBook.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long id, StockDTO stockDTO) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }

        int newTotalStock = book.getTotalStock();
        int newAvailableStock = book.getAvailableStock();

        if (stockDTO.getType() == 1) {
            // 入库
            newTotalStock += stockDTO.getQuantity();
            newAvailableStock += stockDTO.getQuantity();
        } else if (stockDTO.getType() == 2) {
            // 出库
            if (newAvailableStock < stockDTO.getQuantity()) {
                throw new BusinessException(ResultCode.BOOK_STOCK_NOT_ENOUGH);
            }
            newTotalStock -= stockDTO.getQuantity();
            newAvailableStock -= stockDTO.getQuantity();
        }

        baseMapper.updateStock(id, newTotalStock, newAvailableStock);
        log.info("调整库存: bookId={}, type={}, quantity={}", id, stockDTO.getType(), stockDTO.getQuantity());
    }

    @Override
    public boolean decreaseStock(Long bookId, int count) {
        int result = baseMapper.decreaseAvailableStock(bookId, count);
        return result > 0;
    }

    @Override
    public boolean increaseStock(Long bookId, int count) {
        int result = baseMapper.increaseAvailableStock(bookId, count);
        return result > 0;
    }

    @Override
    public PageResult<Book> getStockWarning(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Book> list = baseMapper.selectStockWarning(stockWarningThreshold, offset, pageSize);
        long total = baseMapper.countStockWarning(stockWarningThreshold);
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public String uploadCover(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择文件");
        }

        // 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";

        // 生成文件名
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = IdUtil.simpleUUID() + suffix;
        String relativePath = "covers/" + dateDir + "/" + fileName;
        String fullPath = uploadPath + "/" + relativePath;

        try {
            // 创建目录
            File destFile = new File(fullPath);
            FileUtil.mkParentDirs(destFile);
            // 保存文件
            file.transferTo(destFile);
            log.info("上传封面成功: {}", relativePath);
            return "/uploads/" + relativePath;
        } catch (Exception e) {
            log.error("上传封面失败", e);
            throw new BusinessException(ResultCode.ERROR, "文件上传失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importBooks(List<BookDTO> books) {
        for (BookDTO bookDTO : books) {
            // 检查ISBN是否已存在
            if (baseMapper.selectByIsbn(bookDTO.getIsbn()) != null) {
                log.warn("导入时跳过已存在的ISBN: {}", bookDTO.getIsbn());
                continue;
            }
            createBook(bookDTO);
        }
        log.info("批量导入图书完成，共{}条", books.size());
    }

    @Override
    public List<Book> exportBooks(BookQueryDTO queryDTO) {
        Map<String, Object> params = new HashMap<>();
        if (queryDTO.getKeyword() != null) {
            params.put("keyword", queryDTO.getKeyword());
        }
        if (queryDTO.getCategoryId() != null) {
            params.put("categoryId", queryDTO.getCategoryId());
        }
        if (queryDTO.getStatus() != null) {
            params.put("status", queryDTO.getStatus());
        }
        return baseMapper.selectByCondition(params);
    }

    @Override
    public Book getByIsbn(String isbn) {
        return baseMapper.selectByIsbn(isbn);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }
        baseMapper.updateStatus(id, status);
        log.info("更新图书状态: bookId={}, status={}", id, status);
    }
}
