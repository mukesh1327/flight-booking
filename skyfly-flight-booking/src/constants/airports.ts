/**
 * Mock Airports Data
 * Common airports used in Indian domestic and international flight network
 */

import type { Airport } from '../types';

export const MOCK_AIRPORTS: Record<string, Airport> = {
  DEL: {
    code: 'DEL',
    name: 'Indira Gandhi International',
    city: 'New Delhi',
    country: 'India',
    timezone: 'IST',
  },
  BOM: {
    code: 'BOM',
    name: 'Bombay High Airport',
    city: 'Mumbai',
    country: 'India',
    timezone: 'IST',
  },
  BLR: {
    code: 'BLR',
    name: 'Kempegowda International',
    city: 'Bangalore',
    country: 'India',
    timezone: 'IST',
  },
  HYD: {
    code: 'HYD',
    name: 'Rajiv Gandhi International',
    city: 'Hyderabad',
    country: 'India',
    timezone: 'IST',
  },
  COK: {
    code: 'COK',
    name: 'Cochin International',
    city: 'Kochi',
    country: 'India',
    timezone: 'IST',
  },
  MAA: {
    code: 'MAA',
    name: 'Chennai International',
    city: 'Chennai',
    country: 'India',
    timezone: 'IST',
  },
  PNQ: {
    code: 'PNQ',
    name: 'Pune Airport',
    city: 'Pune',
    country: 'India',
    timezone: 'IST',
  },
  GOI: {
    code: 'GOI',
    name: 'Dabolim Airport',
    city: 'Goa',
    country: 'India',
    timezone: 'IST',
  },
  JAI: {
    code: 'JAI',
    name: 'Jaipur International',
    city: 'Jaipur',
    country: 'India',
    timezone: 'IST',
  },
  LKO: {
    code: 'LKO',
    name: 'Lucknow Airport',
    city: 'Lucknow',
    country: 'India',
    timezone: 'IST',
  },
  AMI: {
    code: 'AMI',
    name: 'Amedabad International',
    city: 'Ahmedabad',
    country: 'India',
    timezone: 'IST',
  },
  CCU: {
    code: 'CCU',
    name: 'Netaji Subhas Chandra Bose',
    city: 'Kolkata',
    country: 'India',
    timezone: 'IST',
  },
  DXB: {
    code: 'DXB',
    name: 'Dubai International',
    city: 'Dubai',
    country: 'UAE',
    timezone: 'GST',
  },
  LHR: {
    code: 'LHR',
    name: 'London Heathrow',
    city: 'London',
    country: 'United Kingdom',
    timezone: 'GMT',
  },
  SIN: {
    code: 'SIN',
    name: 'Singapore Changi',
    city: 'Singapore',
    country: 'Singapore',
    timezone: 'SGT',
  },
  BKK: {
    code: 'BKK',
    name: 'Bangkok Suvarnabhumi',
    city: 'Bangkok',
    country: 'Thailand',
    timezone: 'ICT',
  },
};

export const MAJOR_CITIES = [
  'New Delhi',
  'Mumbai',
  'Bangalore',
  'Hyderabad',
  'Kochi',
  'Chennai',
  'Pune',
  'Goa',
  'Jaipur',
  'Lucknow',
];
