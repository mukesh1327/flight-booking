import type { FormEvent } from 'react'
import type { RegisterRequest } from '../../types/auth'

type Props = {
  form: RegisterRequest
  isLoading: boolean
  onFormChange: (next: RegisterRequest) => void
  onSubmit: () => void
  onShowLogin: () => void
}

export const RegisterCard = ({ form, isLoading, onFormChange, onSubmit, onShowLogin }: Props) => {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void onSubmit()
  }

  return (
    <>
      <h2>Create customer account</h2>
      <form onSubmit={submit}>
        <label>
          Full Name
          <input required value={form.name} onChange={(event) => onFormChange({ ...form, name: event.target.value })} />
        </label>
        <label>
          Username
          <input
            required
            minLength={4}
            maxLength={32}
            value={form.username}
            onChange={(event) => onFormChange({ ...form, username: event.target.value })}
          />
        </label>
        <label>
          Email
          <input
            required
            type="email"
            value={form.email}
            onChange={(event) => onFormChange({ ...form, email: event.target.value })}
          />
        </label>
        <label>
          Password
          <input
            required
            type="password"
            minLength={8}
            value={form.password}
            onChange={(event) => onFormChange({ ...form, password: event.target.value })}
          />
        </label>
        <label>
          Phone
          <input required value={form.phone} onChange={(event) => onFormChange({ ...form, phone: event.target.value })} />
        </label>
        <button type="submit" disabled={isLoading}>
          Create account
        </button>
      </form>
      <p className="footnote">
        Already registered?{' '}
        <button className="inline-btn" type="button" onClick={onShowLogin}>
          Back to sign in
        </button>
      </p>
    </>
  )
}
