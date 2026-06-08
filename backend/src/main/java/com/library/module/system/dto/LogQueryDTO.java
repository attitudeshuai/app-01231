package com.library.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志查询DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class LogQueryDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 开始时间
     */
    private LocalDateTime startDate;

    /**
     * 结束时间
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
