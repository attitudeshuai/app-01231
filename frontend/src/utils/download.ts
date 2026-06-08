/**
 * 下载工具模块
 *
 * 提供文件下载（Blob）、Excel导出、JSON/CSV导出和页面打印等功能。
 * 下载请求自动携带JWT Token认证头。
 *
 * @module utils/download
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * 下载文件（通过Blob）
 *
 * 自动从Content-Disposition响应头解析文件名，支持中文文件名。
 *
 * @param url - 文件下载地址
 * @param filename - 可选的自定义文件名
 */
export async function downloadFile(url: string, filename?: string): Promise<void> {
  try {
    const userStore = useUserStore()
    const response = await axios({
      url,
      method: 'GET',
      responseType: 'blob',
      headers: {
        Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
      }
    })

    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition']
    let finalFilename = filename
    if (!finalFilename && contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename\*?=(?:UTF-8'')?([^;\n]+)/i)
      if (filenameMatch) {
        finalFilename = decodeURIComponent(filenameMatch[1].replace(/['"]/g, ''))
      }
    }
    if (!finalFilename) {
      finalFilename = `download_${Date.now()}`
    }

    // 创建下载链接
    const blob = new Blob([response.data])
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = finalFilename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)

    ElMessage.success('下载成功')
  } catch (error: any) {
    console.error('下载失败:', error)
    ElMessage.error(error.message || '下载失败')
    throw error
  }
}

/**
 * 下载Excel文件
 *
 * @param url - Excel文件下载地址
 * @param filename - 可选的自定义文件名，默认为 export_时间戳.xlsx
 */
export async function downloadExcel(url: string, filename?: string): Promise<void> {
  const finalFilename = filename || `export_${Date.now()}.xlsx`
  await downloadFile(url, finalFilename)
}

/**
 * 导出数据为JSON文件
 *
 * @param data - 要导出的数据对象
 * @param filename - 可选的自定义文件名
 */
export function exportJSON(data: any, filename?: string): void {
  const jsonStr = JSON.stringify(data, null, 2)
  const blob = new Blob([jsonStr], { type: 'application/json' })
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = filename || `data_${Date.now()}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)
}

/**
 * 导出数据为CSV文件
 *
 * 自动添加BOM头支持中文，用双引号包裹字段值防止逗号干扰。
 *
 * @param data - 要导出的数据数组
 * @param headers - 列头定义（key-字段名, label-显示名）
 * @param filename - 可选的自定义文件名
 */
export function exportCSV(
  data: Record<string, any>[],
  headers: { key: string; label: string }[],
  filename?: string
): void {
  if (!data || data.length === 0) {
    ElMessage.warning('没有数据可导出')
    return
  }

  // 生成 CSV 内容
  const headerRow = headers.map((h) => `"${h.label}"`).join(',')
  const dataRows = data.map((row) => {
    return headers
      .map((h) => {
        const value = row[h.key]
        if (value === null || value === undefined) return '""'
        return `"${String(value).replace(/"/g, '""')}"`
      })
      .join(',')
  })

  const csvContent = '\uFEFF' + [headerRow, ...dataRows].join('\n') // 添加 BOM 支持中文
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = filename || `export_${Date.now()}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)

  ElMessage.success('导出成功')
}

/**
 * 打印指定区域的页面内容
 *
 * 通过打开新窗口实现打印，自动添加基础样式和表格样式。
 *
 * @param elementId - 要打印的DOM元素ID
 * @param title - 可选的打印标题
 */
export function printContent(elementId: string, title?: string): void {
  const element = document.getElementById(elementId)
  if (!element) {
    ElMessage.error('打印区域未找到')
    return
  }

  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.error('无法打开打印窗口，请检查浏览器设置')
    return
  }

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>${title || '打印'}</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f5f5f5; }
        @media print { body { padding: 0; } }
      </style>
    </head>
    <body>
      ${element.innerHTML}
    </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  printWindow.print()
  printWindow.close()
}
