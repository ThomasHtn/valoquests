import { describe, expect, it } from 'vitest';

import { Campaign } from '@core/campaign/campaign.model';
import { CampaignHistory } from '@core/campaign/campaign-history.model';
import { formatFigure } from '@core/i18n/format/number-format.utils';

import {
  boardColumns,
  formatWeekSpan,
  placeWeekInCampaign,
  weekChallengeCeiling,
} from './leaderboard-board.utils';

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
  it('spells a shared month once, where the language writes it', () => {
    expect(formatWeekSpan('2026-09-07', 'en-US')).toBe('Sep 7 – 13');
    expect(formatWeekSpan('2026-09-07', 'fr-FR')).toBe('7 – 13 sept.');
  });

  it('spells both months when the week straddles them', () => {
    expect(formatWeekSpan('2026-08-31', 'en-US')).toBe('Aug 31 – Sep 6');
    expect(formatWeekSpan('2026-08-31', 'fr-FR')).toBe('31 août – 6 sept.');
  });
});

describe('formatFigure', () => {
  it('abbreviates on request and strips the spacing', () => {
    expect(formatFigure(27400, 'en', true)).toBe('27.4K');
    expect(formatFigure(27400, 'en')).toBe('27,400');
  });
});

describe('weekChallengeCeiling', () => {
  it('adds one daily per day to the weekly draw', () => {
    expect(weekChallengeCeiling(5)).toBe(12);
  });
});

describe('boardColumns', () => {
  it('lists the six figures in board order', () => {
    expect(boardColumns(true).map((column) => column.key)).toEqual([
      'score',
      'damage',
      'points',
      'challenges',
      'matches',
      'streak',
    ]);
  });

  it('names challenge earnings wounded in a campaign, points outside', () => {
    expect(boardColumns(true)[2].label).toBe('leaderboard.board.points');
    expect(boardColumns(false)[2].label).toBe('leaderboard.board.pointsOff');
    expect(boardColumns(false)[0].help).toBe('leaderboard.board.scoreHelpOff');
  });
});
