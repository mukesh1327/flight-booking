/**
 * User Service
 * Handles user profile and preferences (mocked)
 */

import type {
  User,
  ApiResponse,
  EmailNotification,
  SMSNotification,
  PushNotification,
} from '../types';
import { MOCK_CURRENT_USER } from '../constants/users';

class UserService {
  /**
   * GET /api/v1/users/me
   */
  async getMe(): Promise<ApiResponse<User>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          data: MOCK_CURRENT_USER,
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * PATCH /api/v1/users/me
   */
  async patchMe(updates: Partial<User>): Promise<ApiResponse<User>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const updated = { ...MOCK_CURRENT_USER, ...updates };
        resolve({
          success: true,
          data: updated,
          timestamp: new Date(),
        });
      }, 400);
    });
  }

  /**
   * POST /api/v1/users/me/mobile/verify/request
   */
  async requestMobileVerification(phone: string): Promise<ApiResponse<string>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          data: `otp-request-${phone}-${Date.now()}`,
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * POST /api/v1/users/me/mobile/verify/confirm
   */
  async confirmMobileVerification(
    requestId: string,
    otp: string
  ): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        // Mock OTP validation
        void requestId;
        if (otp === '123456') {
          resolve({
            success: true,
            data: true,
            timestamp: new Date(),
          });
        } else {
          resolve({
            success: false,
            error: {
              code: 'INVALID_OTP',
              message: 'Invalid OTP',
            },
            timestamp: new Date(),
          });
        }
      }, 400);
    });
  }

  /**
   * POST /api/v1/notifications/email
   */
  async sendEmailNotification(
    payload: EmailNotification
  ): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        void payload;
        resolve({
          success: true,
          data: true,
          timestamp: new Date(),
        });
      }, 250);
    });
  }

  /**
   * POST /api/v1/notifications/sms
   */
  async sendSmsNotification(payload: SMSNotification): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        void payload;
        resolve({
          success: true,
          data: true,
          timestamp: new Date(),
        });
      }, 250);
    });
  }

  /**
   * POST /api/v1/notifications/push
   */
  async sendPushNotification(
    payload: PushNotification
  ): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        void payload;
        resolve({
          success: true,
          data: true,
          timestamp: new Date(),
        });
      }, 250);
    });
  }

  // Backward-compatible aliases
  async getProfile(): Promise<ApiResponse<User>> {
    return this.getMe();
  }

  async updateProfile(updates: Partial<User>): Promise<ApiResponse<User>> {
    return this.patchMe(updates);
  }
}

export const userService = new UserService();
