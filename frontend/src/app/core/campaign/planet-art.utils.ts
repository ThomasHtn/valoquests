import { CAMPAIGN_WEEK_COUNT } from './campaign.model';
import { PLANET_ART_FOLDER, RINGED_PLANET_RANKS } from './planet-art.constants';

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
  return `${PLANET_ART_FOLDER}/planet-${String(resolvePlanetRank(weekIndex)).padStart(2, '0')}.svg`;
}

/**
 * Tells whether a week's drawing carries rings reaching past its globe.
 *
 * @param weekIndex - One-based index of the week in the campaign.
 * @returns `true` for the ringed drawings, which need room around the globe.
 */
export function isRingedPlanet(weekIndex: number): boolean {
  return RINGED_PLANET_RANKS.includes(resolvePlanetRank(weekIndex));
}

/**
 * Clamps a week index onto the rank of an existing drawing.
 *
 * @param weekIndex - One-based index of the week in the campaign.
 * @returns The drawing's rank, between 1 and the campaign's week count.
 */
function resolvePlanetRank(weekIndex: number): number {
  return Math.min(CAMPAIGN_WEEK_COUNT, Math.max(1, Math.round(weekIndex)));
}
