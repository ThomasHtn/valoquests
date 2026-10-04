import { MILLISECONDS_PER_DAY } from './date.constants';

/**
 * Local midnight of a `YYYY-MM-DD` day, shifted by `plusDays`.
 */
export function localMidnight(isoDate: string, plusDays = 0): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day + plusDays);
}

/**
 * Signed whole days between two `YYYY-MM-DD` days; rounded so a DST hour cannot tip it.
 */
export function daysBetween(from: string, to: string): number {
  return Math.round(
    (localMidnight(to).getTime() - localMidnight(from).getTime()) / MILLISECONDS_PER_DAY,
  );
}

/**
 * UTC midnight of a `YYYY-MM-DD` date, whatever the browser's time zone.
 */
function parseIsoDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

/**
 * Shifts a `YYYY-MM-DD` date by whole days.
 */
export function addDays(isoDate: string, days: number): string {
  const shifted = new Date(parseIsoDate(isoDate).getTime() + days * MILLISECONDS_PER_DAY);
  return shifted.toISOString().slice(0, 10);
}
