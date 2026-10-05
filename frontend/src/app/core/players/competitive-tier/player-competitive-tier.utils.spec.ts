import { describe, expect, it } from 'vitest';

import { CompetitiveTier } from './player-competitive-tier.model';
import {
  resolveCompetitiveTierColorVariable,
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
} from './player-competitive-tier.utils';

/**
 * Tier the backend could add before this build knows it.
 */
const UNKNOWN_TIER = 'MYTHIC_1' as CompetitiveTier;

describe('competitive tier visuals', () => {
  it('labels a known tier with its group and division', () => {
    const visual = resolveCompetitiveTierVisual('DIAMOND_2', (key) => key);
    expect(visual.label).toBe('players.tiers.diamond 2');
  });

  it('falls back to unranked for an unknown tier instead of throwing', () => {
    const unranked = resolveCompetitiveTierVisual('UNRANKED', (key) => key);
    expect(resolveCompetitiveTierVisual(UNKNOWN_TIER, (key) => key)).toEqual(unranked);
    expect(resolveCompetitiveTierIconUrl(UNKNOWN_TIER)).toBeNull();
    expect(resolveCompetitiveTierColorVariable(UNKNOWN_TIER)).toBe(
      resolveCompetitiveTierColorVariable('UNRANKED'),
    );
  });
});
