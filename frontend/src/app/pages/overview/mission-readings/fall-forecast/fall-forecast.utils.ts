import { CampaignWeek } from '@core/campaign/campaign.model';
import { campaignMidnight } from '@core/date/campaign-time-zone.utils';
import { CAMPAIGN_TIME_ZONE, WEEK_DAYS } from '@core/date/date-time.constants';
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
  FallChart,
  FallChartPoint,
  FallPointer,
  FallReading,
  FallZone,
  GuardianFall,
} from './fall-forecast.model';

/**
 * Rebuilds the week's descent from its daily damage and projects it at the pace held since Monday.
 *
 * The played part only knows each finished day's close: within a day the curve is smoothed, never
 * read. Today is pinned on the week's own total, the figure the duel shows.
 *
 * @param week - The week in progress, or `null` outside one.
 * @param now - The current instant, in epoch milliseconds.
 * @returns The descent, or `null` while nobody has hit the guardian yet.
 */
export function buildGuardianFall(week: CampaignWeek | null, now: number): GuardianFall | null {
  if (!week || week.guardianHitPoints <= 0) {
    return null;
  }
  const hitPoints = week.guardianHitPoints;
  const weekStart = campaignMidnight(week.weekStart).getTime();
  const deadline = campaignMidnight(week.weekStart, WEEK_DAYS).getTime();
  const killAt = week.defeated && week.defeatedAt ? Date.parse(week.defeatedAt) : null;
  const until = killAt ?? Math.min(Math.max(now, weekStart), deadline);

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

  const shape = { hitPoints, weekStart, deadline };
  if (killAt !== null) {
    readings.push({ kind: 'kill', time: killAt, left: 0 });
    return {
      ...shape,
      outcome: 'down',
      readings,
      pace: hitPoints / Math.max(1, killAt - weekStart),
      fallAt: killAt,
      leftAtDeadline: 0,
    };
  }

  const total = Math.min(hitPoints, week.damageDealt);
  if (total <= 0 || until <= weekStart) {
    return null;
  }
  const left = hitPoints - total;
  readings.push({ kind: 'now', time: until, left });
  const pace = total / (until - weekStart);
  const fallAt = until + left / pace;
  if (fallAt <= deadline) {
    return { ...shape, outcome: 'ahead', readings, pace, fallAt, leftAtDeadline: 0 };
  }
  return {
    ...shape,
    outcome: 'short',
    readings,
    pace,
    fallAt: null,
    leftAtDeadline: left - pace * (deadline - until),
  };
}

/**
 * Lays the descent out for one width: tinted columns, the played curve, the projection and its
 * markers.
 *
 * @param fall - The descent.
 * @param width - Width of the drawing, in pixels.
 * @returns The chart's geometry.
 */
export function layoutFallChart(fall: GuardianFall, width: number): FallChart {
  const narrow = width < FALL_NARROW_WIDTH;
  const plotHeight = narrow ? FALL_PLOT_HEIGHT_NARROW : FALL_PLOT_HEIGHT;
  const x = (time: number): number => fallX(fall, width, time);
  const y = (left: number): number => fallY(fall, plotHeight, left);

  const last = fall.readings[fall.readings.length - 1];
  const reaches = fall.fallAt !== null;
  const projectionEnd = fall.fallAt ?? fall.deadline;

  const zones: FallZone[] = [{ kind: 'past', x: 0, width: x(last.time) }];
  if (fall.outcome !== 'down') {
    zones.push({
      kind: reaches ? 'ahead' : 'short',
      x: x(last.time),
      width: x(projectionEnd) - x(last.time),
    });
  }
  if (fall.fallAt !== null) {
    zones.push({ kind: 'spare', x: x(fall.fallAt), width: width - x(fall.fallAt) });
  }

  const pastLine = smoothPath(
    fall.readings.map((reading) => point(x(reading.time), y(reading.left))),
  );
  const floorPath = (from: number, to: number): string =>
    ` L${round(to)},${plotHeight} L${round(from)},${plotHeight} Z`;

  let projectionLine: string | null = null;
  let projectionArea: string | null = null;
  if (fall.outcome !== 'down') {
    const endLeft = reaches ? 0 : fall.leftAtDeadline;
    projectionLine = `M${round(x(last.time))},${round(y(last.left))} L${round(x(projectionEnd))},${round(y(endLeft))}`;
    projectionArea = projectionLine + floorPath(x(last.time), x(projectionEnd));
  }

  const markX = fall.fallAt === null ? null : x(fall.fallAt);
  return {
    width,
    plotHeight,
    height: plotHeight + FALL_TICK_ROW,
    zones,
    pastLine,
    pastArea: pastLine + floorPath(0, x(last.time)),
    projectionLine,
    projectionArea,
    now: fall.outcome === 'down' ? null : point(x(last.time), y(last.left)),
    // Pinned a little inside the edge so its dot is not cut in half.
    end: fall.outcome === 'short' ? point(width - 10, y(fall.leftAtDeadline)) : null,
    fall: markX === null ? null : point(markX, y(0)),
    fallAnchor:
      markX === null || (markX >= FALL_LABEL_EDGE && markX <= width - FALL_LABEL_EDGE)
        ? 'middle'
        : markX < FALL_LABEL_EDGE
          ? 'start'
          : 'end',
    gutter: narrow ? FALL_GUTTER_NARROW : FALL_GUTTER,
  };
}

/**
 * Places an instant across the chart.
 *
 * @param fall - The descent.
 * @param width - Width of the drawing, in pixels.
 * @param time - The instant, in epoch milliseconds.
 * @returns Its horizontal position, in pixels.
 */
export function fallX(fall: GuardianFall, width: number, time: number): number {
  return ((time - fall.weekStart) / (fall.deadline - fall.weekStart)) * width;
}

/**
 * Places a hit point count up the chart: the full pool under the labels' room, zero just above
 * the floor.
 *
 * @param fall - The descent.
 * @param plotHeight - Height of the plot, in pixels.
 * @param left - Hit points left.
 * @returns Its vertical position, in pixels.
 */
export function fallY(fall: GuardianFall, plotHeight: number, left: number): number {
  const floor = plotHeight - FALL_FLOOR_ROOM;
  return floor - (left / fall.hitPoints) * (floor - FALL_TOP_ROOM);
}

/**
 * Turns a horizontal position on the chart back into an instant.
 *
 * @param fall - The descent.
 * @param width - Width of the drawing, in pixels.
 * @param x - The position, in pixels.
 * @returns The instant, in epoch milliseconds.
 */
export function fallTimeAt(fall: GuardianFall, width: number, x: number): number {
  return fall.weekStart + (x / width) * (fall.deadline - fall.weekStart);
}

/**
 * Reads the descent at one instant: the nearest known reading over the played part, the
 * projection beyond it.
 *
 * @param fall - The descent.
 * @param time - The instant pointed at, in epoch milliseconds.
 * @returns What the chart says there.
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
 * Splits a duration into whole days and hours, as the fold row spells it.
 *
 * @param duration - The duration, in milliseconds.
 * @returns Its whole days and the hours left over.
 */
export function splitSpan(duration: number): { readonly days: number; readonly hours: number } {
  const safe = Math.max(0, duration);
  return { days: Math.floor(safe / DAY_MS), hours: Math.floor((safe % DAY_MS) / HOUR_MS) };
}

/**
 * Rounds an estimated instant to the nearest hour: the pace is an average, its minutes mean nothing.
 *
 * @param time - The instant, in epoch milliseconds.
 * @returns The instant on the nearest hour.
 */
export function toNearestHour(time: number): number {
  return Math.round(time / HOUR_MS) * HOUR_MS;
}

/**
 * Formats an instant in the campaign time zone, the one the week's days are cut in.
 *
 * @param time - The instant, in epoch milliseconds.
 * @param language - The reader's language.
 * @param options - Which fields to show.
 * @returns The formatted instant.
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
 * Draws a monotone cubic through the points (Fritsch-Carlson), so the curve never rises between
 * two readings of a guardian that only loses hit points.
 *
 * @param points - The points, left to right.
 * @returns The SVG path.
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
 * Rounds a coordinate to a tenth of a pixel, enough for a crisp path and a short string.
 */
function round(value: number): number {
  return Math.round(value * 10) / 10;
}

/**
 * Capitalizes a sentence that opens on a weekday, which French writes in lower case.
 *
 * @param text - The sentence.
 * @returns The sentence with its first letter in upper case.
 */
export function capitalizeFirst(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
