/**
 * Auth Service
 * Handles authentication and authorization (mocked)
 */

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
import { MOCK_CURRENT_USER } from '../constants/users';

class AuthService {
  private corpLoginSessions = new Map<
    string,
    { email: string; allowedFactors: string[]; requiresStepUp: boolean }
  >();
  private mfaChallenges = new Map<string, { loginFlowId: string; otp: string }>();

  private generateAuthResponse(user: User = MOCK_CURRENT_USER): AuthResponse {
    return {
      token: `token-${Date.now()}`,
      refreshToken: `refresh-${Date.now()}`,
      user,
    };
  }

  private generateSessionResponse(): AuthSessionResponse {
    return {
      tokens: {
        accessToken: `access-${Date.now()}`,
        refreshToken: `refresh-${Date.now()}`,
        expiresIn: 900,
      },
      user: {
        userId: MOCK_CURRENT_USER.id,
        email: MOCK_CURRENT_USER.email,
        realm: 'skyfly-corp',
        status: 'ACTIVE',
        roles: ['CORP_USER'],
      },
      isNewUser: false,
      profileStatus: 'COMPLETE',
      mfaLevel: 'MFA_VERIFIED',
    };
  }

  /**
   * Login with email and password
   */
  async login(request: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        if (request.email === 'john.doe@example.com' && request.password === 'password') {
          resolve({
            success: true,
            data: this.generateAuthResponse(),
            timestamp: new Date(),
          });
        } else {
          resolve({
            success: false,
            error: {
              code: 'INVALID_CREDENTIALS',
              message: 'Invalid email or password',
            },
            timestamp: new Date(),
          });
        }
      }, 500);
    });
  }

  /**
   * Register a new user
   */
  async register(
    email: string,
    password: string,
    firstName: string,
    lastName: string
  ): Promise<ApiResponse<AuthResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        void password;
        const newUser: User = {
          id: `user-${Date.now()}`,
          email,
          firstName,
          lastName,
          createdAt: new Date(),
          preferences: {
            seatPreference: 'window',
            mealPreference: [],
            notifications: true,
            currency: 'INR',
          },
        };

        resolve({
          success: true,
          data: this.generateAuthResponse(newUser),
          timestamp: new Date(),
        });
      }, 600);
    });
  }

  /**
   * Logout current user
   */
  async logout(): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          data: true,
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * Refresh auth token
   */
  async refreshToken(refreshToken: string): Promise<ApiResponse<AuthResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        void refreshToken;
        resolve({
          success: true,
          data: this.generateAuthResponse(),
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * GET /api/v1/auth/public/google/start
   */
  async startGoogleOAuth(): Promise<ApiResponse<GoogleOAuthStartResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const state = `google-state-${Date.now()}`;
        resolve({
          success: true,
          data: {
            state,
            codeChallengeMethod: 'S256',
            authorizationUrl:
              'https://accounts.google.com/o/oauth2/v2/auth?scope=openid%20email&response_type=code',
          },
          timestamp: new Date(),
        });
      }, 200);
    });
  }

  /**
   * GET /api/v1/auth/public/google/callback
   */
  async handleGoogleCallback(
    request: GoogleOAuthCallbackRequest
  ): Promise<ApiResponse<AuthSessionResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        if (!request.code || !request.state) {
          resolve({
            success: false,
            error: {
              code: 'INVALID_GOOGLE_CALLBACK',
              message: 'Missing oauth code or state',
            },
            timestamp: new Date(),
          });
          return;
        }

        resolve({
          success: true,
          data: this.generateSessionResponse(),
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * POST /api/v1/auth/corp/login/init
   */
  async initCorpLogin(
    request: CorpLoginInitRequest
  ): Promise<ApiResponse<CorpLoginInitResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const loginFlowId = `flow-${Date.now()}`;
        const allowedFactors = ['PASSWORD', 'PASSKEY'];
        const requiresStepUp = true;
        this.corpLoginSessions.set(loginFlowId, {
          email: request.email,
          allowedFactors,
          requiresStepUp,
        });

        resolve({
          success: true,
          data: {
            loginFlowId,
            allowedFactors,
            requiresStepUp,
          },
          timestamp: new Date(),
        });
      }, 250);
    });
  }

  /**
   * POST /api/v1/auth/corp/login/verify
   */
  async verifyCorpLogin(
    request: CorpLoginVerifyRequest
  ): Promise<ApiResponse<CorpLoginVerifyResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const loginSession = this.corpLoginSessions.get(request.loginFlowId);

        if (!loginSession) {
          resolve({
            success: false,
            error: {
              code: 'LOGIN_FLOW_NOT_FOUND',
              message: 'Login flow not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        if (!loginSession.allowedFactors.includes(request.factorType)) {
          resolve({
            success: false,
            error: {
              code: 'FACTOR_NOT_ALLOWED',
              message: `Factor ${request.factorType} is not allowed`,
            },
            timestamp: new Date(),
          });
          return;
        }

        if (!request.assertion || request.assertion.length < 4) {
          resolve({
            success: false,
            error: {
              code: 'INVALID_ASSERTION',
              message: 'Assertion is invalid',
            },
            timestamp: new Date(),
          });
          return;
        }

        const isPasswordValid =
          request.factorType !== 'PASSWORD' || request.assertion === 'dummy-assertion';
        if (!isPasswordValid) {
          resolve({
            success: false,
            error: {
              code: 'INVALID_CREDENTIALS',
              message: 'Invalid corporate credentials',
            },
            timestamp: new Date(),
          });
          return;
        }

        resolve({
          success: true,
          data: {
            session: loginSession.requiresStepUp ? undefined : this.generateSessionResponse(),
            challengeRequired: loginSession.requiresStepUp,
            challengeType: loginSession.requiresStepUp ? 'OTP' : undefined,
            challengeMetadata: loginSession.requiresStepUp
              ? { hint: 'Use 123456 as mock OTP' }
              : undefined,
          },
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * POST /api/v1/auth/corp/mfa/challenge
   */
  async createCorpMfaChallenge(
    request: CorpMfaChallengeRequest
  ): Promise<ApiResponse<CorpMfaChallengeResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        if (!this.corpLoginSessions.has(request.loginFlowId)) {
          resolve({
            success: false,
            error: {
              code: 'LOGIN_FLOW_NOT_FOUND',
              message: 'Login flow not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        if (request.factorType === 'PASSKEY') {
          resolve({
            success: true,
            data: {
              factorType: request.factorType,
              challenge: `webauthn-${Date.now()}`,
              rpId: 'localhost',
              timeout: 60000,
            },
            timestamp: new Date(),
          });
          return;
        }

        const challengeId = `otp-${Date.now()}`;
        this.mfaChallenges.set(challengeId, { loginFlowId: request.loginFlowId, otp: '123456' });
        resolve({
          success: true,
          data: {
            factorType: request.factorType,
            challengeId,
            expiresIn: 300,
            resendAfter: 30,
          },
          timestamp: new Date(),
        });
      }, 250);
    });
  }

  /**
   * POST /api/v1/auth/corp/mfa/verify
   */
  async verifyCorpMfa(
    request: CorpMfaVerifyRequest
  ): Promise<ApiResponse<AuthSessionResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const challenge = this.mfaChallenges.get(request.challengeId);
        if (!challenge || challenge.otp !== request.otpOrAssertion) {
          resolve({
            success: false,
            error: {
              code: 'INVALID_MFA_OTP',
              message: 'Invalid MFA code',
            },
            timestamp: new Date(),
          });
          return;
        }

        this.mfaChallenges.delete(request.challengeId);
        resolve({
          success: true,
          data: this.generateSessionResponse(),
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * Get current user
   */
  async getCurrentUser(): Promise<ApiResponse<User>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          data: MOCK_CURRENT_USER,
          timestamp: new Date(),
        });
      }, 200);
    });
  }

  /**
   * Update user profile
   */
  async updateProfile(user: Partial<User>): Promise<ApiResponse<User>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const updatedUser = { ...MOCK_CURRENT_USER, ...user };
        resolve({
          success: true,
          data: updatedUser,
          timestamp: new Date(),
        });
      }, 400);
    });
  }
}

export const authService = new AuthService();
