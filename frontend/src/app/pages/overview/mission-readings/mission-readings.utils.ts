import { Campaign } from '@core/campaign/campaign.model';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { campaignMidnight, weekDayIndex } from '@core/campaign/calendar/campaign-calendar.utils';
import { CAMPAIGN_TIME_ZONE } from '@core/campaign/calendar/campaign-calendar.constants';
import { Language } from '@core/i18n/translation.model';
import { PlayerSummary } from '@core/players/player-summary.model';
import { CurrentRanking } from '@core/ranking/ranking.model';
import { Contribution, ContributionShare, Mission, SundayStakes } from './mission-readings.model';

/**
 * The fatal blow, `null` while the guardian stands; timed at the match, not the sync.
 */
export function fatalBlow(
  week: CampaignWeek,
  players: readonly PlayerSummary[],
  language: Language,
): Mission['defeated'] {
  if (!week.defeated || !week.defeatedAt) {
    return null;
  }
  const at = new Date(week.defeatedAt);
  return {
    // Paris time, like the forecast quoting the same instant.
    weekday: new Intl.DateTimeFormat(language, {
      weekday: 'long',
      timeZone: CAMPAIGN_TIME_ZONE,
    }).format(at),
    time: new Intl.DateTimeFormat(language, {
      hour: '2-digit',
      minute: '2-digit',
      timeZone: CAMPAIGN_TIME_ZONE,
    }).format(at),
    by: players.find((player) => player.id === week.defeatedByPlayerId)?.displayName ?? null,
  };
}

/**
 * Situation report of the week in progress, `null` outside a running week.
 */
export function buildMission(
  campaign: Campaign | null,
  week: CampaignWeek | null,
  players: readonly PlayerSummary[],
  language: Language,
): Mission | null {
  if (!campaign || !week) {
    return null;
  }
  const hitPointsLeft = Math.max(0, week.guardianHitPoints - week.damageDealt);
  return {
    weekIndex: week.weekIndex,
    planetName: week.planetName,
    category: week.category,
    dayOfWeek: weekDayIndex(week.weekStart, campaign.today) + 1,
    hitPointsLeft,
    hitPoints: week.guardianHitPoints,
    breachPercent: week.progressPercent,
    guardianLeft: week.guardianHitPoints > 0 ? hitPointsLeft / week.guardianHitPoints : 0,
    defeated: fatalBlow(week, players, language),
    wounded: week.woundedCount,
    crew: campaign.rosterSize ?? 0,
    extractionDeadline: campaignMidnight(week.weekStart, 7).getTime(),
  };
}

/**
 * Sunday's two outcomes as gaps from the forecast, `null` without a forecast or once defeated.
 * Mirrors the backend's `ExtractionEstimate` and the replay's strike.
 */
export function buildSundayStakes(
  campaign: Campaign | null,
  week: CampaignWeek | null,
): SundayStakes | null {
  const base = campaign?.base;
  const forecast = campaign?.forecast;
  if (!base || !forecast || !week || week.defeated) {
    return null;
  }
  const remainingGroup = forecast.woundedCount - forecast.challengeRescued;
  const reachable = Math.min(remainingGroup, base.rescuesByComponents, base.rescuesByFood);
  const standing = 1 - week.progressPercent / 100;
  return {
    gain: Math.max(0, reachable - forecast.extractionRescued),
    loss: Math.round(base.population * standing * standing * (base.guardianLossPercent / 100)),
  };
}

/**
 * Squad contribution to the week, `null` without a ranking or a guardian.
 */
export function buildContribution(
  ranking: CurrentRanking | null,
  week: CampaignWeek | null,
): Contribution | null {
  if (!ranking || !week || week.guardianHitPoints <= 0) {
    return null;
  }
  if (ranking.ranking.length === 0) {
    return null;
  }
  // An all-zero Monday still renders: an empty bar is the starting position.
  const total = ranking.ranking.reduce((sum, entry) => sum + entry.totalPoints, 0);
  const shares: ContributionShare[] = ranking.ranking
    .filter((entry) => entry.totalPoints > 0)
    .map((entry) => ({
      playerId: entry.player.id,
      name: entry.player.displayName,
      damage: entry.guardianDamage,
      challengePoints: entry.challengePoints,
      total: entry.totalPoints,
      sharePercent: total > 0 ? Math.round((entry.totalPoints / total) * 100) : 0,
    }))
    .sort((left, right) => right.total - left.total);
  return { total, hitPoints: week.guardianHitPoints, shares };
}
