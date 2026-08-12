import { BossCategory } from '@core/boss/boss.model';
import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { ChallengeVisual } from '@core/challenges/challenge-visual.model';
import { resolveDifficultyVisual } from '@core/challenges/challenge-visual.utils';
import { GameMode } from '@core/matches/game-mode.model';

/**
 * The four beats of the weekly loop, in order.
 *
 * Each entry is the middle segment of its `rules.steps.*` translation keys; the step's number is
 * its position in this list, so neither the copy nor the markup has to repeat it.
 */
export const RULES_STEPS: readonly string[] = ['challenges', 'damage', 'boss', 'ranking'];

/**
 * One difficulty tier shown in the challenges step's damage ladder, from easiest to hardest.
 */
interface DifficultyShowcase {
  readonly difficulty: ChallengeDifficulty;
  readonly damage: number;
  /** Share of the hardest tier's damage, driving the row's inline bar. */
  readonly share: number;
  readonly visual: Omit<ChallengeVisual, 'icon'>;
}

/**
 * Damage awarded by the hardest tier, the reference the ladder's bars are scaled against.
 */
const HARDEST_TIER_DAMAGE = 9_000;

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
    ['VERY_HARD', HARDEST_TIER_DAMAGE],
  ] as const
).map(([difficulty, damage]) => ({
  difficulty,
  damage,
  share: Math.round((damage / HARDEST_TIER_DAMAGE) * 100),
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
 *
 * `intensityClass` ramps the tile's brand tint and top rule with the reward, so a ladder reads as
 * a climb before any number is parsed. Written as literal utilities rather than computed opacities
 * because Tailwind resolves class names statically.
 */
interface BonusShowcase {
  readonly label: string;
  readonly bonus: number;
  readonly intensityClass: string;
}

/**
 * One tier of the regularity bonus: extra damage awarded once the group has been active on this
 * many distinct days in the week. Mirrors `ScoringRulesetV1#regularityBonus`; a single active day
 * earns nothing, so the ladder starts at two.
 */
export const REGULARITY_BONUS_SHOWCASE: readonly BonusShowcase[] = [
  { label: '2', bonus: 300, intensityClass: 'bg-brand-500/5 border-brand-500/30' },
  { label: '3', bonus: 700, intensityClass: 'bg-brand-500/8 border-brand-500/40' },
  { label: '4', bonus: 1_200, intensityClass: 'bg-brand-500/10 border-brand-500/55' },
  { label: '5', bonus: 1_800, intensityClass: 'bg-brand-500/12 border-brand-500/70' },
  { label: '6', bonus: 2_400, intensityClass: 'bg-brand-500/15 border-brand-500/85' },
  { label: '7', bonus: 3_000, intensityClass: 'bg-brand-500/20 border-brand-500' },
];

/**
 * One tier of the team bonus: extra damage awarded once this many players have completed the same
 * challenge. Mirrors `ScoringRulesetV1#teamBonus`; a single player earns nothing, and the fixed
 * roster's potential 7th player is capped to the 6-player tier rather than extrapolated, so the
 * ladder stops there.
 */
export const TEAM_BONUS_SHOWCASE: readonly BonusShowcase[] = [
  { label: '2', bonus: 150, intensityClass: 'bg-brand-500/5 border-brand-500/30' },
  { label: '3', bonus: 300, intensityClass: 'bg-brand-500/8 border-brand-500/45' },
  { label: '4', bonus: 500, intensityClass: 'bg-brand-500/12 border-brand-500/60' },
  { label: '5', bonus: 750, intensityClass: 'bg-brand-500/16 border-brand-500/80' },
  { label: '6+', bonus: 1_100, intensityClass: 'bg-brand-500/20 border-brand-500' },
];

/**
 * What the weekly ranking is made of, as the numbered facts closing the page.
 *
 * Each entry is the last segment of its `rules.sections.ranking.facts.*` translation key.
 */
export const RANKING_FACTS: readonly string[] = ['damage', 'championTitle', 'crown'];
