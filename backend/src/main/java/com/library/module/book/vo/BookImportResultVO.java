package com.library.module.book.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 图书导入结果VO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
@Builder
public class BookImportResultVO {

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failCount;

    /**
     * 总数量
     */
    private int totalCount;

    /**
     * 错误信息列表
     */
    private List<String> errorMessages;
}
