package com.library.module.auth.controller;

import com.library.aspect.OperationLog;
import com.library.common.response.Result;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.dto.SendSmsDTO;
import com.library.module.auth.service.AuthService;
import com.library.module.auth.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证Controller
 *
 * <p>提供用户认证相关的RESTful API，包括登录、注册、图形验证码获取、
 * 短信验证码发送、Token刷新、登出等功能。</p>
 *
 * <p>所有认证接口均在 {@code /api/auth} 路径下，
 * 登录、注册、验证码接口无需携带Token。</p>
 *
 * @author Library System
 * @since 1.0.0
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * <p>需要提供用户名、密码、图形验证码和短信验证码。
     * 登录成功后返回JWT accessToken和refreshToken。</p>
     *
     * @param loginDTO 登录参数
     * @return 登录结果，包含Token和用户信息
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @OperationLog(value = "用户登录", type = OperationLog.OperationType.LOGIN)
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 发送短信验证码
     *
     * <p>60秒内不可重复发送。开发环境直接返回验证码供前端展示。</p>
     *
     * @param sendSmsDTO 发送参数（手机号或用户名）
     * @return 脱敏手机号和验证码
     */
    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-sms-code")
    public Result<Map<String, String>> sendSmsCode(@Valid @RequestBody SendSmsDTO sendSmsDTO) {
        String[] smsResult = authService.sendSmsCode(sendSmsDTO.getPhone(), sendSmsDTO.getUsername());
        Map<String, String> result = new HashMap<>();
        result.put("maskedPhone", smsResult[0]);
        result.put("code", smsResult[1]);
        return Result.success(result);
    }

    /**
     * 用户注册
     *
     * <p>需要提供用户名、密码、手机号、图形验证码和短信验证码。
     * 注册成功后自动分配普通用户角色。</p>
     *
     * @param registerDTO 注册参数
     * @return 新创建的用户ID
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @OperationLog(value = "用户注册", type = OperationLog.OperationType.CREATE)
    public Result<Long> register(@Valid @RequestBody RegisterDTO registerDTO) {
        Long userId = authService.register(registerDTO);
        return Result.success(userId);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh-token")
    public Result<LoginVO> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        LoginVO loginVO = authService.refreshToken(refreshToken);
        return Result.success(loginVO);
    }

    /**
     * 获取图形验证码
     *
     * <p>生成并返回图形验证码图片（Base64编码）和对应的key。
     * 前端在登录/注册时需将key和用户输入的验证码一并提交。</p>
     *
     * @param key 可选的验证码标识，为空时自动生成UUID
     * @return 包含key和Base64图片数据的Map
     */
    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha(@RequestParam(required = false) String key) {
        String captchaKey = (key != null && !key.isEmpty()) ? key : java.util.UUID.randomUUID().toString();
        String captchaImage = authService.generateCaptcha(captchaKey);
        Map<String, String> result = new HashMap<>();
        result.put("key", captchaKey);
        result.put("image", captchaImage);
        return Result.success(result);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    @OperationLog(value = "用户登出", type = OperationLog.OperationType.LOGOUT)
    public Result<Void> logout() {
        // JWT无状态，客户端删除Token即可
        return Result.success();
    }
}
