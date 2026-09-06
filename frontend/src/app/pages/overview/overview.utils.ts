import {
  Campaign,
  CAMPAIGN_WEEK_COUNT,
  CampaignToday,
  CampaignWeek,
  WEEKLY_TITLES,
  WeeklyTitle,
} from '@core/campaign/campaign.model';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { CurrentChallenges } from '@core/challenges/challenge.model';
import { daysBetween, localMidnight } from '@core/date/date-time.utils';
import { Language } from '@core/i18n/translation.model';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { CurrentRanking, DailyRanking, RankingHistoryWeek } from '@core/ranking/ranking.model';
import {
  Capacity,
  DailyOrder,
  DayTally,
  FriezeWeek,
  Mission,
  MissionReport,
  SquadRow,
} from './overview.model';

/**
 * Translates a key, the same shape as `Translation.translate`, kept as a structural type here so
 * this module stays free of any dependency on the i18n service.
 */
export type Translate = (key: string, params?: Readonly<Record<string, string | number>>) => string;

/**
 * Builds the ten-week frieze: each week's outcome, and how far the guardian was pushed.
 *
 * @param campaign - The campaign, or `null` outside one.
 * @param translate - Translation function for each week's title tooltip.
 * @returns One entry per week, or an empty frieze outside a campaign.
 */
export function buildFrieze(
  campaign: Campaign | null,
  translate: Translate,
): readonly FriezeWeek[] {
  if (!campaign || campaign.weeks.length === 0) {
    return [];
  }
  return campaign.weeks.map((week) => toFriezeWeek(week, campaign, translate));
}

function toFriezeWeek(week: CampaignWeek, campaign: Campaign, translate: Translate): FriezeWeek {
  const isCurrent = week.weekIndex === campaign.currentWeekIndex && campaign.status === 'RUNNING';
  const label = String(week.weekIndex).padStart(2, '0');
  if (week.defeated) {
    return {
      index: week.weekIndex,
      label,
      state: 'won',
      advance: 1,
      mark: '✓',
      title: translate('overview.frieze.won'),
    };
  }
  if (week.settled) {
    return {
      index: week.weekIndex,
      label,
      state: 'lost',
      advance: week.progressPercent / 100,
      mark: '✕',
      title: translate('overview.frieze.lost', { percent: week.progressPercent }),
    };
  }
  if (isCurrent) {
    return {
      index: week.weekIndex,
      label,
      state: 'now',
      advance: week.progressPercent / 100,
      mark: '●',
      title: translate('overview.frieze.now', { percent: week.progressPercent }),
    };
  }
  // A closed campaign's remaining weeks were never played: they are not coming any more.
  const unplayed = campaign.status === 'CLOSED';
  return {
    index: week.weekIndex,
    label,
    state: 'ahead',
    advance: 0,
    mark: week.weekIndex === CAMPAIGN_WEEK_COUNT && !unplayed ? '★' : '·',
    title: translate(unplayed ? 'overview.frieze.unplayed' : 'overview.frieze.ahead'),
  };
}

/**
 * The fatal blow as the report states it, or `null` while the guardian stands. The blow belongs to
 * the match, so its time is the match's, never the synchronization's.
 */
function fatalBlow(
  week: CampaignWeek,
  players: readonly PlayerSummary[],
  language: Language,
): Mission['defeated'] {
  if (!week.defeated || !week.defeatedAt) {
    return null;
  }
  const at = new Date(week.defeatedAt);
  return {
    weekday: new Intl.DateTimeFormat(language, { weekday: 'long' }).format(at),
    time: new Intl.DateTimeFormat(language, { hour: '2-digit', minute: '2-digit' }).format(at),
    by: players.find((player) => player.id === week.defeatedByPlayerId)?.displayName ?? null,
  };
}

/**
 * The fatal blow in one line: who, when, on which map, in which mode and on what score.
 */
function blowLine(
  week: CampaignWeek,
  players: readonly PlayerSummary[],
  language: Language,
  translate: Translate,
): string | null {
  const blow = fatalBlow(week, players, language);
  if (!blow) {
    return null;
  }
  const detail = week.fatalBlow;
  const score =
    detail?.allyScore !== null && detail?.allyScore !== undefined && detail.enemyScore !== null
      ? `${detail.allyScore} – ${detail.enemyScore}`
      : '';
  return translate('overview.missionReport.blow', {
    name: blow.by ?? '',
    weekday: blow.weekday,
    time: blow.time,
    map: detail?.mapName ?? '',
    mode: detail?.gameMode ? translate(`common.gameMode.${detail.gameMode}`) : '',
    score,
  });
}

/**
 * Builds the week-in-progress situation report.
 *
 * @param campaign - The campaign, or `null` outside one.
 * @param week - The week in progress, or `null` outside one.
 * @param players - Tracked players, used to name who dealt the fatal blow.
 * @param language - The reader's language, for the fatal blow's weekday and time.
 * @returns The mission, or `null` outside a running week.
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
    dayOfWeek: Math.min(7, Math.max(1, daysBetween(week.weekStart, campaign.today) + 1)),
    guardianName: week.guardianName ?? '',
    hitPointsLeft,
    hitPoints: week.guardianHitPoints,
    breachPercent: week.progressPercent,
    guardianLeft: week.guardianHitPoints > 0 ? hitPointsLeft / week.guardianHitPoints : 0,
    defeated: fatalBlow(week, players, language),
    wounded: week.woundedCount,
    crew: campaign.rosterSize ?? 0,
    extractionDeadline: localMidnight(week.weekStart, 7).getTime(),
  };
}

/**
 * Builds the last settled week's Monday report.
 *
 * @param campaign - The campaign, or `null` outside one.
 * @param players - Tracked players, used to resolve portraits and the fatal blow's name.
 * @param history - Frozen weeks, for the settled week's titles and ranking.
 * @param language - The reader's language, for dates and the fatal blow.
 * @param translate - Translation function for the fatal blow's sentence.
 * @returns The report, or `null` before the first settled week.
 */
export function buildMissionReport(
  campaign: Campaign | null,
  players: readonly PlayerSummary[],
  history: readonly RankingHistoryWeek[],
  language: Language,
  translate: Translate,
): MissionReport | null {
  const settled = campaign?.weeks.filter((week) => week.settled).at(-1);
  if (!campaign || !settled) {
    return null;
  }
  const portraitOf = (id: number): string | null =>
    resolvePlayerAvatarUrl(players.find((player) => player.id === id)?.portrait ?? null);
  const frozen = history.find((week) => week.weekStart === settled.weekStart) ?? null;
  const next = campaign.weeks[settled.weekIndex] ?? null;
  return {
    weekStart: settled.weekStart,
    weekIndex: settled.weekIndex,
    planetName: settled.planetName,
    settledOn: new Intl.DateTimeFormat(language, { day: 'numeric', month: 'short' }).format(
      localMidnight(settled.weekStart, 6),
    ),
    guardianName: settled.guardianName ?? '',
    defeated: settled.defeated,
    hitPoints: settled.guardianHitPoints,
    hitPointsLeft: Math.max(0, settled.guardianHitPoints - settled.damageDealt),
    breachPercent: settled.progressPercent,
    blow: blowLine(settled, players, language, translate),
    baseLoss: settled.baseLoss,
    rescued: settled.challengeRescued + settled.extractionRescued,
    spotted: settled.woundedCount,
    byChallenges: settled.challengeRescued,
    limiter: settled.limiter,
    population: settled.base?.population ?? null,
    populationChange: settled.base?.populationChange ?? 0,
    titles: frozen
      ? WEEKLY_TITLES.map((key) => {
          const holder = frozen.ranking.find((entry) => entry.titles.includes(key)) ?? null;
          return {
            key,
            ...resolveTitleVisual(key),
            holder: holder?.displayName ?? null,
            portrait: holder ? portraitOf(holder.playerId) : null,
          };
        })
      : null,
    ranking: frozen
      ? frozen.ranking.map((entry) => ({
          position: entry.position,
          name: entry.displayName,
          portrait: portraitOf(entry.playerId),
          total: entry.totalPoints,
        }))
      : [],
    next: next
      ? {
          planetName: next.planetName,
          hitPoints: next.guardianHitPoints,
          wounded: next.woundedCount,
        }
      : null,
  };
}

/**
 * Builds the four extraction-capacity dials.
 *
 * @param campaign - The campaign, or `null` outside one.
 * @param week - The week in progress, or `null` outside one.
 * @returns The capacity, or `null` outside a running week with a forecast.
 */
export function buildCapacity(
  campaign: Campaign | null,
  week: CampaignWeek | null,
): Capacity | null {
  const base = campaign?.base;
  const forecast = campaign?.forecast;
  if (!campaign || !week || !base || !forecast) {
    return null;
  }
  const wounded = Math.max(1, week.woundedCount);
  const fraction = (value: number): number => Math.min(1, value / wounded);
  return {
    wounded: week.woundedCount,
    carry: {
      value: base.rescuesByComponents,
      fraction: fraction(base.rescuesByComponents),
      stock: base.componentsStock,
    },
    shelter: {
      value: base.rescuesByFood,
      fraction: fraction(base.rescuesByFood),
      stock: base.foodStock,
    },
    breach: {
      value: week.progressPercent,
      fraction: week.progressPercent / 100,
      stock: Math.max(0, week.guardianHitPoints - week.damageDealt),
    },
    aboard: forecast.rescued,
    aboardFraction: fraction(forecast.rescued),
    fromGuardian: forecast.extractionRescued,
    fromChallenges: forecast.challengeRescued,
    leftBehind: forecast.leftBehind,
    limiter: forecast.limiter,
    componentsPerRescue: base.componentsPerRescue,
    foodPerRescue: base.foodPerRescue,
    hitPointsPerPercent: Math.round(week.guardianHitPoints / 100),
  };
}

/**
 * Builds the day's challenge and who has validated it.
 *
 * @param challenges - Today's challenge draw, or `null` while unresolved.
 * @param ranking - The current weekly ranking, or `null` while unresolved.
 * @returns The order, or `null` when there is no daily to show.
 */
export function buildDailyOrder(
  challenges: CurrentChallenges | null,
  ranking: CurrentRanking | null,
): DailyOrder | null {
  if (!challenges) {
    return null;
  }
  const daily = challenges.dailies.find((entry) => entry.day === challenges.today) ?? null;
  if (!daily) {
    return null;
  }
  const validated = (ranking?.ranking ?? [])
    .filter((entry) => entry.position !== null)
    .map((entry) => ({
      name: entry.player.displayName,
      done:
        entry.challengeProgress.find((line) => line.cadence === 'DAILY' && line.id === daily.id)
          ?.completed ?? false,
    }));
  return {
    name: daily.name,
    description: daily.description,
    survivors: daily.survivors,
    validated,
    doneCount: validated.filter((operator) => operator.done).length,
    deadline: localMidnight(challenges.today, 1).getTime(),
  };
}

/**
 * Builds what the day has given, line by line.
 *
 * @param today - The day in progress, or `null` while unresolved.
 * @param week - The week in progress, or `null` outside one.
 * @param campaign - The campaign, or `null` outside one.
 * @returns The tally, or `null` while any of the three is missing.
 */
export function buildTally(
  today: CampaignToday | null,
  week: CampaignWeek | null,
  campaign: Campaign | null,
): DayTally | null {
  const base = campaign?.base;
  if (!today || !week || !base) {
    return null;
  }
  return {
    guardianName: week.guardianName ?? '',
    damage: today.damage,
    components: today.components,
    carryGained: today.carryGained,
    food: today.food,
    shelterGained: today.shelterGained,
    upkeep: today.dailyUpkeep,
    population: base.population,
    presence: today.presenceCount,
    roster: today.rosterSize,
    pips: Array.from({ length: today.rosterSize }, (_, index) => index < today.presenceCount),
  };
}

/**
 * Bonus a streak of that many days pays: nothing on the first day, two percent per day after,
 * capped at ten — the barème's ladder, restated for an operator who has not played yet and whose
 * streak the daily board therefore does not price.
 */
function streakBonusOf(streakDays: number): number {
  return Math.max(0, Math.min(10, (streakDays - 1) * 2));
}

/**
 * Formats a streak bonus as a multiplier (`"×1.04"`).
 */
function formatMultiplier(bonusPercent: number, language: Language): string {
  const locale = language === 'fr' ? 'fr-FR' : 'en-US';
  return `×${new Intl.NumberFormat(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(1 + bonusPercent / 100)}`;
}

/**
 * Builds the squad sheet: one row per active operator's day.
 *
 * @param daily - The day's ranking, or `null` while unresolved.
 * @param today - The day in progress, used to resolve who holds each title today.
 * @param language - The reader's language, for the streak multiplier's number format.
 * @returns One row per active operator, or an empty sheet while unresolved.
 */
export function buildSquad(
  daily: DailyRanking | null,
  today: CampaignToday | null,
  language: Language,
): readonly SquadRow[] {
  if (!daily) {
    return [];
  }
  const titlesByPlayer = new Map<number, WeeklyTitle>();
  for (const [title, playerId] of Object.entries(today?.titles ?? {})) {
    if (playerId !== undefined && !titlesByPlayer.has(playerId)) {
      titlesByPlayer.set(playerId, title as WeeklyTitle);
    }
  }
  // An inactive operator has no ranking slot: they never deal guardian damage, so they have no
  // line here either.
  const active = daily.ranking.filter((entry) => entry.position !== null);
  return active.map((entry) => {
    const title = titlesByPlayer.get(entry.playerId) ?? null;
    const played = entry.matchCount > 0;
    return {
      position: played ? entry.position : null,
      playerId: entry.playerId,
      name: entry.displayName,
      portrait: resolvePlayerAvatarUrl(entry.portrait),
      title: title === null ? null : { key: title, ...resolveTitleVisual(title) },
      played,
      streakMultiplier: formatMultiplier(
        played ? entry.streakBonusPercent : streakBonusOf(entry.streakAtStake),
        language,
      ),
      streakDays: played ? entry.streakDays : entry.streakAtStake,
      streakAtStake: entry.streakAtStake,
      damage: entry.damage,
      matchCount: entry.matchCount,
      reducedMatchCount: entry.reducedMatchCount,
      components: entry.components,
      food: entry.food,
    };
  });
}
