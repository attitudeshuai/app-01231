package com.library.module.auth.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送短信验证码DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class SendSmsDTO {

    /**
     * 手机号（注册时使用）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 用户名（登录时使用，通过用户名自动获取绑定手机号）
     */
    private String username;
}
