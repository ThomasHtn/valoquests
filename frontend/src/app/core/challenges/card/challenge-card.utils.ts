import { formatFigure } from '@core/i18n/format/number-format.utils';
import { Language, TranslateFn } from '@core/i18n/translation.model';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { ChallengeProgress, RosterPlayer } from '../challenge.model';
import { MAX_SEGMENTED_TARGET } from './challenge-card.constants';
import {
  BoardMark,
  BoardRow,
  ChallengeCard,
  ChallengeLook,
  ChallengeOperator,
  ChallengeRung,
  MarkDetail,
  OperatorProgress,
} from './challenge-card.model';

/**
 * Share of the target reached, in [0, 1]; an open-ended challenge is empty or full.
 */
export function progressFraction(target: number | null, value: number, done: boolean): number {
  if (done) {
    return 1;
  }
  return target !== null && target > 0 ? Math.min(1, value / target) : 0;
}

/**
 * One card line per operator, furthest along first, roster order settling ties.
 */
function buildRungs(
  operators: readonly ChallengeOperator[],
  target: number | null,
  progressOf: (playerId: number) => OperatorProgress,
  format: (amount: number) => string,
): ChallengeRung[] {
  return operators
    .map((operator, index) => {
      const { value, done } = progressOf(operator.playerId);
      const rung: ChallengeRung = {
        playerId: operator.playerId,
        name: operator.name,
        portrait: operator.portrait,
        fraction: progressFraction(target, value, done),
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
 * Band tooltip: exact figures behind the compact labels, and what remains.
 */
export function describeRung(
  rung: ChallengeRung,
  target: number | null,
  language: Language,
  translate: TranslateFn,
): string {
  const value = formatFigure(rung.value, language);
  if (target === null || target <= 0) {
    return translate(rung.done ? 'openEndedDone' : 'openEnded', { value });
  }
  const params = { value, target: formatFigure(target, language) };
  if (rung.done) {
    return translate('done', params);
  }
  const remaining = Math.max(0, target - rung.value);
  // `count` picks the plural branch of the remaining units.
  return translate('open', {
    ...params,
    remaining: formatFigure(remaining, language),
    count: remaining,
  });
}

/**
 * Hover bubble content: exact figures and what remains or exceeds.
 */
export function toMarkDetail(
  rung: ChallengeRung,
  target: number | null,
  tone: string,
  language: Language,
): MarkDetail {
  const state = rung.done ? 'done' : rung.idle ? 'idle' : 'open';
  const base = { name: rung.name, tone, state, value: formatFigure(rung.value, language) } as const;
  if (target === null || target <= 0) {
    return {
      ...base,
      target: '',
      gap: 'none',
      gapLabel: '',
      gapCount: 0,
    };
  }
  const surplus = rung.value - target;
  const gap = surplus > 0 ? 'surplus' : rung.done ? 'none' : 'remaining';
  const gapCount = gap === 'surplus' ? surplus : gap === 'remaining' ? target - rung.value : 0;
  return {
    ...base,
    target: formatFigure(target, language),
    gap,
    gapLabel:
      gap === 'none' ? '' : `${gap === 'surplus' ? '+' : ''}${formatFigure(gapCount, language)}`,
    gapCount,
  };
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

/**
 * Splits `12,7k` into `12,7` and `k` so the unit can be set smaller.
 */
export function splitCompactFigure(label: string): { figure: string; unit: string } {
  const match = /^(.*\d)(\D*)$/.exec(label);
  return match ? { figure: match[1], unit: match[2] } : { figure: label, unit: '' };
}

/**
 * One operator's mark on a board row.
 */
export function toBoardMark(
  rung: ChallengeRung,
  target: number | null,
  tip: string,
  detail: MarkDetail,
): BoardMark {
  const segmented = target !== null && target >= 1 && target <= MAX_SEGMENTED_TARGET;
  return {
    ...rung,
    ...splitCompactFigure(rung.valueLabel),
    tip,
    detail,
    segments: segmented
      ? Array.from({ length: target }, (_, index) => rung.done || index < Math.floor(rung.value))
      : [],
  };
}

/**
 * A card as a board row, one mark per rung; `closesAt` is `null` for a weekly or closed day.
 */
export function toBoardRow(
  challenge: ChallengeProgress,
  card: ChallengeCard,
  rungs: readonly ChallengeRung[],
  closesAt: number | null,
  language: Language,
  translate: TranslateFn,
): BoardRow {
  const marks = rungs.map((rung) => {
    const tip = describeRung(rung, card.target, language, (key, params) =>
      translate(`challenges.card.bandTooltip.${key}`, params),
    );
    const named = translate('challenges.board.markTip', { name: rung.name, tip });
    return toBoardMark(
      rung,
      card.target,
      named,
      toMarkDetail(rung, card.target, card.tone, language),
    );
  });
  return {
    ...card,
    key: `${challenge.cadence}-${challenge.id}-${challenge.day ?? ''}`,
    daily: challenge.cadence === 'DAILY',
    closesAt,
    marks,
  };
}
