package com.library.module.book.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书导出DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class BookExportDTO {

    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("书名")
    @ColumnWidth(30)
    private String title;

    @ExcelProperty("作者")
    @ColumnWidth(20)
    private String author;

    @ExcelProperty("ISBN")
    @ColumnWidth(20)
    private String isbn;

    @ExcelProperty("出版社")
    @ColumnWidth(25)
    private String publisher;

    @ExcelProperty("出版日期")
    @ColumnWidth(15)
    private LocalDate publishDate;

    @ExcelProperty("分类ID")
    @ColumnWidth(10)
    private Long categoryId;

    @ExcelProperty("价格")
    @ColumnWidth(10)
    private BigDecimal price;

    @ExcelProperty("总库存")
    @ColumnWidth(10)
    private Integer stock;

    @ExcelProperty("可用库存")
    @ColumnWidth(10)
    private Integer availableStock;

    @ExcelProperty("存放位置")
    @ColumnWidth(15)
    private String location;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;

    @ExcelProperty("简介")
    @ColumnWidth(50)
    private String description;
}
