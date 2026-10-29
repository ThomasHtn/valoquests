import { createRequire } from 'node:module';

import { APP_URL } from './tour.constants.mjs';

/**
 * Playwright comes from the frontend workspace, which already pins it for the e2e tests.
 */
export const { chromium } = createRequire(new URL('../../frontend/package.json', import.meta.url))(
  'playwright',
);

/**
 * Monday of the last settled week, whose mission report the visitor has already closed.
 */
async function lastSettledWeek() {
  const campaign = await (await fetch(`${APP_URL}/api/campaign`)).json();
  return campaign.weeks.filter((week) => week.settled).at(-1)?.weekStart ?? '';
}

/**
 * Opens a browser context in French for a returning visitor: past the landing page, the one-time
 * in-app tour and the latest mission report, so every address opens the page itself.
 */
export async function openContext(browser, options) {
  const context = await browser.newContext({
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    ...options,
  });
  await context.addInitScript((seenReport) => {
    localStorage.setItem('valo-quests.landing-entered', 'true');
    localStorage.setItem('valo-quests.tour-completed', 'true');
    localStorage.setItem('valo-quests.language', 'fr');
    localStorage.setItem('valoquests.missionReport.seen', seenReport);
  }, await lastSettledWeek());
  return context;
}
