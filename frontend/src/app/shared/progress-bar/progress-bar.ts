import { Component, input } from '@angular/core';

/**
 * Thin, rounded progress track with a colored fill.
 *
 * Shared by every screen showing completion or performance as a percentage (win rate, challenge
 * and ranking progress) so they all render the exact same track. The host element is the track
 * itself; callers control its width with a plain `class` attribute (e.g. `class="w-16"` or
 * `class="flex-1"`) since that varies with the surrounding layout.
 *
 * Hidden from assistive technology: every call site renders the same value as adjacent text (for
 * example `"61 %"` beside a win-rate bar, or `"3 / 7 joueurs"` beside a challenge bar), so the bar
 * is a redundant visual encoding. Exposing it as a `progressbar` would make screen readers
 * announce the same number twice. Any future call site that renders a bar *without* an adjacent
 * textual value must expose the value itself rather than relying on this component.
 */
@Component({
  selector: 'app-progress-bar',
  templateUrl: './progress-bar.html',
  host: {
    class: 'block overflow-hidden rounded-full bg-surface-700',
    '[class]': 'heightClass()',
    'aria-hidden': 'true',
  },
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
