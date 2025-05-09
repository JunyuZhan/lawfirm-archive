import { ElMessage } from 'element-plus'
import type { AxiosError } from 'axios'

/**
 * 处理API错误并显示适当的错误消息
 */
export function handleApiError(error: unknown, defaultMessage = '操作失败'): void {
  console.error('API请求错误:', error)
  
  // 处理Axios错误
  if (isAxiosError(error)) {
    const axiosError = error as AxiosError
    
    // 处理响应错误
    if (axiosError.response) {
      const status = axiosError.response.status
      const responseData = axiosError.response.data as any
      
      // 处理特定HTTP状态码
      switch (status) {
        case 400:
          ElMessage.error(responseData.message || '请求参数错误')
          break
        case 401:
          ElMessage.error('未授权，请重新登录')
          // 可以在此处理登录过期逻辑
          break
        case 403:
          ElMessage.error('没有操作权限')
          break
        case 404:
          ElMessage.error(responseData.message || '请求的资源不存在')
          break
        case 500:
          ElMessage.error(responseData.message || '服务器内部错误')
          break
        default:
          ElMessage.error(responseData.message || defaultMessage)
      }
    } 
    // 处理请求错误
    else if (axiosError.request) {
      ElMessage.error('无法连接到服务器，请检查网络连接')
    } 
    // 处理其他错误
    else {
      ElMessage.error(axiosError.message || defaultMessage)
    }
  } 
  // 处理其他类型的错误
  else if (error instanceof Error) {
    ElMessage.error(error.message || defaultMessage)
  } 
  // 处理未知错误
  else {
    ElMessage.error(defaultMessage)
  }
}

/**
 * 类型保护：判断是否为Axios错误
 */
function isAxiosError(error: unknown): boolean {
  return (
    typeof error === 'object' && 
    error !== null && 
    'isAxiosError' in error && 
    (error as any).isAxiosError === true
  )
}

/**
 * 创建用于捕获异步操作错误的包装函数
 */
export function createAsyncErrorHandler<T extends (...args: any[]) => Promise<any>>(
  fn: T, 
  errorMessage = '操作失败'
): (...args: Parameters<T>) => Promise<Awaited<ReturnType<T>> | undefined> {
  return async (...args: Parameters<T>) => {
    try {
      return await fn(...args)
    } catch (error) {
      handleApiError(error, errorMessage)
      return undefined
    }
  }
}

/**
 * 防抖函数：限制函数的调用频率
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T, 
  delay: number
): (...args: Parameters<T>) => void {
  let timer: number | null = null
  
  return function(...args: Parameters<T>) {
    if (timer) {
      clearTimeout(timer)
    }
    
    timer = window.setTimeout(() => {
      fn(...args)
      timer = null
    }, delay)
  }
} 