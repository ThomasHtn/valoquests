import { formatLocalDate, toLocalDayKey } from '@core/date/date-time.utils';
import { Match } from '@core/matches/match.model';
import { MatchDay } from './match-day.model';

/**
 * Groups a page of match history into consecutive days.
 *
 * Groups are emitted in the order the matches arrive, and a new group is opened every time the
 * calendar day changes rather than by looking the day up in a map: the API already returns the
 * page sorted, and preserving that order keeps the rendered history in sync with the pagination.
 *
 * @param matches - The page's matches, sorted by start instant as returned by the API.
 * @returns One group per day, each carrying the day's win/loss record.
 */
export function groupMatchesByDay(matches: readonly Match[]): readonly MatchDay[] {
  const days: MatchDay[] = [];

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
      dateLabel: formatLocalDate(match.startedAt),
      wins: match.result === 'WIN' ? 1 : 0,
      losses: match.result === 'LOSS' ? 1 : 0,
      matches: [match],
    });
  }

  return days;
}
