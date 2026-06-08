package com.library.module.role.controller;

import com.library.aspect.OperationLog;
import com.library.common.response.PageResult;
import com.library.common.response.Result;
import com.library.module.role.dto.RoleDTO;
import com.library.module.role.entity.Permission;
import com.library.module.role.entity.Role;
import com.library.module.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色Controller
 *
 * @author Library System
 * @since 1.0.0
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping
    public Result<PageResult<Role>> page(
            @RequestParam(required = false) String roleName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Role> page = roleService.pageRoles(roleName, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取所有角色")
    @GetMapping("/all")
    public Result<List<Role>> listAll() {
        List<Role> roles = roleService.listAll();
        return Result.success(roles);
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        Role role = roleService.getById(id);
        return Result.success(role);
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @OperationLog(value = "新增角色", type = OperationLog.OperationType.CREATE)
    public Result<Long> create(@Valid @RequestBody RoleDTO roleDTO) {
        Long roleId = roleService.createRole(roleDTO);
        return Result.success(roleId);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @OperationLog(value = "更新角色", type = OperationLog.OperationType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        roleService.updateRole(id, roleDTO);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @OperationLog(value = "删除角色", type = OperationLog.OperationType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "获取角色权限")
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = roleService.getRolePermissions(id);
        return Result.success(permissionIds);
    }

    @Operation(summary = "分配角色权限")
    @PutMapping("/{id}/permissions")
    @OperationLog(value = "分配角色权限", type = OperationLog.OperationType.UPDATE)
    public Result<Void> assignPermissions(@PathVariable Long id, 
                                           @RequestBody Map<String, List<Long>> request) {
        List<Long> permissionIds = request.get("permissionIds");
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }

    @Operation(summary = "获取权限树")
    @GetMapping("/permissions")
    public Result<List<Permission>> getPermissionTree() {
        List<Permission> tree = roleService.getPermissionTree();
        return Result.success(tree);
    }
}
