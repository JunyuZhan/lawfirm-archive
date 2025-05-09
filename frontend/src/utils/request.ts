import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token');
    if (token) {
      // 设置请求头中的Authorization字段
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    console.log(error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response;
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401: // 未授权
          ElMessage.error('未登录或登录已过期，请重新登录');
          // 清除token并跳转到登录页
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          router.push('/login');
          break;
        case 403: // 权限不足
          ElMessage.error('您没有权限访问此资源');
          router.push('/403');
          break;
        case 404: // 资源不存在
          ElMessage.error('请求的资源不存在');
          router.push('/404');
          break;
        case 500: // 服务器错误
          ElMessage.error('服务器错误，请稍后再试');
          break;
        default:
          ElMessage.error(error.response.data.message || '请求失败');
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接');
    }
    return Promise.reject(error);
  }
);

export default request; 