/**
 * Films the tour: drives stage.html around two live copies of the app and screenshots it in a loop.
 * Usage: node record.mjs <out dir>, then encode_tour.py turns <out dir> into the animated WebP.
 */
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

import { APP_URL, PAGE_READY, STAGE_VIEWPORT } from './tour.constants.mjs';
import { chromium, openContext } from './tour.utils.mjs';

/**
 * Where the frames and their timings land.
 */
const OUT_DIR = process.argv[2];

/**
 * Served on the app's origin, so the stage can reach into both iframes.
 */
const STAGE_URL = `${APP_URL}/__tour`;

if (!OUT_DIR) throw new Error('Usage: node record.mjs <out dir>');
mkdirSync(join(OUT_DIR, 'frames'), { recursive: true });

/**
 * One browser for the whole take.
 */
const browser = await chromium.launch();

/**
 * Context at the size of the final video, as a returning visitor.
 */
const context = await openContext(browser, { viewport: STAGE_VIEWPORT });
await context.route(STAGE_URL, (route) =>
  route.fulfill({
    contentType: 'text/html',
    body: readFileSync(new URL('./stage.html', import.meta.url), 'utf8'),
  }),
);

/* The app forbids being framed (CSP frame-ancestors, X-Frame-Options); only this copy lifts it, for the two iframes. */
await context.route(`${APP_URL}/**`, async (route) => {
  const request = route.request();
  if (request.resourceType() !== 'document' || request.url() === STAGE_URL) {
    return route.fallback();
  }
  const response = await route.fetch();
  const headers = { ...response.headers() };
  delete headers['content-security-policy'];
  delete headers['x-frame-options'];
  return route.fulfill({ response, headers });
});

/**
 * The stage, filmed as a whole.
 */
const page = await context.newPage();
await page.goto(STAGE_URL);
await page.evaluate(() => document.fonts.ready);

/**
 * The app in the browser window.
 */
const desktop = page.frameLocator('#desktop-frame');

/**
 * The app in the phone.
 */
const phone = page.frameLocator('#phone-frame');

/**
 * Points one device at an address of the app and waits until its page has settled.
 */
async function open(device, path, locator) {
  await page.evaluate(([id, url]) => (document.getElementById(id).src = url), [
    `${device}-frame`,
    `${APP_URL}${path}`,
  ]);
  await (device === 'desktop' ? desktop : phone).locator(locator).first().waitFor();
}

/* Both apps load before the first frame, so no loading screen is ever filmed. */
await open('desktop', '/overview', '.overview__report-button');
await open('phone', '/challenges', PAGE_READY);
await page.waitForTimeout(1500);

/**
 * Screenshots with the time each was taken, so playback keeps the real pace.
 */
const frames = [];

/**
 * Cleared once the last scene is over.
 */
let recording = true;

/**
 * Screenshots as fast as the browser allows, alongside the scenes below.
 */
const capture = (async () => {
  while (recording) {
    frames.push({ buffer: await page.screenshot({ type: 'png' }), at: Date.now() });
  }
})();

/**
 * Lets a scene breathe.
 */
const wait = (ms) => page.waitForTimeout(ms);

/**
 * Calls one of the stage's window.tour methods.
 */
const stage = (method, ...args) =>
  page.evaluate(([name, values]) => window.tour[name](...values), [method, args]);

/* 1. The pitch. */
await stage('show', 'intro');
await wait(4200);

/* 2. The base, grown by every match the squad played. */
await stage('caption', 'The base', 'Every match <em>grows the base.</em>', '');
await stage('layout', 'desktop');
await stage('show', 'scene');
await wait(3800);

/* 3. This week's guardian, and the countdown to Sunday. */
await stage('caption', 'The guardian', 'One boss a week. <em>Break it by Sunday.</em>', '');
await stage('scroll', 'desktop', 440);
await wait(3800);

/* 4. Food and components, what the matches bring back. */
await stage('caption', 'The stocks', 'Food feeds them. <em>Components get them out.</em>', '');
await stage('scroll', 'desktop', 700);
await wait(3800);

/* 5. Sunday's mission report. */
await stage('caption', 'Every Monday', 'The mission <em>report.</em>', '');
await stage('scroll', 'desktop', 0);
await wait(1200);
await stage('tap', 'desktop', '.overview__report-button', 0);
await wait(4200);

/* 6. The challenges, on a phone; the desktop meanwhile opens a player out of sight. */
await stage(
  'caption',
  'Challenges',
  'One a day.<br><em>Three a week.</em>',
  'The whole squad on the same card, filled in as the matches come in.',
);
await stage('layout', 'phone');
await open('desktop', '/players/1?view=progress', 'canvas');
await wait(3000);
await stage('scroll', 'phone', 300);
await wait(3000);

/* 7. The leaderboard, reached from the menu like a visitor would. */
await stage(
  'caption',
  'Leaderboard',
  'Titles and a podium.<br><em>Every week.</em>',
  'A champion and four titles: the week decides, Monday starts over.',
);
await stage('tap', 'phone', '.page-header__menu-button', 0);
await wait(1200);
await stage('tap', 'phone', 'a.sidebar__nav-item[href="/leaderboard"]', 0);
await phone.locator(PAGE_READY).first().waitFor();
await wait(4200);

/* 8. A player's own story. */
await stage('caption', 'The squad', 'Every player. <em>Every match.</em>', '');
await stage('layout', 'desktop');
await wait(4200);

/* 9. The way out. */
await stage('layout', 'none');
await stage('show', 'outro');
await wait(4600);

recording = false;
await capture;
await browser.close();

/**
 * How long each frame stays on screen: until the next one was taken.
 */
const timings = frames.map(({ at }, index) => (frames[index + 1]?.at ?? at + 70) - at);
frames.forEach(({ buffer }, index) =>
  writeFileSync(join(OUT_DIR, 'frames', `${String(index).padStart(5, '0')}.png`), buffer),
);
writeFileSync(join(OUT_DIR, 'timings.json'), JSON.stringify(timings));
console.log(`${frames.length} frames, ${(timings.reduce((a, b) => a + b, 0) / 1000).toFixed(1)} s`);
