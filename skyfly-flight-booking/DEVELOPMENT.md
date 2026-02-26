# 🛫 SkyFly Flight Booking Platform

A professional, feature-rich flight booking UI built with **React 18**, **TypeScript**, and **Vite**. This is a complete business-grade application following clean architecture principles with responsive design and mock data integration.

## 🌟 Features

### ✈️ Flight Search
- **Smart Search Form**: From/To airports, dates, passengers, cabin class
- **Trip Types**: One-way and round-trip flights
- **Passenger Management**: Adults, children, infants with counters
- **Airport Swap**: Quick swap between departure and arrival airports
- **Date Validation**: Prevents invalid date selections

### 🔍 Search Results & Filtering
- **Real-time Filtering**:
  - Price range slider (₹1,000 - ₹20,000+)
  - Airline selection
  - Number of stops
  - Departure time ranges
  
- **Sorting Options**:
  - Lowest Price
  - Shortest Duration
  - Earliest Departure
  - Earliest Arrival

### 🎫 Flight Display
- **Flight Cards** with:
  - Airline logo and details
  - Departure/Arrival times
  - Total duration
  - Number of stops
  - Seat availability
  - Pricing per person
  - Discount badges

### 📱 Responsive Design
- Mobile-first approach
- Tablet-optimized layout
- Desktop-optimized experience
- Touch-friendly controls
- Adaptive grids and layouts

### 🎨 Professional UI
- **Color Scheme**: Modern blue (#0066cc) with complementary colors
- **Typography**: Clear hierarchy and readability
- **Animations**: Smooth transitions and hover effects
- **Components**: Reusable, well-organized, and documented
- **Themes**: CSS variable-based theming

## 🏗️ Architecture

### Clean Code Principles
✅ **Separation of Concerns** - Components, Services, Hooks, Types
✅ **Single Responsibility** - Each module does one thing well
✅ **DRY (Don't Repeat Yourself)** - Reusable components and logic
✅ **Testability** - Pure components with clear interfaces
✅ **Scalability** - Easy to add features and services

### Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── common/         # Button, Modal, Card, Badge, Loading
│   ├── flight/         # FlightCard, FlightSearch, FlightFilters
│   └── layout/         # Header, Footer
├── hooks/              # Custom React hooks
│   ├── useFlightSearch # Flight search logic
│   ├── useAuth        # Authentication
│   ├── useBooking     # Booking management
│   └── useUtility     # useDebounce, usePagination
├── services/           # API service layer (mocked)
│   ├── flightService
│   ├── authService
│   ├── bookingService
│   ├── paymentService
│   └── userService
├── types/              # TypeScript interfaces
├── constants/          # Mock data & constants
│   ├── airports
│   ├── airlines
│   ├── flights
│   └── users
└── pages/              # Page components
    ├── Home           # Landing page
    └── SearchResults  # Search results page
```

## 📊 Mock Data

The application includes comprehensive mock data:

- **15+ Airports**: Delhi, Mumbai, Bangalore, Hyderabad, Goa, Chennai, etc.
- **10 Airlines**: IndiGo, Air India, SpiceJet, Vistara, Air India Express, etc.
- **500+ Flights**: Dynamically generated with realistic:
  - Pricing (₹1,800 - ₹5,500+)
  - Duration (90-150 minutes)
  - Stop information
  - Availability data

### Easy API Integration

Simply update the service layer to replace mock data with real API calls. No component changes needed!

```typescript
// Before: Mocked
async searchFlights(criteria) {
  return { success: true, data: MOCK_FLIGHTS };
}

// After: Real API
async searchFlights(criteria) {
  const response = await fetch('/api/v1/flights/search', {
    method: 'POST',
    body: JSON.stringify(criteria)
  });
  return response.json();
}
```

## 🚀 Getting Started

### Prerequisites
- Node.js 16+ 
- npm or yarn

### Installation

```bash
# Clone the repository
cd skyfly-flight-booking

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Development Server
The app will be available at `http://localhost:5173`

## 🎯 Key Technologies

- **React 18**: Component-based UI library
- **TypeScript**: Type-safe JavaScript
- **Vite**: Fast build tool and dev server
- **CSS3**: Modern styling with variables and flexbox
- **ES6+**: Modern JavaScript features

## 📖 API Plan (Based on README)

### Flight Service
- `GET /api/v1/flights/search` - Search flights
- `GET /api/v1/flights/{flightId}` - Get flight details
- `GET /api/v1/flights/{flightId}/availability` - Check availability

### Booking Service
- `POST /api/v1/bookings/reserve` - Create booking
- `POST /api/v1/bookings/{bookingId}/confirm` - Confirm booking
- `GET /api/v1/bookings/{bookingId}` - Get booking details
- `GET /api/v1/bookings` - List user bookings
- `POST /api/v1/bookings/{bookingId}/cancel` - Cancel booking

### Payment Service
- `POST /api/v1/payments/intent` - Create payment intent
- `POST /api/v1/payments/{paymentId}/authorize` - Authorize payment
- `POST /api/v1/payments/{paymentId}/capture` - Capture payment

### Auth Service
- `GET /api/v1/auth/public/google/start` - Google OAuth login
- `POST /api/v1/auth/token/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Logout

### Pricing Service
- `POST /api/v1/pricing/quote` - Get price quote

## 🎨 Design Inspiration

- **ixigo.com**: Layout, filters, flight card design
- **goindigo.in**: Color scheme, typography, professional look

## 📱 Responsive Breakpoints

- **Desktop**: 1024px and above
- **Tablet**: 768px to 1023px  
- **Mobile**: Below 768px

## 🔐 Security Features (Ready for Implementation)

- JWT token management
- OAuth2/OIDC integration
- Payment gateway integration
- Rate limiting
- Input validation
- XSS protection

## 🧪 Testing (Ready for Implementation)

```bash
# Unit tests
npm run test

# Component tests
npm run test:components

# E2E tests
npm run test:e2e
```

## 📦 Deployment

### Build for Production
```bash
npm run build
```

The `dist` folder contains the production build ready for deployment.

### Deploy Options
- Vercel (recommended for best performance)
- Netlify
- GitHub Pages
- Cloud platforms (AWS, Google Cloud, Azure)

## 🔄 API Response Format

All services maintain consistent response format:

```typescript
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: Record<string, unknown>;
  };
  timestamp: Date;
}
```

## 🛠️ Customization

### Change Theme Colors

Edit `src/App.css`:
```css
:root {
  --primary-color: #0066cc;
  --secondary-color: #667eea;
  --success-color: #10b981;
  /* ... other colors ... */
}
```

### Add New Components

1. Create component in `src/components/`
2. Create associated CSS file
3. Export from component index
4. Use in pages

### Add New Pages

1. Create page in `src/pages/`
2. Add to App.tsx routing logic
3. Update navigation in Header

## 🚀 Future Enhancements

- [ ] Complete booking flow with passenger form
- [ ] Seat selection interface
- [ ] Real payment gateway integration
- [ ] User authentication & profiles
- [ ] Booking history & management
- [ ] Wishlist/saved flights
- [ ] Price alerts
- [ ] Hotel bundling
- [ ] Multi-city flights
- [ ] PWA capabilities
- [ ] Unit and E2E tests
- [ ] Analytics integration

## 📚 Code Quality

- **100% TypeScript**: Full type safety
- **ESLint Ready**: Configure as needed
- **Responsive**: Mobile-first design
- **Accessible**: WCAG compliant components
- **Performance**: Lazy loading and code splitting ready

## 📄 LICENSE

This project is created for demonstration and educational purposes.

## 👨‍💻 Developer Notes

### Component Conventions
- Functional components with hooks
- Props interface for each component
- Descriptive component names
- One root element per component

### Styling Conventions
- CSS files alongside components
- BEM-like class naming
- CSS variables for colors/spacing
- Mobile-first media queries

### Service Layer
- Consistent method signatures
- Promise-based async operations
- Standard error handling
- Response wrapping

## 🤝 Contributing

This is a template project. Feel free to:
- Customize components
- Add new features
- Integrate real APIs
- Deploy and use in production

## 📞 Support

For questions or issues, refer to:
- Component documentation in code
- TypeScript interfaces for data structures
- Service layer documentation
- Mock data in constants folder

---

**Built with ❤️ for travelers worldwide**

*SkyFly - Making flight booking simple and beautiful.*
