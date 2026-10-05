import { TranslateFn } from '@core/i18n/translation.model';
import { EPISODE_SEASON_PATTERN, YEAR_SEASON_PATTERN } from './season-name.constants';
import { SeasonCode, SeasonParts } from './season-name.model';

/**
 * Era and act of a raw season code, `null` when no known era matches.
 */
function parseSeasonCode(name: string): SeasonCode | null {
  const episode = EPISODE_SEASON_PATTERN.exec(name);
  if (episode) {
    return { era: 'episode', number: Number(episode[1]), act: Number(episode[2]) };
  }

  const year = YEAR_SEASON_PATTERN.exec(name);
  if (year) {
    return { era: 'year', number: 2000 + Number(year[1]), act: Number(year[2]) };
  }

  return null;
}

/**
 * Riot-style label of a raw season code, or the code itself when no known era matches.
 */
export function formatSeasonName(name: string, translate: TranslateFn): string {
  const code = parseSeasonCode(name);
  if (!code) {
    return name;
  }

  return code.era === 'episode'
    ? translate('seasons.episode', { episode: code.number, act: code.act })
    : translate('seasons.year', { year: code.number, act: code.act });
}

/**
 * Era and act of a raw season code for narrow labels, `null` when no known era matches.
 */
export function splitSeasonName(name: string, translate: TranslateFn): SeasonParts | null {
  const code = parseSeasonCode(name);
  if (!code) {
    return null;
  }

  if (code.era === 'episode') {
    return {
      era: translate('seasons.episodeOnly', { episode: code.number }),
      eraMark: translate('seasons.episodeMark', { episode: code.number }),
      act: code.act,
    };
  }

  const era = String(code.number);
  return { era, eraMark: era, act: code.act };
}
