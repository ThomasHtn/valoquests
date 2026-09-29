import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  model,
  viewChildren,
} from '@angular/core';
import { LucideMap, LucideRadio, LucideStar, LucideSunrise, LucideUserPen } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { OverviewTab, OverviewTabKey } from '../overview.model';

/**
 * The overview's tab bar, under the mission. It stands as the heading of its panel, in place of a
 * section rule, its tabs sharing the rule's hairline across the full width.
 * Follows the WAI-ARIA tabs pattern, arrows and Home/End moving the selection along the bar.
 * The tab on screen carries a star that pins it as the one the page opens on.
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
    this.favorite.set(this.favorite() === key ? null : key);
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
