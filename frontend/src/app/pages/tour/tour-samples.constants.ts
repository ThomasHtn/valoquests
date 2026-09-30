import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { ChallengeOperator } from '@pages/challenges/challenges.model';
import { BoardRow } from '@pages/leaderboard/leaderboard.model';
import { Capacity, Contribution, Mission, SundayStakes } from '@pages/overview/overview.model';

import { TourSampleChallenge, TourSampleMatch } from './tour.model';

/*
 * The tour's illustrations run on this fixed sample campaign rather than on the live one: between
 * two campaigns, or on a Monday morning, the live screens have nothing to show yet. The figures are
 * kept consistent with each other and with the rules, so the excerpts read like the real screens.
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
    damage: 312,
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
    damage: 248,
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
    damage: 96,
    damagePercent: 60,
  },
];

/**
 * Inhabitants of the sample base.
 */
export const TOUR_SAMPLE_POPULATION = 12_480;

/**
 * Inhabitants the sample base gained today.
 */
export const TOUR_SAMPLE_POPULATION_CHANGE = 214;

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
  hitPointsLeft: 37_440,
  hitPoints: 96_000,
  breachPercent: 61,
  guardianLeft: 0.39,
  defeated: null,
  wounded: 640,
  crew: 4,
};

/**
 * What the sample squad put into the week: the damage adds up to the hit points taken above.
 */
export const TOUR_SAMPLE_CONTRIBUTION: Contribution = {
  total: 58_560,
  hitPoints: 96_000,
  shares: [
    {
      playerId: 1,
      name: 'Kairo',
      damage: 21_400,
      challengePoints: 180,
      total: 21_580,
      sharePercent: 36,
    },
    {
      playerId: 2,
      name: 'Sable',
      damage: 17_300,
      challengePoints: 150,
      total: 17_450,
      sharePercent: 30,
    },
    {
      playerId: 3,
      name: 'Nova',
      damage: 12_100,
      challengePoints: 120,
      total: 12_220,
      sharePercent: 21,
    },
    {
      playerId: 4,
      name: 'Vex',
      damage: 7_760,
      challengePoints: 60,
      total: 7_820,
      sharePercent: 13,
    },
  ],
};

/**
 * The sample week's four dials: food is the tightest stock, so it caps the extraction.
 */
export const TOUR_SAMPLE_CAPACITY: Capacity = {
  wounded: 640,
  carry: { value: 410, fraction: 0.64, stock: 5_740 },
  shelter: { value: 380, fraction: 0.59, stock: 4_560 },
  breach: { value: 61, fraction: 0.61, stock: 37_440 },
  aboard: 328,
  aboardFraction: 0.51,
  fromGuardian: 232,
  fromChallenges: 96,
  leftBehind: 312,
  limiter: 'FOOD',
  componentsPerRescue: 14,
  foodPerRescue: 12,
  hitPointsPerPercent: 960,
};

/**
 * The sample week's Sunday: the food-capped 380 reachable minus the 232 already forecast, and the
 * base's 12 480 inhabitants struck at 61 % breakthrough.
 */
export const TOUR_SAMPLE_STAKES: SundayStakes = {
  gain: 148,
  loss: 664,
};

/**
 * Three of the sample week's five challenges, one tier apart.
 */
export const TOUR_SAMPLE_CHALLENGES: readonly TourSampleChallenge[] = [
  { key: 'sharpSight', difficulty: 'NORMAL', survivors: 9, target: 50, progress: [50, 50, 31, 50] },
  { key: 'fullWeek', difficulty: 'MEDIUM', survivors: 14, target: 5, progress: [5, 3, 5, 2] },
  { key: 'insatiable', difficulty: 'HARD', survivors: 21, target: 28, progress: [19, 11, 28, 7] },
];

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
    total: 21_580,
    damage: 21_400,
    challengePoints: 180,
    title: null,
    challengesCompleted: 9,
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
    total: 17_450,
    damage: 17_300,
    challengePoints: 150,
    title: { key: 'MECHANIC', measure: null, ...resolveTitleVisual('MECHANIC') },
    challengesCompleted: 7,
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
    total: 12_220,
    damage: 12_100,
    challengePoints: 120,
    title: { key: 'REGULAR', measure: null, ...resolveTitleVisual('REGULAR') },
    challengesCompleted: 8,
    challengesMax: 12,
    matchCount: 9,
    streak: { week: null, days: 4, bonusPercent: 6 },
  },
];
