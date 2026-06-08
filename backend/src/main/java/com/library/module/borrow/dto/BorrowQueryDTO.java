package com.library.module.borrow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅查询DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class BorrowQueryDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 图书标题
     */
    private String bookTitle;

    /**
     * 状态：0-借阅中 1-已归还 2-逾期
     */
    private Integer status;

    /**
     * 开始日期
     */
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    private LocalDateTime endDate;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
