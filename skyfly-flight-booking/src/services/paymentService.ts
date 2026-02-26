/**
 * Payment Service
 * Handles payment-related API calls (mocked)
 */

import type {
  PaymentIntent,
  PaymentIntentRequest,
  PaymentAuthorizeRequest,
  PaymentCaptureRequest,
  PaymentWebhookPayload,
  ApiResponse,
} from '../types';

class PaymentService {
  private intents = new Map<string, PaymentIntent>();

  /**
   * Create a payment intent
   */
  async createPaymentIntent(
    request: PaymentIntentRequest
  ): Promise<ApiResponse<PaymentIntent>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const intent: PaymentIntent = {
          id: `pi-${Date.now()}`,
          amount: request.amount,
          currency: request.currency,
          status: 'pending',
          bookingId: request.bookingId,
          createdAt: new Date(),
          expiresAt: new Date(Date.now() + 15 * 60 * 1000), // 15 minutes
        };
        this.intents.set(intent.id, intent);

        resolve({
          success: true,
          data: intent,
          timestamp: new Date(),
        });
      }, 400);
    });
  }

  /**
   * Authorize payment
   */
  async authorizePayment(
    request: PaymentAuthorizeRequest
  ): Promise<ApiResponse<PaymentIntent>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const existing = this.intents.get(request.paymentId);
        const intent: PaymentIntent = existing
          ? { ...existing, status: 'authorized' }
          : {
              id: request.paymentId,
              amount: 5600,
              currency: 'INR',
              status: 'authorized',
              bookingId: 'bk-001',
              createdAt: new Date(),
              expiresAt: new Date(Date.now() + 15 * 60 * 1000),
            };
        this.intents.set(intent.id, intent);

        resolve({
          success: true,
          data: intent,
          timestamp: new Date(),
        });
      }, 500);
    });
  }

  /**
   * Capture payment
   */
  async capturePayment(
    request: PaymentCaptureRequest
  ): Promise<ApiResponse<PaymentIntent>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const existing = this.intents.get(request.paymentId);
        const intent: PaymentIntent = existing
          ? { ...existing, status: 'captured' }
          : {
              id: request.paymentId,
              amount: 5600,
              currency: 'INR',
              status: 'captured',
              bookingId: 'bk-001',
              createdAt: new Date(),
              expiresAt: new Date(Date.now() + 15 * 60 * 1000),
            };
        this.intents.set(intent.id, intent);

        resolve({
          success: true,
          data: intent,
          timestamp: new Date(),
        });
      }, 600);
    });
  }

  /**
   * Refund payment
   */
  async refundPayment(paymentId: string): Promise<ApiResponse<PaymentIntent>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const existing = this.intents.get(paymentId);
        const intent: PaymentIntent = existing
          ? { ...existing, status: 'refunded' }
          : {
              id: paymentId,
              amount: 5600,
              currency: 'INR',
              status: 'refunded',
              bookingId: 'bk-001',
              createdAt: new Date(),
              expiresAt: new Date(Date.now() + 15 * 60 * 1000),
            };
        this.intents.set(intent.id, intent);

        resolve({
          success: true,
          data: intent,
          timestamp: new Date(),
        });
      }, 500);
    });
  }

  /**
   * Get payment status
   */
  async getPaymentStatus(paymentId: string): Promise<ApiResponse<PaymentIntent>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const intent = this.intents.get(paymentId);
        if (!intent) {
          resolve({
            success: false,
            error: {
              code: 'PAYMENT_NOT_FOUND',
              message: 'Payment intent not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        resolve({
          success: true,
          data: intent,
          timestamp: new Date(),
        });
      }, 200);
    });
  }

  /**
   * POST /api/v1/payments/webhooks/provider
   */
  async processProviderWebhook(
    payload: PaymentWebhookPayload
  ): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const intent = this.intents.get(payload.paymentId);
        if (intent) {
          this.intents.set(payload.paymentId, { ...intent, status: payload.status });
        }
        resolve({
          success: true,
          data: true,
          timestamp: new Date(),
        });
      }, 250);
    });
  }
}

export const paymentService = new PaymentService();
