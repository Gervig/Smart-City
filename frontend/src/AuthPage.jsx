import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './auth.jsx'

// One component for both /login and /register.
export default function AuthPage({ mode }) {
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const isLogin = mode === 'login'

  async function submit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (isLogin) await login(username, password)
      else await register(username, password)
      navigate('/families')
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="narrow">
      <h1>{isLogin ? 'Log in' : 'Create account'}</h1>
      {error && <p role="alert" className="error" data-testid="error">{error}</p>}
      <form onSubmit={submit} className="card" noValidate>
        <label>
          Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} data-testid="username" autoComplete="username" />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} data-testid="password" autoComplete={isLogin ? 'current-password' : 'new-password'} />
        </label>
        <div className="actions">
          <button type="submit" disabled={busy} data-testid="submit">{isLogin ? 'Log in' : 'Register'}</button>
        </div>
      </form>
      <p>
        {isLogin ? <>No account? <Link to="/register">Register</Link></> : <>Already registered? <Link to="/login">Log in</Link></>}
      </p>
    </section>
  )
}
