import { ParamMap } from '@angular/router';

import { PLAYER_SORT_PARAMS, PLAYER_SORT_COLUMNS } from './players.constants';
import { PlayerSortKey, PlayerSortOrder } from './players.model';

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
