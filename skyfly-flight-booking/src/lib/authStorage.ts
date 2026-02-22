const ACCESS_TOKEN_KEY = 'skyfly.accessToken'
const REFRESH_TOKEN_KEY = 'skyfly.refreshToken'
const CODE_VERIFIER_KEY = 'skyfly.codeVerifier'
const REDIRECT_URI_KEY = 'skyfly.redirectUri'

export const authStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY) ?? '',
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY) ?? '',
  getCodeVerifier: () => localStorage.getItem(CODE_VERIFIER_KEY) ?? '',
  getRedirectUri: (fallback: string) => localStorage.getItem(REDIRECT_URI_KEY) ?? fallback,
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },
  setCodeVerifier: (value: string) => localStorage.setItem(CODE_VERIFIER_KEY, value),
  setRedirectUri: (value: string) => localStorage.setItem(REDIRECT_URI_KEY, value),
  clearSession: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(CODE_VERIFIER_KEY)
  },
}
