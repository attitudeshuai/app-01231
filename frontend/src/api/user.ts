/**
 * 用户模块API
 *
 * 提供用户CRUD、状态管理、角色分配、密码重置、个人资料管理等接口。
 *
 * @module api/user
 */
import { request } from '@/utils/request'
import type { PageResult } from './book'

/** 用户实体 */
export interface User {
  id: number
  username: string
  email: string
  phone: string
  avatar: string
  status: number
  createdAt: string
  updatedAt: string
}

/** 用户查询参数 */
export interface UserQueryParams {
  username?: string
  email?: string
  phone?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 用户表单数据（新增/编辑） */
export interface UserFormData {
  username: string
  password?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  roleIds?: number[]
}

/**
 * 分页查询用户列表（管理员）
 *
 * @param params - 查询参数
 * @returns 分页用户列表
 */
export function getUserList(params: UserQueryParams) {
  return request.get<{ data: PageResult<User> }>('/users', { params })
}

/**
 * 获取用户详情
 *
 * @param id - 用户ID
 * @returns 用户详细信息
 */
export function getUserById(id: number) {
  return request.get<{ data: User }>(`/users/${id}`)
}

/**
 * 新增用户（管理员）
 *
 * @param data - 用户表单数据
 * @returns 新创建的用户ID
 */
export function createUser(data: UserFormData) {
  return request.post<{ data: number }>('/users', data)
}

/**
 * 更新用户信息
 *
 * @param id - 用户ID
 * @param data - 更新的用户数据
 */
export function updateUser(id: number, data: UserFormData) {
  return request.put(`/users/${id}`, data)
}

/**
 * 删除用户
 *
 * @param id - 用户ID
 */
export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

/**
 * 修改用户状态（启用/禁用）
 *
 * @param id - 用户ID
 * @param status - 状态值（1=启用，0=禁用）
 */
export function updateUserStatus(id: number, status: number) {
  return request.put(`/users/${id}/status`, { status })
}

/**
 * 重置用户密码为默认密码（管理员）
 *
 * @param id - 用户ID
 */
export function resetPassword(id: number) {
  return request.put(`/users/${id}/reset-password`)
}

/**
 * 为用户分配角色
 *
 * @param id - 用户ID
 * @param roleIds - 角色ID数组
 */
export function assignRoles(id: number, roleIds: number[]) {
  return request.put(`/users/${id}/roles`, { roleIds })
}

/**
 * 获取当前登录用户的个人资料
 *
 * @returns 当前用户信息
 */
export function getProfile() {
  return request.get<{ data: any }>('/users/profile')
}

/**
 * 更新当前登录用户的个人资料
 *
 * @param data - 要更新的资料字段
 */
export function updateProfile(data: Partial<UserFormData>) {
  return request.put('/users/profile', data)
}

/**
 * 修改当前用户密码
 *
 * @param oldPassword - 原密码
 * @param newPassword - 新密码
 */
export function updatePassword(oldPassword: string, newPassword: string) {
  return request.put('/users/password', { oldPassword, newPassword })
}
