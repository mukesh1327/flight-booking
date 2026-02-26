/**
 * Mock Users & Auth Data
 * Sample user data for the flight booking platform
 */

import type { User } from '../types';

export const MOCK_USERS: Record<string, User> = {
  user1: {
    id: 'user-001',
    email: 'john.doe@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phone: '+91-9876543210',
    avatar: 'https://i.pravatar.cc/150?img=1',
    createdAt: new Date('2024-01-15'),
    preferences: {
      seatPreference: 'window',
      mealPreference: ['vegetarian'],
      notifications: true,
      currency: 'INR',
    },
  },
  user2: {
    id: 'user-002',
    email: 'sarah.smith@example.com',
    firstName: 'Sarah',
    lastName: 'Smith',
    phone: '+91-9876543211',
    avatar: 'https://i.pravatar.cc/150?img=2',
    createdAt: new Date('2024-02-20'),
    preferences: {
      seatPreference: 'aisle',
      mealPreference: ['non-vegetarian', 'vegan'],
      notifications: true,
      currency: 'INR',
    },
  },
};

export const MOCK_CURRENT_USER = MOCK_USERS.user1;
