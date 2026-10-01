import { describe, expect, it } from 'vitest';

import { SeasonRank } from '@core/players/player-progression.model';
import {
  buildRankJourneyAxisLabels,
  buildRankJourneyFigures,
  buildRankJourneySeries,
  buildRankJourneyTooltip,
  describeRankDelta,
  findPeakSeasonIndex,
} from './rank-journey.utils';

/**
 * Stands in for the translation service, echoing the key and its parameters.
 */
function translate(key: string, params?: Readonly<Record<string, string | number>>): string {
  const suffix = Object.entries(params ?? {})
    .map(([name, value]) => `${name}=${value}`)
    .join(',');
  return suffix ? `${key}(${suffix})` : key;
}

/**
 * Builds one season of the journey.
 */
function season(
  seasonName: string,
  finalTier: SeasonRank['finalTier'],
  lowestTier: SeasonRank['finalTier'] = finalTier,
  highestTier: SeasonRank['finalTier'] = finalTier,
): SeasonRank {
  return {
    seasonId: 1,
    seasonName,
    active: false,
    finalTier,
    highestTier,
    lowestTier,
    matchesPlayed: 10,
    wins: 4,
    rankedTiers: [lowestTier, highestTier, finalTier],
  };
}

const JOURNEY: readonly SeasonRank[] = [
  season('e10a6', 'GOLD_3'),
  season('e11a1', 'PLATINUM_3'),
  season('e11a2', 'DIAMOND_1', 'PLATINUM_3', 'DIAMOND_1'),
  season('e11a3', 'PLATINUM_2', 'PLATINUM_2', 'DIAMOND_1'),
];

describe('findPeakSeasonIndex', () => {
  it('picks the first season to reach the highest rank', () => {
    expect(findPeakSeasonIndex(JOURNEY)).toBe(2);
  });
});

describe('describeRankDelta', () => {
  it('counts a climb in divisions, as a gain', () => {
    expect(describeRankDelta('GOLD_3', 'PLATINUM_2', translate)).toEqual({
      label: 'playerProfile.progression.rankJourney.divisions(sign=+,count=2)',
      icon: 'trendUp',
      tone: 'good',
    });
  });

  it('uses the singular for a single division lost', () => {
    expect(describeRankDelta('PLATINUM_3', 'PLATINUM_2', translate)).toEqual({
      label: 'playerProfile.progression.rankJourney.division(sign=−,count=1)',
      icon: 'trendDown',
      tone: 'bad',
    });
  });

  it('calls an unchanged rank stable', () => {
    expect(describeRankDelta('GOLD_1', 'GOLD_1', translate).tone).toBe('neutral');
  });
});

describe('buildRankJourneyAxisLabels', () => {
  it('names the era under the first act of each era only', () => {
    expect(buildRankJourneyAxisLabels(JOURNEY, false, translate)).toEqual([
      ['playerProfile.progression.rankJourney.axisAct(act=6)', 'seasons.episodeOnly(episode=10)'],
      ['playerProfile.progression.rankJourney.axisAct(act=1)', 'seasons.episodeOnly(episode=11)'],
      ['playerProfile.progression.rankJourney.axisAct(act=2)', ''],
      ['playerProfile.progression.rankJourney.axisAct(act=3)', ''],
    ]);
  });
});

describe('buildRankJourneySeries', () => {
  it('plots one point per season across several seasons', () => {
    const series = buildRankJourneySeries(JOURNEY, false, translate);

    expect(series.mode).toBe('seasons');
    expect(series.tiers).toEqual(['GOLD_3', 'PLATINUM_3', 'DIAMOND_1', 'PLATINUM_2']);
    expect(series.peakIndex).toBe(2);
    expect(series.peakOnPoint).toBe(true);
  });

  it('plots one point per ranked match within a single season', () => {
    const single = [
      { ...season('e11a4', 'PLATINUM_2'), rankedTiers: ['PLATINUM_3', 'DIAMOND_1', 'PLATINUM_2'] },
    ] as const satisfies readonly SeasonRank[];
    const series = buildRankJourneySeries(single, false, translate);

    expect(series.mode).toBe('matches');
    expect(series.labels).toEqual(['1', '2', '3']);
    expect(series.peakIndex).toBe(1);
    expect(series.peakTier).toBe('DIAMOND_1');
  });
});

describe('buildRankJourneyFigures', () => {
  it('reports the peak, the last act and the climb since the first act', () => {
    const [peak, last, climb] = buildRankJourneyFigures(JOURNEY, translate);

    expect(peak.rankIconUrl).toBe('/ranks/diamond-1.svg');
    expect(peak.detail).toBe('seasons.episode(episode=11,act=2)');
    expect(last.rankIconUrl).toBe('/ranks/platinum-2.svg');
    expect(climb.value).toBe('playerProfile.progression.rankJourney.divisions(sign=+,count=2)');
    expect(climb.tone).toBe('good');
  });

  it('measures the climb from the first match of a single season', () => {
    const single = [
      { ...season('e11a4', 'PLATINUM_2'), rankedTiers: ['PLATINUM_3', 'DIAMOND_1', 'PLATINUM_2'] },
    ] as const satisfies readonly SeasonRank[];
    const [, last, climb] = buildRankJourneyFigures(single, translate);

    expect(last.caption).toBe('playerProfile.progression.rankJourney.figures.final');
    expect(climb.value).toBe('playerProfile.progression.rankJourney.division(sign=−,count=1)');
    expect(climb.detail).toBe('playerProfile.progression.rankJourney.figures.sinceStart');
  });
});

describe('buildRankJourneyTooltip', () => {
  const series = buildRankJourneySeries(JOURNEY, false, translate);

  it('shows the range only for a season that left its rank', () => {
    expect(buildRankJourneyTooltip(JOURNEY, series, 1, translate)?.lowest).toBeNull();
    expect(buildRankJourneyTooltip(JOURNEY, series, 3, translate)?.highest?.iconUrl).toBe(
      '/ranks/diamond-1.svg',
    );
  });

  it('has no move for the first point', () => {
    expect(buildRankJourneyTooltip(JOURNEY, series, 0, translate)?.delta).toBeNull();
  });

  it('describes a match by its position and leaves the season record out', () => {
    const single = [
      { ...season('e11a4', 'PLATINUM_2'), rankedTiers: ['PLATINUM_3', 'DIAMOND_1', 'PLATINUM_2'] },
    ] as const satisfies readonly SeasonRank[];
    const tip = buildRankJourneyTooltip(
      single,
      buildRankJourneySeries(single, false, translate),
      1,
      translate,
    );

    expect(tip?.heading).toBe('playerProfile.progression.rankJourney.tip.match(index=2,total=3)');
    expect(tip?.delta?.tone).toBe('good');
    expect(tip?.matchesPlayed).toBeNull();
  });

  it('returns null past the last point', () => {
    expect(buildRankJourneyTooltip(JOURNEY, series, 9, translate)).toBeNull();
  });
});
