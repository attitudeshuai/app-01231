/**
 * 角色权限模块API
 *
 * 提供角色CRUD、权限分配、权限树查询等接口。
 *
 * @module api/role
 */
import { request } from '@/utils/request'
import type { PageResult } from './book'

/** 角色实体 */
export interface Role {
  id: number
  roleName: string
  roleCode: string
  description: string
  createdAt: string
}

/** 权限实体（支持树形嵌套） */
export interface Permission {
  id: number
  permissionName: string
  permissionCode: string
  resourceType: string
  parentId: number
  sortOrder: number
  children?: Permission[]
}

/** 角色表单数据（新增/编辑） */
export interface RoleFormData {
  roleName: string
  roleCode: string
  description?: string
  permissionIds?: number[]
}

/**
 * 分页查询角色列表
 *
 * @param params - 查询参数
 * @returns 分页角色列表
 */
export function getRoleList(params: { roleName?: string; pageNum?: number; pageSize?: number }) {
  return request.get<{ data: PageResult<Role> }>('/roles', { params })
}

/**
 * 获取所有角色（不分页，用于下拉选择）
 *
 * @returns 所有角色列表
 */
export function getAllRoles() {
  return request.get<{ data: Role[] }>('/roles/all')
}

/**
 * 获取角色详情
 *
 * @param id - 角色ID
 * @returns 角色详细信息
 */
export function getRoleById(id: number) {
  return request.get<{ data: Role }>(`/roles/${id}`)
}

/**
 * 新增角色
 *
 * @param data - 角色表单数据
 * @returns 新创建的角色ID
 */
export function createRole(data: RoleFormData) {
  return request.post<{ data: number }>('/roles', data)
}

/**
 * 更新角色信息
 *
 * @param id - 角色ID
 * @param data - 更新的角色数据
 */
export function updateRole(id: number, data: RoleFormData) {
  return request.put(`/roles/${id}`, data)
}

/**
 * 删除角色
 *
 * @param id - 角色ID
 */
export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}

/**
 * 获取角色已分配的权限ID列表
 *
 * @param id - 角色ID
 * @returns 权限ID数组
 */
export function getRolePermissions(id: number) {
  return request.get<{ data: number[] }>(`/roles/${id}/permissions`)
}

/**
 * 为角色分配权限
 *
 * @param id - 角色ID
 * @param permissionIds - 权限ID数组
 */
export function assignPermissions(id: number, permissionIds: number[]) {
  return request.put(`/roles/${id}/permissions`, { permissionIds })
}

/**
 * 获取权限树形结构
 *
 * @returns 树形嵌套的权限列表
 */
export function getPermissionTree() {
  return request.get<{ data: Permission[] }>('/roles/permissions')
}
