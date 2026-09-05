import { GuardianCategory } from '@core/campaign/campaign.model';
import { ChallengeDifficulty } from '@core/challenges/challenge.model';

/**
 * The figures the rules page quotes, copied from `docs/GAMEPLAY.md`.
 *
 * Static on purpose: the page explains the game as it is written, not as a given campaign happens
 * to be sized. The worked examples are computed on the squad the document itself uses.
 */

/**
 * Reference the worked examples are computed on, the document's own.
 */
export const EXAMPLE_REFERENCE = 5_300;

/**
 * Active operators of the squad the campaign example is sized for.
 */
export const EXAMPLE_OPERATORS = 7;

/**
 * Share of the squad's weekly reference a guardian's hit points are set at.
 */
export const GUARDIAN_FACTOR = 0.78;

/**
 * Share of the squad's weekly reference a week's group of wounded is set at.
 */
export const GROUP_FACTOR = 0.05;

/**
 * Linear growth of groups and challenge rewards, per campaign week past the first.
 */
export const PROGRESSION_PER_WEEK = 0.04;

/**
 * What a match is worth by mode and outcome.
 */
export interface MatchDamageRow {
  readonly key: string;
  readonly loss: number;
  readonly draw: number | null;
  readonly win: number;
}

/**
 * The modes, grouped by how their value splits into food and components: the split is the group's,
 * not the mode's, so it is stated once per group.
 */
export interface ModeGroup {
  readonly key: string;
  readonly foodPercent: number;
  readonly modes: readonly MatchDamageRow[];
}

export const MODE_GROUPS: readonly ModeGroup[] = [
  {
    key: 'long',
    foodPercent: 30,
    modes: [
      { key: 'competitive', loss: 350, draw: 425, win: 500 },
      { key: 'unrated', loss: 320, draw: 390, win: 460 },
    ],
  },
  {
    key: 'short',
    foodPercent: 70,
    modes: [
      { key: 'teamDeathmatch', loss: 110, draw: 135, win: 160 },
      { key: 'spikeRush', loss: 110, draw: null, win: 150 },
      { key: 'deathmatch', loss: 100, draw: null, win: 150 },
      { key: 'skirmish', loss: 90, draw: 110, win: 130 },
    ],
  },
];

/**
 * A step of a ladder: a label key and the percentage it applies.
 */
export interface LadderStep {
  readonly key: string;
  readonly percent: number;
}

/**
 * Daily diminishing returns, by rank of the match in the day.
 */
export const DECAY_LADDER: readonly LadderStep[] = [
  { key: 'first', percent: 100 },
  { key: 'next', percent: 50 },
  { key: 'rest', percent: 25 },
];

/**
 * Streak bonus, by consecutive days played; the last step is open-ended.
 */
export interface StreakStep {
  readonly days: number;
  readonly percent: number;
  readonly open: boolean;
}

export const STREAK_LADDER: readonly StreakStep[] = [
  { days: 1, percent: 0, open: false },
  { days: 2, percent: 2, open: false },
  { days: 3, percent: 4, open: false },
  { days: 4, percent: 6, open: false },
  { days: 5, percent: 8, open: false },
  { days: 6, percent: 10, open: true },
];

/**
 * What a day of the week does, in order.
 */
export const WEEK_STEP_KEYS: readonly string[] = ['sync', 'midnight', 'sunday', 'monday'];

/**
 * The three limits of Sunday's extraction, then what they add up to.
 */
export const SUNDAY_TERM_KEYS: readonly string[] = ['seats', 'beds', 'breach', 'rescued'];

/**
 * Sunday's worked example, one line per figure, on a group of forty wounded.
 */
export interface SundayExampleRow {
  readonly key: string;
  readonly value: string;
  readonly emphasised: boolean;
}

export const SUNDAY_EXAMPLE: readonly SundayExampleRow[] = [
  { key: 'challenges', value: '12', emphasised: true },
  { key: 'remaining', value: '28', emphasised: false },
  { key: 'seats', value: '30', emphasised: false },
  { key: 'beds', value: '20', emphasised: false },
  { key: 'extraction', value: '15', emphasised: false },
  { key: 'rescued', value: '12 + 15 = 27', emphasised: true },
];

/**
 * What a surviving guardian takes from the base, by breach reached.
 */
export interface LossStep {
  readonly breach: number;
  readonly percent: number;
}

export const GUARDIAN_LOSS_LADDER: readonly LossStep[] = [
  { breach: 99, percent: 0.004 },
  { breach: 84, percent: 0.9 },
  { breach: 70, percent: 3.2 },
  { breach: 20, percent: 22 },
  { breach: 0, percent: 35 },
];

/**
 * The ten weeks of a campaign: the guardian's category and the two weights, in shares of the
 * squad's reference. `how` names the weeks whose shape is worth a word.
 */
export interface CampaignWeekShape {
  readonly category: GuardianCategory;
  readonly guardian: number;
  readonly group: number;
  readonly how: boolean;
}

export const CAMPAIGN_WEEKS: readonly CampaignWeekShape[] = [
  { category: 'MINOR', guardian: 0.6, group: 1, how: true },
  { category: 'STANDARD', guardian: 0.8, group: 1.3, how: false },
  { category: 'STANDARD', guardian: 0.95, group: 0.9, how: false },
  { category: 'STANDARD', guardian: 0.85, group: 1.1, how: false },
  { category: 'ELITE', guardian: 1.3, group: 1.5, how: true },
  { category: 'MINOR', guardian: 0.6, group: 1.2, how: true },
  { category: 'STANDARD', guardian: 1, group: 0.8, how: true },
  { category: 'STANDARD', guardian: 0.9, group: 1.1, how: false },
  { category: 'STANDARD', guardian: 0.95, group: 1, how: false },
  { category: 'ELITE', guardian: 1.35, group: 2, how: true },
];

/**
 * The campaign's life, in order.
 */
export const LIFECYCLE_KEYS: readonly string[] = ['open', 'start', 'close', 'between'];

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
 * How the reference is read, in the order the document states it.
 */
export const CALIBRATION_FACT_KEYS: readonly string[] = ['window', 'emptyWeeks', 'floor', 'once'];

/**
 * What a challenge is worth, by cadence and difficulty, at the example reference.
 */
export interface ChallengeWorth {
  readonly difficulty: ChallengeDifficulty | null;
  readonly weight: number;
  readonly survivors: number;
}

export const CHALLENGE_WORTH: readonly ChallengeWorth[] = [
  { difficulty: null, weight: 1.2, survivors: 6 },
  { difficulty: 'EASY', weight: 1, survivors: 5 },
  { difficulty: 'NORMAL', weight: 1.7, survivors: 9 },
  { difficulty: 'MEDIUM', weight: 2.7, survivors: 14 },
  { difficulty: 'HARD', weight: 3.9, survivors: 21 },
  { difficulty: 'VERY_HARD', weight: 5.4, survivors: 29 },
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
