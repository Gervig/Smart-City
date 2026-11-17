import { defineConfig, devices } from '@playwright/test'

// The tests drive the real UI against the real backend (port 7070) and database.
// Start the Spring-free Javalin backend first, then run: npm run e2e
// Playwright starts the Vite dev server itself (or reuses one that is already running).
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 7_000 },
  workers: 1, // one shared database: keep the tests in a predictable order
  fullyParallel: false,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
