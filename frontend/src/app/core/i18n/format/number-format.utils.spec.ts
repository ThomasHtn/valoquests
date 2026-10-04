import { describe, expect, it } from 'vitest';

import { formatDecimal, formatFigure, formatPercent } from './number-format.utils';

// Intl separates French figures with narrow no-break spaces; compared on plain spaces here.
const plain = (text: string): string => text.replace(/[\u00a0\u202f]/g, ' ');

describe('formatDecimal', () => {
  it('writes a decimal point in every language', () => {
    expect(formatDecimal(1.578, 'fr', 2)).toBe('1.58');
    expect(formatDecimal(1.578, 'en', 2)).toBe('1.58');
  });
});

describe('formatPercent', () => {
  it('spaces the sign in French only', () => {
    expect(plain(formatPercent(23.44, 'fr', 1))).toBe('23.4 %');
    expect(formatPercent(23.44, 'en', 1)).toBe('23.4%');
  });

  it('rounds to whole percents by default', () => {
    expect(plain(formatPercent(55.6, 'fr'))).toBe('56 %');
  });
});

describe('formatFigure', () => {
  it('keeps the French grouping beside a decimal point', () => {
    expect(plain(formatFigure(1234.5, 'fr-FR'))).toBe('1 234.5');
    expect(formatFigure(12700, 'fr-FR', true)).toBe('12.7k');
  });
});
