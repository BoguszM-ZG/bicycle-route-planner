export interface RegisterRequest {
  email: string;
  password?: string;
  fullName?: string;
}

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface UserDto {
  id: number;
  email: string;
  fullName: string;
  role: string;
  active: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresInMs: number;
  refreshTokenExpiresInMs: number;
  tokenType: string;
  user: UserDto;
}
