import { describe, expect, it } from 'vitest';

import { Campaign, CampaignBase, CampaignToday, CampaignWeek } from '@core/campaign/campaign.model';
import { CurrentChallenges } from '@core/challenges/challenge.model';
import { CurrentRanking, DailyRanking, RankingHistoryWeek } from '@core/ranking/ranking.model';
import { PlayerSummary } from '@core/players/player-summary.model';
import {
  buildCapacity,
  buildDailyOrder,
  buildFrieze,
  buildMission,
  buildMissionReport,
  buildSquad,
  buildTally,
  Translate,
} from './overview.utils';

const translate: Translate = (key, params) =>
  params ? `${key}(${Object.values(params).join(',')})` : key;

function week(overrides: Partial<CampaignWeek> = {}): CampaignWeek {
  return {
    weekIndex: 1,
    weekStart: '2026-01-05',
    planetName: 'Kepler',
    category: 'STANDARD',
    guardianName: 'Vex',
    guardianDescription: null,
    guardianHitPoints: 1000,
    damageDealt: 0,
    progressPercent: 0,
    defeated: false,
    defeatedAt: null,
    defeatedByPlayerId: null,
    fatalBlow: null,
    woundedCount: 10,
    challengeRescued: 0,
    extractionRescued: 0,
    foodSpent: 0,
    componentsSpent: 0,
    limiter: 'NONE',
    baseLoss: 0,
    settled: false,
    base: null,
    ...overrides,
  };
}

function base(overrides: Partial<CampaignBase> = {}): CampaignBase {
  return {
    population: 1000,
    foodStock: 100,
    componentsStock: 100,
    dailyUpkeep: 10,
    protectedFood: 10,
    rescuesByComponents: 5,
    rescuesByFood: 5,
    populationChange: 0,
    componentsPerRescue: 2,
    foodPerRescue: 2,
    guardianLossPercent: 5,
    ...overrides,
  };
}

function campaign(overrides: Partial<Campaign> = {}): Campaign {
  return {
    id: 1,
    status: 'RUNNING',
    number: 3,
    tier: 'NORMAL',
    reference: 1000,
    rosterSize: 5,
    firstWeekStart: '2026-01-05',
    lastWeekStart: '2026-03-09',
    today: '2026-01-05',
    currentWeekIndex: 1,
    base: base(),
    forecast: null,
    weeks: [week()],
    totals: null,
    ...overrides,
  };
}

function player(overrides: Partial<PlayerSummary> = {}): PlayerSummary {
  return {
    id: 1,
    riotId: 'Player#EU1',
    displayName: 'Player',
    portrait: null,
    competitiveTier: 'GOLD_1',
    rankRating: null,
    kda: null,
    winRate: null,
    headshotPercentage: null,
    matchesPlayed: 10,
    status: 'ACTIVE',
    lastSuccessfulSynchronizationAt: null,
    ...overrides,
  };
}

describe('buildFrieze', () => {
  it('returns nothing outside a campaign', () => {
    expect(buildFrieze(null, translate)).toEqual([]);
  });

  it('marks a defeated week as won, fully advanced', () => {
    const [entry] = buildFrieze(campaign({ weeks: [week({ defeated: true })] }), translate);

    expect(entry).toMatchObject({ state: 'won', advance: 1, mark: '✓' });
  });

  it('marks a settled but undefeated week as lost, advanced by its breakthrough', () => {
    const [entry] = buildFrieze(
      campaign({ weeks: [week({ settled: true, progressPercent: 64 })] }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'lost', advance: 0.64, mark: '✕' });
  });

  it('marks the week in progress as now', () => {
    const [entry] = buildFrieze(
      campaign({ currentWeekIndex: 1, weeks: [week({ progressPercent: 30 })] }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'now', advance: 0.3, mark: '●' });
  });

  it('stars the final unplayed week of a still-running campaign', () => {
    const [entry] = buildFrieze(
      campaign({
        currentWeekIndex: 1,
        weeks: [week({ weekIndex: 10 })],
      }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', mark: '★' });
  });

  it('dots an unplayed week of a closed campaign instead of starring it', () => {
    const [entry] = buildFrieze(
      campaign({ status: 'CLOSED', currentWeekIndex: null, weeks: [week({ weekIndex: 10 })] }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', mark: '·' });
    expect(entry.title).toBe('overview.frieze.unplayed');
  });
});

describe('buildMission', () => {
  it('returns null outside a running week', () => {
    expect(buildMission(campaign(), null, [], 'en')).toBeNull();
    expect(buildMission(null, week(), [], 'en')).toBeNull();
  });

  it('reports how much of the guardian is left standing', () => {
    const mission = buildMission(
      campaign(),
      week({ guardianHitPoints: 1000, damageDealt: 400 }),
      [],
      'en',
    );

    expect(mission).toMatchObject({ hitPointsLeft: 600, guardianLeft: 0.6 });
  });

  it('clamps the day of week between 1 and 7', () => {
    const mission = buildMission(
      campaign({ today: '2026-01-20' }),
      week({ weekStart: '2026-01-05' }),
      [],
      'en',
    );

    expect(mission?.dayOfWeek).toBe(7);
  });

  it('names who dealt the fatal blow once the guardian is down', () => {
    const mission = buildMission(
      campaign(),
      week({
        defeated: true,
        defeatedAt: '2026-01-07T18:30:00Z',
        defeatedByPlayerId: 42,
      }),
      [player({ id: 42, displayName: 'Killjoy Main' })],
      'en',
    );

    expect(mission?.defeated?.by).toBe('Killjoy Main');
  });
});

describe('buildMissionReport', () => {
  it('returns null before the first settled week', () => {
    expect(buildMissionReport(campaign(), [], [], 'en', translate)).toBeNull();
  });

  it('reports an empty ranking and no titles when the week was never frozen', () => {
    const report = buildMissionReport(
      campaign({ weeks: [week({ settled: true })] }),
      [],
      [],
      'en',
      translate,
    );

    expect(report?.titles).toBeNull();
    expect(report?.ranking).toEqual([]);
  });

  it('resolves the frozen ranking and titles once the week has one', () => {
    const history: readonly RankingHistoryWeek[] = [
      {
        weekStart: '2026-01-05',
        weekEnd: '2026-01-11',
        finalizedAt: '2026-01-12T00:05:00Z',
        winnerPlayerId: 7,
        ranking: [
          {
            position: 1,
            playerId: 7,
            displayName: 'Scout Prime',
            guardianDamage: 500,
            challengePoints: 100,
            totalPoints: 600,
            completedChallenges: 5,
            completedDailyChallenges: 3,
            activeDays: 7,
            streakDays: 7,
            titles: ['SCOUT'],
          },
        ],
      },
    ];

    const report = buildMissionReport(
      campaign({ weeks: [week({ settled: true, weekStart: '2026-01-05' })] }),
      [player({ id: 7, displayName: 'Scout Prime' })],
      history,
      'en',
      translate,
    );

    expect(report?.ranking).toEqual([
      { position: 1, name: 'Scout Prime', portrait: null, total: 600 },
    ]);
    expect(report?.titles?.find((title) => title.key === 'SCOUT')?.holder).toBe('Scout Prime');
  });

  it('states the fatal blow in one line, translated', () => {
    const report = buildMissionReport(
      campaign({
        weeks: [
          week({
            settled: true,
            defeated: true,
            defeatedAt: '2026-01-07T18:30:00Z',
            defeatedByPlayerId: 42,
          }),
        ],
      }),
      [player({ id: 42, displayName: 'Killjoy Main' })],
      [],
      'en',
      translate,
    );

    expect(report?.blow).toContain('overview.missionReport.blow');
  });

  it('says nothing about the blow while the guardian held', () => {
    const report = buildMissionReport(
      campaign({ weeks: [week({ settled: true, defeated: false })] }),
      [],
      [],
      'en',
      translate,
    );

    expect(report?.blow).toBeNull();
  });
});

describe('buildCapacity', () => {
  it('returns null outside a running week with a forecast', () => {
    expect(buildCapacity(campaign({ forecast: null }), week())).toBeNull();
  });

  it('caps every fraction at one wounded fully covered', () => {
    const capacity = buildCapacity(
      campaign({
        base: base({ rescuesByComponents: 20, rescuesByFood: 3 }),
        forecast: {
          weekIndex: 1,
          woundedCount: 10,
          challengeRescued: 2,
          extractionRescued: 5,
          rescued: 7,
          leftBehind: 3,
          limiter: 'FOOD',
        },
      }),
      week({ woundedCount: 10 }),
    );

    expect(capacity?.carry).toMatchObject({ value: 20, fraction: 1, stock: 100 });
    expect(capacity?.shelter).toMatchObject({ value: 3, fraction: 0.3 });
    expect(capacity?.aboardFraction).toBe(0.7);
  });
});

describe('buildDailyOrder', () => {
  const challenges: CurrentChallenges = {
    weekStart: '2026-01-05',
    weekEnd: '2026-01-11',
    today: '2026-01-06',
    lastSuccessfulSynchronizationAt: null,
    roster: [],
    challenges: [],
    dailies: [
      {
        id: 9,
        code: 'DAILY_HEADSHOTS',
        name: 'Headshot sweep',
        description: 'Land headshots',
        cadence: 'DAILY',
        difficulty: null,
        competitiveOnly: false,
        metric: 'HEADSHOTS',
        targetValue: 10,
        survivors: 3,
        rankingPoints: 5,
        day: '2026-01-06',
        completedPlayers: 1,
        totalPlayers: 4,
        completedPlayerIds: [1],
        completionPercentage: 25,
      },
    ],
  };

  it('returns null without a challenge draw', () => {
    expect(buildDailyOrder(null, null)).toBeNull();
  });

  it('returns null when today has no daily challenge', () => {
    expect(buildDailyOrder({ ...challenges, dailies: [] }, null)).toBeNull();
  });

  it('marks an operator done once their daily progress line completes', () => {
    const ranking: CurrentRanking = {
      weekStart: '2026-01-05',
      weekEnd: '2026-01-11',
      today: '2026-01-06',
      calculatedAt: '2026-01-06T00:10:00Z',
      ranking: [
        {
          position: 1,
          previousPosition: 1,
          positionVariation: 0,
          player: {
            id: 1,
            displayName: 'Operator',
            portrait: null,
            competitiveTier: 'GOLD_1',
            rankRating: null,
          },
          guardianDamage: 100,
          food: 10,
          components: 10,
          matchCount: 3,
          activeDays: 1,
          streakDays: 1,
          challengePoints: 5,
          completedChallenges: 1,
          totalChallenges: 5,
          completedDailyChallenges: 1,
          totalPoints: 105,
          titles: [],
          challengeProgress: [
            {
              id: 9,
              code: 'DAILY_HEADSHOTS',
              name: 'Headshot sweep',
              cadence: 'DAILY',
              difficulty: null,
              day: '2026-01-06',
              metric: 'HEADSHOTS',
              currentValue: 10,
              targetValue: 10,
              unit: 'headshots',
              completed: true,
              rankingPoints: 5,
            },
          ],
        },
      ],
    };

    const order = buildDailyOrder(challenges, ranking);

    expect(order?.doneCount).toBe(1);
    expect(order?.validated).toEqual([{ name: 'Operator', done: true }]);
  });
});

describe('buildTally', () => {
  const today: CampaignToday = {
    day: '2026-01-06',
    damage: 500,
    food: 20,
    components: 10,
    presenceCount: 3,
    rosterSize: 5,
    dailyUpkeep: 15,
    carryGained: 4,
    shelterGained: 6,
    players: [],
    titles: {},
  };

  it('returns null while any input is missing', () => {
    expect(buildTally(null, week(), campaign())).toBeNull();
    expect(buildTally(today, null, campaign())).toBeNull();
    expect(buildTally(today, week(), campaign({ base: null }))).toBeNull();
  });

  it('lights one pip per operator who played today', () => {
    const tally = buildTally(today, week(), campaign());

    expect(tally?.pips).toEqual([true, true, true, false, false]);
  });
});

describe('buildSquad', () => {
  it('returns nothing while the daily ranking is unresolved', () => {
    expect(buildSquad(null, null, 'en')).toEqual([]);
  });

  it('excludes an inactive operator, who never consumes a ranking slot', () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
      previousDay: '2026-01-05',
      playedPlayerCount: 1,
      rosterPlayerCount: 2,
      ranking: [entry({ playerId: 1, position: 1 }), entry({ playerId: 2, position: null })],
    };

    const squad = buildSquad(daily, null, 'en');

    expect(squad).toHaveLength(1);
    expect(squad[0].playerId).toBe(1);
  });

  it('resolves the title an operator holds today', () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
      previousDay: '2026-01-05',
      playedPlayerCount: 1,
      rosterPlayerCount: 1,
      ranking: [entry({ playerId: 1, position: 1 })],
    };
    const today: CampaignToday = {
      day: '2026-01-06',
      damage: 0,
      food: 0,
      components: 0,
      presenceCount: 0,
      rosterSize: 0,
      dailyUpkeep: 0,
      carryGained: 0,
      shelterGained: 0,
      players: [],
      titles: { MECHANIC: 1 },
    };

    const [row] = buildSquad(daily, today, 'en');

    expect(row.title?.key).toBe('MECHANIC');
  });

  it("prices an unplayed operator's stake off yesterday's streak, not today's", () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
      previousDay: '2026-01-05',
      playedPlayerCount: 0,
      rosterPlayerCount: 1,
      ranking: [entry({ playerId: 1, position: 1, matchCount: 0, streakAtStake: 5 })],
    };

    const [row] = buildSquad(daily, null, 'en');

    expect(row.played).toBe(false);
    expect(row.streakDays).toBe(5);
  });

  function entry(overrides: {
    playerId: number;
    position: number | null;
    matchCount?: number;
    streakAtStake?: number;
  }) {
    return {
      position: overrides.position,
      playerId: overrides.playerId,
      displayName: `Player ${overrides.playerId}`,
      portrait: null,
      damage: 100,
      food: 10,
      components: 10,
      matchCount: overrides.matchCount ?? 1,
      reducedMatchCount: 0,
      streakDays: 3,
      streakBonusPercent: 4,
      streakAtStake: overrides.streakAtStake ?? 0,
      previousDamage: 0,
      damageVariation: 100,
    };
  }
});
