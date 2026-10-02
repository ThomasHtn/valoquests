import { describe, expect, it } from 'vitest';

import { resolveBackLabelKey, routePath, sameRoutePath } from './navigation-history.utils';

describe('routePath', () => {
  it('drops the query string and the fragment', () => {
    expect(routePath('/players/3?view=progress#top')).toBe('/players/3');
  });
});

describe('sameRoutePath', () => {
  it('ignores the query string', () => {
    expect(sameRoutePath('/players/3', '/players/3?mode=COMPETITIVE')).toBe(true);
    expect(sameRoutePath('/players/3', '/players/4')).toBe(false);
  });
});

describe('resolveBackLabelKey', () => {
  it('names the pages a back link can lead to', () => {
    expect(resolveBackLabelKey('/leaderboard')).toBe('sidebar.nav.leaderboard');
    expect(resolveBackLabelKey('/players/3?view=progress')).toBe(
      'playerProfile.matches.detail.back',
    );
  });

  it('names no other page', () => {
    expect(resolveBackLabelKey('/admin/players')).toBeNull();
  });
});
