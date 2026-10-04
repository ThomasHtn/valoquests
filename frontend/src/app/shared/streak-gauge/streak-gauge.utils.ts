import { daysBetween, localMidnight } from '@core/date/date.utils';
import { Language } from '@core/i18n/translation.model';
import { StreakPip } from './streak-gauge.model';

/**
 * Streak bonus in percent (0 on day one, +2 per day, max 10) for operators not yet priced.
 */
export function streakBonusOf(streakDays: number): number {
  return Math.max(0, Math.min(10, (streakDays - 1) * 2));
}

/**
 * Week around `day`, Monday first, marking the days played.
 */
export function streakWeekOf(day: string, playedDays: readonly string[]): readonly StreakPip[] {
  const todayIndex = (localMidnight(day).getDay() + 6) % 7;
  const played = new Set(playedDays.map((playedDay) => todayIndex - daysBetween(playedDay, day)));
  return Array.from({ length: 7 }, (_, index): StreakPip => {
    if (played.has(index)) {
      return 'played';
    }
    // Today reads as played or not, never as pending.
    return index <= todayIndex ? 'missed' : 'ahead';
  });
}

/**
 * Weekday initials, Monday first (`L M M J V S D`).
 */
export function weekdayInitials(language: Language): readonly string[] {
  const format = new Intl.DateTimeFormat(language, { weekday: 'narrow', timeZone: 'UTC' });
  // 2024-01-01 is a Monday; read at noon UTC so no zone shifts the weekday.
  return Array.from({ length: 7 }, (_, index) =>
    format.format(new Date(Date.UTC(2024, 0, 1 + index, 12))),
  );
}
