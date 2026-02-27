/**
 * Home Page
 * Main landing page with flight search
 */

import React from 'react';
import type { FlightSearchRequest, User } from '../types';
import { FlightSearch } from '../components/flight';
import { Header, Footer } from '../components/layout';
import './Home.css';

interface HomePageProps {
  onSearch: (criteria: FlightSearchRequest) => void;
  isSearching?: boolean;
  onLogin?: () => void;
  onLogout?: () => void;
  user?: User | null;
  onNavigate?: (path: string) => void;
}

export const HomePage: React.FC<HomePageProps> = ({
  onSearch,
  isSearching = false,
  onLogin,
  onLogout,
  user,
  onNavigate,
}) => {
  return (
    <div className="home-page">
      <Header user={user} onNavigate={onNavigate} onLogin={onLogin} onLogout={onLogout} />

      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">
          <h1>Find Your Perfect Flight</h1>
          <p>Search and book flights to destinations worldwide in just a few clicks</p>
        </div>
      </section>

      {/* Search Form */}
      <section className="search-section">
        <div className="container">
          <FlightSearch onSearch={onSearch} isLoading={isSearching} />
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="container">
          <h2>Why Choose SkyFly?</h2>
          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">✈️</div>
              <h3>Best Prices</h3>
              <p>Compare prices from multiple airlines and find the best deals</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🔒</div>
              <h3>Secure Booking</h3>
              <p>Safe and secure payment processing with SSL encryption</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">📞</div>
              <h3>24/7 Support</h3>
              <p>Round-the-clock customer support for all your queries</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🎉</div>
              <h3>Exclusive Deals</h3>
              <p>Access exclusive offers and discounts on flights</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🛡️</div>
              <h3>Protection</h3>
              <p>Booking protection and easy cancellation policy</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">⚡</div>
              <h3>Instant Booking</h3>
              <p>Book your flights instantly with live seat availability</p>
            </div>
          </div>
        </div>
      </section>

      {/* Popular Routes */}
      <section className="popular-routes-section">
        <div className="container">
          <h2>Popular Flight Routes</h2>
          <div className="routes-grid">
            {[
              { from: 'Delhi', to: 'Mumbai', price: '₹2,500' },
              { from: 'Mumbai', to: 'Bangalore', price: '₹3,200' },
              { from: 'Delhi', to: 'Goa', price: '₹4,500' },
              { from: 'Bangalore', to: 'Hyderabad', price: '₹1,999' },
              { from: 'Delhi', to: 'Pune', price: '₹3,800' },
              { from: 'Mumbai', to: 'Dubai', price: '₹8,500' },
            ].map((route, idx) => (
              <div key={idx} className="route-card">
                <div className="route-from">{route.from}</div>
                <div className="route-arrow">→</div>
                <div className="route-to">{route.to}</div>
                <div className="route-price">{route.price}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
};

export default HomePage;
