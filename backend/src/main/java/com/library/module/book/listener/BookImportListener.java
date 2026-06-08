package com.library.module.book.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.dto.BookImportDTO;
import com.library.module.book.service.BookService;
import com.library.module.category.entity.Category;
import com.library.module.category.service.CategoryService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图书导入监听器
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
public class BookImportListener extends AnalysisEventListener<BookImportDTO> {

    private static final int BATCH_SIZE = 100;

    private final BookService bookService;
    private final CategoryService categoryService;
    private final List<BookDTO> batchList = new ArrayList<>();
    private Map<String, Long> categoryMap;

    @Getter
    private int successCount = 0;
    @Getter
    private int failCount = 0;
    @Getter
    private final List<String> errorMessages = new ArrayList<>();

    public BookImportListener(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        initCategoryMap();
    }

    private void initCategoryMap() {
        List<Category> categories = categoryService.getCategoryTree();
        categoryMap = flattenCategories(categories).stream()
                .collect(Collectors.toMap(Category::getName, Category::getId, (k1, k2) -> k1));
    }

    private List<Category> flattenCategories(List<Category> categories) {
        List<Category> result = new ArrayList<>();
        for (Category category : categories) {
            result.add(category);
            if (category.getChildren() != null && !category.getChildren().isEmpty()) {
                result.addAll(flattenCategories(category.getChildren()));
            }
        }
        return result;
    }

    @Override
    public void invoke(BookImportDTO data, AnalysisContext context) {
        int rowNum = context.readRowHolder().getRowIndex() + 1;

        // 数据校验
        String error = validateData(data, rowNum);
        if (error != null) {
            failCount++;
            errorMessages.add(error);
            return;
        }

        // 转换为 BookDTO
        BookDTO bookDTO = convertToBookDTO(data);
        if (bookDTO == null) {
            failCount++;
            errorMessages.add("第" + rowNum + "行：分类名称不存在：" + data.getCategoryName());
            return;
        }

        batchList.add(bookDTO);

        // 批量处理
        if (batchList.size() >= BATCH_SIZE) {
            saveData();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理剩余数据
        if (!batchList.isEmpty()) {
            saveData();
        }
        log.info("图书导入完成，成功：{}，失败：{}", successCount, failCount);
    }

    private String validateData(BookImportDTO data, int rowNum) {
        if (StrUtil.isBlank(data.getTitle())) {
            return "第" + rowNum + "行：书名不能为空";
        }
        if (StrUtil.isBlank(data.getIsbn())) {
            return "第" + rowNum + "行：ISBN不能为空";
        }
        if (data.getPrice() == null || data.getPrice().doubleValue() < 0) {
            return "第" + rowNum + "行：价格无效";
        }
        if (data.getStock() == null || data.getStock() < 0) {
            return "第" + rowNum + "行：库存无效";
        }
        // 检查ISBN是否已存在
        if (bookService.getByIsbn(data.getIsbn()) != null) {
            return "第" + rowNum + "行：ISBN已存在：" + data.getIsbn();
        }
        return null;
    }

    private BookDTO convertToBookDTO(BookImportDTO data) {
        BookDTO bookDTO = new BookDTO();
        bookDTO.setTitle(data.getTitle());
        bookDTO.setAuthor(data.getAuthor());
        bookDTO.setIsbn(data.getIsbn());
        bookDTO.setPublisher(data.getPublisher());

        // 解析出版日期
        if (StrUtil.isNotBlank(data.getPublishDate())) {
            try {
                bookDTO.setPublishDate(LocalDate.parse(data.getPublishDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (Exception e) {
                try {
                    bookDTO.setPublishDate(LocalDate.parse(data.getPublishDate(), DateTimeFormatter.ofPattern("yyyy/MM/dd")));
                } catch (Exception e2) {
                    log.warn("日期解析失败: {}", data.getPublishDate());
                }
            }
        }

        // 解析分类
        if (StrUtil.isNotBlank(data.getCategoryName())) {
            Long categoryId = categoryMap.get(data.getCategoryName());
            if (categoryId == null) {
                return null;
            }
            bookDTO.setCategoryId(categoryId);
        }

        bookDTO.setPrice(data.getPrice());
        bookDTO.setTotalStock(data.getStock());
        bookDTO.setLocation(data.getLocation());
        bookDTO.setDescription(data.getDescription());
        bookDTO.setStatus(1); // 默认上架

        return bookDTO;
    }

    private void saveData() {
        try {
            bookService.importBooks(batchList);
            successCount += batchList.size();
        } catch (Exception e) {
            log.error("批量导入图书失败", e);
            failCount += batchList.size();
            errorMessages.add("批量保存失败：" + e.getMessage());
        }
        batchList.clear();
    }
}
