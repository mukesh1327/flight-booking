import { useMemo, useState } from 'react';
import type { Booking, FlightSearchRequest, FlightWithPrice, PassengerInfo, PaymentMethod, User } from './types';
import { useAuth, useBooking } from './hooks';
import { bookingService, paymentService, userService } from './services';
import {
  HomePage,
  SearchResultsPage,
  AuthPage,
  CheckoutPage,
  BookingConfirmationPage,
  BookingsPage,
  ProfilePage,
} from './pages';
import './App.css';

type AppPage = 'home' | 'search-results' | 'auth' | 'checkout' | 'confirmation' | 'bookings' | 'profile';

interface ConfirmationState {
  booking: Booking;
  paymentId: string;
}

function App() {
  const { user, isAuthenticated, isLoading: authLoading, error: authError, login, register, logout, setUser } = useAuth();
  const {
    userBookings,
    isLoading: bookingsLoading,
    error: bookingsError,
    getUserBookings,
    cancelBooking,
  } = useBooking();

  const [currentPage, setCurrentPage] = useState<AppPage>('home');
  const [lastSearchCriteria, setLastSearchCriteria] = useState<FlightSearchRequest | null>(null);
  const [selectedFlight, setSelectedFlight] = useState<FlightWithPrice | null>(null);
  const [postAuthTarget, setPostAuthTarget] = useState<AppPage>('home');
  const [checkoutSubmitting, setCheckoutSubmitting] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [confirmation, setConfirmation] = useState<ConfirmationState | null>(null);
  const [profileUser, setProfileUser] = useState<User | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);

  const effectiveUser = useMemo(() => profileUser || user, [profileUser, user]);

  const ensureAuthenticated = (target: AppPage) => {
    if (isAuthenticated) {
      return true;
    }

    setPostAuthTarget(target);
    setCurrentPage('auth');
    return false;
  };

  const navigate = (path: string) => {
    if (path === '/') {
      setCurrentPage('home');
      return;
    }

    if (path === '/bookings') {
      if (ensureAuthenticated('bookings')) {
        setCurrentPage('bookings');
      }
      return;
    }

    if (path === '/profile') {
      if (ensureAuthenticated('profile')) {
        setCurrentPage('profile');
      }
      return;
    }

    if (path === '/search-results' && lastSearchCriteria) {
      setCurrentPage('search-results');
      return;
    }

    setCurrentPage('home');
  };

  const handleSearch = (criteria: FlightSearchRequest) => {
    setLastSearchCriteria(criteria);
    setSelectedFlight(null);
    setCurrentPage('search-results');
  };

  const handleSelectFlight = (flight: FlightWithPrice) => {
    setSelectedFlight(flight);

    if (!ensureAuthenticated('checkout')) {
      return;
    }

    setCurrentPage('checkout');
  };

  const handleConfirmBooking = async (payload: {
    passengers: PassengerInfo[];
    contactEmail: string;
    contactPhone: string;
    paymentMethod: PaymentMethod;
  }): Promise<{ booking: Booking; paymentId: string } | null> => {
    if (!selectedFlight) {
      setCheckoutError('No flight selected for checkout.');
      return null;
    }

    setCheckoutSubmitting(true);
    setCheckoutError(null);

    try {
      const reserved = await bookingService.reserve({
        flightIds: [selectedFlight.flight.id],
        passengers: payload.passengers,
        contactEmail: payload.contactEmail,
        contactPhone: payload.contactPhone,
      });

      if (!reserved.success || !reserved.data) {
        setCheckoutError(reserved.error?.message || 'Unable to reserve booking.');
        return null;
      }

      const intent = await paymentService.createPaymentIntent({
        bookingId: reserved.data.id,
        amount: reserved.data.pricing.totalPrice,
        currency: reserved.data.pricing.currency,
        paymentMethod: payload.paymentMethod,
      });

      if (!intent.success || !intent.data) {
        setCheckoutError(intent.error?.message || 'Unable to create payment intent.');
        return null;
      }

      const authorized = await paymentService.authorizePayment({
        paymentId: intent.data.id,
      });

      if (!authorized.success || !authorized.data) {
        setCheckoutError(authorized.error?.message || 'Payment authorization failed.');
        return null;
      }

      const captured = await paymentService.capturePayment({
        paymentId: authorized.data.id,
      });

      if (!captured.success || !captured.data) {
        setCheckoutError(captured.error?.message || 'Payment capture failed.');
        return null;
      }

      const confirmed = await bookingService.confirmBooking({
        bookingId: reserved.data.id,
        paymentId: captured.data.id,
      });

      if (!confirmed.success || !confirmed.data) {
        setCheckoutError(confirmed.error?.message || 'Booking confirmation failed.');
        return null;
      }

      void getUserBookings();
      return {
        booking: confirmed.data,
        paymentId: captured.data.id,
      };
    } catch (error) {
      setCheckoutError('Something went wrong while processing your booking.');
      console.error(error);
      return null;
    } finally {
      setCheckoutSubmitting(false);
    }
  };

  const handleCheckoutSuccess = (booking: Booking, paymentId: string) => {
    setConfirmation({ booking, paymentId });
    setCurrentPage('confirmation');
  };

  const handleLoadProfile = async (): Promise<User | null> => {
    setProfileLoading(true);
    setProfileError(null);
    try {
      const response = await userService.getMe();
      if (!response.success || !response.data) {
        setProfileError(response.error?.message || 'Unable to load profile.');
        return null;
      }

      setProfileUser(response.data);
      return response.data;
    } catch (error) {
      setProfileError('Unable to load profile.');
      console.error(error);
      return null;
    } finally {
      setProfileLoading(false);
    }
  };

  const handleSaveProfile = async (updates: Partial<User>): Promise<User | null> => {
    setProfileLoading(true);
    setProfileError(null);

    try {
      const response = await userService.patchMe(updates);
      if (!response.success || !response.data) {
        setProfileError(response.error?.message || 'Unable to save profile.');
        return null;
      }

      setProfileUser(response.data);
      setUser(response.data);
      localStorage.setItem('user', JSON.stringify(response.data));
      return response.data;
    } catch (error) {
      setProfileError('Unable to save profile.');
      console.error(error);
      return null;
    } finally {
      setProfileLoading(false);
    }
  };

  const handleAuthSuccess = () => {
    setCurrentPage(postAuthTarget);
  };

  const handleCancelBooking = async (bookingId: string) => {
    await cancelBooking(bookingId);
    await getUserBookings();
  };

  const handleLogout = async () => {
    await logout();
    setCurrentPage('home');
    setSelectedFlight(null);
    setConfirmation(null);
  };

  return (
    <div className="app">
      {currentPage === 'home' && (
        <HomePage
          onSearch={handleSearch}
          onLogin={() => {
            setPostAuthTarget('home');
            setCurrentPage('auth');
          }}
          user={effectiveUser}
          onNavigate={navigate}
        />
      )}

      {currentPage === 'search-results' && lastSearchCriteria && (
        <SearchResultsPage
          initialCriteria={lastSearchCriteria}
          onSelectFlight={handleSelectFlight}
          onLogin={() => {
            setPostAuthTarget('search-results');
            setCurrentPage('auth');
          }}
          user={effectiveUser}
          onNavigate={navigate}
        />
      )}

      {currentPage === 'auth' && (
        <AuthPage
          isLoading={authLoading}
          error={authError}
          onLogin={login}
          onRegister={register}
          onBackHome={() => setCurrentPage('home')}
          onSuccess={handleAuthSuccess}
        />
      )}

      {currentPage === 'checkout' && selectedFlight && lastSearchCriteria && (
        <CheckoutPage
          userName={effectiveUser?.firstName}
          criteria={lastSearchCriteria}
          selectedFlight={selectedFlight}
          isSubmitting={checkoutSubmitting}
          error={checkoutError}
          onBackToResults={() => setCurrentPage('search-results')}
          onNavigate={navigate}
          onConfirmBooking={handleConfirmBooking}
          onSuccess={handleCheckoutSuccess}
        />
      )}

      {currentPage === 'confirmation' && confirmation && (
        <BookingConfirmationPage
          booking={confirmation.booking}
          paymentId={confirmation.paymentId}
          onNavigate={navigate}
        />
      )}

      {currentPage === 'bookings' && (
        <BookingsPage
          userName={effectiveUser?.firstName}
          bookings={userBookings}
          isLoading={bookingsLoading}
          error={bookingsError}
          onNavigate={navigate}
          onLoad={getUserBookings}
          onCancel={handleCancelBooking}
        />
      )}

      {currentPage === 'profile' && (
        <ProfilePage
          user={effectiveUser}
          isLoading={profileLoading}
          error={profileError}
          onNavigate={navigate}
          onLoadProfile={handleLoadProfile}
          onSaveProfile={handleSaveProfile}
        />
      )}

      {effectiveUser && (
        <button className="floating-logout" onClick={handleLogout}>
          Logout
        </button>
      )}
    </div>
  );
}

export default App;
