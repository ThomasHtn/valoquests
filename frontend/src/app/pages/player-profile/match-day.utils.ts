import { toCampaignDayKey } from '@core/date/campaign-time-zone.utils';
import { CAMPAIGN_TIME_ZONE } from '@core/date/date-time.constants';
import { formatLocalDayMonth } from '@core/date/date-time.utils';
import { Match } from '@core/matches/match.model';
import { MatchDay } from './match-day.model';

/**
 * A {@link MatchDay} before its day-level averages ({@link MatchDay.avgKd} and friends) have been
 * derived from {@link MatchDay.matches} - computed once per day, after grouping, by
 * {@link withDayAverages} rather than kept incrementally in sync on every match folded into the
 * group.
 */
type MatchDayGroup<T extends Match> = Omit<
  MatchDay<T>,
  | 'avgAcs'
  | 'avgAdr'
  | 'avgHeadshotPercentage'
  | 'avgKd'
  | 'totalAssists'
  | 'totalDeaths'
  | 'totalKills'
  | 'totalValoquestsDamage'
>;

/**
 * Groups a page of match history into consecutive days.
 *
 * Groups are emitted in the order the matches arrive, and a new group is opened every time the
 * calendar day changes rather than by looking the day up in a map: the API already returns the
 * page sorted, and preserving that order keeps the rendered history in sync with the pagination.
 *
 * @param matches - The page's matches, sorted by start instant as returned by the API.
 * @param language - The app language {@link MatchDay.dateLabel} is spelled out in.
 * @returns One group per day, each carrying the day's win/loss record and stat averages.
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
 * Derives a day's stat averages and totals from its matches.
 *
 * @param day - The grouped day, with its matches already collected.
 * @returns The day, with its averages and totals filled in.
 */
function withDayAverages<T extends Match>(day: MatchDayGroup<T>): MatchDay<T> {
  const sum = (selector: (match: Match) => number): number =>
    day.matches.reduce((total, match) => total + selector(match), 0);

  // Matches without shot data are left out rather than averaged in as a false 0 %.
  const headshotPercentages = day.matches
    .map((match) => match.headshotPercentage)
    .filter((percentage) => percentage !== null);

  // Same for scores: deathmatch and escalation report none, the field is absent from the payload.
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
