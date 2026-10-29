export type UserRole = 'ADMIN' | 'EMPLOYEE';
export type PermissionCode = 'CONFIG_WRITE' | 'CATEGORIAS_WRITE' | 'DEPRECIACION_WRITE';

export interface Account {
  id: number;
  fullName: string;
  username: string;
  role: UserRole;
  enabled: boolean;
  permissions?: PermissionCode[];
  createdAt: string;
}

export interface AccountRequest {
  fullName: string;
  username: string;
  password: string;
  role?: UserRole;
  permissions?: PermissionCode[];
}

export interface LoginRequest {
  username: string;
  password: string;
}
