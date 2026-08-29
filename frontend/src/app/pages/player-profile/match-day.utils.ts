import { formatLocalDayMonth, toLocalDayKey } from '@core/date/date-time.utils';
import { Match } from '@core/matches/match.model';
import { MatchDay } from './match-day.model';

/**
 * A {@link MatchDay} before its day-level averages ({@link MatchDay.avgKda} and friends) have been
 * derived from {@link MatchDay.matches} - computed once per day, after grouping, by
 * {@link withDayAverages} rather than kept incrementally in sync on every match folded into the
 * group.
 */
type MatchDayGroup = Omit<
  MatchDay,
  | 'avgAcs'
  | 'avgAdr'
  | 'avgHeadshotPercentage'
  | 'avgKda'
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
export function groupMatchesByDay(
  matches: readonly Match[],
  language: 'fr' | 'en',
): readonly MatchDay[] {
  const days: MatchDayGroup[] = [];

  for (const match of matches) {
    const dayKey = toLocalDayKey(match.startedAt);
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
      dateLabel: formatLocalDayMonth(match.startedAt, language),
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
function withDayAverages(day: MatchDayGroup): MatchDay {
  const count = day.matches.length;
  const sum = (selector: (match: Match) => number): number =>
    day.matches.reduce((total, match) => total + selector(match), 0);

  return {
    ...day,
    avgKda: sum((match) => match.kda) / count,
    avgHeadshotPercentage: sum((match) => match.headshotPercentage) / count,
    avgAdr: sum((match) => match.adr) / count,
    avgAcs: sum((match) => match.acs) / count,
    totalKills: sum((match) => match.kills),
    totalDeaths: sum((match) => match.deaths),
    totalAssists: sum((match) => match.assists),
    totalValoquestsDamage: sum((match) => match.valoquestsDamage),
  };
}
