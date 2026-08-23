import { BossCategory } from '@core/boss/boss.model';
import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { ChallengeVisual } from '@core/challenges/challenge-visual.model';
import { resolveDifficultyVisual } from '@core/challenges/challenge-visual.utils';
import { GameMode } from '@core/matches/game-mode.model';

/**
 * One difficulty tier shown in the challenges step's damage ladder, from easiest to hardest.
 */
interface DifficultyShowcase {
  readonly difficulty: ChallengeDifficulty;
  readonly damage: number;
  readonly visual: Omit<ChallengeVisual, 'icon'>;
}

/**
 * Damage ladder shown alongside the challenges step, colored the same way as the weekly
 * challenges board (`challenge-visual.utils.ts`) so both pages read as one system.
 */
export const DIFFICULTY_SHOWCASE: readonly DifficultyShowcase[] = (
  [
    ['EASY', 1_500],
    ['NORMAL', 2_500],
    ['MEDIUM', 4_000],
    ['HARD', 6_000],
    ['VERY_HARD', 9_000],
  ] as const
).map(([difficulty, damage]) => ({
  difficulty,
  damage,
  visual: resolveDifficultyVisual(difficulty),
}));

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
 * One step of a bonus ladder.
 */
interface BonusShowcase {
  readonly label: string;
  readonly bonus: number;
}

/**
 * One tier of the regularity bonus: extra damage awarded to a player who played on this many
 * distinct days in the week — it is counted per player, not for the group. Mirrors
 * `ScoringRulesetV1#regularityBonus`; a single active day earns nothing, so the ladder starts at two.
 */
export const REGULARITY_BONUS_SHOWCASE: readonly BonusShowcase[] = [
  { label: '2', bonus: 300 },
  { label: '3', bonus: 700 },
  { label: '4', bonus: 1_200 },
  { label: '5', bonus: 1_800 },
  { label: '6', bonus: 2_400 },
  { label: '7', bonus: 3_000 },
];

/**
 * One tier of the team bonus: extra damage awarded once this many players have completed the same
 * challenge. Mirrors `ScoringRulesetV1#teamBonus`; a single player earns nothing, and the fixed
 * roster's potential 7th player is capped to the 6-player tier rather than extrapolated, so the
 * ladder stops there.
 */
export const TEAM_BONUS_SHOWCASE: readonly BonusShowcase[] = [
  { label: '2', bonus: 150 },
  { label: '3', bonus: 300 },
  { label: '4', bonus: 500 },
  { label: '5', bonus: 750 },
  { label: '6+', bonus: 1_100 },
];

/**
 * What the weekly ranking is made of, as the numbered facts closing the page.
 *
 * Each entry is the last segment of its `rules.sections.ranking.facts.*` translation key.
 */
export const RANKING_FACTS: readonly string[] = [
  'damage',
  'championTitle',
  'crown',
  'finishingBlow',
];
