import { toCampaignDayKey } from '@core/campaign/calendar/campaign-calendar.utils';
import { formatCampaignDayMonth } from '@core/date/date-format.utils';
import { Language } from '@core/i18n/translation.model';
import { Match } from '@core/matches/match.model';

import { MatchDay, MatchDayGroup } from './match-day.model';

/**
 * Groups a sorted history page into consecutive days, keeping API order to match pagination.
 */
export function groupMatchesByDay<T extends Match>(
  matches: readonly T[],
  language: Language,
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
      dateLabel: formatCampaignDayMonth(match.startedAt, language, 'short'),
      wins: match.result === 'WIN' ? 1 : 0,
      losses: match.result === 'LOSS' ? 1 : 0,
      matches: [match],
    });
  }

  return days.map(withDayAverages);
}

/**
 * Mean of the values a mode reports, `null` when none does.
 */
function averageOfReported(values: readonly (number | null)[]): number | null {
  const reported = values.filter((value): value is number => value !== null);
  return reported.length === 0
    ? null
    : reported.reduce((total, value) => total + value, 0) / reported.length;
}

/**
 * Fills a grouped day's averages and totals from its matches.
 */
function withDayAverages<T extends Match>(day: MatchDayGroup<T>): MatchDay<T> {
  const sum = (selector: (match: Match) => number): number =>
    day.matches.reduce((total, match) => total + selector(match), 0);
  const totalKills = sum((match) => match.kills);
  const totalDeaths = sum((match) => match.deaths);

  return {
    ...day,
    // From the day's totals, so it agrees with the K/D/A printed beside it.
    avgKd: totalKills / Math.max(1, totalDeaths),
    // Matches without the figure are left out rather than averaged in as a false 0.
    avgHeadshotPercentage: averageOfReported(day.matches.map((match) => match.headshotPercentage)),
    avgAdr: averageOfReported(day.matches.map((match) => match.adr)),
    avgAcs: averageOfReported(day.matches.map((match) => match.acs)),
    totalKills,
    totalDeaths,
    totalAssists: sum((match) => match.assists),
    totalValoquestsDamage: sum((match) => match.valoquestsDamage),
  };
}
