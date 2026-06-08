/**
 * 格式化工具模块
 *
 * 提供日期、金额、数字、文件大小等常用格式化函数，
 * 以及手机号/邮箱脱敏、文本截断、状态标签格式化等实用函数。
 *
 * @module utils/format
 */
import dayjs from 'dayjs'

/**
 * 日期格式化
 *
 * @param date - 日期值，支持字符串、Date对象或null
 * @param format - 格式化模板，默认 'YYYY-MM-DD'
 * @returns 格式化后的日期字符串，空值返回 '-'
 */
export function formatDate(date: string | Date | null | undefined, format = 'YYYY-MM-DD'): string {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 日期时间格式化
 *
 * @param date - 日期值
 * @param format - 格式化模板，默认 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期时间字符串
 */
export function formatDateTime(
  date: string | Date | null | undefined,
  format = 'YYYY-MM-DD HH:mm:ss'
): string {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 相对时间格式化（如“今天”、“昨天”、“3天前”）
 *
 * @param date - 日期值
 * @returns 相对时间描述，超过7天显示具体日期
 */
export function formatRelativeTime(date: string | Date | null | undefined): string {
  if (!date) return '-'
  const now = dayjs()
  const target = dayjs(date)
  const diffDays = now.diff(target, 'day')

  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays === -1) return '明天'
  if (diffDays > 0 && diffDays < 7) return `${diffDays}天前`
  if (diffDays < 0 && diffDays > -7) return `${Math.abs(diffDays)}天后`
  return formatDate(date)
}

/**
 * 金额格式化
 *
 * @param amount - 金额值
 * @param options - 配置项（前缀符号、小数位数）
 * @returns 格式化后的金额字符串，如 '¥100.00'
 */
export function formatMoney(
  amount: number | string | null | undefined,
  options: { prefix?: string; decimals?: number } = {}
): string {
  const { prefix = '¥', decimals = 2 } = options
  if (amount === null || amount === undefined || amount === '') return '-'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '-'
  return `${prefix}${num.toFixed(decimals)}`
}

/**
 * 数字千分位格式化
 *
 * @param num - 数字值
 * @returns 带千分位分隔符的字符串，如 '1,234,567'
 */
export function formatNumber(num: number | string | null | undefined): string {
  if (num === null || num === undefined || num === '') return '-'
  const value = typeof num === 'string' ? parseFloat(num) : num
  if (isNaN(value)) return '-'
  return value.toLocaleString('zh-CN')
}

/**
 * 文件大小格式化
 *
 * @param bytes - 字节数
 * @returns 可读的文件大小字符串，如 '1.50 MB'
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined) return '-'
  if (bytes === 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  const size = bytes / Math.pow(k, i)

  return `${size.toFixed(i === 0 ? 0 : 2)} ${units[i]}`
}

/**
 * 手机号脱敏，中间4位替换为****
 *
 * @param phone - 手机号
 * @returns 脱敏后的手机号，如 '138****1234'
 */
export function maskPhone(phone: string | null | undefined): string {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 邮箱脱敏，用户名部分只保留前2位
 *
 * @param email - 邮箱地址
 * @returns 脱敏后的邮箱，如 'ab***@example.com'
 */
export function maskEmail(email: string | null | undefined): string {
  if (!email) return '-'
  const [name, domain] = email.split('@')
  if (!domain) return email
  const maskedName = name.length > 2 ? name.slice(0, 2) + '***' : name + '***'
  return `${maskedName}@${domain}`
}

/**
 * 截断文本，超出部分以...显示
 *
 * @param text - 原始文本
 * @param maxLength - 最大长度，默认50
 * @returns 截断后的文本
 */
export function truncateText(text: string | null | undefined, maxLength = 50): string {
  if (!text) return '-'
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

/**
 * 状态标签格式化，将状态值转换为可读文本
 *
 * @param status - 状态值（1/true=启用，0/false=禁用）
 * @param options - 自定义文本配置
 * @returns 状态文本
 */
export function formatStatus(
  status: number | string | boolean,
  options: { trueText?: string; falseText?: string } = {}
): string {
  const { trueText = '启用', falseText = '禁用' } = options
  if (status === 1 || status === '1' || status === true) return trueText
  return falseText
}
