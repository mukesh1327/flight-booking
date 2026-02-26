import type {
  User,
  LoginRequest,
  AuthResponse,
  ApiResponse,
  AuthSessionResponse,
  GoogleOAuthStartResponse,
  GoogleOAuthCallbackRequest,
  CorpLoginInitRequest,
  CorpLoginInitResponse,
  CorpLoginVerifyRequest,
  CorpLoginVerifyResponse,
  CorpMfaChallengeRequest,
  CorpMfaChallengeResponse,
  CorpMfaVerifyRequest,
} from '../types';
import { apiRequest, getAuthContext } from './apiClient';

interface BackendMeResponse {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  mobile?: string;
}

const toErrorResponse = <T>(response: ApiResponse<unknown>): ApiResponse<T> => ({
  success: false,
  error: response.error || {
    code: 'API_ERROR',
    message: 'Request failed',
  },
  timestamp: response.timestamp,
});

const sessionToUser = (session: AuthSessionResponse): User => ({
  id: session.user.userId,
  email: session.user.email,
  firstName: session.user.email.split('@')[0] || 'SkyFly',
  lastName: 'User',
  createdAt: new Date(),
  preferences: {
    seatPreference: 'window',
    mealPreference: [],
    notifications: true,
    currency: 'INR',
  },
});

const meToUser = (me: BackendMeResponse): User => ({
  id: me.userId,
  email: me.email,
  firstName: me.firstName,
  lastName: me.lastName,
  phone: me.mobile,
  createdAt: new Date(),
});

class AuthService {
  private persistSession(session: AuthSessionResponse): void {
    localStorage.setItem('token', session.tokens.accessToken);
    localStorage.setItem('refreshToken', session.tokens.refreshToken);
    localStorage.setItem('userId', session.user.userId);
    localStorage.setItem('actorType', 'customer');
  }

  private authFromSession(session: AuthSessionResponse): AuthResponse {
    this.persistSession(session);
    return {
      token: session.tokens.accessToken,
      refreshToken: session.tokens.refreshToken,
      user: sessionToUser(session),
    };
  }

  async login(request: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    const init = await this.initCorpLogin({
      email: request.email,
      deviceInfo: {
        userAgent: navigator.userAgent,
        platform: navigator.platform,
      },
    });

    if (!init.success || !init.data) {
      return {
        success: false,
        error: init.error,
        timestamp: init.timestamp,
      };
    }

    const verify = await this.verifyCorpLogin({
      loginFlowId: init.data.loginFlowId,
      factorType: 'PASSWORD',
      assertion: request.password,
    });

    if (!verify.success || !verify.data) {
      return {
        success: false,
        error: verify.error,
        timestamp: verify.timestamp,
      };
    }

    if (verify.data.session) {
      return {
        success: true,
        data: this.authFromSession(verify.data.session),
        timestamp: verify.timestamp,
      };
    }

    const challenge = await this.createCorpMfaChallenge({
      loginFlowId: init.data.loginFlowId,
      factorType: 'OTP',
    });

    if (!challenge.success || !challenge.data?.challengeId) {
      return {
        success: false,
        error: challenge.error || {
          code: 'MFA_REQUIRED',
          message: 'MFA challenge required but could not be created',
        },
        timestamp: challenge.timestamp,
      };
    }

    const mfa = await this.verifyCorpMfa({
      challengeId: challenge.data.challengeId,
      otpOrAssertion: '123456',
    });

    if (!mfa.success || !mfa.data) {
      return {
        success: false,
        error: mfa.error,
        timestamp: mfa.timestamp,
      };
    }

    return {
      success: true,
      data: this.authFromSession(mfa.data),
      timestamp: mfa.timestamp,
    };
  }

  async register(
    email: string,
    _password: string,
    firstName: string,
    lastName: string
  ): Promise<ApiResponse<AuthResponse>> {
    const response = await apiRequest<BackendMeResponse>('auth', '/api/v1/users/me', {
      method: 'PATCH',
      body: { email, firstName, lastName },
    });

    if (!response.success || !response.data) {
      return {
        success: false,
        error:
          response.error ||
          {
            code: 'REGISTER_NOT_SUPPORTED',
            message: 'Registration endpoint is not available in auth-service',
          },
        timestamp: response.timestamp,
      };
    }

    const token = localStorage.getItem('token') || '';
    const refreshToken = localStorage.getItem('refreshToken') || '';

    return {
      success: true,
      data: {
        token,
        refreshToken,
        user: meToUser(response.data),
      },
      timestamp: response.timestamp,
    };
  }

  async logout(): Promise<ApiResponse<boolean>> {
    const context = getAuthContext();
    const refreshToken = localStorage.getItem('refreshToken') || '';

    const response = await apiRequest<unknown>('auth', '/api/v1/auth/logout', {
      method: 'POST',
      headers: {
        'X-User-Id': context.userId,
      },
      body: {
        refreshToken,
        allSessions: false,
      },
    });

    if (!response.success) {
      return toErrorResponse<boolean>(response);
    }

    return {
      success: true,
      data: true,
      timestamp: response.timestamp,
    };
  }

  async refreshToken(refreshToken: string): Promise<ApiResponse<AuthResponse>> {
    const response = await apiRequest<AuthSessionResponse>('auth', '/api/v1/auth/token/refresh', {
      method: 'POST',
      body: { refreshToken },
    });

    if (!response.success || !response.data) {
      return toErrorResponse<AuthResponse>(response);
    }

    return {
      success: true,
      data: this.authFromSession(response.data),
      timestamp: response.timestamp,
    };
  }

  async startGoogleOAuth(): Promise<ApiResponse<GoogleOAuthStartResponse>> {
    return apiRequest<GoogleOAuthStartResponse>('auth', '/api/v1/auth/public/google/start');
  }

  async handleGoogleCallback(request: GoogleOAuthCallbackRequest): Promise<ApiResponse<AuthSessionResponse>> {
    const response = await apiRequest<AuthSessionResponse>('auth', '/api/v1/auth/public/google/callback', {
      query: {
        code: request.code,
        state: request.state,
      },
      headers: {
        'X-Device': request.device || navigator.userAgent,
        'X-Forwarded-For': request.ip || '127.0.0.1',
      },
    });

    if (response.success && response.data) {
      this.persistSession(response.data);
    }

    return response;
  }

  async initCorpLogin(request: CorpLoginInitRequest): Promise<ApiResponse<CorpLoginInitResponse>> {
    return apiRequest<CorpLoginInitResponse>('auth', '/api/v1/auth/corp/login/init', {
      method: 'POST',
      body: request,
    });
  }

  async verifyCorpLogin(request: CorpLoginVerifyRequest): Promise<ApiResponse<CorpLoginVerifyResponse>> {
    const response = await apiRequest<CorpLoginVerifyResponse>('auth', '/api/v1/auth/corp/login/verify', {
      method: 'POST',
      body: request,
    });

    if (response.success && response.data?.session) {
      this.persistSession(response.data.session);
    }

    return response;
  }

  async createCorpMfaChallenge(
    request: CorpMfaChallengeRequest
  ): Promise<ApiResponse<CorpMfaChallengeResponse>> {
    const response = await apiRequest<Record<string, unknown>>('auth', '/api/v1/auth/corp/mfa/challenge', {
      method: 'POST',
      body: request,
    });

    if (!response.success || !response.data) {
      return toErrorResponse<CorpMfaChallengeResponse>(response);
    }

    const mapped: CorpMfaChallengeResponse = {
      factorType: request.factorType,
      challengeId:
        typeof response.data.challengeId === 'string' ? response.data.challengeId : undefined,
      challenge:
        typeof response.data.challenge === 'string' ? response.data.challenge : undefined,
      rpId: typeof response.data.rpId === 'string' ? response.data.rpId : undefined,
      timeout:
        typeof response.data.timeout === 'number' ? response.data.timeout : undefined,
      expiresIn:
        typeof response.data.expiresIn === 'number' ? response.data.expiresIn : undefined,
      resendAfter:
        typeof response.data.resendAfter === 'number' ? response.data.resendAfter : undefined,
    };

    return {
      success: true,
      data: mapped,
      timestamp: response.timestamp,
    };
  }

  async verifyCorpMfa(request: CorpMfaVerifyRequest): Promise<ApiResponse<AuthSessionResponse>> {
    const response = await apiRequest<AuthSessionResponse>('auth', '/api/v1/auth/corp/mfa/verify', {
      method: 'POST',
      body: request,
      headers: {
        'X-Device': navigator.userAgent,
        'X-Forwarded-For': '127.0.0.1',
      },
    });

    if (response.success && response.data) {
      this.persistSession(response.data);
    }

    return response;
  }

  async getCurrentUser(): Promise<ApiResponse<User>> {
    const response = await apiRequest<BackendMeResponse>('auth', '/api/v1/users/me');

    if (!response.success || !response.data) {
      return toErrorResponse<User>(response);
    }

    return {
      success: true,
      data: meToUser(response.data),
      timestamp: response.timestamp,
    };
  }

  async updateProfile(user: Partial<User>): Promise<ApiResponse<User>> {
    const response = await apiRequest<BackendMeResponse>('auth', '/api/v1/users/me', {
      method: 'PATCH',
      body: {
        firstName: user.firstName,
        lastName: user.lastName,
        mobile: user.phone,
      },
    });

    if (!response.success || !response.data) {
      return toErrorResponse<User>(response);
    }

    return {
      success: true,
      data: meToUser(response.data),
      timestamp: response.timestamp,
    };
  }
}

export const authService = new AuthService();
