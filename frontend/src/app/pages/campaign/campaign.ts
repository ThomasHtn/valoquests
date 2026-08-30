import { NgOptimizedImage, NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideArrowDown,
  LucideCheck,
  LucideChevronDown,
  LucideGauge,
  LucideHammer,
  LucideLock,
  LucideMagnet,
  LucidePackage,
  LucideSkull,
  LucideSwords,
  LucideTrendingUp,
  LucideUserCheck,
  LucideUsers,
  LucideWheat,
  LucideX,
} from '@lucide/angular';

import { BossCampaign } from '@core/boss/boss-campaign';
import {
  BossTimelineNodeStatus,
  LEGEND_INNER_SCALE,
  resolveBossTerritoryTier,
  TERRAIN_RING_CLASS,
  TERRITORY_FILL_CLASS,
  TILE_INNER_SCALE,
} from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import {
  resolveBossCategoryColorClass,
  resolveBossNumberLabel,
} from '@core/boss/boss-visual.utils';
import { BossCategory } from '@core/boss/boss.model';
import { ColonyView } from '@core/colony/colony-view';
import { ColonyBossView } from '@core/colony/colony-view.model';
import { resolveColonyDeltaColorClass } from '@core/colony/colony-visual.utils';
import { Breakpoint } from '@core/viewport/breakpoint';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ColonyCard, ColonyCardFormulaRow } from './colony-card/colony-card';
import { PageHeader } from '@layout/page-header/page-header';
import { BarChart } from '@shared/chart/bar-chart';
import { resolveSeriesColor } from '@shared/chart/chart-theme';
import { ChartBar, ChartSeries } from '@shared/chart/chart.model';
import { LineChart } from '@shared/chart/line-chart';
import { ResourceState } from '@shared/resource-state/resource-state';
import { TOOLTIP_SURFACE_CLASS } from '@shared/tooltip/tooltip.constants';
import { BossDetail } from './boss-detail/boss-detail';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * One week's territory tile, joined with what its fight is worth the colony — the row's job is to
 * place a week beside its neighbours, not to restate the colony's own view model field by field.
 */
interface CampaignMapEntry {
  readonly node: BossTimelineNode;
  readonly boss: ColonyBossView | null;
}

/**
 * Bare terrain tiles framing the row of territories at each end, echoing the earlier serpentine
 * map's own field extending past both ends of its path — just horizontal now, and one tile deep
 * rather than a whole row.
 */
const LEAD_TERRAIN_TILES = 1;
const TRAIL_TERRAIN_TILES = 1;

/**
 * Campaign page: the run told as one screen, at rest — the card of its ten weeks, the population
 * curve that card feeds, the economy behind it folded away, then every campaign before this one.
 *
 * The card's own territory tiles (ring, halo, damage fill, outcome icon) are the earlier
 * serpentine honeycomb map's, unchanged — only the path is gone, the ten weeks now standing in one
 * straight row rather than snaking down the page. The boss drawer opens on a tile click and stands
 * closed the rest of the time, never pre-rendered open under the row — showing both a resting page
 * and an open drawer at once only read as clutter, twice over.
 */
@Component({
  selector: 'app-campaign',
  imports: [
    TranslatePipe,
    BossDetail,
    ResourceState,
    NgOptimizedImage,
    ColonyCard,
    BarChart,
    LineChart,
    NgTemplateOutlet,
    RouterLink,
    LucideArrowDown,
    LucideCheck,
    LucideChevronDown,
    LucideGauge,
    LucideHammer,
    LucideLock,
    LucideMagnet,
    LucidePackage,
    LucideSkull,
    LucideSwords,
    LucideTrendingUp,
    LucideUserCheck,
    LucideUsers,
    LucideWheat,
    LucideX,
    PageHeader,
  ],
  templateUrl: './campaign.html',
  host: { class: PAGE_LAYOUT_CLASS },
  providers: [BossCampaign, ColonyView],
})
export class Campaign {
  /**
   * The campaign itself: every week resolved into a display-ready node, plus the loading and error
   * state of the resources behind them.
   */
  protected readonly campaign = inject(BossCampaign);

  /**
   * The colony the campaign feeds, resolved into display-ready view models.
   */
  protected readonly colony = inject(ColonyView);

  /**
   * Names the plotted line, the one label the page resolves itself rather than reading off a view
   * model.
   */
  private readonly translation = inject(Translation);

  /**
   * Week asked for in the URL as `?week=YYYY-MM-DD`, or `null` when the page was opened plain.
   *
   * Read once from the snapshot rather than followed reactively, the same restraint
   * `Leaderboard.requestedWeekStart` takes: this is a deep link from a closed week's own row on
   * `/leaderboard`, and the reader's own clicks take over from the moment they touch a node.
   */
  private readonly requestedWeekStart = inject(ActivatedRoute).snapshot.queryParamMap.get('week');

  /**
   * Id of the node whose detail panel is open, `null` while explicitly closed, or `undefined`
   * while the reader has not touched a node yet — the card's resting state, and the one a
   * reviewer should judge as the page's normal look. Left `undefined` rather than defaulting to
   * `null` outright so {@link selectedNode} can still fall back to {@link requestedWeekStart} once,
   * without a click or a close ever being undone by that same fallback.
   */
  private readonly selectedNodeId = signal<string | null | undefined>(undefined);

  /**
   * Whether the viewport is wide enough to lay the resource tiles out as a grid.
   *
   * Below `lg` they stack into a single column, and ten of them in a row put the map ten screens
   * down — see {@link isDetailOpen}. From `lg` up the fold does not exist at all: the wrapper it
   * hangs on is `display: contents` there, so every tile lands in the page grid unchanged.
   */
  protected readonly isLarge = inject(Breakpoint).isLarge;

  /**
   * Whether the colony's eight explanatory tiles are unfolded, on the narrow layout where they are
   * folded away by default.
   *
   * Only the population and the materials still owed to the next tier stand outside the fold:
   * those two are the run's standing, and the rest is how it got there.
   */
  protected readonly isDetailOpen = signal(false);

  /**
   * Whether either half of the page is still resolving, or has failed. The two are reported as one:
   * the colony and the campaign are the same run, and half a run on screen reads as a bug.
   */
  protected readonly isLoading = computed(
    () => this.campaign.isLoading() || this.colony.isLoading(),
  );
  protected readonly hasError = computed(() => this.campaign.hasError() || this.colony.hasError());

  /**
   * The ten weeks of the card's territory row, oldest first, each joined with what its fight is
   * worth the colony. The two are the same ten weeks, joined on the run week each of them carries
   * — never on their position in the list, which a week that closed without a fight would shift.
   */
  protected readonly nodesWithBoss = computed<readonly CampaignMapEntry[]>(() => {
    const bossByRunWeek = new Map(this.colony.bosses().map((boss) => [boss.weekIndex, boss]));

    return this.campaign.nodes().map((node) => ({
      node,
      boss: bossByRunWeek.get(node.runWeekIndex) ?? null,
    }));
  });

  /**
   * The run's population as one plotted line, day by day.
   *
   * A line rather than a column per day: the curve is read for its slope — a step up is a building
   * going up, a sag is a quiet week — and a run is seventy-one days long, which is more bars than a
   * panel this size can seat.
   */
  protected readonly curveSeries = computed<readonly ChartSeries[]>(() => {
    const curve = this.colony.curve();
    if (curve.length === 0) {
      return [];
    }

    return [
      {
        label: this.translation.translate('colony.population'),
        color: resolveSeriesColor(0),
        points: curve.map((bar) => bar.value),
      },
    ];
  });

  /**
   * The week's harvest as one bar per day of the food window: a placeholder slot (a day not lived
   * yet) is drawn recessive at zero, and today's own bar is highlighted since it is still the one
   * closing at tonight's rollover.
   */
  protected readonly foodWeekBars = computed<readonly ChartBar[]>(() =>
    this.colony.foodDays().map((day) => ({
      label: day.weekdayInitial,
      value: day.harvestValue ?? 0,
      valueLabel: day.harvestLabel,
      detail: day.ariaLabel,
      highlighted: day.isToday,
      muted: day.isPlaceholder,
    })),
  );

  /**
   * Sr-only prose standing in for the food bars, each day's own accessible label read in sequence
   * — the canvas is one opaque image to assistive technology.
   */
  protected readonly foodWeekSummary = computed<string>(() =>
    this.colony
      .foodDays()
      .map((day) => day.ariaLabel)
      .join(', '),
  );

  /**
   * The most recent tier the run has reached, the one line under the curve's own caption worth
   * repeating permanently (design-review.md's mock-up keeps only the latest milestone in that
   * caption; the full list stays available below it for every step the run took).
   */
  protected readonly latestMilestoneLabel = computed(() => {
    const milestones = this.colony.milestoneLabels();
    return milestones.length > 0 ? milestones[milestones.length - 1] : null;
  });

  /**
   * The Food-this-week tile's own formula rows: consumption and efficiency, read a second time as
   * the tile's own bubble.
   */
  protected readonly foodWeekFormulaRows = computed<readonly ColonyCardFormulaRow[]>(() => {
    const ring = this.colony.foodRing();
    if (ring === null) {
      return [];
    }

    return [
      {
        label: this.translation.translate('colony.track.food.consumption'),
        value: ring.consumptionLabel,
      },
      {
        label: this.translation.translate('colony.track.food.efficiency'),
        value: ring.efficiencyLabel,
      },
    ];
  });

  /**
   * The Participation tile's own formula row: tonight's turnout multiplier, the same bonus row
   * the presence hover card already states.
   */
  protected readonly participationFormulaRows = computed<readonly ColonyCardFormulaRow[]>(() => {
    const battery = this.colony.battery();
    if (battery === null) {
      return [];
    }

    return [
      {
        label: this.translation.translate('colony.track.presence.bonus'),
        value: battery.multiplierLabel,
      },
    ];
  });

  /**
   * The node whose detail panel is open, or `null` while the panel is closed.
   *
   * Falls back to {@link requestedWeekStart} only while {@link selectedNodeId} is still
   * `undefined` — a deep link opens the matching node once the campaign's nodes have loaded, but
   * never fights back a reader's own click or close.
   */
  protected readonly selectedNode = computed<BossTimelineNode | null>(() => {
    const nodes = this.campaign.nodes();
    const selected = this.selectedNodeId();

    if (selected !== undefined) {
      return nodes.find((node) => node.id === selected) ?? null;
    }

    return this.requestedWeekStart === null
      ? null
      : (nodes.find((node) => node.weekStart === this.requestedWeekStart) ?? null);
  });

  /**
   * What {@link selectedNode}'s fight is worth the colony, joined on the run week — never on the
   * node's position in the list, which a week that closed without a fight would shift.
   */
  protected readonly selectedColonyBoss = computed(() => {
    const node = this.selectedNode();
    if (node === null) {
      return null;
    }

    return this.colony.bosses().find((boss) => boss.weekIndex === node.runWeekIndex) ?? null;
  });

  /**
   * Position of {@link selectedNode} within the campaign's nodes, or `-1` while the panel is
   * closed.
   */
  private readonly selectedIndex = computed(() => {
    const selectedId = this.selectedNode()?.id;
    return selectedId === undefined
      ? -1
      : this.campaign.nodes().findIndex((node) => node.id === selectedId);
  });

  /**
   * Whether the panel can step to an earlier / later week without leaving the campaign.
   */
  protected readonly hasPreviousNode = computed(() => this.selectedIndex() > 0);
  protected readonly hasNextNode = computed(() => {
    const index = this.selectedIndex();
    return index >= 0 && index < this.campaign.nodes().length - 1;
  });

  /**
   * Boss numbering, exposed to the template.
   */
  protected readonly bossNumberLabel = resolveBossNumberLabel;

  /**
   * Resolves a week's territory treatment, exposed to the template.
   */
  protected readonly territoryTier = resolveBossTerritoryTier;

  /**
   * Geometry and ground treatments the territory row shares with its legend, exposed to the
   * template.
   */
  protected readonly tileInnerScale = TILE_INNER_SCALE;
  protected readonly legendInnerScale = LEGEND_INNER_SCALE;
  protected readonly territoryFillClass = TERRITORY_FILL_CLASS;
  protected readonly terrainRingClass = TERRAIN_RING_CLASS;

  /**
   * Silhouette the hover card borrows from the sidebar's tooltips, so a surface floating over the
   * page reads the same wherever it comes from.
   */
  protected readonly tooltipSurfaceClass = TOOLTIP_SURFACE_CLASS;

  /**
   * The four statuses the legend spells out, the two settled outcomes first, then the fight in
   * progress and the ground beyond it.
   */
  protected readonly legendStatuses: readonly BossTimelineNodeStatus[] = [
    'defeated',
    'survived',
    'current',
    'upcoming',
  ];

  /**
   * Bare terrain tiles framing the row at each end, as lists the template can iterate.
   */
  protected readonly leadTerrainTiles: readonly number[] = Array.from(
    { length: LEAD_TERRAIN_TILES },
    (_, index) => index,
  );
  protected readonly trailTerrainTiles: readonly number[] = Array.from(
    { length: TRAIL_TERRAIN_TILES },
    (_, index) => index,
  );

  /**
   * Text color of the Arrivals tile's own figure, by the direction the night moved.
   */
  protected readonly deltaColorClass = resolveColonyDeltaColorClass;

  /**
   * Opens the detail panel on one week.
   *
   * @param node - The week to detail.
   */
  protected select(node: BossTimelineNode): void {
    this.selectedNodeId.set(node.id);
  }

  /**
   * Steps the open panel to the adjacent week, if there is one in that direction.
   *
   * @param offset - `-1` for the previous week, `1` for the next one.
   */
  protected step(offset: -1 | 1): void {
    const target = this.campaign.nodes()[this.selectedIndex() + offset];
    if (target) {
      this.selectedNodeId.set(target.id);
    }
  }

  /**
   * Closes the detail panel.
   */
  protected closePanel(): void {
    this.selectedNodeId.set(null);
  }

  /**
   * Reloads every backing resource after a failure, on both halves of the page.
   */
  protected reload(): void {
    this.campaign.reload();
    this.colony.reload();
  }

  /**
   * Unfolds the colony's explanatory tiles on the narrow layout, and folds them back.
   */
  protected toggleDetail(): void {
    this.isDetailOpen.update((isOpen) => !isOpen);
  }

  /**
   * Icon color for the week currently being fought: the boss's own difficulty once it's drawn,
   * rather than the tier's flat amber, so the one mark that isn't an outcome yet still says
   * something about the fight. The hexagon's ring stays the outcome's alone (see
   * {@link resolveBossTerritoryTier}) — this only ever touches the glyph riding on top of it.
   *
   * @param category - The active week's boss category, or `null` for the one tick before it's drawn.
   * @returns The Tailwind text color utility to apply to the boss icon.
   */
  protected currentBossIconColorClass(category: BossCategory | null): string {
    return category === null
      ? this.territoryTier('current').iconClass
      : resolveBossCategoryColorClass(category);
  }
}
