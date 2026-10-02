import { ParamMap } from '@angular/router';

import { FILTERABLE_GAME_MODES } from '@core/matches/game-mode.model';
import { Season } from '@core/matches/season.model';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import {
  ALL_SEASONS_PARAM,
  PROFILE_QUERY_KEYS,
  PROGRESS_VIEW_PARAM,
} from './player-profile.constants';
import { ProfileQuery, SeasonParam } from './player-profile.model';

/**
 * Pure helpers of the player-profile page.
 */

/**
 * Resolves the id of the current season - the one flagged `active` - or the first (most-recent)
 * known season if none is active, or `null` if none are known yet.
 */
export function resolveCurrentSeasonId(seasons: readonly Season[]): number | null {
  return (seasons.find((season) => season.active) ?? seasons[0])?.id ?? null;
}

/**
 * Colour of the share the next match keeps: amber at full value, muted once the ladder has
 * started taking a cut. A red would overstate a rule that still pays half.
 */
export function resolveYieldToneClass(percent: number): string {
  return percent >= 100 ? 'text-brand-500' : 'text-text-secondary';
}

/**
 * Builds the plate shown when the route names a player or match the backend does not know.
 *
 * @param translate - Resolves a translation key.
 * @param keyPrefix - i18n prefix holding the plate's `title` and `text`.
 * @param address - The URL that led here, echoed like the generic not-found page does.
 * @returns The plate, already translated.
 */
export function buildNotFoundPlate(
  translate: (key: string) => string,
  keyPrefix: string,
  address: string,
): EmptyPlate {
  return {
    illustration: 'radar',
    eyebrow: translate('notFound.eyebrow'),
    title: translate(`${keyPrefix}.title`),
    text: translate(`${keyPrefix}.text`),
    readouts: [{ tone: 'info', label: translate('notFound.address'), value: address }],
  };
}

/**
 * Reads the profile's state from the address, ignoring anything it does not recognise.
 *
 * @param params - The route's query parameters.
 * @returns The view, mode and season the address asks for.
 */
export function readProfileQuery(params: ParamMap): ProfileQuery {
  const mode = params.get(PROFILE_QUERY_KEYS.mode);
  const season = params.get(PROFILE_QUERY_KEYS.season);
  const seasonId = Number(season);
  return {
    view: params.get(PROFILE_QUERY_KEYS.view) === PROGRESS_VIEW_PARAM ? 'PROGRESS' : 'MATCHES',
    mode: FILTERABLE_GAME_MODES.find((candidate) => candidate === mode) ?? null,
    season:
      season === ALL_SEASONS_PARAM
        ? 'ALL'
        : season !== null && Number.isInteger(seasonId) && seasonId > 0
          ? seasonId
          : null,
  };
}

/**
 * Writes the profile's state as query parameters, leaving out every default so a plain profile
 * keeps a plain address.
 *
 * @param query - The view, mode and season on screen.
 * @param currentSeasonId - The season the history opens on by default.
 * @returns The query parameters, `null` for those to remove.
 */
export function writeProfileQuery(
  query: ProfileQuery,
  currentSeasonId: number | null,
): Record<string, string | null> {
  let season: string | null = null;
  if (query.season === 'ALL') {
    season = ALL_SEASONS_PARAM;
  } else if (query.season !== null && query.season !== currentSeasonId) {
    season = String(query.season);
  }
  return {
    [PROFILE_QUERY_KEYS.view]: query.view === 'PROGRESS' ? PROGRESS_VIEW_PARAM : null,
    [PROFILE_QUERY_KEYS.mode]: query.mode,
    [PROFILE_QUERY_KEYS.season]: season,
  };
}

/**
 * The season the history opens on, from the address when it names one that exists.
 *
 * @param seasons - Every known season.
 * @param requested - The season scope read from the address.
 * @returns The season id, or `null` for every season.
 */
export function resolveRequestedSeasonId(
  seasons: readonly Season[],
  requested: SeasonParam,
): number | null {
  if (requested === 'ALL') {
    return null;
  }
  if (requested !== null && seasons.some((season) => season.id === requested)) {
    return requested;
  }
  return resolveCurrentSeasonId(seasons);
}
