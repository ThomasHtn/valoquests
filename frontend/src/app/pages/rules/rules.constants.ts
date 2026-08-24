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
    ['EASY', 800],
    ['NORMAL', 1_400],
    ['MEDIUM', 2_200],
    ['HARD', 3_200],
    ['VERY_HARD', 4_500],
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
 * Mirrors `DefaultScoringRuleset#matchDamage` on the backend. Every mode the barème values has a row
 * here: a mode missing from this table reads as worth nothing, which is how Premier was presented
 * before it was priced.
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
  { mode: 'PREMIER', win: 500, draw: 425, loss: 350 },
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
 * `DefaultScoringRuleset#regularityBonus`; a single active day earns nothing, so the ladder starts at two.
 */
export const REGULARITY_BONUS_SHOWCASE: readonly BonusShowcase[] = [
  { label: '2', bonus: 600 },
  { label: '3', bonus: 1_400 },
  { label: '4', bonus: 2_400 },
  { label: '5', bonus: 3_600 },
  { label: '6', bonus: 4_800 },
  { label: '7', bonus: 6_000 },
];

/**
 * One tier of the squad bonus, as the percentage it adds to a challenge's own damage.
 *
 * Held as a percentage rather than a formatted multiplier so the decimal separator follows the
 * active language, resolved through `formatSquadMultiplier`. Mirrors
 * `DefaultScoringRuleset#challengeTeamBonus`: a lone completer earns nothing, each further player adds
 * ten percent, and the fixed roster's potential 7th is capped to the 6-player tier.
 */
export const TEAM_BONUS_SHOWCASE: readonly BonusShowcase[] = [
  { label: '2', bonus: 10 },
  { label: '3', bonus: 20 },
  { label: '4', bonus: 30 },
  { label: '5', bonus: 40 },
  { label: '6+', bonus: 50 },
];

/**
 * One band of the daily diminishing-returns ladder: the matches of a single day, ranked from the
 * most to the least valuable, keep this share of their damage.
 *
 * Mirrors `DefaultScoringRuleset#matchDamageCoefficientPercent`. Ranking by value rather than by play
 * order is deliberate and worth stating on the page: a warm-up never costs a ranked game its tier.
 */
export const MATCH_DECAY_SHOWCASE: readonly { label: string; percent: number }[] = [
  { label: '1-5', percent: 100 },
  { label: '6-9', percent: 50 },
  { label: '10+', percent: 25 },
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
