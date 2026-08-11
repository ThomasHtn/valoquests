import { BossCategory } from '@core/boss/boss.model';
import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { GameMode } from '@core/matches/game-mode.model';

/**
 * One difficulty tier shown in the challenges step's damage ladder, from easiest to hardest.
 */
interface DifficultyShowcase {
  readonly difficulty: ChallengeDifficulty;
  readonly damage: number;
  readonly colorClass: string;
}

/**
 * Damage ladder shown alongside the challenges step, colored the same way as the weekly
 * challenges card (`challenge-visual.utils.ts`) so both pages read as one system.
 */
export const DIFFICULTY_SHOWCASE: readonly DifficultyShowcase[] = [
  { difficulty: 'EASY', damage: 1_500, colorClass: 'bg-accent-green/15 text-accent-green' },
  { difficulty: 'NORMAL', damage: 2_500, colorClass: 'bg-accent-blue/15 text-accent-blue' },
  { difficulty: 'MEDIUM', damage: 4_000, colorClass: 'bg-accent-gold/15 text-accent-gold' },
  { difficulty: 'HARD', damage: 6_000, colorClass: 'bg-accent-pink/15 text-accent-pink' },
  { difficulty: 'VERY_HARD', damage: 9_000, colorClass: 'bg-accent-red/15 text-accent-red' },
];

/**
 * Boss weight classes shown in the boss step, weakest to strongest.
 */
export const BOSS_CATEGORY_SHOWCASE: readonly BossCategory[] = ['MINOR', 'STANDARD', 'ELITE'];

/**
 * Damage a single synced match awards for one game mode, by outcome.
 *
 * `draw` is `null` for the two modes that cannot end on a draw (`SPIKE_RUSH`, `DEATHMATCH`).
 * Mirrors `ScoringRulesetV1#matchDamage` on the backend; that class is frozen once published (see
 * its own doc comment), so these values are safe to transcribe here as-is.
 */
interface MatchDamageShowcase {
  readonly mode: GameMode;
  readonly win: number;
  readonly draw: number | null;
  readonly loss: number;
}

/**
 * Damage ladder shown in the damage step, competitive first as the mode worth the most.
 */
export const MATCH_DAMAGE_SHOWCASE: readonly MatchDamageShowcase[] = [
  { mode: 'COMPETITIVE', win: 500, draw: 425, loss: 350 },
  { mode: 'UNRATED', win: 400, draw: 340, loss: 280 },
  { mode: 'SPIKE_RUSH', win: 180, draw: null, loss: 130 },
  { mode: 'SKIRMISH', win: 170, draw: 145, loss: 120 },
  { mode: 'TEAM_DEATHMATCH', win: 160, draw: 135, loss: 110 },
  { mode: 'DEATHMATCH', win: 150, draw: null, loss: 100 },
];

/**
 * One tier of the regularity bonus: extra damage awarded once the group has been active on this
 * many distinct days in the week. Mirrors `ScoringRulesetV1#regularityBonus`; a single active day
 * earns nothing, so the ladder starts at two.
 */
interface RegularityBonusShowcase {
  readonly days: number;
  readonly bonus: number;
}

export const REGULARITY_BONUS_SHOWCASE: readonly RegularityBonusShowcase[] = [
  { days: 2, bonus: 300 },
  { days: 3, bonus: 700 },
  { days: 4, bonus: 1_200 },
  { days: 5, bonus: 1_800 },
  { days: 6, bonus: 2_400 },
  { days: 7, bonus: 3_000 },
];

/**
 * One tier of the team bonus: extra damage awarded once this many players have completed the same
 * challenge. Mirrors `ScoringRulesetV1#teamBonus`; a single player earns nothing, and the fixed
 * roster's potential 7th player is capped to the 6-player tier rather than extrapolated, so the
 * ladder stops there.
 */
interface TeamBonusShowcase {
  readonly playersLabel: string;
  readonly bonus: number;
}

export const TEAM_BONUS_SHOWCASE: readonly TeamBonusShowcase[] = [
  { playersLabel: '2', bonus: 150 },
  { playersLabel: '3', bonus: 300 },
  { playersLabel: '4', bonus: 500 },
  { playersLabel: '5', bonus: 750 },
  { playersLabel: '6+', bonus: 1_100 },
];
