import { Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import AuthPage from './AuthPage.jsx'
import CrudPage from './CrudPage.jsx'
import NearbyPage from './NearbyPage.jsx'
import { RESOURCES } from './resources.js'

// Pages that need a token redirect to /login when there is none.
function Protected({ children }) {
  const { token } = useAuth()
  return token ? children : <Navigate to="/login" replace />
}

export default function App() {
  const { token, username, logout } = useAuth()

  return (
    <>
      <header>
        <strong>Smart City</strong>
        {token && (
          <nav data-testid="nav">
            {RESOURCES.map((r) => <NavLink key={r.key} to={`/${r.key}`}>{r.label}</NavLink>)}
            <NavLink to="/nearby">Nearby</NavLink>
          </nav>
        )}
        <span className="spacer" />
        {token ? (
          <>
            <span data-testid="current-user">{username}</span>
            <button type="button" className="secondary" onClick={logout} data-testid="logout">Log out</button>
          </>
        ) : (
          <nav>
            <NavLink to="/login">Log in</NavLink>
            <NavLink to="/register">Register</NavLink>
          </nav>
        )}
      </header>
      <main>
        <Routes>
          <Route path="/login" element={token ? <Navigate to="/families" replace /> : <AuthPage mode="login" />} />
          <Route path="/register" element={token ? <Navigate to="/families" replace /> : <AuthPage mode="register" />} />
          {RESOURCES.map((r) => (
            <Route key={r.key} path={`/${r.key}`} element={<Protected><CrudPage key={r.key} resource={r} /></Protected>} />
          ))}
          <Route path="/nearby" element={<Protected><NearbyPage /></Protected>} />
          <Route path="*" element={<Navigate to={token ? '/families' : '/login'} replace />} />
        </Routes>
      </main>
    </>
  )
}
