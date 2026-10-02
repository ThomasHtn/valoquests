import { ParamMap } from '@angular/router';

import { PLAYER_SORT_PARAMS } from './players.constants';
import { PLAYER_SORT_COLUMNS, PlayerSortKey, PlayerSortOrder } from './players.model';

/**
 * The column a sort key naturally starts on: A to Z for the name, best first for a statistic.
 */
export function defaultSortDirection(key: PlayerSortKey): 1 | -1 {
  return key === 'name' ? 1 : -1;
}

/**
 * Names the order a column and direction produce, in the words the phone's toggle shows.
 *
 * @param key - The sorted column.
 * @param direction - `1` ascending, `-1` descending, as the comparator reads it.
 * @returns The order.
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
 * Reads the squad's sort from the address, falling back to rank, best first.
 *
 * @param params - The route's query parameters.
 * @returns The column and direction to sort on.
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
 * Writes the squad's sort as query parameters, leaving out the default so the address stays plain.
 *
 * @param key - The column sorted on.
 * @param direction - The direction.
 * @returns The query parameters, `null` for those to remove.
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
