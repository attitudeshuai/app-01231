package com.library.module.role.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.role.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 查询所有权限（按排序号排序）
     */
    List<Permission> selectAllOrdered();

    /**
     * 根据父级ID查询子权限
     */
    List<Permission> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据权限编码查询
     */
    Permission selectByPermissionCode(@Param("permissionCode") String permissionCode);

    /**
     * 根据角色ID列表查询权限
     */
    List<Permission> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}
