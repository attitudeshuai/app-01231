package com.library.module.book.controller;

import com.alibaba.excel.EasyExcel;
import com.library.aspect.OperationLog;
import com.library.common.response.PageResult;
import com.library.common.response.Result;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.dto.BookExportDTO;
import com.library.module.book.dto.BookImportDTO;
import com.library.module.book.dto.BookQueryDTO;
import com.library.module.book.dto.StockDTO;
import com.library.module.book.entity.Book;
import com.library.module.book.listener.BookImportListener;
import com.library.module.book.service.BookService;
import com.library.module.book.vo.BookImportResultVO;
import com.library.module.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图书Controller
 *
 * <p>提供图书管理相关的RESTful API，包括图书CRUD、封面上传、库存管理、
 * Excel批量导入导出等功能。管理员角色方可执行写操作。</p>
 *
 * <h3>封面上传与预览：</h3>
 * <ul>
 *   <li>上传接口：{@code POST /api/books/cover/upload}，支持图片文件上传</li>
 *   <li>预览方式：上传后返回图片URL（{@code /uploads/covers/...}），
 *     通过Nginx静态资源映射直接访问</li>
 * </ul>
 *
 * @author Library System
 * @since 1.0.0
 * @see BookService
 */
@Tag(name = "图书管理")
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @Operation(summary = "分页查询图书")
    @GetMapping
    public Result<PageResult<Book>> page(BookQueryDTO queryDTO) {
        PageResult<Book> page = bookService.pageBooks(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "获取图书详情")
    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Long id) {
        Book book = bookService.getById(id);
        return Result.success(book);
    }

    @Operation(summary = "新增图书")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "新增图书", type = OperationLog.OperationType.CREATE)
    public Result<Long> create(@Valid @RequestBody BookDTO bookDTO) {
        Long bookId = bookService.createBook(bookDTO);
        return Result.success(bookId);
    }

    @Operation(summary = "更新图书")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "更新图书", type = OperationLog.OperationType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BookDTO bookDTO) {
        bookService.updateBook(id, bookDTO);
        return Result.success();
    }

    @Operation(summary = "删除图书")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "删除图书", type = OperationLog.OperationType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        bookService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "调整库存")
    @PostMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "调整库存", type = OperationLog.OperationType.UPDATE)
    public Result<Void> adjustStock(@PathVariable Long id, @Valid @RequestBody StockDTO stockDTO) {
        bookService.adjustStock(id, stockDTO);
        return Result.success();
    }

    @Operation(summary = "库存预警列表")
    @GetMapping("/stock-warning")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Book>> stockWarning(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Book> page = bookService.getStockWarning(pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 上传图书封面图片
     *
     * <p>支持jpg/png等常见图片格式，文件按日期分目录存储，
     * 返回的URL可直接用于前端图片预览和图书信息保存。</p>
     *
     * @param file 封面图片文件
     * @return 封面图片的访问URL
     */
    @Operation(summary = "上传封面")
    @PostMapping("/cover/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = bookService.uploadCover(file);
        return Result.success(url);
    }

    @Operation(summary = "批量导入图书（JSON）")
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "批量导入图书", type = OperationLog.OperationType.IMPORT)
    public Result<Void> importBooks(@RequestBody List<BookDTO> books) {
        bookService.importBooks(books);
        return Result.success();
    }

    @Operation(summary = "批量导入图书（Excel）")
    @PostMapping("/import/excel")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "批量导入图书Excel", type = OperationLog.OperationType.IMPORT)
    public Result<BookImportResultVO> importBooksExcel(@RequestParam("file") MultipartFile file) {
        try {
            BookImportListener listener = new BookImportListener(bookService, categoryService);
            EasyExcel.read(file.getInputStream(), BookImportDTO.class, listener).sheet().doRead();

            BookImportResultVO result = BookImportResultVO.builder()
                    .successCount(listener.getSuccessCount())
                    .failCount(listener.getFailCount())
                    .totalCount(listener.getSuccessCount() + listener.getFailCount())
                    .errorMessages(listener.getErrorMessages())
                    .build();

            return Result.success(result);
        } catch (IOException e) {
            return Result.error("文件读取失败：" + e.getMessage());
        }
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("图书导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        // 写入示例数据
        List<BookImportDTO> templateData = new ArrayList<>();
        BookImportDTO example = new BookImportDTO();
        example.setTitle("示例书名");
        example.setAuthor("示例作者");
        example.setIsbn("978-7-111-12345-6");
        example.setPublisher("示例出版社");
        example.setPublishDate("2024-01-01");
        example.setCategoryName("计算机");
        example.setPrice(new java.math.BigDecimal("59.00"));
        example.setStock(100);
        example.setLocation("A区-1层-01架");
        example.setDescription("这是一本示例图书的简介");
        templateData.add(example);

        EasyExcel.write(response.getOutputStream(), BookImportDTO.class)
                .sheet("图书导入")
                .doWrite(templateData);
    }

    @Operation(summary = "导出图书")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "导出图书", type = OperationLog.OperationType.EXPORT)
    public Result<List<Book>> exportBooks(BookQueryDTO queryDTO) {
        List<Book> books = bookService.exportBooks(queryDTO);
        return Result.success(books);
    }

    @Operation(summary = "导出图书Excel")
    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "导出图书Excel", type = OperationLog.OperationType.EXPORT)
    public void exportBooksExcel(BookQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("图书列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        List<Book> books = bookService.exportBooks(queryDTO);
        List<BookExportDTO> exportList = new ArrayList<>();
        for (Book book : books) {
            BookExportDTO dto = new BookExportDTO();
            BeanUtils.copyProperties(book, dto);
            dto.setStatusText(book.getStatus() == 1 ? "上架" : "下架");
            exportList.add(dto);
        }

        EasyExcel.write(response.getOutputStream(), BookExportDTO.class)
                .sheet("图书列表")
                .doWrite(exportList);
    }

    @Operation(summary = "更新图书状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "更新图书状态", type = OperationLog.OperationType.UPDATE)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        bookService.updateStatus(id, status);
        return Result.success();
    }
}
