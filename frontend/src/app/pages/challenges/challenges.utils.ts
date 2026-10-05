import { ChallengeProgress } from '@core/challenges/challenge.model';
import { ChallengeOperator } from '@core/challenges/card/challenge-card.model';
import { progressFraction } from '@core/challenges/card/challenge-card.utils';
import { localMidnight } from '@core/date/date.utils';
import { RULE_NUMBER } from './challenges.constants';
import { DayCell, DayPickSource, RulePart } from './challenges.model';

/**
 * Default picked day: the last drawn day not ahead, `null` before the first draw.
 */
export function defaultPickedDay(days: readonly DayCell[]): number | null {
  return days.filter((day) => day.drawn && day.state !== 'ahead').at(-1)?.index ?? null;
}

/**
 * Keeps the reader's pick across a reload; a new week or an undrawn pick falls back to default.
 */
export function resolvePickedDay(
  source: DayPickSource,
  previous?: { readonly source: DayPickSource; readonly value: number | null },
): number | null {
  const fallback = defaultPickedDay(source.days);
  if (!previous || previous.source.weekStart !== source.weekStart) {
    return fallback;
  }
  const picked = previous.value;
  const wasFollowing = picked === defaultPickedDay(previous.source.days);
  const stillDrawn = source.days.some(
    (day) => day.index === picked && day.drawn && day.state !== 'ahead',
  );
  return !wasFollowing && stillDrawn ? picked : fallback;
}

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
 * Board order: pinned first, then validated count, then progress, roster order on ties.
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
 * Cuts a rule into words and numbers; joined back they give the rule unchanged.
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
 * Day name, capitalised and without the trailing dot when short (`Lun`).
 */
export function formatWeekday(isoDate: string, locale: string, width: 'short' | 'long'): string {
  const label = new Intl.DateTimeFormat(locale, { weekday: width }).format(localMidnight(isoDate));
  if (width === 'long') {
    return label;
  }
  const bare = label.replace('.', '');
  return bare.charAt(0).toUpperCase() + bare.slice(1);
}

/**
 * Day and spelled-out month, for the week's span (`5 octobre`).
 */
export function formatDayMonth(isoDate: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long' }).format(
    localMidnight(isoDate),
  );
}

/**
 * Day of the month alone, under a day cell's weekday (`5`).
 */
export function formatDayOfMonth(isoDate: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { day: 'numeric' }).format(localMidnight(isoDate));
}
