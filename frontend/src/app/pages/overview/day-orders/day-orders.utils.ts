import { Campaign } from '@core/campaign/campaign.model';
import { CampaignToday } from '@core/campaign/campaign-today.model';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { DAILY_TONE } from '@core/challenges/card/challenge-card.constants';
import { BoardRow } from '@core/challenges/card/challenge-card.model';
import {
  buildChallengeCard,
  toBoardRow,
  toOperators,
} from '@core/challenges/card/challenge-card.utils';
import { CurrentChallenges } from '@core/challenges/challenge.model';
import { campaignMidnight } from '@core/campaign/calendar/campaign-calendar.utils';
import { Language, TranslateFn } from '@core/i18n/translation.model';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { formatSigned } from '../overview.utils';
import { DayTally, TallyTile } from './day-orders.model';

/**
 * Day's challenge card, closing at midnight; `null` when there is no daily to show.
 * `rescueActive`: whether a running campaign turns validations into rescued wounded.
 */
export function buildDailyRow(
  challenges: CurrentChallenges | null,
  rescueActive: boolean,
  kind: string,
  format: (amount: number) => string,
  language: Language,
  translate: TranslateFn,
): BoardRow | null {
  if (!challenges) {
    return null;
  }
  const daily = challenges.dailies.find((entry) => entry.day === challenges.today) ?? null;
  if (!daily) {
    return null;
  }
  const card = buildChallengeCard(
    daily,
    { tone: DAILY_TONE, mark: 'D', kind },
    toOperators(challenges.roster),
    rescueActive,
    format,
  );
  const closesAt = campaignMidnight(challenges.today, 1).getTime();
  return toBoardRow(daily, card, card.rungs, closesAt, language, translate);
}

/**
 * Day's gains, `null` while `today`, `week` or the base is missing.
 */
export function buildTally(
  today: CampaignToday | null,
  week: CampaignWeek | null,
  campaign: Campaign | null,
  players: readonly PlayerSummary[] = [],
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
    pips: Array.from({ length: today.rosterSize }, (_, index) => {
      const player = today.players[index];
      const summary = player ? players.find((entry) => entry.id === player.playerId) : undefined;
      return {
        name: player?.gameName ?? null,
        portrait: resolvePlayerAvatarUrl(summary?.portrait ?? null),
        on: index < today.presenceCount,
      };
    }),
  };
}

/**
 * Day's base flows: components and food with the capacity they bought, population, then upkeep.
 */
export function buildTallyTiles(
  tally: DayTally,
  translate: TranslateFn,
  format: (amount: number) => string,
): readonly TallyTile[] {
  const signed = (amount: number): string => formatSigned(amount, format);
  return [
    {
      tone: 'components',
      icon: CONCEPT_ICONS.components,
      label: translate('common.resource.components'),
      tooltip: translate('overview.orders.componentsTooltip'),
      figure: signed(tally.components),
      gain: translate('overview.orders.carryGain', { count: tally.carryGained }),
      note: null,
    },
    {
      tone: 'food',
      icon: CONCEPT_ICONS.food,
      label: translate('common.resource.food'),
      tooltip: translate('overview.orders.foodTooltip'),
      figure: signed(tally.food),
      gain: translate('overview.orders.shelterGain', { count: tally.shelterGained }),
      note: null,
    },
    {
      // Amber while the base grows, red when it shrinks, like the scene's delta.
      tone: tally.populationChange < 0 ? 'decline' : 'growth',
      icon: CONCEPT_ICONS.base,
      label: translate('overview.orders.population'),
      tooltip: translate('overview.orders.populationTooltip'),
      figure: signed(tally.populationChange),
      gain: null,
      note: translate('overview.orders.tonight', { count: format(tally.population) }),
    },
    {
      tone: 'cost',
      icon: CONCEPT_ICONS.food,
      label: translate('overview.orders.ate'),
      tooltip: translate('overview.orders.ateTooltip'),
      figure: signed(-tally.upkeep),
      gain: null,
      note: translate('overview.orders.food'),
    },
  ];
}
