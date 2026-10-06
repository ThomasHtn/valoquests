import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { Language } from '@core/i18n/translation.model';
import { resolveResultTone } from '@core/matches/display/match-visual.utils';
import { MatchDetail } from '@core/matches/match.model';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { formatKda, formatScore } from '@core/players/player-format.utils';
import { resolveKdVisual } from '@core/players/stats/player-stats.utils';

import { MatchFigure, MatchShotCount, MatchTeammateRow } from './match-detail.model';

/**
 * Stat grid of a match: the history row's figures plus raw damage and rounds.
 */
export function buildMatchFigures(match: MatchDetail, language: Language): readonly MatchFigure[] {
  const columns = 'playerProfile.matches.columns';
  const hints = 'playerProfile.matches.columnHints';
  return [
    {
      labelKey: `${columns}.kda`,
      hintKey: `${hints}.kda`,
      value: `${match.kills}/${match.deaths}/${match.assists}`,
      tone: null,
    },
    {
      labelKey: `${columns}.kd`,
      hintKey: `${hints}.kd`,
      value: formatKda(match.kd, language),
      tone: resolveKdVisual(match.kd).tone,
    },
    {
      labelKey: `${columns}.acs`,
      hintKey: `${hints}.acs`,
      value: formatScore(match.acs),
      tone: null,
    },
    {
      labelKey: `${columns}.adr`,
      hintKey: `${hints}.adr`,
      value: formatScore(match.adr),
      tone: null,
    },
    {
      labelKey: 'playerProfile.matches.detail.damageDealt',
      hintKey: null,
      value: formatFigure(match.damageDealt, language),
      tone: null,
    },
    {
      labelKey: 'playerProfile.matches.detail.roundsPlayed',
      hintKey: null,
      value: String(match.roundsPlayed),
      tone: null,
    },
  ];
}

/**
 * Hits per body zone, head first.
 */
export function buildMatchShotCounts(match: MatchDetail): readonly MatchShotCount[] {
  return [
    { labelKey: 'playerProfile.matches.detail.headshots', count: match.headshots },
    { labelKey: 'playerProfile.matches.detail.bodyshots', count: match.bodyshots },
    { labelKey: 'playerProfile.matches.detail.legshots', count: match.legshots },
  ];
}

/**
 * Tracked teammates of the lobby with their champion flag and weekly title.
 */
export function buildMatchTeammateRows(
  match: MatchDetail,
  championPlayerId: number | null,
  titlesByPlayer: ReadonlyMap<number, WeeklyTitle>,
): readonly MatchTeammateRow[] {
  return match.teammates.map((teammate) => {
    const isChampion = teammate.playerId === championPlayerId;
    return {
      playerId: teammate.playerId,
      displayName: teammate.displayName,
      avatarUrl: resolvePlayerAvatarUrl(teammate.portrait),
      isChampion,
      title: isChampion ? null : (titlesByPlayer.get(teammate.playerId) ?? null),
      agentName: teammate.agentName,
      sideKey: `playerProfile.matches.detail.${teammate.sameTeam ? 'sameTeam' : 'otherTeam'}`,
      kda: `${teammate.kills}/${teammate.deaths}/${teammate.assists}`,
      resultTone: resolveResultTone(teammate.result),
    };
  });
}
