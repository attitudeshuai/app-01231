/**
 * 分类模块API
 *
 * 提供图书分类的CRUD和树形结构查询接口。
 *
 * @module api/category
 */
import { request } from '@/utils/request'

/** 分类实体（支持树形嵌套） */
export interface Category {
  id: number
  name: string
  description: string
  parentId: number
  sortOrder: number
  createdAt: string
  children?: Category[]
}

/** 分类表单数据（新增/编辑） */
export interface CategoryFormData {
  name: string
  description?: string
  parentId?: number
  sortOrder?: number
}

/**
 * 获取分类列表（扁平结构）
 *
 * @returns 所有分类列表
 */
export function getCategoryList() {
  return request.get<{ data: Category[] }>('/categories')
}

/**
 * 获取分类树形结构
 *
 * @returns 树形嵌套的分类列表
 */
export function getCategoryTree() {
  return request.get<{ data: Category[] }>('/categories/tree')
}

/**
 * 获取分类详情
 *
 * @param id - 分类ID
 * @returns 分类详细信息
 */
export function getCategoryById(id: number) {
  return request.get<{ data: Category }>(`/categories/${id}`)
}

/**
 * 新增分类
 *
 * @param data - 分类表单数据
 * @returns 新创建的分类ID
 */
export function createCategory(data: CategoryFormData) {
  return request.post<{ data: number }>('/categories', data)
}

/**
 * 更新分类信息
 *
 * @param id - 分类ID
 * @param data - 更新的分类数据
 */
export function updateCategory(id: number, data: CategoryFormData) {
  return request.put(`/categories/${id}`, data)
}

/**
 * 删除分类
 *
 * @param id - 分类ID
 */
export function deleteCategory(id: number) {
  return request.delete(`/categories/${id}`)
}
