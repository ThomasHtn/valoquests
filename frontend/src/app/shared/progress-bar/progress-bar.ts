import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Thin, rounded progress track with a colored fill.
 *
 * Shared by every screen showing completion or performance as a percentage (win rate, challenge
 * and ranking progress) so they all render the exact same track. The host element is the track
 * itself; callers control its width with a plain `class` attribute (e.g. `class="w-16"` or
 * `class="flex-1"`) since that varies with the surrounding layout.
 */
@Component({
  selector: 'app-progress-bar',
  templateUrl: './progress-bar.html',
  host: {
    class: 'block overflow-hidden rounded-full bg-surface-700',
    '[class]': 'heightClass()',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProgressBar {
  /**
   * Fill percentage, from 0 to 100.
   */
  public readonly percentage = input.required<number>();

  /**
   * Tailwind background color utility applied to the fill, resolved by the caller so this
   * component stays agnostic of the domain the percentage comes from.
   */
  public readonly colorClass = input.required<string>();

  /**
   * Tailwind height utility applied to the track, for the few call sites needing a taller bar.
   */
  public readonly heightClass = input('h-1');
}
