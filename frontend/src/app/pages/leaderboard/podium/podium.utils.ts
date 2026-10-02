import { BoardRow } from '../leaderboard.model';
import { PodiumPlace } from './podium.model';

/**
 * Groups the ranked rows on the three plinths. Ties share a plinth and leave the places they skip
 * empty (1, 1, 3 has no 2nd), as the ranking itself does.
 *
 * @param rows - The ranked rows, in order.
 * @returns The occupied places, 1st first.
 */
export function groupPodium(rows: readonly BoardRow[]): PodiumPlace[] {
  const places: PodiumPlace[] = [];
  for (const position of [1, 2, 3]) {
    const holders = rows.filter((row) => row.position === position);
    if (holders.length > 0) {
      places.push({ position, rows: holders });
    }
  }
  return places;
}
