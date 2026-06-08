/**
 * 图书模块API
 *
 * 提供图书CRUD、封面上传、库存管理、Excel批量导入导出等接口。
 *
 * @module api/book
 */
import { request } from '@/utils/request'

/** 图书实体 */
export interface Book {
  id: number
  isbn: string
  title: string
  author: string
  publisher: string
  publishDate: string
  price: number
  categoryId: number
  categoryName: string
  coverUrl: string
  description: string
  totalStock: number
  availableStock: number
  location: string
  status: number
  createdAt: string
  updatedAt: string
}

/** 图书查询参数 */
export interface BookQueryParams {
  keyword?: string
  title?: string
  author?: string
  isbn?: string
  categoryId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 图书表单数据（新增/编辑） */
export interface BookFormData {
  isbn: string
  title: string
  author: string
  publisher?: string
  publishDate?: string
  price?: number
  categoryId: number
  coverUrl?: string
  description?: string
  totalStock?: number
  location?: string
  status?: number
}

/** 库存调整参数 */
export interface StockAdjust {
  type: number // 1-入库 2-出库
  quantity: number
  remark?: string
}

/** 分页结果通用类型 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/**
 * 分页查询图书列表
 *
 * @param params - 查询参数（关键词、分类、状态等）
 * @returns 分页图书列表
 */
export function getBookList(params: BookQueryParams) {
  return request.get<{ data: PageResult<Book> }>('/books', { params })
}

/**
 * 获取图书详情
 *
 * @param id - 图书ID
 * @returns 图书详细信息
 */
export function getBookById(id: number) {
  return request.get<{ data: Book }>(`/books/${id}`)
}

/**
 * 新增图书
 *
 * @param data - 图书表单数据
 * @returns 新创建的图书ID
 */
export function createBook(data: BookFormData) {
  return request.post<{ data: number }>('/books', data)
}

/**
 * 更新图书信息
 *
 * @param id - 图书ID
 * @param data - 更新的图书数据
 */
export function updateBook(id: number, data: BookFormData) {
  return request.put(`/books/${id}`, data)
}

/**
 * 删除图书
 *
 * @param id - 图书ID
 */
export function deleteBook(id: number) {
  return request.delete(`/books/${id}`)
}

/**
 * 调整图书库存
 *
 * @param id - 图书ID
 * @param data - 库存调整参数（入库/出库）
 */
export function adjustStock(id: number, data: StockAdjust) {
  return request.post(`/books/${id}/stock`, data)
}

/**
 * 获取库存预警图书列表
 *
 * @param pageNum - 页码，默认1
 * @param pageSize - 每页数量，默认10
 * @returns 库存低于阈值的图书列表
 */
export function getStockWarning(pageNum: number = 1, pageSize: number = 10) {
  return request.get<{ data: PageResult<Book> }>('/books/stock-warning', { params: { pageNum, pageSize } })
}

/**
 * 上传图书封面图片
 *
 * 支持jpg/png等常见图片格式，上传后返回图片URL可直接用于预览。
 *
 * @param file - 封面图片文件
 * @returns 封面图片的访问URL
 */
export function uploadCover(file: File) {
  return request.upload<{ data: string }>('/books/cover/upload', file)
}

/**
 * 更新图书状态（上架/下架）
 *
 * @param id - 图书ID
 * @param status - 状态值（1=上架，0=下架）
 */
export function updateBookStatus(id: number, status: number) {
  return request.put(`/books/${id}/status`, { status })
}

/** Excel批量导入结果 */
export interface ImportResult {
  successCount: number
  failCount: number
  totalCount: number
  errorMessages: string[]
}

/**
 * 批量导入图书（Excel格式）
 *
 * @param file - Excel文件
 * @returns 导入结果（成功/失败计数和错误信息）
 */
export function importBooksExcel(file: File) {
  return request.upload<{ data: ImportResult }>('/books/import/excel', file)
}

/**
 * 获取图书导入模板下载地址
 *
 * @returns 模板文件的下载URL
 */
export function getImportTemplateUrl(): string {
  return '/api/books/import/template'
}

/**
 * 获取图书导出Excel的下载地址
 *
 * @param params - 可选的查询参数，用于筛选导出内容
 * @returns Excel文件的下载URL
 */
export function getExportExcelUrl(params?: BookQueryParams): string {
  const query = new URLSearchParams()
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        query.append(key, String(value))
      }
    })
  }
  const queryStr = query.toString()
  return `/api/books/export/excel${queryStr ? '?' + queryStr : ''}`
}
