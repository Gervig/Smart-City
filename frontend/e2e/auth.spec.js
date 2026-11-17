import { expect, test } from '@playwright/test'
import { createUser, PASSWORD, uniqueName } from './helpers.js'

test.describe('Authentication', () =>
{
  test('an unauthenticated visitor is redirected to the login page', async ({ page }) =>
  {
    await page.goto('/citizens')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible()
  })

  test('a new user can register and is logged in', async ({ page }) =>
  {
    const username = uniqueName('register')
    await page.goto('/register')

    await page.getByTestId('username').fill(username)
    await page.getByTestId('password').fill(PASSWORD)
    await page.getByTestId('submit').click()

    await expect(page).toHaveURL(/\/families$/)
    await expect(page.getByTestId('current-user')).toHaveText(username)
  })

  test('an existing user can log in', async ({ page, request }) =>
  {
    const user = await createUser(request, 'login')
    await page.goto('/login')

    await page.getByTestId('username').fill(user.username)
    await page.getByTestId('password').fill(user.password)
    await page.getByTestId('submit').click()

    await expect(page).toHaveURL(/\/families$/)
    await expect(page.getByTestId('current-user')).toHaveText(user.username)
  })

  test('a wrong password shows an error and stays on the login page', async ({ page, request }) =>
  {
    const user = await createUser(request, 'wrongpw')
    await page.goto('/login')

    await page.getByTestId('username').fill(user.username)
    await page.getByTestId('password').fill('not-the-password')
    await page.getByTestId('submit').click()

    await expect(page.getByTestId('error')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('an unknown user cannot log in', async ({ page }) =>
  {
    await page.goto('/login')

    await page.getByTestId('username').fill(uniqueName('ghost'))
    await page.getByTestId('password').fill(PASSWORD)
    await page.getByTestId('submit').click()

    await expect(page.getByTestId('error')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('an empty login form is rejected', async ({ page }) =>
  {
    await page.goto('/login')

    await page.getByTestId('submit').click()

    await expect(page.getByTestId('error')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('logging out returns to the login page and blocks protected pages', async ({ page, request }) =>
  {
    const user = await createUser(request, 'logout')
    await page.goto('/login')
    await page.getByTestId('username').fill(user.username)
    await page.getByTestId('password').fill(user.password)
    await page.getByTestId('submit').click()
    await expect(page).toHaveURL(/\/families$/)

    await page.getByTestId('logout').click()
    await expect(page).toHaveURL(/\/login$/)

    await page.goto('/families')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('a 401 from the API logs the user out and returns to the login page', async ({ page }) =>
  {
    await page.addInitScript(() => localStorage.setItem('smartcity.auth', JSON.stringify({ token: 'expired', username: 'x' })))
    await page.route('**/backend/families', (route) => route.fulfill({ status: 401, body: 'Unauthorized' }))

    await page.goto('/families')

    await expect(page).toHaveURL(/\/login$/)
  })
})
