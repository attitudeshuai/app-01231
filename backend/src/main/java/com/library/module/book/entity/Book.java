package com.library.module.book.entity;

import com.library.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书实体
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Book extends BaseEntity {

    /**
     * ISBN号
     */
    private String isbn;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
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
    private BigDecimal price;

    /**
     * 分类ID
     */
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
    private Integer totalStock;

    /**
     * 可用库存
     */
    private Integer availableStock;

    /**
     * 馆藏位置
     */
    private String location;

    /**
     * 状态：0-下架 1-上架
     */
    private Integer status;

    /**
     * 分类名称（非数据库字段）
     */
    private transient String categoryName;
}
