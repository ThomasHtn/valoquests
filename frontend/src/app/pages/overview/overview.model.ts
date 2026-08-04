import { RemainingTime } from '@core/date/week-period.utils';

/**
 * Active week summary shown across the overview page's hero: its ISO week number, formatted date
 * range and time remaining before rollover.
 *
 * Computed once by `Overview` from a single ticking clock and passed down to `BossEncounter` and
 * `TeamProgress`, so the countdown never drifts between widgets.
 */
export interface WeekSummary {
  readonly number: number;
  readonly dateRange: string;
  readonly remaining: RemainingTime;
}
