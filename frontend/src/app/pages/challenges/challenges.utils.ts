import { ChallengeProgress, RosterPlayer } from '@core/challenges/challenge.model';
import { localMidnight } from '@core/date/date-time.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { formatFigure } from '../leaderboard/leaderboard-board.utils';
import { MAX_SEGMENTED_TARGET, PINNED_PLAYER_KEY, RULE_NUMBER } from './challenges.constants';
import {
  BoardMark,
  BoardRow,
  ChallengeCard,
  ChallengeLook,
  ChallengeOperator,
  ChallengeRung,
  MarkDetail,
  OperatorProgress,
  RulePart,
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
 * Share of the target one operator reached, in [0, 1]; an open-ended challenge is empty or full.
 */
function progressFraction(target: number | null, value: number, done: boolean): number {
  if (done) {
    return 1;
  }
  return target !== null && target > 0 ? Math.min(1, value / target) : 0;
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
 * An operator's progress laid out for its hover bubble: exact figures and what remains or exceeds.
 *
 * @param rung - The operator's line.
 * @param target - Value to reach, or `null` for an open-ended challenge.
 * @param tone - Accent colour of the challenge.
 * @param locale - `Intl` locale the figures are written in.
 * @returns The bubble's content.
 */
export function toMarkDetail(
  rung: ChallengeRung,
  target: number | null,
  tone: string,
  locale: string,
): MarkDetail {
  const state = rung.done ? 'done' : rung.idle ? 'idle' : 'open';
  const base = { name: rung.name, tone, state, value: formatFigure(rung.value, locale) } as const;
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
    target: formatFigure(target, locale),
    gap,
    gapLabel:
      gap === 'none' ? '' : `${gap === 'surplus' ? '+' : ''}${formatFigure(gapCount, locale)}`,
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

/**
 * The board's operator order: the pinned one first, then by weekly challenges validated, then by
 * weekly progress, the roster's order settling ties.
 *
 * @param operators - The roster, in its own order.
 * @param weekly - The week's challenges and each operator's progress on them.
 * @param pinnedId - The operator the reader pinned, or `null`.
 * @returns The operators in board order.
 */
export function orderOperators(
  operators: readonly ChallengeOperator[],
  weekly: readonly ChallengeProgress[],
  pinnedId: number | null,
): ChallengeOperator[] {
  return operators
    .map((operator, index) => {
      let done = 0;
      let progress = 0;
      for (const challenge of weekly) {
        const line = challenge.players.find((entry) => entry.playerId === operator.playerId);
        const completed = line?.completed ?? false;
        done += completed ? 1 : 0;
        progress += progressFraction(challenge.targetValue, line?.currentValue ?? 0, completed);
      }
      return { operator, index, done, progress, pinned: operator.playerId === pinnedId };
    })
    .sort(
      (left, right) =>
        Number(right.pinned) - Number(left.pinned) ||
        right.done - left.done ||
        right.progress - left.progress ||
        left.index - right.index,
    )
    .map(({ operator }) => operator);
}

/**
 * A compact figure split from its unit (`12,7k` into `12,7` and `k`), so the unit can be set smaller.
 */
export function splitCompactFigure(label: string): { figure: string; unit: string } {
  const match = /^(.*\d)(\D*)$/.exec(label);
  return match ? { figure: match[1], unit: match[2] } : { figure: label, unit: '' };
}

/**
 * One operator's mark on a board row.
 *
 * @param rung - The operator's line on the challenge.
 * @param target - Value to reach, or `null` for an open-ended challenge.
 * @param tip - Worded progress, operator named.
 * @param detail - The exact figures the hover bubble lays out.
 * @returns The mark.
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
 * A challenge card as the board lays it out, one mark per operator in the given order.
 *
 * @param challenge - The drawn challenge, which keys the row.
 * @param card - The challenge as one card shows it.
 * @param rungs - The card's lines, in the order the marks follow.
 * @param closesAt - When a running day's challenge closes; `null` for a weekly one or a closed day.
 * @param locale - `Intl` locale the figures are written in.
 * @param translate - Resolves a full translation key with its parameters.
 * @returns The row.
 */
export function toBoardRow(
  challenge: ChallengeProgress,
  card: ChallengeCard,
  rungs: readonly ChallengeRung[],
  closesAt: number | null,
  locale: string,
  translate: (key: string, params: Record<string, string | number>) => string,
): BoardRow {
  const marks = rungs.map((rung) => {
    const tip = describeRung(rung, card.target, locale, (key, params) =>
      translate(`challenges.card.bandTooltip.${key}`, params),
    );
    const named = translate('challenges.board.markTip', { name: rung.name, tip });
    return toBoardMark(
      rung,
      card.target,
      named,
      toMarkDetail(rung, card.target, card.tone, locale),
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

/**
 * Cuts a rule into plain words and the numbers it holds.
 *
 * @param rule - The translated rule of a challenge.
 * @returns The rule's stretches, in order; joined back they give the rule unchanged.
 */
export function toRuleParts(rule: string): RulePart[] {
  const parts: RulePart[] = [];
  let last = 0;
  for (const match of rule.matchAll(RULE_NUMBER)) {
    if (match.index > last) {
      parts.push({ text: rule.slice(last, match.index), number: false });
    }
    parts.push({ text: match[0], number: true });
    last = match.index + match[0].length;
  }
  if (last < rule.length) {
    parts.push({ text: rule.slice(last), number: false });
  }
  return parts;
}

/**
 * The operator the reader pinned first, or `null`; a malformed value or a storage failure reads as none.
 */
export function readPinnedPlayer(): number | null {
  try {
    const stored = Number(localStorage.getItem(PINNED_PLAYER_KEY));
    return Number.isInteger(stored) && stored > 0 ? stored : null;
  } catch {
    return null;
  }
}

/**
 * Remembers the pinned operator, or forgets it on `null`; storage failures are ignored.
 */
export function writePinnedPlayer(playerId: number | null): void {
  try {
    if (playerId === null) {
      localStorage.removeItem(PINNED_PLAYER_KEY);
    } else {
      localStorage.setItem(PINNED_PLAYER_KEY, String(playerId));
    }
  } catch {
    // Nothing to do: the board opens in its own order next time.
  }
}
