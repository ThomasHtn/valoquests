import { ParamMap } from '@angular/router';

import { Language, TranslateFn } from '@core/i18n/translation.model';
import { FILTERABLE_GAME_MODES } from '@core/matches/game-mode/match-game-mode.constants';
import { PlayerStatistics } from '@core/players/player-details.model';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
  formatWinRate,
} from '@core/players/player-format.utils';
import { resolveKdaVisual, resolveWinRateVisual } from '@core/players/stats/player-stats.utils';
import { Season } from '@core/seasons/season.model';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import {
  ALL_SEASONS_PARAM,
  PROFILE_QUERY_KEYS,
  PROGRESS_VIEW_PARAM,
} from './player-profile.constants';
import { ProfileQuery, SeasonParam, StatStrip } from './player-profile.model';

/**
 * Id of the active season, else the most recent one, `null` when none is known.
 */
export function resolveCurrentSeasonId(seasons: readonly Season[]): number | null {
  return (seasons.find((season) => season.active) ?? seasons[0])?.id ?? null;
}

/**
 * Translated not-found plate for an unknown player or match, echoing the `address`.
 */
export function buildNotFoundPlate(
  translate: TranslateFn,
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
 * Reads the profile state from the query parameters, ignoring unknown values.
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
 * Profile state as query parameters; defaults map to `null` to keep the address plain.
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
 * Season the history opens on: the address's when it exists, `null` for every season.
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

/**
 * Stat strip of the filtered matches; without a match every figure is judged `null` so it greys out.
 */
export function buildStatStrip(statistics: PlayerStatistics, language: Language): StatStrip {
  const hasSample = statistics.matchesPlayed > 0;
  const sampled = (value: number): number | null => (hasSample ? value : null);
  const winRate = sampled(statistics.winRate);
  const kda = sampled(statistics.kda);
  return {
    winRate: statistics.winRate,
    winRateLabel: formatWinRate(winRate, language),
    winRateVisual: resolveWinRateVisual(winRate),
    wins: statistics.wins,
    losses: statistics.losses,
    kdaLabel: formatKda(kda, language),
    kdaTone: resolveKdaVisual(kda).tone,
    headshotPercentageLabel: formatHeadshotPercentage(
      sampled(statistics.headshotPercentage),
      language,
    ),
    adrLabel: formatScore(sampled(statistics.adr)),
    acsLabel: formatScore(sampled(statistics.acs)),
    matchesPlayed: statistics.matchesPlayed,
  };
}
