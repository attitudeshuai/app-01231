package com.library.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.library.common.exception.BusinessException;
import com.library.common.response.ResultCode;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.service.AuthService;
import com.library.module.auth.vo.LoginVO;
import com.library.module.user.dto.UserDTO;
import com.library.module.user.service.UserService;
import com.library.security.LoginUser;
import com.library.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证Service实现
 *
 * <p>提供用户登录、注册、图形验证码生成与校验、短信验证码发送与校验等功能。</p>
 *
 * <h3>验证码机制：</h3>
 * <ul>
 *   <li><b>图形验证码</b>：使用Hutool的{@link cn.hutool.captcha.CaptchaUtil}生成线性干扰验证码，
 *       返回Base64编码图片，校验时不区分大小写，一次性使用</li>
 *   <li><b>短信验证码</b>：生成6位随机数字验证码，60秒内不可重复发送，
 *       开发环境的验证码输出到日志并返回前端展示</li>
 * </ul>
 *
 * <h3>登录流程：</h3>
 * <ol>
 *   <li>校验图形验证码</li>
 *   <li>校验短信验证码</li>
 *   <li>校验用户名和密码</li>
 *   <li>生成JWT Token（accessToken + refreshToken）</li>
 * </ol>
 *
 * @author Library System
 * @since 1.0.0
 * @see AuthService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private long expiration;

    // 短信验证码存储（生产环境应使用Redis并设置过期时间）
    private static final Map<String, String> SMS_CODE_CACHE = new ConcurrentHashMap<>();
    // 短信发送时间记录（用于频率限制）
    private static final Map<String, Long> SMS_SEND_TIME_CACHE = new ConcurrentHashMap<>();
    // 图形验证码存储
    private static final Map<String, String> CAPTCHA_CACHE = new ConcurrentHashMap<>();

    /**
     * 发送短信验证码
     *
     * <p>支持两种场景：</p>
     * <ul>
     *   <li>登录场景：通过username查找绑定手机号发送</li>
     *   <li>注册场景：直接使用传入的手机号发送</li>
     * </ul>
     *
     * @param phone    手机号（注册场景使用）
     * @param username 用户名（登录场景使用，用于查找绑定手机号）
     * @return 字符串数组 [0]=脱敏手机号, [1]=验证码（开发环境供前端展示）
     * @throws BusinessException 手机号未绑定或发送过于频繁时抛出
     */
    @Override
    public String[] sendSmsCode(String phone, String username) {
        // 根据参数确定实际手机号
        String targetPhone;
        if (StrUtil.isNotBlank(username)) {
            // 登录场景：通过用户名查找绑定手机号
            LoginUser loginUser = userService.loadUserByUsername(username);
            targetPhone = loginUser.getPhone();
            if (StrUtil.isBlank(targetPhone)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该账号未绑定手机号，无法发送验证码");
            }
        } else if (StrUtil.isNotBlank(phone)) {
            // 注册场景：直接使用传入的手机号
            targetPhone = phone;
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请提供手机号或用户名");
        }

        // 频率限制：60秒内不能重复发送
        Long lastSendTime = SMS_SEND_TIME_CACHE.get(targetPhone);
        if (lastSendTime != null && System.currentTimeMillis() - lastSendTime < 60000) {
            throw new BusinessException(ResultCode.SMS_SEND_TOO_FREQUENT);
        }

        // 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 存储验证码
        SMS_CODE_CACHE.put(targetPhone, code);
        SMS_SEND_TIME_CACHE.put(targetPhone, System.currentTimeMillis());

        // TODO: 生产环境应接入短信服务商（如阿里云短信、腾讯云短信）发送验证码
        // 开发环境直接打印到日志并返回验证码供前端展示
        log.info("【短信验证码】手机号: {}, 验证码: {} （开发环境模拟，未实际发送短信）", targetPhone, code);

        // 返回 [脱敏手机号, 验证码]
        return new String[]{maskPhone(targetPhone), code};
    }

    /**
     * 手机号脱敏，如 13800000001 -> 138****0001
     */
    private String maskPhone(String phone) {
        if (phone != null && phone.length() >= 7) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }

    /**
     * 验证短信验证码
     */
    private void verifySmsCode(String phone, String smsCode) {
        String cachedCode = SMS_CODE_CACHE.get(phone);
        if (cachedCode == null) {
            throw new BusinessException(ResultCode.SMS_CODE_EXPIRED);
        }
        if (!cachedCode.equals(smsCode)) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        // 验证成功后移除验证码（一次性使用）
        SMS_CODE_CACHE.remove(phone);
    }

    /**
     * 用户登录
     *
     * <p>登录流程：图形验证码校验 → 短信验证码校验 → 密码校验 → 生成JWT Token</p>
     *
     * @param loginDTO 登录参数（用户名、密码、图形验证码、短信验证码）
     * @return 登录结果，包含JWT Token和用户信息
     * @throws BusinessException 验证码错误、密码错误等情况抛出
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 校验图形验证码
        verifyCaptcha(loginDTO.getCaptchaKey(), loginDTO.getCaptchaCode());

        // 加载用户信息
        LoginUser loginUser = userService.loadUserByUsername(loginDTO.getUsername());

        // 通过用户名获取绑定手机号，验证短信验证码
        String phone = loginUser.getPhone();
        if (StrUtil.isBlank(phone)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该账号未绑定手机号");
        }
        verifySmsCode(phone, loginDTO.getSmsCode());

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), loginUser.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 生成Token
        String accessToken = jwtTokenProvider.generateToken(loginUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(loginUser);

        log.info("用户登录成功: {}", loginUser.getUsername());

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .userId(loginUser.getUserId())
                .username(loginUser.getUsername())
                .email(loginUser.getEmail())
                .avatar(loginUser.getAvatar())
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .build();
    }

    /**
     * 用户注册
     *
     * <p>注册流程：图形验证码校验 → 短信验证码校验 → 密码一致性检查 → 用户名/邮箱唯一性检查 → 创建用户</p>
     *
     * @param registerDTO 注册参数（用户名、密码、邮箱、手机号、验证码等）
     * @return 新创建的用户ID
     * @throws BusinessException 验证码错误、用户名已存在、密码不一致等情况抛出
     */
    @Override
    public Long register(RegisterDTO registerDTO) {
        verifyCaptcha(registerDTO.getCaptchaKey(), registerDTO.getCaptchaCode());

        // 验证短信验证码
        verifySmsCode(registerDTO.getPhone(), registerDTO.getSmsCode());

        // 验证两次密码一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 检查用户名是否存在
        if (userService.existsByUsername(registerDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST);
        }

        // 检查邮箱是否存在
        if (StrUtil.isNotBlank(registerDTO.getEmail()) && userService.existsByEmail(registerDTO.getEmail())) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "邮箱已被使用");
        }

        // 创建用户
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(registerDTO.getUsername());
        userDTO.setPassword(registerDTO.getPassword());
        userDTO.setEmail(registerDTO.getEmail());
        userDTO.setPhone(registerDTO.getPhone());
        userDTO.setStatus(1);
        // 默认分配普通用户角色（roleId=2）
        userDTO.setRoleIds(Collections.singletonList(2L));

        Long userId = userService.createUser(userDTO);
        log.info("用户注册成功: {}", registerDTO.getUsername());

        return userId;
    }

    /**
     * 生成图形验证码
     *
     * <p>使用Hutool生成120x40像素、4位字符的线性干扰验证码图片，
     * 返回Base64编码字符串供前端直接展示。验证码存储在内存中，一次性使用。</p>
     *
     * @param key 验证码唯一标识（通常为UUID）
     * @return Base64编码的验证码图片数据
     */
    @Override
    public String generateCaptcha(String key) {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 60);
        CAPTCHA_CACHE.put(key, captcha.getCode().toLowerCase());
        log.debug("生成图形验证码, key={}, code={}", key, captcha.getCode());
        return captcha.getImageBase64Data();
    }

    /**
     * 校验图形验证码
     *
     * <p>不区分大小写比较，验证成功后自动删除缓存（一次性使用）。</p>
     *
     * @param key  验证码唯一标识
     * @param code 用户输入的验证码
     * @throws BusinessException 验证码为空、过期或错误时抛出
     */
    private void verifyCaptcha(String key, String code) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR, "请输入图形验证码");
        }
        String cachedCode = CAPTCHA_CACHE.remove(key);
        if (cachedCode == null) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED, "图形验证码已过期，请刷新");
        }
        if (!cachedCode.equals(code.toLowerCase())) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR, "图形验证码错误");
        }
    }

    /**
     * 刷新Token
     *
     * <p>使用refreshToken重新生成accessToken和refreshToken。</p>
     *
     * @param refreshToken 刷新令牌
     * @return 新的登录凭证，包含新的accessToken和refreshToken
     * @throws BusinessException Token无效或过期时抛出
     */
    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 验证刷新Token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token已过期，请重新登录");
        }

        // 获取用户信息
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        LoginUser loginUser = userService.loadUserByUsername(username);

        // 生成新Token
        String newAccessToken = jwtTokenProvider.generateToken(loginUser);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(loginUser);

        log.info("刷新Token成功: {}", username);

        return LoginVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .userId(loginUser.getUserId())
                .username(loginUser.getUsername())
                .email(loginUser.getEmail())
                .avatar(loginUser.getAvatar())
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .build();
    }
}
