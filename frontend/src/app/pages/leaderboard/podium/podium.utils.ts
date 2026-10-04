import { BoardRow } from '../leaderboard.model';
import { PodiumPlace } from './podium.model';

/**
 * Groups ranked rows on the three plinths; ties leave the skipped place empty (1, 1, 3).
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
