import { TranslateFn } from '@core/i18n/translation.model';
import { Season } from '@core/seasons/season.model';
import { formatSeasonName, splitSeasonName } from '@core/seasons/season-name.utils';
import { SeasonPickerOption } from './season-picker.model';

/**
 * Picker options of the seasons, newest first as given.
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
