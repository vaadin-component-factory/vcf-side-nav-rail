import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  // First run from a clean build triggers Vaadin's dev-bundle compile on the
  // very first page load, which can take 30–60 s. The per-test timeout and
  // the default navigation/action timeouts are generous enough to cover that.
  timeout: 120_000,
  retries: 1,
  expect: {
    // Covers the first-paint wait on a freshly compiled dev bundle.
    timeout: 30_000,
  },
  use: {
    baseURL: 'http://localhost:8081',
    trace: 'on-first-retry',
    navigationTimeout: 60_000,
    actionTimeout: 15_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
