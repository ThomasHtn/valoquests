import { SquadMatch } from '@core/matches/match-squad.model';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { HistoryMatch } from '@core/matches/day/match-day.model';

/**
 * History row of a squad entry, carrying who played it.
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
