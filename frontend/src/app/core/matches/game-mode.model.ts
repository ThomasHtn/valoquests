/**
 * Game modes a match can be played in, in the order they are offered as a filter.
 *
 * Declared as a runtime list rather than as a bare union so the match-history filter can enumerate
 * them without repeating the values; {@link GameMode} is derived from it, which keeps the two in
 * sync by construction.
 *
 * Mirrors the backend `GameMode` enum.
 */
export const GAME_MODES = [
  'COMPETITIVE',
  'UNRATED',
  'SWIFTPLAY',
  'SPIKE_RUSH',
  'DEATHMATCH',
  'TEAM_DEATHMATCH',
  'ESCALATION',
  'PREMIER',
  'CUSTOM',
  'OTHER',
] as const;

/**
 * Game mode played during a match.
 */
export type GameMode = (typeof GAME_MODES)[number];
