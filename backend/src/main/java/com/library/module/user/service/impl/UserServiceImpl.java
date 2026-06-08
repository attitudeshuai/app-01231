package com.library.module.user.service.impl;

import com.library.common.base.BaseServiceImpl;
import com.library.common.exception.BusinessException;
import com.library.common.response.PageResult;
import com.library.common.response.ResultCode;
import com.library.module.user.dto.UserDTO;
import com.library.module.user.dto.UserQueryDTO;
import com.library.module.user.entity.User;
import com.library.module.user.mapper.UserMapper;
import com.library.module.user.service.UserService;
import com.library.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户Service实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public User getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public LoginUser loadUserByUsername(String username) {
        User user = baseMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 获取角色和权限
        Set<String> roles = baseMapper.selectRolesByUserId(user.getId());
        Set<String> permissions = baseMapper.selectPermissionsByUserId(user.getId());

        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    @Override
    public PageResult<User> pageUsers(UserQueryDTO queryDTO) {
        Map<String, Object> params = new HashMap<>();
        if (queryDTO.getUsername() != null) {
            params.put("username", queryDTO.getUsername());
        }
        if (queryDTO.getEmail() != null) {
            params.put("email", queryDTO.getEmail());
        }
        if (queryDTO.getPhone() != null) {
            params.put("phone", queryDTO.getPhone());
        }
        if (queryDTO.getStatus() != null) {
            params.put("status", queryDTO.getStatus());
        }
        return page(params, queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserDTO userDTO) {
        // 检查用户名是否存在
        if (existsByUsername(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST);
        }

        // 检查邮箱是否存在
        if (userDTO.getEmail() != null && existsByEmail(userDTO.getEmail())) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "邮箱已被使用");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(
                userDTO.getPassword() != null ? userDTO.getPassword() : DEFAULT_PASSWORD));
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setAvatar(userDTO.getAvatar());
        user.setStatus(userDTO.getStatus() != null ? userDTO.getStatus() : 1);

        save(user);
        log.info("创建用户成功: {}", user.getUsername());

        // 分配角色
        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), userDTO.getRoleIds());
        }

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserDTO userDTO) {
        User existUser = getById(id);
        if (existUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 检查用户名是否被其他用户使用
        if (userDTO.getUsername() != null && !userDTO.getUsername().equals(existUser.getUsername())) {
            User byUsername = baseMapper.selectByUsername(userDTO.getUsername());
            if (byUsername != null) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXIST);
            }
            existUser.setUsername(userDTO.getUsername());
        }

        // 检查邮箱是否被其他用户使用
        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(existUser.getEmail())) {
            User byEmail = baseMapper.selectByEmail(userDTO.getEmail());
            if (byEmail != null) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "邮箱已被使用");
            }
            existUser.setEmail(userDTO.getEmail());
        }

        if (userDTO.getPhone() != null) {
            existUser.setPhone(userDTO.getPhone());
        }
        if (userDTO.getAvatar() != null) {
            existUser.setAvatar(userDTO.getAvatar());
        }
        if (userDTO.getStatus() != null) {
            existUser.setStatus(userDTO.getStatus());
        }

        updateById(existUser);
        log.info("更新用户成功: {}", existUser.getUsername());

        // 更新角色
        if (userDTO.getRoleIds() != null) {
            assignRoles(id, userDTO.getRoleIds());
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        baseMapper.updateStatus(id, status);
        log.info("更新用户状态: userId={}, status={}", id, status);
    }

    @Override
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        baseMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        log.info("用户修改密码成功: {}", user.getUsername());
    }

    @Override
    public void resetPassword(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        baseMapper.updatePassword(id, passwordEncoder.encode(DEFAULT_PASSWORD));
        log.info("重置用户密码成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 先删除原有角色
        baseMapper.deleteUserRoles(userId);
        // 添加新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                baseMapper.insertUserRole(userId, roleId);
            }
        }
        log.info("分配用户角色: userId={}, roleIds={}", userId, roleIds);
    }

    @Override
    public boolean existsByUsername(String username) {
        return baseMapper.selectByUsername(username) != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return baseMapper.selectByEmail(email) != null;
    }
}
