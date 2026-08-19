import { Component, input } from '@angular/core';

/**
 * The micro-label captioning the block under it, set against the trailing edge of the page: a
 * running total, the scope a table is filtered to, the name of a backoffice section.
 *
 * Was a diamond-tipped rule carrying that label at its far end. The rule went: with the context
 * bar now closing the chrome with a rule of its own, a second one immediately under it read as a
 * repeat of the first rather than as a separator, and on the screens whose divider carried no
 * label at all it was drawing a line for nothing. What was worth keeping is the label — so a
 * caption with nothing to say is simply not rendered, and its call site is dropped instead.
 *
 * Pulled tight against the block below through a negative bottom margin: it names that block, and
 * the page stack's full gap on both sides would leave it floating between two of them.
 */
@Component({
  selector: 'app-section-label',
  templateUrl: './section-label.html',
  host: {
    class: 'flex justify-end',
    '[class.hidden]': '!label()',
    '[class.-mb-3]': '!!label()',
  },
})
export class SectionLabel {
  /**
   * Already-translated caption. Empty on a screen whose label depends on state (the campaign only
   * has a tally to show in its history view), in which case nothing is rendered at all.
   */
  public readonly label = input('');
}
