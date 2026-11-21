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
