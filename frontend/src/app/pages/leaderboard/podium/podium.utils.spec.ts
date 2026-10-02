import { describe, expect, it } from 'vitest';

import { BoardRow } from '../leaderboard.model';
import { groupPodium } from './podium.utils';

const row = (playerId: number, position: number | null): BoardRow =>
  ({ playerId, position }) as BoardRow;

describe('groupPodium', () => {
  it('puts one operator on each plinth without ties', () => {
    const places = groupPodium([row(1, 1), row(2, 2), row(3, 3), row(4, 4)]);
    expect(places.map((place) => place.rows.length)).toEqual([1, 1, 1]);
  });

  it('shares the plinth on a tie and leaves the skipped place empty', () => {
    const places = groupPodium([row(1, 1), row(2, 1), row(3, 3)]);
    expect(places.map((place) => place.position)).toEqual([1, 3]);
    expect(places[0].rows.map((entry) => entry.playerId)).toEqual([1, 2]);
  });

  it('ignores unranked rows', () => {
    expect(groupPodium([row(1, null)])).toEqual([]);
  });
});
