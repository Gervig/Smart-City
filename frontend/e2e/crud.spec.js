import {expect, test} from '@playwright/test'
import {assertResponseStatus, fillForm, signIn, uniqueName} from './helpers.js'

function headerCell(button) {
    return button.locator('xpath=..')
}

async function waitForTable(page, testId) {
    const table = page.getByTestId(testId)
    const error = page.getByTestId('error')
    await table.or(error).waitFor({state: 'visible'})
    if (await error.isVisible()) {
        throw new Error(`Loading ${testId} failed: ${await error.innerText()}`)
    }
}

test.describe('CRUD pages', () => {
    test.beforeEach(async ({page, request}) => {
        await signIn(page, request)
    })

    test('create, edit and delete a family', async ({page}) => {
        const name = uniqueName('Family')
        const renamed = `${name}_renamed`
        await page.goto('/families')

        await fillForm(page, {familyName: name})
        await expect(page.getByTestId('notice')).toHaveText('Created.')
        await page.getByTestId('filter-familyName').fill(name)
        await page.getByRole('button', {name: 'Apply filters'}).click()
        const row = page.getByTestId('row').filter({hasText: name})
        await expect(row).toBeVisible()

        await row.getByTestId('edit').click()
        await fillForm(page, {familyName: renamed})
        await expect(page.getByTestId('notice')).toHaveText('Updated.')
        await page.getByTestId('filter-familyName').fill(renamed)
        await page.getByRole('button', {name: 'Apply filters'}).click()
        const renamedRow = page.getByTestId('row').filter({hasText: renamed})
        await expect(renamedRow).toBeVisible()
        await expect(page.getByTestId('row').filter({hasText: `${name}`}).filter({hasNotText: 'renamed'})).toHaveCount(0)

        await renamedRow.getByTestId('delete').click()
        await expect(page.getByTestId('notice')).toHaveText('Deleted.')
        await expect(page.getByTestId('row').filter({hasText: renamed})).toHaveCount(0)
    })

    test('a family without a name is rejected', async ({page}) => {
        await page.goto('/families')

        await page.getByTestId('submit').click()

        await expect(page.getByTestId('error')).toContainText('required')
    })

    test('sorting and filters reset when navigating between resources', async ({page}) => {
        const listRequests = []
        page.on('request', (request) => {
            if (request.url().includes('/backend/')) listRequests.push(request.url())
        })

        await page.goto('/families')
        await waitForTable(page, 'families-table')
        const familyIdButton = page.getByRole('button', {name: /^ID/})
        await expect(headerCell(familyIdButton)).toHaveAttribute('aria-sort', 'ascending')
        const familyNameButton = page.getByRole('button', {name: /Family name/})
        const familyNameColumn = headerCell(familyNameButton)
        await familyNameButton.click()
        await expect(familyNameColumn).toHaveAttribute('aria-sort', 'ascending')

        const familyFilterRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/families'
            && new URL(request.url()).searchParams.has('familyName'))
        await page.getByTestId('filter-familyName').fill('state-reset-test')
        await page.getByRole('button', {name: 'Apply filters'}).click()
        await familyFilterRequest

        const citizenRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens')
        await page.getByRole('link', {name: 'Citizens'}).click()
        const citizenUrl = new URL((await citizenRequest).url())
        expect(citizenUrl.searchParams.get('sort')).toBe('id,asc')
        expect(citizenUrl.searchParams.has('familyName')).toBe(false)
        await waitForTable(page, 'citizens-table')
        const citizenIdButton = page.getByRole('button', {name: /^ID/})
        const citizenIdColumn = headerCell(citizenIdButton)
        await expect(citizenIdColumn).toHaveAttribute('aria-sort', 'ascending')
        const lastNameButton = page.getByRole('button', {name: /Last name/})
        const lastNameColumn = headerCell(lastNameButton)
        await expect(lastNameColumn).toHaveAttribute('aria-sort', 'none')

        const lastNameRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens'
            && new URL(request.url()).searchParams.get('sort') === 'lastName,asc')
        await lastNameButton.click()
        await lastNameRequest

        const locationRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/locations')
        await page.getByRole('link', {name: 'Locations'}).click()
        const locationUrl = new URL((await locationRequest).url())
        expect(locationUrl.searchParams.get('sort')).toBe('id,asc')
        expect(locationUrl.searchParams.has('familyName')).toBe(false)
        expect(locationUrl.searchParams.has('lastName')).toBe(false)

        await waitForTable(page, 'locations-table')
        const locationIdButton = page.getByRole('button', {name: /^ID/})
        await expect(headerCell(locationIdButton)).toHaveAttribute('aria-sort', 'ascending')
        const streetAddressButton = page.getByRole('button', {name: /Street address/})
        const streetAddressColumn = headerCell(streetAddressButton)
        const streetAddressRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/locations'
            && new URL(request.url()).searchParams.get('sort') === 'streetAddress,asc')
        await streetAddressButton.click()
        await streetAddressRequest

        const housingRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/housings')
        await page.getByRole('link', {name: 'Housings'}).click()
        const housingUrl = new URL((await housingRequest).url())
        expect(housingUrl.searchParams.get('sort')).toBe('id,asc')
        expect(housingUrl.searchParams.has('familyName')).toBe(false)
        expect(housingUrl.searchParams.has('lastName')).toBe(false)
        expect(housingUrl.searchParams.has('streetAddress')).toBe(false)
        await waitForTable(page, 'housings-table')
        const housingIdButton = page.getByRole('button', {name: /^ID/})
        await expect(headerCell(housingIdButton)).toHaveAttribute('aria-sort', 'ascending')
        expect(listRequests.some((url) => new URL(url).searchParams.get('sort') === 'familyName,asc')).toBe(true)
    })

    test('a delayed response from the previous resource cannot replace current rows', async ({page}) => {
        let releaseFamilyResponse
        let familyRequestStarted
        const familyRequestObserved = new Promise((resolve) => {
            familyRequestStarted = resolve
        })
        const familyResponseGate = new Promise((resolve) => {
            releaseFamilyResponse = resolve
        })
        await page.route('**/backend/families**', async (route) => {
            if (route.request().method() !== 'GET' || new URL(route.request().url()).pathname !== '/backend/families') {
                await route.continue()
                return
            }

            familyRequestStarted()
            await familyResponseGate
            await route.fulfill({
                json: {
                    data: [{id: 987654321, familyName: 'Stale family response'}],
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
        await page.getByRole('link', {name: 'Citizens'}).click()
        await citizenRequest
        await expect(page.getByTestId('citizens-table')).toBeVisible()

        const familyResponse = page.waitForResponse((response) =>
            new URL(response.url()).pathname === '/backend/families')
        releaseFamilyResponse()
        await familyResponse
        await expect(page.getByText('Stale family response')).toHaveCount(0)
        await expect(page.getByRole('heading', {name: 'Citizens'})).toBeVisible()
    })

    test('create a location and a housing that points to it', async ({page}) => {
        const place = uniqueName('Place')
        await page.goto('/locations')
        await fillForm(page, {name: place, streetAddress: 'Testvej 1'})
        await expect(page.getByTestId('notice')).toHaveText('Created.')
        await page.getByTestId('filter-name').fill(place)
        await page.getByRole('button', {name: 'Apply filters'}).click()
        const locationRow = page.getByTestId('row').filter({hasText: place})
        await expect(locationRow).toBeVisible()
        const locationId = await locationRow.locator('td').first().innerText()

        await page.getByRole('link', {name: 'Housings'}).click()
        await fillForm(page, {locationId, housingType: 'apartment', capacity: 4})
        await expect(page.getByTestId('notice')).toHaveText('Created.')
        await page.getByTestId('filter-locationId').fill(locationId)
        await page.getByTestId('filter-housingType').selectOption('apartment')
        await page.getByRole('button', {name: 'Apply filters'}).click()

        await expect(page.getByTestId('row').filter({hasText: 'apartment'}).filter({hasText: locationId}).first()).toBeVisible()
    })

    test('a housing with capacity 0 is rejected', async ({page}) => {
        await page.goto('/housings')

        await fillForm(page, {housingType: 'house', capacity: 0}, {expected: 'error'})

        await expect(page.getByTestId('error')).toContainText('capacity must be between 1 and 10000')
    })

    test('a citizen can be created and linked to a family', async ({page}) => {
        const family = uniqueName('CitFam')
        await page.goto('/families')
        await fillForm(page, {familyName: family})
        await page.getByTestId('filter-familyName').fill(family)
        await page.getByRole('button', {name: 'Apply filters'}).click()
        const familyId = await page.getByTestId('row').filter({hasText: family}).locator('td').first().innerText()

        await page.getByRole('link', {name: 'Citizens'}).click()
        const first = uniqueName('Anna')
        await fillForm(page, {
            firstName: first,
            lastName: 'Hansen',
            birthDate: '1990-05-17',
            status: 'alive',
            gender: 'female',
            familyId
        })
        await expect(page.getByTestId('notice')).toHaveText('Created.')
        await page.getByTestId('filter-familyId').fill(familyId)
        await page.getByRole('button', {name: 'Apply filters'}).click()

        const row = page.getByTestId('row').filter({hasText: first})
        await expect(row).toBeVisible()
        await expect(row).toContainText('1990-05-17')
        await expect(row).toContainText(familyId)
    })

    test('citizen list paginates, filters and toggles the active sort direction', async ({page, request}) => {
        const {token} = await signIn(page, request)
        const pageResponse = await request.get('/backend/citizens?page=0&size=1&sort=id,desc', {
            headers: {Authorization: `Bearer ${token}`},
        })
        await assertResponseStatus(pageResponse, 200, 'Paginated citizen GET')
        const pageResult = await pageResponse.json()
        expect(pageResult).toMatchObject({page: 0, size: 1})
        expect(Array.isArray(pageResult.data)).toBe(true)
        expect(typeof pageResult.totalElements).toBe('number')
        expect(typeof pageResult.totalPages).toBe('number')

        const invalidSort = await request.get('/backend/citizens?sort=unsupported,asc', {
            headers: {Authorization: `Bearer ${token}`},
        })
        await assertResponseStatus(invalidSort, 400, 'Invalid sort GET')

        const invalidFilter = await request.get('/backend/citizens?unknownFilter=value', {
            headers: {Authorization: `Bearer ${token}`},
        })
        await assertResponseStatus(invalidFilter, 400, 'Invalid filter GET')

        const familyName = uniqueName('FilterFam')
        await page.goto('/families')
        await fillForm(page, {familyName})
        await page.getByTestId('filter-familyName').fill(familyName)
        await page.getByRole('button', {name: 'Apply filters'}).click()
        const familyId = await page.getByTestId('row').filter({hasText: familyName}).locator('td').first().innerText()

        const lastName = uniqueName('FilterLast')
        await page.goto('/citizens')
        const firstName = uniqueName('FilterFirst')
        await fillForm(page, {
            firstName,
            lastName,
            birthDate: '1990-05-17',
            status: 'alive',
            gender: 'female',
            familyId,
        })

        await expect(page.getByTestId('notice')).toHaveText('Created.')

        // Add enough matching records to span the UI's fixed 20-row page size.
        for (let index = 0; index < 20; index += 1) {
            const response = await request.post('/backend/citizens', {
                headers: {Authorization: `Bearer ${token}`},
                data: {
                    firstName: uniqueName('PageCitizen'),
                    lastName,
                    birthDate: '1990-05-17',
                    deathDate: null,
                    status: 'alive',
                    gender: 'female',
                    familyId,
                    housingId: null,
                },
            })
            await assertResponseStatus(response, 201, `Create pagination record ${index + 1}`)
        }

        const ascendingPageResponse = await request.get('/backend/citizens?page=0&size=1&sort=id,asc', {
            headers: {Authorization: `Bearer ${token}`},
        })
        await assertResponseStatus(ascendingPageResponse, 200, 'Ascending citizen GET')
        const ascendingPage = await ascendingPageResponse.json()
        const newestPageResponse = await request.get('/backend/citizens?page=0&size=1&sort=id,desc', {
            headers: {Authorization: `Bearer ${token}`},
        })
        await assertResponseStatus(newestPageResponse, 200, 'Descending citizen GET')
        const newestPage = await newestPageResponse.json()
        expect(ascendingPage.data[0].id).toBeLessThan(newestPage.data[0].id)

        const idButton = page.getByRole('button', {name: /^ID/})
        const descendingIdRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens'
            && new URL(request.url()).searchParams.get('sort') === 'id,desc')
        await idButton.click()
        await descendingIdRequest
        await expect(headerCell(idButton)).toHaveAttribute('aria-sort', 'descending')

        const unfilteredNextPageRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens'
            && new URL(request.url()).searchParams.get('page') === '1')
        await page.getByRole('button', {name: 'Next'}).click()
        await unfilteredNextPageRequest
        await expect(page.locator('.pagination span')).toContainText(/^Page 2 of /)

        const filteredRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens'
            && new URL(request.url()).searchParams.get('lastName') === lastName
            && new URL(request.url()).searchParams.get('page') === '0')
        await page.getByTestId('filter-lastName').fill(lastName)
        await page.getByRole('button', {name: 'Apply filters'}).click()
        await filteredRequest
        await expect(page.getByTestId('row').filter({hasText: lastName}).first()).toBeVisible()
        await expect(page.locator('.pagination span')).toHaveText('Page 1 of 2 (20 records displayed out of 21 total)')

        const filteredNextPageRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens'
            && new URL(request.url()).searchParams.get('lastName') === lastName
            && new URL(request.url()).searchParams.get('page') === '1')
        await page.getByRole('button', {name: 'Next'}).click()
        await filteredNextPageRequest
        await expect(page.locator('.pagination span')).toHaveText('Page 2 of 2 (1 records displayed out of 21 total)')
        await expect(page.getByTestId('row').filter({hasText: firstName})).toBeVisible()

        const lastNameButton = page.getByRole('button', {name: /Last name/})
        const lastNameColumn = headerCell(lastNameButton)
        const ascendingLastNameRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens'
            && new URL(request.url()).searchParams.get('sort') === 'lastName,asc')
        await lastNameButton.click()
        await ascendingLastNameRequest
        await expect(lastNameColumn).toHaveAttribute('aria-sort', 'ascending')

        const descendingLastNameRequest = page.waitForRequest((request) =>
            new URL(request.url()).pathname === '/backend/citizens'
            && new URL(request.url()).searchParams.get('sort') === 'lastName,desc')
        await lastNameButton.click()
        await descendingLastNameRequest
        await expect(lastNameColumn).toHaveAttribute('aria-sort', 'descending')
    })

    test('a citizen born in the future is rejected', async ({page}) => {
        await page.goto('/citizens')

        await fillForm(page, {
            firstName: 'Future',
            lastName: 'Person',
            birthDate: '2999-01-01',
            status: 'alive'
        }, {expected: 'error'})

        await expect(page.getByTestId('error')).toContainText('birthDate must not be in the future')
    })

    test('an alive citizen with a death date is rejected', async ({page}) => {
        await page.goto('/citizens')

        await fillForm(page, {
            firstName: 'Zombie',
            lastName: 'Person',
            birthDate: '1980-01-01',
            deathDate: '2020-01-01',
            status: 'alive'
        }, {expected: 'error'})

        await expect(page.getByTestId('error')).toContainText('deathDate must be null when status is alive')
    })

    test('a citizen with a missing first name is rejected', async ({page}) => {
        await page.goto('/citizens')

        await fillForm(page, {lastName: 'Nameless', birthDate: '1980-01-01', status: 'alive'}, {expected: 'error'})

        await expect(page.getByTestId('error')).toContainText('firstName is required')
    })
})

test.describe('Nearby places', () => {
    test('shows the places returned by the API (Overpass stubbed)', async ({page, request}) => {
        await signIn(page, request)
        await page.route('**/backend/locations/*/nearby**', (route) => route.fulfill({
            json: {
                locationId: 1,
                enriched: false,
                message: null,
                places: [{name: 'Rigshospitalet', type: 'hospital', latitude: 55.69, longitude: 12.57}],
            },
        }))
        await page.goto('/nearby')

        await page.getByTestId('nearby-location').fill('1')
        await page.getByTestId('nearby-search').click()

        await expect(page.getByTestId('nearby-place')).toHaveText(/Rigshospitalet/)
    })

    test('an unknown location shows an error from the API', async ({page, request}) => {
        await signIn(page, request)
        await page.goto('/nearby')

        await page.getByTestId('nearby-location').fill('999999999')
        await page.getByTestId('nearby-search').click()

        await expect(page.getByTestId('error')).toBeVisible()
    })
})
