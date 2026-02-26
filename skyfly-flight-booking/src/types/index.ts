/**
 * SkyFly Flight Booking Platform
 * Type definitions for the flight booking application
 */

// ============ Auth & User Types ============
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  avatar?: string;
  createdAt: Date;
  preferences?: UserPreferences;
}

export interface UserPreferences {
  seatPreference: 'window' | 'middle' | 'aisle';
  mealPreference: string[];
  notifications: boolean;
  currency: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: User;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserSummary {
  userId: string;
  email: string;
  realm: string;
  status: string;
  roles: string[];
}

export interface AuthSessionResponse {
  tokens: TokenPair;
  user: UserSummary;
  isNewUser: boolean;
  profileStatus: string;
  mfaLevel: string;
}

export interface GoogleOAuthStartResponse {
  authorizationUrl: string;
  state: string;
  codeChallengeMethod: string;
}

export interface GoogleOAuthCallbackRequest {
  code: string;
  state: string;
  device?: string;
  ip?: string;
}

export interface DeviceInfo {
  userAgent?: string;
  ip?: string;
  deviceId?: string;
  platform?: string;
}

export interface CorpLoginInitRequest {
  email: string;
  deviceInfo: DeviceInfo;
}

export interface CorpLoginInitResponse {
  loginFlowId: string;
  allowedFactors: string[];
  requiresStepUp: boolean;
}

export interface CorpLoginVerifyRequest {
  loginFlowId: string;
  factorType: string;
  assertion: string;
}

export interface CorpLoginVerifyResponse {
  session?: AuthSessionResponse;
  challengeRequired: boolean;
  challengeType?: string;
  challengeMetadata?: Record<string, unknown>;
}

export interface CorpMfaChallengeRequest {
  loginFlowId: string;
  factorType: string;
}

export interface CorpMfaChallengeResponse {
  factorType: string;
  challengeId?: string;
  challenge?: string;
  rpId?: string;
  timeout?: number;
  expiresIn?: number;
  resendAfter?: number;
}

export interface CorpMfaVerifyRequest {
  challengeId: string;
  otpOrAssertion: string;
}

// ============ Flight Types ============
export interface Airport {
  code: string;
  name: string;
  city: string;
  country: string;
  timezone: string;
}

export interface AirlineInfo {
  code: string;
  name: string;
  logo: string;
}

export interface Aircraft {
  type: string;
  manufacturer: string;
  model: string;
}

export interface Segment {
  id: string;
  flightId: string;
  airline: AirlineInfo;
  aircraft: Aircraft;
  departureAirport: Airport;
  arrivalAirport: Airport;
  departureTime: Date;
  arrivalTime: Date;
  duration: number; // minutes
  stops: number;
  stopAirports?: Airport[];
}

export interface Flight {
  id: string;
  segments: Segment[];
  totalDuration: number; // minutes
  totalStops: number;
  arrivalDate?: Date;
  departureDate?: Date;
}

export interface FlightSearchRequest {
  fromCode: string;
  toCode: string;
  departureDate: string | Date;
  returnDate?: string | Date;
  passengers: {
    adults: number;
    children: number;
    infants: number;
  };
  tripType: 'one-way' | 'round-trip';
  classOfTravel: 'economy' | 'premium-economy' | 'business' | 'first';
}

export interface FlightSearchResponse {
  flights: FlightWithPrice[];
  filters: SearchFilters;
  totalResults: number;
}

export interface FlightWithPrice {
  flight: Flight;
  pricing: PricingInfo;
  availability: Availability;
}

export interface Availability {
  seats: number;
  lastSeatId?: string;
}

// ============ Pricing Types ============
export interface PricingInfo {
  baseFare: number;
  taxes: number;
  fees: number;
  discount?: number;
  totalPrice: number;
  currency: string;
  fareBasis: string;
  fareFamily: 'economy' | 'light' | 'standard' | 'flex' | 'business';
}

export interface PricingQuoteRequest {
  flightId: string;
  passengers: PassengerInfo[];
  classOfTravel: string;
}

export interface PricingQuoteResponse {
  quoteId: string;
  pricing: PricingInfo;
  validUntil: Date;
}

export interface InventoryHoldRequest {
  flightId: string;
  seatCount: number;
}

export interface InventoryHoldResponse {
  holdId: string;
  flightId: string;
  seatCount: number;
  expiresAt: Date;
}

export interface InventoryCommitRequest {
  holdId: string;
}

export interface InventoryReleaseRequest {
  holdId: string;
}

// ============ Booking Types ============
export interface PassengerInfo {
  id?: string;
  title: 'Mr' | 'Ms' | 'Mrs' | 'Dr';
  firstName: string;
  lastName: string;
  dateOfBirth: Date;
  nationality: string;
  passportNumber?: string;
  passportExpiry?: Date;
  email: string;
  phone: string;
  type: 'adult' | 'child' | 'infant';
}

export interface Booking {
  id: string;
  bookingReference: string;
  userId: string;
  flights: FlightWithPrice[];
  passengers: PassengerInfo[];
  pricing: PricingInfo;
  status: 'pending' | 'confirmed' | 'cancelled' | 'completed';
  paymentStatus: 'pending' | 'authorized' | 'captured' | 'failed' | 'refunded';
  bookingDate: Date;
  createdAt: Date;
}

export interface BookingRequest {
  flightIds: string[];
  passengers: PassengerInfo[];
  contactEmail: string;
  contactPhone: string;
  specialRequests?: string;
}

export interface BookingConfirmationRequest {
  bookingId: string;
  paymentId: string;
}

export interface BookingChangeRequest {
  bookingId: string;
  flightIds: string[];
  reason?: string;
}

// ============ Payment Types ============
export interface PaymentMethod {
  type: 'credit-card' | 'debit-card' | 'upi' | 'net-banking' | 'wallet';
}

export interface CreditCard extends PaymentMethod {
  type: 'credit-card' | 'debit-card';
  cardNumber: string;
  cardHolder: string;
  expiryMonth: number;
  expiryYear: number;
  cvv: string;
  issuer?: string;
}

export interface PaymentIntent {
  id: string;
  amount: number;
  currency: string;
  status: 'pending' | 'authorized' | 'captured' | 'failed' | 'refunded';
  bookingId: string;
  createdAt: Date;
  expiresAt: Date;
}

export interface PaymentIntentRequest {
  bookingId: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
}

export interface PaymentAuthorizeRequest {
  paymentId: string;
  otp?: string;
}

export interface PaymentCaptureRequest {
  paymentId: string;
}

export interface PaymentWebhookPayload {
  provider: string;
  eventType: string;
  paymentId: string;
  status: PaymentIntent['status'];
  rawPayload: Record<string, unknown>;
}

// ============ Search & Filter Types ============
export interface SearchFilters {
  priceRange: {
    min: number;
    max: number;
  };
  airlines: AirlineInfo[];
  stops: number[];
  departureTimeRanges: {
    label: string;
    start: number; // hours
    end: number; // hours
  }[];
  duration: {
    min: number;
    max: number;
  };
}

export interface AppliedFilters {
  maxPrice?: number;
  selectedAirlines?: string[];
  maxStops?: number;
  departureTimeRange?: {
    start: number;
    end: number;
  };
  sortBy?: 'price' | 'duration' | 'departure' | 'arrival';
  sortOrder?: 'asc' | 'desc';
}

// ============ Notification Types ============
export interface Notification {
  from: string;
  subject: string;
  template?: string;
}

export interface EmailNotification extends Notification {
  from: 'email';
  to: string;
  html: string;
}

export interface SMSNotification extends Notification {
  from: 'sms';
  to: string;
  message: string;
}

export interface PushNotification extends Notification {
  from: 'push';
  userId: string;
  title: string;
  body: string;
  data?: Record<string, string>;
}

// ============ API Response Types ============
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: Record<string, unknown>;
  };
  timestamp: Date;
}

export interface PaginatedResponse<T> {
  success: boolean;
  data: T[];
  pagination: {
    page: number;
    pageSize: number;
    totalResults: number;
    totalPages: number;
  };
  timestamp: Date;
}

// ============ UI State Types ============
export interface SearchState {
  fromCode: string;
  toCode: string;
  departureDate: Date;
  returnDate?: Date;
  passengers: {
    adults: number;
    children: number;
    infants: number;
  };
  tripType: 'one-way' | 'round-trip';
  classOfTravel: 'economy' | 'premium-economy' | 'business' | 'first';
  isLoading: boolean;
  error?: string;
}

export interface CartState {
  outboundFlight?: FlightWithPrice;
  returnFlight?: FlightWithPrice;
  passengers: PassengerInfo[];
  totalPrice: number;
}
