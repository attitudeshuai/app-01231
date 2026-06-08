package com.library.module.role.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.role.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色编码查询
     */
    Role selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 获取角色的权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 分配角色权限
     */
    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 删除角色权限
     */
    int deleteRolePermissions(@Param("roleId") Long roleId);

    /**
     * 检查角色是否被使用
     */
    int countUsersByRoleId(@Param("roleId") Long roleId);
}
