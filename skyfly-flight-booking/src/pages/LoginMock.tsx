import React, { useState } from 'react';
import { Header, Footer } from '../components/layout';
import { Button, ErrorMessage } from '../components/common';
import { authService, flightService, bookingService, paymentService, userService } from '../services';
import type { Booking, FlightWithPrice, PaymentIntent, User } from '../types';
import './LoginMock.css';

interface LoginMockPageProps {
  onBackHome: () => void;
}

type AuthMode = 'public' | 'corp';

export const LoginMockPage: React.FC<LoginMockPageProps> = ({ onBackHome }) => {
  const [mode, setMode] = useState<AuthMode>('public');
  const [email, setEmail] = useState('demo.user@skyfly.com');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [status, setStatus] = useState('No action yet');
  const [loggedIn, setLoggedIn] = useState<string>('');
  const [selectedFlight, setSelectedFlight] = useState<FlightWithPrice | null>(null);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [payment, setPayment] = useState<PaymentIntent | null>(null);
  const [profile, setProfile] = useState<User | null>(null);

  const runPublicLogin = async () => {
    setError(null);
    setIsLoading(true);
    try {
      const start = await authService.startGoogleOAuth();
      if (!start.success || !start.data) {
        setError(start.error?.message || 'Login failed');
        return;
      }
      const callback = await authService.handleGoogleCallback({
        code: 'demo-code',
        state: start.data.state,
      });
      if (!callback.success || !callback.data) {
        setError(callback.error?.message || 'Login failed');
        return;
      }
      setLoggedIn(callback.data.user.email);
      setStatus('Customer login success');
    } finally {
      setIsLoading(false);
    }
  };

  const runCorpLogin = async () => {
    setError(null);
    setIsLoading(true);
    try {
      const init = await authService.initCorpLogin({
        email,
        deviceInfo: {
          userAgent: navigator.userAgent,
          ip: '127.0.0.1',
          deviceId: 'demo-device',
          platform: navigator.platform,
        },
      });
      if (!init.success || !init.data) {
        setError(init.error?.message || 'Corp login init failed');
        return;
      }

      const verify = await authService.verifyCorpLogin({
        loginFlowId: init.data.loginFlowId,
        factorType: 'PASSWORD',
        assertion: 'dummy-assertion',
      });
      if (!verify.success || !verify.data) {
        setError(verify.error?.message || 'Corp login verify failed');
        return;
      }

      if (verify.data.session) {
        setLoggedIn(verify.data.session.user.email);
        setStatus('Corporate login success');
        return;
      }

      const challenge = await authService.createCorpMfaChallenge({
        loginFlowId: init.data.loginFlowId,
        factorType: 'OTP',
      });
      if (!challenge.success || !challenge.data?.challengeId) {
        setError(challenge.error?.message || 'MFA challenge failed');
        return;
      }

      const mfa = await authService.verifyCorpMfa({
        challengeId: challenge.data.challengeId,
        otpOrAssertion: '123456',
      });
      if (!mfa.success || !mfa.data) {
        setError(mfa.error?.message || 'MFA verify failed');
        return;
      }
      setLoggedIn(mfa.data.user.email);
      setStatus('Corporate login success with MFA');
    } finally {
      setIsLoading(false);
    }
  };

  const runFlightSearch = async () => {
    setError(null);
    setIsLoading(true);
    try {
      const response = await flightService.searchFlights({
        fromCode: 'DEL',
        toCode: 'BOM',
        departureDate: new Date(),
        passengers: { adults: 1, children: 0, infants: 0 },
        tripType: 'one-way',
        classOfTravel: 'economy',
      });
      if (!response.success || !response.data) {
        setError(response.error?.message || 'Flight search failed');
        return;
      }
      const first = response.data.flights[0] || null;
      setSelectedFlight(first);
      setStatus(first ? `Found ${response.data.totalResults} flights` : 'No flights found');
    } finally {
      setIsLoading(false);
    }
  };

  const runBooking = async () => {
    setError(null);
    setIsLoading(true);
    try {
      let flight = selectedFlight;
      if (!flight) {
        const search = await flightService.searchFlights({
          fromCode: 'DEL',
          toCode: 'BOM',
          departureDate: new Date(),
          passengers: { adults: 1, children: 0, infants: 0 },
          tripType: 'one-way',
          classOfTravel: 'economy',
        });
        flight = search.data?.flights[0] || null;
        setSelectedFlight(flight);
      }
      if (!flight) {
        setError('No flight available for booking');
        return;
      }

      const reserve = await bookingService.reserve({
        flightIds: [flight.flight.id],
        passengers: [
          {
            title: 'Mr',
            firstName: 'Demo',
            lastName: 'User',
            dateOfBirth: new Date('1992-01-01'),
            nationality: 'IN',
            email,
            phone: '+91-9000000000',
            type: 'adult',
          },
        ],
        contactEmail: email,
        contactPhone: '+91-9000000000',
      });
      if (!reserve.success || !reserve.data) {
        setError(reserve.error?.message || 'Booking failed');
        return;
      }
      setBooking(reserve.data);
      setStatus(`Booking created: ${reserve.data.bookingReference}`);
    } finally {
      setIsLoading(false);
    }
  };

  const runPayment = async () => {
    setError(null);
    setIsLoading(true);
    try {
      if (!booking) {
        setError('Create booking first');
        return;
      }
      const intent = await paymentService.createPaymentIntent({
        bookingId: booking.id,
        amount: booking.pricing.totalPrice,
        currency: booking.pricing.currency,
        paymentMethod: { type: 'credit-card' },
      });
      if (!intent.success || !intent.data) {
        setError(intent.error?.message || 'Payment intent failed');
        return;
      }

      const authorized = await paymentService.authorizePayment({ paymentId: intent.data.id });
      if (!authorized.success || !authorized.data) {
        setError(authorized.error?.message || 'Payment authorize failed');
        return;
      }

      const captured = await paymentService.capturePayment({ paymentId: authorized.data.id });
      if (!captured.success || !captured.data) {
        setError(captured.error?.message || 'Payment capture failed');
        return;
      }
      setPayment(captured.data);
      setStatus(`Payment captured: ${captured.data.id}`);
    } finally {
      setIsLoading(false);
    }
  };

  const runProfile = async () => {
    setError(null);
    setIsLoading(true);
    try {
      const me = await userService.getMe();
      if (!me.success || !me.data) {
        setError(me.error?.message || 'Profile load failed');
        return;
      }
      setProfile(me.data);
      setStatus('Profile loaded');
    } finally {
      setIsLoading(false);
    }
  };

  const runAll = async () => {
    if (mode === 'public') {
      await runPublicLogin();
    } else {
      await runCorpLogin();
    }
    await runFlightSearch();
    await runBooking();
    await runPayment();
    await runProfile();
  };

  return (
    <div className="login-mock-page">
      <Header onNavigate={(path: string) => (path === '/' ? onBackHome() : undefined)} />
      <main className="login-mock-main">
        <section className="login-mock-panel">
          <div className="login-mock-title">
            <h1>Service Integration Testing</h1>
            <p>Run login and booking flows against live local microservices.</p>
          </div>

          <div className="login-mode-switch">
            <button
              className={mode === 'public' ? 'active' : ''}
              onClick={() => setMode('public')}
            >
              Customer Login
            </button>
            <button className={mode === 'corp' ? 'active' : ''} onClick={() => setMode('corp')}>
              Staff Login
            </button>
          </div>

          {error && <ErrorMessage message={error} onDismiss={() => setError(null)} />}

          <div className="auth-block">
            <h2>1) Login</h2>
            <div className="form-row">
              <label>Email</label>
              <input value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="action-row">
              {mode === 'public' ? (
                <Button onClick={runPublicLogin} isLoading={isLoading}>
                  Login as Customer
                </Button>
              ) : (
                <Button onClick={runCorpLogin} isLoading={isLoading}>
                  Login as Staff (with MFA)
                </Button>
              )}
            </div>
            <p className="simple-note">Staff demo MFA code is auto-handled using 123456.</p>
          </div>

          <div className="auth-block">
            <h2>2) Other Services</h2>
            <div className="action-row">
              <Button onClick={runFlightSearch} variant="secondary" isLoading={isLoading}>
                Search Flights
              </Button>
              <Button onClick={runBooking} variant="secondary" isLoading={isLoading}>
                Create Booking
              </Button>
              <Button onClick={runPayment} variant="secondary" isLoading={isLoading}>
                Complete Payment
              </Button>
              <Button onClick={runProfile} variant="secondary" isLoading={isLoading}>
                Load Profile
              </Button>
            </div>
            <div className="action-row">
              <Button onClick={runAll} isLoading={isLoading}>
                Run Full Demo
              </Button>
            </div>
          </div>
        </section>

        <aside className="contract-preview">
          <h3>Demo Status</h3>
          <p><strong>Last status:</strong> {status}</p>
          <p><strong>Logged in:</strong> {loggedIn || 'Not logged in'}</p>
          <p>
            <strong>Flight:</strong>{' '}
            {selectedFlight
              ? `${selectedFlight.flight.segments[0].departureAirport.code} to ${selectedFlight.flight.segments[0].arrivalAirport.code}`
              : 'Not selected'}
          </p>
          <p><strong>Booking:</strong> {booking?.bookingReference || 'Not created'}</p>
          <p><strong>Payment:</strong> {payment?.status || 'Not completed'}</p>
          <p><strong>Profile:</strong> {profile ? `${profile.firstName} ${profile.lastName}` : 'Not loaded'}</p>
        </aside>
      </main>
      <Footer />
    </div>
  );
};

export default LoginMockPage;
