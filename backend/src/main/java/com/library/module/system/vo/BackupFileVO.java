package com.library.module.system.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 备份文件VO
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
@Builder
public class BackupFileVO {

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 格式化的文件大小
     */
    private String sizeText;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 文件路径
     */
    private String path;
}
