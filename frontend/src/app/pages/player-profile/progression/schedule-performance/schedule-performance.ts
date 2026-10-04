import { Component, computed, inject, input } from '@angular/core';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { formatPercent } from '@core/i18n/format/number-format.utils';
import { Translation } from '@core/i18n/translation';
import {
  HourSlotPerformance,
  WeekdayPerformance,
} from '@core/players/progression/player-progression.model';
import { BarChart } from '@shared/chart/bar-chart/bar-chart';
import { ChartBar } from '@shared/chart/chart.model';
import { Tooltip } from '@shared/tooltip/tooltip';
import { MINIMUM_SAMPLE } from './schedule-performance.constants';

/**
 * Win rate by weekday and time of day ("when am I good", not "when do I play").
 * Slots under the sample floor are drawn recessive and can never be the best.
 */
@Component({
  selector: 'app-schedule-performance',
  imports: [TranslatePipe, BarChart, Tooltip],
  templateUrl: './schedule-performance.html',
  // Fills its card so the reading note can sink to the card's bottom.
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
   * Translates axis labels and tooltip details.
   */
  private readonly translation = inject(Translation);

  /**
   * Sample a slot needs before it can be highlighted.
   */
  protected readonly minimumSample = MINIMUM_SAMPLE;

  /**
   * Weekday chart as text for screen readers.
   */
  protected readonly weekdaySummary = computed(() => this.describe(this.weekdayBars()));

  /**
   * Time-slot chart as text for screen readers.
   */
  protected readonly hourSlotSummary = computed(() => this.describe(this.hourSlotBars()));

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
   * Strongest weekday name, empty when none qualifies.
   */
  protected readonly bestWeekday = computed(() => {
    const best = this.weekdays().find((day) => day.best);
    return best
      ? this.translation.translate(`playerProfile.progression.schedule.dayLong.${best.day}`)
      : '';
  });

  /**
   * Strongest time slot label, empty when none qualifies.
   */
  protected readonly bestHourSlot = computed(() => {
    const slot = this.hourSlots().find((entry) => entry.best);
    return slot ? this.slotRange(slot.startHour) : '';
  });

  /**
   * Formats a tooltip win rate in the reader's notation.
   */
  protected readonly formatRate = (value: number): string =>
    formatPercent(value, this.translation.language());

  /**
   * Translated sample line of a slot's tooltip.
   */
  private sampleLabel(matchesPlayed: number): string {
    return this.translation.translate('playerProfile.progression.schedule.sample', {
      count: matchesPlayed,
    });
  }

  /**
   * Hours a slot covers, e.g. `18h – 21h`, the last one ending at midnight.
   */
  protected slotRange(startHour: number): string {
    const end = startHour + 3;
    const endLabel =
      end === 24
        ? this.translation.translate('playerProfile.progression.schedule.midnight')
        : `${String(end).padStart(2, '0')}h`;
    return `${String(startHour).padStart(2, '0')}h – ${endLabel}`;
  }

  /**
   * Each bar as `label rate (sample)`, joined into one sentence.
   */
  private describe(bars: readonly ChartBar[]): string {
    return bars
      .map((bar) => `${bar.label} ${this.formatRate(bar.value)} (${bar.detail})`)
      .join('; ');
  }
}
