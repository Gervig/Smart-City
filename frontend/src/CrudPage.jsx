import { useCallback, useEffect, useState } from 'react'
import { request } from './api.js'
import { useAuth } from './auth.jsx'
import { emptyForm, formatValue, fromRecord, toPayload } from './resources.js'

export default function CrudPage({ resource }) {
  const { token, logout } = useAuth()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(() => emptyForm(resource))
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const handleError = useCallback((e) => {
    if (e.status === 401) {
      logout() // expired or invalid token: back to the login page
      return
    }
    setError(e.message)
  }, [logout])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setRows(await request(resource.path, { token }))
    } catch (e) {
      handleError(e)
    } finally {
      setLoading(false)
    }
  }, [resource.path, token, handleError])

  useEffect(() => {
    setForm(emptyForm(resource))
    setEditingId(null)
    setError('')
    setNotice('')
    load()
  }, [resource, load])

  function change(name, value) {
    setForm((f) => ({ ...f, [name]: value }))
  }

  function reset() {
    setForm(emptyForm(resource))
    setEditingId(null)
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    try {
      const body = toPayload(resource, form)
      if (editingId === null) {
        await request(resource.path, { method: 'POST', body, token })
        setNotice('Created.')
      } else {
        await request(`${resource.path}/${editingId}`, { method: 'PUT', body, token })
        setNotice('Updated.')
      }
      reset()
      await load()
    } catch (e) {
      handleError(e)
    }
  }

  async function remove(id) {
    setError('')
    setNotice('')
    try {
      await request(`${resource.path}/${id}`, { method: 'DELETE', token })
      setNotice('Deleted.')
      await load()
    } catch (e) {
      handleError(e)
    }
  }

  function edit(row) {
    setEditingId(row.id)
    setForm(fromRecord(resource, row))
    setError('')
    setNotice('')
  }

  const testId = (suffix) => `${resource.key}-${suffix}`

  return (
    <section>
      <h1>{resource.label}</h1>

      {error && <p role="alert" className="error" data-testid="error">{error}</p>}
      {notice && <p role="status" className="notice" data-testid="notice">{notice}</p>}

      <form onSubmit={submit} className="card" data-testid={testId('form')} noValidate>
        <h2>{editingId === null ? 'Add new' : `Edit #${editingId}`}</h2>
        <div className="grid">
          {resource.fields.map((f) => (
            <label key={f.name}>
              {f.label}
              {f.type === 'select' ? (
                <select
                  name={f.name}
                  value={form[f.name]}
                  onChange={(e) => change(f.name, e.target.value)}
                  data-testid={`field-${f.name}`}
                >
                  {f.options.map((o) => <option key={o} value={o}>{o}</option>)}
                </select>
              ) : (
                <input
                  name={f.name}
                  type={f.type === 'id' ? 'number' : f.type}
                  value={form[f.name]}
                  onChange={(e) => change(f.name, e.target.value)}
                  data-testid={`field-${f.name}`}
                />
              )}
            </label>
          ))}
        </div>
        <div className="actions">
          <button type="submit" data-testid="submit">{editingId === null ? 'Create' : 'Save'}</button>
          {editingId !== null && <button type="button" className="secondary" onClick={reset} data-testid="cancel">Cancel</button>}
        </div>
      </form>

      {loading ? (
        <p>Loading…</p>
      ) : (
        <table data-testid={testId('table')}>
          <thead>
            <tr>
              <th>id</th>
              {resource.fields.map((f) => <th key={f.name}>{f.label}</th>)}
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr><td colSpan={resource.fields.length + 2} data-testid="empty">No rows.</td></tr>
            )}
            {rows.map((row) => (
              <tr key={row.id} data-testid="row">
                <td>{row.id}</td>
                {resource.fields.map((f) => <td key={f.name}>{formatValue(f, row[f.name])}</td>)}
                <td className="row-actions">
                  <button type="button" className="secondary" onClick={() => edit(row)} data-testid="edit">Edit</button>
                  <button type="button" className="danger" onClick={() => remove(row.id)} data-testid="delete">Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
