import { describe, expect, it } from 'vitest';

import { CurrentRanking, RankingEntry } from '@core/ranking/ranking.model';
import { base, campaign, player, week } from '../overview.fixtures';
import { buildContribution, buildMission, buildSundayStakes } from './mission-readings.utils';

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

describe('buildSundayStakes', () => {
  const forecast = {
    weekIndex: 1,
    woundedCount: 100,
    challengeRescued: 10,
    extractionRescued: 12,
    rescued: 22,
    leftBehind: 78,
    limiter: 'FOOD' as const,
  };

  it('returns null once the guardian is down', () => {
    expect(buildSundayStakes(campaign({ forecast }), week({ defeated: true }))).toBeNull();
  });

  it('returns null without a forecast', () => {
    expect(buildSundayStakes(campaign({ forecast: null }), week())).toBeNull();
  });

  it('measures both outcomes against the forecast', () => {
    const stakes = buildSundayStakes(
      campaign({
        base: base({
          population: 2000,
          rescuesByComponents: 60,
          rescuesByFood: 40,
          guardianLossPercent: 35,
        }),
        forecast,
      }),
      week({ progressPercent: 30, defeated: false }),
    );

    // Food caps the reach at 40, 12 already forecast; the base loses 2000 x 0.7² x 35 %.
    expect(stakes).toEqual({ gain: 28, loss: 343 });
  });

  it('caps the reach at the group the challenges left', () => {
    const stakes = buildSundayStakes(
      campaign({
        base: base({ rescuesByComponents: 500, rescuesByFood: 500 }),
        forecast: { ...forecast, extractionRescued: 90 },
      }),
      week({ progressPercent: 100, defeated: false }),
    );

    expect(stakes?.gain).toBe(0);
    expect(stakes?.loss).toBe(0);
  });
});

describe('buildContribution', () => {
  function entry(id: number, damage: number, points: number): RankingEntry {
    return {
      position: id,
      competing: true,
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
      playedDays: 1,
      challengePoints: points,
      completedChallenges: 0,
      totalChallenges: 5,
      completedDailyChallenges: 0,
      totalPoints: damage + points,
      titles: [],
    };
  }

  function ranking(entries: readonly RankingEntry[]): CurrentRanking {
    return {
      weekStart: '2026-01-05',
      weekEnd: '2026-01-11',
      today: '2026-01-08',
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

  it('orders the segments by contribution', () => {
    const contribution = buildContribution(
      ranking([entry(1, 100, 0), entry(2, 300, 100)]),
      week({ guardianHitPoints: 1000 }),
    );

    expect(contribution?.total).toBe(500);
    expect(contribution?.hitPoints).toBe(1000);
    expect(contribution?.shares.map((share) => share.name)).toEqual(['Operator 2', 'Operator 1']);
    expect(contribution?.shares[0]).toMatchObject({ total: 400, sharePercent: 80 });
  });
});
