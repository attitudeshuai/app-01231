package com.library.module.book.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 图书导入DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class BookImportDTO {

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
    private String publishDate;

    @ExcelProperty("分类名称")
    @ColumnWidth(15)
    private String categoryName;

    @ExcelProperty("价格")
    @ColumnWidth(10)
    private BigDecimal price;

    @ExcelProperty("总库存")
    @ColumnWidth(10)
    private Integer stock;

    @ExcelProperty("存放位置")
    @ColumnWidth(15)
    private String location;

    @ExcelProperty("简介")
    @ColumnWidth(50)
    private String description;
}
