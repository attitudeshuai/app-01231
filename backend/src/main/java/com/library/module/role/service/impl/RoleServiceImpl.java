package com.library.module.role.service.impl;

import com.library.common.base.BaseServiceImpl;
import com.library.common.exception.BusinessException;
import com.library.common.response.PageResult;
import com.library.common.response.ResultCode;
import com.library.module.role.dto.RoleDTO;
import com.library.module.role.entity.Permission;
import com.library.module.role.entity.Role;
import com.library.module.role.mapper.PermissionMapper;
import com.library.module.role.mapper.RoleMapper;
import com.library.module.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色Service实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends BaseServiceImpl<RoleMapper, Role> implements RoleService {

    private final PermissionMapper permissionMapper;

    @Override
    public PageResult<Role> pageRoles(String roleName, int pageNum, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        if (roleName != null) {
            params.put("roleName", roleName);
        }
        return page(params, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleDTO roleDTO) {
        // 检查角色编码是否存在
        if (existsByRoleCode(roleDTO.getRoleCode())) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "角色编码已存在");
        }

        Role role = new Role();
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleCode(roleDTO.getRoleCode());
        role.setDescription(roleDTO.getDescription());

        save(role);
        log.info("创建角色成功: {}", role.getRoleName());

        // 分配权限
        if (roleDTO.getPermissionIds() != null && !roleDTO.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), roleDTO.getPermissionIds());
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleDTO roleDTO) {
        Role existRole = getById(id);
        if (existRole == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }

        // 检查角色编码是否被其他角色使用
        if (roleDTO.getRoleCode() != null && !roleDTO.getRoleCode().equals(existRole.getRoleCode())) {
            Role byCode = baseMapper.selectByRoleCode(roleDTO.getRoleCode());
            if (byCode != null) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "角色编码已存在");
            }
            existRole.setRoleCode(roleDTO.getRoleCode());
        }

        if (roleDTO.getRoleName() != null) {
            existRole.setRoleName(roleDTO.getRoleName());
        }
        if (roleDTO.getDescription() != null) {
            existRole.setDescription(roleDTO.getDescription());
        }

        updateById(existRole);
        log.info("更新角色成功: {}", existRole.getRoleName());

        // 更新权限
        if (roleDTO.getPermissionIds() != null) {
            assignPermissions(id, roleDTO.getPermissionIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }

        // 检查角色是否被使用
        int userCount = baseMapper.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(ResultCode.DATA_ERROR, "该角色已分配给用户，无法删除");
        }

        // 删除角色权限关联
        baseMapper.deleteRolePermissions(id);
        // 删除角色
        removeById(id);
        log.info("删除角色成功: {}", role.getRoleName());
    }

    @Override
    public List<Long> getRolePermissions(Long roleId) {
        return baseMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 先删除原有权限
        baseMapper.deleteRolePermissions(roleId);
        // 添加新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                baseMapper.insertRolePermission(roleId, permissionId);
            }
        }
        log.info("分配角色权限: roleId={}, permissionIds={}", roleId, permissionIds);
    }

    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> allPermissions = permissionMapper.selectAllOrdered();
        return buildPermissionTree(allPermissions, 0L);
    }

    /**
     * 构建权限树
     */
    private List<Permission> buildPermissionTree(List<Permission> permissions, Long parentId) {
        Map<Long, List<Permission>> groupByParent = permissions.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));

        List<Permission> roots = groupByParent.getOrDefault(parentId, new ArrayList<>());
        for (Permission permission : roots) {
            permission.setChildren(buildPermissionTree(permissions, permission.getId()));
        }
        return roots;
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        return baseMapper.selectByRoleCode(roleCode) != null;
    }
}
