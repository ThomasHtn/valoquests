import { ChallengeProgress } from '@core/challenges/challenge.model';
import { WEEK_DAYS } from '@core/date/date-time.constants';
import { DAILY_TONE } from '@pages/challenges/challenges.constants';
import { BoardRow, ChallengeOperator, DayCell, DayState } from '@pages/challenges/challenges.model';
import { buildChallengeCard, toBoardRow } from '@pages/challenges/challenges.utils';
import { formatFigure } from '@pages/leaderboard/leaderboard-board.utils';

import { TourSampleDaily } from './tour.model';

/**
 * Resolves a full translation key with its parameters.
 */
type Translate = (key: string, params?: Record<string, string | number>) => string;

/**
 * The sample day's challenge as the challenges page lays it out on a phone, through the page's own
 * builders so the card reads exactly like the real one.
 *
 * @param daily - The sample challenge.
 * @param operators - The sample squad, in roster order.
 * @param closesAt - When the day closes, in epoch milliseconds.
 * @param locale - `Intl` locale the figures are written in.
 * @param translate - Resolves a full translation key with its parameters.
 * @returns The board row the deck card renders.
 */
export function buildTourDailyRow(
  daily: TourSampleDaily,
  operators: readonly ChallengeOperator[],
  closesAt: number,
  locale: string,
  translate: Translate,
): BoardRow {
  const players = operators.map((operator, index) => {
    const value = daily.progress[index] ?? 0;
    return { playerId: operator.playerId, currentValue: value, completed: value >= daily.target };
  });
  const completedPlayerIds = players.filter((line) => line.completed).map((line) => line.playerId);
  const challenge: ChallengeProgress = {
    id: 0,
    code: daily.key,
    name: translate(`tour.samples.challenges.${daily.key}.name`),
    description: translate(`tour.samples.challenges.${daily.key}.description`),
    cadence: 'DAILY',
    difficulty: null,
    competitiveOnly: false,
    metric: 'MATCHES_PLAYED',
    targetValue: daily.target,
    survivors: daily.survivors,
    rankingPoints: daily.survivors,
    day: null,
    completedPlayers: completedPlayerIds.length,
    totalPlayers: operators.length,
    completedPlayerIds,
    completionPercentage: Math.round((completedPlayerIds.length / operators.length) * 100),
    players,
  };
  const look = { tone: DAILY_TONE, mark: 'D' as const, kind: translate('challenges.daily.key') };
  const card = buildChallengeCard(challenge, look, operators, true, (amount) =>
    formatFigure(amount, locale, amount >= 1_000),
  );
  return toBoardRow(challenge, card, card.rungs, closesAt, locale, translate);
}

/**
 * The sample week's seven-day strip: the days before today closed with their tallies, today
 * running, the rest ahead.
 *
 * @param tally - Operators who validated each closed day's challenge, Monday first.
 * @param todayDone - Operators who validated today's challenge so far.
 * @param total - Operators on the roster.
 * @param weekStart - Local midnight of the week's Monday.
 * @param locale - `Intl` locale the weekdays are written in.
 * @param translate - Resolves a full translation key with its parameters.
 * @returns One cell per day, Monday first.
 */
export function buildTourWeek(
  tally: readonly number[],
  todayDone: number,
  total: number,
  weekStart: Date,
  locale: string,
  translate: Translate,
): DayCell[] {
  const todayIndex = tally.length;
  return Array.from({ length: WEEK_DAYS }, (_, index) => {
    const date = new Date(weekStart);
    date.setDate(weekStart.getDate() + index);
    const state: DayState = index < todayIndex ? 'closed' : index === todayIndex ? 'now' : 'ahead';
    const drawn = state !== 'ahead';
    const doneCount =
      index < todayIndex ? (tally[index] ?? 0) : index === todayIndex ? todayDone : 0;
    const weekday = new Intl.DateTimeFormat(locale, { weekday: 'short' })
      .format(date)
      .replace('.', '');
    let tip = translate('challenges.daily.tipUnavailable');
    if (drawn) {
      tip =
        doneCount === 0
          ? translate('challenges.daily.tipNone', { total })
          : translate('challenges.daily.tipDone', { count: doneCount, total });
    }
    return {
      index,
      state,
      weekday: weekday.charAt(0).toUpperCase() + weekday.slice(1),
      date: new Intl.DateTimeFormat(locale, { day: 'numeric' }).format(date),
      drawn,
      doneCount,
      total,
      tip,
    };
  });
}

/**
 * Local midnight of the Monday of the week holding a moment.
 *
 * @param now - The moment, in epoch milliseconds.
 * @returns The week's Monday at local midnight.
 */
export function startOfWeek(now: number): Date {
  const date = new Date(now);
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
  return date;
}

/**
 * Next local midnight after a moment, when that day's challenge closes.
 *
 * @param now - The moment, in epoch milliseconds.
 * @returns The next local midnight, in epoch milliseconds.
 */
export function endOfDay(now: number): number {
  const date = new Date(now);
  date.setHours(24, 0, 0, 0);
  return date.getTime();
}
