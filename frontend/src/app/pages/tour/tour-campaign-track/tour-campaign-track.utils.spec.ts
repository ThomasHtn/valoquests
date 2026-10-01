import { describe, expect, it } from 'vitest';

import { buildTourTrack } from './tour-campaign-track.utils';

describe('buildTourTrack', () => {
  const track = buildTourTrack(4, 'Hollin');

  it('lays out one planet per week', () => {
    expect(track.map((planet) => planet.weekIndex)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  });

  it('marks the weeks before the current one as done and the rest as ahead', () => {
    expect(track.map((planet) => planet.state)).toEqual([
      'done',
      'done',
      'done',
      'now',
      'ahead',
      'ahead',
      'ahead',
      'ahead',
      'ahead',
      'ahead',
    ]);
  });

  it('names the current planet and numbers the others', () => {
    expect(track[3].label).toBe('Hollin');
    expect(track[0].label).toBe('01');
    expect(track[9].label).toBe('10');
  });
});
