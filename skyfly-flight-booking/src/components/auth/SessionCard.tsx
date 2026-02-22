import { AUTH_API_BASE_URL } from '../../config/env'
import type { UserProfile } from '../../types/auth'

type Props = {
  profile: UserProfile | null
  roles: string[]
  isLoading: boolean
  onRefresh: () => void
  onLogout: () => void
}

export const SessionCard = ({ profile, roles, isLoading, onRefresh, onLogout }: Props) => (
  <section className="dashboard-card">
    <div>
      <h2>Welcome</h2>
      <p>You are signed in to SkyFly.</p>
    </div>
    <div className="badge-row">
      <span className="badge">API: {AUTH_API_BASE_URL || 'same-origin'}</span>
      <span className="badge">Roles: {roles.join(', ') || 'none'}</span>
    </div>
    {profile ? (
      <div className="profile-grid">
        <p>
          <span>Name</span>
          <strong>{profile.name}</strong>
        </p>
        <p>
          <span>Email</span>
          <strong>{profile.email}</strong>
        </p>
        <p>
          <span>Phone</span>
          <strong>{profile.phone}</strong>
        </p>
        <p>
          <span>User ID</span>
          <strong>{profile.userId}</strong>
        </p>
      </div>
    ) : (
      <p className="footnote">Profile could not be loaded.</p>
    )}
    <div className="button-row">
      <button type="button" className="secondary" onClick={onRefresh} disabled={isLoading}>
        Refresh session
      </button>
      <button type="button" onClick={onLogout} disabled={isLoading}>
        Logout
      </button>
    </div>
  </section>
)
