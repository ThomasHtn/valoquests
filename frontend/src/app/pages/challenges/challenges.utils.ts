import { localMidnight } from '@core/date/date-time.utils';

/**
 * The ISO date `offset` days after another.
 */
export function shiftDay(isoDate: string, offset: number): string {
  const date = localMidnight(isoDate);
  date.setDate(date.getDate() + offset);
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${`${date.getDate()}`.padStart(2, '0')}`;
}
