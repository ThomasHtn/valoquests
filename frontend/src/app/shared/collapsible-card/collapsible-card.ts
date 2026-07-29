import { Component, input, model } from '@angular/core';
import { LucideChevronDown } from '@lucide/angular';

/**
 * Monotonically increasing counter backing {@link CollapsibleCard.contentId}.
 *
 * A per-instance id is required because the disclosure button references its panel through
 * `aria-controls`, which must resolve to exactly one element in the document.
 */
let instanceCount = 0;

/**
 * Bordered card whose body can be collapsed from its header.
 *
 * Shared by the overview widgets so they render the same disclosure affordance and expose the
 * same `aria-expanded`/`aria-controls` wiring. The header icon is projected through a `cardIcon`
 * attribute; everything else is projected into the collapsible body.
 *
 * The body stays in the DOM and is hidden with the `hidden` attribute rather than removed with
 * `@if`, so `aria-controls` always resolves to a real element (an `aria-controls` pointing at a
 * missing id is an accessibility violation) and the loaded data survives a collapse/expand cycle.
 */
@Component({
  selector: 'app-collapsible-card',
  imports: [LucideChevronDown],
  templateUrl: './collapsible-card.html',
  host: { class: 'block' },
})
export class CollapsibleCard {
  /**
   * Already-translated heading rendered next to the projected icon.
   */
  public readonly heading = input.required<string>();

  /**
   * Whether the card's body is visible. Two-way bindable so callers can drive or observe it.
   */
  public readonly expanded = model(true);

  /**
   * Unique id linking the disclosure button to the panel it controls.
   */
  protected readonly contentId = `collapsible-card-${++instanceCount}`;

  /**
   * Toggles the card's body between expanded and collapsed.
   */
  protected toggle(): void {
    this.expanded.update((expanded) => !expanded);
  }
}
