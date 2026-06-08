package com.library.module.user.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Set;

/**
 * 用户Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询
     */
    User selectByPhone(@Param("phone") String phone);

    /**
     * 获取用户角色编码列表
     */
    Set<String> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 获取用户权限编码列表
     */
    Set<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 分配用户角色
     */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 删除用户角色
     */
    int deleteUserRoles(@Param("userId") Long userId);
}
