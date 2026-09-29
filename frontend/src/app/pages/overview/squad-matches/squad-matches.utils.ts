import { SquadMatch } from '@core/matches/squad-match.model';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { HistoryMatch } from '../../player-profile/match-day.model';

/**
 * Turns a squad history entry into a history row named after its player.
 *
 * @param entry - The entry, as the API returns it.
 * @returns The player's match, carrying who played it.
 */
export function toHistoryMatch(entry: SquadMatch): HistoryMatch {
  return {
    ...entry.match,
    player: {
      id: entry.playerId,
      name: entry.displayName,
      portrait: resolvePlayerAvatarUrl(entry.portrait),
    },
  };
}
