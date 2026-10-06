import { MatchScore } from '../match.model';
import { AGENT_IMAGE_IDS, MAP_IMAGE_FILES, UNKNOWN_AGENT_INITIAL } from './match-format.constants';

/**
 * Uppercased agent initial for agents missing from the bundled portraits, `?` when empty.
 */
export function resolveAgentInitial(agentName: string): string {
  return agentName.trim().charAt(0).toUpperCase() || UNKNOWN_AGENT_INITIAL;
}

/**
 * Bundled portrait path of an agent, `null` to fall back to the monogram.
 */
export function resolveAgentImageUrl(agentName: string): string | null {
  const id = agentName
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
  return AGENT_IMAGE_IDS.has(id) ? `/agents/${id}.webp` : null;
}

/**
 * Bundled image path of a map, `null` when none matches.
 */
export function resolveMapImageUrl(mapName: string): string | null {
  const trimmed = mapName.trim();
  for (const map of MAP_IMAGE_FILES) {
    if (map.toLowerCase() === trimmed.toLowerCase()) {
      return `/maps/${map.toLowerCase()}.webp`;
    }
  }

  return null;
}

/**
 * Split round score so the player's side can be coloured, `null` unless both sides are reported.
 * Checks finiteness: modes without rounds omit the field, which would pass a `!== null` check.
 */
export function resolveMatchScore(
  allyScore: number | null,
  enemyScore: number | null,
): MatchScore | null {
  return Number.isFinite(allyScore) && Number.isFinite(enemyScore)
    ? { ally: allyScore as number, enemy: enemyScore as number }
    : null;
}

/**
 * Translation key explaining a match's damage: unvalued, full or reduced, as the cell shows.
 */
export function resolveDamageHintKey(coefficientPercent: number): string {
  if (coefficientPercent <= 0) {
    return 'playerProfile.matches.damage.unvalued';
  }
  return coefficientPercent >= 100
    ? 'playerProfile.matches.damage.full'
    : 'playerProfile.matches.damage.reduced';
}
