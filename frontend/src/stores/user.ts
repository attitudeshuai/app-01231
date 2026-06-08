/**
 * 用户状态管理Store
 *
 * 基于Pinia实现，管理用户登录状态、JWT Token、角色权限等信息。
 * Token和用户信息会持久化到localStorage，支持页面刷新后自动恢复登录状态。
 *
 * @module stores/user
 */
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, type LoginParams, type LoginResult } from '@/api/auth'
import router from '@/router'

/** 用户状态接口 */
interface UserState {
  token: string
  refreshToken: string
  userId: number | null
  username: string
  email: string
  avatar: string
  roles: string[]
  permissions: string[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    userId: null,
    username: localStorage.getItem('username') || '',
    email: '',
    avatar: '',
    roles: JSON.parse(localStorage.getItem('roles') || '[]'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.roles.includes('ADMIN'),
    hasPermission: (state) => (permission: string) => {
      return state.permissions.includes(permission) || state.roles.includes('ADMIN')
    }
  },

  actions: {
    /**
     * 用户登录
     *
     * 调用登录API并将返回的Token和用户信息存储到状态和localStorage。
     *
     * @param params - 登录参数（用户名、密码、验证码）
     * @returns 登录结果
     */
    async login(params: LoginParams) {
      const res = await loginApi(params)
      const data: LoginResult = res.data
      
      this.setUserInfo(data)
      
      return data
    },

    /**
     * 设置用户信息并持久化到localStorage
     *
     * @param data - 登录返回的用户信息
     */
    setUserInfo(data: LoginResult) {
      this.token = data.accessToken
      this.refreshToken = data.refreshToken
      this.userId = data.userId
      this.username = data.username
      this.email = data.email
      this.avatar = data.avatar
      this.roles = data.roles
      this.permissions = data.permissions

      // 持久化存储
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('username', data.username)
      localStorage.setItem('roles', JSON.stringify(data.roles))
      localStorage.setItem('permissions', JSON.stringify(data.permissions))
    },

    /**
     * 退出登录
     *
     * 调用登出API并清除本地存储的用户信息，然后跳转到登录页。
     */
    async logout() {
      try {
        await logoutApi()
      } catch (error) {
        console.error('退出登录失败:', error)
      } finally {
        this.clearUserInfo()
        router.push('/login')
      }
    },

    /** 清除内存和localStorage中的所有用户信息 */
    clearUserInfo() {
      this.token = ''
      this.refreshToken = ''
      this.userId = null
      this.username = ''
      this.email = ''
      this.avatar = ''
      this.roles = []
      this.permissions = []

      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('username')
      localStorage.removeItem('roles')
      localStorage.removeItem('permissions')
    },

    /**
     * 更新用户头像
     *
     * @param avatar - 新头像URL
     */
    updateAvatar(avatar: string) {
      this.avatar = avatar
    }
  }
})
