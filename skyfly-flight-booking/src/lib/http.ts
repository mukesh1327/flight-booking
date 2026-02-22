import { AUTH_API_BASE_URL } from '../config/env'
import type { ApiErrorBody } from '../types/auth'

export class ApiClientError extends Error {
  status: number
  code?: string
  correlationId?: string

  constructor(status: number, message: string, code?: string, correlationId?: string) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.code = code
    this.correlationId = correlationId
  }
}

const toApiError = (status: number, payload: unknown): ApiClientError => {
  const fallback = `Request failed with status ${status}`

  if (typeof payload === 'string') {
    return new ApiClientError(status, payload || fallback)
  }

  const body = payload as ApiErrorBody | null
  if (!body) {
    return new ApiClientError(status, fallback)
  }

  const details = [body.code, body.message].filter(Boolean).join(': ')
  return new ApiClientError(status, details || fallback, body.code, body.correlationId)
}

export const apiRequest = async <T,>(
  path: string,
  method: string,
  options?: { body?: unknown; token?: string },
): Promise<T> => {
  const response = await fetch(`${AUTH_API_BASE_URL}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'x-correlation-id': crypto.randomUUID(),
      ...(options?.token ? { Authorization: `Bearer ${options.token}` } : {}),
    },
    ...(options?.body === undefined ? {} : { body: JSON.stringify(options.body) }),
  })

  const text = await response.text()
  const payload = text ? (JSON.parse(text) as unknown) : null
  if (!response.ok) {
    throw toApiError(response.status, payload)
  }

  return payload as T
}
