import { CAMPAIGN_WEEK_COUNT } from '../campaign.constants';
import { PLANET_ART_FOLDER, RINGED_PLANET_RANKS } from './campaign-planet-art.constants';

/**
 * SVG path of a week's planet (one-based `weekIndex`), chosen by rank, not by planet name.
 */
export function resolvePlanetArtUrl(weekIndex: number): string {
  return `${PLANET_ART_FOLDER}/planet-${String(resolvePlanetRank(weekIndex)).padStart(2, '0')}.svg`;
}

/**
 * Whether a week's drawing has rings needing room around the globe.
 */
export function isRingedPlanet(weekIndex: number): boolean {
  return RINGED_PLANET_RANKS.includes(resolvePlanetRank(weekIndex));
}

/**
 * Clamps a one-based week index to an existing drawing rank.
 */
function resolvePlanetRank(weekIndex: number): number {
  return Math.min(CAMPAIGN_WEEK_COUNT, Math.max(1, Math.round(weekIndex)));
}
