package com.library.module.user.service;

import com.library.common.base.BaseService;
import com.library.common.response.PageResult;
import com.library.module.user.dto.UserDTO;
import com.library.module.user.dto.UserQueryDTO;
import com.library.module.user.entity.User;
import com.library.security.LoginUser;

import java.util.List;

/**
 * 用户Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface UserService extends BaseService<User> {

    /**
     * 根据用户名查询
     */
    User getByUsername(String username);

    /**
     * 加载用户信息（包含角色权限）
     */
    LoginUser loadUserByUsername(String username);

    /**
     * 分页查询用户
     */
    PageResult<User> pageUsers(UserQueryDTO queryDTO);

    /**
     * 创建用户
     */
    Long createUser(UserDTO userDTO);

    /**
     * 更新用户
     */
    void updateUser(Long id, UserDTO userDTO);

    /**
     * 修改用户状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 修改密码
     */
    void updatePassword(Long id, String oldPassword, String newPassword);

    /**
     * 重置密码
     */
    void resetPassword(Long id);

    /**
     * 分配角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
}
