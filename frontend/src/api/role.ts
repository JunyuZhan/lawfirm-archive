import axios from 'axios';
import type { AxiosResponse } from 'axios';
import { RoleDto, PermissionDto } from '@/types/user';
import { PageResponse } from '@/types/common';

// 获取角色列表
export const getRoleList = (page = 0, size = 10): Promise<AxiosResponse<PageResponse<RoleDto>>> => {
  return axios.get('/api/roles', {
    params: { page, size }
  });
};

// 获取所有角色（不分页）
export const getAllRoles = (): Promise<AxiosResponse<RoleDto[]>> => {
  return axios.get('/api/roles/all');
};

// 获取角色详情
export const getRoleById = (id: number): Promise<AxiosResponse<RoleDto>> => {
  return axios.get(`/api/roles/${id}`);
};

// 创建角色
export const createRole = (role: RoleDto): Promise<AxiosResponse<RoleDto>> => {
  return axios.post('/api/roles', role);
};

// 更新角色
export const updateRole = (id: number, role: RoleDto): Promise<AxiosResponse<RoleDto>> => {
  return axios.put(`/api/roles/${id}`, role);
};

// 删除角色
export const deleteRole = (id: number): Promise<AxiosResponse<void>> => {
  return axios.delete(`/api/roles/${id}`);
};

// 获取所有权限
export const getAllPermissions = (): Promise<AxiosResponse<PermissionDto[]>> => {
  return axios.get('/api/permissions');
};

// 给角色分配权限
export const assignPermissions = (roleId: number, permissionIds: number[]): Promise<AxiosResponse<void>> => {
  return axios.put(`/api/roles/${roleId}/permissions`, permissionIds);
};