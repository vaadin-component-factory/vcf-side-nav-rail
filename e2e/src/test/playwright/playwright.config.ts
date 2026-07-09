import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  // The e2e module runs in PRODUCTION mode — the frontend bundle is prebuilt at
  // Maven `build-frontend` time, so there is no dev-bundle compile on first page
  // load. Passing tests settle in well under a second; these timeouts only bound
  // how long a *failing* assertion waits before giving up, so keeping them tight
  // makes real failures surface in seconds instead of half a minute (× retries).
  // The one genuinely slow moment is the very first navigation against a cold
  // JVM, which navigationTimeout covers with margin.
  timeout: 30_000,
  retries: 1,
  expect: {
    timeout: 5_000,
  },
  use: {
    baseURL: 'http://localhost:8081',
    trace: 'on-first-retry',
    navigationTimeout: 20_000,
    actionTimeout: 5_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
