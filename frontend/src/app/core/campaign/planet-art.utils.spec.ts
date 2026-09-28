import { describe, expect, it } from 'vitest';

import { isRingedPlanet, resolvePlanetArtUrl } from './planet-art.utils';

describe('resolvePlanetArtUrl', () => {
  it('picks the drawing of the week, zero-padded', () => {
    expect(resolvePlanetArtUrl(1)).toBe('/planets/planet-01.svg');
    expect(resolvePlanetArtUrl(10)).toBe('/planets/planet-10.svg');
  });

  it('clamps an index outside the ten weeks onto the nearest drawing', () => {
    expect(resolvePlanetArtUrl(0)).toBe('/planets/planet-01.svg');
    expect(resolvePlanetArtUrl(14)).toBe('/planets/planet-10.svg');
  });
});

describe('isRingedPlanet', () => {
  it('flags only the drawings whose rings reach past the globe', () => {
    expect(isRingedPlanet(4)).toBe(false);
    expect(isRingedPlanet(9)).toBe(true);
    expect(isRingedPlanet(10)).toBe(true);
  });

  it('reads an index outside the ten weeks on the nearest drawing', () => {
    expect(isRingedPlanet(14)).toBe(true);
  });
});
