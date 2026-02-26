# 🛫 SkyFly Flight Booking Platform

> A professional, production-ready flight booking UI built with React 18, TypeScript, and Vite

## 📋 Quick Overview

This is a **complete business-grade flight booking application** with:

✅ **Professional UI** - Modern, responsive design inspired by ixigo.com and goindigo.in
✅ **Clean Architecture** - Separation of concerns, scalable structure
✅ **Full TypeScript** - 100% type-safe codebase
✅ **Mock Data** - 500+ realistic flights, ready for API integration
✅ **Responsive Design** - Mobile, tablet, and desktop optimized
✅ **Reusable Components** - Well-organized component library
✅ **Custom Hooks** - State management using modern React patterns
✅ **Service Layer** - Abstracted API calls, easy to integrate real APIs

## 🚀 Quick Start

```bash
# Install dependencies
npm install

# Start development server (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## 📸 Features Overview

### ✈️ Flight Search
- Smart form with from/to airports, dates, passengers
- Trip type selection (one-way, round-trip)
- Cabin class selection (economy, business, first, etc.)
- Quick airport swap functionality

### 🔍 Advanced Filtering
- Price range slider
- Airline selection
- Number of stops filter
- Departure time ranges
- Multiple sorting options

### 🎫 Flight Results
- Beautiful flight cards with all details
- Real-time availability indicators
- Discount badges
- Estimated arrival calculation
- Responsive grid layout

### 📱 Mobile First
- Fully responsive on all devices
- Touch-optimized controls
- Mobile-friendly navigation
- Efficient data display on small screens

## 📦 Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── common/         # Button, Modal, Card, Badge, Loading
│   ├── flight/         # Flight-specific components
│   └── layout/         # Header, Footer
├── hooks/              # Custom React hooks
│   ├── useFlightSearch # Flight search state & logic
│   ├── useAuth        # Authentication
│   └── useBooking     # Booking management
├── services/           # API service layer (mocked)
│   ├── flightService  # Flight-related APIs
│   ├── authService    # Authentication
│   └── ...
├── types/              # TypeScript interfaces & types
├── constants/          # Mock data & application constants
├── pages/              # Page components
│   ├── Home           # Landing page
│   └── SearchResults  # Search results page
└── styles/             # Global & component styles
```

## 🏗️ Architecture

### Clean Code Principles
- ✅ Separation of concerns
- ✅ Single responsibility principle
- ✅ DRY (Don't Repeat Yourself)
- ✅ Testable code
- ✅ Scalable structure

### Component Hierarchy
```
App
├── HomePage
│   ├── Header
│   ├── FlightSearch
│   ├── Features
│   └── Footer
└── SearchResultsPage
    ├── Header
    ├── FlightSearch (sticky)
    ├── FlightFilters (sidebar)
    ├── FlightCard (list)
    └── Footer
```

## 🎨 Design System

### Color Palette
- **Primary**: #0066cc (Sky Blue)
- **Secondary**: #667eea (Purple)
- **Success**: #10b981 (Green)
- **Warning**: #f59e0b (Orange)
- **Danger**: #ef4444 (Red)
- **Text**: #1f2937 (Dark Gray)
- **Border**: #e5e7eb (Light Gray)

### Typography
- **Headings**: 700 weight, -1px letter-spacing
- **Body**: 400 weight, 1.6 line-height
- **System Font**: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto

## 🔌 Mock Data

The application includes comprehensive mock data:

### Airports
- 15+ popular Indian airports (DEL, BOM, BLR, HYD, PNQ, GOI, etc.)
- International airports (DXB, LHR, SIN, BKK)
- Each with city, timezone, and airport code

### Airlines
- 10 major airlines (IndiGo, Air India, SpiceJet, Vistara, etc.)
- Brand logos and airline codes
- Easy to add more airlines

### Flights
- 500+ dynamically generated flights
- Multiple routes and time slots
- Realistic pricing (₹1,800 - ₹5,500+)
- Availability data
- Stop information

### Users
- Sample user profiles
- User preferences
- Auth credentials for testing

## 📡 API Integration

### Current Status: ✅ Fully Mocked

All services return mock data with realistic delays. To integrate real APIs:

1. Update `/src/services/` files
2. Replace mock implementations with real HTTP calls
3. Component code stays the same!
4. No breaking changes to types

### Example API Integration

```typescript
// Before (Mocked)
async searchFlights(criteria: FlightSearchRequest) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        success: true,
        data: { flights: MOCK_FLIGHTS, ... }
      });
    }, 800);
  });
}

// After (Real API)
async searchFlights(criteria: FlightSearchRequest) {
  const response = await fetch('/api/v1/flights/search', {
    method: 'POST',
    body: JSON.stringify(criteria),
    headers: { 'Content-Type': 'application/json' }
  });
  const data = await response.json();
  return data;
}
```

## 🛠️ Technology Stack

| Technology | Purpose | Version |
|-----------|---------|---------|
| React | UI library | ^18 |
| TypeScript | Type safety | ^5 |
| Vite | Build tool | ^5 |
| CSS3 | Styling | Native |
| JavaScript | Logic | ES2020+ |

## 🎯 Key Features Implemented

### Search & Browse
- [x] Flight search form with multiple filters
- [x] Date picker with validation
- [x] Passenger counter
- [x] Airport selection
- [x] Trip type selection

### Results & Filtering
- [x] Flight listing with cards
- [x] Real-time filtering
- [x] Multiple sort options
- [x] Price range slider
- [x] Airline filter
- [x] Stops filter
- [x] Time range filter

### UI/UX
- [x] Responsive design
- [x] Loading states
- [x] Error handling
- [x] Empty states
- [x] Success messages
- [x] Modal components
- [x] Badge components
- [x] Button component

### Code Quality
- [x] 100% TypeScript
- [x] Clean architecture
- [x] Reusable components
- [x] Custom hooks
- [x] Service layer abstraction
- [x] Well-documented

## 📚 Documentation

- **[DEVELOPMENT.md](./DEVELOPMENT.md)** - Comprehensive development guide
- **[PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)** - Architecture & structure details
- Component comments and JSDoc
- TypeScript interfaces for data structures

## 🚀 Deployment

### Build
```bash
npm run build
# Creates optimized production build in 'dist' folder
```

### Deploy Options
- **Vercel** (Recommended): `vercel deploy`
- **Netlify**: `netlify deploy --prod --dir=dist`
- **GitHub Pages**: Configure in vite.config.ts
- **Docker**: Create a Dockerfile for containerization
- **Cloud Platforms**: AWS S3, Google Cloud Storage, Azure Blob

## 🧪 Testing (Ready to Implement)

```bash
# Setup testing (when ready)
npm install --save-dev vitest @testing-library/react @testing-library/user-event

# Run tests
npm run test

# Coverage
npm run test:coverage
```

## 🔮 Future Enhancements

### Phase 2: Booking Flow
- [ ] Passenger form component
- [ ] Seat selection interface
- [ ] Baggage options
- [ ] Final confirmation

### Phase 3: Authentication
- [ ] User registration
- [ ] Login/logout
- [ ] Profile management
- [ ] Saved flights

### Phase 4: Payments
- [ ] Payment gateway integration
- [ ] Multiple payment methods
- [ ] Refund tracking
- [ ] Invoice generation

### Phase 5: Advanced Features
- [ ] Price alerts
- [ ] Flexible dates
- [ ] Multi-city flights
- [ ] Hotel bundling
- [ ] Car rentals

## 🙋 Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (iOS Safari, Chrome Android)

## 📄 License

This project is provided as a template for educational and commercial use.

## 👨‍🏫 Learning Resources

This project demonstrates:
- Modern React patterns with hooks
- TypeScript best practices
- Clean code architecture
- Component composition
- State management patterns
- Service layer abstraction
- Responsive web design
- CSS variables and theming

## 💡 Tips & Tricks

### Change Theme
Edit CSS variables in `src/App.css`:
```css
:root {
  --primary-color: #YOUR_COLOR;
  /* ... other colors ... */
}
```

### Add New Route/Page
1. Create page in `src/pages/`
2. Add to App.tsx switch statement
3. Update Header navigation

### Create Custom Hook
1. Create in `src/hooks/`
2. Follow naming convention: `use[Name].ts`
3. Export from `src/hooks/index.ts`

### Add New Service
1. Create in `src/services/` with `[name]Service.ts`
2. Follow ApiResponse pattern
3. Export from `src/services/index.ts`

## 🤝 Contributing

Feel free to fork and customize this project for your needs:
- Add real API integration
- Customize colors and typography
- Add new features
- Submit improvements

## 📞 Support

Refer to the documentation files for detailed information about:
- Component API and usage
- Hook documentation
- Service layer details
- Mock data structure

---

**Built with ❤️ using React, TypeScript & Vite**

*SkyFly - Making flight booking simple and beautiful* ✈️
# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```
