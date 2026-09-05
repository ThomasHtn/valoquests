import { GuardianCategory } from '@core/campaign/campaign.model';
import { ChallengeDifficulty } from '@core/challenges/challenge.model';

/**
 * The figures the rules page quotes, copied from `docs/GAMEPLAY.md`.
 *
 * Static on purpose: the page explains the game as it is written, not as a given campaign happens
 * to be sized. The one live figure, the reference a squad is measured at, is shown as the example
 * the document itself uses.
 */

/**
 * Reference the worked examples are computed on, the document's own.
 */
export const EXAMPLE_REFERENCE = 5_300;

/**
 * Floor a squad's reference never goes under.
 */
export const REFERENCE_FLOOR = 2_000;

/**
 * The four loops, from the fastest to the slowest.
 */
export const LOOP_KEYS: readonly string[] = ['sync', 'daily', 'weekly', 'campaign'];

/**
 * What a match is worth by mode and outcome, with how long one lasts on average.
 */
export interface MatchDamageRow {
  readonly key: string;
  readonly loss: number;
  readonly draw: number | null;
  readonly win: number;
  readonly minutes: number;

  /**
   * Share of the value that becomes food, the rest being components.
   */
  readonly foodPercent: number;
}

export const MATCH_DAMAGE: readonly MatchDamageRow[] = [
  { key: 'competitive', loss: 350, draw: 425, win: 500, minutes: 35, foodPercent: 30 },
  { key: 'unrated', loss: 320, draw: 390, win: 460, minutes: 33, foodPercent: 30 },
  { key: 'teamDeathmatch', loss: 110, draw: 135, win: 160, minutes: 10, foodPercent: 70 },
  { key: 'spikeRush', loss: 110, draw: null, win: 150, minutes: 9, foodPercent: 70 },
  { key: 'deathmatch', loss: 100, draw: null, win: 150, minutes: 9, foodPercent: 70 },
  { key: 'skirmish', loss: 90, draw: 110, win: 130, minutes: 6, foodPercent: 70 },
];

/**
 * Four matches, split into the two resources.
 */
export interface ResourceExample {
  readonly key: string;
  readonly total: number;
  readonly food: number;
  readonly components: number;
}

export const RESOURCE_EXAMPLES: readonly ResourceExample[] = [
  { key: 'competitiveWin', total: 500, food: 150, components: 350 },
  { key: 'competitiveLoss', total: 350, food: 105, components: 245 },
  { key: 'deathmatchWin', total: 150, food: 105, components: 45 },
  { key: 'deathmatchLoss', total: 100, food: 70, components: 30 },
];

/**
 * A step of a ladder: a label and the percentage it applies.
 */
export interface LadderStep {
  readonly label: string;
  readonly percent: number;
}

/**
 * Daily diminishing returns, by rank of the match in the day.
 */
export const DECAY_LADDER: readonly LadderStep[] = [
  { label: '1 – 5', percent: 100 },
  { label: '6 – 9', percent: 50 },
  { label: '10+', percent: 25 },
];

/**
 * Streak bonus, by consecutive days played.
 */
export const STREAK_LADDER: readonly LadderStep[] = [
  { label: '1', percent: 0 },
  { label: '2', percent: 2 },
  { label: '3', percent: 4 },
  { label: '4', percent: 6 },
  { label: '5', percent: 8 },
  { label: '6+', percent: 10 },
];

/**
 * The rules of the streak, in the order the document states them.
 */
export const STREAK_RULE_KEYS: readonly string[] = [
  'individual',
  'valued',
  'wholeDay',
  'calendar',
  'neverStops',
  'everything',
];

/**
 * What a day does, in order.
 */
export const DAY_STEP_KEYS: readonly string[] = ['grow', 'guardian', 'eat', 'daily'];

/**
 * Share of the squad's food the base's upkeep absorbs, by campaign week.
 */
export const UPKEEP_LADDER: readonly LadderStep[] = [
  { label: '1', percent: 0.7 },
  { label: '3', percent: 2.6 },
  { label: '5', percent: 5.1 },
  { label: '7', percent: 7.2 },
  { label: '9', percent: 9.6 },
  { label: '10', percent: 11 },
];

/**
 * Inhabitants lost by evenings of famine.
 */
export interface FamineStep {
  readonly evenings: number;
  readonly percent: number;
}

export const FAMINE_LADDER: readonly FamineStep[] = [
  { evenings: 1, percent: 5 },
  { evenings: 3, percent: 14 },
  { evenings: 5, percent: 23 },
  { evenings: 7, percent: 30 },
];

/**
 * What a surviving guardian takes from the base, by breach reached.
 */
export const GUARDIAN_LOSS_LADDER: readonly LadderStep[] = [
  { label: '99 %', percent: 0.004 },
  { label: '93 %', percent: 0.2 },
  { label: '84 %', percent: 0.9 },
  { label: '70 %', percent: 3.2 },
  { label: '20 %', percent: 22 },
];

/**
 * The ten weeks of a campaign: the guardian's category and the two weights, in shares of the
 * squad's reference.
 */
export interface CampaignWeekShape {
  readonly category: GuardianCategory;
  readonly guardian: number;
  readonly group: number;
}

export const CAMPAIGN_WEEKS: readonly CampaignWeekShape[] = [
  { category: 'MINOR', guardian: 0.6, group: 1 },
  { category: 'STANDARD', guardian: 0.8, group: 1.3 },
  { category: 'STANDARD', guardian: 0.95, group: 0.9 },
  { category: 'STANDARD', guardian: 0.85, group: 1.1 },
  { category: 'ELITE', guardian: 1.3, group: 1.5 },
  { category: 'MINOR', guardian: 0.6, group: 1.2 },
  { category: 'STANDARD', guardian: 1, group: 0.8 },
  { category: 'STANDARD', guardian: 0.9, group: 1.1 },
  { category: 'STANDARD', guardian: 0.95, group: 1 },
  { category: 'ELITE', guardian: 1.35, group: 2 },
];

/**
 * The tiers, with the reference each spans.
 */
export interface TierBand {
  readonly key: string;
  readonly min: number | null;
  readonly max: number | null;
}

export const TIER_BANDS: readonly TierBand[] = [
  { key: 'AMATEUR', min: null, max: 3_500 },
  { key: 'NORMAL', min: 3_500, max: 9_000 },
  { key: 'CONFIRMED', min: 9_000, max: 16_000 },
  { key: 'ELITE', min: 16_000, max: null },
];

/**
 * What a challenge is worth, by cadence and difficulty, at the example reference.
 */
export interface ChallengeWorth {
  readonly difficulty: ChallengeDifficulty | null;
  readonly weight: number;
  readonly survivors: number;
  readonly points: number;
}

export const CHALLENGE_WORTH: readonly ChallengeWorth[] = [
  { difficulty: null, weight: 1.2, survivors: 6, points: 64 },
  { difficulty: 'EASY', weight: 1, survivors: 5, points: 53 },
  { difficulty: 'NORMAL', weight: 1.7, survivors: 9, points: 90 },
  { difficulty: 'MEDIUM', weight: 2.7, survivors: 14, points: 143 },
  { difficulty: 'HARD', weight: 3.9, survivors: 21, points: 207 },
  { difficulty: 'VERY_HARD', weight: 5.4, survivors: 29, points: 286 },
];

/**
 * The closing table, one key per constant of the document.
 */
export const CONSTANT_KEYS: readonly string[] = [
  'growth',
  'upkeep',
  'famine',
  'guardianSize',
  'groupSize',
  'progression',
  'challengePoints',
  'componentsPerRescue',
  'foodPerRescue',
  'protectedFood',
  'challengeWounded',
  'challengeDamage',
  'guardianLoss',
  'challengeSurvivors',
  'syncInterval',
  'calibrationWindow',
  'reference',
  'duration',
];
