/**
 * Mock Flights Data
 * Sample flight search results based on routes and schedules
 */

import type { FlightWithPrice } from '../types';
import { MOCK_AIRPORTS } from './airports';
import { MOCK_AIRLINES } from './airlines';

const generateFlightData = (): FlightWithPrice[] => {
  const baseDate = new Date(2026, 1, 26); // Feb 26, 2026
  const flights: FlightWithPrice[] = [];

  // Sample flight routes with multiple timings
  const routes = [
    { from: 'DEL', to: 'BOM' },
    { from: 'DEL', to: 'BLR' },
    { from: 'BOM', to: 'BLR' },
    { from: 'DEL', to: 'HYD' },
    { from: 'HYD', to: 'BLR' },
    { from: 'PNQ', to: 'BOM' },
  ];

  const flightNumbers = [
    { airline: '6E', prefix: 'I5' },
    { airline: 'AI', prefix: 'AI' },
    { airline: 'SG', prefix: 'SG' },
    { airline: 'UK', prefix: 'UK' },
    { airline: 'IX', prefix: 'IX' },
  ];

  routes.forEach((route, routeIdx) => {
    flightNumbers.forEach((fn, fnIdx) => {
      // Morning flight
      const morningDeparture = new Date(baseDate);
      morningDeparture.setHours(6, 30 + fnIdx * 15, 0);

      const morningArrival = new Date(morningDeparture);
      const durationMinutes = 90 + Math.random() * 60; // 90-150 minutes
      morningArrival.setMinutes(morningArrival.getMinutes() + durationMinutes);

      flights.push({
        flight: {
          id: `FLT-${route.from}-${route.to}-${fnIdx}-${routeIdx}-0`,
          segments: [
            {
              id: `SEG-${route.from}-${route.to}-${fnIdx}-${routeIdx}-0`,
              flightId: `FLT-${route.from}-${route.to}-${fnIdx}-${routeIdx}-0`,
              airline: MOCK_AIRLINES[fn.airline],
              aircraft: {
                type: 'Narrow Body',
                manufacturer: 'Airbus',
                model: 'A320',
              },
              departureAirport: MOCK_AIRPORTS[route.from],
              arrivalAirport: MOCK_AIRPORTS[route.to],
              departureTime: morningDeparture,
              arrivalTime: morningArrival,
              duration: durationMinutes,
              stops: Math.random() > 0.7 ? 1 : 0,
              stopAirports: Math.random() > 0.7 ? [MOCK_AIRPORTS['DEL']] : undefined,
            },
          ],
          totalDuration: durationMinutes,
          totalStops: Math.random() > 0.7 ? 1 : 0,
          departureDate: morningDeparture,
          arrivalDate: morningArrival,
        },
        pricing: {
          baseFare: 2000 + Math.random() * 3000,
          taxes: 200 + Math.random() * 500,
          fees: 50 + Math.random() * 150,
          discount: Math.random() > 0.6 ? Math.random() * 500 : 0,
          totalPrice: 2500 + Math.random() * 3500,
          currency: 'INR',
          fareBasis: 'Y0BASIC',
          fareFamily: 'economy',
        },
        availability: {
          seats: Math.floor(Math.random() * 100) + 1,
        },
      });

      // Afternoon flight
      const afternoonDeparture = new Date(baseDate);
      afternoonDeparture.setHours(12, fnIdx * 15, 0);

      const afternoonArrival = new Date(afternoonDeparture);
      const afternoonDuration = 90 + Math.random() * 60;
      afternoonArrival.setMinutes(afternoonArrival.getMinutes() + afternoonDuration);

      flights.push({
        flight: {
          id: `FLT-${route.from}-${route.to}-${fnIdx}-${routeIdx}-1`,
          segments: [
            {
              id: `SEG-${route.from}-${route.to}-${fnIdx}-${routeIdx}-1`,
              flightId: `FLT-${route.from}-${route.to}-${fnIdx}-${routeIdx}-1`,
              airline: MOCK_AIRLINES[fn.airline],
              aircraft: {
                type: 'Narrow Body',
                manufacturer: 'Airbus',
                model: 'A320',
              },
              departureAirport: MOCK_AIRPORTS[route.from],
              arrivalAirport: MOCK_AIRPORTS[route.to],
              departureTime: afternoonDeparture,
              arrivalTime: afternoonArrival,
              duration: afternoonDuration,
              stops: 0,
            },
          ],
          totalDuration: afternoonDuration,
          totalStops: 0,
          departureDate: afternoonDeparture,
          arrivalDate: afternoonArrival,
        },
        pricing: {
          baseFare: 2200 + Math.random() * 3200,
          taxes: 220 + Math.random() * 600,
          fees: 60 + Math.random() * 200,
          discount: 0,
          totalPrice: 2600 + Math.random() * 4000,
          currency: 'INR',
          fareBasis: 'Y0STANDARD',
          fareFamily: 'economy',
        },
        availability: {
          seats: Math.floor(Math.random() * 80) + 20,
        },
      });

      // Evening flight
      const eveningDeparture = new Date(baseDate);
      eveningDeparture.setHours(18, 30 + fnIdx * 20, 0);

      const eveningArrival = new Date(eveningDeparture);
      const eveningDuration = 90 + Math.random() * 60;
      eveningArrival.setMinutes(eveningArrival.getMinutes() + eveningDuration);

      flights.push({
        flight: {
          id: `FLT-${route.from}-${route.to}-${fnIdx}-${routeIdx}-2`,
          segments: [
            {
              id: `SEG-${route.from}-${route.to}-${fnIdx}-${routeIdx}-2`,
              flightId: `FLT-${route.from}-${route.to}-${fnIdx}-${routeIdx}-2`,
              airline: MOCK_AIRLINES[fn.airline],
              aircraft: {
                type: 'Narrow Body',
                manufacturer: 'Airbus',
                model: 'A320',
              },
              departureAirport: MOCK_AIRPORTS[route.from],
              arrivalAirport: MOCK_AIRPORTS[route.to],
              departureTime: eveningDeparture,
              arrivalTime: eveningArrival,
              duration: eveningDuration,
              stops: 0,
            },
          ],
          totalDuration: eveningDuration,
          totalStops: 0,
          departureDate: eveningDeparture,
          arrivalDate: eveningArrival,
        },
        pricing: {
          baseFare: 1800 + Math.random() * 2800,
          taxes: 180 + Math.random() * 400,
          fees: 40 + Math.random() * 100,
          discount: Math.random() > 0.5 ? Math.random() * 600 : 0,
          totalPrice: 2200 + Math.random() * 3300,
          currency: 'INR',
          fareBasis: 'Y0LIGHT',
          fareFamily: 'economy',
        },
        availability: {
          seats: Math.floor(Math.random() * 60) + 40,
        },
      });
    });
  });

  return flights;
};

export const MOCK_FLIGHTS = generateFlightData();
