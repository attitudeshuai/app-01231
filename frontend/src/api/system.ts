/**
 * 系统管理模块API
 *
 * 提供操作日志查询、系统配置管理、仪表盘数据、数据备份等接口。
 *
 * @module api/system
 */
import { request } from '@/utils/request'
import type { PageResult } from './book'

/** 操作日志实体 */
export interface OperationLog {
  id: number
  userId: number
  username: string
  operation: string
  method: string
  params: string
  ip: string
  timeCost: number
  status: number
  errorMsg: string
  createdAt: string
}

/** 系统配置项 */
export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description: string
  updatedAt: string
}

/** 日志查询参数 */
export interface LogQueryParams {
  username?: string
  operation?: string
  status?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

/** 仪表盘数据（首页概览） */
export interface DashboardData {
  userCount: number
  bookCount: number
  borrowStats: {
    totalBorrows: number
    returnedCount: number
    borrowingCount: number
    overdueCount: number
    totalFine: number
  }
  stockWarningCount: number
  stockWarningBooks: any[]
  overdueCount: number
  overdueRecords: any[]
}

/**
 * 分页查询操作日志（管理员）
 *
 * @param params - 查询参数
 * @returns 分页日志列表
 */
export function getLogList(params: LogQueryParams) {
  return request.get<{ data: PageResult<OperationLog> }>('/system/logs', { params })
}

/**
 * 获取系统配置列表
 *
 * @returns 所有系统配置项
 */
export function getSystemConfig() {
  return request.get<{ data: SystemConfig[] }>('/system/config')
}

/**
 * 批量更新系统配置
 *
 * @param configs - 配置键值对
 */
export function updateSystemConfig(configs: Record<string, string>) {
  return request.put('/system/config', configs)
}

/**
 * 获取仪表盘概览数据
 *
 * 包含用户数、图书数、借阅统计、库存预警、逾期记录等。
 *
 * @returns 仪表盘数据
 */
export function getDashboardData() {
  return request.get<{ data: DashboardData }>('/system/dashboard')
}

/**
 * 创建数据备份（管理员）
 *
 * @returns 备份文件名
 */
export function backupData() {
  return request.post<{ data: string }>('/system/backup')
}

/**
 * 清理历史操作日志（管理员）
 *
 * @param days - 保留天数，默认30天，超出的日志将被删除
 * @returns 删除的日志条数
 */
export function cleanLogs(days: number = 30) {
  return request.delete<{ data: number }>('/system/logs/clean', { params: { days } })
}
