import { toLocalDayKey } from '@core/date/date-time.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { resolveLatestSynchronization } from '@core/players/player-summary.utils';

/**
 * How long after local midnight the day is considered to have turned.
 *
 * The backend closes a day at 00:10 (challenge drawn, meal written down by the nightly tick), so a
 * reload fired at 00:00 sharp would still read yesterday's state. Fifteen minutes leaves the tick
 * room to finish.
 */
export const DAY_TURN_GRACE_MS = 15 * 60_000;

/**
 * Summarizes everything that makes the backend's public data change, as one comparable string.
 *
 * Two things move the data: a synchronization, which every player's last successful
 * synchronization instant reveals, and the day turning, which the nightly tick acts on with no
 * synchronization involved. A stamp that differs from the previous one means the screens are stale.
 *
 * @param players - The roster as last fetched.
 * @param now - The current instant.
 * @returns The stamp.
 */
export function liveRefreshStamp(players: readonly PlayerSummary[], now: Date): string {
  const day = toLocalDayKey(new Date(now.getTime() - DAY_TURN_GRACE_MS).toISOString());
  return `${resolveLatestSynchronization(players) ?? ''}|${day}`;
}
