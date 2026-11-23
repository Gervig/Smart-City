// All calls go through the Vite proxy: /backend/<path> -> http://localhost:7070/<path>
const BASE = '/backend'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

// The backend answers errors as {"error": "..."} (CRUD) or {"msg": "..."} (login).
// Some 401 responses are plain text. Success bodies can be empty (204).
export async function request(path, { method = 'GET', body, token } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server.')
  }

  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = text
    }
  }

  if (!response.ok) {
    let message = (data && (data.error || data.msg)) || (typeof data === 'string' ? data : '')
    if (!message) {
      message = response.status >= 500
        ? `The API is not responding (status ${response.status}). Is the backend running?`
        : `Request failed with status ${response.status}`
    }
    throw new ApiError(response.status, `HTTP ${response.status}: ${message}; response body: ${text || '(empty)'}`)
  }
  return data
}
