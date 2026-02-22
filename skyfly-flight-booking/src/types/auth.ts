export type RegisterRequest = {
  name: string
  username: string
  email: string
  password: string
  phone: string
}

export type LoginRequest = {
  code: string
  codeVerifier: string
  redirectUri: string
}

export type RefreshTokenRequest = {
  refreshToken: string
}

export type LogoutRequest = {
  refreshToken: string
  accessToken: string
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: string
}

export type AuthorizeResponse = {
  authorizationUrl: string
  codeVerifier: string
  state: string
  redirectUri: string
  scope: string
}

export type RegisterResponse = {
  userId: string
  message: string
}

export type MessageResponse = {
  message: string
}

export type UserProfile = {
  userId: string
  name: string
  email: string
  phone: string
}

export type ApiErrorBody = {
  code?: string
  message?: string
  correlationId?: string
}
