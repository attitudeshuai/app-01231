/**
 * 数据备份模块API
 *
 * 提供数据备份列表、创建备份、恢复备份、下载备份、删除备份等接口。
 *
 * @module api/backup
 */
import { request } from '@/utils/request'

/** 备份文件信息 */
export interface BackupFile {
  filename: string
  size: number
  sizeText: string
  createdAt: string
  path: string
}

/**
 * 获取备份文件列表
 *
 * @returns 所有备份文件信息
 */
export function getBackupList() {
  return request.get<{ data: BackupFile[] }>('/system/backups')
}

/**
 * 创建数据备份
 *
 * @returns 备份文件名
 */
export function createBackup() {
  return request.post<{ data: string }>('/system/backup')
}

/**
 * 从备份文件恢复数据
 *
 * @param filename - 备份文件名
 */
export function restoreBackup(filename: string) {
  return request.post('/system/backup/restore', { filename })
}

/**
 * 获取备份文件下载地址
 *
 * @param filename - 备份文件名
 * @returns 下载URL
 */
export function getBackupDownloadUrl(filename: string): string {
  return `/api/system/backup/download/${encodeURIComponent(filename)}`
}

/**
 * 删除指定备份文件
 *
 * @param filename - 备份文件名
 */
export function deleteBackup(filename: string) {
  return request.delete(`/system/backup/${encodeURIComponent(filename)}`)
}

/**
 * 清理过期备份文件
 *
 * @param days - 保留天数，默认7天，超出的备份将被删除
 * @returns 删除的备份文件数
 */
export function cleanBackups(days: number = 7) {
  return request.delete<{ data: number }>('/system/backups/clean', { params: { days } })
}
