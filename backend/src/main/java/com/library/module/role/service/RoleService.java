package com.library.module.role.service;

import com.library.common.base.BaseService;
import com.library.common.response.PageResult;
import com.library.module.role.dto.RoleDTO;
import com.library.module.role.entity.Permission;
import com.library.module.role.entity.Role;

import java.util.List;

/**
 * 角色Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface RoleService extends BaseService<Role> {

    /**
     * 分页查询角色
     */
    PageResult<Role> pageRoles(String roleName, int pageNum, int pageSize);

    /**
     * 创建角色
     */
    Long createRole(RoleDTO roleDTO);

    /**
     * 更新角色
     */
    void updateRole(Long id, RoleDTO roleDTO);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 获取角色权限ID列表
     */
    List<Long> getRolePermissions(Long roleId);

    /**
     * 分配角色权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 获取权限树
     */
    List<Permission> getPermissionTree();

    /**
     * 检查角色编码是否存在
     */
    boolean existsByRoleCode(String roleCode);
}
