import { useEffect, useMemo, useState } from 'react'
import { REDIRECT_URI } from '../config/env'
import { authStorage } from '../lib/authStorage'
import { decodeRoles } from '../lib/jwt'
import { authService } from '../services/authService'
import type { RegisterRequest, UserProfile } from '../types/auth'

type Screen = 'login' | 'register'

const defaultRegisterForm: RegisterRequest = {
  name: '',
  username: '',
  email: '',
  password: '',
  phone: '',
}

export const useAuth = () => {
  const [screen, setScreen] = useState<Screen>('login')
  const [registerForm, setRegisterForm] = useState<RegisterRequest>(defaultRegisterForm)
  const [accessToken, setAccessToken] = useState(authStorage.getAccessToken())
  const [refreshToken, setRefreshToken] = useState(authStorage.getRefreshToken())
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [bootstrapped, setBootstrapped] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const isAuthenticated = Boolean(accessToken)
  const roles = useMemo(() => decodeRoles(accessToken), [accessToken])

  useEffect(() => {
    authStorage.setRedirectUri(REDIRECT_URI)
  }, [])

  useEffect(() => {
    const runCallbackFlow = async () => {
      if (window.location.pathname !== '/auth/callback') {
        setBootstrapped(true)
        return
      }

      const code = new URLSearchParams(window.location.search).get('code') ?? ''
      const codeVerifier = authStorage.getCodeVerifier()
      if (!code || !codeVerifier) {
        setErrorMessage('Missing authorization code or verifier.')
        setBootstrapped(true)
        return
      }

      setIsLoading(true)
      try {
        const auth = await authService.login({ code, codeVerifier, redirectUri: REDIRECT_URI })
        setAccessToken(auth.accessToken)
        setRefreshToken(auth.refreshToken)
        authStorage.setTokens(auth.accessToken, auth.refreshToken)
        setSuccessMessage('Signed in successfully.')
        window.history.replaceState({}, document.title, '/')
      } catch (error) {
        setErrorMessage((error as Error).message)
      } finally {
        setIsLoading(false)
        setBootstrapped(true)
      }
    }

    void runCallbackFlow()
  }, [])

  useEffect(() => {
    const loadProfile = async () => {
      if (!accessToken) {
        setProfile(null)
        return
      }

      try {
        const userProfile = await authService.getProfile(accessToken)
        setProfile(userProfile)
      } catch {
        setProfile(null)
      }
    }

    if (bootstrapped) {
      void loadProfile()
    }
  }, [accessToken, bootstrapped])

  const clearMessages = () => {
    setErrorMessage('')
    setSuccessMessage('')
  }

  const startLogin = async () => {
    clearMessages()
    setIsLoading(true)
    try {
      const authorize = await authService.buildAuthorizeUrl(REDIRECT_URI)
      authStorage.setCodeVerifier(authorize.codeVerifier)
      window.location.assign(authorize.authorizationUrl)
    } catch (error) {
      setErrorMessage((error as Error).message)
      setIsLoading(false)
    }
  }

  const register = async () => {
    clearMessages()
    setIsLoading(true)
    try {
      const response = await authService.register(registerForm)
      setSuccessMessage(`${response.message}. You can now sign in.`)
      setRegisterForm(defaultRegisterForm)
      setScreen('login')
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  const refreshSession = async () => {
    clearMessages()
    if (!refreshToken) {
      setErrorMessage('Refresh token missing.')
      return
    }

    setIsLoading(true)
    try {
      const auth = await authService.refresh({ refreshToken })
      setAccessToken(auth.accessToken)
      setRefreshToken(auth.refreshToken)
      authStorage.setTokens(auth.accessToken, auth.refreshToken)
      setSuccessMessage('Session refreshed.')
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  const logout = async () => {
    clearMessages()
    if (!accessToken || !refreshToken) {
      setErrorMessage('No active session.')
      return
    }

    setIsLoading(true)
    try {
      await authService.logout({ accessToken, refreshToken }, accessToken)
      authStorage.clearSession()
      setAccessToken('')
      setRefreshToken('')
      setProfile(null)
      setScreen('login')
      setSuccessMessage('Logged out.')
    } catch (error) {
      setErrorMessage((error as Error).message)
    } finally {
      setIsLoading(false)
    }
  }

  return {
    screen,
    setScreen,
    registerForm,
    setRegisterForm,
    isAuthenticated,
    roles,
    profile,
    isLoading,
    bootstrapped,
    errorMessage,
    successMessage,
    startLogin,
    register,
    refreshSession,
    logout,
  }
}
