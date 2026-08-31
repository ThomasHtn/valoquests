import { RemainingTime } from '@core/date/week-period.utils';

/**
 * Active week summary shown across the overview page: its ISO week number, formatted date range
 * and time remaining before rollover.
 *
 * Computed once by `Overview` from a single ticking clock and passed down to `ConfrontationBand`,
 * so the two figures the page reads off it never drift apart.
 */
export interface WeekSummary {
  readonly number: number;
  readonly dateRange: string;
  readonly remaining: RemainingTime;
}
