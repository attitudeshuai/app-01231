package com.library.module.role.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 权限实体
 *
 * @author Library System
 * @since 1.0.0
 */
@Data
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 资源类型：menu-菜单 button-按钮
     */
    private String resourceType;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 子权限列表（非数据库字段）
     */
    private transient List<Permission> children;
}
