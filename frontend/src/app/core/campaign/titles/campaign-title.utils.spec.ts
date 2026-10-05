import { describe, expect, it } from 'vitest';

import { RankingEntry } from '@core/ranking/ranking.model';
import { WeeklyTitle } from './campaign-title.model';
import { buildTitlesByPlayer, primaryTitleOf } from './campaign-title.utils';

/**
 * Ranking line carrying only what the title helpers read.
 */
function entry(playerId: number, titles: readonly WeeklyTitle[]): RankingEntry {
  return { player: { id: playerId }, titles } as unknown as RankingEntry;
}

describe('buildTitlesByPlayer', () => {
  it('keeps the first title of each player and skips the untitled', () => {
    const titles = buildTitlesByPlayer([entry(1, ['SCOUT', 'REGULAR']), entry(2, [])]);

    expect(titles.get(1)).toBe('SCOUT');
    expect(titles.has(2)).toBe(false);
  });
});

describe('primaryTitleOf', () => {
  it('picks the first title in ladder order the player holds', () => {
    expect(primaryTitleOf({ MECHANIC: 3, SCOUT: 3 }, 3)).toBe('SCOUT');
    expect(primaryTitleOf({ MECHANIC: 3 }, 4)).toBeNull();
  });
});
