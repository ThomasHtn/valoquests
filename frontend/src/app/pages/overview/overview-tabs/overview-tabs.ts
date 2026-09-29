import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  input,
  model,
  viewChildren,
} from '@angular/core';
import { LucideMap, LucideRadio, LucideTarget, LucideUserPen } from '@lucide/angular';
import { RouterLink } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { OverviewTab, OverviewTabKey, OverviewTabLink } from '../overview.model';

/**
 * The overview's tab bar, under the mission. It stands as the heading of its panel, in place of a
 * section rule: the tabs on the rule's hairline, the page expanding the panel at its trailing end.
 * Follows the WAI-ARIA tabs pattern, arrows and Home/End moving the selection along the bar.
 */
@Component({
  selector: 'app-overview-tabs',
  imports: [RouterLink, TranslatePipe, LucideMap, LucideRadio, LucideTarget, LucideUserPen],
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
   * Id of the panel the selected tab controls.
   */
  public readonly panelId = input.required<string>();

  /**
   * Page that expands the selected tab's panel, or `null` when none does.
   */
  protected readonly selectedLink = computed<OverviewTabLink | null>(
    () => this.tabs().find((tab) => tab.key === this.selected())?.link ?? null,
  );

  private readonly buttons = viewChildren<ElementRef<HTMLButtonElement>>('tab');

  protected tabId(key: OverviewTabKey): string {
    return `overview-tab-${key}`;
  }

  protected select(key: OverviewTabKey, index: number): void {
    this.selected.set(key);
    // Keeps the picked tab in view where the bar scrolls sideways on a phone.
    this.buttons()[index]?.nativeElement.scrollIntoView({ block: 'nearest', inline: 'nearest' });
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
    this.select(this.tabs()[target].key, target);
    this.buttons()[target]?.nativeElement.focus();
  }
}
