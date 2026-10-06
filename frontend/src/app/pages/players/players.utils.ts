import { ParamMap } from '@angular/router';

import { resolveTierOrdinal } from '@core/players/competitive-tier/player-competitive-tier.utils';

import { PLAYER_SORT_COLUMNS, PLAYER_SORT_PARAMS } from './players.constants';
import { PlayerRow, PlayerSortKey, PlayerSortOrder } from './players.model';

/**
 * Natural direction of a column: A to Z for the name, best first otherwise.
 */
export function defaultSortDirection(key: PlayerSortKey): 1 | -1 {
  return key === 'name' ? 1 : -1;
}

/**
 * Order produced by a column and direction (`1` ascending), as the phone's toggle words it.
 */
export function toPlayerSortOrder(key: PlayerSortKey, direction: 1 | -1): PlayerSortOrder {
  const natural = direction === defaultSortDirection(key);
  if (key === 'name') {
    return natural ? 'az' : 'za';
  }
  if (key === 'rank') {
    return natural ? 'best' : 'worst';
  }
  return natural ? 'high' : 'low';
}

/**
 * Reads the sort from the query parameters, defaulting to rank, best first.
 */
export function readPlayerSort(params: ParamMap): { key: PlayerSortKey; direction: 1 | -1 } {
  const key =
    PLAYER_SORT_COLUMNS.find((column) => column.key === params.get(PLAYER_SORT_PARAMS.key))?.key ??
    'rank';
  const direction = params.get(PLAYER_SORT_PARAMS.direction);
  return {
    key,
    direction: direction === 'asc' ? 1 : direction === 'desc' ? -1 : defaultSortDirection(key),
  };
}

/**
 * Sort as query parameters; the default maps to `null` so the address stays plain.
 */
export function writePlayerSort(
  key: PlayerSortKey,
  direction: 1 | -1,
): Record<string, string | null> {
  const isDefault = key === 'rank' && direction === -1;
  return {
    [PLAYER_SORT_PARAMS.key]: isDefault ? null : key,
    [PLAYER_SORT_PARAMS.direction]: isDefault ? null : direction === 1 ? 'asc' : 'desc',
  };
}

/**
 * Rows sorted on a column (`1` ascending); `null` stats always sort last, missing is not worst.
 */
export function sortPlayerRows(
  rows: readonly PlayerRow[],
  key: PlayerSortKey,
  direction: 1 | -1,
): readonly PlayerRow[] {
  return [...rows].sort((a, b) => {
    if (key === 'name') {
      return direction * a.displayName.localeCompare(b.displayName);
    }

    if (key === 'rank') {
      // Tier first, rating within the tier; an unrated player sits below every rated one.
      const comparison =
        resolveTierOrdinal(a.competitiveTier) - resolveTierOrdinal(b.competitiveTier) ||
        (a.rankRating ?? -1) - (b.rankRating ?? -1);
      return direction * comparison;
    }

    const valueA = a[key];
    const valueB = b[key];
    if (valueA === null || valueB === null) {
      return valueA === valueB ? 0 : valueA === null ? 1 : -1;
    }
    return direction * (valueA - valueB);
  });
}
