import { expect, test } from '@playwright/test';

import { expectAccessible, expectSettledWithoutError, openAsReturningVisitor } from './e2e.utils';

/**
 * Public pages of the shell, with the title each one sets.
 */
const PAGES = [
  { path: '/overview', title: /^Vue d'ensemble/ },
  { path: '/challenges', title: /^Défis/ },
  { path: '/leaderboard', title: /^Classement/ },
  { path: '/players', title: /^Escouade/ },
  { path: '/rules', title: /^Règles du jeu/ },
];

test('a first visit enters the app from the landing page', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await expectAccessible(page);

  await page.getByRole('button', { name: /Entrer/ }).click();
  await expect(page).not.toHaveURL(/\/$/);
});

for (const { path, title } of PAGES) {
  test(`${path} renders without error and stays accessible`, async ({ page }) => {
    await openAsReturningVisitor(page, path);
    await expect(page).toHaveTitle(title);
    await expectSettledWithoutError(page);
    await expectAccessible(page);
  });
}

test('an unknown address shows the not-found page with a way back', async ({ page }) => {
  await openAsReturningVisitor(page, '/no-such-page');
  const back = page.getByRole('link', { name: "Retour à la vue d'ensemble" });
  await expect(back).toBeVisible();
  await back.click();
  await expect(page).toHaveURL(/\/overview/);
});

test('the backoffice refuses a wrong key', async ({ page }) => {
  await openAsReturningVisitor(page, '/admin/login');
  await expectAccessible(page);

  await page.getByLabel("Clé d'administration").fill('wrong-key-for-the-smoke-test-0000');
  await page.getByRole('button', { name: 'Entrer' }).click();
  await expect(page.getByRole('alert')).toHaveText('Clé refusée par le serveur.');
});

test('the API and the probes answer through nginx', async ({ request }) => {
  expect((await request.get('/healthz')).status()).toBe(200);
  expect((await request.get('/readyz')).status()).toBe(200);
  expect((await request.get('/api/players')).status()).toBe(200);
});
