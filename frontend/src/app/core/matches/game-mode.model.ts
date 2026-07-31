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
 * Game modes synchronization actually imports, in the order they are offered as a filter.
 *
 * Mirrors the backend `GameMode#isImportEligible()`. Swiftplay, New Map, Escalation and custom
 * games are deliberately absent: no match of those modes is ever stored, so offering them would
 * only ever yield the empty state.
 *
 * `OTHER` is offered despite naming no mode in particular: it is imported on purpose, as the
 * bucket for a queue the backend cannot classify yet, and is therefore the only way to reach those
 * matches.
 *
 * Kept as a subset of {@link GAME_MODES} rather than replacing it, since a match returned by the
 * API may still carry a mode that is no longer imported.
 */
export const FILTERABLE_GAME_MODES = [
  'COMPETITIVE',
  'UNRATED',
  'PREMIER',
  'SPIKE_RUSH',
  'SKIRMISH',
  'DEATHMATCH',
  'TEAM_DEATHMATCH',
  'OTHER',
] as const satisfies readonly GameMode[];
