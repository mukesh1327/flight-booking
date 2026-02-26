import type {
  PaymentIntent,
  PaymentIntentRequest,
  PaymentAuthorizeRequest,
  PaymentCaptureRequest,
  PaymentWebhookPayload,
  ApiResponse,
} from '../types';
import { apiRequest, toDate } from './apiClient';

interface BackendPayment {
  paymentId: string;
  bookingId: string;
  amount: number;
  currency: string;
  status: string;
  updatedAt: string;
}

const toErrorResponse = <T>(response: ApiResponse<unknown>): ApiResponse<T> => ({
  success: false,
  error: response.error || {
    code: 'API_ERROR',
    message: 'Request failed',
  },
  timestamp: response.timestamp,
});

const mapStatus = (status: string): PaymentIntent['status'] => {
  switch (status.toUpperCase()) {
    case 'AUTHORIZED':
      return 'authorized';
    case 'CAPTURED':
      return 'captured';
    case 'REFUNDED':
      return 'refunded';
    case 'FAILED':
      return 'failed';
    default:
      return 'pending';
  }
};

const mapBackendPayment = (payment: BackendPayment): PaymentIntent => ({
  id: payment.paymentId,
  amount: payment.amount,
  currency: payment.currency,
  status: mapStatus(payment.status),
  bookingId: payment.bookingId,
  createdAt: toDate(payment.updatedAt),
  expiresAt: new Date(Date.now() + 15 * 60 * 1000),
});

class PaymentService {
  async createPaymentIntent(request: PaymentIntentRequest): Promise<ApiResponse<PaymentIntent>> {
    const response = await apiRequest<BackendPayment>('payment', '/api/v1/payments/intent', {
      method: 'POST',
      body: {
        bookingId: request.bookingId,
        amount: request.amount,
        currency: request.currency,
      },
    });

    if (!response.success || !response.data) {
      return toErrorResponse<PaymentIntent>(response);
    }

    return {
      success: true,
      data: mapBackendPayment(response.data),
      timestamp: response.timestamp,
    };
  }

  async authorizePayment(request: PaymentAuthorizeRequest): Promise<ApiResponse<PaymentIntent>> {
    const response = await apiRequest<BackendPayment>('payment', `/api/v1/payments/${request.paymentId}/authorize`, {
      method: 'POST',
    });

    if (!response.success || !response.data) {
      return toErrorResponse<PaymentIntent>(response);
    }

    return {
      success: true,
      data: mapBackendPayment(response.data),
      timestamp: response.timestamp,
    };
  }

  async capturePayment(request: PaymentCaptureRequest): Promise<ApiResponse<PaymentIntent>> {
    const response = await apiRequest<BackendPayment>('payment', `/api/v1/payments/${request.paymentId}/capture`, {
      method: 'POST',
    });

    if (!response.success || !response.data) {
      return toErrorResponse<PaymentIntent>(response);
    }

    return {
      success: true,
      data: mapBackendPayment(response.data),
      timestamp: response.timestamp,
    };
  }

  async refundPayment(paymentId: string): Promise<ApiResponse<PaymentIntent>> {
    const response = await apiRequest<BackendPayment>('payment', `/api/v1/payments/${paymentId}/refund`, {
      method: 'POST',
    });

    if (!response.success || !response.data) {
      return toErrorResponse<PaymentIntent>(response);
    }

    return {
      success: true,
      data: mapBackendPayment(response.data),
      timestamp: response.timestamp,
    };
  }

  async getPaymentStatus(_paymentId: string): Promise<ApiResponse<PaymentIntent>> {
    return {
      success: false,
      error: {
        code: 'NOT_SUPPORTED',
        message: 'Payment status lookup endpoint is not exposed by payment-service',
      },
      timestamp: new Date(),
    };
  }

  async processProviderWebhook(payload: PaymentWebhookPayload): Promise<ApiResponse<boolean>> {
    const response = await apiRequest<{ accepted: boolean }>('payment', '/api/v1/payments/webhooks/provider', {
      method: 'POST',
      body: {
        provider: payload.provider,
        eventType: payload.eventType,
        payload: payload.rawPayload,
      },
    });

    if (!response.success || !response.data) {
      return toErrorResponse<boolean>(response);
    }

    return {
      success: true,
      data: response.data.accepted,
      timestamp: response.timestamp,
    };
  }
}

export const paymentService = new PaymentService();
