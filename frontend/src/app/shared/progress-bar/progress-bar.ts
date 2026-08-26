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
    '[class]': 'heightClass() + " " + radiusClass()',
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
   * Level a second band, drawn from the fill's head, reaches. `null` draws a single-band bar.
   *
   * The colony's food bar is what this exists for: the first band is what the town already eats,
   * the second is what is left over to make it grow, and the empty remainder is the housing the
   * food does not reach. Three shapes in one track, so "should we play tonight" is answered by
   * looking at it rather than by reading a sentence. The morale bar reuses it the other way round,
   * with its unreachable floor as the first band.
   *
   * The two bands meet flush: the colour change is the split, and the dark seam that used to be cut
   * between them read as a notch in the track rather than as a boundary.
   */
  public readonly secondaryPercentage = input<number | null>(null);

  /**
   * Tailwind background color utility applied to the second band.
   */
  public readonly secondaryColorClass = input('');

  /**
   * Tailwind height utility applied to the track, for the few call sites needing a taller bar.
   */
  public readonly heightClass = input('h-1');

  /**
   * Tailwind border-radius utility applied to the track.
   *
   * Square by default, which is the direction's own silhouette everywhere a bar reads as a plain
   * indicator — a win rate, a challenge, a ranking row. The colony's resource rails opt into a
   * rounded track instead: they are the gauges of a management screen, read as a row of capsules
   * rather than as a column of ledger figures. The host clips its own overflow, so this rounds the
   * fill and both bands with it.
   */
  public readonly radiusClass = input('');

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
   * `null` draws nothing.
   */
  public readonly targetMarker = input<number | null>(null);

  /**
   * The second band's width, or `null` when there is no second band to draw.
   *
   * Clamped at the fill's head rather than allowed to run backwards: the two bands describe one
   * quantity split in two, so a second level under the first is a caller bug, and drawing it as a
   * negative width would put a stray sliver at the wrong end of the track.
   */
  protected readonly secondaryWidth = computed<number | null>(() => {
    const secondary = this.secondaryPercentage();

    return secondary === null ? null : Math.max(0, secondary - this.percentage());
  });

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
