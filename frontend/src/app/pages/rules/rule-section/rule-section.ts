import { Component, input } from '@angular/core';

import { RuleText } from '../rule-text/rule-text';

/**
 * Shell shared by the numbered sections of the rules page: a marker, a title, the rule stated in
 * one sentence and a description on the left, the section's own figures projected on the right.
 */
@Component({
  selector: 'app-rule-section',
  imports: [RuleText],
  templateUrl: './rule-section.html',
  // A box rather than `display: contents`: the section is this component's only child, so the box
  // costs nothing, and without one the page stack's gutter would not reach it.
  host: { class: 'block' },
})
export class RuleSection {
  /**
   * Two-digit marker shown beside the title (e.g. "01").
   */
  public readonly index = input.required<string>();

  /**
   * Already-translated section title.
   */
  public readonly heading = input.required<string>();

  /**
   * Already-translated one-sentence statement of the rule, set under the title.
   */
  public readonly statement = input.required<string>();

  /**
   * Already-translated section description. May carry icon tokens, see `app-rule-text`.
   */
  public readonly description = input.required<string>();

  /**
   * Whether the section opens with a top hairline, off only for the first one since there is
   * nothing above it to separate from.
   */
  public readonly bordered = input(true);

  /**
   * Fragment identifying this section, from `RULE_ANCHOR`: the `id` a deep link from another
   * screen targets.
   */
  public readonly anchor = input.required<string>();
}
