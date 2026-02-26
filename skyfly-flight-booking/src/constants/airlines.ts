/**
 * Mock Airlines Data
 * Popular airlines in the Indian and international market
 */

import type { AirlineInfo } from '../types';

export const MOCK_AIRLINES: Record<string, AirlineInfo> = {
  '6E': {
    code: '6E',
    name: 'IndiGo',
    logo: 'https://images.ixigo.com/image/upload/airlines/indigo.png',
  },
  AI: {
    code: 'AI',
    name: 'Air India',
    logo: 'https://images.ixigo.com/image/upload/airlines/air-india.png',
  },
  SG: {
    code: 'SG',
    name: 'SpiceJet',
    logo: 'https://images.ixigo.com/image/upload/airlines/spicejet.png',
  },
  UK: {
    code: 'UK',
    name: 'Vistara',
    logo: 'https://images.ixigo.com/image/upload/airlines/vistara.png',
  },
  IX: {
    code: 'IX',
    name: 'Air India Express',
    logo: 'https://images.ixigo.com/image/upload/airlines/air-india-express.png',
  },
  QP: {
    code: 'QP',
    name: 'Akasa Air',
    logo: 'https://images.ixigo.com/image/upload/airlines/akasa-air.png',
  },
  EK: {
    code: 'EK',
    name: 'Emirates',
    logo: 'https://images.ixigo.com/image/upload/airlines/emirates.png',
  },
  EY: {
    code: 'EY',
    name: 'Etihad Airways',
    logo: 'https://images.ixigo.com/image/upload/airlines/etihad.png',
  },
  BA: {
    code: 'BA',
    name: 'British Airways',
    logo: 'https://images.ixigo.com/image/upload/airlines/british-airways.png',
  },
  SQ: {
    code: 'SQ',
    name: 'Singapore Airlines',
    logo: 'https://images.ixigo.com/image/upload/airlines/singapore-airlines.png',
  },
};

export const TOP_AIRLINES = ['6E', 'AI', 'SG', 'UK', 'IX'];
