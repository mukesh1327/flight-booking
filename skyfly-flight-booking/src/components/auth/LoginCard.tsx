type Props = {
  isLoading: boolean
  onLogin: () => void
  onShowRegister: () => void
}

export const LoginCard = ({ isLoading, onLogin, onShowRegister }: Props) => (
  <>
    <h2>Sign in</h2>
    <p>Continue with your SkyFly account.</p>
    <button type="button" onClick={onLogin} disabled={isLoading}>
      Continue to login
    </button>
    <p className="footnote">
      New customer?{' '}
      <button className="inline-btn" type="button" onClick={onShowRegister}>
        Create account
      </button>
    </p>
  </>
)
