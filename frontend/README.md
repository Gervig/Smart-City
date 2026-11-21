# Smart City frontend (demo + E2E tests)

Vite + React + JavaScript. Small demo UI for the Smart City API and the Playwright E2E tests for the exam.

## Run
1. Start the backend (port 7070) with the database running.
2. In this folder:
   ```
   npm install
   npx playwright install chromium     # first time only
   npm run dev                         # http://localhost:5173
   ```
The browser calls `/backend/...` on the Vite server, which proxies to `http://localhost:7070` (see `vite.config.js`). This avoids CORS, so the backend needs no changes. Use `VITE_API_TARGET` to point somewhere else.

## E2E tests (Playwright, real UI + real backend)
```
npm run e2e          # headless, starts the dev server automatically
npm run e2e:headed   # watch the browser
npm run e2e:ui       # Playwright UI mode
npm run e2e:report   # open the HTML report
```
- `e2e/auth.spec.js`: register, login, wrong password, unknown user, empty form, logout, protected-route redirect, corrupted token.
- `e2e/crud.spec.js`: family create/edit/delete, location + housing, citizen create, validation errors (capacity 0, future birth date, alive with death date, missing name), nearby places (Overpass stubbed with `page.route`, plus unknown location).
- Tests create their own users with unique names, so they can run repeatedly against the same database.
- Selectors use `data-testid` and roles.

## Layout
`src/resources.js` describes each CRUD resource (fields, types); `src/CrudPage.jsx` renders table and form from it. Add a resource there to get a new page.
