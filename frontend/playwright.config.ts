import { defineConfig, devices } from '@playwright/test';

/**
 * On CI an HTML report is kept and a stray `test.only` fails the run.
 */
const ci = !!process.env['CI'];

/**
 * Smoke tests against the whole app from its images: `make reset up`, then `npm run e2e`, or the
 * delivery workflow. They expect an empty squad, so they never depend on the Henrik API.
 */
export default defineConfig({
  testDir: 'e2e',
  testMatch: '**/*.e2e.ts',
  forbidOnly: ci,
  retries: ci ? 1 : 0,
  expect: { timeout: 15_000 },
  reporter: ci ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:8000',
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    reducedMotion: 'reduce',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
    {
      name: 'mobile',
      use: { ...devices['Pixel 7'] },
    },
  ],
});
