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
 * Per-player weekly output each weight class is sized against, as a percentage.
 *
 * Mirrors the switch in `DefaultScoringRuleset#bossHitPoints`, and the figures the three class
 * cards quote in prose.
 */
const BOSS_CATEGORY_WEIGHT_PERCENT: Readonly<Record<BossCategory, number>> = {
  MINOR: 65,
  STANDARD: 85,
  ELITE: 105,
};

/**
 * Same weights, normalized on the heaviest class, so the ladder can draw each week at the height it
 * actually costs instead of as ten equal steps.
 */
export const BOSS_CATEGORY_WEIGHT_SHARE: Readonly<Record<BossCategory, number>> =
  Object.fromEntries(
    Object.entries(BOSS_CATEGORY_WEIGHT_PERCENT).map(([category, percent]) => [
      category,
      Math.round((percent / BOSS_CATEGORY_WEIGHT_PERCENT.ELITE) * 100),
    ]),
  ) as Record<BossCategory, number>;

/**
 * Materials one player earns from a defeated boss, by weight class.
 *
 * Mirrors `DefaultColonyRuleset#materialsForDefeatedBoss`. The spread is wider than the morale
 * table's on purpose: a run schedules exactly two elite weeks, and they are the only ones that can
 * move the town by a step on their own.
 */
export const BOSS_MATERIALS_SHOWCASE: readonly { category: BossCategory; materials: number }[] = [
  { category: 'MINOR', materials: 40 },
  { category: 'STANDARD', materials: 80 },
  { category: 'ELITE', materials: 140 },
];

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

/**
 * One line of the colony's morale table: what a Monday's fight moves it by.
 *
 * Mirrors `DefaultColonyRuleset#moraleForDefeatedBoss` and `#moraleForSurvivingBoss`. The fight is
 * the only thing in the whole model that touches morale, which is exactly why it is worth a table of
 * its own: everything else the squad does is already measured by the seven-day food window.
 *
 * The four figures look small beside the eighty points the gauge spans, and that is the calibration:
 * the ten fights a run schedules pay fifty morale between them, which is exactly the room between the
 * fifty a run opens on and the hundred it tops out at. A flawless run lands on the ceiling with its
 * tenth fight rather than its fourth, so no fight of the run is ever worth nothing.
 */
export const COLONY_MORALE_SHOWCASE: readonly { label: string; morale: number }[] = [
  { label: 'MINOR', morale: 3 },
  { label: 'STANDARD', morale: 5 },
  { label: 'ELITE', morale: 7 },
  { label: 'SURVIVED', morale: -7 },
];

/**
 * Materials one player earns by completing a challenge, by difficulty.
 *
 * Mirrors `DefaultColonyRuleset#materialsForChallenge`, which divides the same challenge damage the
 * ladder above uses by a hundred — so the colony cannot drift from the ranking on what a `HARD` is
 * worth, and the two tables on this page are one table read twice.
 */
export const COLONY_MATERIALS_SHOWCASE: readonly {
  difficulty: ChallengeDifficulty;
  materials: number;
}[] = DIFFICULTY_SHOWCASE.map((tier) => ({
  difficulty: tier.difficulty,
  materials: Math.floor(tier.damage / 100),
}));

/**
 * The town's first named tiers, with the efficiency each opens at.
 *
 * Cut at the point the ladder starts repeating: past `GREAT_CITY` every further step is a numbered
 * citadel, so listing them would be listing an infinite series. Mirrors `DefaultColonyRuleset`'s own
 * name table and its 0.75 step, which paces a regular run at one milestone a week.
 */
export const COLONY_TIER_SHOWCASE: readonly { name: string; threshold: number }[] = [
  { name: 'CAMP', threshold: 8 },
  { name: 'HAMLET', threshold: 8.75 },
  { name: 'VILLAGE', threshold: 9.5 },
  { name: 'BOROUGH', threshold: 10.25 },
  { name: 'TOWN', threshold: 11 },
  { name: 'CITY', threshold: 11.75 },
  { name: 'RESIDENTIAL_QUARTER', threshold: 12.5 },
  { name: 'GREAT_CITY', threshold: 13.25 },
];

/**
 * The four numbers the colony's whole chain is made of, as the ordered facts of its beat.
 *
 * Each entry is the last segment of its `rules.sections.colony.facts.*` translation key.
 */
export const COLONY_FACTS: readonly string[] = ['harvest', 'feeds', 'eats', 'window'];
