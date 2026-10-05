import { describe, expect, it } from 'vitest';

import { easeInOutQuad } from './count-up.utils';

describe('easeInOutQuad', () => {
  it('starts at 0, crosses the middle at half way and ends at 1', () => {
    expect(easeInOutQuad(0)).toBe(0);
    expect(easeInOutQuad(0.5)).toBe(0.5);
    expect(easeInOutQuad(1)).toBe(1);
  });

  it('starts slow and ends slow', () => {
    expect(easeInOutQuad(0.25)).toBe(0.125);
    expect(easeInOutQuad(0.75)).toBe(0.875);
  });
});
