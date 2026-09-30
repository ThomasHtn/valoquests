import { daysBetween, localMidnight } from '@core/date/date-time.utils';
import { Language } from '@core/i18n/translation.model';
import { StreakPip } from './streak-gauge.model';

/**
 * Bonus a streak of that many days pays: nothing on the first day, two percent per day after,
 * capped at ten — the barème's ladder, restated for an operator who has not played yet and whose
 * streak the daily board therefore does not price.
 */
export function streakBonusOf(streakDays: number): number {
  return Math.max(0, Math.min(10, (streakDays - 1) * 2));
}

/**
 * Lays out the week from Monday to Sunday around a day, marking the days an operator played.
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
 * Initials of the days of the week, Monday first (`L M M J V S D`, `M T W T F S S`).
 *
 * @param language - The reader's language.
 * @returns Seven single-letter labels.
 */
export function weekdayInitials(language: Language): readonly string[] {
  const format = new Intl.DateTimeFormat(language, { weekday: 'narrow', timeZone: 'UTC' });
  // 2024-01-01 is a Monday; read at noon UTC so no zone shifts the weekday.
  return Array.from({ length: 7 }, (_, index) =>
    format.format(new Date(Date.UTC(2024, 0, 1 + index, 12))),
  );
}
