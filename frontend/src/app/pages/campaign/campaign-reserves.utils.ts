import { Campaign, CampaignWeek, ExtractionLimiter } from '@core/campaign/campaign.model';
import { LOSS_EXAMPLES, SAMPLE_POPULATION } from './campaign.constants';
import { LawNotes, RescueLaw, Reserves } from './campaign.model';

/**
 * Pure builders of the rescue law, its notes and the base's reserves, out of the campaign the
 * API returned. Each returns `null` when the campaign has nothing to say yet.
 */

/**
 * The formula settling every Sunday, with the live figures in every term.
 */
export function buildRescueLaw(
  campaign: Campaign | null,
  week: CampaignWeek | null,
): RescueLaw | null {
  const base = campaign?.base;
  const forecast = campaign?.forecast;
  if (!week || !base || !forecast) {
    return null;
  }
  return {
    carry: base.rescuesByComponents,
    shelter: base.rescuesByFood,
    wounded: week.woundedCount,
    planetName: week.planetName,
    breachPercent: week.progressPercent,
    extracted: forecast.extractionRescued,
    byChallenges: forecast.challengeRescued,
    componentsPerRescue: base.componentsPerRescue,
    foodPerRescue: base.foodPerRescue,
    hitPointsPerPercent: Math.round(week.guardianHitPoints / 100),
  };
}

/**
 * The loss note: what a failed breakthrough costs, illustrated at a few breach levels.
 */
export function buildLawNotes(campaign: Campaign | null): LawNotes | null {
  if (!campaign?.difficulty || campaign.reference === null) {
    return null;
  }
  // Before the first day is replayed the base is empty: the note then reasons on a sample base.
  const sample = (campaign.base?.population ?? 0) === 0;
  const population = sample ? SAMPLE_POPULATION : (campaign.base?.population ?? 0);
  const rate = (campaign.base?.guardianLossPercent ?? 0) / 100;
  return {
    difficulty: campaign.difficulty,
    reference: campaign.reference,
    population,
    sample,
    losses: LOSS_EXAMPLES.map((breachPercent) => ({
      breachPercent,
      lost: Math.round(population * (1 - breachPercent / 100) ** 2 * rate),
    })),
  };
}

/**
 * The base's stocks, what they can carry, and the campaign's rescue totals so far.
 */
export function buildReserves(
  campaign: Campaign | null,
  currentWeek: CampaignWeek | null,
): Reserves | null {
  const base = campaign?.base;
  const totals = campaign?.totals;
  // An opened campaign has no replayed day yet: nothing to read, so nothing shown.
  if (!campaign || !base || !totals || campaign.status === 'OPENED') {
    return null;
  }
  const settled = campaign.weeks.filter((week) => week.settled);
  const reference = currentWeek ?? settled.at(-1) ?? campaign.weeks[0];
  const wounded = reference?.woundedCount ?? 0;
  const spotted = settled.reduce((sum, week) => sum + week.woundedCount, 0);
  const byExtraction = totals.rescued - totals.challengeRescued;
  const leftBehind = Math.max(0, spotted - totals.rescued);
  const share = (value: number): number =>
    spotted > 0 ? Math.round((value / spotted) * 1000) / 10 : 0;
  const limited = (limiter: ExtractionLimiter): number =>
    settled.filter((week) => week.limiter === limiter).length;
  return {
    food: {
      stock: base.foodStock,
      capacity: base.rescuesByFood,
      fraction: wounded > 0 ? Math.min(1, base.rescuesByFood / wounded) : 0,
    },
    components: {
      stock: base.componentsStock,
      capacity: base.rescuesByComponents,
      fraction: wounded > 0 ? Math.min(1, base.rescuesByComponents / wounded) : 0,
    },
    dailyUpkeep: base.dailyUpkeep,
    wounded,
    planetName: reference?.planetName ?? '',
    rescued: totals.rescued,
    spotted,
    byExtraction,
    byChallenges: totals.challengeRescued,
    byChallengesPercent:
      totals.rescued > 0 ? Math.round((totals.challengeRescued / totals.rescued) * 100) : 0,
    leftBehind,
    shares: [share(byExtraction), share(totals.challengeRescued), share(leftBehind)],
    guardiansDefeated: totals.guardiansDefeated,
    weeksSettled: totals.weeksSettled,
    limitedByFood: limited('FOOD'),
    limitedByComponents: limited('COMPONENTS'),
    wholeGroup: limited('NONE') + limited('GROUP'),
  };
}
