import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCheck, LucideHistory, LucideLock, LucideSwords, LucideX } from '@lucide/angular';

import { BossCampaign } from '@core/boss/boss-campaign';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionDivider } from '@shared/section-divider/section-divider';
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
 * picking any week's hexagon opens the same detail panel the battle history (`Boss`) uses. A link
 * in the header opens that history, told as a chronology of fought weeks rather than as territory.
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
    RouterLink,
    LucideCheck,
    LucideHistory,
    LucideLock,
    LucideSwords,
    LucideX,
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
   * The node whose detail panel is open, or `null` while the panel is closed.
   */
  protected readonly selectedNode = computed<BossTimelineNode | null>(
    () => this.campaign.nodes().find((node) => node.id === this.selectedNodeId()) ?? null,
  );

  /**
   * Position of {@link selectedNode} within the campaign, or `-1` while the panel is closed.
   */
  private readonly selectedIndex = computed(() =>
    this.campaign.nodes().findIndex((node) => node.id === this.selectedNodeId()),
  );

  /**
   * Whether the panel can step to an earlier / later week without leaving the campaign.
   */
  protected readonly hasPreviousNode = computed(() => this.selectedIndex() > 0);
  protected readonly hasNextNode = computed(() => {
    const index = this.selectedIndex();
    return index >= 0 && index < this.campaign.nodes().length - 1;
  });

  /**
   * Grid geometry and treatments, exposed to the template.
   */
  protected readonly columns = MAP_COLUMNS;
  protected readonly legendStatuses = MAP_LEGEND_STATUSES;
  protected readonly territoryTier = resolveBossTerritoryTier;
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
   * Reloads every backing resource after a failure.
   */
  protected reload(): void {
    this.hasScrolledToCurrentNode = false;
    this.campaign.reload();
  }
}
