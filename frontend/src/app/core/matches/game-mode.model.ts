/**
 * Every game mode a stored match can carry.
 *
 * Declared as a runtime list rather than as a bare union so {@link GameMode} is derived from it,
 * which keeps the type and the values in sync by construction.
 *
 * Mirrors the backend `GameMode` enum, including the modes synchronization no longer imports: the
 * enum still declares them, so a match persisted before the import filter narrowed can still be
 * returned with one of those values and must still resolve to a label.
 */
export const GAME_MODES = [
  'COMPETITIVE',
  'UNRATED',
  'SWIFTPLAY',
  'NEW_MAP',
  'SPIKE_RUSH',
  'DEATHMATCH',
  'TEAM_DEATHMATCH',
  'ESCALATION',
  'SKIRMISH',
  'PREMIER',
  'CUSTOM',
  'OTHER',
] as const;

/**
 * Game mode played during a match.
 */
export type GameMode = (typeof GAME_MODES)[number];

/**
 * Game modes offered as a filter, in order of importance: the first ones get their own button, the
 * rest fall into the overflow menu as the screen narrows.
 *
 * A subset of the modes synchronization imports (backend `GameMode#isImportEligible()`). New Map
 * and custom games are never stored, so offering them would only ever yield the empty state;
 * Premier and the unclassified `OTHER` bucket are stored but left out, being too rare to earn a
 * place in the filter.
 *
 * Kept as a subset of {@link GAME_MODES} rather than replacing it, since a match returned by the
 * API may still carry a mode that is not offered here.
 */
export const FILTERABLE_GAME_MODES = [
  'COMPETITIVE',
  'UNRATED',
  'DEATHMATCH',
  'TEAM_DEATHMATCH',
  'SKIRMISH',
  'SWIFTPLAY',
  'SPIKE_RUSH',
  'ESCALATION',
] as const satisfies readonly GameMode[];
