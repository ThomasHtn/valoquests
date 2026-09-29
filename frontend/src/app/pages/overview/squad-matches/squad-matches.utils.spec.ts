import { describe, expect, it } from 'vitest';
import { Match } from '@core/matches/match.model';
import { toHistoryMatch } from './squad-matches.utils';

const MATCH = { id: 42, mapName: 'Ascent' } as Match;

describe('toHistoryMatch', () => {
  it('names the match after its player, with the bundled avatar resolved', () => {
    const row = toHistoryMatch({
      playerId: 3,
      displayName: 'natank',
      portrait: 'sova',
      match: MATCH,
    });

    expect(row.id).toBe(42);
    expect(row.mapName).toBe('Ascent');
    expect(row.player).toEqual({ id: 3, name: 'natank', portrait: '/player-avatars/Sova.webp' });
  });

  it('leaves the portrait empty when the player has none', () => {
    const row = toHistoryMatch({
      playerId: 3,
      displayName: 'natank',
      portrait: null,
      match: MATCH,
    });

    expect(row.player?.portrait).toBeNull();
  });
});
