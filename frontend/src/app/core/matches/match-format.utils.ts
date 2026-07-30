/**
 * Placeholder initial used when a match carries no agent name.
 */
const UNKNOWN_AGENT_INITIAL = '?';

/**
 * Resolves the monogram standing in for an agent's portrait.
 *
 * The application bundles portraits for six agents only, so a match played on any other agent has
 * no artwork to show; a monogram keeps every row anchored the same way until real assets exist.
 *
 * @param agentName - The agent played during the match.
 * @returns The agent's initial, uppercased, or a question mark when the name is empty.
 */
export function resolveAgentInitial(agentName: string): string {
  return agentName.trim().charAt(0).toUpperCase() || UNKNOWN_AGENT_INITIAL;
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
