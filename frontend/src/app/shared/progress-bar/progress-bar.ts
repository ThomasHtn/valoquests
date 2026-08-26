import { Component, computed, input } from '@angular/core';

/**
 * Thin, square-ended progress track with a colored fill.
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
    class: 'relative block overflow-hidden bg-surface-sunken',
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

  /**
   * Draws a bright hairline at the fill's head, the game-style marker the boss health bar uses.
   * Opt-in: on a thin track it reads as decoration, so only the call sites treating the bar as a
   * gauge rather than a plain indicator turn it on.
   */
  public readonly edgeMarker = input(false);

  /**
   * Draws a second, static tick at the level the fill is heading for, so the value can be read
   * against where it is supposed to sit rather than against an empty track.
   *
   * The colony's gauges need this: the one holding the colony back settles far below the health it
   * produces, so a squad comfortably holding half its capacity still sees that bar around a third.
   * Without the tick it reads as a failure; with it, as being on the mark.
   *
   * `null` draws nothing.
   */
  public readonly targetMarker = input<number | null>(null);

  /**
   * The target tick, or `null` when there is nothing to draw.
   *
   * Skipped at the two ends for the same reason the leading edge is: at 0 % it sits on an empty
   * track and reads as a value of its own, at 100 % it merges with the track's own edge.
   */
  protected readonly visibleTargetMarker = computed<number | null>(() => {
    const target = this.targetMarker();

    return target !== null && target > 0 && target < 100 ? target : null;
  });
}
