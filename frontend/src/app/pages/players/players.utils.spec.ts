import { convertToParamMap } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { readPlayerSort, toPlayerSortOrder, writePlayerSort } from './players.utils';

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
