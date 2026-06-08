package com.library.module.book.dto;

import lombok.Data;

/**
 * 图书查询DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class BookQueryDTO {

    /**
     * 关键词（书名/作者/ISBN）
     */
    private String keyword;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
