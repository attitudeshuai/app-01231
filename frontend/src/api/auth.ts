/**
 * 认证模块API
 *
 * 提供用户登录、注册、图形验证码获取、短信验证码发送等认证相关接口。
 * 登录和注册流程均需要图形验证码和短信验证码双重验证。
 *
 * @module api/auth
 */
import { request } from '@/utils/request'

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
  smsCode: string
}

/** 注册请求参数 */
export interface RegisterParams {
  username: string
  password: string
  confirmPassword: string
  email?: string
  phone: string
  smsCode: string
  captchaKey: string
  captchaCode: string
}

/** 登录响应结果，包含JWT Token和用户信息 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  email: string
  avatar: string
  roles: string[]
  permissions: string[]
}

/** 短信验证码发送结果 */
export interface SendSmsResult {
  maskedPhone: string
  code: string
}

/**
 * 用户登录
 *
 * 需要提供用户名、密码、图形验证码和短信验证码。
 *
 * @param data - 登录参数
 * @returns 包含Token和用户信息的登录结果
 */
export function login(data: LoginParams) {
  return request.post<{ data: LoginResult }>('/auth/login', data)
}

/**
 * 发送短信验证码
 *
 * 60秒内不可重复发送。登录场景传username，注册场景传phone。
 * 开发环境直接返回验证码供前端展示。
 *
 * @param params - 包含手机号或用户名的发送参数
 * @returns 脱敏手机号和验证码
 */
export function sendSmsCode(params: { phone?: string; username?: string }) {
  return request.post<{ data: SendSmsResult }>('/auth/send-sms-code', params)
}

/**
 * 用户注册
 *
 * 需要提供用户名、密码、手机号、图形验证码和短信验证码。
 *
 * @param data - 注册参数
 * @returns 新创建的用户ID
 */
export function register(data: RegisterParams) {
  return request.post<{ data: number }>('/auth/register', data)
}

/**
 * 刷新Token
 *
 * 使用refreshToken重新获取accessToken和refreshToken。
 *
 * @param refreshToken - 刷新令牌
 * @returns 新的登录凭证
 */
export function refreshToken(refreshToken: string) {
  return request.post<{ data: LoginResult }>('/auth/refresh-token', { refreshToken })
}

/**
 * 获取图形验证码
 *
 * 生成并返回图形验证码图片（Base64编码）和对应的key。
 * 前端在登录/注册时需将key和用户输入的验证码一并提交。
 *
 * @param key - 可选的验证码标识，为空时后端自动生成UUID
 * @returns 包含key和Base64图片数据的对象
 */
export function getCaptcha(key?: string) {
  return request.get<{ data: { key: string; image: string } }>('/auth/captcha', { params: { key } })
}

/**
 * 退出登录
 *
 * JWT无状态，服务端不维护会话，客户端删除Token即可。
 */
export function logout() {
  return request.post('/auth/logout')
}
