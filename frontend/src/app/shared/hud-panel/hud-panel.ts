import { Component, input } from '@angular/core';

/**
 * One block of a dashboard, framed as a module of an instrument console rather than as a heading
 * followed by loose content.
 *
 * The overview page used to mix the two: its right column held framed cards (the boss fight, the
 * ranking rows) while its left column was bare content sitting directly on the page background
 * under a rule. The two columns therefore started at the same y and agreed on nothing after it,
 * which is what read as "the day block doesn't line up with the column beside it" — it was not a
 * spacing bug, it was one column being a panel and the other not being one.
 *
 * Everything the frame is made of already existed in the design language and was simply never
 * applied consistently: the top-right cut (`notch-tr`, with `notch-tr-edge` redrawing the border
 * across it), the panel fill and its cast shadow, and the hexagonal plate the challenge tiers and
 * the boss card wear. This puts them in one place so no screen has to reassemble them, and so a
 * block can never again be half a panel.
 */
@Component({
  selector: 'app-hud-panel',
  templateUrl: './hud-panel.html',
  host: {
    class:
      'notch-tr notch-tr-edge panel flex min-w-0 flex-col border border-edge [--notch:0.875rem]',
  },
})
export class HudPanel {
  /**
   * Already-translated name of the block, set in the header band.
   */
  public readonly heading = input.required<string>();

  /**
   * Padding and layout of the projected body, overridable for a block whose content reaches its own
   * edges (a list of rows, a chart bleeding to the frame) rather than sitting in a gutter.
   */
  public readonly bodyClass = input('flex min-w-0 flex-1 flex-col p-4');
}
