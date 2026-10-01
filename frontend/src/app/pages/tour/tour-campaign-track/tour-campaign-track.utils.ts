import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.model';
import { resolvePlanetArtUrl } from '@core/campaign/planet-art.utils';

import { TourTrackPlanet, TourTrackState } from './tour-campaign-track.model';

/**
 * Lays the campaign's planets out in order, the weeks before the current one evacuated.
 *
 * @param weekIndex - One-based index of the current week.
 * @param planetName - Name of the current week's planet.
 * @returns One planet per week of the campaign.
 */
export function buildTourTrack(weekIndex: number, planetName: string): readonly TourTrackPlanet[] {
  return Array.from({ length: CAMPAIGN_WEEK_COUNT }, (_, offset) => {
    const index = offset + 1;
    const state: TourTrackState =
      index < weekIndex ? 'done' : index === weekIndex ? 'now' : 'ahead';
    return {
      weekIndex: index,
      art: resolvePlanetArtUrl(index),
      state,
      label: state === 'now' ? planetName : String(index).padStart(2, '0'),
    };
  });
}
