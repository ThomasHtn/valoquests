import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { ChallengeOperator } from '@pages/challenges/challenges.model';
import { BoardRow } from '@pages/leaderboard/leaderboard.model';
import { Capacity, Contribution, Mission, SundayStakes } from '@pages/overview/overview.model';

import { TourSampleDaily, TourSampleMatch } from './tour.model';

/*
 * The tour's illustrations run on this fixed sample campaign rather than on the live one: between
 * two campaigns, or on a Monday morning, the live screens have nothing to show yet. The figures are
 * computed from the rules for week 4 of an Amateur campaign with 4 operators on a five-day streak
 * (+8 %), so the excerpts read like the real screens: 19 822 HP, 1 306 wounded, 7 survivors a daily.
 */

/**
 * The sample squad, in the order the challenge cards list them before sorting by progress.
 */
export const TOUR_SAMPLE_OPERATORS: readonly ChallengeOperator[] = [
  { playerId: 1, name: 'Kairo', portrait: resolvePlayerAvatarUrl('Neon') },
  { playerId: 2, name: 'Sable', portrait: resolvePlayerAvatarUrl('Sova') },
  { playerId: 3, name: 'Nova', portrait: resolvePlayerAvatarUrl('Killjoy') },
  { playerId: 4, name: 'Vex', portrait: resolvePlayerAvatarUrl('Skye') },
];

/**
 * One evening of one operator, newest first, as the profile's match history lists it.
 */
export const TOUR_SAMPLE_MATCHES: readonly TourSampleMatch[] = [
  {
    mapName: 'Ascent',
    agentName: 'Jett',
    gameMode: 'COMPETITIVE',
    time: '23:05',
    result: 'WIN',
    allyScore: 13,
    enemyScore: 9,
    kills: 22,
    deaths: 14,
    assists: 5,
    damage: 540,
    damagePercent: 100,
  },
  {
    mapName: 'Lotus',
    agentName: 'Sova',
    gameMode: 'UNRATED',
    time: '22:12',
    result: 'LOSS',
    allyScore: 10,
    enemyScore: 13,
    kills: 15,
    deaths: 16,
    assists: 8,
    damage: 346,
    damagePercent: 100,
  },
  {
    mapName: 'Bind',
    agentName: 'Raze',
    gameMode: 'SWIFTPLAY',
    time: '21:30',
    result: 'WIN',
    allyScore: 5,
    enemyScore: 3,
    kills: 14,
    deaths: 9,
    assists: 3,
    damage: 248,
    damagePercent: 100,
  },
];

/**
 * Inhabitants of the sample base.
 */
export const TOUR_SAMPLE_POPULATION = 3_480;

/**
 * Guardians the sample squad defeated so far, one rocket stage each.
 */
export const TOUR_SAMPLE_STAGES_DONE = 3;

/**
 * Time left before the sample week's extraction, so the countdown reads as a Friday evening.
 */
export const TOUR_SAMPLE_DEADLINE_IN_MS = (2 * 24 + 5) * 3_600_000 + 42 * 60_000;

/**
 * The sample week, fourth of the campaign, without its deadline: that one is set from the clock.
 */
export const TOUR_SAMPLE_MISSION: Omit<Mission, 'extractionDeadline'> = {
  weekIndex: 4,
  planetName: 'Hollin',
  category: 'STANDARD',
  dayOfWeek: 5,
  hitPointsLeft: 7_732,
  hitPoints: 19_822,
  breachPercent: 61,
  guardianLeft: 0.39,
  defeated: null,
  wounded: 1_306,
  crew: 4,
};

/**
 * What the sample squad put into the week: the damage adds up to the hit points taken above.
 */
export const TOUR_SAMPLE_CONTRIBUTION: Contribution = {
  total: 12_090,
  hitPoints: 19_822,
  shares: [
    {
      playerId: 1,
      name: 'Kairo',
      damage: 4_380,
      challengePoints: 50,
      total: 4_430,
      sharePercent: 36,
    },
    {
      playerId: 2,
      name: 'Sable',
      damage: 3_600,
      challengePoints: 44,
      total: 3_644,
      sharePercent: 30,
    },
    {
      playerId: 3,
      name: 'Nova',
      damage: 2_520,
      challengePoints: 31,
      total: 2_551,
      sharePercent: 21,
    },
    {
      playerId: 4,
      name: 'Vex',
      damage: 1_590,
      challengePoints: 20,
      total: 1_610,
      sharePercent: 13,
    },
  ],
};

/**
 * The sample week's four dials: food is the tightest stock, so it caps the extraction.
 */
export const TOUR_SAMPLE_CAPACITY: Capacity = {
  wounded: 1_306,
  carry: { value: 990, fraction: 0.76, stock: 13_860 },
  shelter: { value: 840, fraction: 0.64, stock: 10_275 },
  breach: { value: 61, fraction: 0.61, stock: 7_732 },
  aboard: 657,
  aboardFraction: 0.5,
  fromGuardian: 512,
  fromChallenges: 145,
  leftBehind: 649,
  limiter: 'FOOD',
  componentsPerRescue: 14,
  foodPerRescue: 12,
  hitPointsPerPercent: 198,
};

/**
 * The sample week's Sunday: the food-capped 840 reachable minus the 512 already forecast, and the
 * base's 3 480 inhabitants struck at 61 % breakthrough.
 */
export const TOUR_SAMPLE_STAKES: SundayStakes = {
  gain: 328,
  loss: 185,
};

/**
 * The sample Friday's challenge, worded so a non-player reads it at once.
 */
export const TOUR_SAMPLE_DAILY: TourSampleDaily = {
  key: 'session',
  target: 3,
  survivors: 7,
  progress: [3, 2, 3, 0],
};

/**
 * Operators who validated each day's challenge before the sample Friday, Monday first.
 */
export const TOUR_SAMPLE_DAILY_TALLY: readonly number[] = [4, 2, 3, 4];

/**
 * The sample week's three leaders, the champion on top.
 */
export const TOUR_SAMPLE_PODIUM: readonly BoardRow[] = [
  {
    playerId: 1,
    name: 'Kairo',
    portrait: resolvePlayerAvatarUrl('Neon'),
    position: 1,
    variation: 0,
    isChampion: true,
    total: 4_430,
    damage: 4_380,
    challengePoints: 50,
    title: null,
    challengesCompleted: 6,
    challengesMax: 12,
    matchCount: 14,
    streak: { week: null, days: 5, bonusPercent: 8 },
  },
  {
    playerId: 2,
    name: 'Sable',
    portrait: resolvePlayerAvatarUrl('Sova'),
    position: 2,
    variation: 0,
    isChampion: false,
    total: 3_644,
    damage: 3_600,
    challengePoints: 44,
    title: { key: 'MECHANIC', measure: null, ...resolveTitleVisual('MECHANIC') },
    challengesCompleted: 6,
    challengesMax: 12,
    matchCount: 11,
    streak: { week: null, days: 4, bonusPercent: 6 },
  },
  {
    playerId: 3,
    name: 'Nova',
    portrait: resolvePlayerAvatarUrl('Killjoy'),
    position: 3,
    variation: 0,
    isChampion: false,
    total: 2_551,
    damage: 2_520,
    challengePoints: 31,
    title: { key: 'REGULAR', measure: null, ...resolveTitleVisual('REGULAR') },
    challengesCompleted: 4,
    challengesMax: 12,
    matchCount: 9,
    streak: { week: null, days: 4, bonusPercent: 6 },
  },
];
