package com.library.module.user.dto;

import lombok.Data;

/**
 * 用户查询DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class UserQueryDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

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
