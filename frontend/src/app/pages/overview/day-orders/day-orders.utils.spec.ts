import { describe, expect, it } from 'vitest';

import { CampaignToday } from '@core/campaign/campaign-today.model';
import { CurrentChallenges } from '@core/challenges/challenge.model';
import { base, campaign, player, translate, week } from '../overview.fixtures';
import { DayTally } from './day-orders.model';
import { buildDailyRow, buildTally, buildTallyTiles } from './day-orders.utils';

describe('buildDailyRow', () => {
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
        players: [],
      },
    ],
  };

  it('returns null without a challenge draw', () => {
    expect(buildDailyRow(null, false, '', String, 'en', translate)).toBeNull();
  });

  it('returns null when today has no daily challenge', () => {
    expect(
      buildDailyRow({ ...challenges, dailies: [] }, false, '', String, 'en', translate),
    ).toBeNull();
  });

  it('draws one line per roster operator, the validated first', () => {
    const row = buildDailyRow(
      {
        ...challenges,
        roster: [
          { id: 1, displayName: 'Operator 1', portrait: null },
          { id: 2, displayName: 'Operator 2', portrait: null },
        ],
        dailies: [
          {
            ...challenges.dailies[0],
            players: [
              { playerId: 1, currentValue: 4, completed: false },
              { playerId: 2, currentValue: 10, completed: true },
            ],
          },
        ],
      },
      true,
      'Daily challenge',
      String,
      'en',
      translate,
    );

    expect(row?.doneCount).toBe(1);
    expect(row?.rescueActive).toBe(true);
    expect(row?.daily).toBe(true);
    expect(row?.marks.map((mark) => [mark.name, mark.fraction])).toEqual([
      ['Operator 2', 1],
      ['Operator 1', 0.4],
    ]);
    // Ten units: the first four segments lit.
    expect(row?.marks[1].segments.filter(Boolean)).toHaveLength(4);
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

  it('names and portrays each operator who played, leaving empty slots blank', () => {
    const played = {
      ...today,
      presenceCount: 1,
      rosterSize: 2,
      players: [
        {
          playerId: 7,
          gameName: 'Sable',
          tagLine: 'EU1',
          damage: 500,
          food: 20,
          components: 10,
          matchCount: 2,
          reducedMatchCount: 0,
          streakDays: 1,
          streakBonusPercent: 0,
        },
      ],
    };

    const tally = buildTally(played, week(), campaign(), [player({ id: 7, portrait: 'Sova' })]);

    expect(tally?.pips).toEqual([
      { name: 'Sable', portrait: '/player-avatars/Sova.webp', on: true },
      { name: null, portrait: null, on: false },
    ]);
  });

  it('carries the base population change through', () => {
    const tally = buildTally(today, week(), campaign({ base: base({ populationChange: -4 }) }));

    expect(tally?.populationChange).toBe(-4);
  });
});

describe('buildTallyTiles', () => {
  const tally: DayTally = {
    weekIndex: 1,
    damage: 0,
    components: 300,
    carryGained: 20,
    food: 0,
    shelterGained: 0,
    upkeep: 63,
    population: 900,
    populationChange: -12,
    presence: 0,
    roster: 0,
    pips: [],
  };
  const tiles = buildTallyTiles(tally, translate, String);

  it('signs each flow, the upkeep as a loss', () => {
    expect(tiles.map((tile) => tile.figure)).toEqual(['+300', '0', '−12', '−63']);
  });

  it('turns the population tile red when the base shrinks', () => {
    expect(tiles.map((tile) => tile.tone)).toEqual(['components', 'food', 'decline', 'cost']);
  });

  it('chips the capacity bought by a resource, notes the others', () => {
    expect(tiles[0]).toMatchObject({ gain: 'overview.orders.carryGain(20)', note: null });
    expect(tiles[2]).toMatchObject({ gain: null, note: 'overview.orders.tonight(900)' });
  });
});
