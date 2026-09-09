/**
 * Game modes that feed each stock the most, as the scoring splits a match's value.
 */
export const CARRY_MODES: readonly string[] = ['COMPETITIVE', 'PREMIER', 'UNRATED'];

/**
 * Game modes whose value goes mostly to food, the shelter stock.
 */
export const SHELTER_MODES: readonly string[] = [
  'DEATHMATCH',
  'SPIKE_RUSH',
  'TEAM_DEATHMATCH',
  'SKIRMISH',
];
