import { describe, expect, it } from 'vitest';

import { RankingHistoryWeek } from '@core/ranking/ranking.model';

import { campaign, player, translate, week } from '../overview.fixtures';
import { buildMissionReport } from './mission-report.utils';

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
            matchCount: 12,
            playedDays: 7,
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
