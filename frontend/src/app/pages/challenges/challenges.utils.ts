import { ChallengeProgress, RosterPlayer } from '@core/challenges/challenge.model';
import { localMidnight } from '@core/date/date-time.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { formatFigure } from '../leaderboard/leaderboard-board.utils';
import {
  ChallengeCard,
  ChallengeLook,
  ChallengeOperator,
  ChallengeRung,
  OperatorProgress,
} from './challenges.model';

/**
 * The ISO date `offset` days after another.
 */
export function shiftDay(isoDate: string, offset: number): string {
  const date = localMidnight(isoDate);
  date.setDate(date.getDate() + offset);
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${`${date.getDate()}`.padStart(2, '0')}`;
}

/**
 * One line per operator on a challenge card, the furthest along first.
 *
 * @param operators - The roster, in its own order, which settles ties.
 * @param target - Value to reach, or `null` for an open-ended challenge.
 * @param progressOf - Where one operator stands on the challenge.
 * @param format - Formats a figure for the band's labels.
 * @returns The card's lines.
 */
export function buildRungs(
  operators: readonly ChallengeOperator[],
  target: number | null,
  progressOf: (playerId: number) => OperatorProgress,
  format: (amount: number) => string,
): ChallengeRung[] {
  return operators
    .map((operator, index) => {
      const { value, done } = progressOf(operator.playerId);
      // An open-ended challenge has no scale: its band is either empty or full.
      const fraction = target !== null && target > 0 ? Math.min(1, value / target) : done ? 1 : 0;
      const rung: ChallengeRung = {
        playerId: operator.playerId,
        name: operator.name,
        portrait: operator.portrait,
        fraction: done ? 1 : fraction,
        value,
        valueLabel: format(value),
        targetLabel: target === null ? '' : format(target),
        done,
        idle: !done && value === 0,
      };
      return { rung, index };
    })
    .sort((left, right) => right.rung.fraction - left.rung.fraction || left.index - right.index)
    .map(({ rung }) => rung);
}

/**
 * The line a band's tooltip reads: the exact figures its compact labels round, and what remains.
 *
 * @param rung - The operator's line.
 * @param target - Value to reach, or `null` for an open-ended challenge.
 * @param locale - `Intl` locale the figures are written in.
 * @param translate - Resolves a `challenges.card.bandTooltip` key with its parameters.
 * @returns The worded line.
 */
export function describeRung(
  rung: ChallengeRung,
  target: number | null,
  locale: string,
  translate: (key: string, params: Record<string, string | number>) => string,
): string {
  const value = formatFigure(rung.value, locale);
  if (target === null || target <= 0) {
    return translate(rung.done ? 'openEndedDone' : 'openEnded', { value });
  }
  const params = { value, target: formatFigure(target, locale) };
  if (rung.done) {
    return translate('done', params);
  }
  const remaining = Math.max(0, target - rung.value);
  // `count` picks the plural branch of the remaining units.
  return translate('open', {
    ...params,
    remaining: formatFigure(remaining, locale),
    count: remaining,
  });
}

/**
 * The roster as the cards line it up.
 */
export function toOperators(roster: readonly RosterPlayer[]): ChallengeOperator[] {
  return roster.map((operator) => ({
    playerId: operator.id,
    name: operator.displayName,
    portrait: resolvePlayerAvatarUrl(operator.portrait),
  }));
}

/**
 * A challenge as one card shows it, whichever page draws it.
 *
 * @param challenge - The drawn challenge and each operator's progress on it.
 * @param look - Tone, mark and key line of the card.
 * @param operators - The roster, in its own order.
 * @param rescueActive - Whether a running campaign turns validations into wounded brought home.
 * @param format - Formats a figure for the band's labels.
 * @returns The card.
 */
export function buildChallengeCard(
  challenge: ChallengeProgress,
  look: ChallengeLook,
  operators: readonly ChallengeOperator[],
  rescueActive: boolean,
  format: (amount: number) => string,
): ChallengeCard {
  const rungs = buildRungs(
    operators,
    challenge.targetValue,
    (playerId) => {
      const progress = challenge.players.find((line) => line.playerId === playerId);
      return { value: progress?.currentValue ?? 0, done: progress?.completed ?? false };
    },
    format,
  );
  return {
    ...look,
    name: challenge.name,
    description: challenge.description,
    survivors: challenge.survivors,
    rankingPoints: challenge.rankingPoints,
    rescueActive,
    target: challenge.targetValue,
    rungs,
    doneCount: rungs.filter((rung) => rung.done).length,
  };
}
