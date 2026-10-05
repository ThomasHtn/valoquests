import { Language, TranslateFn } from '@core/i18n/translation.model';
import { ChallengeProgress } from '@core/challenges/challenge.model';
import { WEEK_DAYS } from '@core/date/date.constants';
import { DAILY_TONE } from '@core/challenges/card/challenge-card.constants';
import { BoardRow, ChallengeOperator } from '@core/challenges/card/challenge-card.model';
import { DayCell, DayState } from '@pages/challenges/challenges.model';
import { buildChallengeCard, toBoardRow } from '@core/challenges/card/challenge-card.utils';
import { formatFigure } from '@core/i18n/format/number-format.utils';

import { TOUR_EMPHASIS_MARKER } from './tour.constants';
import { ClaimRun, TourSampleDaily } from './tour.model';

/**
 * Sample daily challenge built with the page's own builders; `closesAt` in epoch ms.
 */
export function buildTourDailyRow(
  daily: TourSampleDaily,
  operators: readonly ChallengeOperator[],
  closesAt: number,
  language: Language,
  translate: TranslateFn,
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
    formatFigure(amount, language, amount >= 1_000),
  );
  return toBoardRow(challenge, card, card.rungs, closesAt, language, translate);
}

/**
 * Sample week strip, Monday first; `tally` holds each closed day's count, today comes next.
 */
export function buildTourWeek(
  tally: readonly number[],
  todayDone: number,
  total: number,
  weekStart: Date,
  locale: string,
  translate: TranslateFn,
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
 * Local midnight of the Monday of the week holding `now` (epoch ms).
 */
export function startOfWeek(now: number): Date {
  const date = new Date(now);
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
  return date;
}

/**
 * Next local midnight after `now`, in epoch ms.
 */
export function endOfDay(now: number): number {
  const date = new Date(now);
  date.setHours(24, 0, 0, 0);
  return date.getTime();
}

/**
 * Splits a translated claim into plain and `*emphasized*` runs, empty runs dropped.
 */
export function splitEmphasis(claim: string): readonly ClaimRun[] {
  return claim
    .split(TOUR_EMPHASIS_MARKER)
    .map((text, index) => ({ text, strong: index % 2 === 1 }))
    .filter((run) => run.text.length > 0);
}
