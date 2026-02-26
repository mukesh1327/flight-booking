/**
 * useAuth Hook
 * Manages authentication state
 */

import { useState, useCallback, useEffect } from 'react';
import type { User } from '../types';
import { authService } from '../services';

interface UseAuthReturn {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<boolean>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<boolean>;
  logout: () => Promise<boolean>;
  setUser: (user: User | null) => void;
}

export const useAuth = (): UseAuthReturn => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Check if user is already logged in on mount
  useEffect(() => {
    const checkAuth = async () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          setUser(JSON.parse(storedUser));
        } catch (err) {
          localStorage.removeItem('user');
        }
      }
    };

    checkAuth();
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await authService.login({ email, password });

        if (response.success && response.data) {
          const { user: userData, token } = response.data;
          setUser(userData);
          localStorage.setItem('user', JSON.stringify(userData));
          localStorage.setItem('token', token);
          return true;
        } else {
          setError(response.error?.message || 'Login failed');
          return false;
        }
      } catch (err) {
        setError('An error occurred during login');
        console.error(err);
        return false;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const register = useCallback(
    async (email: string, password: string, firstName: string, lastName: string) => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await authService.register(email, password, firstName, lastName);

        if (response.success && response.data) {
          const { user: userData, token } = response.data;
          setUser(userData);
          localStorage.setItem('user', JSON.stringify(userData));
          localStorage.setItem('token', token);
          return true;
        } else {
          setError(response.error?.message || 'Registration failed');
          return false;
        }
      } catch (err) {
        setError('An error occurred during registration');
        console.error(err);
        return false;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const logout = useCallback(async () => {
    setIsLoading(true);
    try {
      await authService.logout();
      setUser(null);
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('actorType');
      localStorage.removeItem('userId');
      return true;
    } catch (err) {
      setError('An error occurred during logout');
      console.error(err);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    user,
    isAuthenticated: !!user,
    isLoading,
    error,
    login,
    register,
    logout,
    setUser,
  };
};
