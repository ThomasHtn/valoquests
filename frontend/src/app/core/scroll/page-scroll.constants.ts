/**
 * Selector of the routed page's scroll container (see `page-body` in `styles.css`).
 */
export const PAGE_BODY_SELECTOR = '.page-body';

/**
 * How far down, in viewport heights, the reader must be before the way back to the top shows.
 */
export const SCROLL_TOP_THRESHOLD_SCREENS = 1.2;

/**
 * Frames spent waiting for a page to grow tall enough to restore its scroll offset.
 */
export const SCROLL_RESTORE_MAX_FRAMES = 30;

/**
 * Delay between two attempts at restoring a scroll offset, about one frame.
 */
export const SCROLL_RESTORE_STEP_MS = 16;
