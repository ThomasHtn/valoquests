import { GameMode } from './match-game-mode.model';

/**
 * Every stored game mode, source of `GameMode`. Mirrors the backend enum, legacy included.
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
 * Filter modes by importance, last ones overflow first; rare or never-imported modes left out.
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
