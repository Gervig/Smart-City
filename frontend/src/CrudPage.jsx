import {useCallback, useEffect, useState} from 'react'
import {request} from './api.js'
import {useAuth} from './auth.jsx'
import {emptyForm, formatValue, fromRecord, toPayload} from './resources.js'

export default function CrudPage({resource}) {
    const {token, logout} = useAuth()

    const [rows, setRows] = useState([])
    const [page, setPage] = useState(0)
    const size = 20

    const [sortField, setSortField] = useState('id')
    const [sortDirection, setSortDirection] = useState('asc')
    const [totalElements, setTotalElements] = useState(0)
    const [totalPages, setTotalPages] = useState(0)

    const [loading, setLoading] = useState(true)
    const [form, setForm] = useState(() => emptyForm(resource))
    const [editingId, setEditingId] = useState(null)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')

    const handleError = useCallback((e) => {
        if (e.status === 401) {
            logout()
            return
        }

        setError(e.message)
    }, [logout])

    // Load the current page of records.
    const load = useCallback(async () => {
        setLoading(true)

        try {
            const result = await request(
                `${resource.path}?page=${page}&size=${size}&sort=${sortField},${sortDirection}`,
                { token }
            )

            setRows(result.data)
            setTotalElements(result.totalElements)
            setTotalPages(result.totalPages)
        } catch (e) {
            handleError(e)
        } finally {
            setLoading(false)
        }
    }, [resource.path, token, handleError, page, sortField, sortDirection])

    // Reset the form when switching resources.
    useEffect(() => {
        setForm(emptyForm(resource))
        setEditingId(null)
        setError('')
        setNotice('')
        setPage(0)
    }, [resource])

    // Load records whenever the page or resource changes.
    useEffect(() => {
        load()
    }, [load])

    function change(name, value) {
        setForm((f) => ({...f, [name]: value}))
    }

    function handleSort(field) {
        if (sortField === field) {
            setSortDirection((current) =>
                current === 'asc' ? 'desc' : 'asc'
            )
        } else {
            setSortField(field)
            setSortDirection('asc')
        }

        setPage(0)
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
                await request(resource.path, {
                    method: 'POST',
                    body,
                    token
                })

                setNotice('Created.')
            } else {
                await request(`${resource.path}/${editingId}`, {
                    method: 'PUT',
                    body,
                    token
                })

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
            await request(`${resource.path}/${id}`, {
                method: 'DELETE',
                token
            })

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

            {error && (
                <p role="alert" className="error" data-testid="error">
                    {error}
                </p>
            )}

            {notice && (
                <p role="status" className="notice" data-testid="notice">
                    {notice}
                </p>
            )}

            <form
                onSubmit={submit}
                className="card"
                data-testid={testId('form')}
                noValidate
            >
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
                                    {f.options.map((o) => (
                                        <option key={o} value={o}>
                                            {o}
                                        </option>
                                    ))}
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
                    <button type="submit" data-testid="submit">
                        {editingId === null ? 'Create' : 'Save'}
                    </button>

                    {editingId !== null && (
                        <button
                            type="button"
                            className="secondary"
                            onClick={reset}
                            data-testid="cancel"
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </form>

            {loading ? (
                <p>Loading…</p>
            ) : (
                <>
                    <div className="pagination">
                        <button
                            type="button"
                            className="secondary"
                            disabled={page === 0}
                            onClick={() => setPage((current) => current - 1)}
                        >
                            Previous
                        </button>

                        <span>
              Page {page + 1} of {totalPages} (
                            {rows.length} records displayed out of {totalElements} total)
            </span>

                        <button
                            type="button"
                            className="secondary"
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage((current) => current + 1)}
                        >
                            Next
                        </button>
                    </div>

                    <table data-testid={testId('table')}>
                        <thead>
                        <tr>
                            <th>
                                <button
                                    type="button"
                                    className="secondary"
                                    onClick={() => handleSort('id')}
                                >
                                    ID {sortField === 'id' && (sortDirection === 'asc' ? '↑' : '↓')}
                                </button>
                            </th>

                            {resource.fields.map((f) => (
                                <th key={f.name}>
                                    <button
                                        type="button"
                                        className="secondary"
                                        onClick={() => handleSort(f.name)}
                                    >
                                        {f.label}{' '}
                                        {sortField === f.name &&
                                            (sortDirection === 'asc' ? '↑' : '↓')}
                                    </button>
                                </th>
                            ))}

                            <th></th>
                        </tr>
                        </thead>

                        <tbody>
                        {rows.length === 0 && (
                            <tr>
                                <td
                                    colSpan={resource.fields.length + 2}
                                    data-testid="empty"
                                >
                                    No rows.
                                </td>
                            </tr>
                        )}

                        {rows.map((row) => (
                            <tr key={row.id} data-testid="row">
                                <td>{row.id}</td>

                                {resource.fields.map((f) => (
                                    <td key={f.name}>
                                        {formatValue(f, row[f.name])}
                                    </td>
                                ))}

                                <td className="row-actions">
                                    <button
                                        type="button"
                                        className="secondary"
                                        onClick={() => edit(row)}
                                        data-testid="edit"
                                    >
                                        Edit
                                    </button>

                                    <button
                                        type="button"
                                        className="danger"
                                        onClick={() => remove(row.id)}
                                        data-testid="delete"
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>

                    <div className="pagination">
                        <button
                            type="button"
                            className="secondary"
                            disabled={page === 0}
                            onClick={() => setPage((current) => current - 1)}
                        >
                            Previous
                        </button>

                        <span>
              Page {page + 1} of {totalPages} ({totalElements} records)
            </span>

                        <button
                            type="button"
                            className="secondary"
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage((current) => current + 1)}
                        >
                            Next
                        </button>
                    </div>
                </>
            )}
        </section>
    )
}
