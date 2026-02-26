/**
 * Header Component
 * Navigation header with logo and user menu
 */

import React from 'react';
import type { User } from '../../types';
import { Button } from '../common';
import './Header.css';

interface HeaderProps {
  user?: User | null;
  onLogin?: () => void;
  onLogout?: () => void;
  onNavigate?: (path: string) => void;
}

export const Header: React.FC<HeaderProps> = ({
  user,
  onLogin,
  onLogout,
  onNavigate,
}) => {
  const [mobileMenuOpen, setMobileMenuOpen] = React.useState(false);

  return (
    <header className="header">
      <div className="header-container">
        {/* Logo */}
        <div className="header-logo" onClick={() => onNavigate?.('/')}>
          <span className="logo-icon">✈️</span>
          <span className="logo-text">SkyFly</span>
        </div>

        {/* Navigation */}
        <nav className={`header-nav ${mobileMenuOpen ? 'mobile-open' : ''}`}>
          <a href="#" onClick={(e) => { e.preventDefault(); onNavigate?.('/'); }}>
            Flights
          </a>
          <a href="#" onClick={(e) => { e.preventDefault(); onNavigate?.('/bookings'); }}>
            My Bookings
          </a>
          <a href="#" onClick={(e) => { e.preventDefault(); onNavigate?.('/about'); }}>
            About
          </a>
        </nav>

        {/* User Section */}
        <div className="header-user">
          {user ? (
            <div className="user-profile">
              <img src={user.avatar} alt={user.firstName} className="user-avatar" />
              <span className="user-name">{user.firstName}</span>
              <button
                className="logout-btn"
                onClick={onLogout}
                title="Logout"
              >
                →
              </button>
            </div>
          ) : (
            <Button variant="primary" size="sm" onClick={onLogin}>
              Sign in
            </Button>
          )}
        </div>

        {/* Mobile Menu Toggle */}
        <button
          className="mobile-menu-btn"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        >
          ☰
        </button>
      </div>
    </header>
  );
};

export default Header;
