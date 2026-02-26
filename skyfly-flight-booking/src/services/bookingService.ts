/**
 * Booking Service
 * Handles all booking-related API calls (mocked)
 */

import type {
  Booking,
  BookingRequest,
  BookingConfirmationRequest,
  BookingChangeRequest,
  ApiResponse,
} from '../types';
import { MOCK_FLIGHTS } from '../constants/flights';

class BookingService {
  private bookings = new Map<string, Booking>();

  constructor() {
    const seededBookings = this.createSeedBookings();
    seededBookings.forEach((booking) => this.bookings.set(booking.id, booking));
  }

  private createSeedBookings(): Booking[] {
    const first = MOCK_FLIGHTS[0];
    const second = MOCK_FLIGHTS[1];
    const third = MOCK_FLIGHTS[2];
    const now = new Date();

    return [
      {
        id: 'bk-001',
        bookingReference: 'SK672891',
        userId: 'user-001',
        flights: first ? [first] : [],
        passengers: [],
        pricing: first?.pricing || {
          baseFare: 5000,
          taxes: 500,
          fees: 100,
          totalPrice: 5600,
          currency: 'INR',
          fareBasis: 'Y0STANDARD',
          fareFamily: 'economy',
        },
        status: 'completed',
        paymentStatus: 'captured',
        bookingDate: new Date('2025-12-20'),
        createdAt: new Date('2025-12-20'),
      },
      {
        id: 'bk-002',
        bookingReference: 'SK672892',
        userId: 'user-001',
        flights: [second, third].filter(Boolean) as Booking['flights'],
        passengers: [],
        pricing: second?.pricing || {
          baseFare: 3500,
          taxes: 350,
          fees: 75,
          totalPrice: 3925,
          currency: 'INR',
          fareBasis: 'Y0LIGHT',
          fareFamily: 'economy',
        },
        status: 'confirmed',
        paymentStatus: 'captured',
        bookingDate: new Date('2026-01-10'),
        createdAt: new Date('2026-01-10'),
      },
      {
        id: 'bk-003',
        bookingReference: 'SK672893',
        userId: 'user-001',
        flights: first ? [first] : [],
        passengers: [],
        pricing: first?.pricing || {
          baseFare: 4200,
          taxes: 480,
          fees: 90,
          totalPrice: 4770,
          currency: 'INR',
          fareBasis: 'Y0STANDARD',
          fareFamily: 'economy',
        },
        status: 'pending',
        paymentStatus: 'pending',
        bookingDate: now,
        createdAt: now,
      },
    ];
  }

  private findFlightsByIds(flightIds: string[]) {
    return MOCK_FLIGHTS.filter((item) => flightIds.includes(item.flight.id));
  }

  private mergePricing(flights: Booking['flights']) {
    if (!flights.length) {
      return {
        baseFare: 0,
        taxes: 0,
        fees: 0,
        discount: 0,
        totalPrice: 0,
        currency: 'INR' as const,
        fareBasis: 'Y0STANDARD',
        fareFamily: 'economy' as const,
      };
    }

    const aggregated = flights.reduce(
      (acc, current) => {
        acc.baseFare += current.pricing.baseFare;
        acc.taxes += current.pricing.taxes;
        acc.fees += current.pricing.fees;
        acc.discount += current.pricing.discount || 0;
        return acc;
      },
      { baseFare: 0, taxes: 0, fees: 0, discount: 0 }
    );

    return {
      baseFare: aggregated.baseFare,
      taxes: aggregated.taxes,
      fees: aggregated.fees,
      discount: aggregated.discount,
      totalPrice:
        aggregated.baseFare + aggregated.taxes + aggregated.fees - aggregated.discount,
      currency: flights[0].pricing.currency,
      fareBasis: flights[0].pricing.fareBasis,
      fareFamily: flights[0].pricing.fareFamily,
    };
  }

  /**
   * Create a booking reservation
   */
  async reserve(
    bookingRequest: BookingRequest
  ): Promise<ApiResponse<Booking>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const bookingId = `bk-${Date.now()}`;
        const flights = this.findFlightsByIds(bookingRequest.flightIds);
        const booking: Booking = {
          id: bookingId,
          bookingReference: this.generatePNR(),
          userId: 'user-001',
          flights,
          passengers: bookingRequest.passengers,
          pricing: this.mergePricing(flights),
          status: 'pending',
          paymentStatus: 'pending',
          bookingDate: new Date(),
          createdAt: new Date(),
        };
        this.bookings.set(booking.id, booking);

        resolve({
          success: true,
          data: booking,
          timestamp: new Date(),
        });
      }, 600);
    });
  }

  /**
   * Confirm a booking
   */
  async confirmBooking(
    request: BookingConfirmationRequest
  ): Promise<ApiResponse<Booking>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const booking = this.bookings.get(request.bookingId);
        if (!booking) {
          resolve({
            success: false,
            error: {
              code: 'BOOKING_NOT_FOUND',
              message: 'Booking not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        const confirmed: Booking = {
          ...booking,
          status: 'confirmed',
          paymentStatus: 'captured',
        };
        this.bookings.set(request.bookingId, confirmed);

        resolve({
          success: true,
          data: confirmed,
          timestamp: new Date(),
        });
      }, 700);
    });
  }

  /**
   * Get booking details
   */
  async getBooking(bookingId: string): Promise<ApiResponse<Booking>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const booking = this.bookings.get(bookingId);
        if (!booking) {
          resolve({
            success: false,
            error: {
              code: 'BOOKING_NOT_FOUND',
              message: 'Booking not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        resolve({
          success: true,
          data: booking,
          timestamp: new Date(),
        });
      }, 300);
    });
  }

  /**
   * Get user's bookings
   */
  async getUserBookings(): Promise<ApiResponse<Booking[]>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const bookings = Array.from(this.bookings.values()).filter(
          (booking) => booking.userId === 'user-001'
        );

        resolve({
          success: true,
          data: bookings,
          timestamp: new Date(),
        });
      }, 400);
    });
  }

  /**
   * Cancel a booking
   */
  async cancelBooking(bookingId: string): Promise<ApiResponse<Booking>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const booking = this.bookings.get(bookingId);
        if (!booking) {
          resolve({
            success: false,
            error: {
              code: 'BOOKING_NOT_FOUND',
              message: 'Booking not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        const cancelled: Booking = {
          ...booking,
          status: 'cancelled',
          paymentStatus: 'refunded',
        };
        this.bookings.set(bookingId, cancelled);

        resolve({
          success: true,
          data: cancelled,
          timestamp: new Date(),
        });
      }, 500);
    });
  }

  /**
   * POST /api/v1/bookings/{bookingId}/change
   */
  async changeBooking(request: BookingChangeRequest): Promise<ApiResponse<Booking>> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const booking = this.bookings.get(request.bookingId);
        if (!booking) {
          resolve({
            success: false,
            error: {
              code: 'BOOKING_NOT_FOUND',
              message: 'Booking not found',
            },
            timestamp: new Date(),
          });
          return;
        }

        const flights = this.findFlightsByIds(request.flightIds);
        const changed: Booking = {
          ...booking,
          flights,
          pricing: this.mergePricing(flights),
          status: 'confirmed',
        };
        this.bookings.set(request.bookingId, changed);

        resolve({
          success: true,
          data: changed,
          timestamp: new Date(),
        });
      }, 550);
    });
  }

  /**
   * Generate a random PNR (Passenger Name Record)
   */
  private generatePNR(): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < 6; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }
}

export const bookingService = new BookingService();
