import { Campaign } from '@core/campaign/campaign.model';
import { LedgerCell, LedgerRow, Planet } from './campaign.model';

/**
 * Pure builder of the reserve ledger: one row per resource, one cell per planet.
 */

type LedgerKey = LedgerRow['key'];

/**
 * Both rows of the ledger, scaled together so a food bar and a components bar of the same height
 * mean the same quantity. Empty until a week has been replayed.
 */
export function buildLedger(
  campaign: Campaign | null,
  planets: readonly Planet[],
): readonly LedgerRow[] {
  const totals = campaign?.totals;
  if (!campaign || !totals || !campaign.weeks.some((week) => week.base !== null)) {
    return [];
  }
  const raw = (['food', 'components'] as const).map((key) => ({
    key,
    cells: buildLedgerCells(campaign, planets, key),
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
    spark: cells.map((cell) => (cell.kind === 'ahead' ? null : cell.got / top)),
  }));
}

/**
 * One cell per week for one resource; the stock a settled week leaves is carried into the next.
 */
function buildLedgerCells(
  campaign: Campaign,
  planets: readonly Planet[],
  key: LedgerKey,
): LedgerCell[] {
  const base = campaign.base!;
  const per = key === 'food' ? base.foodPerRescue : base.componentsPerRescue;
  let carriedIn = 0;
  return campaign.weeks.map((week, offset) => {
    const planet = planets[offset];
    const kind = planet.state === 'now' ? 'now' : planet.state === 'ahead' ? 'ahead' : 'settled';
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
