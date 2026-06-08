package com.library.module.user.controller;

import com.library.aspect.OperationLog;
import com.library.common.response.PageResult;
import com.library.common.response.Result;
import com.library.common.util.SecurityUtil;
import com.library.module.user.dto.UserDTO;
import com.library.module.user.dto.UserQueryDTO;
import com.library.module.user.entity.User;
import com.library.module.user.service.UserService;
import com.library.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户Controller
 *
 * @author Library System
 * @since 1.0.0
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<User>> page(UserQueryDTO queryDTO) {
        PageResult<User> page = userService.pageUsers(queryDTO);
        // 脱敏处理
        page.getList().forEach(user -> user.setPassword(null));
        return Result.success(page);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "新增用户", type = OperationLog.OperationType.CREATE)
    public Result<Long> create(@Valid @RequestBody UserDTO userDTO) {
        Long userId = userService.createUser(userDTO);
        return Result.success(userId);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "更新用户", type = OperationLog.OperationType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        userService.updateUser(id, userDTO);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "删除用户", type = OperationLog.OperationType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "修改用户状态", type = OperationLog.OperationType.UPDATE)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        userService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "重置用户密码", type = OperationLog.OperationType.UPDATE)
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "分配用户角色", type = OperationLog.OperationType.UPDATE)
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        List<Long> roleIds = request.get("roleIds");
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public Result<LoginUser> getProfile() {
        LoginUser loginUser = SecurityUtil.getLoginUser();
        loginUser.setPassword(null);
        return Result.success(loginUser);
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/profile")
    @OperationLog(value = "更新个人信息", type = OperationLog.OperationType.UPDATE)
    public Result<Void> updateProfile(@RequestBody UserDTO userDTO) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.updateUser(userId, userDTO);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    @OperationLog(value = "修改密码", type = OperationLog.OperationType.UPDATE)
    public Result<Void> updatePassword(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success();
    }
}
