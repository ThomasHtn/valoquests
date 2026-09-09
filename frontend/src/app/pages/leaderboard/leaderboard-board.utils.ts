import { Campaign, CampaignHistory, WeeklyTitle } from '@core/campaign/campaign.model';
import { WEEK_DAYS } from '@core/date/date-time.constants';
import { daysBetween, localMidnight } from '@core/date/date-time.utils';
import { RankingEntry } from '@core/ranking/ranking.model';
import { BoardColumn, BoardRow, WeekOption } from './leaderboard.model';

/**
 * Pure helpers of the leaderboard: figures, dates and campaign placement, with no i18n service.
 */

/**
 * The figure each title is awarded on, the way the backend awards them.
 */
export function resolveTitleMeasures(entry: RankingEntry): Partial<Record<WeeklyTitle, number>> {
  return {
    MECHANIC: entry.components,
    QUARTERMASTER: entry.food,
    REGULAR: entry.streakDays,
    SCOUT: entry.completedChallenges + entry.completedDailyChallenges,
  };
}

/**
 * One column per challenge the rows carry, in the order they carry them: every operator gets the
 * same draw, so the first row that holds a cell names the column for all of them.
 */
export function buildBoardColumns(rows: readonly BoardRow[]): BoardColumn[] {
  const columns = new Map<number, BoardColumn>();
  for (const cell of rows.flatMap((row) => row.progress ?? [])) {
    if (!columns.has(cell.id)) {
      columns.set(cell.id, {
        id: cell.id,
        mark: cell.mark,
        barClass: cell.barClass,
        iconClass: cell.visual.iconClass,
        tip: cell.name,
      });
    }
  }
  return [...columns.values()];
}

/**
 * Where a Monday falls: the running campaign's own week list first, then every closed campaign
 * by its first and last Mondays. Outside all of them, no index and no group.
 */
export function placeWeekInCampaign(
  weekStart: string,
  campaign: Campaign | null,
  history: readonly CampaignHistory[],
): Pick<WeekOption, 'index' | 'group'> {
  const week = campaign?.weeks.find((candidate) => candidate.weekStart === weekStart);
  if (week && campaign) {
    return { index: week.weekIndex, group: campaign.id };
  }
  const closed = history.find(
    (candidate) => weekStart >= candidate.firstWeekStart && weekStart <= candidate.lastWeekStart,
  );
  if (closed) {
    return {
      index: daysBetween(closed.firstWeekStart, weekStart) / WEEK_DAYS + 1,
      group: closed.id,
    };
  }
  return { index: null, group: null };
}

/**
 * A figure in the reader's locale; abbreviated (`27k`) on request, for the ring's own fallback
 * once the exact figure runs wider than its disc.
 */
export function formatFigure(amount: number, locale: string, compact = false): string {
  const label = new Intl.NumberFormat(locale, {
    notation: compact ? 'compact' : 'standard',
    maximumFractionDigits: compact ? 1 : 2,
  }).format(amount);
  return compact ? label.replace(/[\s\u00a0\u202f]+/g, '') : label;
}

/**
 * Monday to Sunday, the month spelled once when both days share it (`31 août – 6 sept.`).
 */
export function formatWeekSpan(weekStart: string, locale: string): string {
  const monday = localMidnight(weekStart);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + WEEK_DAYS - 1);
  const short = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' });
  if (monday.getMonth() === sunday.getMonth()) {
    return `${monday.getDate()} – ${short.format(sunday)}`;
  }
  return `${short.format(monday)} – ${short.format(sunday)}`;
}
