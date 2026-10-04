import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { resolvePlanetArtUrl } from '@core/campaign/planets/campaign-planet-art.utils';

import { TourTrackPlanet, TourTrackState } from './tour-campaign-track.model';

/**
 * One planet per campaign week, those before the one-based `weekIndex` evacuated.
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
