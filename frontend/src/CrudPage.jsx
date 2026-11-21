import {useCallback, useEffect, useRef, useState} from 'react'
import {request} from './api.js'
import {useAuth} from './auth.jsx'
import {emptyForm, formatValue, fromRecord, toPayload} from './resources.js'

export default function CrudPage({resource}) {
    const {token, logout} = useAuth()

    const [rows, setRows] = useState([])
    const [page, setPage] = useState(0)
    const size = 20

    const [sortField, setSortField] = useState(resource.defaultSort.field)
    const [sortDirection, setSortDirection] = useState(resource.defaultSort.direction)
    const [totalElements, setTotalElements] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [filters, setFilters] = useState({})
    const [filterDraft, setFilterDraft] = useState({})

    const [loading, setLoading] = useState(true)
    const [form, setForm] = useState(() => emptyForm(resource))
    const [editingId, setEditingId] = useState(null)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')
    const latestRequest = useRef(0)

    const handleError = useCallback((e) => {
        if (e.status === 401) {
            logout()
            return
        }

        setError(e.message)
    }, [logout])

    // Load the current page of records.
    const load = useCallback(async (requestedPage = page) => {
        const requestId = ++latestRequest.current
        setLoading(true)

        try {
            const params = new URLSearchParams({
                page: String(requestedPage),
                size: String(size),
                sort: `${sortField},${sortDirection}`,
            })
            Object.entries(filters).forEach(([name, value]) => {
                if (value !== '') params.set(name, String(value))
            })

            const result = await request(`${resource.path}?${params}`, { token })

            if (requestId !== latestRequest.current) return
            setRows(result.data)
            setTotalElements(result.totalElements)
            setTotalPages(result.totalPages)
            const lastPage = Math.max(result.totalPages - 1, 0)
            if (requestedPage > lastPage) setPage(lastPage)
        } catch (e) {
            if (requestId === latestRequest.current) handleError(e)
        } finally {
            if (requestId === latestRequest.current) setLoading(false)
        }
    }, [resource.path, token, handleError, page, sortField, sortDirection, filters])

    useEffect(() => {
        setFilterDraft(filters)
    }, [filters])

    // Load records whenever the page or resource changes.
    useEffect(() => {
        void load()
        return () => {
            latestRequest.current += 1
        }
    }, [load])

    function change(name, value) {
        setForm((f) => ({...f, [name]: value}))
    }

    function changeFilter(name, value) {
        setFilterDraft((current) => ({...current, [name]: value}))
    }

    function applyFilters(event) {
        event.preventDefault()
        const nextFilters = Object.fromEntries(
            Object.entries(filterDraft).filter(([, value]) => value !== '')
        )
        setPage(0)
        setFilters(nextFilters)
    }

    function clearFilters() {
        setFilterDraft({})
        setPage(0)
        setFilters({})
    }

    function handleSort(field) {
        if (!resource.sortableFields.includes(field)) return

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
                setPage(0)
            } else {
                await request(`${resource.path}/${editingId}`, {
                    method: 'PUT',
                    body,
                    token
                })

                setNotice('Updated.')
            }

            reset()
            await load(editingId === null ? 0 : page)
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

            {resource.filters?.length > 0 && (
                <form className="card" onSubmit={applyFilters} data-testid="filters">
                    <h2>Filter records</h2>
                    <div className="grid">
                        {resource.filters.map((filter) => (
                            <label key={filter.name}>
                                {filter.label}
                                {filter.type === 'select' ? (
                                    <select
                                        name={filter.name}
                                        value={filterDraft[filter.name] ?? ''}
                                        onChange={(e) => changeFilter(filter.name, e.target.value)}
                                        data-testid={`filter-${filter.name}`}
                                    >
                                        <option value="">All</option>
                                        {filter.options.map((option) => (
                                            <option key={option} value={option}>{option}</option>
                                        ))}
                                    </select>
                                ) : (
                                    <input
                                        name={filter.name}
                                        type={filter.type}
                                        value={filterDraft[filter.name] ?? ''}
                                        onChange={(e) => changeFilter(filter.name, e.target.value)}
                                        data-testid={`filter-${filter.name}`}
                                    />
                                )}
                            </label>
                        ))}
                    </div>
                    <div className="actions">
                        <button type="submit">Apply filters</button>
                        <button type="button" className="secondary" onClick={clearFilters}>Clear</button>
                    </div>
                </form>
            )}

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
                            {totalPages === 0
                                ? 'No pages'
                                : `Page ${page + 1} of ${totalPages}`}
                            {` (${rows.length} records displayed out of ${totalElements} total)`}
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
                            <th aria-sort={sortField === 'id' ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none'}>
                                <button
                                    type="button"
                                    className="secondary"
                                    onClick={() => handleSort('id')}
                                >
                                    ID {sortField === 'id' && (sortDirection === 'asc' ? '↑' : '↓')}
                                </button>
                            </th>

                            {resource.fields.map((f) => (
                                <th
                                    key={f.name}
                                    aria-sort={sortField === f.name ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none'}
                                >
                                    <button
                                        type="button"
                                        className="secondary"
                                        disabled={!resource.sortableFields.includes(f.name)}
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
                </>
            )}
        </section>
    )
}
