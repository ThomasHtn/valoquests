import { convertToParamMap } from '@angular/router';

import { describe, expect, it } from 'vitest';

import { PlayerRow } from './players.model';
import {
  readPlayerSort,
  sortPlayerRows,
  toPlayerSortOrder,
  writePlayerSort,
} from './players.utils';

/**
 * Row with only the sorted fields set.
 */
function row(id: number, overrides: Partial<PlayerRow>): PlayerRow {
  return {
    id,
    displayName: `P${id}`,
    competitiveTier: 'UNRANKED',
    rankRating: null,
    kda: null,
    ...overrides,
  } as PlayerRow;
}

describe('player sort in the address', () => {
  it('reads a known column and direction', () => {
    expect(readPlayerSort(convertToParamMap({ sort: 'kda', dir: 'asc' }))).toEqual({
      key: 'kda',
      direction: 1,
    });
  });

  it('falls back to rank, best first, on anything else', () => {
    expect(readPlayerSort(convertToParamMap({ sort: 'nope' }))).toEqual({
      key: 'rank',
      direction: -1,
    });
  });

  it('leaves the default out of the address', () => {
    expect(writePlayerSort('rank', -1)).toEqual({ sort: null, dir: null });
    expect(writePlayerSort('name', 1)).toEqual({ sort: 'name', dir: 'asc' });
  });
});

describe('toPlayerSortOrder', () => {
  it('words each column in its own terms', () => {
    expect(toPlayerSortOrder('name', 1)).toBe('az');
    expect(toPlayerSortOrder('name', -1)).toBe('za');
    expect(toPlayerSortOrder('rank', -1)).toBe('best');
    expect(toPlayerSortOrder('rank', 1)).toBe('worst');
    expect(toPlayerSortOrder('kda', -1)).toBe('high');
    expect(toPlayerSortOrder('kda', 1)).toBe('low');
  });
});

describe('sortPlayerRows', () => {
  const ids = (rows: readonly PlayerRow[]): number[] => rows.map((entry) => entry.id);

  it('sorts on tier, then rating, best first', () => {
    const rows = [
      row(1, { competitiveTier: 'GOLD_1', rankRating: 10 }),
      row(2, { competitiveTier: 'DIAMOND_1', rankRating: 5 }),
      row(3, { competitiveTier: 'GOLD_1', rankRating: 80 }),
    ];
    expect(ids(sortPlayerRows(rows, 'rank', -1))).toEqual([2, 3, 1]);
    expect(ids(sortPlayerRows(rows, 'rank', 1))).toEqual([1, 3, 2]);
  });

  it('keeps missing figures last in both directions', () => {
    const rows = [row(1, { kda: null }), row(2, { kda: 1.2 }), row(3, { kda: 0.8 })];
    expect(ids(sortPlayerRows(rows, 'kda', -1))).toEqual([2, 3, 1]);
    expect(ids(sortPlayerRows(rows, 'kda', 1))).toEqual([3, 2, 1]);
  });
});
