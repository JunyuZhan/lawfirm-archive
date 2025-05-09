import axios from 'axios';
import type { AxiosResponse } from 'axios';
import request from '@/utils/request';

interface LoginRequest {
  username: string;
  password: string;
}

interface AuthResponse {
  token: string;
}

// 登录
export const login = (loginRequest: LoginRequest): Promise<AxiosResponse<AuthResponse>> => {
  return request.post('/api/auth/login', loginRequest);
};

// 注销
export const logout = (): void => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  // 重定向到登录页
  window.location.href = '/login';
}; 