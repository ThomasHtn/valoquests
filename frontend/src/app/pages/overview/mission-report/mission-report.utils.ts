import { WEEKLY_TITLES } from '@core/campaign/campaign.constants';
import { Campaign } from '@core/campaign/campaign.model';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { resolveTitleVisual } from '@core/campaign/titles/campaign-title-visual.utils';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { Language, TranslateFn } from '@core/i18n/translation.model';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { RankingHistoryWeek } from '@core/ranking/ranking.model';

import { fatalBlow } from '../mission-readings/mission-readings.utils';
import { SEEN_REPORT_KEY } from './mission-report.constants';
import {
  MissionReport,
  MissionReportBlow,
  MissionReportChampion,
  MissionReportGain,
} from './mission-report.model';

/**
 * Fatal blow in three parts: when, who, and where (map, mode, score) when known.
 */
function blowOf(
  week: CampaignWeek,
  players: readonly PlayerSummary[],
  language: Language,
  translate: TranslateFn,
): MissionReportBlow | null {
  const blow = fatalBlow(week, players, language);
  if (!blow) {
    return null;
  }
  const detail = week.fatalBlow;
  let where = '';
  if (detail?.mapName && detail.gameMode) {
    where += translate('overview.missionReport.blowWhere', {
      map: detail.mapName,
      mode: translate(`common.gameMode.${detail.gameMode}`),
    });
  }
  if (detail && detail.allyScore !== null && detail.enemyScore !== null) {
    where += translate('overview.missionReport.blowScore', {
      ally: detail.allyScore,
      enemy: detail.enemyScore,
    });
  }
  return {
    when: translate('overview.missionReport.blow', { weekday: blow.weekday, time: blow.time }),
    by: blow.by,
    where,
  };
}

/**
 * Monday report of a settled week (last one if `weekIndex` is `null`), `null` if unsettled.
 */
export function buildMissionReport(
  campaign: Campaign | null,
  players: readonly PlayerSummary[],
  history: readonly RankingHistoryWeek[],
  language: Language,
  translate: TranslateFn,
  weekIndex: number | null = null,
): MissionReport | null {
  const settled = campaign ? settledWeekOf(campaign, weekIndex) : null;
  if (!campaign || !settled) {
    return null;
  }
  const portraitOf = (id: number): string | null =>
    resolvePlayerAvatarUrl(players.find((player) => player.id === id)?.portrait ?? null);
  const frozen = history.find((week) => week.weekStart === settled.weekStart) ?? null;
  const rescued = settled.challengeRescued + settled.extractionRescued;
  return {
    weekStart: settled.weekStart,
    weekIndex: settled.weekIndex,
    planetName: settled.planetName,
    defeated: settled.defeated,
    breachPercent: settled.progressPercent,
    blow: blowOf(settled, players, language, translate),
    baseLoss: settled.baseLoss,
    rescued,
    spotted: settled.woundedCount,
    rescuedShare: settled.woundedCount > 0 ? Math.min(1, rescued / settled.woundedCount) : 0,
    byChallenges: settled.challengeRescued,
    byShip: settled.extractionRescued,
    leftBehind: Math.max(0, settled.woundedCount - rescued),
    limiter: settled.limiter,
    base: baseOf(settled),
    titles: frozen ? titlesOf(frozen, portraitOf) : null,
    champion: frozen ? championOf(frozen, portraitOf) : null,
    next: nextTargetOf(campaign, settled),
  };
}

/**
 * Settled week to report, the last one when `weekIndex` is `null`.
 */
function settledWeekOf(campaign: Campaign, weekIndex: number | null): CampaignWeek | null {
  const settledWeeks = campaign.weeks.filter((week) => week.settled);
  if (weekIndex === null) {
    return settledWeeks.at(-1) ?? null;
  }
  return settledWeeks.find((week) => week.weekIndex === weekIndex) ?? null;
}

/**
 * Base gains and losses over the week, `null` without a base reading.
 */
function baseOf(settled: CampaignWeek): MissionReport['base'] {
  if (!settled.base) {
    return null;
  }
  return {
    foodGained: settled.base.foodGained,
    componentsGained: settled.base.componentsGained,
    population: settled.base.population,
    populationChange: settled.base.populationChange,
  };
}

/**
 * Each weekly title and its holder when the week froze.
 */
function titlesOf(
  frozen: RankingHistoryWeek,
  portraitOf: (id: number) => string | null,
): MissionReport['titles'] {
  return WEEKLY_TITLES.map((key) => {
    const holder = frozen.ranking.find((entry) => entry.titles.includes(key)) ?? null;
    return {
      key,
      ...resolveTitleVisual(key),
      holder: holder?.displayName ?? null,
      portrait: holder ? portraitOf(holder.playerId) : null,
    };
  });
}

/**
 * Next week, `null` once played: an older report must not name a target already faced.
 */
function nextTargetOf(campaign: Campaign, settled: CampaignWeek): MissionReport['next'] {
  // One-based indexes: the settled week's index points at the following week.
  const next = campaign.weeks[settled.weekIndex] ?? null;
  if (!next || next.settled) {
    return null;
  }
  return {
    planetName: next.planetName,
    hitPoints: next.guardianHitPoints,
    wounded: next.woundedCount,
  };
}

/**
 * Frozen week's sole first place, holder `null` when nobody won outright.
 */
function championOf(
  frozen: RankingHistoryWeek,
  portraitOf: (id: number) => string | null,
): MissionReportChampion {
  const winner = frozen.ranking.find((entry) => entry.playerId === frozen.winnerPlayerId) ?? null;
  return {
    holder: winner?.displayName ?? null,
    portrait: winner ? portraitOf(winner.playerId) : null,
    points: winner?.totalPoints ?? 0,
  };
}

/**
 * Monday of the last dismissed report, `null` if none or storage fails.
 */
export function readSeenReport(): string | null {
  try {
    return localStorage.getItem(SEEN_REPORT_KEY);
  } catch {
    return null;
  }
}

/**
 * Stores the report as dismissed; storage failures are ignored.
 */
export function writeSeenReport(weekStart: string): void {
  try {
    localStorage.setItem(SEEN_REPORT_KEY, weekStart);
  } catch {
    // The report opens again next time.
  }
}

/**
 * Loot lines of the week (food, components, population), none without a base reading.
 */
export function buildReportGains(
  base: MissionReport['base'],
  translate: TranslateFn,
  format: (amount: number) => string,
): readonly MissionReportGain[] {
  if (!base) {
    return [];
  }
  return [
    {
      tone: 'food',
      icon: CONCEPT_ICONS.food,
      value: base.foodGained,
      sign: '+',
      label: translate('overview.missionReport.food'),
    },
    {
      tone: 'components',
      icon: CONCEPT_ICONS.components,
      value: base.componentsGained,
      sign: '+',
      label: translate('overview.missionReport.components'),
    },
    {
      tone: base.populationChange < 0 ? 'decline' : 'growth',
      icon: CONCEPT_ICONS.base,
      value: base.populationChange,
      sign: base.populationChange > 0 ? '+' : '',
      label: translate('overview.missionReport.population', { total: format(base.population) }),
    },
  ];
}
