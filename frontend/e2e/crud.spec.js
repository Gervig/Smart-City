import { expect, test } from '@playwright/test'
import { fillForm, signIn, uniqueName } from './helpers.js'

test.describe('CRUD pages', () =>
{
  test.beforeEach(async ({ page, request }) =>
  {
    await signIn(page, request)
  })

  test('create, edit and delete a family', async ({ page }) =>
  {
    const name = uniqueName('Family')
    const renamed = `${name}_renamed`
    await page.goto('/families')

    await fillForm(page, { familyName: name })
    const row = page.getByTestId('row').filter({ hasText: name })
    await expect(row).toBeVisible()
    await expect(page.getByTestId('notice')).toHaveText('Created.')

    await row.getByTestId('edit').click()
    await fillForm(page, { familyName: renamed })
    const renamedRow = page.getByTestId('row').filter({ hasText: renamed })
    await expect(renamedRow).toBeVisible()
    await expect(page.getByTestId('row').filter({ hasText: `${name}` }).filter({ hasNotText: 'renamed' })).toHaveCount(0)

    await renamedRow.getByTestId('delete').click()
    await expect(page.getByTestId('row').filter({ hasText: renamed })).toHaveCount(0)
  })

  test('a family without a name is rejected', async ({ page }) =>
  {
    await page.goto('/families')

    await page.getByTestId('submit').click()

    await expect(page.getByTestId('error')).toContainText('required')
  })

  test('sorting and filters reset when navigating between resources', async ({ page }) =>
  {
    const listRequests = []
    page.on('request', (request) =>
    {
      if (request.url().includes('/backend/')) listRequests.push(request.url())
    })

    await page.goto('/families')
    const familyNameColumn = page.getByRole('columnheader').filter({ has: page.getByRole('button', { name: /Family name/ }) })
    await familyNameColumn.getByRole('button').click()
    await expect(familyNameColumn).toHaveAttribute('aria-sort', 'ascending')

    const familyFilterRequest = page.waitForRequest((request) =>
      new URL(request.url()).pathname === '/backend/families'
      && new URL(request.url()).searchParams.has('familyName'))
    await page.getByTestId('filter-familyName').fill('state-reset-test')
    await page.getByRole('button', { name: 'Apply filters' }).click()
    await familyFilterRequest

    const citizenRequest = page.waitForRequest((request) =>
      new URL(request.url()).pathname === '/backend/citizens')
    await page.getByRole('link', { name: 'Citizens' }).click()
    const citizenUrl = new URL((await citizenRequest).url())
    expect(citizenUrl.searchParams.get('sort')).toBe('id,asc')
    expect(citizenUrl.searchParams.has('familyName')).toBe(false)
    const citizenIdColumn = page.getByRole('columnheader').filter({ has: page.getByRole('button', { name: /^ID/ }) })
    await expect(citizenIdColumn).toHaveAttribute('aria-sort', 'ascending')
    const lastNameColumn = page.getByRole('columnheader').filter({ has: page.getByRole('button', { name: /Last name/ }) })
    await expect(lastNameColumn).toHaveAttribute('aria-sort', 'none')

    const lastNameRequest = page.waitForRequest((request) =>
      new URL(request.url()).pathname === '/backend/citizens'
      && new URL(request.url()).searchParams.get('sort') === 'lastName,asc')
    await lastNameColumn.getByRole('button').click()
    await lastNameRequest

    const locationRequest = page.waitForRequest((request) =>
      new URL(request.url()).pathname === '/backend/locations')
    await page.getByRole('link', { name: 'Locations' }).click()
    const locationUrl = new URL((await locationRequest).url())
    expect(locationUrl.searchParams.get('sort')).toBe('id,asc')
    expect(locationUrl.searchParams.has('familyName')).toBe(false)
    expect(locationUrl.searchParams.has('lastName')).toBe(false)

    const streetAddressColumn = page.getByRole('columnheader').filter({ has: page.getByRole('button', { name: /Street address/ }) })
    const streetAddressRequest = page.waitForRequest((request) =>
      new URL(request.url()).pathname === '/backend/locations'
      && new URL(request.url()).searchParams.get('sort') === 'streetAddress,asc')
    await streetAddressColumn.getByRole('button').click()
    await streetAddressRequest

    const housingRequest = page.waitForRequest((request) =>
      new URL(request.url()).pathname === '/backend/housings')
    await page.getByRole('link', { name: 'Housings' }).click()
    const housingUrl = new URL((await housingRequest).url())
    expect(housingUrl.searchParams.get('sort')).toBe('id,asc')
    expect(housingUrl.searchParams.has('familyName')).toBe(false)
    expect(housingUrl.searchParams.has('lastName')).toBe(false)
    expect(housingUrl.searchParams.has('streetAddress')).toBe(false)
    expect(listRequests.some((url) => new URL(url).searchParams.get('sort') === 'familyName,asc')).toBe(true)
  })

  test('a delayed response from the previous resource cannot replace current rows', async ({ page }) =>
  {
    let releaseFamilyResponse
    let familyRequestStarted
    const familyRequestObserved = new Promise((resolve) => { familyRequestStarted = resolve })
    const familyResponseGate = new Promise((resolve) => { releaseFamilyResponse = resolve })
    await page.route('**/backend/families**', async (route) =>
    {
      if (route.request().method() !== 'GET' || new URL(route.request().url()).pathname !== '/backend/families')
      {
        await route.continue()
        return
      }

      familyRequestStarted()
      await familyResponseGate
      await route.fulfill({
        json: {
          data: [{ id: 987654321, familyName: 'Stale family response' }],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      })
    })

    await page.goto('/families')
    await familyRequestObserved

    const citizenRequest = page.waitForRequest((request) =>
      new URL(request.url()).pathname === '/backend/citizens')
    await page.getByRole('link', { name: 'Citizens' }).click()
    await citizenRequest
    await expect(page.getByTestId('citizens-table')).toBeVisible()

    const familyResponse = page.waitForResponse((response) =>
      new URL(response.url()).pathname === '/backend/families')
    releaseFamilyResponse()
    await familyResponse
    await expect(page.getByText('Stale family response')).toHaveCount(0)
    await expect(page.getByRole('heading', { name: 'Citizens' })).toBeVisible()
  })

  test('create a location and a housing that points to it', async ({ page }) =>
  {
    const place = uniqueName('Place')
    await page.goto('/locations')
    await fillForm(page, { name: place, streetAddress: 'Testvej 1' })
    const locationRow = page.getByTestId('row').filter({ hasText: place })
    await expect(locationRow).toBeVisible()
    const locationId = await locationRow.locator('td').first().innerText()

    await page.getByRole('link', { name: 'Housings' }).click()
    await fillForm(page, { locationId, housingType: 'apartment', capacity: 4 })

    await expect(page.getByTestId('row').filter({ hasText: 'apartment' }).filter({ hasText: locationId }).first()).toBeVisible()
  })

  test('a housing with capacity 0 is rejected', async ({ page }) =>
  {
    await page.goto('/housings')

    await fillForm(page, { housingType: 'house', capacity: 0 })

    await expect(page.getByTestId('error')).toContainText('capacity must be at least 1')
  })

  test('a citizen can be created and linked to a family', async ({ page }) =>
  {
    const family = uniqueName('CitFam')
    await page.goto('/families')
    await fillForm(page, { familyName: family })
    const familyId = await page.getByTestId('row').filter({ hasText: family }).locator('td').first().innerText()

    await page.getByRole('link', { name: 'Citizens' }).click()
    const first = uniqueName('Anna')
    await fillForm(page, { firstName: first, lastName: 'Hansen', birthDate: '1990-05-17', status: 'alive', gender: 'female', familyId })

    const row = page.getByTestId('row').filter({ hasText: first })
    await expect(row).toBeVisible()
    await expect(row).toContainText('1990-05-17')
    await expect(row).toContainText(familyId)
  })

  test('citizen list paginates, filters and toggles the active sort direction', async ({ page, request }) =>
  {
    const { token } = await signIn(page, request)
    const pageResponse = await request.get('/backend/citizens?page=0&size=1&sort=id,desc', {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(pageResponse.status()).toBe(200)
    const pageResult = await pageResponse.json()
    expect(pageResult).toMatchObject({ page: 0, size: 1 })
    expect(Array.isArray(pageResult.data)).toBe(true)
    expect(typeof pageResult.totalElements).toBe('number')
    expect(typeof pageResult.totalPages).toBe('number')

    const invalidSort = await request.get('/backend/citizens?sort=unsupported,asc', {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(invalidSort.status()).toBe(400)

    const invalidFilter = await request.get('/backend/citizens?unknownFilter=value', {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(invalidFilter.status()).toBe(400)

    const familyName = uniqueName('FilterFam')
    await page.goto('/families')
    await fillForm(page, { familyName })
    const familyId = await page.getByTestId('row').filter({ hasText: familyName }).locator('td').first().innerText()

    const lastName = uniqueName('FilterLast')
    await page.goto('/citizens')
    await fillForm(page, {
      firstName: uniqueName('FilterFirst'),
      lastName,
      birthDate: '1990-05-17',
      status: 'alive',
      gender: 'female',
      familyId,
    })

    await page.getByTestId('filter-lastName').fill(lastName)
    await page.getByRole('button', { name: 'Apply filters' }).click()
    await expect(page.getByTestId('row').filter({ hasText: lastName })).toBeVisible()

    const lastNameColumn = page.getByRole('columnheader').filter({ has: page.getByRole('button', { name: /Last name/ }) })
    await lastNameColumn.getByRole('button').click()
    await expect(lastNameColumn).toHaveAttribute('aria-sort', 'ascending')
    await lastNameColumn.getByRole('button').click()
    await expect(lastNameColumn).toHaveAttribute('aria-sort', 'descending')
  })

  test('a citizen born in the future is rejected', async ({ page }) =>
  {
    await page.goto('/citizens')

    await fillForm(page, { firstName: 'Future', lastName: 'Person', birthDate: '2999-01-01', status: 'alive' })

    await expect(page.getByTestId('error')).toContainText('birthDate must not be in the future')
  })

  test('an alive citizen with a death date is rejected', async ({ page }) =>
  {
    await page.goto('/citizens')

    await fillForm(page, { firstName: 'Zombie', lastName: 'Person', birthDate: '1980-01-01', deathDate: '2020-01-01', status: 'alive' })

    await expect(page.getByTestId('error')).toContainText('deathDate must be null when status is alive')
  })

  test('a citizen with a missing first name is rejected', async ({ page }) =>
  {
    await page.goto('/citizens')

    await fillForm(page, { lastName: 'Nameless', birthDate: '1980-01-01', status: 'alive' })

    await expect(page.getByTestId('error')).toContainText('firstName is required')
  })
})

test.describe('Nearby places', () =>
{
  test('shows the places returned by the API (Overpass stubbed)', async ({ page, request }) =>
  {
    await signIn(page, request)
    await page.route('**/backend/locations/*/nearby**', (route) => route.fulfill({
      json: {
        locationId: 1,
        enriched: false,
        message: null,
        places: [{ name: 'Rigshospitalet', type: 'hospital', latitude: 55.69, longitude: 12.57 }],
      },
    }))
    await page.goto('/nearby')

    await page.getByTestId('nearby-location').fill('1')
    await page.getByTestId('nearby-search').click()

    await expect(page.getByTestId('nearby-place')).toHaveText(/Rigshospitalet/)
  })

  test('an unknown location shows an error from the API', async ({ page, request }) =>
  {
    await signIn(page, request)
    await page.goto('/nearby')

    await page.getByTestId('nearby-location').fill('999999999')
    await page.getByTestId('nearby-search').click()

    await expect(page.getByTestId('error')).toBeVisible()
  })
})
