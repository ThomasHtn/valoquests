/**
 * Accent colour of the daily challenge.
 */
export const DAILY_TONE = 'var(--color-accent-cyan)';

/**
 * Largest target a phone line is cut into one segment per unit for: past it, a continuous line.
 */
export const MAX_SEGMENTED_TARGET = 12;

/**
 * Width of one operator column on the board, in rem.
 */
export const BOARD_COLUMN_REM = 5.25;

/**
 * Narrowest the board's challenge column gets before operator columns start sliding, in rem.
 */
export const BOARD_LEAD_MIN_REM = 30;

/**
 * Widest the board's challenge column gets when the whole squad fits: past it, the operator
 * columns widen instead, in rem.
 */
export const BOARD_LEAD_MAX_REM = 34;

/**
 * Operator columns shown at once on the board; the arrows turn a page of this many.
 */
export const BOARD_PAGE_SIZE = 7;

/**
 * Pointer travel, in pixels, before a press on the operator columns turns into a drag.
 */
export const BOARD_DRAG_THRESHOLD_PX = 4;

/**
 * Delay between two board rows as their rings close on arrival, in milliseconds.
 */
export const BOARD_ROW_STAGGER_MS = 90;

/**
 * A number in a rule ("3", "25 000"); thousands may be split by any space, the narrow no-break one
 * included.
 */
export const RULE_NUMBER = /\d(?:[\d\s]*\d)?/g;
