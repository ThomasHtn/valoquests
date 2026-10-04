import { describe, expect, it } from 'vitest';

import { mixColor, moonAt, skyAt, sunAt } from './town-sky-cycle.utils';
import { SKY_KEYS } from './town-scene.constants';

describe('skyAt', () => {
  it('returns a key exactly at its hour', () => {
    expect(skyAt(13)).toEqual(SKY_KEYS.find((key) => key.hour === 13)?.sky);
  });

  it('shows the stars at night and hides them at noon', () => {
    expect(skyAt(2).stars).toBe(1);
    expect(skyAt(12).stars).toBe(0);
  });

  it('lights more windows in the evening than in the afternoon', () => {
    expect(skyAt(21).lit).toBeGreaterThan(skyAt(15).lit);
  });

  it('wraps past midnight', () => {
    expect(skyAt(25)).toEqual(skyAt(1));
    expect(skyAt(-1)).toEqual(skyAt(23));
  });

  it('closes the day on the colour it opened with', () => {
    expect(skyAt(23.999).skyTop).toBe(skyAt(0).skyTop);
  });
});

describe('sunAt and moonAt', () => {
  it('keeps the sun up by day only', () => {
    expect(sunAt(13)?.elevation).toBeGreaterThan(0.8);
    expect(sunAt(2)).toBeNull();
  });

  it('keeps the moon up by night, across midnight', () => {
    expect(moonAt(23)).not.toBeNull();
    expect(moonAt(3)).not.toBeNull();
    expect(moonAt(13)).toBeNull();
  });

  it('crosses the sky from left to right', () => {
    expect(sunAt(8)!.x).toBeLessThan(sunAt(18)!.x);
  });
});

describe('mixColor', () => {
  it('returns the endpoints and blends in between', () => {
    expect(mixColor('#000000', '#ffffff', 0)).toBe('#000000');
    expect(mixColor('#000000', '#ffffff', 1)).toBe('#ffffff');
    expect(mixColor('#000000', '#ff0000', 0.5)).toBe('#800000');
  });
});
