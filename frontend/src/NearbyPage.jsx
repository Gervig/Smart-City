import { useState } from 'react'
import { request } from './api.js'
import { useAuth } from './auth.jsx'

// Demonstrates GET /locations/{id}/nearby?radius=&amenity= (backed by the public Overpass API).
export default function NearbyPage() {
  const { token, logout } = useAuth()
  const [locationId, setLocationId] = useState('')
  const [radius, setRadius] = useState('1000')
  const [amenity, setAmenity] = useState('hospital')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function search(event) {
    event.preventDefault()
    setError('')
    setResult(null)
    setBusy(true)
    try {
      const query = new URLSearchParams({ radius, amenity }).toString()
      setResult(await request(`/locations/${locationId}/nearby?${query}`, { token }))
    } catch (e) {
      if (e.status === 401) logout()
      else setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section>
      <h1>Nearby places</h1>
      <form onSubmit={search} className="card" data-testid="nearby-form" noValidate>
        <div className="grid">
          <label>
            Location id
            <input type="number" value={locationId} onChange={(e) => setLocationId(e.target.value)} data-testid="nearby-location" />
          </label>
          <label>
            Radius (m)
            <input type="number" value={radius} onChange={(e) => setRadius(e.target.value)} data-testid="nearby-radius" />
          </label>
          <label>
            Amenity
            <select value={amenity} onChange={(e) => setAmenity(e.target.value)} data-testid="nearby-amenity">
              {['hospital', 'school', 'pharmacy', 'restaurant', 'library'].map((a) => <option key={a}>{a}</option>)}
            </select>
          </label>
        </div>
        <div className="actions">
          <button type="submit" disabled={busy} data-testid="nearby-search">{busy ? 'Searching…' : 'Search'}</button>
        </div>
      </form>

      {error && <p role="alert" className="error" data-testid="error">{error}</p>}

      {result && (
        <div data-testid="nearby-result">
          {result.message && <p className="notice" data-testid="nearby-message">{result.message}</p>}
          {result.places?.length === 0 ? (
            <p data-testid="nearby-empty">No places found.</p>
          ) : (
            <ul>
              {result.places?.map((p, i) => (
                <li key={i} data-testid="nearby-place">{p.name} <small>({p.type})</small></li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}
