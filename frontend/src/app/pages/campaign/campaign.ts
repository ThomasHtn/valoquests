import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { LucideTarget } from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import {
  Campaign as CampaignModel,
  CAMPAIGN_WEEK_COUNT,
  CampaignHistory,
  CampaignWeek,
} from '@core/campaign/campaign.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { daysBetween, localMidnight } from '@core/date/date-time.utils';
import { resolveLocale } from '@core/i18n/locale.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PlayersApi } from '@core/players/players-api';
import { PageHeader } from '@layout/page-header/page-header';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { ROCKET_PART_COUNT } from '@shared/rocket/rocket-drawing.constants';
import { SectionRule } from '@shared/section-rule/section-rule';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { BaseReserves } from './base-reserves/base-reserves';
import { CampaignHistoryView } from './campaign-history/campaign-history';
import { buildLedger } from './campaign-ledger.utils';
import { padCurve, resolvePlanetState, resolveSeasonKey } from './campaign-planet.utils';
import { buildLawNotes, buildRescueLaw, buildReserves } from './campaign-reserves.utils';
import { CURRENT_CURVE_COLOR, PAST_CURVE_COLORS } from './campaign.constants';
import {
  HistoryCurve,
  HistoryRow,
  LawNotes,
  LedgerColumn,
  LedgerRow,
  Planet,
  PlanetReport,
  PlanetState,
  planetLook,
  PlanetStateIcon,
  RescueLaw,
  Reserves,
  RocketPart,
} from './campaign.model';
import { PlanetStrip } from './planet-strip/planet-strip';
import { RescueLawView } from './rescue-law/rescue-law';
import { ReserveLedger } from './reserve-ledger/reserve-ledger';
import { RocketShowcase } from './rocket-showcase/rocket-showcase';
import { StarField } from './star-field';

/**
 * The road of the ten planets: where the campaign stands, the rule that settles every Sunday,
 * the base's reserves, the rocket being built and the campaigns before this one.
 *
 * This is where the game is explained. The overview only measures; the formula, the notes and
 * the ledger live here, once, with the live figures in every term.
 */
@Component({
  selector: 'app-campaign',
  imports: [
    TranslatePipe,
    PageHeader,
    ResourceState,
    SectionRule,
    StarField,
    PlanetStrip,
    RescueLawView,
    BaseReserves,
    ReserveLedger,
    RocketShowcase,
    CampaignHistoryView,
    LucideTarget,
  ],
  templateUrl: './campaign.html',
  styleUrl: './campaign.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Campaign {
  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  private readonly campaignApi = inject(CampaignApi);

  private readonly playersApi = inject(PlayersApi);

  private readonly translation = inject(Translation);

  protected readonly campaignResource = this.campaignApi.campaign;

  protected readonly historyResource = this.campaignApi.history;

  protected readonly isLoading = anyLoading(this.campaignResource, this.historyResource);

  protected readonly isError = anyError(this.campaignResource, this.historyResource);

  protected readonly campaign = computed(() => resourceValue(this.campaignResource, null) ?? null);

  protected readonly history = computed<readonly CampaignHistory[]>(
    () => resourceValue(this.historyResource, null) ?? [],
  );

  protected readonly hasCampaign = computed(() => this.campaign()?.status !== null);

  /**
   * The week in progress of a running campaign, or `null`.
   */
  protected readonly currentWeek = computed<CampaignWeek | null>(() => {
    const campaign = this.campaign();
    if (campaign?.status !== 'RUNNING' || campaign.currentWeekIndex === null) {
      return null;
    }
    const week = campaign.weeks[campaign.currentWeekIndex - 1] ?? null;
    return week?.settled ? null : week;
  });

  /**
   * The campaign, then its tier: the header's slot beside the eyebrow only exists on a page with a
   * way back, so the tier rides in the line itself.
   */
  protected readonly headerEyebrow = computed(() => {
    const campaign = this.campaign();
    if (!campaign || campaign.number === null) {
      return this.translation.translate('campaign.header.noCampaign');
    }
    const eyebrow = this.translation.translate('campaign.header.eyebrow', {
      number: campaign.number,
    });
    return campaign.tier
      ? `${eyebrow} · ${this.translation.translate('campaign.header.tier', {
          tier: this.translation.translate(`common.tier.${campaign.tier}`),
        })}`
      : eyebrow;
  });

  protected readonly planets = computed<readonly Planet[]>(() => {
    const campaign = this.campaign();
    if (!campaign) {
      return [];
    }
    return campaign.weeks.map((week) => this.toPlanet(week, campaign));
  });

  protected readonly law = computed<RescueLaw | null>(() =>
    buildRescueLaw(this.campaign(), this.currentWeek()),
  );

  protected readonly notes = computed<LawNotes | null>(() => buildLawNotes(this.campaign()));

  protected readonly reserves = computed<Reserves | null>(() =>
    buildReserves(this.campaign(), this.currentWeek()),
  );

  protected readonly ledgerColumns = computed<readonly LedgerColumn[]>(() =>
    this.planets().map((planet) => ({
      index: planet.index,
      name: planet.name,
      state: planet.state,
    })),
  );

  protected readonly ledger = computed<readonly LedgerRow[]>(() =>
    buildLedger(this.campaign(), this.planets()),
  );

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

  protected readonly builtCount = computed(() => this.campaign()?.totals?.guardiansDefeated ?? 0);

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
      tier: past.tier,
      population: past.population,
      guardiansDefeated: past.guardiansDefeated,
      weeksPlayed: past.weeklyPopulation.length,
      rescued: past.rescued,
      current: false,
    }));
    if (campaign && campaign.number !== null && campaign.tier && campaign.status !== 'CLOSED') {
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
        tier: campaign.tier,
        population: campaign.base?.population ?? 0,
        guardiansDefeated: campaign.totals?.guardiansDefeated ?? 0,
        weeksPlayed: campaign.totals?.weeksSettled ?? 0,
        rescued: campaign.totals?.rescued ?? 0,
        current: true,
      });
    }
    return rows
      .sort((a, b) => b.population - a.population)
      .map((row, position) => ({ ...row, rank: position + 1 }));
  });

  /**
   * What the page says in place of the road when there is nothing to show.
   */
  protected readonly stateKey = computed(() => {
    const campaign = this.campaign();
    if (!campaign || campaign.status === null) {
      return 'none';
    }
    return campaign.status === 'OPENED' ? 'opened' : campaign.status === 'CLOSED' ? 'closed' : '';
  });

  /**
   * The empty state: the road to be drawn, and when it starts once a campaign is opened.
   */
  protected readonly emptyPlate = computed<EmptyPlate>(() => {
    const t = (suffix: string, params?: Readonly<Record<string, number>>) =>
      this.translation.translate(`campaign.state.none.${suffix}`, params);
    return {
      illustration: 'road',
      eyebrow: t('eyebrow'),
      title: t('title'),
      text: t('text'),
      readouts: [
        { tone: 'todo', label: t('start'), value: t('startValue') },
        {
          tone: 'info',
          label: t('duration'),
          value: t('durationValue', { weeks: CAMPAIGN_WEEK_COUNT }),
        },
      ],
    };
  });

  protected retry(): void {
    reloadAll(this.campaignResource, this.historyResource);
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  private locale(): string {
    return resolveLocale(this.translation.language());
  }

  private weekday(date: Date): string {
    const weekday = new Intl.DateTimeFormat(this.locale(), { weekday: 'long' }).format(date);
    return weekday.charAt(0).toUpperCase() + weekday.slice(1);
  }

  private season(isoDate: string): string {
    const date = localMidnight(isoDate);
    return this.translation.translate(`campaign.history.season.${resolveSeasonKey(date)}`, {
      year: date.getFullYear(),
    });
  }

  private toPlanet(week: CampaignWeek, campaign: CampaignModel): Planet {
    const state = resolvePlanetState(week, campaign);
    const final = week.weekIndex === CAMPAIGN_WEEK_COUNT;
    const { radius, hue } = planetLook(week, final);
    const [stateLabel, stateIcon] = this.stateLine(
      week,
      state,
      final,
      campaign.status === 'CLOSED',
    );
    return {
      index: week.weekIndex,
      label: String(week.weekIndex).padStart(2, '0'),
      name: week.planetName,
      category: week.category,
      state,
      final,
      advance: state === 'ahead' ? 0 : week.progressPercent / 100,
      radius,
      hue,
      stateLabel,
      stateIcon,
      report: this.report(week, state, final, campaign),
    };
  }

  private stateLine(
    week: CampaignWeek,
    state: PlanetState,
    final: boolean,
    closed: boolean,
  ): [string, PlanetStateIcon] {
    const t = (key: string, params?: Record<string, string | number>): string =>
      this.translation.translate(`campaign.galaxy.${key}`, params);
    switch (state) {
      case 'won':
        return [t('won', { weekday: this.defeatedWeekday(week).toLowerCase() }), 'check'];
      case 'lost':
        return [t('lost', { percent: week.progressPercent }), 'x'];
      case 'now':
        return [t('now', { percent: week.progressPercent }), 'swords'];
      default:
        // A closed campaign's remaining planets were never reached.
        if (closed) {
          return [t('unplayed'), null];
        }
        return final ? [t('final'), 'star'] : [t('ahead'), null];
    }
  }

  private defeatedWeekday(week: CampaignWeek): string {
    return week.defeatedAt ? this.weekday(new Date(week.defeatedAt)) : '';
  }

  private report(
    week: CampaignWeek,
    state: PlanetState,
    final: boolean,
    campaign: CampaignModel,
  ): PlanetReport {
    const hitPointsLeft = Math.max(0, week.guardianHitPoints - week.damageDealt);
    if (state === 'now') {
      return {
        kind: 'now',
        hitPointsLeft,
        hitPoints: week.guardianHitPoints,
        breachPercent: week.progressPercent,
        daysLeft: Math.max(0, 6 - daysBetween(week.weekStart, campaign.today)),
        wounded: week.woundedCount,
        tonight: campaign.forecast?.rescued ?? 0,
      };
    }
    if (state === 'ahead') {
      return { kind: 'ahead', final, unplayed: campaign.status === 'CLOSED' };
    }
    return {
      kind: 'settled',
      defeated: week.defeated,
      defeatedWeekday: this.defeatedWeekday(week).toLowerCase(),
      defeatedBy:
        resourceValue(this.playersApi.players, []).find(
          (player) => player.id === week.defeatedByPlayerId,
        )?.displayName ?? null,
      hitPoints: week.guardianHitPoints,
      hitPointsLeft,
      breachPercent: week.progressPercent,
      rescued: week.challengeRescued + week.extractionRescued,
      spotted: week.woundedCount,
      byChallenges: week.challengeRescued,
      limiter: week.limiter,
      population: week.base?.population ?? null,
      populationChange: week.base?.populationChange ?? 0,
      baseLoss: week.baseLoss,
    };
  }
}
