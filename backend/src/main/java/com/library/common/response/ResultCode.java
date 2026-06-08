package com.library.common.response;

import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author Library System
 * @since 1.0.0
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "数据冲突"),

    // 服务端错误 5xx
    ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务错误 1xxx
    PARAM_ERROR(1001, "参数校验失败"),
    DATA_NOT_EXIST(1002, "数据不存在"),
    DATA_ALREADY_EXIST(1003, "数据已存在"),
    DATA_ERROR(1004, "数据异常"),

    // 用户相关 2xxx
    USER_NOT_EXIST(2001, "用户不存在"),
    USER_PASSWORD_ERROR(2002, "密码错误"),
    USER_DISABLED(2003, "用户已被禁用"),
    USER_ALREADY_EXIST(2004, "用户名已存在"),
    CAPTCHA_ERROR(2005, "验证码错误"),
    CAPTCHA_EXPIRED(2006, "验证码已过期"),
    OLD_PASSWORD_ERROR(2007, "原密码错误"),
    SMS_CODE_ERROR(2008, "短信验证码错误"),
    SMS_CODE_EXPIRED(2009, "短信验证码已过期"),
    SMS_SEND_TOO_FREQUENT(2010, "短信发送过于频繁，请稍后再试"),

    // 图书相关 3xxx
    BOOK_NOT_EXIST(3001, "图书不存在"),
    BOOK_STOCK_NOT_ENOUGH(3002, "库存不足"),
    BOOK_ISBN_EXIST(3003, "ISBN已存在"),

    // 借阅相关 4xxx
    BORROW_LIMIT_EXCEEDED(4001, "借阅数量已达上限"),
    BORROW_RECORD_NOT_EXIST(4002, "借阅记录不存在"),
    BOOK_ALREADY_BORROWED(4003, "该图书已被借阅"),
    RENEW_LIMIT_EXCEEDED(4004, "续借次数已达上限"),
    BOOK_NOT_RETURNED(4005, "图书尚未归还"),
    BORROW_OVERDUE(4006, "借阅已逾期，请先归还");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 消息
     */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
