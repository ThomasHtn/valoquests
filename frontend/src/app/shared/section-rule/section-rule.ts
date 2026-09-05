import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Heading of a page section: the title, a fading rule, an optional link to the page that owns
 * the subject in full, and the diamond that closes the line.
 *
 * Declared once so the sections of a screen read as one document rather than as a stack of
 * blocks each with its own idea of a title.
 */
@Component({
  selector: 'app-section-rule',
  imports: [RouterLink],
  templateUrl: './section-rule.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionRule {
  /**
   * Section title.
   */
  public readonly heading = input.required<string>();

  /**
   * Id the title carries, so the section can be labelled by it.
   */
  public readonly headingId = input.required<string>();

  /**
   * Label of the link on the right, omitted when there is nowhere to go.
   */
  public readonly linkLabel = input('');

  /**
   * Route the link leads to.
   */
  public readonly link = input<string | null>(null);

  /**
   * Caption on the right, for a section whose heading needs a count beside it.
   */
  public readonly side = input('');
}
