import { MILLISECONDS_PER_DAY } from './date.constants';
import { IsoDateParts } from './date.model';

/**
 * Year, month (1-12) and day of a `YYYY-MM-DD` date.
 */
export function parseIsoDate(isoDate: string): IsoDateParts {
  const [year, month, day] = isoDate.split('-').map(Number);
  return { year, month, day };
}

/**
 * Local midnight of a `YYYY-MM-DD` day, shifted by `plusDays`.
 */
export function localMidnight(isoDate: string, plusDays = 0): Date {
  const { year, month, day } = parseIsoDate(isoDate);
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
 * Shifts a `YYYY-MM-DD` date by whole days, in UTC so the browser's time zone cannot shift it.
 */
export function addDays(isoDate: string, days: number): string {
  const { year, month, day } = parseIsoDate(isoDate);
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}
