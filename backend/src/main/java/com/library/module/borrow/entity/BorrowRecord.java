package com.library.module.borrow.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅记录实体
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 借阅日期
     */
    private LocalDateTime borrowDate;

    /**
     * 应还日期
     */
    private LocalDateTime dueDate;

    /**
     * 实际归还日期
     */
    private LocalDateTime returnDate;

    /**
     * 续借次数
     */
    private Integer renewCount;

    /**
     * 罚款金额
     */
    private BigDecimal fineAmount;

    /**
     * 状态：0-借阅中 1-已归还 2-逾期
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // 非数据库字段，用于关联查询
    /**
     * 用户名
     */
    private transient String username;

    /**
     * 图书标题
     */
    private transient String bookTitle;

    /**
     * ISBN
     */
    private transient String isbn;

    /**
     * 作者
     */
    private transient String author;
}
