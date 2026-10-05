import { Component, input } from '@angular/core';

import { RuleText } from '../rule-text/rule-text';

/**
 * Numbered rules section: title and statement on the left, projected figures on the right.
 */
@Component({
  selector: 'app-rule-section',
  imports: [RuleText],
  templateUrl: './rule-section.html',
  styleUrl: './rule-section.scss',
  // A box, not `display: contents`, so the page stack's gutter reaches it.
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
   * Translated description with `app-rule-text` tokens, empty when the figures suffice.
   */
  public readonly description = input('');

  /**
   * Whether the section opens with a top hairline (off for the first one).
   */
  public readonly bordered = input(true);

  /**
   * Deep-link fragment from `RULE_ANCHOR`, used as the section `id`.
   */
  public readonly anchor = input.required<string>();
}
