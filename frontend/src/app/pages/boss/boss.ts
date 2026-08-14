import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronLeft } from '@lucide/angular';

import { BossCampaign } from '@core/boss/boss-campaign';
import { resolveBossTimelineTier } from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { BossDetail } from '@pages/campaign/boss-detail/boss-detail';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Battle history: the group's run of weekly boss confrontations fought so far.
 *
 * A centered vertical timeline from the oldest finalized week to the active one, auto-scrolled to
 * the current week's marker on load. Weeks not yet fought are left off — this page tells what
 * happened, not what's ahead, which is the battle map's (`Campaign`) job. Selecting any node opens
 * a detail panel breaking that week's damage down per player. It renders {@link BossCampaign} —
 * the same nodes the map does, minus the locked placeholders.
 */
@Component({
  selector: 'app-boss',
  imports: [
    TranslatePipe,
    BossDetail,
    ResourceState,
    SectionDivider,
    RouterLink,
    LucideChevronLeft,
  ],
  templateUrl: './boss.html',
  host: { class: PAGE_LAYOUT_CLASS },
  providers: [BossCampaign],
})
export class Boss {
  /**
   * The campaign itself: every week resolved into a display-ready node, plus the loading and error
   * state of the resources behind them.
   */
  protected readonly campaign = inject(BossCampaign);

  /**
   * Host element, queried once to scroll the current week's marker into view on load.
   */
  private readonly hostElement = inject(ElementRef<HTMLElement>);

  /**
   * Id of the node whose detail panel is open, or `null` while the panel is closed.
   */
  private readonly selectedNodeId = signal<string | null>(null);

  /**
   * Whether the current week's marker has already been scrolled into view, so the one-time
   * auto-scroll effect never fires twice for the same load.
   */
  private hasScrolledToCurrentNode = false;

  /**
   * The weeks this page tells: every fought week, oldest first, up to and including the active
   * one — the campaign's locked placeholders for weeks ahead are left out, since a history has
   * nothing to say about what hasn't happened yet.
   */
  protected readonly nodes = computed<readonly BossTimelineNode[]>(() =>
    this.campaign.nodes().filter((node) => node.status !== 'upcoming'),
  );

  /**
   * The node whose detail panel is open, or `null` while the panel is closed.
   */
  protected readonly selectedNode = computed<BossTimelineNode | null>(
    () => this.nodes().find((node) => node.id === this.selectedNodeId()) ?? null,
  );

  /**
   * Position of {@link selectedNode} within the history, or `-1` while the panel is closed.
   */
  private readonly selectedIndex = computed(() =>
    this.nodes().findIndex((node) => node.id === this.selectedNodeId()),
  );

  /**
   * Whether the panel can step to an earlier / later week without leaving the history.
   */
  protected readonly hasPreviousNode = computed(() => this.selectedIndex() > 0);
  protected readonly hasNextNode = computed(() => {
    const index = this.selectedIndex();
    return index >= 0 && index < this.nodes().length - 1;
  });

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Resolves a timeline node's visual tier (marker, panel tint, pill, bar), exposed to the
   * template.
   */
  protected readonly timelineTier = resolveBossTimelineTier;

  /**
   * Scrolls the current week's marker into view once, the first time the timeline finishes
   * loading — the page's one-time "you are here" cue on a potentially long campaign.
   *
   * `block: 'nearest'` rather than `'center'`: centering can require enough scroll to push the
   * page's own `<header>` half off-screen (clipped, not fully hidden) on short mobile viewports,
   * since it scrolls above the marker in the same document flow. `'nearest'` still scrolls when
   * the marker starts off-screen, just without overshooting past a fully-visible result.
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
          .querySelector('[data-timeline-current]')
          ?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      });
    });
  }

  /**
   * Background for one row's segment of the timeline's center line: ground already covered in brand
   * amber, turning red exactly at the active week's marker, then flat and muted ahead — the same
   * three colors the markers themselves use, in the same order.
   *
   * The line is drawn one segment per row rather than as a single gradient spanning the whole list
   * because rows do not share a height, so no percentage along the list maps to a marker's center
   * and the handover always landed short of the active hexagon. A marker is vertically centered in
   * its row, which puts that handover at a hard 50% of the row's own segment.
   *
   * @param index - Position of the row in the campaign.
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
   * Opens the detail panel on one week.
   *
   * @param node - The timeline node to detail.
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
    const target = this.nodes()[this.selectedIndex() + offset];
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
