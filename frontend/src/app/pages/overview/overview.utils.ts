import {
  Campaign,
  CAMPAIGN_WEEK_COUNT,
  CampaignToday,
  CampaignWeek,
  WEEKLY_TITLES,
} from '@core/campaign/campaign.model';
import { primaryTitleOf } from '@core/campaign/campaign-title.utils';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { resolvePlanetArtUrl } from '@core/campaign/planet-art.utils';
import { CurrentChallenges } from '@core/challenges/challenge.model';
import { campaignMidnight } from '@core/date/campaign-time-zone.utils';
import { daysBetween, localMidnight } from '@core/date/date-time.utils';
import { Language } from '@core/i18n/translation.model';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { CurrentRanking, DailyRanking, RankingHistoryWeek } from '@core/ranking/ranking.model';
import {
  Capacity,
  Contribution,
  ContributionShare,
  DailyOrder,
  DayTally,
  FriezeWeek,
  Mission,
  MissionReport,
  MissionReportBlow,
  MissionReportChampion,
  SquadRow,
  StreakPip,
} from './overview.model';
import { SEEN_REPORT_KEY } from './overview.constants';

/**
 * Translates a key, the same shape as `Translation.translate`, kept as a structural type here so
 * this module stays free of any dependency on the i18n service.
 */
export type Translate = (key: string, params?: Readonly<Record<string, string | number>>) => string;

/**
 * Builds the ten-week frieze: each week's planet, its guardian's level, its outcome, and how much
 * of the guardian is left.
 *
 * @param campaign - The campaign, or `null` outside one.
 * @param translate - Translation function for each week's level, status and spoken label.
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

/**
 * Maps one campaign week onto its frieze cell.
 */
function toFriezeWeek(week: CampaignWeek, campaign: Campaign, translate: Translate): FriezeWeek {
  const isCurrent = week.weekIndex === campaign.currentWeekIndex && campaign.status === 'RUNNING';
  const planet = {
    index: week.weekIndex,
    label: String(week.weekIndex).padStart(2, '0'),
    name: week.planetName,
    art: resolvePlanetArtUrl(week.weekIndex),
    level: translate(`overview.frieze.level.${week.category}`),
    settled: week.settled,
  };
  // The level in full leads the spoken label, which spells out what the cell abbreviates.
  const category = translate(`common.guardianCategory.${week.category}`);
  const title = (state: string): string => translate('overview.frieze.title', { category, state });
  if (week.defeated) {
    return {
      ...planet,
      state: 'won',
      standing: 0,
      status: translate('overview.frieze.status.won'),
      title: title(translate('overview.frieze.won')),
    };
  }
  // The guardian's hit points left, the reading the ring shows: a guardian that held with 22 %
  // reads 22, not the 78 % of breakthrough.
  const left = 100 - week.progressPercent;
  if (week.settled) {
    return {
      ...planet,
      state: 'lost',
      standing: left / 100,
      status: translate('overview.frieze.status.lost', { percent: left }),
      title: title(translate('overview.frieze.lost', { percent: left })),
    };
  }
  if (isCurrent) {
    return {
      ...planet,
      state: 'now',
      standing: left / 100,
      status: translate('overview.frieze.status.now', { percent: week.progressPercent }),
      title: title(translate('overview.frieze.now', { percent: week.progressPercent })),
    };
  }
  // A closed campaign's remaining weeks were never played: they are not coming any more.
  const unplayed = campaign.status === 'CLOSED';
  let status = 'ahead';
  if (unplayed) {
    status = 'unplayed';
  } else if (week.weekIndex === CAMPAIGN_WEEK_COUNT) {
    status = 'final';
  }
  return {
    ...planet,
    state: 'ahead',
    standing: 1,
    status: translate(`overview.frieze.status.${status}`),
    title: title(translate(unplayed ? 'overview.frieze.unplayed' : 'overview.frieze.ahead')),
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
 * The fatal blow in three parts: when, who, and on which map, mode and score when the match is known.
 */
function blowOf(
  week: CampaignWeek,
  players: readonly PlayerSummary[],
  language: Language,
  translate: Translate,
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
 * Builds a settled week's Monday report, the last one unless another is asked for.
 *
 * @param campaign - The campaign, or `null` outside one.
 * @param players - Tracked players, used to resolve portraits and the fatal blow's name.
 * @param history - Frozen weeks, for the settled week's titles and ranking.
 * @param language - The reader's language, for the fatal blow's weekday and time.
 * @param translate - Translation function for the fatal blow's sentence.
 * @param weekIndex - The settled week to report, or `null` for the last one.
 * @returns The report, or `null` when that week is not settled yet.
 */
export function buildMissionReport(
  campaign: Campaign | null,
  players: readonly PlayerSummary[],
  history: readonly RankingHistoryWeek[],
  language: Language,
  translate: Translate,
  weekIndex: number | null = null,
): MissionReport | null {
  const settledWeeks = campaign?.weeks.filter((week) => week.settled) ?? [];
  const settled =
    weekIndex === null
      ? settledWeeks.at(-1)
      : settledWeeks.find((week) => week.weekIndex === weekIndex);
  if (!campaign || !settled) {
    return null;
  }
  const portraitOf = (id: number): string | null =>
    resolvePlayerAvatarUrl(players.find((player) => player.id === id)?.portrait ?? null);
  const frozen = history.find((week) => week.weekStart === settled.weekStart) ?? null;
  const next = campaign.weeks[settled.weekIndex] ?? null;
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
    base: settled.base
      ? {
          foodGained: settled.base.foodGained,
          componentsGained: settled.base.componentsGained,
          population: settled.base.population,
          populationChange: settled.base.populationChange,
        }
      : null,
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
    champion: frozen ? championOf(frozen, portraitOf) : null,
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
 * Resolves a frozen week's champion, the operator who finished alone in first place.
 *
 * @param frozen - The frozen week.
 * @param portraitOf - Resolves a player's portrait.
 * @returns The champion, holder `null` when nobody won the week outright.
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
 * Builds the squad's contribution to the week, on the guardian's own scale.
 *
 * The bar answers one question the four dials cannot: who is carrying the week. It is drawn on the
 * guardian's hit points rather than on the squad's own total, so the empty end of the bar states
 * what is left to do — and the challenge points ride in the same segments, because that sum is what
 * the ranking orders on and calling it "damage" would hide half of it.
 *
 * @param ranking - The current weekly ranking, or `null` while unresolved.
 * @param week - The week in progress, or `null` outside one.
 * @returns The contribution, or `null` outside a week with a roster to read.
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
  // Monday opens with every figure at zero, and the row stays: an empty bar beside a full guardian
  // is the week's starting position, not a missing reading.
  const total = ranking.ranking.reduce((sum, entry) => sum + entry.totalPoints, 0);
  const shares: ContributionShare[] = ranking.ranking
    .filter((entry) => entry.totalPoints > 0)
    .map((entry) => ({
      playerId: entry.player.id,
      name: entry.player.displayName,
      damage: entry.guardianDamage,
      challengePoints: entry.challengePoints,
      total: entry.totalPoints,
      fraction: Math.min(1, entry.totalPoints / week.guardianHitPoints),
      sharePercent: total > 0 ? Math.round((entry.totalPoints / total) * 100) : 0,
    }))
    .sort((left, right) => right.total - left.total);
  return { total, hitPoints: week.guardianHitPoints, shares };
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
    .filter((entry) => entry.competing)
    .map((entry) => ({
      name: entry.player.displayName,
      done:
        entry.challengeProgress.find((line) => line.cadence === 'DAILY' && line.id === daily.id)
          ?.completed ?? false,
    }))
    // Validated operators first so the lit hexagons form one unbroken run.
    .sort((left, right) => Number(right.done) - Number(left.done));
  return {
    name: daily.name,
    description: daily.description,
    survivors: daily.survivors,
    validated,
    doneCount: validated.filter((operator) => operator.done).length,
    deadline: campaignMidnight(challenges.today, 1).getTime(),
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
    weekIndex: week.weekIndex,
    damage: today.damage,
    components: today.components,
    carryGained: today.carryGained,
    food: today.food,
    shelterGained: today.shelterGained,
    upkeep: today.dailyUpkeep,
    population: base.population,
    populationChange: base.populationChange,
    presence: today.presenceCount,
    roster: today.rosterSize,
    pips: Array.from({ length: today.rosterSize }, (_, index) => ({
      name: today.players[index]?.gameName ?? null,
      on: index < today.presenceCount,
    })),
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
 * Lays out the week from Monday to Sunday around a day, marking the days an operator played.
 */
function streakWeekOf(day: string, playedDays: readonly string[]): readonly StreakPip[] {
  const todayIndex = (localMidnight(day).getDay() + 6) % 7;
  const played = new Set(playedDays.map((playedDay) => todayIndex - daysBetween(playedDay, day)));
  return Array.from({ length: 7 }, (_, index): StreakPip => {
    if (played.has(index)) {
      return 'played';
    }
    if (index === todayIndex) {
      return 'today';
    }
    return index < todayIndex ? 'missed' : 'ahead';
  });
}

/**
 * Initials of the days of the week, Monday first (`L M M J V S D`, `M T W T F S S`).
 *
 * @param language - The reader's language.
 * @returns Seven single-letter labels.
 */
export function weekdayInitials(language: Language): readonly string[] {
  const format = new Intl.DateTimeFormat(language, { weekday: 'narrow', timeZone: 'UTC' });
  // 2024-01-01 is a Monday; read at noon UTC so no zone shifts the weekday.
  return Array.from({ length: 7 }, (_, index) =>
    format.format(new Date(Date.UTC(2024, 0, 1 + index, 12))),
  );
}

/**
 * Builds the squad sheet: one row per active operator's day.
 *
 * @param daily - The day's ranking, or `null` while unresolved.
 * @param today - The day in progress, used to resolve who holds each title today.
 * @returns One row per active operator, or an empty sheet while unresolved.
 */
export function buildSquad(
  daily: DailyRanking | null,
  today: CampaignToday | null,
): readonly SquadRow[] {
  if (!daily) {
    return [];
  }
  const titles = today?.titles ?? {};
  // An inactive operator never deals guardian damage, so they have no line here either.
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

/**
 * Monday of the last mission report the reader dismissed, or `null`; storage failures read as never seen.
 */
export function readSeenReport(): string | null {
  try {
    return localStorage.getItem(SEEN_REPORT_KEY);
  } catch {
    return null;
  }
}

/**
 * Remembers the mission report as dismissed; storage failures are ignored.
 */
export function writeSeenReport(weekStart: string): void {
  try {
    localStorage.setItem(SEEN_REPORT_KEY, weekStart);
  } catch {
    // Nothing to do: the report will open again next time.
  }
}
