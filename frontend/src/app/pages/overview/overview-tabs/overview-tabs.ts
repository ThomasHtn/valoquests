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
import { Tooltip } from '@shared/tooltip/tooltip';
import { OverviewTab, OverviewTabKey } from './overview-tabs.model';
import { TAB_SLIDE_EASING, TAB_SLIDE_MS } from './overview-tabs.constants';

/**
 * Overview tab bar (WAI-ARIA tabs pattern); a star pins the default tab, which leads the bar.
 */
@Component({
  selector: 'app-overview-tabs',
  imports: [
    Tooltip,
    TranslatePipe,
    LucideMap,
    LucideRadio,
    LucideStar,
    LucideSunrise,
    LucideUserPen,
  ],
  templateUrl: './overview-tabs.html',
  styleUrl: './overview-tabs.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OverviewTabs {
  /**
   * Tabs in bar order.
   */
  public readonly tabs = input.required<readonly OverviewTab[]>();

  /**
   * Tab whose panel is on screen.
   */
  public readonly selected = model.required<OverviewTabKey>();

  /**
   * Tab the page opens on, `null` when none is pinned.
   */
  public readonly favorite = model<OverviewTabKey | null>(null);

  /**
   * Id of the panel the selected tab controls.
   */
  public readonly panelId = input.required<string>();

  /**
   * Tab buttons, to move the focus on arrow keys.
   */
  private readonly buttons = viewChildren<ElementRef<HTMLButtonElement>>('tab');

  /**
   * Tab slots, measured to slide them when the pinned tab reorders the bar.
   */
  private readonly slots = viewChildren<ElementRef<HTMLElement>>('slot');

  /**
   * Injector for the after-render hook that slides the slots.
   */
  private readonly injector = inject(Injector);

  /**
   * Stable DOM id of a tab, referenced by its panel's `aria-labelledby`.
   */
  protected tabId(key: OverviewTabKey): string {
    return `overview-tab-${key}`;
  }

  /**
   * Shows the tab's panel.
   */
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

  /**
   * Records each slot's place before a reorder, keyed by tab.
   */
  private slotPositions(): Map<string, DOMRect> {
    return new Map(
      this.slots().map(({ nativeElement }) => [
        nativeElement.dataset['tab'] ?? '',
        nativeElement.getBoundingClientRect(),
      ]),
    );
  }

  /**
   * Slides each moved slot from its former place (FLIP).
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

  /**
   * Arrow, Home and End keys select and focus another tab, as the tabs pattern expects.
   */
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
