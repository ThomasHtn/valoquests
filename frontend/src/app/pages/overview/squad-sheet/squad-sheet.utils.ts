import { primaryTitleOf } from '@core/campaign/titles/campaign-title.utils';
import { resolveTitleVisual } from '@core/campaign/titles/campaign-title-visual.utils';
import { CampaignToday } from '@core/campaign/campaign-today.model';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { DailyRanking } from '@core/ranking/ranking.model';
import { streakBonusOf, streakWeekOf } from '@shared/streak-gauge/streak-gauge.utils';
import { SquadRow } from './squad-sheet.model';

/**
 * One row per active operator, empty while `daily` is unresolved.
 */
export function buildSquad(
  daily: DailyRanking | null,
  today: CampaignToday | null,
  championId: number | null = null,
): readonly SquadRow[] {
  if (!daily) {
    return [];
  }
  const titles = today?.titles ?? {};
  // An inactive operator never deals damage, so has no line.
  const active = daily.ranking.filter((entry) => entry.competing);
  return active.map((entry) => {
    const title = primaryTitleOf(titles, entry.playerId);
    const played = entry.matchCount > 0;
    const daysPlayed = entry.weekPlayedDays.length;
    return {
      position: played ? entry.position : null,
      playerId: entry.playerId,
      name: entry.displayName,
      portrait: resolvePlayerAvatarUrl(entry.portrait),
      champion: entry.playerId === championId,
      title: title === null ? null : { key: title, ...resolveTitleVisual(title) },
      played,
      // An idle operator shows the bonus playing today would earn, not yesterday's.
      streakBonusPercent: played ? entry.streakBonusPercent : streakBonusOf(daysPlayed + 1),
      streakDays: daysPlayed,
      streakWeek: streakWeekOf(daily.day, entry.weekPlayedDays),
      damage: entry.damage,
      matchCount: entry.matchCount,
      reducedMatchCount: entry.reducedMatchCount,
      components: entry.components,
      food: entry.food,
    };
  });
}
