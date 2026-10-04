import { CAMPAIGN_TIME_ZONE } from '@core/campaign/calendar/campaign-calendar.constants';
import { toCampaignDayKey } from '@core/campaign/calendar/campaign-calendar.utils';
import { formatLocalDayMonth } from '@core/date/date-format.utils';
import { Match } from '@core/matches/match.model';
import { MatchDay, MatchDayGroup } from './match-day.model';

/**
 * Groups a sorted history page into consecutive days, keeping API order to match pagination.
 */
export function groupMatchesByDay<T extends Match>(
  matches: readonly T[],
  language: 'fr' | 'en',
): readonly MatchDay<T>[] {
  const days: MatchDayGroup<T>[] = [];

  for (const match of matches) {
    const dayKey = toCampaignDayKey(match.startedAt);
    const currentDay = days.at(-1);

    if (currentDay?.dayKey === dayKey) {
      days[days.length - 1] = {
        ...currentDay,
        wins: currentDay.wins + (match.result === 'WIN' ? 1 : 0),
        losses: currentDay.losses + (match.result === 'LOSS' ? 1 : 0),
        matches: [...currentDay.matches, match],
      };
      continue;
    }

    days.push({
      dayKey,
      dateLabel: formatLocalDayMonth(match.startedAt, language, 'short', CAMPAIGN_TIME_ZONE),
      wins: match.result === 'WIN' ? 1 : 0,
      losses: match.result === 'LOSS' ? 1 : 0,
      matches: [match],
    });
  }

  return days.map(withDayAverages);
}

/**
 * Fills a grouped day's averages and totals from its matches.
 */
function withDayAverages<T extends Match>(day: MatchDayGroup<T>): MatchDay<T> {
  const sum = (selector: (match: Match) => number): number =>
    day.matches.reduce((total, match) => total + selector(match), 0);

  // Matches without shot data are left out rather than averaged in as a false 0 %.
  const headshotPercentages = day.matches
    .map((match) => match.headshotPercentage)
    .filter((percentage) => percentage !== null);

  // Same for scores: deathmatch and escalation omit the field.
  const average = (selector: (match: Match) => number): number | null => {
    const values = day.matches.map(selector).filter((value) => Number.isFinite(value));
    return values.length === 0
      ? null
      : values.reduce((total, value) => total + value, 0) / values.length;
  };

  return {
    ...day,
    // From the day's totals, so it agrees with the K/D/A printed beside it.
    avgKd:
      sum((match) => match.kills) /
      Math.max(
        1,
        sum((match) => match.deaths),
      ),
    avgHeadshotPercentage:
      headshotPercentages.length === 0
        ? null
        : headshotPercentages.reduce((total, percentage) => total + percentage, 0) /
          headshotPercentages.length,
    avgAdr: average((match) => match.adr),
    avgAcs: average((match) => match.acs),
    totalKills: sum((match) => match.kills),
    totalDeaths: sum((match) => match.deaths),
    totalAssists: sum((match) => match.assists),
    totalValoquestsDamage: sum((match) => match.valoquestsDamage),
  };
}
