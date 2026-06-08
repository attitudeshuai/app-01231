/**
 * 借阅模块API
 *
 * 提供图书借阅、归还、续借、逾期管理、借阅统计等接口。
 *
 * @module api/borrow
 */
import { request } from '@/utils/request'
import type { PageResult } from './book'

/** 借阅记录实体 */
export interface BorrowRecord {
  id: number
  userId: number
  bookId: number
  borrowDate: string
  dueDate: string
  returnDate: string
  renewCount: number
  fineAmount: number
  status: number
  remark: string
  createdAt: string
  username: string
  bookTitle: string
  isbn: string
  author: string
}

/** 借阅记录查询参数 */
export interface BorrowQueryParams {
  userId?: number
  username?: string
  bookTitle?: string
  status?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

/** 借阅统计数据 */
export interface BorrowStatistics {
  totalBorrows: number
  returnedCount: number
  borrowingCount: number
  overdueCount: number
  totalFine: number
}

/**
 * 分页查询借阅记录（管理员）
 *
 * @param params - 查询参数
 * @returns 分页借阅记录列表
 */
export function getBorrowList(params: BorrowQueryParams) {
  return request.get<{ data: PageResult<BorrowRecord> }>('/borrows', { params })
}

/**
 * 获取借阅详情
 *
 * @param id - 借阅记录ID
 * @returns 借阅记录详情
 */
export function getBorrowById(id: number) {
  return request.get<{ data: BorrowRecord }>(`/borrows/${id}`)
}

/**
 * 借阅图书
 *
 * @param bookId - 图书ID
 * @returns 借阅记录ID
 */
export function borrowBook(bookId: number) {
  return request.post<{ data: number }>('/borrows', { bookId })
}

/**
 * 管理员代借阅
 *
 * @param userId - 用户ID
 * @param bookId - 图书ID
 * @returns 借阅记录ID
 */
export function adminBorrowBook(userId: number, bookId: number) {
  return request.post<{ data: number }>('/borrows/admin', { userId, bookId })
}

/**
 * 归还图书，逾期时自动计算罚款
 *
 * @param id - 借阅记录ID
 */
export function returnBook(id: number) {
  return request.put(`/borrows/${id}/return`)
}

/**
 * 续借图书，每本书最多续借1次
 *
 * @param id - 借阅记录ID
 */
export function renewBook(id: number) {
  return request.put(`/borrows/${id}/renew`)
}

/**
 * 获取当前用户的借阅记录
 *
 * @param params - 查询参数（状态、分页）
 * @returns 分页借阅记录
 */
export function getMyBorrows(params: { status?: number; pageNum?: number; pageSize?: number }) {
  return request.get<{ data: PageResult<BorrowRecord> }>('/borrows/my', { params })
}

/**
 * 获取当前用户正在借阅中的图书列表
 *
 * @returns 借阅中的记录列表
 */
export function getCurrentBorrows() {
  return request.get<{ data: BorrowRecord[] }>('/borrows/current')
}

/**
 * 获取逾期借阅列表（管理员）
 *
 * @param pageNum - 页码，默认1
 * @param pageSize - 每页数量，默认10
 * @returns 逾期借阅记录列表
 */
export function getOverdueList(pageNum: number = 1, pageSize: number = 10) {
  return request.get<{ data: PageResult<BorrowRecord> }>('/borrows/overdue', { params: { pageNum, pageSize } })
}

/**
 * 获取借阅统计数据（管理员）
 *
 * @returns 借阅统计（总数、归还数、借阅中、逾期数、罚款总额）
 */
export function getBorrowStatistics() {
  return request.get<{ data: BorrowStatistics }>('/borrows/statistics')
}

/**
 * 检查当前用户是否可以借阅指定图书
 *
 * @param bookId - 图书ID
 * @returns 是否可借阅
 */
export function canBorrow(bookId: number) {
  return request.get<{ data: boolean }>('/borrows/can-borrow', { params: { bookId } })
}
