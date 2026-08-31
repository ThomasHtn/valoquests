import { Component, input } from '@angular/core';

/**
 * Shell shared by the four numbered beats of the rules page: a numbered marker, a title and a
 * description on the left, the beat's own figures projected on the right.
 *
 * Not used by the bonuses section between beats 02 and 03: that one is deliberately marked and
 * styled differently (a "+" instead of a number, a tinted background) because it is not a fifth
 * beat of the loop but a multiplier sitting on top of the two around it.
 */
@Component({
  selector: 'app-rule-section',
  templateUrl: './rule-section.html',
  // A box rather than `display: contents`: the section is this component's only child, so the box
  // costs nothing, and without one the page stack's gutter would not reach it.
  //
  // Hidden from the host rather than from the `<section>` inside it, so a filtered-out beat leaves
  // no empty box behind in the page's stack — `page-body`'s own `gap` would otherwise still spend a
  // gutter on it.
  host: { class: 'block', '[hidden]': '!visible()' },
})
export class RuleSection {
  /**
   * Two-digit beat number shown as the section's marker (e.g. "01").
   */
  public readonly index = input.required<string>();

  /**
   * Already-translated section title.
   */
  public readonly heading = input.required<string>();

  /**
   * Already-translated section description.
   */
  public readonly description = input.required<string>();

  /**
   * Whether the section opens with a top hairline, off only for the first beat since there is
   * nothing above it to separate from.
   */
  public readonly bordered = input(true);

  /**
   * Fragment identifying this section, from `RULE_ANCHORS` — the `id` a contents entry and a deep
   * link from another screen both target.
   */
  public readonly anchor = input.required<string>();

  /**
   * Whether the section is shown at all. Off while a search is running and this section does not
   * match it.
   */
  public readonly visible = input(true);
}
