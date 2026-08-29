import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideGauge,
  LucideHammer,
  LucideMagnet,
  LucideSkull,
} from '@lucide/angular';

import { resolveBossTimelineTier } from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { ColonyBossView } from '@core/colony/colony-view.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { BossContributions } from '@shared/boss-contributions/boss-contributions';
import { Drawer } from '@shared/drawer/drawer';
import { resolveBossNumberLabel } from '../campaign-map.constants';

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
    RouterLink,
    BossContributions,
    Drawer,
    LucideChevronLeft,
    LucideChevronRight,
    LucideGauge,
    LucideHammer,
    LucideMagnet,
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
   * What the week's fight is worth the colony — the same figures the map's own hover card and the
   * always-visible current-boss panel read, joined on the node by its caller. `null` for a week the
   * colony has nothing to report on yet (a locked week ahead).
   */
  public readonly colonyBoss = input<ColonyBossView | null>(null);

  /**
   * What a boss still standing costs the colony, a fixed penalty read here only for the week
   * currently being fought, since it is not yet a settled cost.
   */
  public readonly defeatMoraleLabel = input('');

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

  /**
   * Position of the week inside its run, as the two-digit boss number the header badge leads with —
   * the same number the map's own hexagons carry.
   */
  protected readonly bossNumberLabel = resolveBossNumberLabel;
}
