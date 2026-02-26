import type { ApiResponse } from '../types';

type HttpMethod = 'GET' | 'POST' | 'PATCH' | 'DELETE';

interface RequestOptions {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
  query?: Record<string, string | number | boolean | undefined | null>;
}

interface ServiceConfig {
  baseUrl: string;
}

const DEFAULT_USER_ID = 'U-CUSTOMER-1';
const DEFAULT_ACTOR_TYPE = 'customer';

const resolveServiceConfig = (): Record<
  'gateway' | 'auth' | 'flight' | 'booking' | 'customer' | 'payment',
  ServiceConfig
> => ({
  gateway: { baseUrl: import.meta.env.VITE_GATEWAY_API_URL || '/gateway-api' },
  auth: { baseUrl: import.meta.env.VITE_AUTH_API_URL || '/auth-api' },
  flight: { baseUrl: import.meta.env.VITE_FLIGHT_API_URL || '/flight-api' },
  booking: { baseUrl: import.meta.env.VITE_BOOKING_API_URL || '/booking-api' },
  customer: { baseUrl: import.meta.env.VITE_CUSTOMER_API_URL || '/customer-api' },
  payment: { baseUrl: import.meta.env.VITE_PAYMENT_API_URL || '/payment-api' },
});

const serviceConfig = resolveServiceConfig();

const buildQueryString = (query?: RequestOptions['query']): string => {
  if (!query) {
    return '';
  }

  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).length > 0) {
      params.append(key, String(value));
    }
  });

  const encoded = params.toString();
  return encoded ? `?${encoded}` : '';
};

export const getAuthContext = () => {
  const token = localStorage.getItem('token') || '';
  const actorType = localStorage.getItem('actorType') || DEFAULT_ACTOR_TYPE;
  const userId = localStorage.getItem('userId') || DEFAULT_USER_ID;

  return {
    token,
    actorType,
    userId,
  };
};

const buildHeaders = (customHeaders?: Record<string, string>): Record<string, string> => {
  const context = getAuthContext();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-Actor-Type': context.actorType,
    'X-User-Id': context.userId,
    ...customHeaders,
  };

  if (context.token) {
    headers.Authorization = `Bearer ${context.token}`;
  }

  return headers;
};

const createErrorResponse = <T>(message: string, details?: unknown): ApiResponse<T> => ({
  success: false,
  error: {
    code: 'API_ERROR',
    message,
    details: details && typeof details === 'object' ? (details as Record<string, unknown>) : undefined,
  },
  timestamp: new Date(),
});

export const apiRequest = async <T>(
  service: keyof typeof serviceConfig,
  path: string,
  options: RequestOptions = {}
): Promise<ApiResponse<T>> => {
  const { method = 'GET', body, headers, query } = options;

  const base = serviceConfig[service].baseUrl.replace(/\/+$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${base}${normalizedPath}${buildQueryString(query)}`;

  try {
    const response = await fetch(url, {
      method,
      headers: buildHeaders(headers),
      body: body === undefined ? undefined : JSON.stringify(body),
    });

    if (response.status === 204) {
      return {
        success: true,
        data: undefined,
        timestamp: new Date(),
      };
    }

    const data = await response.json().catch(() => undefined);

    if (!response.ok) {
      const message =
        data?.message ||
        data?.detail ||
        data?.error ||
        `Request failed with status ${response.status}`;
      return createErrorResponse<T>(message, data);
    }

    return {
      success: true,
      data: data as T,
      timestamp: new Date(),
    };
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Network request failed';
    return createErrorResponse<T>(message);
  }
};

export const toDate = (value: string | Date | undefined): Date => {
  if (!value) {
    return new Date();
  }
  return value instanceof Date ? value : new Date(value);
};
