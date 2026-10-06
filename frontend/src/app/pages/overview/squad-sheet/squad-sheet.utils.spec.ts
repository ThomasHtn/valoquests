import { describe, expect, it } from 'vitest';

import { CampaignToday } from '@core/campaign/campaign-today.model';
import { DailyRanking } from '@core/ranking/ranking.model';
import { buildSquad } from './squad-sheet.utils';

describe('buildSquad', () => {
  it('returns nothing while the daily ranking is unresolved', () => {
    expect(buildSquad(null, null)).toEqual([]);
  });

  it('excludes an inactive operator, who never consumes a ranking slot', () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
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
      rescuesByComponentsGained: 0,
      rescuesByFoodGained: 0,
      players: [],
      titles: { MECHANIC: 1 },
    };

    const [row] = buildSquad(daily, today);

    expect(row.title?.key).toBe('MECHANIC');
  });

  it('crowns the reigning champion', () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
      rosterPlayerCount: 2,
      ranking: [entry({ playerId: 1, position: 1 }), entry({ playerId: 2, position: 2 })],
    };

    const squad = buildSquad(daily, null, 2);

    expect(squad.map((row) => row.champion)).toEqual([false, true]);
  });

  it('shows an unplayed operator the days already played and the bonus playing today would earn', () => {
    const daily: DailyRanking = {
      day: '2026-01-06',
      rosterPlayerCount: 1,
      ranking: [entry({ playerId: 1, position: 1, matchCount: 0, weekPlayedDays: ['2026-01-05'] })],
    };

    const [row] = buildSquad(daily, null);

    expect(row.played).toBe(false);
    expect(row.playedDays).toBe(1);
    expect(row.streakBonusPercent).toBe(2);
    expect(row.streakWeek).toEqual([
      'played',
      'missed',
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

    expect(row.playedDays).toBe(3);
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
      playedDays: 3,
      streakBonusPercent: 4,
      weekPlayedDays: overrides.weekPlayedDays ?? [],
    };
  }
});
