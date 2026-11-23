// Shared helpers for the E2E tests.
import { expect } from '@playwright/test'

const AUTH_KEY = 'smartcity.auth'
const PASSWORD = 'Passw0rd!'

export async function assertResponseOk(response, operation)
{
  if (!response.ok())
  {
    const body = await response.text()
    throw new Error(`${operation} returned HTTP ${response.status()}; response body: ${body || '(empty)'}`)
  }
}

export async function assertResponseStatus(response, expectedStatus, operation)
{
  if (response.status() !== expectedStatus)
  {
    const body = await response.text()
    throw new Error(`${operation} returned HTTP ${response.status()} (expected ${expectedStatus}); response body: ${body || '(empty)'}`)
  }
}

export function uniqueName(prefix)
{
  return `${prefix}_${Date.now()}_${Math.floor(Math.random() * 1e6)}`
}

// Registers a user through the API (fast, no UI) and returns the credentials.
export async function createUser(request, prefix = 'e2e')
{
  const username = uniqueName(prefix)
  const response = await request.post('/backend/api/auth/register', { data: { username, password: PASSWORD } })
  await assertResponseOk(response, 'register')
  return { username, password: PASSWORD }
}

// Logs in through the API and stores the token where the app looks for it,
// so a test can start on a protected page without repeating the login form.
export async function signIn(page, request)
{
  const user = await createUser(request)
  const response = await request.post('/backend/api/auth/login', { data: { username: user.username, password: user.password } })
  await assertResponseOk(response, 'login')
  const { token, username } = await response.json()
  await page.addInitScript(([key, value]) => localStorage.setItem(key, value), [AUTH_KEY, JSON.stringify({ token, username })])
  return { ...user, token }
}

// Fills the CRUD form by field name and submits it.
export async function fillForm(page, values, { expected = 'success' } = {})
{
  for (const [name, value] of Object.entries(values))
  {
    const field = page.getByTestId(`field-${name}`)
    if ((await field.evaluate((el) => el.tagName)) === 'SELECT') await field.selectOption(String(value))
    else await field.fill(String(value))
  }
  await page.getByTestId('submit').click()

  const error = page.getByTestId('error')
  if (expected === 'error')
  {
    await expect(error).toBeVisible()
    return
  }

  const notice = page.getByTestId('notice')
  await notice.or(error).waitFor({ state: 'visible' })
  if (await error.isVisible())
  {
    throw new Error(`CRUD submission failed: ${await error.innerText()}`)
  }

  await expect(page.locator('table[data-testid$="-table"]')).toBeVisible()
  if (await error.isVisible())
  {
    throw new Error(`CRUD refresh failed: ${await error.innerText()}`)
  }
  if (await error.isVisible())
  {
    throw new Error(`CRUD refresh failed: ${await error.innerText()}`)
  }
}

export { PASSWORD }
