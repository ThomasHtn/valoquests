import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { LucideChevronDown, LucideHistory } from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import { Campaign } from '@core/campaign/campaign.model';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { CampaignHistory } from '@core/campaign/campaign-history.model';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { localMidnight } from '@core/date/date.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ResourceState } from '@shared/resource-state/resource-state';
import { ROCKET_PART_COUNT } from '@shared/rocket/rocket-drawing.constants';
import { BaseReserves } from './base-reserves/base-reserves';
import { CampaignHistoryView } from './campaign-history/campaign-history';
import { CURRENT_CURVE_COLOR, PAST_CURVE_COLORS } from './campaign-panel.constants';
import {
  HistoryCurve,
  HistoryRow,
  LedgerColumn,
  LedgerRow,
  Reserves,
  RocketPart,
} from './campaign-panel.model';
import {
  buildLedger,
  buildReserves,
  padCurve,
  resolvePlanetState,
  resolveSeasonKey,
} from './campaign-panel.utils';
import { ReserveLedger } from './reserve-ledger/reserve-ledger';
import { RocketShowcase } from './rocket-showcase/rocket-showcase';

/**
 * Overview campaign tab: rocket, base reserves, ledger and folded campaign history.
 */
@Component({
  selector: 'app-campaign-panel',
  imports: [
    TranslatePipe,
    LucideChevronDown,
    LucideHistory,
    ResourceState,
    BaseReserves,
    ReserveLedger,
    RocketShowcase,
    CampaignHistoryView,
  ],
  templateUrl: './campaign-panel.html',
  styleUrl: './campaign-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CampaignPanel {
  /**
   * Campaign held by the overview.
   */
  public readonly campaign = input.required<Campaign | null>();

  /**
   * Translation service, to name rocket parts, curves and seasons.
   */
  private readonly translation = inject(Translation);

  /**
   * Closed campaigns shared with the API service, behind the history fold.
   */
  protected readonly historyResource = inject(CampaignApi).history;

  /**
   * Whether the history is still loading, for its loading state.
   */
  protected readonly historyLoading = anyLoading(this.historyResource);

  /**
   * Whether the history failed, to offer a retry.
   */
  protected readonly historyError = anyError(this.historyResource);

  /**
   * Whether the campaigns' history is unfolded.
   */
  protected readonly historyOpen = signal(false);

  /**
   * Past campaigns, empty while loading or on error.
   */
  private readonly history = computed<readonly CampaignHistory[]>(() =>
    resourceValue(this.historyResource, []),
  );

  /**
   * Week in progress, `null` once settled or outside the ten weeks.
   */
  private readonly currentWeek = computed<CampaignWeek | null>(() => {
    const campaign = this.campaign();
    if (campaign?.status !== 'RUNNING' || campaign.currentWeekIndex === null) {
      return null;
    }
    const week = campaign.weeks[campaign.currentWeekIndex - 1] ?? null;
    return week?.settled ? null : week;
  });

  /**
   * Base stocks and rescue totals, `null` without a campaign.
   */
  protected readonly reserves = computed<Reserves | null>(() =>
    buildReserves(this.campaign(), this.currentWeek()),
  );

  /**
   * One ledger column per campaign week, with its planet state.
   */
  protected readonly ledgerColumns = computed<readonly LedgerColumn[]>(() => {
    const campaign = this.campaign();
    if (!campaign) {
      return [];
    }
    return campaign.weeks.map((week) => ({
      index: week.weekIndex,
      name: week.planetName,
      state: resolvePlanetState(week, campaign),
    }));
  });

  /**
   * Ledger rows tracing each resource across the weeks.
   */
  protected readonly ledger = computed<readonly LedgerRow[]>(() =>
    buildLedger(this.campaign(), this.ledgerColumns()),
  );

  /**
   * Rocket parts with their build state, one fitted per defeated guardian.
   */
  protected readonly rocketParts = computed<readonly RocketPart[]>(() => {
    const campaign = this.campaign();
    if (!campaign) {
      return [];
    }
    const fittedWeeks = campaign.weeks
      .filter((week) => week.defeated)
      .map((week) => week.weekIndex);
    const built = fittedWeeks.length;
    const running = campaign.status === 'RUNNING';
    return Array.from({ length: ROCKET_PART_COUNT }, (_, offset) => {
      const index = offset + 1;
      const state = index <= built ? 'built' : index === built + 1 && running ? 'next' : 'locked';
      return {
        index,
        label: String(index).padStart(2, '0'),
        name: this.translation.translate(`campaign.rocket.parts.${index}`),
        state,
        week: fittedWeeks[offset] ?? null,
      };
    });
  });

  /**
   * Population curves of the current campaign and the past ones.
   */
  protected readonly curves = computed<readonly HistoryCurve[]>(() => {
    const campaign = this.campaign();
    const curves: HistoryCurve[] = [];
    if (campaign && campaign.number !== null && campaign.status !== 'CLOSED') {
      const points = campaign.weeks.map((week) => week.base?.population ?? null);
      curves.push({
        series: {
          label: this.translation.translate('campaign.history.current', {
            number: campaign.number,
          }),
          color: CURRENT_CURVE_COLOR,
          points: padCurve(points),
          filled: true,
        },
        figure: campaign.base?.population ?? 0,
        current: true,
      });
    }
    this.history().forEach((past, rank) => {
      curves.push({
        series: {
          label: this.translation.translate('campaign.history.past', { number: past.number }),
          color: PAST_CURVE_COLORS[rank % PAST_CURVE_COLORS.length],
          points: padCurve(past.weeklyPopulation),
          dashed: rank % 2 === 1,
        },
        figure: past.population,
        current: false,
      });
    });
    return curves;
  });

  /**
   * Past and current campaigns ranked by final population.
   */
  protected readonly historyRows = computed<readonly HistoryRow[]>(() => {
    const campaign = this.campaign();
    const rows: HistoryRow[] = this.history().map((past) => ({
      rank: 0,
      number: past.number,
      subtitle:
        past.stoppedOn === null
          ? this.season(past.firstWeekStart)
          : this.translation.translate('campaign.history.stopped', {
              season: this.season(past.firstWeekStart),
            }),
      difficulty: past.difficulty,
      population: past.population,
      guardiansDefeated: past.guardiansDefeated,
      weeksPlayed: past.weeklyPopulation.length,
      rescued: past.rescued,
      current: false,
    }));
    if (
      campaign &&
      campaign.number !== null &&
      campaign.difficulty &&
      campaign.status !== 'CLOSED'
    ) {
      rows.push({
        rank: 0,
        number: campaign.number,
        subtitle:
          campaign.status === 'RUNNING' && campaign.currentWeekIndex !== null
            ? this.translation.translate('campaign.history.inProgress', {
                week: campaign.currentWeekIndex,
                weeks: CAMPAIGN_WEEK_COUNT,
              })
            : this.translation.translate('common.campaignStatus.OPENED'),
        difficulty: campaign.difficulty,
        population: campaign.base?.population ?? 0,
        guardiansDefeated: campaign.totals?.guardiansDefeated ?? 0,
        weeksPlayed: campaign.currentWeekIndex ?? campaign.totals?.weeksSettled ?? 0,
        rescued: campaign.totals?.rescued ?? 0,
        current: true,
      });
    }
    return rows
      .sort((a, b) => b.population - a.population)
      .map((row, position) => ({ ...row, rank: position + 1 }));
  });

  /**
   * Mirrors the fold state so the history renders only once opened.
   */
  protected toggleHistory(event: Event): void {
    this.historyOpen.set((event.target as HTMLDetailsElement).open);
  }

  /**
   * Reloads the history after a failure.
   */
  protected retryHistory(): void {
    reloadAll(this.historyResource);
  }

  /**
   * Names the season a campaign started in, as its table subtitle.
   */
  private season(isoDate: string): string {
    const date = localMidnight(isoDate);
    return this.translation.translate(`campaign.history.season.${resolveSeasonKey(date)}`, {
      year: date.getFullYear(),
    });
  }
}
