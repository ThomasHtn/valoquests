import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { LucideChevronLeft, LucideChevronRight, LucideSkull } from '@lucide/angular';

import { resolveBossTimelineTier } from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { Drawer } from '@shared/drawer/drawer';
import { PositionBadge } from '@shared/position-badge/position-badge';

/**
 * Detail panel for one week of the campaign, opened from the battle map or the legacy timeline.
 *
 * Rendered inside the shared `app-drawer`, which owns the panel and its dismissal, so this
 * component is only the week's contents. It stays mounted while the reader steps between
 * weeks — only {@link node} changes — and is destroyed by the page once closed.
 */
@Component({
  selector: 'app-boss-detail',
  imports: [
    TranslatePipe,
    Avatar,
    ChampionBadge,
    PositionBadge,
    Drawer,
    NgOptimizedImage,
    LucideChevronLeft,
    LucideChevronRight,
    LucideSkull,
  ],
  templateUrl: './boss-detail.html',
})
export class BossDetail {
  /**
   * The week being detailed.
   */
  public readonly node = input.required<BossTimelineNode>();

  /**
   * Whether the timeline holds an earlier / later week to step to.
   */
  public readonly hasPrevious = input(false);
  public readonly hasNext = input(false);

  /**
   * Emitted when the reader asks for the adjacent week.
   */
  public readonly previous = output<void>();
  public readonly next = output<void>();

  /**
   * Emitted once the drawer has been dismissed, by any means the platform offers (the close
   * button, Escape, or a click on the backdrop).
   */
  public readonly closed = output<void>();

  /**
   * Visual treatment matching the detailed week's status, shared with its timeline node.
   */
  protected readonly tier = computed(() => resolveBossTimelineTier(this.node().status));
}
