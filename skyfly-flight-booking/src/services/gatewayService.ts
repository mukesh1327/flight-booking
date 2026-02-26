/**
 * Gateway Service
 * Handles API gateway utility endpoints (mocked)
 */

import type { ApiResponse } from '../types';

interface HealthStatus {
  status: 'ok';
  service: string;
  checkedAt: Date;
}

class GatewayService {
  /**
   * GET /api/v1/health
   */
  async healthCheck(): Promise<ApiResponse<HealthStatus>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          data: {
            status: 'ok',
            service: 'api-gateway',
            checkedAt: new Date(),
          },
          timestamp: new Date(),
        });
      }, 120);
    });
  }
}

export const gatewayService = new GatewayService();
