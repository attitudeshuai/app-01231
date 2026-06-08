package com.library.module.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class BookDTO {

    /**
     * ISBN
     */
    @NotBlank(message = "ISBN不能为空")
    private String isbn;

    /**
     * 书名
     */
    @NotBlank(message = "书名不能为空")
    private String title;

    /**
     * 作者
     */
    @NotBlank(message = "作者不能为空")
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版日期
     */
    private LocalDate publishDate;

    /**
     * 价格
     */
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal price;

    /**
     * 分类ID
     */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /**
     * 封面URL
     */
    private String coverUrl;

    /**
     * 图书简介
     */
    private String description;

    /**
     * 总库存
     */
    @Min(value = 0, message = "库存不能为负数")
    private Integer totalStock;

    /**
     * 馆藏位置
     */
    private String location;

    /**
     * 状态
     */
    private Integer status;
}
