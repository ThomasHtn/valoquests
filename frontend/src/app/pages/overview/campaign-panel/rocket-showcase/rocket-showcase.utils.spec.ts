import { describe, expect, it } from 'vitest';

import { blueprintInk } from './rocket-showcase.utils';

describe('blueprintInk', () => {
  it('writes the ink at the given opacity', () => {
    expect(blueprintInk(35)).toBe('rgb(127 182 216 / 35%)');
  });

  it('defaults to the opaque ink', () => {
    expect(blueprintInk()).toBe('rgb(127 182 216 / 100%)');
  });
});
