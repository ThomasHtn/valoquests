import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
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
import { BossCategory } from '@core/boss/boss.model';
import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { resolveBossTimelineTier } from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { ColonyView } from '@core/colony/colony-view';
import { ColonyBossView } from '@core/colony/colony-view.model';
import { resolveColonyDeltaColorClass } from '@core/colony/colony-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Breakpoint } from '@core/viewport/breakpoint';
import { ColonyCard, ColonyCardFormulaRow } from './colony-card/colony-card';
import { PageHeader } from '@layout/page-header/page-header';
import { BarChart } from '@shared/chart/bar-chart';
import { resolveSeriesColor } from '@shared/chart/chart-theme';
import { ChartBar, ChartSeries } from '@shared/chart/chart.model';
import { LineChart } from '@shared/chart/line-chart';
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
    Select,
    Tooltip,
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
   * Whether the viewport is wide enough to lay the tiles out as a grid.
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
   * Only the population and the materials still owed to the next tier stand outside the fold: those
   * two are the run's standing, and the rest is how it got there.
   */
  protected readonly isDetailOpen = signal(false);

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
      // The view model already spells the harvest in the reader's language; the raw number printed
      // an English decimal point above bars sitting under tiles that use a comma.
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
   * The week currently being fought, or `null` while no week is active.
   *
   * What used to only surface on hover, over the map tile being fought, now stands permanently
   * beside the map — see {@link currentColonyBoss} for the reward figures paired with it.
   */
  protected readonly currentBossNode = computed<BossTimelineNode | null>(() => {
    const index = this.campaign.currentNodeIndex();
    return index >= 0 ? this.campaign.nodes()[index] : null;
  });

  /**
   * What the week being fought is worth the colony: materials on the table, and what surviving it
   * would cost. `null` until {@link currentBossNode} resolves, joined on the run week exactly as the
   * map's own tiles are (see `rows`).
   */
  protected readonly currentColonyBoss = computed<ColonyBossView | null>(() => {
    const node = this.currentBossNode();
    if (node === null) {
      return null;
    }

    return this.colony.bosses().find((boss) => boss.weekIndex === node.runWeekIndex) ?? null;
  });

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
   * What {@link selectedNode}'s fight is worth the colony, joined on the run week exactly as
   * {@link currentColonyBoss} is — the panel reads the same figures whichever week it was opened
   * on, not only the one under way.
   */
  protected readonly selectedColonyBoss = computed<ColonyBossView | null>(() => {
    const node = this.selectedNode();
    if (node === null) {
      return null;
    }

    return this.colony.bosses().find((boss) => boss.weekIndex === node.runWeekIndex) ?? null;
  });

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
   * Text color of the Arrivals tile's own figure, by the direction the night moved.
   */
  protected readonly deltaColorClass = resolveColonyDeltaColorClass;

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
   * Unfolds the colony's explanatory tiles on the narrow layout, and folds them back.
   */
  protected toggleDetail(): void {
    this.isDetailOpen.update((isOpen) => !isOpen);
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
}
