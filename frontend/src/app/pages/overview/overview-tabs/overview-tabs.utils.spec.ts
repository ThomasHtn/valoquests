import { describe, expect, it } from 'vitest';

import { translate } from '../overview.fixtures';
import { buildTabs } from './overview-tabs.utils';

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
