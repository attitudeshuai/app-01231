/**
 * HTTP请求工具模块
 *
 * 基于Axios封装，提供统一的请求/响应拦截、JWT Token自动携带、
 * 错误处理（401自动跳转登录、业务错误提示）和文件上传支持。
 *
 * - 基础URL：`/api`（通过Nginx代理到后端9999端口）
 * - 超时时间：30秒
 * - 认证白名单：登录、注册、验证码等接口无需携带Token
 *
 * @module utils/request
 */
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/** 创建Axios实例，配置基础URL和超时时间 */
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/** 是否正在刷新Token */
let isRefreshing = false
/** 重试请求队列 */
let retryQueue: Array<(token: string) => void> = []

/** 防重复弹窗标记 */
let isShowingLogoutDialog = false

/** 不需要携带Token的接口路径白名单 */
const AUTH_WHITE_LIST = ['/auth/login', '/auth/register', '/auth/captcha', '/auth/send-sms-code', '/auth/refresh-token']

/** 请求拦截器：自动为非白名单接口添加JWT Authorization头 */
service.interceptors.request.use(
  (config) => {
    const isAuthApi = AUTH_WHITE_LIST.some(path => config.url?.startsWith(path))
    if (!isAuthApi) {
      const userStore = useUserStore()
      if (userStore.token) {
        config.headers.Authorization = `Bearer ${userStore.token}`
      }
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/** 响应拦截器：统一处理业务错误码和HTTP错误状态 */
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    
    // 如果是文件下载，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    
    // 成功
    if (res.code === 200) {
      return res
    }
    
    // 业务错误
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          // 在登录/注册页面不弹出过期提示
          if (window.location.pathname === '/login' || window.location.pathname === '/register') {
            break
          }
          // 防止重复弹窗
          if (!isShowingLogoutDialog) {
            isShowingLogoutDialog = true
            ElMessageBox.confirm(
              '登录已过期，请重新登录',
              '提示',
              {
                confirmButtonText: '重新登录',
                cancelButtonText: '取消',
                type: 'warning'
              }
            ).then(() => {
              const userStore = useUserStore()
              userStore.logout()
              router.push('/login')
            }).finally(() => {
              isShowingLogoutDialog = false
            })
          }
          break
        case 403:
          ElMessage.error('没有操作权限')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error(data?.message || '服务器内部错误')
          break
        default:
          ElMessage.error(data?.message || '网络错误')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时')
    } else {
      ElMessage.error('网络连接失败')
    }
    
    return Promise.reject(error)
  }
)

/**
 * 封装的HTTP请求方法集合
 *
 * 提供get/post/put/delete/upload五种方法，
 * 所有方法均经过请求拦截器和响应拦截器处理。
 */
export const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  },
  
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },
  
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },
  
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  },
  
  upload<T = any>(url: string, file: File, config?: AxiosRequestConfig): Promise<T> {
    const formData = new FormData()
    formData.append('file', file)
    return service.post(url, formData, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

export default service
