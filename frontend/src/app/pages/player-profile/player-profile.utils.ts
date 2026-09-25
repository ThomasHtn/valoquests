import { Season } from '@core/matches/season.model';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';

/**
 * Pure helpers of the player-profile page.
 */

/**
 * Resolves the id of the current season - the one flagged `active` - or the first (most-recent)
 * known season if none is active, or `null` if none are known yet.
 */
export function resolveCurrentSeasonId(seasons: readonly Season[]): number | null {
  return (seasons.find((season) => season.active) ?? seasons[0])?.id ?? null;
}

/**
 * Colour of the share the next match keeps: amber at full value, muted once the ladder has
 * started taking a cut. A red would overstate a rule that still pays half.
 */
export function resolveYieldToneClass(percent: number): string {
  return percent >= 100 ? 'text-brand-500' : 'text-text-secondary';
}

/**
 * Builds the plate shown when the route names a player or match the backend does not know.
 *
 * @param translate - Resolves a translation key.
 * @param keyPrefix - i18n prefix holding the plate's `title` and `text`.
 * @param address - The URL that led here, echoed like the generic not-found page does.
 * @returns The plate, already translated.
 */
export function buildNotFoundPlate(
  translate: (key: string) => string,
  keyPrefix: string,
  address: string,
): EmptyPlate {
  return {
    illustration: 'radar',
    eyebrow: translate('notFound.eyebrow'),
    title: translate(`${keyPrefix}.title`),
    text: translate(`${keyPrefix}.text`),
    readouts: [{ tone: 'info', label: translate('notFound.address'), value: address }],
  };
}
