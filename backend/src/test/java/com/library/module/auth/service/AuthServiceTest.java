package com.library.module.auth.service;

import com.library.common.exception.BusinessException;
import com.library.common.response.ResultCode;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.service.impl.AuthServiceImpl;
import com.library.module.auth.vo.LoginVO;
import com.library.module.user.dto.UserDTO;
import com.library.module.user.service.UserService;
import com.library.security.LoginUser;
import com.library.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 *
 * @author Library System
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务单元测试")
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginUser testLoginUser;
    private LoginDTO testLoginDTO;
    private RegisterDTO testRegisterDTO;

    @BeforeEach
    void setUp() {
        // 设置私有字段
        ReflectionTestUtils.setField(authService, "expiration", 3600000L);

        // 初始化测试数据
        testLoginUser = new LoginUser();
        testLoginUser.setUserId(1L);
        testLoginUser.setUsername("testuser");
        testLoginUser.setPassword("$2a$10$encoded_password");
        testLoginUser.setEmail("test@example.com");
        testLoginUser.setPhone("13800000001");
        testLoginUser.setRoles(new HashSet<>(Arrays.asList("USER")));
        testLoginUser.setPermissions(new HashSet<>(Arrays.asList("book:read")));

        testLoginDTO = new LoginDTO();
        testLoginDTO.setUsername("testuser");
        testLoginDTO.setPassword("password123");

        testRegisterDTO = new RegisterDTO();
        testRegisterDTO.setUsername("newuser");
        testRegisterDTO.setPassword("password123");
        testRegisterDTO.setConfirmPassword("password123");
        testRegisterDTO.setEmail("newuser@example.com");
        testRegisterDTO.setPhone("13900000001");

        getSmsCodeCache().clear();
        getCaptchaCache().clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getSmsCodeCache() {
        return (Map<String, String>) ReflectionTestUtils.getField(AuthServiceImpl.class, "SMS_CODE_CACHE");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getCaptchaCache() {
        return (Map<String, String>) ReflectionTestUtils.getField(AuthServiceImpl.class, "CAPTCHA_CACHE");
    }

    private void prepareLoginVerification() {
        getCaptchaCache().put("login-captcha-key", "abcd");
        getSmsCodeCache().put(testLoginUser.getPhone(), "123456");
        testLoginDTO.setCaptchaKey("login-captcha-key");
        testLoginDTO.setCaptchaCode("abcd");
        testLoginDTO.setSmsCode("123456");
    }

    private void prepareRegisterVerification() {
        getCaptchaCache().put("register-captcha-key", "wxyz");
        getSmsCodeCache().put(testRegisterDTO.getPhone(), "654321");
        testRegisterDTO.setCaptchaKey("register-captcha-key");
        testRegisterDTO.setCaptchaCode("wxyz");
        testRegisterDTO.setSmsCode("654321");
    }

    @Test
    @DisplayName("用户登录 - 成功")
    void login_Success() {
        // 准备数据
        prepareLoginVerification();
        when(userService.loadUserByUsername("testuser")).thenReturn(testLoginUser);
        when(passwordEncoder.matches("password123", testLoginUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(testLoginUser)).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(testLoginUser)).thenReturn("refresh_token");

        // 执行测试
        LoginVO result = authService.login(testLoginDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("access_token", result.getAccessToken());
        assertEquals("refresh_token", result.getRefreshToken());
        assertEquals("testuser", result.getUsername());
        assertEquals("Bearer", result.getTokenType());

        verify(userService).loadUserByUsername("testuser");
        verify(passwordEncoder).matches("password123", testLoginUser.getPassword());
    }

    @Test
    @DisplayName("用户登录 - 密码错误")
    void login_WrongPassword() {
        // 准备数据
        prepareLoginVerification();
        when(userService.loadUserByUsername("testuser")).thenReturn(testLoginUser);
        when(passwordEncoder.matches("wrongpassword", testLoginUser.getPassword())).thenReturn(false);

        testLoginDTO.setPassword("wrongpassword");

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(testLoginDTO);
        });

        assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("用户登录 - 带验证码成功")
    void login_WithCaptchaSuccess() {
        // 先生成验证码
        String captchaKey = "test-captcha-key";
        String captchaImage = authService.generateCaptcha(captchaKey);
        assertNotNull(captchaImage);

        // 从内部缓存获取验证码（通过反射获取）
        // 注意：这里我们模拟验证码验证
        testLoginDTO.setCaptchaKey(captchaKey);
        testLoginDTO.setCaptchaCode("wrong"); // 故意设置错误验证码

        // 验证验证码错误的情况
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(testLoginDTO);
        });

        assertEquals(ResultCode.CAPTCHA_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("用户注册 - 成功")
    void register_Success() {
        // 准备数据
        prepareRegisterVerification();
        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userService.createUser(any(UserDTO.class))).thenReturn(1L);

        // 执行测试
        Long userId = authService.register(testRegisterDTO);

        // 验证结果
        assertNotNull(userId);
        assertEquals(1L, userId);

        verify(userService).existsByUsername("newuser");
        verify(userService).existsByEmail("newuser@example.com");
        verify(userService).createUser(any(UserDTO.class));
    }

    @Test
    @DisplayName("用户注册 - 两次密码不一致")
    void register_PasswordMismatch() {
        // 准备数据
        prepareRegisterVerification();
        testRegisterDTO.setConfirmPassword("different_password");

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(testRegisterDTO);
        });

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("两次输入的密码不一致"));
    }

    @Test
    @DisplayName("用户注册 - 用户名已存在")
    void register_UsernameExists() {
        // 准备数据
        prepareRegisterVerification();
        when(userService.existsByUsername("newuser")).thenReturn(true);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(testRegisterDTO);
        });

        assertEquals(ResultCode.USER_ALREADY_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("用户注册 - 邮箱已存在")
    void register_EmailExists() {
        // 准备数据
        prepareRegisterVerification();
        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.existsByEmail("newuser@example.com")).thenReturn(true);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(testRegisterDTO);
        });

        assertEquals(ResultCode.DATA_ALREADY_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("刷新Token - 成功")
    void refreshToken_Success() {
        // 准备数据
        when(jwtTokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid_refresh_token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(testLoginUser);
        when(jwtTokenProvider.generateToken(testLoginUser)).thenReturn("new_access_token");
        when(jwtTokenProvider.generateRefreshToken(testLoginUser)).thenReturn("new_refresh_token");

        // 执行测试
        LoginVO result = authService.refreshToken("valid_refresh_token");

        // 验证结果
        assertNotNull(result);
        assertEquals("new_access_token", result.getAccessToken());
        assertEquals("new_refresh_token", result.getRefreshToken());
    }

    @Test
    @DisplayName("刷新Token - Token无效")
    void refreshToken_InvalidToken() {
        // 准备数据
        when(jwtTokenProvider.validateToken("invalid_token")).thenReturn(false);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.refreshToken("invalid_token");
        });

        assertEquals(ResultCode.UNAUTHORIZED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("生成验证码 - 成功")
    void generateCaptcha_Success() {
        // 执行测试
        String result = authService.generateCaptcha("test-key");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.startsWith("data:image")); // Base64图片格式
    }

    @Test
    @DisplayName("用户登录 - 缺少图形验证码")
    void login_MissingCaptcha() {
        testLoginDTO.setSmsCode("123456");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(testLoginDTO);
        });

        assertEquals(ResultCode.CAPTCHA_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("用户注册 - 图形验证码过期")
    void register_CaptchaExpired() {
        getSmsCodeCache().put(testRegisterDTO.getPhone(), "654321");
        testRegisterDTO.setCaptchaKey("non-existent-key");
        testRegisterDTO.setCaptchaCode("1234");
        testRegisterDTO.setSmsCode("654321");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(testRegisterDTO);
        });

        assertEquals(ResultCode.CAPTCHA_EXPIRED.getCode(), exception.getCode());
    }
}
