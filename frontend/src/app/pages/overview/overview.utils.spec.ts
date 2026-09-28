import { describe, expect, it } from 'vitest';

import { Campaign, CampaignBase, CampaignToday, CampaignWeek } from '@core/campaign/campaign.model';
import { CurrentChallenges } from '@core/challenges/challenge.model';
import {
  CurrentRanking,
  DailyRanking,
  RankingEntry,
  RankingHistoryWeek,
} from '@core/ranking/ranking.model';
import { PlayerSummary } from '@core/players/player-summary.model';
import {
  buildCapacity,
  buildContribution,
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
    difficulty: 'AMATEUR',
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

  it('names each week after its planet, its drawing and its guardian level, abbreviated', () => {
    const [entry] = buildFrieze(
      campaign({ weeks: [week({ weekIndex: 3, category: 'ELITE' })] }),
      translate,
    );

    expect(entry).toMatchObject({
      label: '03',
      name: 'Kepler',
      art: '/planets/planet-03.svg',
      level: 'overview.frieze.level.ELITE',
      title: 'overview.frieze.title(common.guardianCategory.ELITE,overview.frieze.ahead)',
    });
  });

  it('flags only the weeks Sunday has settled, whose report can be opened', () => {
    const [done, running] = buildFrieze(
      campaign({
        currentWeekIndex: 2,
        weeks: [week({ weekIndex: 1, settled: true }), week({ weekIndex: 2, defeated: true })],
      }),
      translate,
    );

    expect(done.settled).toBe(true);
    expect(running).toMatchObject({ state: 'won', settled: false });
  });

  it('marks a defeated week as won, its ring empty', () => {
    const [entry] = buildFrieze(campaign({ weeks: [week({ defeated: true })] }), translate);

    expect(entry).toMatchObject({
      state: 'won',
      standing: 0,
      status: 'overview.frieze.status.won',
    });
  });

  it('marks a settled but undefeated week as lost, with the hit points it kept', () => {
    const [entry] = buildFrieze(
      campaign({ weeks: [week({ settled: true, progressPercent: 78 })] }),
      translate,
    );

    expect(entry).toMatchObject({
      state: 'lost',
      standing: 0.22,
      status: 'overview.frieze.status.lost(22)',
      title: 'overview.frieze.title(common.guardianCategory.STANDARD,overview.frieze.lost(22))',
    });
  });

  it('marks the week in progress as now, with its breakthrough', () => {
    const [entry] = buildFrieze(
      campaign({ currentWeekIndex: 1, weeks: [week({ progressPercent: 30 })] }),
      translate,
    );

    expect(entry).toMatchObject({
      state: 'now',
      standing: 0.7,
      status: 'overview.frieze.status.now(30)',
    });
  });

  it('marks a week still ahead as ahead', () => {
    const [entry] = buildFrieze(
      campaign({ currentWeekIndex: 1, weeks: [week({ weekIndex: 4 })] }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', status: 'overview.frieze.status.ahead' });
  });

  it('announces the final unplayed week of a still-running campaign', () => {
    const [entry] = buildFrieze(
      campaign({
        currentWeekIndex: 1,
        weeks: [week({ weekIndex: 10 })],
      }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', status: 'overview.frieze.status.final' });
  });

  it('calls an unplayed week of a closed campaign unplayed rather than the final', () => {
    const [entry] = buildFrieze(
      campaign({ status: 'CLOSED', currentWeekIndex: null, weeks: [week({ weekIndex: 10 })] }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', status: 'overview.frieze.status.unplayed' });
    expect(entry.title).toBe(
      'overview.frieze.title(common.guardianCategory.STANDARD,overview.frieze.unplayed)',
    );
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

  it('reports the last settled week unless another one is asked for', () => {
    const weeks = [
      week({ weekIndex: 1, planetName: 'Orune', settled: true, defeated: true }),
      week({ weekIndex: 2, planetName: 'Vell', settled: true }),
      week({ weekIndex: 3, planetName: 'Tessar' }),
    ];

    expect(buildMissionReport(campaign({ weeks }), [], [], 'en', translate)).toMatchObject({
      weekIndex: 2,
      planetName: 'Vell',
    });
    expect(buildMissionReport(campaign({ weeks }), [], [], 'en', translate, 1)).toMatchObject({
      weekIndex: 1,
      planetName: 'Orune',
      defeated: true,
    });
  });

  it('reports nothing for a week Sunday has not settled yet', () => {
    const weeks = [week({ weekIndex: 1, settled: true }), week({ weekIndex: 2 })];

    expect(buildMissionReport(campaign({ weeks }), [], [], 'en', translate, 2)).toBeNull();
  });

  it('reports no titles and no champion when the week was never frozen', () => {
    const report = buildMissionReport(
      campaign({ weeks: [week({ settled: true })] }),
      [],
      [],
      'en',
      translate,
    );

    expect(report?.titles).toBeNull();
    expect(report?.champion).toBeNull();
  });

  it('resolves the titles and the champion once the week is frozen', () => {
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

    expect(report?.titles?.find((title) => title.key === 'SCOUT')?.holder).toBe('Scout Prime');
    expect(report?.champion).toEqual({ holder: 'Scout Prime', portrait: null, points: 600 });
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

    expect(report?.blow?.when).toContain('overview.missionReport.blow(');
    expect(report?.blow?.by).toBe('Killjoy Main');
  });

  it('adds the map, mode and score of the fatal blow when the match is known', () => {
    const report = buildMissionReport(
      campaign({
        weeks: [
          week({
            settled: true,
            defeated: true,
            defeatedAt: '2026-01-07T18:30:00Z',
            fatalBlow: {
              mapName: 'Ascent',
              gameMode: 'COMPETITIVE',
              result: 'WIN',
              allyScore: 13,
              enemyScore: 9,
              agentName: 'Sova',
            },
          }),
        ],
      }),
      [],
      [],
      'en',
      translate,
    );

    expect(report?.blow?.by).toBeNull();
    expect(report?.blow?.where).toBe(
      'overview.missionReport.blowWhere(Ascent,common.gameMode.COMPETITIVE)' +
        'overview.missionReport.blowScore(13,9)',
    );
  });

  it('counts the wounded left on the planet and the share brought home', () => {
    const report = buildMissionReport(
      campaign({
        weeks: [
          week({ settled: true, woundedCount: 10, challengeRescued: 2, extractionRescued: 6 }),
        ],
      }),
      [],
      [],
      'en',
      translate,
    );

    expect(report?.rescued).toBe(8);
    expect(report?.leftBehind).toBe(2);
    expect(report?.rescuedShare).toBe(0.8);
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

describe('buildContribution', () => {
  function entry(id: number, damage: number, points: number): RankingEntry {
    return {
      position: id,
      competing: true,
      previousPosition: id,
      positionVariation: 0,
      player: {
        id,
        displayName: `Operator ${id}`,
        portrait: null,
        competitiveTier: 'GOLD_1',
        rankRating: null,
      },
      guardianDamage: damage,
      food: 0,
      components: 0,
      matchCount: 1,
      activeDays: 1,
      streakDays: 1,
      challengePoints: points,
      completedChallenges: 0,
      totalChallenges: 5,
      completedDailyChallenges: 0,
      totalPoints: damage + points,
      titles: [],
      challengeProgress: [],
    };
  }

  function ranking(entries: readonly RankingEntry[]): CurrentRanking {
    return {
      weekStart: '2026-01-05',
      weekEnd: '2026-01-11',
      today: '2026-01-08',
      calculatedAt: null,
      ranking: entries,
    };
  }

  it('returns null without a ranking to read', () => {
    expect(buildContribution(null, week())).toBeNull();
    expect(buildContribution(ranking([]), week())).toBeNull();
  });

  it("keeps the reading with an empty bar before the week's first match", () => {
    const contribution = buildContribution(ranking([entry(1, 0, 0)]), week());

    expect(contribution?.total).toBe(0);
    expect(contribution?.shares).toEqual([]);
  });

  it('orders the segments by contribution and measures them on the guardian', () => {
    const contribution = buildContribution(
      ranking([entry(1, 100, 0), entry(2, 300, 100)]),
      week({ guardianHitPoints: 1000 }),
    );

    expect(contribution?.total).toBe(500);
    expect(contribution?.hitPoints).toBe(1000);
    expect(contribution?.shares.map((share) => share.name)).toEqual(['Operator 2', 'Operator 1']);
    expect(contribution?.shares[0]).toMatchObject({ total: 400, fraction: 0.4, sharePercent: 80 });
  });

  it('caps a segment at the whole bar once one operator covers the guardian alone', () => {
    const contribution = buildContribution(
      ranking([entry(1, 4000, 0)]),
      week({ guardianHitPoints: 1000 }),
    );

    expect(contribution?.shares[0].fraction).toBe(1);
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
          competing: true,
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

    expect(tally?.pips.map((pip) => pip.on)).toEqual([true, true, true, false, false]);
  });

  it('carries the base population change through', () => {
    const tally = buildTally(today, week(), campaign({ base: base({ populationChange: -4 }) }));

    expect(tally?.populationChange).toBe(-4);
  });
});

describe('buildSquad', () => {
  it('returns nothing while the daily ranking is unresolved', () => {
    expect(buildSquad(null, null)).toEqual([]);
  });

  it('excludes an inactive operator, who never consumes a ranking slot', () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
      previousDay: '2026-01-05',
      playedPlayerCount: 1,
      rosterPlayerCount: 2,
      ranking: [
        entry({ playerId: 1, position: 1 }),
        entry({ playerId: 2, position: null, competing: false }),
      ],
    };

    const squad = buildSquad(daily, null);

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

    const [row] = buildSquad(daily, today);

    expect(row.title?.key).toBe('MECHANIC');
  });

  it('shows an unplayed operator the days already played and the bonus playing today would earn', () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
      previousDay: '2026-01-05',
      playedPlayerCount: 0,
      rosterPlayerCount: 1,
      ranking: [entry({ playerId: 1, position: 1, matchCount: 0, weekPlayedDays: ['2026-01-05'] })],
    };

    const [row] = buildSquad(daily, null);

    expect(row.played).toBe(false);
    expect(row.streakDays).toBe(1);
    expect(row.streakBonusPercent).toBe(2);
    expect(row.streakWeek).toEqual([
      'played',
      'today',
      'ahead',
      'ahead',
      'ahead',
      'ahead',
      'ahead',
    ]);
  });

  it("shows a played operator's own streak and bonus, a skipped day left unlit", () => {
    const daily: DailyRanking = {
      day: '2026-01-08',
      previousDay: '2026-01-07',
      playedPlayerCount: 1,
      rosterPlayerCount: 1,
      ranking: [
        entry({
          playerId: 1,
          position: 1,
          weekPlayedDays: ['2026-01-05', '2026-01-07', '2026-01-08'],
        }),
      ],
    };

    const [row] = buildSquad(daily, null);

    expect(row.streakDays).toBe(3);
    expect(row.streakBonusPercent).toBe(4);
    expect(row.streakWeek).toEqual([
      'played',
      'missed',
      'played',
      'played',
      'ahead',
      'ahead',
      'ahead',
    ]);
  });

  function entry(overrides: {
    playerId: number;
    position: number | null;
    competing?: boolean;
    matchCount?: number;
    weekPlayedDays?: readonly string[];
  }) {
    return {
      position: overrides.position,
      competing: overrides.competing ?? true,
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
      weekPlayedDays: overrides.weekPlayedDays ?? [],
      previousDamage: 0,
      damageVariation: 100,
    };
  }
});
