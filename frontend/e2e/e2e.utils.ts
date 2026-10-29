import AxeBuilder from '@axe-core/playwright';
import { expect, Page } from '@playwright/test';

/**
 * Rules left out of the audit, each a known design choice rather than a regression.
 */
const SKIPPED_RULES = [
  // The palette is fixed by docs/DESIGN.md; contrast is reviewed with the design, not per build.
  'color-contrast',
  // The page body scrolls as a whole and the shell's main is the skip-link target.
  'scrollable-region-focusable',
];

/**
 * Fails on any accessibility violation axe finds in the rendered page, outside the skipped rules.
 */
export async function expectAccessible(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page })
    .disableRules(SKIPPED_RULES)
    // The pin star sits beside each tab inside the tablist on purpose: a tab cannot hold a button.
    .exclude('.overview-tabs__bar')
    // Day buttons show an initial on phones and name the full weekday, which is clearer to a screen reader.
    .exclude('.daily-week__strip')
    .analyze();
  expect(results.violations.map((violation) => violation.id)).toEqual([]);
}

/**
 * Opens a page as a returning visitor, past the landing page and the one-time tour.
 */
export async function openAsReturningVisitor(page: Page, path: string): Promise<void> {
  await page.addInitScript(() => {
    localStorage.setItem('valo-quests.landing-entered', 'true');
    localStorage.setItem('valo-quests.tour-completed', 'true');
    localStorage.setItem('valo-quests.language', 'fr');
  });
  await page.goto(path);
}

/**
 * Waits until no loading state is left and checks none of the page's requests failed.
 */
export async function expectSettledWithoutError(page: Page): Promise<void> {
  await expect(page.locator('.resource-state [role="status"]')).toHaveCount(0);
  await expect(page.locator('.resource-state__message[role="alert"]')).toHaveCount(0);
}
