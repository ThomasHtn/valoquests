import { Campaign } from '@core/campaign/campaign.model';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { resolvePlanetArtUrl } from '@core/campaign/planets/campaign-planet-art.utils';
import { TranslateFn } from '@core/i18n/translation.model';
import { FriezeWeek } from './overview.model';

/**
 * Ten-week frieze cells, empty outside a campaign.
 */
export function buildFrieze(
  campaign: Campaign | null,
  translate: TranslateFn,
): readonly FriezeWeek[] {
  if (!campaign || campaign.weeks.length === 0) {
    return [];
  }
  return campaign.weeks.map((week) => toFriezeWeek(week, campaign, translate));
}

/**
 * Maps one campaign week onto its frieze cell.
 */
function toFriezeWeek(week: CampaignWeek, campaign: Campaign, translate: TranslateFn): FriezeWeek {
  const state = friezeStateOf(week, campaign);
  // Only a played week quotes how far the guardian was pushed.
  const played = state === 'lost' || state === 'now';
  const params = played ? { percent: week.progressPercent } : undefined;
  // The spoken label spells out the level the cell abbreviates.
  const category = translate(`common.guardianCategory.${week.category}`);
  const titleState = translate(`overview.frieze.${friezeTitleKeyOf(state, campaign)}`, params);
  return {
    index: week.weekIndex,
    label: String(week.weekIndex).padStart(2, '0'),
    name: week.planetName,
    art: resolvePlanetArtUrl(week.weekIndex),
    level: translate(`overview.frieze.level.${week.category}`),
    settled: week.settled,
    state,
    standing: friezeStandingOf(week, state),
    status: translate(`overview.frieze.status.${friezeStatusKeyOf(week, campaign, state)}`, params),
    title: translate('overview.frieze.title', { category, state: titleState }),
  };
}

/**
 * Where a week stands on the frieze: won, lost, in progress, or still ahead.
 */
function friezeStateOf(week: CampaignWeek, campaign: Campaign): FriezeWeek['state'] {
  if (week.defeated) {
    return 'won';
  }
  if (week.settled) {
    return 'lost';
  }
  const isCurrent = week.weekIndex === campaign.currentWeekIndex && campaign.status === 'RUNNING';
  return isCurrent ? 'now' : 'ahead';
}

/**
 * Share of the guardian left: empty once won, full while ahead.
 */
function friezeStandingOf(week: CampaignWeek, state: FriezeWeek['state']): number {
  if (state === 'won') {
    return 0;
  }
  if (state === 'ahead') {
    return 1;
  }
  return (100 - week.progressPercent) / 100;
}

/**
 * Suffix of the cell's `overview.frieze.status.*` key.
 */
function friezeStatusKeyOf(
  week: CampaignWeek,
  campaign: Campaign,
  state: FriezeWeek['state'],
): string {
  if (state !== 'ahead') {
    return state;
  }
  if (isUnplayed(campaign)) {
    return 'unplayed';
  }
  return week.weekIndex === CAMPAIGN_WEEK_COUNT ? 'final' : 'ahead';
}

/**
 * Suffix of the spoken label's `overview.frieze.*` key.
 */
function friezeTitleKeyOf(state: FriezeWeek['state'], campaign: Campaign): string {
  return state === 'ahead' && isUnplayed(campaign) ? 'unplayed' : state;
}

/**
 * Whether the weeks ahead will never be played (closed campaign).
 */
function isUnplayed(campaign: Campaign): boolean {
  return campaign.status === 'CLOSED';
}

/**
 * Gain or loss with its sign, a true minus for losses, nothing before zero.
 */
export function formatSigned(amount: number, format: (amount: number) => string): string {
  const sign = amount > 0 ? '+' : amount < 0 ? '−' : '';
  return `${sign}${format(Math.abs(amount))}`;
}
