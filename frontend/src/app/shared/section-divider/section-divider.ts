import { Component, input } from '@angular/core';

/**
 * Thin diamond-tipped rule separating two blocks of a page.
 *
 * The direction's separator: a solid diamond at the leading edge where the rule is strongest, a
 * hollow one where it has faded out. Takes an optional trailing {@link label} — a running total or
 * a stat qualifying the block below — which is the one part of the divider that is not decorative
 * and therefore stays exposed to assistive technology.
 */
@Component({
  selector: 'app-section-divider',
  templateUrl: './section-divider.html',
  // The vertical margin rides on the component rather than on every call site: all thirty-odd of
  // them set the same `my-2`, which is not a per-page decision but part of what a divider is.
  host: { class: 'my-2 flex items-center gap-3.5' },
})
export class SectionDivider {
  /**
   * Already-translated text shown between the rule and its trailing diamond, when the divider
   * carries one.
   */
  public readonly label = input('');
}
