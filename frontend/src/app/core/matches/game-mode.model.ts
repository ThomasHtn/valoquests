/**
 * Game mode played during a match.
 *
 * Mirrors the backend `GameMode` enum.
 */
export type GameMode =
  | 'COMPETITIVE'
  | 'UNRATED'
  | 'SWIFTPLAY'
  | 'SPIKE_RUSH'
  | 'DEATHMATCH'
  | 'TEAM_DEATHMATCH'
  | 'ESCALATION'
  | 'PREMIER'
  | 'CUSTOM'
  | 'OTHER';
