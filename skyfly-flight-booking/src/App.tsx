import { LoginCard } from './components/auth/LoginCard'
import { RegisterCard } from './components/auth/RegisterCard'
import { BookingCard } from './components/booking/BookingCard'
import { useAuth } from './hooks/useAuth'
import './App.css'

function App() {
  const {
    screen,
    setScreen,
    registerForm,
    setRegisterForm,
    isAuthenticated,
    profile,
    isLoading,
    bootstrapped,
    errorMessage,
    successMessage,
    startLogin,
    register,
    refreshSession,
    logout,
  } = useAuth()

  if (!bootstrapped) {
    return (
      <main className="app-shell centered">
        <section className="auth-card">
          <h1>Getting things ready</h1>
          <p>Please wait a moment.</p>
        </section>
      </main>
    )
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <p className="eyebrow">SkyFly</p>
        <h1>Book Your Flight</h1>
        <p>Plan your trip in a few easy steps.</p>
      </header>

      {!isAuthenticated ? (
        <section className="auth-card">
          {screen === 'login' ? (
            <LoginCard
              isLoading={isLoading}
              onLogin={() => void startLogin()}
              onShowRegister={() => setScreen('register')}
            />
          ) : (
            <RegisterCard
              form={registerForm}
              isLoading={isLoading}
              onFormChange={setRegisterForm}
              onSubmit={register}
              onShowLogin={() => setScreen('login')}
            />
          )}
        </section>
      ) : (
        <BookingCard
          profile={profile}
          isLoading={isLoading}
          onRefreshDetails={() => void refreshSession()}
          onLogout={() => void logout()}
        />
      )}

      {successMessage ? <p className="status ok">{successMessage}</p> : null}
      {errorMessage ? <p className="status err">{errorMessage}</p> : null}
    </main>
  )
}

export default App
