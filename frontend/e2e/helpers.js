// Shared helpers for the E2E tests.
const AUTH_KEY = 'smartcity.auth'
const PASSWORD = 'Passw0rd!'

export function uniqueName(prefix)
{
  return `${prefix}_${Date.now()}_${Math.floor(Math.random() * 1e6)}`
}

// Registers a user through the API (fast, no UI) and returns the credentials.
export async function createUser(request, prefix = 'e2e')
{
  const username = uniqueName(prefix)
  const response = await request.post('/backend/api/auth/register', { data: { username, password: PASSWORD } })
  if (!response.ok()) throw new Error(`register failed: ${response.status()}`)
  return { username, password: PASSWORD }
}

// Logs in through the API and stores the token where the app looks for it,
// so a test can start on a protected page without repeating the login form.
export async function signIn(page, request)
{
  const user = await createUser(request)
  const response = await request.post('/backend/api/auth/login', { data: { username: user.username, password: user.password } })
  const { token, username } = await response.json()
  await page.addInitScript(([key, value]) => localStorage.setItem(key, value), [AUTH_KEY, JSON.stringify({ token, username })])
  return { ...user, token }
}

// Fills the CRUD form by field name and submits it.
export async function fillForm(page, values)
{
  for (const [name, value] of Object.entries(values))
  {
    const field = page.getByTestId(`field-${name}`)
    if ((await field.evaluate((el) => el.tagName)) === 'SELECT') await field.selectOption(String(value))
    else await field.fill(String(value))
  }
  await page.getByTestId('submit').click()
}

export { PASSWORD }
