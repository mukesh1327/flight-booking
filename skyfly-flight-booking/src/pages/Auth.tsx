import React, { useState } from 'react';
import { Header, Footer, Button, ErrorMessage } from '../components';
import './Auth.css';

interface AuthPageProps {
  isLoading: boolean;
  error: string | null;
  onLogin: (email: string, password: string) => Promise<boolean>;
  onRegister: (
    email: string,
    password: string,
    firstName: string,
    lastName: string
  ) => Promise<boolean>;
  onBackHome: () => void;
  onSuccess: () => void;
}

export const AuthPage: React.FC<AuthPageProps> = ({
  isLoading,
  error,
  onLogin,
  onRegister,
  onBackHome,
  onSuccess,
}) => {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('demo.user@skyfly.com');
  const [password, setPassword] = useState('dummy-assertion');
  const [firstName, setFirstName] = useState('Demo');
  const [lastName, setLastName] = useState('User');

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (mode === 'login') {
      const success = await onLogin(email, password);
      if (success) {
        onSuccess();
      }
      return;
    }

    const success = await onRegister(email, password, firstName, lastName);
    if (success) {
      onSuccess();
    }
  };

  return (
    <div className="auth-page">
      <Header onNavigate={() => onBackHome()} />

      <main className="auth-main">
        <section className="auth-panel">
          <div className="auth-panel-header">
            <h1>Welcome to SkyFly</h1>
            <p>Sign in to continue your booking, manage trips, and receive alerts.</p>
          </div>

          <div className="auth-mode-tabs">
            <button
              className={mode === 'login' ? 'active' : ''}
              onClick={() => setMode('login')}
              type="button"
            >
              Login
            </button>
            <button
              className={mode === 'register' ? 'active' : ''}
              onClick={() => setMode('register')}
              type="button"
            >
              Register
            </button>
          </div>

          {error && <ErrorMessage message={error} />}

          <form className="auth-form" onSubmit={handleSubmit}>
            {mode === 'register' && (
              <div className="auth-grid-two">
                <div>
                  <label>First Name</label>
                  <input
                    value={firstName}
                    onChange={(event) => setFirstName(event.target.value)}
                    required
                  />
                </div>
                <div>
                  <label>Last Name</label>
                  <input
                    value={lastName}
                    onChange={(event) => setLastName(event.target.value)}
                    required
                  />
                </div>
              </div>
            )}

            <div>
              <label>Email</label>
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </div>

            <div>
              <label>Password</label>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
              <small>
                Use <strong>dummy-assertion</strong> for local corporate login password.
              </small>
            </div>

            <Button type="submit" isLoading={isLoading} size="lg" style={{ width: '100%' }}>
              {mode === 'login' ? 'Sign In' : 'Create Account'}
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={onBackHome}
              size="lg"
              style={{ width: '100%' }}
            >
              Continue As Guest
            </Button>
          </form>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default AuthPage;
