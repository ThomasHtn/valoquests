/**
 * Placeholder initial used when a match carries no agent name.
 */
const UNKNOWN_AGENT_INITIAL = '?';

/**
 * Resolves the monogram standing in for an agent's portrait.
 *
 * Falls back for agents released after the bundled portrait set, so a match played on one of them
 * still anchors the row the same way as the others.
 *
 * @param agentName - The agent played during the match.
 * @returns The agent's initial, uppercased, or a question mark when the name is empty.
 */
export function resolveAgentInitial(agentName: string): string {
  return agentName.trim().charAt(0).toUpperCase() || UNKNOWN_AGENT_INITIAL;
}

/**
 * Agent portraits bundled under `public/agents`, keyed by their Riot API agent id (the agent's
 * display name lowercased with punctuation stripped, e.g. `"KAY/O"` -> `"kayo"`).
 */
const AGENT_IMAGE_IDS: ReadonlySet<string> = new Set([
  'astra',
  'breach',
  'brimstone',
  'chamber',
  'clove',
  'cypher',
  'deadlock',
  'fade',
  'gekko',
  'harbor',
  'iso',
  'jett',
  'kayo',
  'killjoy',
  'miks',
  'neon',
  'omen',
  'phoenix',
  'raze',
  'reyna',
  'sage',
  'skye',
  'sova',
  'tejo',
  'veto',
  'viper',
  'vyse',
  'waylay',
  'yoru',
]);

/**
 * Resolves the local portrait matching a match's agent.
 *
 * @param agentName - The agent played during the match.
 * @returns The public asset path to the agent's portrait, or `null` when no bundled image matches,
 * so callers can fall back to the agent monogram.
 */
export function resolveAgentImageUrl(agentName: string): string | null {
  const id = agentName
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
  return AGENT_IMAGE_IDS.has(id) ? `/agents/${id}.png` : null;
}

/**
 * Competitive map images bundled under `public/maps`, keyed by their exact Valorant map name.
 */
const MAP_IMAGE_FILES: ReadonlySet<string> = new Set([
  'Abyss',
  'Ascent',
  'Bind',
  'Breeze',
  'Corrode',
  'Fracture',
  'Haven',
  'Icebox',
  'Lotus',
  'Pearl',
  'Split',
  'Summit',
  'Sunset',
]);

/**
 * Resolves the local map image matching a match's map name.
 *
 * @param mapName - The map a match was played on.
 * @returns The public asset path to the map's image, or `null` when no bundled image matches, so
 * callers can fall back to the agent monogram.
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
 * A match's round score, split so each side can be rendered with its own treatment.
 */
export interface MatchScore {
  /**
   * Rounds won by the player's team.
   */
  readonly ally: number;

  /**
   * Rounds won by the opposing team.
   */
  readonly enemy: number;
}

/**
 * Resolves a match's round score, when the mode reports one.
 *
 * Returned split rather than pre-joined so callers can colour the player's own score by the match
 * result: `"10 : 13"` on its own does not say which side the player was on.
 *
 * Both sides are required: a match reports either both scores or neither, and a half-filled score
 * would render as a bare separator. Guards against `undefined` as well as `null`, since the field
 * is absent from the payload rather than null on the modes without rounds (deathmatch, escalation),
 * and an absent field would otherwise pass a `!== null` check.
 *
 * @param allyScore - Rounds won by the player's team, when reported.
 * @param enemyScore - Rounds won by the opposing team, when reported.
 * @returns The score, or `null` when the mode reports none, so callers can render an explicit
 * placeholder rather than an empty cell.
 */
export function resolveMatchScore(
  allyScore: number | null,
  enemyScore: number | null,
): MatchScore | null {
  return Number.isFinite(allyScore) && Number.isFinite(enemyScore)
    ? { ally: allyScore as number, enemy: enemyScore as number }
    : null;
}
