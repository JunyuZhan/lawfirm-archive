import axios from 'axios';
import type { AxiosResponse } from 'axios';
import type { UserDto, RoleDto } from '../types/user';
import type { PageResponse } from '../types/common';

// 获取用户列表（分页）
export const getUserList = (keyword?: string, page = 0, size = 10): Promise<AxiosResponse<PageResponse<UserDto>>> => {
  return axios.get('/api/users', {
    params: { keyword, page, size }
  });
};

// 获取用户详情
export const getUserById = (id: number): Promise<AxiosResponse<UserDto>> => {
  return axios.get(`/api/users/${id}`);
};

// 创建用户
export const createUser = (user: UserDto): Promise<AxiosResponse<UserDto>> => {
  return axios.post('/api/users', user);
};

// 更新用户
export const updateUser = (id: number, user: UserDto): Promise<AxiosResponse<UserDto>> => {
  return axios.put(`/api/users/${id}`, user);
};

// 删除用户
export const deleteUser = (id: number): Promise<AxiosResponse<void>> => {
  return axios.delete(`/api/users/${id}`);
};

// 更新用户状态
export const updateUserStatus = (id: number, status: number): Promise<AxiosResponse<void>> => {
  return axios.patch(`/api/users/${id}/status?status=${status}`);
};

// 重置用户密码
export const resetUserPassword = (id: number): Promise<AxiosResponse<void>> => {
  return axios.post(`/api/users/${id}/reset-password`);
};

// 修改密码
export const changePassword = (id: number, oldPassword: string, newPassword: string): Promise<AxiosResponse<void>> => {
  return axios.put(`/api/users/${id}/change-password`, null, {
    params: { oldPassword, newPassword }
  });
};

// 分配角色
export const assignRoles = (userId: number, roleIds: number[]): Promise<AxiosResponse<void>> => {
  return axios.put(`/api/users/${userId}/roles`, roleIds);
};

// 获取当前登录用户信息
export const getCurrentUser = (): Promise<AxiosResponse<UserDto>> => {
  return axios.get('/api/users/me');
};

// 批量导入用户
export const importUsers = (users: UserDto[]): Promise<AxiosResponse<{successCount: number, failCount: number}>> => {
  return axios.post('/api/users/import', users);
};

// 导出所有用户
export const exportUsers = (): Promise<AxiosResponse<UserDto[]>> => {
  return axios.get('/api/users/export');
};

// 批量更新用户状态
export const batchUpdateUserStatus = (userIds: number[], status: number): Promise<AxiosResponse<{successCount: number, failCount: number}>> => {
  return axios.patch('/api/users/batch-status', null, {
    params: { userIds: userIds.join(','), status }
  });
}; 