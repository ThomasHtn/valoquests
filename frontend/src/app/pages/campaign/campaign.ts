import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
import {
  LucideArrowDown,
  LucideBuilding2,
  LucideCheck,
  LucideGauge,
  LucideHammer,
  LucideHouse,
  LucideLandmark,
  LucideLock,
  LucideMagnet,
  LucidePackage,
  LucideSkull,
  LucideSwords,
  LucideTent,
  LucideTrendingUp,
  LucideUserCheck,
  LucideUsers,
  LucideWheat,
  LucideX,
} from '@lucide/angular';

import { BossCampaign } from '@core/boss/boss-campaign';
import { BossCategory } from '@core/boss/boss.model';
import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { resolveBossTimelineTier } from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { ColonyView } from '@core/colony/colony-view';
import {
  ColonyBossView,
  ColonyDeltaView,
  ColonyTierStepView,
} from '@core/colony/colony-view.model';
import { ColonyTierState } from '@core/colony/colony.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ColonyCard, ColonyCardFormulaRow } from './colony-card/colony-card';
import { PageHeader } from '@layout/page-header/page-header';
import { BarChart } from '@shared/chart/bar-chart';
import { resolveSeriesColor } from '@shared/chart/chart-theme';
import { ChartBar, ChartSeries } from '@shared/chart/chart.model';
import { LineChart } from '@shared/chart/line-chart';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';
import { Drawer } from '@shared/drawer/drawer';
import { ResourceState } from '@shared/resource-state/resource-state';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { Tooltip } from '@shared/tooltip/tooltip';
import { TOOLTIP_SURFACE_CLASS } from '@shared/tooltip/tooltip.constants';
import { BossDetail } from './boss-detail/boss-detail';
import {
  LEAD_TERRAIN_ROWS,
  LEGEND_INNER_SCALE,
  MAP_COLUMNS,
  MAP_LEGEND_STATUSES,
  resolveBossColumn,
  resolveBossNumberLabel,
  resolveBossTerritoryTier,
  resolveColumnVisibilityClass,
  TERRAIN_RING_CLASS,
  TERRITORY_FILL_CLASS,
  TILE_INNER_SCALE,
  TRAIL_TERRAIN_ROWS,
} from './campaign-map.constants';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Number of hexagon rows the loading skeleton stands in for, enough to fill the fold without
 * pretending to know how long the campaign actually is.
 */
const SKELETON_ROW_COUNT = 8;

/**
 * Rows the run history stands at, real runs and empty berths together.
 *
 * The ledger grows one row per run and never shrinks, so it is drawn at a fixed height from the
 * first run on: a table that resizes under the reader every ten weeks reads as a different table
 * each time.
 */
const HISTORY_ROW_COUNT = 6;

/**
 * Treatments one step of the town's ladder is drawn in.
 *
 * The marker is a hexagon like every other beat of the campaign — the map's territories, the
 * history's week markers — so the ladder reads as the same clock as the rest of the page.
 *
 * Four treatments for three backend states: the step right above the town's own is drawn apart from
 * the rest of the locked ones, because it is the only row anything the squad does moves. Every row
 * carries a veil rather than an opaque plate, which is the surface the rest of the application uses.
 *
 * A filled marker means the step is paid for, an outlined one that it is not — which is what tells
 * the town's own step apart from the one it is climbing. Both wear the brand colour, since one is
 * where the town stands and the other where it is heading; everything further behind is green, and
 * everything further ahead grey.
 */
const LADDER_STEPS: Record<
  ColonyTierState | 'NEXT',
  {
    /**
     * The row's own veil, which is what separates the step being climbed from the rest.
     */
    readonly rowClass: string;

    /**
     * Outline of the marker's hexagon.
     */
    readonly markerClass: string;

    /**
     * Its surface. A step already crossed is a solid hexagon carrying a dark mark; one that is not
     * is an outline around the page's own ground. That difference in *fill*, rather than in hue
     * alone, is what makes a crossed step readable at a glance — a coloured outline beside a
     * coloured outline reads as two shades of one state.
     */
    readonly markerFillClass: string;
    readonly markerIconClass: string;
    readonly nameClass: string;
    readonly valueClass: string;
  }
> = {
  REACHED: {
    rowClass: 'bg-text-primary/10',
    markerClass: 'bg-accent-green',
    markerFillClass: 'bg-accent-green',
    markerIconClass: 'text-surface-950',
    nameClass: 'text-text-secondary',
    valueClass: 'text-text-secondary',
  },
  // The step the town stands in: paid for, so a solid marker like the crossed ones behind it, and
  // brand rather than green because it is the name the town currently answers to. It carries no
  // outline and no bar — both belong to the row below, which is the one still to open.
  CURRENT: {
    rowClass: 'bg-text-primary/10',
    markerClass: 'bg-brand-500',
    markerFillClass: 'bg-brand-500',
    markerIconClass: 'text-surface-950',
    nameClass: 'font-bold text-text-primary',
    valueClass: 'text-text-secondary',
  },
  // The step being climbed: the panel's active row, outlined and lit, carrying both the cost still
  // to gather and the bar that measures it.
  NEXT: {
    rowClass: 'bg-brand-500/12 outline-1 -outline-offset-1 outline-brand-500/40',
    markerClass: 'bg-brand-500',
    markerFillClass: 'bg-surface-950',
    markerIconClass: 'text-brand-500',
    nameClass: 'font-semibold text-text-primary',
    valueClass: 'text-brand-500',
  },
  LOCKED: {
    rowClass: 'bg-text-primary/6',
    markerClass: 'bg-surface-600',
    markerFillClass: 'bg-surface-950',
    markerIconClass: 'text-text-muted',
    nameClass: 'text-text-muted',
    valueClass: 'text-text-muted',
  },
};

/**
 * One step of the town's ladder, paired with the treatment it is drawn in.
 */
interface CampaignLadderStep {
  readonly view: ColonyTierStepView;
  readonly tier: (typeof LADDER_STEPS)[ColonyTierState | 'NEXT'];
}

/**
 * One row of the battle map.
 */
interface CampaignMapRow {
  /**
   * Stable identity for the `@for` track expression.
   */
  readonly key: string;

  /**
   * The week this row renders, or `null` for one of the terrain-only rows framing the path (see
   * `LEAD_TERRAIN_ROWS`), which carry no week and are hidden from assistive technology.
   */
  readonly node: BossTimelineNode | null;

  /**
   * Column of {@link MAP_COLUMNS} the week's hexagon occupies; every other column is terrain.
   * Irrelevant, and set past the grid, on a terrain-only row.
   */
  readonly bossColumn: number;

  /**
   * The whole of what the week's fight is worth, or `null` on a terrain-only row.
   *
   * The tile writes the materials off it, the hover card writes materials and morale both. Held as
   * one object rather than flattened into the row: the row's job is to place a week on the grid, not
   * to restate the colony's own view model field by field.
   */
  readonly boss: ColonyBossView | null;
}

/**
 * Campaign page: the run told as one screen — the colony it feeds, and the ground it takes.
 *
 * The two used to be separate pages, which split one run in half: the town closes part of the gap
 * between what it holds and the lower of its two ceilings every night, and one of those ceilings is
 * bought with the materials the weekly bosses drop. So the page is laid out as the chain it is. The
 * resource band on top carries the population, the three rails that set it and every run before this
 * one; the map underneath is where the housing comes from, each taken week's gain written on its own
 * hexagon; the ladder beside it is what that housing bought. The population's own history is the one
 * reading held back — opened over the page from the hexagon carrying its current value.
 *
 * Nothing on the band says what to do tonight, on purpose. A short bar asks to be filled, and the
 * shape of the food rail against the mark on the hexagon already answers "play, or build" without a
 * sentence being written.
 *
 * The map is a honeycomb standing whole in its panel, one row per week between a row of untouched
 * terrain at each end. Picking any week's hexagon opens the detail panel, and the panel's own title
 * is a dropdown swapping the map for the same campaign told as a chronology — the one reading that
 * survives on a narrow screen, where the field is at its most cramped (see `view`).
 */
@Component({
  selector: 'app-campaign',
  imports: [
    TranslatePipe,
    BossDetail,
    ResourceState,
    NgOptimizedImage,
    ColonyCard,
    Drawer,
    BarChart,
    LineChart,
    ProgressBar,
    ProgressCircle,
    Select,
    Tooltip,
    LucideArrowDown,
    LucideBuilding2,
    LucideCheck,
    LucideGauge,
    LucideHammer,
    LucideHouse,
    LucideLandmark,
    LucideLock,
    LucideMagnet,
    LucidePackage,
    LucideSkull,
    LucideSwords,
    LucideTent,
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
   * Host element, queried once to scroll the week being fought into view on load.
   */
  private readonly hostElement = inject(ElementRef<HTMLElement>);

  /**
   * Names the plotted line, the one label the page resolves itself rather than reading off a view
   * model.
   */
  private readonly translation = inject(Translation);

  /**
   * Id of the node whose detail panel is open, or `null` while the panel is closed.
   */
  private readonly selectedNodeId = signal<string | null>(null);

  /**
   * Whether the population curve is open over the page.
   *
   * The curve answers a different question from the rest of the page — where the population has
   * been, rather than where it stands — and it is the only block here that nothing else is read
   * against, so it is opened from the figure it explains instead of holding a panel of its own.
   */
  protected readonly isCurveOpen = signal(false);

  /**
   * Whether the active week's timeline marker has already been scrolled into view, so switching
   * into the history view auto-scrolls only the first time.
   */
  private hasScrolledToHistoryCurrentNode = false;

  /**
   * Which of the two tellings of the campaign is on screen: the battle map (territory) or the
   * battle history (chronology). Swapped in place by the dropdown the panel's title carries,
   * rather than by navigating to a separate route, since both read off the same `campaign`.
   */
  protected readonly view = signal<'map' | 'history'>('map');

  /**
   * The two tellings, as the dropdown's options. The panel is titled by whichever is on screen, so
   * the title and the control that changes it are the same element.
   */
  protected readonly viewOptions = computed<readonly SelectOption<'map' | 'history'>[]>(() => [
    { value: 'map', label: this.translation.translate('campaign.territory') },
    { value: 'history', label: this.translation.translate('campaign.historyToggle') },
  ]);

  /**
   * Whether either half of the page is still resolving, or has failed. The two are reported as one:
   * the colony and the campaign are the same run, and half a run on screen reads as a bug.
   */
  protected readonly isLoading = computed(
    () => this.campaign.isLoading() || this.colony.isLoading(),
  );
  protected readonly hasError = computed(() => this.campaign.hasError() || this.colony.hasError());

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
   * The Food-this-week tile's own formula rows: the same consumption and efficiency figures
   * `ColonyResourceBand`'s own food hover card states, read a second time as the tile's own bubble
   * now that the tile stands apart from the band.
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
   * The map, row by row, oldest week at the top, framed above and below by terrain the campaign
   * has not reached — the field extends past both ends of the path it carves through it.
   *
   * Every week's tile carries what its fight brought the colony in. The two lists are the same ten
   * weeks, joined on the run week each of them carries — never on their position in the list. A
   * single week that closed without a fight makes those two disagree, and joining by position then
   * wrote every fight's reward onto its neighbour's hexagon.
   *
   * Empty while the campaign itself is, so an unresolved page shows no field at all rather than
   * bare terrain rows with nothing to fight over.
   */
  protected readonly rows = computed<readonly CampaignMapRow[]>(() => {
    const nodes = this.campaign.nodes();
    if (nodes.length === 0) {
      return [];
    }

    const bossByRunWeek = new Map(this.colony.bosses().map((boss) => [boss.weekIndex, boss]));

    return [
      ...Array.from({ length: LEAD_TERRAIN_ROWS }, (_, index) => ({
        key: `lead-${index}`,
        node: null,
        bossColumn: -1,
        boss: null,
      })),
      ...nodes.map((node, index) => {
        const boss = bossByRunWeek.get(node.runWeekIndex) ?? null;

        return {
          key: node.id,
          node,
          bossColumn: resolveBossColumn(index),
          boss,
        };
      }),
      ...Array.from({ length: TRAIL_TERRAIN_ROWS }, (_, index) => ({
        key: `trail-${index}`,
        node: null,
        bossColumn: -1,
        boss: null,
      })),
    ];
  });

  /**
   * The ladder as a window around the town's own step, lowest first.
   *
   * A window rather than the whole ladder, because the ladder has no end: a squad that keeps
   * building always has a next name to climb towards, and a panel this narrow can only ever show the
   * neighbourhood of the one it stands in.
   */
  protected readonly ladderSteps = computed<readonly CampaignLadderStep[]>(() =>
    this.colony
      .ladder()
      .map((view) => ({ view, tier: LADDER_STEPS[view.isNext ? 'NEXT' : view.state] })),
  );

  /**
   * The weeks the history view tells: every fought week, oldest first, up to and including the
   * active one — the campaign's locked placeholders for weeks ahead are left out, since a history
   * has nothing to say about what hasn't happened yet.
   */
  protected readonly historyNodes = computed<readonly BossTimelineNode[]>(() =>
    this.campaign.nodes().filter((node) => node.status !== 'upcoming'),
  );

  /**
   * The node list the detail panel steps through, which depends on which view opened it: the full
   * campaign (locked weeks included) from the map, or the fought-only {@link historyNodes} from
   * the history view.
   */
  private readonly activeNodes = computed<readonly BossTimelineNode[]>(() =>
    this.view() === 'history' ? this.historyNodes() : this.campaign.nodes(),
  );

  /**
   * The node whose detail panel is open, or `null` while the panel is closed.
   */
  protected readonly selectedNode = computed<BossTimelineNode | null>(
    () => this.activeNodes().find((node) => node.id === this.selectedNodeId()) ?? null,
  );

  /**
   * Position of {@link selectedNode} within {@link activeNodes}, or `-1` while the panel is
   * closed.
   */
  private readonly selectedIndex = computed(() =>
    this.activeNodes().findIndex((node) => node.id === this.selectedNodeId()),
  );

  /**
   * Whether the panel can step to an earlier / later week without leaving the campaign.
   */
  protected readonly hasPreviousNode = computed(() => this.selectedIndex() > 0);
  protected readonly hasNextNode = computed(() => {
    const index = this.selectedIndex();
    return index >= 0 && index < this.activeNodes().length - 1;
  });

  /**
   * Grid geometry and treatments, exposed to the template.
   */
  protected readonly columns = MAP_COLUMNS;
  protected readonly legendStatuses = MAP_LEGEND_STATUSES;
  protected readonly territoryTier = resolveBossTerritoryTier;
  protected readonly bossNumberLabel = resolveBossNumberLabel;
  protected readonly timelineTier = resolveBossTimelineTier;
  protected readonly columnVisibilityClass = resolveColumnVisibilityClass;
  protected readonly terrainRingClass = TERRAIN_RING_CLASS;
  protected readonly tileInnerScale = TILE_INNER_SCALE;
  protected readonly legendInnerScale = LEGEND_INNER_SCALE;
  protected readonly territoryFillClass = TERRITORY_FILL_CLASS;

  /**
   * Silhouette the hover card borrows from the sidebar's tooltips, so a surface floating over the
   * page reads the same wherever it comes from.
   */
  protected readonly tooltipSurfaceClass = TOOLTIP_SURFACE_CLASS;

  /**
   * Rows the loading skeleton draws, as a list the template can iterate.
   */
  protected readonly skeletonRows = Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => index);

  /**
   * Empty berths padding the run history out to {@link HISTORY_ROW_COUNT}, so a ledger holding one
   * run still reads as a ledger. None once the runs themselves fill it.
   */
  protected readonly historyPlaceholders = computed<readonly number[]>(() =>
    Array.from(
      { length: Math.max(0, HISTORY_ROW_COUNT - this.colony.runs().length) },
      (_, index) => index,
    ),
  );

  /**
   * Brings the active week's marker into view the first time the battle history comes on screen.
   *
   * The map needs no equivalent: it stands whole in its panel, so there is nothing to scroll to.
   * The history does not — it is a column of panels as long as the run is. `requestAnimationFrame`
   * is a browser-only API, safe to call unconditionally here since this only ever runs client-side,
   * in response to the dropdown.
   */
  constructor() {
    effect(() => {
      if (this.view() !== 'history' || this.hasScrolledToHistoryCurrentNode) {
        return;
      }

      this.hasScrolledToHistoryCurrentNode = true;
      requestAnimationFrame(() => {
        this.hostElement.nativeElement
          .querySelector('[data-timeline-current]')
          ?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      });
    });
  }

  /**
   * Background for one row's segment of the history timeline's center line: ground already
   * covered in brand amber, turning red exactly at the active week's marker, then flat and muted
   * ahead — the same three colors the markers themselves use, in the same order.
   *
   * The line is drawn one segment per row rather than as a single gradient spanning the whole
   * list because rows do not share a height, so no percentage along the list maps to a marker's
   * center and the handover always landed short of the active hexagon. A marker is vertically
   * centered in its row, which puts that handover at a hard 50% of the row's own segment.
   *
   * @param index - Position of the row in {@link historyNodes}.
   * @returns The CSS `background` value for that row's segment.
   */
  protected timelineConnectorBackground(index: number): string {
    const currentIndex = this.campaign.currentNodeIndex();

    if (currentIndex < 0 || index > currentIndex) {
      return 'var(--color-surface-700)';
    }

    if (index < currentIndex) {
      return 'var(--color-brand-500)';
    }

    return (
      `linear-gradient(to bottom, var(--color-brand-500) 0%, var(--color-accent-red) 50%, ` +
      `var(--color-surface-700) 50%)`
    );
  }

  /**
   * Opens the population curve over the page, and closes it again.
   */
  protected openCurve(): void {
    this.isCurveOpen.set(true);
  }

  protected closeCurve(): void {
    this.isCurveOpen.set(false);
  }

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
    const target = this.activeNodes()[this.selectedIndex() + offset];
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
    this.hasScrolledToHistoryCurrentNode = false;
    this.campaign.reload();
    this.colony.reload();
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

  /**
   * Text color of the Arrivals tile's own figure, by the direction the night moved — the same
   * rule `ColonyResourceBand`'s own arrival mark is coloured by.
   *
   * @param delta - What the night moved.
   * @returns The colour utility.
   */
  protected deltaColorClass(delta: ColonyDeltaView): string {
    return delta.isPositive ? 'text-success' : delta.isNegative ? 'text-danger' : 'text-text-muted';
  }
}
