import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
import {
  LucideCheck,
  LucideHistory,
  LucideLock,
  LucideMap,
  LucideSwords,
  LucideX,
} from '@lucide/angular';

import { BossCampaign } from '@core/boss/boss-campaign';
import { resolveBossTimelineTier } from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { PageHeader } from '@shared/page-header/page-header';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { TOOLTIP_SURFACE_CLASS } from '@shared/tooltip/tooltip.constants';
import { BossDetail } from './boss-detail/boss-detail';
import {
  LEAD_TERRAIN_ROWS,
  LEGEND_INNER_SCALE,
  MAP_COLUMNS,
  MAP_LEGEND_STATUSES,
  resolveBossColumn,
  resolveBossTerritoryTier,
  resolveColumnVisibilityClass,
  TERRAIN_FILL_CLASS,
  TERRAIN_RING_CLASS,
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
}

/**
 * Campaign page: the group's run of weekly boss confrontations, as territory to take.
 *
 * A honeycomb of hexagons scrolling top to bottom, one row per week. Each row hands one hexagon to
 * its week — placed along a serpentine path, see `resolveBossColumn` — and leaves the rest as
 * terrain, tinted by whether the front has passed it. The map opens on the week being fought, and
 * picking any week's hexagon opens the same detail panel the battle history view uses.
 *
 * A button in the header swaps the map for that battle history in place — every fought week told
 * as a chronology rather than as territory — and swaps back the same way, so the page never
 * navigates away from itself to tell the same campaign two ways (see `view`).
 */
@Component({
  selector: 'app-campaign',
  imports: [
    TranslatePipe,
    BossDetail,
    ResourceState,
    SectionDivider,
    NgOptimizedImage,
    Avatar,
    LucideCheck,
    LucideHistory,
    LucideLock,
    LucideMap,
    LucideSwords,
    LucideX,
    PageHeader,
  ],
  templateUrl: './campaign.html',
  host: { class: PAGE_LAYOUT_CLASS },
  providers: [BossCampaign],
})
export class Campaign {
  /**
   * The campaign itself: every week resolved into a display-ready node, plus the loading and error
   * state of the resources behind them.
   */
  protected readonly campaign = inject(BossCampaign);

  /**
   * Host element, queried once to scroll the week being fought into view on load.
   */
  private readonly hostElement = inject(ElementRef<HTMLElement>);

  /**
   * Id of the node whose detail panel is open, or `null` while the panel is closed.
   */
  private readonly selectedNodeId = signal<string | null>(null);

  /**
   * Whether the active week's hexagon has already been scrolled into view, so the one-time
   * auto-scroll effect never fires twice for the same load.
   */
  private hasScrolledToCurrentNode = false;

  /**
   * Whether the active week's timeline marker has already been scrolled into view, so switching
   * into the history view auto-scrolls only the first time.
   */
  private hasScrolledToHistoryCurrentNode = false;

  /**
   * Which of the two tellings of the campaign is on screen: the battle map (territory) or the
   * battle history (chronology). Swapped in place by the header button — see `toggleView` —
   * rather than by navigating to a separate route, since both read off the same `campaign`.
   */
  protected readonly view = signal<'map' | 'history'>('map');

  /**
   * The map, row by row, oldest week at the top, framed above and below by terrain the campaign
   * has not reached — the field extends past both ends of the path it carves through it.
   *
   * Empty while the campaign itself is, so an unresolved page shows no field at all rather than
   * bare terrain rows with nothing to fight over.
   */
  protected readonly rows = computed<readonly CampaignMapRow[]>(() => {
    const nodes = this.campaign.nodes();
    if (nodes.length === 0) {
      return [];
    }

    return [
      ...Array.from({ length: LEAD_TERRAIN_ROWS }, (_, index) => ({
        key: `lead-${index}`,
        node: null,
        bossColumn: -1,
      })),
      ...nodes.map((node, index) => ({
        key: node.id,
        node,
        bossColumn: resolveBossColumn(index),
      })),
      ...Array.from({ length: TRAIL_TERRAIN_ROWS }, (_, index) => ({
        key: `trail-${index}`,
        node: null,
        bossColumn: -1,
      })),
    ];
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
  protected readonly timelineTier = resolveBossTimelineTier;
  protected readonly columnVisibilityClass = resolveColumnVisibilityClass;
  protected readonly terrainRingClass = TERRAIN_RING_CLASS;
  protected readonly terrainFillClass = TERRAIN_FILL_CLASS;
  protected readonly tileInnerScale = TILE_INNER_SCALE;
  protected readonly legendInnerScale = LEGEND_INNER_SCALE;

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
   * Placeholder line widths driving the history view's loading skeleton.
   */
  protected readonly historySkeletonRows = SKELETON_ROWS;

  /**
   * Centers the map on the week being fought once, the first time it finishes loading — on a long
   * campaign the front is far down the page, and it is the only row worth opening on.
   *
   * `block: 'center'` rather than the legacy timeline's `'nearest'`: a map row is a single hexagon
   * tall, so centering it costs far less scroll than centering a timeline panel did and leaves the
   * ground on both sides of the front visible, which is the whole point of the shot.
   * `requestAnimationFrame` is a browser-only API, safe to call unconditionally here since this
   * effect only ever runs client-side, after `isLoading` first turns false.
   */
  constructor() {
    effect(() => {
      if (this.hasScrolledToCurrentNode || this.campaign.isLoading()) {
        return;
      }

      this.hasScrolledToCurrentNode = true;
      requestAnimationFrame(() => {
        this.hostElement.nativeElement
          .querySelector('[data-battlemap-current]')
          ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
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
   * Swaps the map for the battle history, or back, in place.
   *
   * The first time the history comes on screen, its active week's marker is scrolled into view
   * the same way the map centers on its own active hexagon on load — `requestAnimationFrame` is a
   * browser-only API, safe to call unconditionally here since this only ever runs client-side, in
   * response to a click.
   */
  protected toggleView(): void {
    const next = this.view() === 'map' ? 'history' : 'map';
    this.view.set(next);

    if (next !== 'history' || this.hasScrolledToHistoryCurrentNode) {
      return;
    }

    this.hasScrolledToHistoryCurrentNode = true;
    requestAnimationFrame(() => {
      this.hostElement.nativeElement
        .querySelector('[data-timeline-current]')
        ?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
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
   * Reloads every backing resource after a failure.
   */
  protected reload(): void {
    this.hasScrolledToCurrentNode = false;
    this.hasScrolledToHistoryCurrentNode = false;
    this.campaign.reload();
  }
}
