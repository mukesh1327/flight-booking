import { apiRequest } from '../lib/http'
import type {
  AuthResponse,
  AuthorizeResponse,
  LoginRequest,
  LogoutRequest,
  MessageResponse,
  RefreshTokenRequest,
  RegisterRequest,
  RegisterResponse,
  UserProfile,
} from '../types/auth'

export const authService = {
  register: (payload: RegisterRequest) =>
    apiRequest<RegisterResponse>('/auth/register', 'POST', { body: payload }),

  buildAuthorizeUrl: (redirectUri: string) => {
    const query = new URLSearchParams({ redirectUri })
    return apiRequest<AuthorizeResponse>(`/auth/login/authorize?${query.toString()}`, 'GET')
  },

  login: (payload: LoginRequest) => apiRequest<AuthResponse>('/auth/login', 'POST', { body: payload }),

  refresh: (payload: RefreshTokenRequest) =>
    apiRequest<AuthResponse>('/auth/token/refresh', 'POST', { body: payload }),

  logout: (payload: LogoutRequest, accessToken: string) =>
    apiRequest<MessageResponse>('/auth/logout', 'POST', { body: payload, token: accessToken }),

  getProfile: (accessToken: string) =>
    apiRequest<UserProfile>('/auth/profile', 'GET', { token: accessToken }),
}
