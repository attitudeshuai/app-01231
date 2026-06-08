package com.library.module.user.service;

import com.library.common.exception.BusinessException;
import com.library.common.response.ResultCode;
import com.library.module.user.dto.UserDTO;
import com.library.module.user.entity.User;
import com.library.module.user.mapper.UserMapper;
import com.library.module.user.service.impl.UserServiceImpl;
import com.library.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User existingUser;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("alice");
        existingUser.setPassword("encoded-old-password");
        existingUser.setEmail("alice@example.com");
        existingUser.setPhone("13800000001");
        existingUser.setAvatar("avatar.png");
        existingUser.setStatus(1);

        userDTO = new UserDTO();
        userDTO.setUsername("alice");
        userDTO.setPassword("password123");
        userDTO.setEmail("alice@example.com");
        userDTO.setPhone("13800000001");
        userDTO.setAvatar("avatar.png");
        userDTO.setStatus(1);
        userDTO.setRoleIds(List.of(2L));
    }

    @Test
    @DisplayName("loadUserByUsername returns login user")
    void loadUserByUsername_Success() {
        when(userMapper.selectByUsername("alice")).thenReturn(existingUser);
        when(userMapper.selectRolesByUserId(1L)).thenReturn(Set.of("USER"));
        when(userMapper.selectPermissionsByUserId(1L)).thenReturn(Set.of("book:read"));

        LoginUser loginUser = userService.loadUserByUsername("alice");

        assertEquals(1L, loginUser.getUserId());
        assertEquals("alice", loginUser.getUsername());
        assertEquals("13800000001", loginUser.getPhone());
        assertTrue(loginUser.getRoles().contains("USER"));
        assertTrue(loginUser.getPermissions().contains("book:read"));
    }

    @Test
    @DisplayName("loadUserByUsername throws when user not found")
    void loadUserByUsername_NotFound() {
        when(userMapper.selectByUsername("missing")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.loadUserByUsername("missing"));

        assertEquals(ResultCode.USER_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("loadUserByUsername throws when user disabled")
    void loadUserByUsername_Disabled() {
        existingUser.setStatus(0);
        when(userMapper.selectByUsername("alice")).thenReturn(existingUser);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.loadUserByUsername("alice"));

        assertEquals(ResultCode.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createUser creates user and assigns roles")
    void createUser_Success() {
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(userMapper.selectByEmail("alice@example.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

        Long userId = userService.createUser(userDTO);

        assertEquals(10L, userId);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("encoded-password", captor.getValue().getPassword());
        verify(userMapper).deleteUserRoles(10L);
        verify(userMapper).insertUserRole(10L, 2L);
    }

    @Test
    @DisplayName("createUser throws when username exists")
    void createUser_UsernameExists() {
        when(userMapper.selectByUsername("alice")).thenReturn(existingUser);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.createUser(userDTO));

        assertEquals(ResultCode.USER_ALREADY_EXIST.getCode(), exception.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("createUser throws when email exists")
    void createUser_EmailExists() {
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(userMapper.selectByEmail("alice@example.com")).thenReturn(existingUser);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.createUser(userDTO));

        assertEquals(ResultCode.DATA_ALREADY_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updatePassword updates encoded password")
    void updatePassword_Success() {
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(passwordEncoder.matches("old-pass", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new-pass");

        userService.updatePassword(1L, "old-pass", "new-pass");

        verify(userMapper).updatePassword(1L, "encoded-new-pass");
    }

    @Test
    @DisplayName("updatePassword throws when old password mismatch")
    void updatePassword_WrongOldPassword() {
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(passwordEncoder.matches("wrong-old", "encoded-old-password")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updatePassword(1L, "wrong-old", "new-pass"));

        assertEquals(ResultCode.OLD_PASSWORD_ERROR.getCode(), exception.getCode());
        verify(userMapper, never()).updatePassword(eq(1L), any());
    }

    @Test
    @DisplayName("resetPassword uses default password")
    void resetPassword_Success() {
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-default");

        userService.resetPassword(1L);

        verify(userMapper).updatePassword(1L, "encoded-default");
    }

    @Test
    @DisplayName("assignRoles deletes existing roles and inserts new ones")
    void assignRoles_Success() {
        userService.assignRoles(5L, List.of(1L, 2L));

        verify(userMapper).deleteUserRoles(5L);
        verify(userMapper).insertUserRole(5L, 1L);
        verify(userMapper).insertUserRole(5L, 2L);
    }

    @Test
    @DisplayName("existsByUsername and existsByEmail reflect mapper results")
    void existsChecks_Success() {
        when(userMapper.selectByUsername("alice")).thenReturn(existingUser);
        when(userMapper.selectByEmail("alice@example.com")).thenReturn(existingUser);

        assertTrue(userService.existsByUsername("alice"));
        assertTrue(userService.existsByEmail("alice@example.com"));
    }
}
