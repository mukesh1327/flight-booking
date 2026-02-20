import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type RegisterRequest = {
  name: string
  username: string
  email: string
  password: string
  phone: string
}

type LoginRequest = {
  code: string
  codeVerifier: string
  redirectUri: string
}

type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: string
}

type ApiError = {
  code?: string
  message?: string
  correlationId?: string
}

type AuthorizeResponse = {
  authorizationUrl: string
  codeVerifier: string
  state: string
  redirectUri: string
}

type UserProfile = {
  userId: string
  name: string
  email: string
  phone: string
}

const ACCESS_TOKEN_KEY = 'skyfly.accessToken'
const REFRESH_TOKEN_KEY = 'skyfly.refreshToken'
const CODE_VERIFIER_KEY = 'skyfly.codeVerifier'
const REDIRECT_URI_KEY = 'skyfly.redirectUri'

const buildDefaultRedirectUri = () => `${window.location.origin}/auth/callback`

const decodeRoles = (token: string): string[] => {
  if (!token) {
    return []
  }

  try {
    const base64Payload = token.split('.')[1]
    if (!base64Payload) {
      return []
    }

    const payload = atob(base64Payload.replace(/-/g, '+').replace(/_/g, '/'))
    const parsed = JSON.parse(payload) as {
      realm_access?: { roles?: string[] }
      resource_access?: Record<string, { roles?: string[] }>
    }

    const roles = new Set<string>()
    parsed.realm_access?.roles?.forEach((role) => roles.add(role))
    const clientRoles = parsed.resource_access?.['authservice-client']?.roles ?? []
    clientRoles.forEach((role) => roles.add(role))

    return Array.from(roles)
  } catch {
    return []
  }
}

const toErrorMessage = (status: number, payload: unknown) => {
  const fallback = `Request failed with status ${status}`

  if (typeof payload === 'string') {
    return payload || fallback
  }

  const apiError = payload as ApiError | null
  if (!apiError) {
    return fallback
  }

  const pieces: string[] = []
  if (apiError.code) {
    pieces.push(apiError.code)
  }
  if (apiError.message) {
    pieces.push(apiError.message)
  }
  return pieces.join(': ') || fallback
}

function App() {
  const [apiBaseUrl, setApiBaseUrl] = useState(import.meta.env.VITE_AUTH_API_URL ?? 'http://localhost:8080')
  const [redirectUri, setRedirectUri] = useState(
    localStorage.getItem(REDIRECT_URI_KEY) ?? buildDefaultRedirectUri(),
  )
  const [registerForm, setRegisterForm] = useState<RegisterRequest>({
    name: '',
    username: '',
    email: '',
    password: '',
    phone: '',
  })
  const [authCode, setAuthCode] = useState('')
  const [codeVerifier, setCodeVerifier] = useState(localStorage.getItem(CODE_VERIFIER_KEY) ?? '')
  const [accessToken, setAccessToken] = useState(localStorage.getItem(ACCESS_TOKEN_KEY) ?? '')
  const [refreshToken, setRefreshToken] = useState(localStorage.getItem(REFRESH_TOKEN_KEY) ?? '')
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const roles = useMemo(() => decodeRoles(accessToken), [accessToken])

  useEffect(() => {
    localStorage.setItem(REDIRECT_URI_KEY, redirectUri)
  }, [redirectUri])

  useEffect(() => {
    const fromQuery = new URLSearchParams(window.location.search).get('code')
    if (fromQuery) {
      setAuthCode(fromQuery)
      setSuccessMessage('Authorization code received. Click "Exchange Code".')
    }
  }, [])

  const request = async <T,>(path: string, method: string, body?: unknown, token?: string): Promise<T> => {
    const correlationId = crypto.randomUUID()
    const response = await fetch(`${apiBaseUrl}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'x-correlation-id': correlationId,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    })

    const text = await response.text()
    const payload = text ? (JSON.parse(text) as unknown) : null
    if (!response.ok) {
      throw new Error(toErrorMessage(response.status, payload))
    }
    return payload as T
  }

  const clearMessages = () => {
    setErrorMessage('')
    setSuccessMessage('')
  }

  const onRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    clearMessages()
    setIsLoading(true)

    try {
      const response = await request<{ message: string; userId: string }>('/auth/register', 'POST', registerForm)
      setSuccessMessage(`${response.message}. userId: ${response.userId}`)
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  const onStartLogin = async () => {
    clearMessages()
    setIsLoading(true)

    try {
      const query = new URLSearchParams({ redirectUri })
      const response = await request<AuthorizeResponse>(`/auth/login/authorize?${query.toString()}`, 'GET')
      setCodeVerifier(response.codeVerifier)
      localStorage.setItem(CODE_VERIFIER_KEY, response.codeVerifier)
      window.location.assign(response.authorizationUrl)
    } catch (error) {
      setErrorMessage((error as Error).message)
      setIsLoading(false)
    }
  }

  const onExchangeCode = async () => {
    clearMessages()
    if (!authCode || !codeVerifier) {
      setErrorMessage('Authorization code and code verifier are required.')
      return
    }

    setIsLoading(true)
    const payload: LoginRequest = { code: authCode, codeVerifier, redirectUri }

    try {
      const response = await request<AuthResponse>('/auth/login', 'POST', payload)
      setAccessToken(response.accessToken)
      setRefreshToken(response.refreshToken)
      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
      setSuccessMessage('Login successful.')
      window.history.replaceState({}, document.title, '/')
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  const onRefreshToken = async () => {
    clearMessages()
    if (!refreshToken) {
      setErrorMessage('Refresh token missing.')
      return
    }

    setIsLoading(true)
    try {
      const response = await request<AuthResponse>('/auth/token/refresh', 'POST', { refreshToken })
      setAccessToken(response.accessToken)
      setRefreshToken(response.refreshToken)
      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
      setSuccessMessage('Token refreshed.')
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  const onLogout = async () => {
    clearMessages()
    if (!accessToken || !refreshToken) {
      setErrorMessage('Access token and refresh token are required to logout.')
      return
    }

    setIsLoading(true)
    try {
      await request<{ message: string }>(
        '/auth/logout',
        'POST',
        { accessToken, refreshToken },
        accessToken,
      )
      setAccessToken('')
      setRefreshToken('')
      setProfile(null)
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(CODE_VERIFIER_KEY)
      setSuccessMessage('Logged out.')
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  const onGetProfile = async () => {
    clearMessages()
    if (!accessToken) {
      setErrorMessage('Login first to access profile.')
      return
    }

    setIsLoading(true)
    try {
      const response = await request<UserProfile>('/auth/profile', 'GET', undefined, accessToken)
      setProfile(response)
      setSuccessMessage('Profile fetched.')
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <p className="eyebrow">SkyFly</p>
        <h1>Flight Booking Auth Console</h1>
        <p>Integrated with `authservice` register and PKCE login flow.</p>
      </header>

      <section className="grid">
        <article className="panel">
          <h2>Service Config</h2>
          <label>
            API Base URL
            <input value={apiBaseUrl} onChange={(event) => setApiBaseUrl(event.target.value)} />
          </label>
          <label>
            Redirect URI
            <input value={redirectUri} onChange={(event) => setRedirectUri(event.target.value)} />
          </label>
        </article>

        <article className="panel">
          <h2>Register Customer</h2>
          <form onSubmit={onRegister}>
            <label>
              Name
              <input
                required
                value={registerForm.name}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, name: event.target.value }))}
              />
            </label>
            <label>
              Username
              <input
                required
                value={registerForm.username}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, username: event.target.value }))}
              />
            </label>
            <label>
              Email
              <input
                required
                type="email"
                value={registerForm.email}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, email: event.target.value }))}
              />
            </label>
            <label>
              Password
              <input
                required
                type="password"
                value={registerForm.password}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, password: event.target.value }))}
              />
            </label>
            <label>
              Phone
              <input
                required
                value={registerForm.phone}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, phone: event.target.value }))}
              />
            </label>
            <button type="submit" disabled={isLoading}>
              Register
            </button>
          </form>
        </article>

        <article className="panel">
          <h2>Login (PKCE)</h2>
          <div className="button-row">
            <button type="button" onClick={onStartLogin} disabled={isLoading}>
              Start Login
            </button>
            <button type="button" className="secondary" onClick={onExchangeCode} disabled={isLoading}>
              Exchange Code
            </button>
          </div>
          <label>
            Authorization Code
            <textarea value={authCode} onChange={(event) => setAuthCode(event.target.value)} />
          </label>
          <label>
            Code Verifier
            <textarea value={codeVerifier} onChange={(event) => setCodeVerifier(event.target.value)} />
          </label>
        </article>

        <article className="panel">
          <h2>Session</h2>
          <div className="button-row">
            <button type="button" onClick={onRefreshToken} disabled={isLoading}>
              Refresh Token
            </button>
            <button type="button" className="secondary" onClick={onLogout} disabled={isLoading}>
              Logout
            </button>
            <button type="button" className="secondary" onClick={onGetProfile} disabled={isLoading}>
              Get Profile
            </button>
          </div>
          <p>
            Access token: <strong>{accessToken ? 'present' : 'missing'}</strong>
          </p>
          <p>
            Refresh token: <strong>{refreshToken ? 'present' : 'missing'}</strong>
          </p>
          <p>
            Roles: <strong>{roles.length ? roles.join(', ') : 'none'}</strong>
          </p>
          {profile ? (
            <pre>{JSON.stringify(profile, null, 2)}</pre>
          ) : (
            <p className="hint">Profile not loaded.</p>
          )}
        </article>
      </section>

      {successMessage ? <p className="status ok">{successMessage}</p> : null}
      {errorMessage ? <p className="status err">{errorMessage}</p> : null}
    </main>
  )
}

export default App
