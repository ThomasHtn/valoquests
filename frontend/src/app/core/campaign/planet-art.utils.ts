import { CAMPAIGN_WEEK_COUNT } from './campaign.model';
import { PLANET_ART_FOLDER } from './planet-art.constants';

/**
 * Resolves the drawing of a week's planet.
 *
 * The drawing follows the week's rank on the road, not the planet's name: the tenth week always
 * gets the ringed giant, whatever the campaign drew for it.
 *
 * @param weekIndex - One-based index of the week in the campaign.
 * @returns The public path of the planet's SVG.
 */
export function resolvePlanetArtUrl(weekIndex: number): string {
  const rank = Math.min(CAMPAIGN_WEEK_COUNT, Math.max(1, Math.round(weekIndex)));
  return `${PLANET_ART_FOLDER}/planet-${String(rank).padStart(2, '0')}.svg`;
}
