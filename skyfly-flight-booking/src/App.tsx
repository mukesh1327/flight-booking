/**
 * SkyFly Flight Booking Application
 * Main App Component with Routing
 */

import { useState } from 'react';
import type { FlightSearchRequest } from './types';
import { HomePage, SearchResultsPage, LoginMockPage } from './pages';
import './App.css';

type AppPage = 'home' | 'search-results' | 'login';

interface AppState {
  currentPage: AppPage;
  lastSearchCriteria: FlightSearchRequest | null;
}

function App() {
  const [appState, setAppState] = useState<AppState>({
    currentPage: 'home',
    lastSearchCriteria: null,
  });

  const handleSearch = (criteria: FlightSearchRequest) => {
    setAppState({
      currentPage: 'search-results',
      lastSearchCriteria: criteria,
    });
  };

  const handleOpenLogin = () => {
    setAppState((prev) => ({
      ...prev,
      currentPage: 'login',
    }));
  };

  const handleBackHome = () => {
    setAppState((prev) => ({
      ...prev,
      currentPage: 'home',
    }));
  };

  return (
    <div className="app">
      {appState.currentPage === 'home' && (
        <HomePage onSearch={handleSearch} onLogin={handleOpenLogin} />
      )}
      
      {appState.currentPage === 'search-results' && appState.lastSearchCriteria && (
        <SearchResultsPage 
          initialCriteria={appState.lastSearchCriteria}
          onLogin={handleOpenLogin}
        />
      )}

      {appState.currentPage === 'login' && <LoginMockPage onBackHome={handleBackHome} />}
    </div>
  );
}

export default App;
