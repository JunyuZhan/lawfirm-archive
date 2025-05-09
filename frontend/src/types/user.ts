// 权限DTO
export interface PermissionDto {
  id?: number;
  name: string;
  description?: string;
  type: string;
  resource?: string;
}

// 角色DTO
export interface RoleDto {
  id?: number;
  name: string;
  description?: string;
  permissions?: PermissionDto[];
}

// 用户DTO
export interface UserDto {
  id?: number;
  username: string;
  password?: string;
  email: string;
  realName?: string;
  phone?: string;
  avatar?: string;
  position?: string;
  department?: string;
  status?: number;
  lastLoginTime?: string;
  createdAt?: string;
  updatedAt?: string;
  roles?: RoleDto[];
} 