import { Component, computed, inject, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { HourSlotPerformance, WeekdayPerformance } from '@core/players/player-progression.model';
import { BarChart } from '@shared/chart/bar-chart';
import { ChartBar } from '@shared/chart/chart.model';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * Matches a slot needs before it can be called a best. Mirrors the backend's own floor, and is
 * shown to the reader so a greyed-out bar explains itself.
 */
const MINIMUM_SAMPLE = 5;

/**
 * When a player wins, by day of the week and by time of day.
 *
 * Bars carry the win rate, not the number of matches: the question this section answers is "when
 * am I good", and a volume chart answers "when do I play" instead. Volume still decides what may
 * be *called* a best — a lone lucky Tuesday morning is not a strength — so slots under the sample
 * floor are drawn recessive and can never win.
 */
@Component({
  selector: 'app-schedule-performance',
  imports: [TranslatePipe, BarChart, Tooltip],
  templateUrl: './schedule-performance.html',
  // Fills the card it sits in, so the reading note at the bottom of the template can be pushed to
  // the bottom of the card rather than to the bottom of the charts.
  host: { class: 'block h-full' },
})
export class SchedulePerformance {
  /**
   * Per-weekday performance, Monday first.
   */
  public readonly weekdays = input.required<readonly WeekdayPerformance[]>();

  /**
   * Per-slot performance, midnight first.
   */
  public readonly hourSlots = input.required<readonly HourSlotPerformance[]>();

  /**
   * i18n service, used for axis labels and tooltip details.
   */
  private readonly translation = inject(Translation);

  /**
   * Sample a slot needs before it can be highlighted, surfaced to the template.
   */
  protected readonly minimumSample = MINIMUM_SAMPLE;

  /**
   * Weekday bars, in display order.
   */
  protected readonly weekdayBars = computed<readonly ChartBar[]>(() =>
    this.weekdays().map((day) => ({
      label: this.translation.translate(`playerProfile.progression.schedule.day.${day.day}`),
      value: Math.round(day.winRate),
      detail: this.sampleLabel(day.matchesPlayed),
      highlighted: day.best,
      muted: day.matchesPlayed < MINIMUM_SAMPLE,
    })),
  );

  /**
   * Time-slot bars, in display order.
   */
  protected readonly hourSlotBars = computed<readonly ChartBar[]>(() =>
    this.hourSlots().map((slot) => ({
      label: `${String(slot.startHour).padStart(2, '0')}h`,
      value: Math.round(slot.winRate),
      detail: this.sampleLabel(slot.matchesPlayed),
      highlighted: slot.best,
      muted: slot.matchesPlayed < MINIMUM_SAMPLE,
    })),
  );

  /**
   * Name of the strongest weekday, or an empty string when none qualifies.
   */
  protected readonly bestWeekday = computed(
    () => this.weekdayBars().find((bar) => bar.highlighted)?.label ?? '',
  );

  /**
   * Label of the strongest time slot, or an empty string when none qualifies.
   */
  protected readonly bestHourSlot = computed(() => {
    const slot = this.hourSlots().find((entry) => entry.best);
    return slot ? this.slotRange(slot.startHour) : '';
  });

  /**
   * Formats a slot's sample for its tooltip.
   *
   * @param matchesPlayed - Matches played in that slot.
   * @returns The already-translated detail line.
   */
  private sampleLabel(matchesPlayed: number): string {
    return this.translation.translate('playerProfile.progression.schedule.sample', {
      count: matchesPlayed,
    });
  }

  /**
   * Spells a slot out as the hours it covers.
   *
   * @param startHour - The slot's first hour.
   * @returns The range, e.g. `21h – 24h`.
   */
  protected slotRange(startHour: number): string {
    return `${String(startHour).padStart(2, '0')}h – ${String(startHour + 3).padStart(2, '0')}h`;
  }
}
