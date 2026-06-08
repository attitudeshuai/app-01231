package com.library.module.book.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存调整DTO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class StockDTO {

    /**
     * 调整类型：1-入库 2-出库
     */
    @NotNull(message = "调整类型不能为空")
    private Integer type;

    /**
     * 调整数量
     */
    @NotNull(message = "调整数量不能为空")
    private Integer quantity;

    /**
     * 备注
     */
    private String remark;
}
