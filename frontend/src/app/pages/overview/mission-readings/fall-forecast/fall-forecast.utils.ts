import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { campaignMidnight } from '@core/campaign/calendar/campaign-calendar.utils';
import { CAMPAIGN_TIME_ZONE } from '@core/campaign/calendar/campaign-calendar.constants';
import { WEEK_DAYS } from '@core/date/date.constants';
import { Language } from '@core/i18n/translation.model';
import {
  DAY_MS,
  FALL_FLOOR_ROOM,
  FALL_GUTTER,
  FALL_GUTTER_NARROW,
  FALL_LABEL_EDGE,
  FALL_NARROW_WIDTH,
  FALL_PLOT_HEIGHT,
  FALL_PLOT_HEIGHT_NARROW,
  FALL_TICK_ROW,
  FALL_TOP_ROOM,
  HOUR_MS,
} from './fall-forecast.constants';
import {
  FallBounds,
  FallChart,
  FallChartPoint,
  FallPointer,
  FallReading,
  FallZone,
  GuardianFall,
} from './fall-forecast.model';

/**
 * The week's descent from its daily closes, projected at Monday's pace; `null` before any hit.
 * Today is pinned on the week's total, the figure the duel shows.
 */
export function buildGuardianFall(week: CampaignWeek | null, now: number): GuardianFall | null {
  if (!week || week.guardianHitPoints <= 0) {
    return null;
  }
  const bounds: FallBounds = {
    hitPoints: week.guardianHitPoints,
    weekStart: campaignMidnight(week.weekStart).getTime(),
    deadline: campaignMidnight(week.weekStart, WEEK_DAYS).getTime(),
  };
  const killAt = week.defeated && week.defeatedAt ? Date.parse(week.defeatedAt) : null;
  // Stops at the kill, or at now clamped to the week.
  const until = killAt ?? Math.min(Math.max(now, bounds.weekStart), bounds.deadline);
  const readings = closedDayReadings(week, bounds, until);

  if (killAt !== null) {
    return fallenGuardian(bounds, readings, killAt);
  }
  return projectFall(bounds, readings, week.damageDealt, until);
}

/**
 * Monday's full pool, then the close of every day finished before `until`.
 */
function closedDayReadings(week: CampaignWeek, bounds: FallBounds, until: number): FallReading[] {
  const { hitPoints, weekStart } = bounds;
  const readings: FallReading[] = [{ kind: 'start', time: weekStart, left: hitPoints }];
  let dealt = 0;
  for (const [index, damage] of week.dailyDamage.entries()) {
    const close = campaignMidnight(week.weekStart, index + 1).getTime();
    if (close >= until) {
      break;
    }
    dealt += damage;
    readings.push({ kind: 'dayEnd', time: close, left: Math.max(0, hitPoints - dealt) });
  }
  return readings;
}

/**
 * A guardian already down: the curve ends on the fatal blow.
 */
function fallenGuardian(
  bounds: FallBounds,
  readings: readonly FallReading[],
  killAt: number,
): GuardianFall {
  return {
    ...bounds,
    outcome: 'down',
    readings: [...readings, { kind: 'kill', time: killAt, left: 0 }],
    pace: bounds.hitPoints / Math.max(1, killAt - bounds.weekStart),
    fallAt: killAt,
    leftAtDeadline: 0,
  };
}

/**
 * A standing guardian: now pinned on the week's total, pace extended; `null` before any hit.
 */
function projectFall(
  bounds: FallBounds,
  closedDays: readonly FallReading[],
  damageDealt: number,
  until: number,
): GuardianFall | null {
  const { hitPoints, weekStart, deadline } = bounds;
  const total = Math.min(hitPoints, damageDealt);
  if (total <= 0 || until <= weekStart) {
    return null;
  }
  const left = hitPoints - total;
  const readings: FallReading[] = [...closedDays, { kind: 'now', time: until, left }];
  const pace = total / (until - weekStart);
  const fallAt = until + left / pace;
  if (fallAt <= deadline) {
    return { ...bounds, outcome: 'ahead', readings, pace, fallAt, leftAtDeadline: 0 };
  }
  return {
    ...bounds,
    outcome: 'short',
    readings,
    pace,
    fallAt: null,
    leftAtDeadline: left - pace * (deadline - until),
  };
}

/**
 * Chart geometry for one drawing width, in pixels.
 */
export function layoutFallChart(fall: GuardianFall, width: number): FallChart {
  const narrow = width < FALL_NARROW_WIDTH;
  const plotHeight = narrow ? FALL_PLOT_HEIGHT_NARROW : FALL_PLOT_HEIGHT;
  const x = (time: number): number => fallX(fall, width, time);
  const y = (left: number): number => fallY(fall, plotHeight, left);

  const last = fall.readings[fall.readings.length - 1];
  const stillStanding = fall.outcome !== 'down';
  const projectionEnd = fall.fallAt ?? fall.deadline;
  const pastLine = smoothPath(
    fall.readings.map((reading) => point(x(reading.time), y(reading.left))),
  );
  const projectionLine = stillStanding ? straightProjection(fall, last, x, y) : null;
  const markX = fall.fallAt === null ? null : x(fall.fallAt);

  return {
    width,
    plotHeight,
    height: plotHeight + FALL_TICK_ROW,
    zones: fallZones(fall, width, x),
    pastLine,
    pastArea: pastLine + floorPath(0, x(last.time), plotHeight),
    projectionLine,
    projectionArea:
      projectionLine === null
        ? null
        : projectionLine + floorPath(x(last.time), x(projectionEnd), plotHeight),
    now: stillStanding ? point(x(last.time), y(last.left)) : null,
    // Inset so its dot is not cut in half.
    end: fall.outcome === 'short' ? point(width - 10, y(fall.leftAtDeadline)) : null,
    fall: markX === null ? null : point(markX, y(0)),
    fallAnchor: fallLabelAnchor(markX, width),
    gutter: narrow ? FALL_GUTTER_NARROW : FALL_GUTTER,
  };
}

/**
 * Tinted columns: played part, projection while standing, spare time after the fall.
 */
function fallZones(fall: GuardianFall, width: number, x: (time: number) => number): FallZone[] {
  const lastX = x(fall.readings[fall.readings.length - 1].time);
  const zones: FallZone[] = [{ kind: 'past', x: 0, width: lastX }];
  if (fall.outcome !== 'down') {
    zones.push({
      kind: fall.fallAt !== null ? 'ahead' : 'short',
      x: lastX,
      width: x(fall.fallAt ?? fall.deadline) - lastX,
    });
  }
  if (fall.fallAt !== null) {
    zones.push({ kind: 'spare', x: x(fall.fallAt), width: width - x(fall.fallAt) });
  }
  return zones;
}

/**
 * Straight line from the last reading to the fall, or to what stands at Sunday midnight.
 */
function straightProjection(
  fall: GuardianFall,
  last: FallReading,
  x: (time: number) => number,
  y: (left: number) => number,
): string {
  const endTime = fall.fallAt ?? fall.deadline;
  const endLeft = fall.fallAt !== null ? 0 : fall.leftAtDeadline;
  return `M${round(x(last.time))},${round(y(last.left))} L${round(x(endTime))},${round(y(endLeft))}`;
}

/**
 * Closes a line into an area by dropping to the plot's floor between `from` and `to`.
 */
function floorPath(from: number, to: number, plotHeight: number): string {
  return ` L${round(to)},${plotHeight} L${round(from)},${plotHeight} Z`;
}

/**
 * Anchors the fall's label to a nearby edge, centred otherwise.
 */
function fallLabelAnchor(markX: number | null, width: number): FallChart['fallAnchor'] {
  if (markX === null) {
    return 'middle';
  }
  if (markX < FALL_LABEL_EDGE) {
    return 'start';
  }
  if (markX <= width - FALL_LABEL_EDGE) {
    return 'middle';
  }
  return 'end';
}

/**
 * Horizontal position of an epoch-millisecond instant, in pixels.
 */
export function fallX(fall: GuardianFall, width: number, time: number): number {
  return ((time - fall.weekStart) / (fall.deadline - fall.weekStart)) * width;
}

/**
 * Vertical position of a hit point count, in pixels; zero sits just above the floor.
 */
export function fallY(fall: GuardianFall, plotHeight: number, left: number): number {
  const floor = plotHeight - FALL_FLOOR_ROOM;
  return floor - (left / fall.hitPoints) * (floor - FALL_TOP_ROOM);
}

/**
 * Epoch-millisecond instant at a horizontal position, in pixels.
 */
export function fallTimeAt(fall: GuardianFall, width: number, x: number): number {
  return fall.weekStart + (x / width) * (fall.deadline - fall.weekStart);
}

/**
 * Reading at an instant: nearest known reading over the played part, projection beyond.
 */
export function readFallAt(fall: GuardianFall, time: number): FallPointer {
  const last = fall.readings[fall.readings.length - 1];
  if (time <= last.time) {
    const nearest = fall.readings.reduce((best, reading) =>
      Math.abs(reading.time - time) < Math.abs(best.time - time) ? reading : best,
    );
    return { kind: nearest.kind, time: nearest.time, left: nearest.left };
  }
  if (fall.outcome === 'down') {
    return { kind: 'after', time, left: 0 };
  }
  const left = Math.max(0, last.left - fall.pace * (time - last.time));
  return { kind: left > 0 ? 'estimate' : 'zero', time, left };
}

/**
 * Splits a duration in milliseconds into whole days and leftover hours.
 */
export function splitSpan(duration: number): { readonly days: number; readonly hours: number } {
  const safe = Math.max(0, duration);
  return { days: Math.floor(safe / DAY_MS), hours: Math.floor((safe % DAY_MS) / HOUR_MS) };
}

/**
 * Rounds an estimate to the hour: the pace is an average, its minutes mean nothing.
 */
export function toNearestHour(time: number): number {
  return Math.round(time / HOUR_MS) * HOUR_MS;
}

/**
 * Formats an epoch-millisecond instant in the campaign time zone.
 */
export function formatCampaignTime(
  time: number,
  language: Language,
  options: Intl.DateTimeFormatOptions,
): string {
  return new Intl.DateTimeFormat(language, { ...options, timeZone: CAMPAIGN_TIME_ZONE }).format(
    time,
  );
}

/**
 * Monotone cubic SVG path (Fritsch-Carlson): the curve never rises between two readings.
 */
export function smoothPath(points: readonly FallChartPoint[]): string {
  const kept = points.filter((current, index) => index === 0 || current.x > points[index - 1].x);
  if (kept.length < 2) {
    return kept.length ? `M${round(kept[0].x)},${round(kept[0].y)}` : '';
  }
  const slopes = kept
    .slice(1)
    .map((next, index) => (next.y - kept[index].y) / (next.x - kept[index].x));
  const tangents = kept.map((_, index) => {
    if (index === 0) {
      return slopes[0];
    }
    if (index === kept.length - 1) {
      return slopes[index - 1];
    }
    const before = slopes[index - 1];
    const after = slopes[index];
    return before * after <= 0 ? 0 : (2 * before * after) / (before + after);
  });
  let path = `M${round(kept[0].x)},${round(kept[0].y)}`;
  for (let index = 0; index < kept.length - 1; index++) {
    const from = kept[index];
    const to = kept[index + 1];
    const third = (to.x - from.x) / 3;
    path +=
      ` C${round(from.x + third)},${round(from.y + tangents[index] * third)}` +
      ` ${round(to.x - third)},${round(to.y - tangents[index + 1] * third)}` +
      ` ${round(to.x)},${round(to.y)}`;
  }
  return path;
}

/**
 * Builds a chart point.
 */
function point(x: number, y: number): FallChartPoint {
  return { x, y };
}

/**
 * Rounds a coordinate to a tenth of a pixel, keeping paths crisp and short.
 */
function round(value: number): number {
  return Math.round(value * 10) / 10;
}

/**
 * Capitalizes a sentence opening on a weekday, which French writes in lower case.
 */
export function capitalizeFirst(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
