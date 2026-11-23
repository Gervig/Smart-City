import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { request } from './api.js'

// The token lives in localStorage so a page reload keeps the user logged in.
// The tests use the same key to sign in without going through the form.
export const AUTH_KEY = 'smartcity.auth'

function load() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY))
  } catch {
    return null
  }
}

function save(value) {
  try {
    if (value) localStorage.setItem(AUTH_KEY, JSON.stringify(value))
    else localStorage.removeItem(AUTH_KEY)
  } catch {
    // storage can be unavailable (private window); the session then lasts until reload
  }
}

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(load)

  const login = useCallback(async (username, password) => {
    const data = await request('/api/auth/login', { method: 'POST', body: { username, password } })
    const next = { token: data.token, username: data.username }
    save(next)
    setAuth(next)
  }, [])

  // Register, then log in with the same credentials.
  const register = useCallback(async (username, password) => {
    await request('/api/auth/register', { method: 'POST', body: { username, password } })
    await login(username, password)
  }, [login])

  // Forget the token here first, so the page never waits, then tell the backend to reject it from now on
  // (POST /api/auth/logout puts it on the denylist). If that call fails (offline, or the token was already
  // invalid, which is why a 401 logs out), there is nothing more to do: the token is gone from this browser.
  const logout = useCallback(() => {
    const token = auth?.token
    save(null)
    setAuth(null)
    if (token) {
      request('/api/auth/logout', { method: 'POST', token }).catch(() => {})
    }
  }, [auth])

  const value = useMemo(
    () => ({ token: auth?.token ?? null, username: auth?.username ?? null, login, register, logout }),
    [auth, login, register, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
