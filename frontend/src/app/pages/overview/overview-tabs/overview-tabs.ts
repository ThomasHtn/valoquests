import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  input,
  model,
  viewChildren,
} from '@angular/core';
import { LucideMap, LucideRadio, LucideStar, LucideSunrise, LucideUserPen } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { OverviewTab, OverviewTabKey } from '../overview.model';
import { TAB_SLIDE_EASING, TAB_SLIDE_MS } from './overview-tabs.constants';

/**
 * The overview's tab bar, under the mission. It stands as the heading of its panel, in place of a
 * section rule, its tabs sharing the rule's hairline across the full width.
 * Follows the WAI-ARIA tabs pattern, arrows and Home/End moving the selection along the bar.
 * The tab on screen carries a star that pins it as the one the page opens on; the pinned tab
 * leads the bar, and the tabs slide to their new places when it changes.
 */
@Component({
  selector: 'app-overview-tabs',
  imports: [TranslatePipe, LucideMap, LucideRadio, LucideStar, LucideSunrise, LucideUserPen],
  templateUrl: './overview-tabs.html',
  styleUrl: './overview-tabs.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OverviewTabs {
  /**
   * The tabs, in bar order.
   */
  public readonly tabs = input.required<readonly OverviewTab[]>();

  /**
   * The tab whose panel is on screen.
   */
  public readonly selected = model.required<OverviewTabKey>();

  /**
   * The tab the page opens on, or `null` when none is pinned.
   */
  public readonly favorite = model<OverviewTabKey | null>(null);

  /**
   * Id of the panel the selected tab controls.
   */
  public readonly panelId = input.required<string>();

  private readonly buttons = viewChildren<ElementRef<HTMLButtonElement>>('tab');

  private readonly slots = viewChildren<ElementRef<HTMLElement>>('slot');

  private readonly injector = inject(Injector);

  protected tabId(key: OverviewTabKey): string {
    return `overview-tab-${key}`;
  }

  protected select(key: OverviewTabKey): void {
    this.selected.set(key);
  }

  /**
   * Pins the tab as the default, or unpins it when it already is.
   */
  protected toggleFavorite(key: OverviewTabKey): void {
    const before = this.slotPositions();
    const focused = document.activeElement;
    this.favorite.set(this.favorite() === key ? null : key);
    afterNextRender(
      () => {
        this.slide(before);
        // Reordering moves nodes in the DOM, which can drop the focus off the star.
        if (focused instanceof HTMLElement && document.activeElement !== focused) {
          focused.focus();
        }
      },
      { injector: this.injector },
    );
  }

  private slotPositions(): Map<string, DOMRect> {
    return new Map(
      this.slots().map(({ nativeElement }) => [
        nativeElement.dataset['tab'] ?? '',
        nativeElement.getBoundingClientRect(),
      ]),
    );
  }

  /**
   * Plays each moved slot from its former place to its new one (FLIP).
   */
  private slide(before: Map<string, DOMRect>): void {
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return;
    }
    for (const { nativeElement } of this.slots()) {
      const from = before.get(nativeElement.dataset['tab'] ?? '');
      if (!from) {
        continue;
      }
      const to = nativeElement.getBoundingClientRect();
      const dx = from.left - to.left;
      const dy = from.top - to.top;
      if (dx === 0 && dy === 0) {
        continue;
      }
      nativeElement.animate([{ translate: `${dx}px ${dy}px` }, { translate: '0 0' }], {
        duration: TAB_SLIDE_MS,
        easing: TAB_SLIDE_EASING,
      });
    }
  }

  protected onKeydown(event: KeyboardEvent, index: number): void {
    const count = this.tabs().length;
    const target =
      event.key === 'ArrowRight'
        ? (index + 1) % count
        : event.key === 'ArrowLeft'
          ? (index - 1 + count) % count
          : event.key === 'Home'
            ? 0
            : event.key === 'End'
              ? count - 1
              : null;
    if (target === null) {
      return;
    }
    event.preventDefault();
    this.select(this.tabs()[target].key);
    this.buttons()[target]?.nativeElement.focus();
  }
}
