package com.library.module.auth.service;

import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.vo.LoginVO;

/**
 * 认证Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 发送短信验证码
     * @return [脱敏手机号, 验证码]
     */
    String[] sendSmsCode(String phone, String username);

    /**
     * 用户注册
     */
    Long register(RegisterDTO registerDTO);

    /**
     * 刷新Token
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 生成图形验证码
     * @param key 验证码唯一标识
     * @return Base64编码的验证码图片
     */
    String generateCaptcha(String key);
}
