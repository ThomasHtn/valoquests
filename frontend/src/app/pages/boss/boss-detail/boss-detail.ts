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
import { LucideChevronLeft, LucideChevronRight, LucideSkull, LucideX } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { resolveBossTimelineTier } from '../boss-timeline.constants';
import { BossTimelineNode } from '../boss.model';

/**
 * Detail panel for one week of the campaign, opened from a timeline node.
 *
 * A right-anchored drawer built on the native `<dialog>` element: modality, the backdrop, Escape
 * to dismiss and the focus trap all come from the platform, so the component only owns opening it
 * and reporting the dismissal back to the page. It stays mounted while the reader steps between
 * weeks — only {@link node} changes — and is destroyed by the page once closed.
 */
@Component({
  selector: 'app-boss-detail',
  imports: [
    TranslatePipe,
    Avatar,
    ChampionBadge,
    NgOptimizedImage,
    LucideChevronLeft,
    LucideChevronRight,
    LucideSkull,
    LucideX,
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
   * The drawer element itself, needed to drive it through the imperative `<dialog>` API.
   */
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  /**
   * Visual treatment matching the detailed week's status, shared with its timeline node.
   */
  protected readonly tier = computed(() => resolveBossTimelineTier(this.node().status));

  /**
   * Opens the drawer as a modal as soon as it is in the DOM — the component is only ever rendered
   * in response to the reader selecting a week, so there is no state where it should sit closed —
   * and wires dismissal by clicking the backdrop.
   *
   * That last listener is bound here rather than as a template `(click)`: a `<dialog>`'s backdrop
   * is a pseudo-element with no node of its own, so the click surfaces on the dialog itself, and a
   * click handler on an element that is not itself a control is exactly what the template
   * accessibility rules reject — rightly, except that here the keyboard equivalent is Escape,
   * which the platform already handles.
   */
  constructor() {
    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      dialog.showModal();
      dialog.addEventListener('click', (event) => {
        if (event.target === dialog) {
          dialog.close();
        }
      });
    });
  }

  /**
   * Dismisses the drawer. The `close` event it fires is what notifies the page, so this path and
   * the platform's own (Escape) end up reporting through the same channel.
   */
  protected close(): void {
    this.dialog().nativeElement.close();
  }
}
