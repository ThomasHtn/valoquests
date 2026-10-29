/**
 * The app as `tour.sh` serves it, on a port of its own so `make up` can keep its 8000.
 */
export const APP_URL = 'http://localhost:8100';

/**
 * Size of the tour, the same as the other apps' so their READMEs look alike.
 */
export const STAGE_VIEWPORT = { width: 1200, height: 760 };

/**
 * A common phone, at twice the density so the framed screens stay sharp.
 */
export const PHONE_DEVICE = {
  viewport: { width: 390, height: 844 },
  deviceScaleFactor: 2,
  isMobile: true,
  hasTouch: true,
};

/**
 * Sign that a page of the shell has loaded: its header, once no resource is left loading.
 */
export const PAGE_READY = '.page-body';
