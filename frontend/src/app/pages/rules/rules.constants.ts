import { WEEKLY_TITLES } from '@core/campaign/campaign.constants';
import { GuardianCategory } from '@core/campaign/campaign-week.model';
import { TitleKey } from '@core/campaign/titles/campaign-title.model';

import {
  CampaignWeekShape,
  ChallengeWorth,
  DifficultyBand,
  LadderStep,
  LossStep,
  ModeGroup,
  RuleConstant,
  StreakStep,
  SundayExampleRow,
} from './rules.model';

/* Figures copied from `docs/GAMEPLAY.md`; static because the page explains the game as written. */

/**
 * Reference the worked examples use, the document's own.
 */
export const EXAMPLE_REFERENCE = 5_300;

/**
 * Active operators of the squad the campaign example is sized for.
 */
export const EXAMPLE_OPERATORS = 7;

/**
 * Share of the squad's weekly reference a guardian's hit points are set at.
 */
export const GUARDIAN_FACTOR = 1.1;

/**
 * Share of the squad's weekly reference a week's group of wounded is set at.
 */
export const GROUP_FACTOR = 0.05;

/**
 * Linear growth of groups and challenge rewards, per campaign week past the first.
 */
export const PROGRESSION_PER_WEEK = 0.04;

/**
 * Game modes grouped by the share of a match going to food.
 */
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
      { key: 'swiftplay', loss: 160, draw: null, win: 230 },
      { key: 'teamDeathmatch', loss: 110, draw: 135, win: 160 },
      { key: 'spikeRush', loss: 110, draw: null, win: 150 },
      { key: 'escalation', loss: 110, draw: null, win: 150 },
      { key: 'deathmatch', loss: 100, draw: null, win: 150 },
      { key: 'skirmish', loss: 90, draw: 110, win: 130 },
    ],
  },
];

/**
 * Daily diminishing returns, by rank of the match in the day.
 */
export const DECAY_LADDER: readonly LadderStep[] = [
  { key: 'first', percent: 100 },
  { key: 'next', percent: 50 },
  { key: 'rest', percent: 25 },
];

/**
 * Bonus per day played in the week, capped at the last step.
 */
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
 * Worked example of one Sunday settlement, line by line.
 */
export const SUNDAY_EXAMPLE: readonly SundayExampleRow[] = [
  { key: 'challenges', value: '12', emphasised: true },
  { key: 'remaining', value: '28', emphasised: false },
  { key: 'seats', value: '30', emphasised: false },
  { key: 'beds', value: '20', emphasised: false },
  { key: 'extraction', value: '15', emphasised: false },
  { key: 'rescued', value: '12 + 15 = 27', emphasised: true },
];

/**
 * Share of the base lost, per breakthrough level, when the guardian stands.
 */
export const GUARDIAN_LOSS_LADDER: readonly LossStep[] = [
  { breach: 99, percent: 0.004 },
  { breach: 84, percent: 0.9 },
  { breach: 70, percent: 3.2 },
  { breach: 20, percent: 22 },
  { breach: 0, percent: 35 },
];

/**
 * Heading modifier per guardian category, matching the campaign page's colours.
 */
export const GUARDIAN_CATEGORY_MODIFIERS: Readonly<Record<GuardianCategory, string>> = {
  MINOR: 'rules__heading--minor',
  STANDARD: 'rules__heading--standard',
  ELITE: 'rules__heading--elite',
};

/**
 * The ten weeks: guardian class, hit points and group size.
 */
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
 * The two difficulties and their references. Mirrors the backend `CampaignDifficulty`.
 */
export const DIFFICULTY_BANDS: readonly DifficultyBand[] = [
  { key: 'AMATEUR', reference: 5_300 },
  { key: 'PRO', reference: 10_600 },
];

/**
 * How the difficulty is decided, in the order the document states it.
 */
export const CALIBRATION_FACT_KEYS: readonly string[] = ['chosen', 'perOperator', 'grid', 'once'];

/**
 * Points and survivors a challenge is worth, per difficulty.
 */
export const CHALLENGE_WORTH: readonly ChallengeWorth[] = [
  { difficulty: null, weight: 1.2, survivors: 6 },
  { difficulty: 'EASY', weight: 1, survivors: 5 },
  { difficulty: 'NORMAL', weight: 1.7, survivors: 9 },
  { difficulty: 'MEDIUM', weight: 2.7, survivors: 14 },
  { difficulty: 'HARD', weight: 3.9, survivors: 21 },
  { difficulty: 'VERY_HARD', weight: 5.4, survivors: 29 },
];

/**
 * The titles a week hands out, the champion first since it outranks the four others.
 */
export const RULE_TITLES: readonly TitleKey[] = ['CHAMPION', ...WEEKLY_TITLES];

/**
 * Closing sheet: the constants a player can picture, in reading order.
 */
export const RULE_CONSTANTS: readonly RuleConstant[] = [
  { key: 'syncInterval', icon: 'sync', tone: 'brand' },
  { key: 'growth', icon: 'base', tone: 'brand' },
  { key: 'upkeep', icon: 'food', tone: 'food' },
  { key: 'componentsPerRescue', icon: 'components', tone: 'components' },
  { key: 'foodPerRescue', icon: 'bed', tone: 'food' },
  { key: 'famine', icon: 'food', tone: 'food' },
];
