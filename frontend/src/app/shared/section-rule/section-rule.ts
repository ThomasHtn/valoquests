import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { fitToLines } from './section-rule.utils';

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
   * Fragment of the linked page to land on, a section of the rules for one.
   */
  public readonly linkFragment = input<string | undefined>(undefined);

  /**
   * Caption on the right, for a section whose heading needs a count beside it.
   */
  public readonly side = input('');

  /**
   * Whether, on a phone, the projected content drops to a line of its own under the heading while
   * the hairline stays between the heading and the diamond. The projected element takes that line
   * itself (`order` and a full `flex-basis` below `sm`).
   */
  public readonly wrapSide = input(false);

  /**
   * Whether the hairline stays on a phone: only a caption, which keeps the title's line, needs its
   * room.
   */
  protected readonly keepsLine = computed(() => this.wrapSide() || !this.side());

  /**
   * Whether, on a phone, the right-hand group dissolves into the row so the link or the projected
   * content can take a line of their own.
   */
  protected readonly dissolves = computed(() => this.wrapSide() || !!this.link());

  private readonly title = viewChild.required<ElementRef<HTMLHeadingElement>>('title');

  constructor() {
    // A title wrapped on a phone hugs its text, so the hairline starts where the words end.
    afterRenderEffect((onCleanup) => {
      this.heading();
      const title = this.title().nativeElement;
      const observer = new ResizeObserver(() => fitToLines(title));
      observer.observe(title.parentElement ?? title);
      onCleanup(() => observer.disconnect());
    });
  }
}
