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

  const logout = useCallback(() => {
    save(null)
    setAuth(null)
  }, [])

  const value = useMemo(
    () => ({ token: auth?.token ?? null, username: auth?.username ?? null, login, register, logout }),
    [auth, login, register, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
