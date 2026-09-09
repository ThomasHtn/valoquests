import { describe, expect, it } from 'vitest';

import { Campaign, CampaignHistory } from '@core/campaign/campaign.model';
import { formatFigure, formatWeekSpan, placeWeekInCampaign } from './leaderboard-board.utils';

const running = {
  id: 7,
  weeks: [{ weekIndex: 2, weekStart: '2026-09-07' }],
} as unknown as Campaign;

const closed = [
  { id: 3, firstWeekStart: '2026-01-05', lastWeekStart: '2026-03-09' },
] as unknown as CampaignHistory[];

describe('placeWeekInCampaign', () => {
  it('finds a Monday in the running campaign first', () => {
    expect(placeWeekInCampaign('2026-09-07', running, closed)).toEqual({ index: 2, group: 7 });
  });

  it('counts the week index inside a closed campaign from its first Monday', () => {
    expect(placeWeekInCampaign('2026-01-19', running, closed)).toEqual({ index: 3, group: 3 });
  });

  it('leaves a Monday outside every campaign unplaced', () => {
    expect(placeWeekInCampaign('2025-06-02', running, closed)).toEqual({
      index: null,
      group: null,
    });
  });
});

describe('formatWeekSpan', () => {
  it('spells the month once when Monday and Sunday share it', () => {
    expect(formatWeekSpan('2026-09-07', 'en-US')).toBe('7 – Sep 13');
  });

  it('spells both months when the week straddles them', () => {
    expect(formatWeekSpan('2026-08-31', 'en-US')).toBe('Aug 31 – Sep 6');
  });
});

describe('formatFigure', () => {
  it('abbreviates on request and strips the spacing', () => {
    expect(formatFigure(27400, 'en-US', true)).toBe('27.4K');
    expect(formatFigure(27400, 'en-US')).toBe('27,400');
  });
});
