import { describe, expect, it } from 'vitest';

import { translate } from '../overview.fixtures';
import { buildTabs, keyedTabIndex } from './overview-tabs.utils';

describe('buildTabs', () => {
  it('keeps the default order when no tab is pinned', () => {
    expect(buildTabs(translate).map((tab) => tab.key)).toEqual([
      'challenges',
      'contributions',
      'matches',
      'campaign',
    ]);
  });

  it('moves the pinned tab to the front, the others keeping their order', () => {
    expect(buildTabs(translate, 'matches').map((tab) => tab.key)).toEqual([
      'matches',
      'challenges',
      'contributions',
      'campaign',
    ]);
  });
});

describe('keyedTabIndex', () => {
  it('wraps the arrow keys around the bar', () => {
    expect(keyedTabIndex('ArrowRight', 3, 4)).toBe(0);
    expect(keyedTabIndex('ArrowLeft', 0, 4)).toBe(3);
  });

  it('jumps to the ends on Home and End, and ignores other keys', () => {
    expect(keyedTabIndex('Home', 2, 4)).toBe(0);
    expect(keyedTabIndex('End', 1, 4)).toBe(3);
    expect(keyedTabIndex('Enter', 1, 4)).toBeNull();
  });
});
