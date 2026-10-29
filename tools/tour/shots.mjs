/**
 * Takes the phone screenshots of the README strip, raw: frame_screens.py draws the phone around them.
 * Usage: node shots.mjs <out dir>
 */
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';

import { APP_URL, PAGE_READY, PHONE_DEVICE } from './tour.constants.mjs';
import { chromium, openContext } from './tour.utils.mjs';

/**
 * Where the raw screenshots land, one PNG per screen.
 */
const OUT_DIR = process.argv[2];

if (!OUT_DIR) throw new Error('Usage: node shots.mjs <out dir>');
mkdirSync(OUT_DIR, { recursive: true });

/**
 * Each screen of the strip: its file name, its address and how far its page body is scrolled.
 */
const SCREENS = [
  { name: 'base', path: '/overview', scroll: 0 },
  { name: 'stocks', path: '/overview', scroll: 860 },
  { name: 'challenges', path: '/challenges', scroll: 0 },
  { name: 'leaderboard', path: '/leaderboard', scroll: 0 },
];

/**
 * One browser for every screen.
 */
const browser = await chromium.launch();

/**
 * A phone, as a returning visitor.
 */
const context = await openContext(browser, { ...PHONE_DEVICE, reducedMotion: 'reduce' });

/**
 * The single tab every screen is taken in.
 */
const page = await context.newPage();

for (const { name, path, scroll } of SCREENS) {
  await page.goto(`${APP_URL}${path}`);
  await page.locator(PAGE_READY).first().waitFor();
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(1500);
  await page.evaluate((top) => document.querySelector('.page-body').scrollTo(0, top), scroll);
  await page.waitForTimeout(800);
  await page.screenshot({ path: join(OUT_DIR, `${name}.png`) });
}

await browser.close();
