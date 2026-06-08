/**
 * 通知模块API
 *
 * 提供站内通知列表查询、未读数量统计、标记已读等接口。
 *
 * @module api/notification
 */
import { request } from '@/utils/request'
import type { PageResult } from './book'

/** 通知实体 */
export interface Notification {
  id: number
  userId: number
  title: string
  content: string
  type: number
  isRead: number
  createdAt: string
}

/**
 * 获取当前用户的通知列表（分页）
 *
 * @param pageNum - 页码，默认1
 * @param pageSize - 每页数量，默认10
 * @returns 分页通知列表
 */
export function getNotificationList(pageNum: number = 1, pageSize: number = 10) {
  return request.get<{ data: PageResult<Notification> }>('/notifications', {
    params: { pageNum, pageSize }
  })
}

/**
 * 获取当前用户的未读通知数量
 *
 * @returns 未读通知数
 */
export function getUnreadCount() {
  return request.get<{ data: number }>('/notifications/unread-count')
}

/**
 * 标记指定通知为已读
 *
 * @param id - 通知ID
 */
export function markAsRead(id: number) {
  return request.put(`/notifications/${id}/read`)
}

/**
 * 将当前用户的所有未读通知标记为已读
 */
export function markAllAsRead() {
  return request.put('/notifications/read-all')
}
