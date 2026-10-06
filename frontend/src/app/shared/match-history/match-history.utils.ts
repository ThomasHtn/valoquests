import { formatCampaignTime } from '@core/date/date-format.utils';
import { formatFigure, formatPercent } from '@core/i18n/format/number-format.utils';
import { Language, TranslateFn } from '@core/i18n/translation.model';
import { HistoryMatch, MatchDay } from '@core/matches/day/match-day.model';
import { resolveDamageHintKey, resolveMatchScore } from '@core/matches/display/match-format.utils';
import { MatchResult } from '@core/matches/match-result.model';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import { KD_GOOD_THRESHOLD } from '@core/players/stats/player-stats.constants';
import {
  MatchDamageCell,
  MatchHistoryDay,
  MatchHistoryRow,
  MatchResultTone,
  MatchStatTone,
} from './match-history.model';

/**
 * Formats the fetched days for the template, in the active language.
 * @param playerId owner of a single player's history, used when a match carries no player
 */
export function buildMatchHistoryDays(
  days: readonly MatchDay<HistoryMatch>[],
  playerId: number | null,
  language: Language,
  translate: TranslateFn,
): MatchHistoryDay[] {
  return days.map((day) => ({
    dayKey: day.dayKey,
    dateLabel: day.dateLabel,
    wins: day.wins,
    losses: day.losses,
    stats: [
      {
        column: 'kda',
        value: `${day.totalKills}/${day.totalDeaths}/${day.totalAssists}`,
        tone: 'primary',
      },
      { column: 'kd', value: formatKda(day.avgKd, language), tone: reportedTone(day.avgKd) },
      {
        column: 'headshotPercentage',
        value: formatHeadshotPercentage(day.avgHeadshotPercentage, language),
        tone: reportedTone(day.avgHeadshotPercentage),
      },
      { column: 'adr', value: formatScore(day.avgAdr), tone: reportedTone(day.avgAdr) },
      { column: 'acs', value: formatScore(day.avgAcs), tone: reportedTone(day.avgAcs) },
    ],
    damage: formatFigure(day.totalValoquestsDamage, language),
    rows: day.matches.map((match) => buildRow(match, playerId, language, translate)),
  }));
}

/**
 * One match formatted for its row.
 */
function buildRow(
  match: HistoryMatch,
  playerId: number | null,
  language: Language,
  translate: TranslateFn,
): MatchHistoryRow {
  return {
    match,
    link: ['/players', match.player?.id ?? playerId ?? '', 'matches', match.id],
    time: formatCampaignTime(match.startedAt),
    resultTone: resolveResultTone(match.result),
    score: resolveMatchScore(match.allyScore, match.enemyScore),
    stats: [
      { column: 'kda', value: `${match.kills}/${match.deaths}/${match.assists}`, tone: 'primary' },
      { column: 'kd', value: formatKda(match.kd, language), tone: resolveKdTone(match.kd) },
      {
        column: 'headshotPercentage',
        value: formatHeadshotPercentage(match.headshotPercentage, language),
        tone: reportedTone(match.headshotPercentage),
      },
      { column: 'adr', value: formatScore(match.adr), tone: reportedTone(match.adr) },
      { column: 'acs', value: formatScore(match.acs), tone: reportedTone(match.acs) },
    ],
    damage: buildDamageCell(match, language, translate),
  };
}

/**
 * Damage cell of a match: amount, explanation, and the kept share when the ladder reduced it.
 */
function buildDamageCell(
  match: HistoryMatch,
  language: Language,
  translate: TranslateFn,
): MatchDamageCell {
  const percent = match.damageCoefficientPercent;
  const reduced = percent > 0 && percent < 100;
  return {
    amount: formatFigure(match.valoquestsDamage, language),
    isZero: match.valoquestsDamage === 0,
    explanation: translate(resolveDamageHintKey(percent), { percent }),
    reducedShare: reduced ? formatPercent(percent, language) : null,
    reducedShareLabel: reduced
      ? translate('playerProfile.matches.damage.share', { percent })
      : null,
  };
}

/**
 * Primary when the figure was reported, muted for the dash some modes leave.
 */
export function reportedTone(value: number | null): MatchStatTone {
  return Number.isFinite(value) ? 'primary' : 'muted';
}

/**
 * K/D against the easiest K/D challenge's bar, muted when missing.
 */
export function resolveKdTone(kd: number | null): MatchStatTone {
  if (!Number.isFinite(kd)) {
    return 'muted';
  }
  return (kd as number) >= KD_GOOD_THRESHOLD ? 'good' : 'average';
}

/**
 * Win or loss; draws and unknown results stay neutral.
 */
export function resolveResultTone(result: MatchResult): MatchResultTone {
  if (result === 'WIN') {
    return 'win';
  }
  return result === 'LOSS' ? 'loss' : 'neutral';
}
