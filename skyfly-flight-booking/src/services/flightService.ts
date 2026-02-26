/**
 * Flight Service
 * Handles all flight-related API calls (mocked)
 */

import type {
  Flight,
  FlightSearchRequest,
  FlightSearchResponse,
  Availability,
  ApiResponse,
  PricingQuoteRequest,
  PricingQuoteResponse,
  InventoryHoldRequest,
  InventoryHoldResponse,
  InventoryReleaseRequest,
  InventoryCommitRequest,
} from '../types';
import { MOCK_FLIGHTS } from '../constants/flights';

const buildRange = (values: number[], fallback = 0) => {
  if (!values.length) {
    return { min: fallback, max: fallback };
  }
  return {
    min: Math.min(...values),
    max: Math.max(...values),
  };
};

class FlightService {
  private holds = new Map<string, InventoryHoldResponse>();

  /**
   * Search flights based on criteria
   */
  async searchFlights(
    criteria: FlightSearchRequest
  ): Promise<ApiResponse<FlightSearchResponse>> {
    return new Promise((resolve) => {
      // Simulate network delay
      setTimeout(() => {
        // Filter mock flights based on search criteria
        const filteredFlights = MOCK_FLIGHTS.filter((flight) => {
          const segment = flight.flight.segments[0];
          return (
            segment.departureAirport.code === criteria.fromCode &&
            segment.arrivalAirport.code === criteria.toCode
          );
        });

        // Sort by price by default
        const sorted = [...filteredFlights].sort(
          (a, b) => a.pricing.totalPrice - b.pricing.totalPrice
        );

        const priceValues = sorted.map((flight) => flight.pricing.totalPrice);
        const durationValues = sorted.map((flight) => flight.flight.totalDuration);
        const stops = sorted.length
          ? Array.from(new Set(sorted.map((flight) => flight.flight.totalStops))).sort(
              (a, b) => a - b
            )
          : [0, 1];

        resolve({
          success: true,
          data: {
            flights: sorted,
            filters: {
              priceRange: buildRange(priceValues),
              airlines: Array.from(
                new Map(
                  sorted.map((f) => [
                    f.flight.segments[0].airline.code,
                    f.flight.segments[0].airline,
                  ])
                ).values()
              ),
              stops,
              departureTimeRanges: [
                { label: 'Early Morning', start: 0, end: 6 },
                { label: 'Morning', start: 6, end: 12 },
                { label: 'Afternoon', start: 12, end: 18 },
                { label: 'Evening', start: 18, end: 24 },
              ],
              duration: {
                ...buildRange(durationValues),
              },
            },
            totalResults: sorted.length,
          },
          timestamp: new Date(),
        });
      }, 800); // Simulate 800ms network delay
    });
  }

  /**
   * Get flight details
   */
  async getFlightDetails(flightId: string): Promise<ApiResponse<Flight>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const flight = MOCK_FLIGHTS.find((f) => f.flight.id === flightId);
        if (flight) {
          resolve({
            success: true,
            data: flight.flight,
            timestamp: new Date(),
          });
        } else {
          resolve({
            success: false,
            error: {
              code: 'FLIGHT_NOT_FOUND',
              message: 'Flight not found',
            },
            timestamp: new Date(),
          });
        }
      }, 300);
    });
  }

  /**
   * Get flight availability
   */
  async getFlightAvailability(flightId: string): Promise<ApiResponse<Availability>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const flight = MOCK_FLIGHTS.find((f) => f.flight.id === flightId);
        if (flight) {
          resolve({
            success: true,
            data: flight.availability,
            timestamp: new Date(),
          });
        } else {
          resolve({
            success: false,
            error: {
              code: 'FLIGHT_NOT_FOUND',
              message: 'Flight not found',
            },
            timestamp: new Date(),
          });
        }
      }, 200);
    });
  }

  /**
   * POST /api/v1/pricing/quote
   */
  async getPricingQuote(
    request: PricingQuoteRequest
  ): Promise<ApiResponse<PricingQuoteResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const flight = MOCK_FLIGHTS.find((item) => item.flight.id === request.flightId);
        if (!flight) {
          resolve({
            success: false,
            error: {
              code: 'FLIGHT_NOT_FOUND',
              message: 'Flight not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        const multiplier =
          request.classOfTravel === 'business'
            ? 1.8
            : request.classOfTravel === 'first'
              ? 2.5
              : request.classOfTravel === 'premium-economy'
                ? 1.3
                : 1;
        const paxCount = Math.max(request.passengers.length, 1);

        const baseFare = Math.round(flight.pricing.baseFare * multiplier * paxCount);
        const taxes = Math.round(flight.pricing.taxes * paxCount);
        const fees = Math.round(flight.pricing.fees * paxCount);
        const discount = flight.pricing.discount || 0;
        const totalPrice = baseFare + taxes + fees - discount;

        resolve({
          success: true,
          data: {
            quoteId: `quote-${request.flightId}-${Date.now()}`,
            pricing: {
              ...flight.pricing,
              baseFare,
              taxes,
              fees,
              discount,
              totalPrice,
            },
            validUntil: new Date(Date.now() + 10 * 60 * 1000),
          },
          timestamp: new Date(),
        });
      }, 350);
    });
  }

  /**
   * Hold inventory for a flight
   */
  async holdInventory(
    requestOrFlightId: InventoryHoldRequest | string,
    seatCount?: number
  ): Promise<ApiResponse<InventoryHoldResponse>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const request: InventoryHoldRequest =
          typeof requestOrFlightId === 'string'
            ? {
                flightId: requestOrFlightId,
                seatCount: seatCount ?? 1,
              }
            : requestOrFlightId;

        const holdId = `hold-${request.flightId}-${request.seatCount}-${Date.now()}`;
        const hold: InventoryHoldResponse = {
          holdId,
          flightId: request.flightId,
          seatCount: request.seatCount,
          expiresAt: new Date(Date.now() + 15 * 60 * 1000),
        };
        this.holds.set(holdId, hold);

        resolve({
          success: true,
          data: hold,
          timestamp: new Date(),
        });
      }, 400);
    });
  }

  /**
   * Release inventory hold
   */
  async releaseInventory(
    requestOrHoldId: InventoryReleaseRequest | string
  ): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const holdId =
          typeof requestOrHoldId === 'string'
            ? requestOrHoldId
            : requestOrHoldId.holdId;
        const released = this.holds.delete(holdId);
        resolve({
          success: true,
          data: released,
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * Commit inventory (final booking)
   */
  async commitInventory(
    requestOrHoldId: InventoryCommitRequest | string
  ): Promise<ApiResponse<boolean>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const holdId =
          typeof requestOrHoldId === 'string'
            ? requestOrHoldId
            : requestOrHoldId.holdId;
        const committed = this.holds.has(holdId);
        if (committed) {
          this.holds.delete(holdId);
        }
        resolve({
          success: true,
          data: committed,
          timestamp: new Date(),
        });
      }, 500);
    });
  }
}

export const flightService = new FlightService();
