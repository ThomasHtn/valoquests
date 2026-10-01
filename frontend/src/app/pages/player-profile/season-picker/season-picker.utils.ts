import { TranslateFn } from '@core/i18n/translation.model';
import { Season } from '@core/matches/season.model';
import { formatSeasonName, splitSeasonName } from '@core/matches/season-name.utils';
import { SeasonPickerOption } from './season-picker.model';

/**
 * Turns the seasons into picker options, keeping their order (newest first).
 *
 * @param seasons - Every known season, newest first.
 * @param translate - Dictionary lookup.
 * @returns One option per season.
 */
export function buildSeasonPickerOptions(
  seasons: readonly Season[],
  translate: TranslateFn,
): readonly SeasonPickerOption[] {
  return seasons.map((season) => {
    const parts = splitSeasonName(season.name, translate);
    const fullName = formatSeasonName(season.name, translate);
    return parts
      ? {
          id: season.id,
          mark: parts.eraMark,
          label: translate('seasons.actOnly', { act: parts.act }),
          fullName,
          era: parts.era,
        }
      : { id: season.id, mark: null, label: fullName, fullName, era: season.name };
  });
}
