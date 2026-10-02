import { describe, expect, it } from 'vitest';

import { hullFigureSize } from './extraction-gauges.utils';

describe('hullFigureSize', () => {
  it('shrinks the figure as it gains digits', () => {
    expect(hullFigureSize(46)).toBe('2rem');
    expect(hullFigureSize(446)).toBe('1.875rem');
    expect(hullFigureSize(1959)).toBe('1.25rem');
    expect(hullFigureSize(1_234_567)).toBe('0.875rem');
  });
});
