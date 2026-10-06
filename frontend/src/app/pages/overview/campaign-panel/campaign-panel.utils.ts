import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { Campaign } from '@core/campaign/campaign.model';
import { CampaignWeek, ExtractionLimiter } from '@core/campaign/campaign-week.model';

import {
  LedgerCell,
  LedgerColumn,
  LedgerKey,
  LedgerRow,
  PlanetState,
  Reserves,
} from './campaign-panel.model';

/**
 * Where a week stands: won, lost, the one being played, or still ahead.
 */
export function resolvePlanetState(week: CampaignWeek, campaign: Campaign): PlanetState {
  if (week.defeated) {
    return 'won';
  }
  if (week.settled) {
    return 'lost';
  }
  return week.weekIndex === campaign.currentWeekIndex && campaign.status === 'RUNNING'
    ? 'now'
    : 'ahead';
}

/**
 * Pads a curve with trailing gaps so every campaign spans the ten weeks.
 */
export function padCurve(points: readonly (number | null)[]): readonly (number | null)[] {
  return Array.from({ length: CAMPAIGN_WEEK_COUNT }, (_, index) => points[index] ?? null);
}

/**
 * Season a date falls in, as a translation key suffix.
 */
export function resolveSeasonKey(date: Date): 'winter' | 'spring' | 'summer' | 'autumn' {
  const month = date.getMonth();
  if (month <= 1 || month === 11) {
    return 'winter';
  }
  if (month <= 4) {
    return 'spring';
  }
  return month <= 7 ? 'summer' : 'autumn';
}

/**
 * Base stocks, what they pay for, and the campaign's rescue totals.
 */
export function buildReserves(
  campaign: Campaign | null,
  currentWeek: CampaignWeek | null,
): Reserves | null {
  const base = campaign?.base;
  const totals = campaign?.totals;
  // An opened campaign has no replayed day yet.
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

/**
 * Both ledger rows on one shared scale; empty until a week has been replayed.
 */
export function buildLedger(
  campaign: Campaign | null,
  columns: readonly LedgerColumn[],
): readonly LedgerRow[] {
  const totals = campaign?.totals;
  if (!campaign || !totals || !campaign.weeks.some((week) => week.base !== null)) {
    return [];
  }
  const raw = (['food', 'components'] as const).map((key) => ({
    key,
    cells: buildLedgerCells(campaign, columns, key),
  }));
  const top = Math.max(
    1,
    ...raw.flatMap((row) => row.cells.flatMap((cell) => [cell.got, cell.spent, cell.carry])),
  );
  return raw.map(({ key, cells }) => ({
    key,
    gained: key === 'food' ? totals.foodGained : totals.componentsGained,
    spent: cells.reduce((sum, cell) => sum + (cell.kind === 'settled' ? cell.spent : 0), 0),
    cells: cells.map((cell) => ({
      ...cell,
      gotShare: cell.got / top,
      spentShare: cell.spent / top,
      carryShare: cell.carry / top,
    })),
  }));
}

/**
 * One cell per week; a settled week's stock carries into the next.
 */
function buildLedgerCells(
  campaign: Campaign,
  columns: readonly LedgerColumn[],
  key: LedgerKey,
): LedgerCell[] {
  const base = campaign.base!;
  const per = key === 'food' ? base.foodPerRescue : base.componentsPerRescue;
  let carriedIn = 0;
  return campaign.weeks.map((week, offset) => {
    const state = columns[offset].state;
    const kind = state === 'now' ? 'now' : state === 'ahead' ? 'ahead' : 'settled';
    const got = key === 'food' ? (week.base?.foodGained ?? 0) : (week.base?.componentsGained ?? 0);
    const spent = key === 'food' ? week.foodSpent : week.componentsSpent;
    const stock = key === 'food' ? (week.base?.foodStock ?? 0) : (week.base?.componentsStock ?? 0);
    const cell: LedgerCell = {
      index: week.weekIndex,
      planetName: week.planetName,
      kind: week.base === null && kind !== 'ahead' ? 'ahead' : kind,
      got,
      spent,
      carry: kind === 'settled' ? stock : 0,
      carriedIn,
      stock,
      rescues: Math.floor(got / per),
      stockRescues: Math.floor(stock / per),
      gotShare: 0,
      spentShare: 0,
      carryShare: 0,
    };
    if (kind === 'settled') {
      carriedIn = stock;
    }
    return cell;
  });
}
