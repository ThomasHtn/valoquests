import { NgOptimizedImage } from '@angular/common';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  input,
  output,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideGauge,
  LucideHammer,
  LucideMagnet,
  LucideSkull,
  LucideX,
} from '@lucide/angular';

import { resolveBossTerritoryTier } from '@core/boss/boss-timeline.constants';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { resolveBossNumberLabel } from '@core/boss/boss-visual.utils';
import { ColonyBossView } from '@core/colony/colony-view.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { BossContributions } from '@shared/boss-contributions/boss-contributions';

/**
 * Detail panel for one week of the campaign, unfolding under the threat map it is opened from.
 *
 * Inline rather than a drawer: the map is the page's own subject, and a modal over it hid the very
 * row the reader was stepping through. The panel owns its own header and dismissal instead, and is
 * mounted flush against the card above it — the seam's colour is the clicked hexagon's own, which
 * is what ties the two together in place of an overlay's animation.
 *
 * It stays mounted while the reader steps between weeks — only {@link node} changes — and is
 * destroyed by the page once closed.
 */
@Component({
  selector: 'app-boss-detail',
  imports: [
    TranslatePipe,
    RouterLink,
    NgOptimizedImage,
    BossContributions,
    LucideChevronLeft,
    LucideChevronRight,
    LucideGauge,
    LucideHammer,
    LucideMagnet,
    LucideSkull,
    LucideX,
  ],
  templateUrl: './boss-detail.html',
  host: { class: 'block' },
})
export class BossDetail {
  /**
   * The week being detailed.
   */
  public readonly node = input.required<BossTimelineNode>();

  /**
   * What the week's fight is worth the colony — the same figures the map's own hover card reads,
   * joined on the node by its caller. `null` for a week the colony has nothing to report on yet (a
   * locked week ahead).
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
   * Emitted when the reader closes the panel, by the close button or by Escape.
   */
  public readonly closed = output<void>();

  /**
   * The panel itself, focused once on open so Escape reaches it and so a reader on a narrow screen
   * lands on the panel rather than on the map they just left.
   */
  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');

  /**
   * Visual treatment matching the detailed week's status — the map's own territory vocabulary, not
   * the legacy timeline's: this panel hangs off a hexagon, and the two must not disagree on what
   * colour a week is.
   */
  protected readonly tier = computed(() => resolveBossTerritoryTier(this.node().status));

  /**
   * Whether the colony has anything to report on this week at all — rewards banked, a defeat still
   * on the table, or a settled loss. Gates the block's own separator, which would otherwise rule
   * off an empty grid.
   */
  protected readonly hasColonyReport = computed(() => {
    const boss = this.colonyBoss();

    if (boss === null) {
      return false;
    }

    return Boolean(boss.materialsLabel) || boss.state === 'CURRENT' || boss.state === 'SURVIVED';
  });

  /**
   * Position of the week inside its run, as the two-digit boss number the header badge leads with —
   * the same number the map's own hexagons carry.
   */
  protected readonly bossNumberLabel = resolveBossNumberLabel;

  constructor() {
    afterNextRender(() => this.panel().nativeElement.focus());
  }
}
