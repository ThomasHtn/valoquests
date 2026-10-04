import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { LucideChevronDown, LucideHourglass } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { Translation } from '@core/i18n/translation';
import {
  FallChart,
  FallHover,
  FallPointer,
  FallReading,
  GuardianFall,
} from './fall-forecast.model';
import {
  capitalizeFirst,
  fallTimeAt,
  fallX,
  fallY,
  formatCampaignTime,
  layoutFallChart,
  readFallAt,
  splitSpan,
  toNearestHour,
} from './fall-forecast.utils';

/**
 * When the guardian falls: one answer line, with a foldable chart projecting Monday's pace.
 */
@Component({
  selector: 'app-fall-forecast',
  imports: [LucideChevronDown, LucideHourglass],
  templateUrl: './fall-forecast.html',
  styleUrl: './fall-forecast.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FallForecast {
  /**
   * The guardian's descent over the week.
   */
  public readonly fall = input.required<GuardianFall>();

  /**
   * Whether the chart is unfolded; folded on arrival since the line already answers.
   */
  protected readonly open = signal(false);

  /**
   * The line's heading.
   */
  protected readonly heading = computed(() =>
    this.translation.translate(
      this.fall().outcome === 'down'
        ? 'overview.report.forecast.keyDown'
        : 'overview.report.forecast.key',
    ),
  );

  /**
   * When the guardian falls, or that it does not in time.
   */
  protected readonly verdict = computed(() => {
    const fall = this.fall();
    if (fall.outcome === 'short' || fall.fallAt === null) {
      return this.translation.translate('overview.report.forecast.short');
    }
    if (fall.outcome === 'down') {
      return this.translation.translate('overview.report.forecast.down', {
        weekday: this.at(fall.fallAt, { weekday: 'long' }),
        time: this.at(fall.fallAt, { hour: '2-digit', minute: '2-digit' }),
      });
    }
    const hour = toNearestHour(fall.fallAt);
    return capitalizeFirst(
      this.translation.translate('overview.report.forecast.ahead', {
        weekday: this.at(hour, { weekday: 'long' }),
        hour: this.at(hour, { hour: 'numeric' }),
      }),
    );
  });

  /**
   * Time left before the extraction, or hit points still standing at the deadline.
   */
  protected readonly margin = computed(() => {
    const fall = this.fall();
    if (fall.fallAt === null) {
      return this.translation.translate('overview.report.forecast.shortMargin', {
        hp: this.hitPoints(Math.round(fall.leftAtDeadline / 100) * 100),
      });
    }
    return this.translation.translate(
      fall.outcome === 'down'
        ? 'overview.report.forecast.downMargin'
        : 'overview.report.forecast.aheadMargin',
      { span: this.span(fall.deadline - fall.fallAt) },
    );
  });

  /**
   * Chart laid out for the panel's width, `null` until measured.
   */
  protected readonly chart = computed<FallChart | null>(() =>
    this.width() > 0 ? layoutFallChart(this.fall(), this.width()) : null,
  );

  /**
   * Label over the fall's dashed post.
   */
  protected readonly fallLabel = computed(() => {
    const fall = this.fall();
    if (fall.fallAt === null) {
      return '';
    }
    if (fall.outcome === 'down') {
      return this.at(fall.fallAt, { weekday: 'short', hour: '2-digit', minute: '2-digit' });
    }
    const hour = toNearestHour(fall.fallAt);
    return this.translation.translate('overview.report.forecast.fallMark', {
      weekday: this.at(hour, { weekday: 'short' }),
      hour: this.at(hour, { hour: 'numeric' }),
    });
  });

  /**
   * Hit points left now, written over the pin.
   */
  protected readonly nowValue = computed(() => this.hitPoints(this.lastReading().left));

  /**
   * Hit points left at Sunday midnight when the pace falls short.
   */
  protected readonly endValue = computed(() =>
    this.translation.translate('overview.report.forecast.leftAtDeadline', {
      hp: this.hitPoints(Math.round(this.fall().leftAtDeadline / 100) * 100),
    }),
  );

  /**
   * The three ticks: Monday, now, Sunday midnight.
   */
  protected readonly ticks = computed(() => ({
    start: this.at(this.fall().weekStart, { weekday: 'short' }),
    now: this.at(this.lastReading().time, { weekday: 'short', hour: '2-digit', minute: '2-digit' }),
    end: this.translation.translate('overview.report.forecast.deadline'),
  }));

  /**
   * The chart in one sentence, for screen readers.
   */
  protected readonly summary = computed(() => {
    const fall = this.fall();
    if (fall.outcome === 'down' && fall.fallAt !== null) {
      return this.translation.translate('overview.report.forecast.ariaDown', {
        weekday: this.at(fall.fallAt, { weekday: 'long' }),
        time: this.at(fall.fallAt, { hour: '2-digit', minute: '2-digit' }),
      });
    }
    if (fall.fallAt === null) {
      return this.translation.translate('overview.report.forecast.ariaShort', {
        hp: this.nowValue(),
        left: this.hitPoints(Math.round(fall.leftAtDeadline / 100) * 100),
      });
    }
    const hour = toNearestHour(fall.fallAt);
    return this.translation.translate('overview.report.forecast.ariaAhead', {
      hp: this.nowValue(),
      weekday: this.at(hour, { weekday: 'long' }),
      hour: this.at(hour, { hour: 'numeric' }),
    });
  });

  /**
   * What the pointer reads, `null` while it is away.
   */
  protected readonly hover = computed<FallHover | null>(() => {
    const chart = this.chart();
    const pointer = this.pointer();
    if (!chart || pointer === null) {
      return null;
    }
    const fall = this.fall();
    const reading = readFallAt(fall, fallTimeAt(fall, chart.width, pointer));
    return {
      x: fallX(fall, chart.width, reading.time),
      y: fallY(fall, chart.plotHeight, reading.left),
      when: this.pointerWhen(reading.kind, reading.time),
      value: this.pointerValue(reading.kind, reading.left),
    };
  });

  /**
   * Width of the panel the chart fills, in pixels.
   */
  private readonly width = signal(0);

  /**
   * Pointer x over the chart, in pixels, `null` while away.
   */
  private readonly pointer = signal<number | null>(null);

  /**
   * Panel measured to lay the chart out at its real width.
   */
  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');

  /**
   * Translation service, to word the forecast and format its figures.
   */
  private readonly translation = inject(Translation);

  constructor() {
    afterRenderEffect((onCleanup) => {
      const element = this.panel().nativeElement;
      const observer = new ResizeObserver(([entry]) => this.width.set(entry.contentRect.width));
      observer.observe(element);
      onCleanup(() => observer.disconnect());
    });
  }

  /**
   * Folds or unfolds the chart.
   */
  protected toggle(): void {
    this.open.update((open) => !open);
  }

  /**
   * Tracks the pointer across the chart, clamped to its width.
   */
  protected point(event: PointerEvent): void {
    const bounds = (event.currentTarget as SVGElement).getBoundingClientRect();
    this.pointer.set(Math.min(bounds.width, Math.max(0, event.clientX - bounds.left)));
  }

  /**
   * Clears the reading once the pointer leaves the chart.
   */
  protected leave(): void {
    this.pointer.set(null);
  }

  /**
   * The last known reading: now, or the fatal blow.
   */
  private lastReading(): FallReading {
    const readings = this.fall().readings;
    return readings[readings.length - 1];
  }

  /**
   * Names the instant under the pointer, by its kind of reading.
   */
  private pointerWhen(kind: FallPointer['kind'], time: number): string {
    switch (kind) {
      case 'start':
        return this.translation.translate('overview.report.forecast.tipStart');
      case 'dayEnd':
        return this.translation.translate('overview.report.forecast.tipDayEnd', {
          weekday: this.at(time - 1, { weekday: 'long' }),
        });
      case 'now':
        return this.translation.translate('overview.report.forecast.tipNow');
      default:
        return capitalizeFirst(
          this.at(time, { weekday: 'long', hour: '2-digit', minute: '2-digit' }),
        );
    }
  }

  /**
   * Words the hit points under the pointer, rounded when estimated.
   */
  private pointerValue(kind: FallPointer['kind'], left: number): string {
    switch (kind) {
      case 'after':
      case 'kill':
        return this.translation.translate('overview.report.forecast.tipDown');
      case 'zero':
        return this.translation.translate('overview.report.forecast.tipZero');
      case 'estimate':
        return this.translation.translate('overview.report.forecast.tipEstimate', {
          hp: this.hitPoints(Math.round(left / 100) * 100),
        });
      default:
        return this.translation.translate('overview.report.forecast.tipLeft', {
          hp: this.hitPoints(left),
        });
    }
  }

  /**
   * Words a duration in days and hours, or hours alone under a day.
   */
  private span(duration: number): string {
    const { days, hours } = splitSpan(duration);
    return days > 0
      ? this.translation.translate('overview.report.forecast.spanDays', { days, hours })
      : this.translation.translate('overview.report.forecast.spanHours', { hours });
  }

  /**
   * Formats hit points in the active language.
   */
  private hitPoints(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  /**
   * Formats an instant in the campaign time zone and the active language.
   */
  private at(time: number, options: Intl.DateTimeFormatOptions): string {
    return formatCampaignTime(time, this.translation.language(), options);
  }
}
