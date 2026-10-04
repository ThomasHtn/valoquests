import { EPISODE_SEASON_PATTERN, YEAR_SEASON_PATTERN } from './season-name.constants';
import { SeasonParts } from './season-name.model';

/**
 * Riot-style label of a raw season code, or the code itself when no known era matches.
 */
export function formatSeasonName(
  name: string,
  translate: (key: string, params?: Readonly<Record<string, string | number>>) => string,
): string {
  const episode = EPISODE_SEASON_PATTERN.exec(name);
  if (episode) {
    return translate('seasons.episode', { episode: Number(episode[1]), act: Number(episode[2]) });
  }

  const year = YEAR_SEASON_PATTERN.exec(name);
  if (year) {
    return translate('seasons.year', { year: 2000 + Number(year[1]), act: Number(year[2]) });
  }

  return name;
}

/**
 * Era and act of a raw season code for narrow labels, `null` when no known era matches.
 */
export function splitSeasonName(
  name: string,
  translate: (key: string, params?: Readonly<Record<string, string | number>>) => string,
): SeasonParts | null {
  const episode = EPISODE_SEASON_PATTERN.exec(name);
  if (episode) {
    return {
      era: translate('seasons.episodeOnly', { episode: Number(episode[1]) }),
      eraMark: translate('seasons.episodeMark', { episode: Number(episode[1]) }),
      act: Number(episode[2]),
    };
  }

  const year = YEAR_SEASON_PATTERN.exec(name);
  if (year) {
    const era = String(2000 + Number(year[1]));
    return { era, eraMark: era, act: Number(year[2]) };
  }

  return null;
}
